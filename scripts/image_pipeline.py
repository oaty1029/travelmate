# -*- coding: utf-8 -*-
"""
图片管线：
1) 读取 scripts/data/cities.tsv 与 attractions.tsv（UTF-8，制表符分隔）
2) 为每个城市下载/生成 1 张封面图
3) 为每个景点下载/生成 3 张图（Commons 真实图优先，不足用本地渐变图兜底）
4) 输出到 backend/uploads/images/{城市}/ 与 .../attractions/
5) 生成 scripts/data/image_map.json（供 SQL 生成器复用 URL 规则）
可重复运行：已存在的非空文件会被跳过。
"""
import io
import json
import os
import re
import sys
import time
import random
import threading

import requests
from PIL import Image, ImageDraw, ImageFont

from query_map import ATTR_QUERY, CITY_QUERY

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE, "scripts", "data")
IMG_ROOT = os.path.join(BASE, "backend", "uploads", "images")

UA = "TravelMateEduProject/1.0 (educational graduation project; contact: student@example.com)"
FONT_BOLD = r"C:/Windows/Fonts/simhei.ttf"
FONT_LIGHT = r"C:/Windows/Fonts/simkai.ttf"

# 16 组渐变配色
PALETTES = [
    ("#667eea", "#764ba2"), ("#f093fb", "#f5576c"), ("#4facfe", "#00f2fe"),
    ("#43e97b", "#38f9d7"), ("#fa709a", "#fee140"), ("#30cfd0", "#330867"),
    ("#a8edea", "#fed6e3"), ("#ff9a9e", "#fecfef"), ("#fbc2eb", "#a6c1ee"),
    ("#fdcbf1", "#e6dee9"), ("#84fab0", "#8fd3f4"), ("#e0c3fc", "#8ec5fc"),
    ("#f6d365", "#fda085"), ("#5ee7df", "#b490ca"), ("#c3cfe2", "#f5f7fa"),
    ("#fddb92", "#d1fdff"),
]

session = requests.Session()
session.headers["User-Agent"] = UA


def sanitize(name):
    """保留中文/字母/数字/下划线，其余字符剔除。"""
    name = (name or "").strip()
    name = re.sub(r"[\s/\\:*?\"<>|]+", "", name)
    return name or "unnamed"


def parse_tsv(path):
    rows = []
    with io.open(path, encoding="utf-8") as f:
        for line in f.read().splitlines():
            if not line.strip():
                continue
            rows.append(line.split("\t"))
    return rows


def search_commons(query, limit=3):
    try:
        r = session.get(
            "https://commons.wikimedia.org/w/api.php",
            params={
                "action": "query",
                "generator": "search",
                "gsrsearch": query,
                "gsrnamespace": "6",
                "gsrlimit": str(limit),
                "prop": "imageinfo",
                "iiprop": "url",
                "iiurlwidth": "1000",
                "format": "json",
            },
            timeout=20,
        )
        d = r.json()
        pages = d.get("query", {}).get("pages", {})
        out = []
        for p in pages.values():
            ii = (p.get("imageinfo") or [{}])[0]
            if ii.get("thumburl"):
                out.append({"title": p.get("title", ""), "url": ii["thumburl"]})
        return out
    except Exception:
        return []


def valid_jpeg(path):
    try:
        im = Image.open(path)
        im.verify()
        return True
    except Exception:
        return False


def download(url, path):
    try:
        r = session.get(url, timeout=30)
        if r.status_code != 200:
            return False
        with open(path, "wb") as f:
            f.write(r.content)
        return valid_jpeg(path)
    except Exception:
        return False


def hex_to_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def gradient_pair(pair):
    return hex_to_rgb(pair[0]), hex_to_rgb(pair[1])


def make_placeholder(path, title, subtitle, pal_idx, variant):
    """生成 1200x800 渐变图，标题居中，副标题在下。"""
    W, H = 1200, 800
    c1, c2 = gradient_pair(PALETTES[(pal_idx + variant * 5) % len(PALETTES)])
    img = Image.new("RGB", (W, H))
    px = img.load()
    for y in range(H):
        t = y / (H - 1)
        r = int(c1[0] * (1 - t) + c2[0] * t)
        g = int(c1[1] * (1 - t) + c2[1] * t)
        b = int(c1[2] * (1 - t) + c2[2] * t)
        for x in range(W):
            px[x, y] = (r, g, b)

    d = ImageDraw.Draw(img, "RGBA")
    # 装饰圆
    for cx, cy, rad, alpha in [(980, 180, 260, 40), (180, 640, 220, 30)]:
        d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=(255, 255, 255, alpha))

    # 标题字号自适应
    n = len(title)
    fsize = max(60, min(150, int(W * 0.82 / max(n, 1))))
    f = ImageFont.truetype(FONT_BOLD, fsize)
    try:
        bbox = d.textbbox((0, 0), title, font=f)
        tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    except Exception:
        tw, th = W // 2, fsize
    d.text(((W - tw) / 2, (H - th) / 2 - 30), title, font=f, fill=(255, 255, 255, 255))

    if subtitle:
        fs = ImageFont.truetype(FONT_LIGHT, 44)
        try:
            b2 = d.textbbox((0, 0), subtitle, font=fs)
            sw = b2[2] - b2[0]
        except Exception:
            sw = len(subtitle) * 44
        d.text(((W - sw) / 2, (H - th) / 2 + th + 60), subtitle, font=fs,
               fill=(255, 255, 255, 230))

    img.save(path, "JPEG", quality=88)


def ensure_placeholder(path, title, subtitle, pal_idx, variant):
    if os.path.exists(path) and os.path.getsize(path) > 0:
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    make_placeholder(path, title, subtitle, pal_idx, variant)


def city_cover_path(city_name):
    return os.path.join(IMG_ROOT, sanitize(city_name), "city.jpg")


def city_cover_url(city_name):
    return "/images/%s/city.jpg" % sanitize(city_name)


def attr_paths(city_name, attr_name):
    d = os.path.join(IMG_ROOT, sanitize(city_name), "attractions")
    base = sanitize(attr_name)
    return [os.path.join(d, base + "_%d.jpg" % i) for i in (1, 2, 3)]


def attr_urls(city_name, attr_name):
    base = sanitize(attr_name)
    return ["/images/%s/attractions/%s_%d.jpg" % (sanitize(city_name), base, i)
            for i in (1, 2, 3)]


def ensure_image(path, query, title, subtitle, pal_idx, variant):
    """下载失败则生成兜底图。"""
    if os.path.exists(path) and os.path.getsize(path) > 0:
        return "cached"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    for hit in search_commons(query, limit=3):
        if download(hit["url"], path):
            return "real"
    make_placeholder(path, title, subtitle, pal_idx, variant)
    return "gen"


def fetch_attr_images(name, cname, count=3):
    """按 英文映射 → 中文名+城市 → 中文名 顺序搜索，返回去重后的图片 URL 列表。"""
    queries = []
    if ATTR_QUERY.get(name):
        queries.append(ATTR_QUERY[name])
    queries.append("%s %s" % (name, cname))
    queries.append(name)
    seen = set()
    urls = []
    for q in queries:
        for hit in search_commons(q, limit=6):
            if hit["url"] not in seen:
                seen.add(hit["url"])
                urls.append(hit["url"])
                if len(urls) >= count:
                    return urls
    return urls


def main():
    cities = parse_tsv(os.path.join(DATA_DIR, "cities.tsv"))
    attrs = parse_tsv(os.path.join(DATA_DIR, "attractions.tsv"))

    city_name = {}
    for c in cities:
        city_name[int(c[0])] = c[2]

    city_urls = {}
    attr_url_map = {}
    stat_city = {"real": 0, "gen": 0, "cached": 0}
    stat_attr = {"real": 0, "gen": 0, "cached": 0}
    lock = threading.Lock()

    print("total cities=%d attractions=%d" % (len(cities), len(attrs)))

    def work_city(c):
        cid = int(c[0])
        name = c[2]
        pinyin = c[3]
        path = city_cover_path(name)
        queries = []
        if CITY_QUERY.get(name):
            queries.append(CITY_QUERY[name])
        if pinyin and pinyin.lower() not in ("null", ""):
            queries.append("%s skyline" % pinyin)
        queries.append(name)
        r = "cached" if (os.path.exists(path) and os.path.getsize(path) > 0) else None
        if r is None:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            r = "gen"
            for q in queries:
                ok = False
                for hit in search_commons(q, limit=3):
                    if download(hit["url"], path):
                        r, ok = "real", True
                        break
                if ok:
                    break
            if r == "gen":
                make_placeholder(path, name, "城市 · %s" % name, int(cid) % 16, 0)
        with lock:
            stat_city[r] += 1
            city_urls[cid] = city_cover_url(name)

    def work_attr(name, cname, category, key):
        paths = attr_paths(cname, name)
        urls = attr_urls(cname, name)
        found = fetch_attr_images(name, cname, 3)
        pal_idx = sum(ord(ch) for ch in name) % 16
        for i, path in enumerate(paths):
            os.makedirs(os.path.dirname(path), exist_ok=True)
            if os.path.exists(path) and os.path.getsize(path) > 0:
                r = "cached"
            elif i < len(found) and download(found[i], path):
                r = "real"
            else:
                make_placeholder(path, name, category or "景点", pal_idx, i)
                r = "gen"
            with lock:
                stat_attr[r] += 1
        with lock:
            attr_url_map[key] = urls

    sem = threading.Semaphore(6)

    def gated(fn):
        def wrapper():
            with sem:
                fn()
        return wrapper

    tasks = [gated(lambda c=c: work_city(c)) for c in cities]

    for a in attrs:
        aid = int(a[0])
        cid = int(a[1])
        name = a[2]
        category = a[3]
        cname = city_name.get(cid, "城市")
        tasks.append(gated(lambda name=name, cname=cname, category=category, aid=aid:
                           work_attr(name, cname, category, aid)))

    # 新增景点（name|city_name|category，无 id，图片按名字生成）
    new_path = os.path.join(DATA_DIR, "new_attractions.tsv")
    if os.path.exists(new_path):
        for a in parse_tsv(new_path):
            name, cname, category = a[0], a[1], a[2]
            tasks.append(gated(lambda name=name, cname=cname, category=category:
                               work_attr(name, cname, category, "new:" + name)))

    threads = [threading.Thread(target=t) for t in tasks]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    result = {
        "cities": city_urls,
        "attractions": attr_url_map,
        "stat_city": stat_city,
        "stat_attr": stat_attr,
    }
    with io.open(os.path.join(DATA_DIR, "image_map.json"), "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=1)

    print("city:", stat_city)
    print("attr:", stat_attr)
    print("done. image_map.json written.")


if __name__ == "__main__":
    main()
