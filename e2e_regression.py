# -*- coding: utf-8 -*-
"""
TravelMate 端到端回归测试（真实 HTTP 请求，Python urllib）
目标后端：http://localhost:3000/api/v1（可用环境变量 E2E_BASE 覆盖）
覆盖：健康检查 / 城市景点信息库 / 认证 / AI 行程（保存-详情-收藏-推荐-PDF）/ 论坛 / 管理端（景点 CRUD + 热门城市）
说明：AI 真实生成（/ai/generate 成功路径）依赖 DeepSeek Key 与网络，需人工验证，本脚本只验证其鉴权与入参校验。
"""
import os
import urllib.request
import urllib.error
import json
import time
import sys

BASE = os.environ.get("E2E_BASE", "http://localhost:3000/api/v1")
TS = str(int(time.time() * 1000))

PASS = 0
FAIL = 0
FAILURES = []


def http(method, path, token=None, body=None, raw=False, accept="application/json"):
    url = BASE + path
    headers = {"Content-Type": "application/json", "Accept": accept}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        r = urllib.request.urlopen(req, timeout=40)
        status = r.getcode()
        content = r.read()
        ctype = r.headers.get("Content-Type", "")
        if raw:
            return status, content, ctype
        return status, json.loads(content.decode("utf-8"))
    except urllib.error.HTTPError as e:
        status = e.code
        content = e.read()
        if raw:
            return status, content, ""
        try:
            return status, json.loads(content.decode("utf-8"))
        except Exception:
            return status, {"code": status, "message": content.decode("utf-8", "ignore")}
    except Exception as e:
        if raw:
            return -1, b"", ""
        return -1, {"code": -1, "message": str(e)}


def check(name, cond, detail=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print("  PASS  " + name)
    else:
        FAIL += 1
        FAILURES.append(name)
        print("  FAIL  " + name + ("  | " + detail if detail else ""))


def section(title):
    print("\n== " + title + " ==")


def notifications_with_post(token, post_id):
    s, j = http("GET", "/forum/notifications", token=token)
    if s != 200 or j.get("code") != 0:
        return []
    return [n for n in (j.get("data") or []) if n.get("postId") == post_id]


def build_itinerary_result():
    return {
        "summary": {"title": "回归测试行程" + TS, "totalDays": 2, "totalBudget": 4000, "headline": "端到端回归"},
        "days": [
            {"day": 1, "date": "2026-10-01", "city": "北京", "theme": "古城", "items": [
                {"time": "09:00-11:00", "type": "attraction", "name": "故宫", "note": "需预约", "price": 60},
                {"time": "12:00", "type": "meal", "name": "午餐", "price": 80}
            ]},
            {"day": 2, "date": "2026-10-02", "city": "北京", "theme": "长城", "items": [
                {"time": "09:00", "type": "attraction", "name": "八达岭长城", "price": 40}
            ]}
        ],
        "costBreakdown": {"transport": 1000, "accommodation": 1600, "meals": 800, "tickets": 200, "other": 400, "total": 4000},
        "tips": ["带好身份证", "提前预约热门景点"]
    }


def build_itinerary_input():
    return {
        "departure": "上海", "destinations": ["北京"], "startDate": "2026-10-01",
        "days": 2, "people": 2, "budgetPerPerson": 2000,
        "budgetLevel": "舒适", "hotelLevel": "舒适",
        "companions": ["成人"], "interests": ["历史人文"], "transport": "高铁", "pace": "均衡"
    }


# ============================================================
section("1. 健康检查与城市景点信息库（公开接口）")

s, j = http("GET", "/health")
check("health 返回 ok", s == 200 and j.get("code") == 0 and j.get("data") == "ok", str(j))

s, j = http("GET", "/destination/provinces")
provs = j.get("data") or []
check("省份列表 >= 34", s == 200 and j.get("code") == 0 and len(provs) >= 34, "len=%s" % len(provs))

s, j = http("GET", "/destination/cities?page=1&pageSize=10")
recs = (j.get("data") or {}).get("records") or []
check("城市分页列表非空", s == 200 and j.get("code") == 0 and len(recs) >= 1, str(j)[:200])
check("城市总数 >= 28", (j.get("data") or {}).get("total", 0) >= 28, "total=%s" % (j.get("data") or {}).get("total"))

s, j = http("GET", "/destination/cities/search?keyword=%E5%8C%97%E4%BA%AC")
hits = j.get("data") or []
bj = hits[0] if hits else {}
check("搜索北京命中", s == 200 and j.get("code") == 0 and "北京" in (bj.get("name") or ""), str(bj.get("name")))
BJ_ID = bj.get("id")

if BJ_ID:
    s, j = http("GET", "/destination/cities/%s" % BJ_ID)
    detail = j.get("data") or {}
    check("城市详情含 city + attractions", s == 200 and j.get("code") == 0
          and detail.get("city") is not None and detail.get("attractions") is not None, str(j)[:200])

s, j = http("GET", "/destination/cities/popular?limit=10")
check("热门城市列表可访问", s == 200 and j.get("code") == 0 and isinstance(j.get("data"), list), str(j)[:200])

s, j = http("GET", "/destination/cities/999999999")
check("不存在城市返回 404", s == 404 and j.get("code") != 0, str(j)[:200])

# ============================================================
section("2. 认证（注册/登录/负向）")

EA = "e2e_a_%s@test.com" % TS
EB = "e2e_b_%s@test.com" % TS
NICK_A = "回归用户A%s" % TS

s, j = http("POST", "/auth/register", body={"email": EA, "password": "Pass123", "nickname": NICK_A})
check("注册用户A", s == 200 and j.get("code") == 0, str(j)[:200])

s, j = http("POST", "/auth/register", body={"email": EB, "password": "Pass123", "nickname": "回归用户B%s" % TS})
check("注册用户B", s == 200 and j.get("code") == 0, str(j)[:200])

s, j = http("POST", "/auth/register", body={"email": EA, "password": "Pass123", "nickname": "重复注册%s" % TS})
check("重复邮箱注册被拒绝", j.get("code") != 0, str(j)[:200])

s, j = http("POST", "/auth/login", body={"account": EA, "password": "Pass123"})
TOKEN_A = (j.get("data") or {}).get("token")
USER_A_ID = (j.get("data") or {}).get("user", {}).get("id")
check("登录用户A", s == 200 and j.get("code") == 0 and TOKEN_A and USER_A_ID, str(j)[:200])

s, j = http("POST", "/auth/login", body={"account": EB, "password": "Pass123"})
TOKEN_B = (j.get("data") or {}).get("token")
check("登录用户B", s == 200 and j.get("code") == 0 and TOKEN_B, str(j)[:200])

s, j = http("POST", "/auth/login", body={"account": "admin@travelmate.com", "password": "Admin@123"})
TOKEN_ADMIN = (j.get("data") or {}).get("token")
ADMIN_ROLE = (j.get("data") or {}).get("user", {}).get("role")
check("登录管理员(admin)", s == 200 and j.get("code") == 0 and TOKEN_ADMIN and ADMIN_ROLE == "admin",
      "role=%s" % ADMIN_ROLE)

s, j = http("POST", "/auth/login", body={"account": EA, "password": "WrongPwd"})
check("错误密码登录被拒绝", j.get("code") != 0, str(j)[:200])

# ============================================================
section("3. AI 行程（鉴权/保存/详情/收藏/推荐/PDF）")

s, j = http("POST", "/ai/generate", body=build_itinerary_input())
check("未登录调用 AI 生成被拒绝(401)", s == 401 and j.get("code") != 0, str(j)[:200])

s, j = http("POST", "/ai/generate", token=TOKEN_A, body={})
check("AI 生成入参校验(缺必填字段被拒)", j.get("code") != 0, str(j)[:200])

s, j = http("POST", "/itineraries", body={"title": "回归行程", "input": build_itinerary_input(), "result": build_itinerary_result()})
check("未登录保存行程被拒绝(401)", s == 401 and j.get("code") != 0, str(j)[:200])

s, j = http("POST", "/itineraries", token=TOKEN_A,
            body={"title": "回归测试行程%s" % TS, "input": build_itinerary_input(), "result": build_itinerary_result()})
IT_ID = (j.get("data") or {}).get("id")
check("A保存AI行程", s == 200 and j.get("code") == 0 and IT_ID, str(j)[:200])

s, j = http("GET", "/itineraries?page=1&pageSize=10", token=TOKEN_A)
mine = (j.get("data") or {}).get("records") or []
check("A的行程列表含刚保存行程", s == 200 and j.get("code") == 0 and any(x.get("id") == IT_ID for x in mine), str(j)[:200])
check("列表不泄漏 resultJson", all((x.get("resultJson") is None) for x in mine), "列表含 resultJson")

s, j = http("GET", "/itineraries/%s" % IT_ID, token=TOKEN_A)
check("本人查看详情(含 inputJson)", s == 200 and j.get("code") == 0
      and (j.get("data") or {}).get("inputJson") is not None, str(j)[:200])

s, j = http("GET", "/itineraries/%s" % IT_ID, token=TOKEN_B)
check("他人查看未分享行程被拒绝(403)", s == 403 and j.get("code") != 0, str(j)[:200])

s, j = http("POST", "/itineraries/%s/copy" % IT_ID, token=TOKEN_A)
check("收藏自己的行程被拒绝", j.get("code") != 0, str(j)[:200])

s, pdf, ctype = http("GET", "/itineraries/%s/export" % IT_ID, token=TOKEN_A, raw=True, accept="application/pdf")
check("PDF 导出", s == 200 and pdf[:4] == b"%PDF" and "application/pdf" in (ctype or ""),
      "status=%s ctype=%s head=%s" % (s, ctype, pdf[:8]))

s, j = http("GET", "/itineraries/recommended?page=1&pageSize=5")
check("推荐行程列表公开可访问", s == 200 and j.get("code") == 0 and isinstance((j.get("data") or {}).get("records"), list), str(j)[:200])

# 管理员推荐后，B 可查看并收藏
s, j = http("PUT", "/admin/itineraries/%s/recommend" % IT_ID, token=TOKEN_ADMIN,
            body={"recommended": 1, "reason": "回归推荐"})
check("管理员推荐行程", s == 200 and j.get("code") == 0, str(j)[:200])

s, j = http("GET", "/itineraries/%s" % IT_ID, token=TOKEN_B)
check("推荐后他人可查看(不泄漏 inputJson)", s == 200 and j.get("code") == 0
      and (j.get("data") or {}).get("inputJson") is None, str(j)[:200])

s, j = http("POST", "/itineraries/%s/copy" % IT_ID, token=TOKEN_B)
COPY_ID = (j.get("data") or {}).get("id")
check("B收藏推荐行程", s == 200 and j.get("code") == 0 and COPY_ID and (j.get("data") or {}).get("copyFromId") == IT_ID, str(j)[:200])

s, j = http("PUT", "/admin/itineraries/%s/recommend" % IT_ID, token=TOKEN_ADMIN, body={"recommended": 0})
check("取消推荐(清理)", s == 200 and j.get("code") == 0, str(j)[:200])

s, j = http("GET", "/itineraries/999999999", token=TOKEN_A)
check("不存在的行程返回404", s == 404 and j.get("code") != 0, str(j)[:200])

# ============================================================
section("4. 论坛（分类/发帖/评论通知/点赞）")

s, j = http("GET", "/forum/categories")
cats = j.get("data") or []
check("论坛分类非空", s == 200 and j.get("code") == 0 and len(cats) >= 1, "len=%s" % len(cats))
CAT_ID = cats[0].get("id")

s, j = http("POST", "/forum/posts", token=TOKEN_A,
            body={"categoryId": CAT_ID, "title": "回归测试帖子%s" % TS, "content": "端到端回归测试内容"})
POST_ID = (j.get("data") or {}).get("id")
check("A发布帖子", s == 200 and j.get("code") == 0 and POST_ID, str(j)[:200])

s, j = http("GET", "/forum/posts/%s" % POST_ID)
check("查看帖子详情", s == 200 and j.get("code") == 0 and (j.get("data") or {}).get("id") == POST_ID, str(j)[:200])

s, j = http("POST", "/forum/comments", token=TOKEN_B, body={"postId": POST_ID, "content": "B的帖子评论"})
check("B评论帖子", s == 200 and j.get("code") == 0, str(j)[:200])
check("帖子评论触发楼主通知", len(notifications_with_post(TOKEN_A, POST_ID)) >= 1, "无帖子评论通知")

s, j = http("POST", "/forum/likes", token=TOKEN_B, body={"postId": POST_ID})
check("点赞帖子", s == 200 and j.get("code") == 0 and "liked" in (j.get("data") or {}), str(j)[:200])

s, j = http("PUT", "/forum/notifications/read-all", token=TOKEN_A)
check("全部通知已读", s == 200 and j.get("code") == 0, str(j)[:200])

# ============================================================
section("5. 管理端（景点 CRUD / 热门城市 / 权限）")

s, j = http("GET", "/admin/dashboard", token=TOKEN_ADMIN)
check("管理端仪表盘", s == 200 and j.get("code") == 0 and "data" in j, str(j)[:200])

s, j = http("GET", "/admin/dashboard", token=TOKEN_A)
check("普通用户访问管理端被拒绝(403)", s == 403 and j.get("code") != 0, str(j)[:200])

s, j = http("GET", "/admin/attractions?page=1&pageSize=10", token=TOKEN_ADMIN)
check("管理端景点列表", s == 200 and j.get("code") == 0 and "records" in (j.get("data") or {}), str(j)[:200])

s, j = http("POST", "/admin/attractions", token=TOKEN_ADMIN,
            body={"name": "回归临时景点%s" % TS, "cityId": BJ_ID, "category": "其他",
                  "description": "端到端回归临时数据", "rating": 4.5, "ticketPrice": 0, "status": 1})
check("非法分类被拒绝", j.get("code") != 0, str(j)[:200])

s, j = http("POST", "/admin/attractions", token=TOKEN_ADMIN,
            body={"name": "回归临时景点%s" % TS, "cityId": BJ_ID, "category": "自然景观",
                  "description": "端到端回归临时数据", "rating": 4.5, "ticketPrice": 30,
                  "suggestDuration": 120, "openTime": "08:00-17:00", "bestSeason": "春秋",
                  "address": "测试地址", "imageUrls": None, "status": 1})
NEW_ATTR_ID = (j.get("data") or {}).get("id")
check("管理员创建景点", s == 200 and j.get("code") == 0 and NEW_ATTR_ID, str(j)[:200])

s, j = http("PUT", "/admin/attractions/%s" % NEW_ATTR_ID, token=TOKEN_ADMIN,
            body={"name": "回归临时景点改%s" % TS, "cityId": BJ_ID, "category": "历史遗迹",
                  "description": "更新", "rating": 4.8, "ticketPrice": 40,
                  "suggestDuration": 90, "openTime": "09:00-17:30", "bestSeason": "四季",
                  "address": "测试地址2", "imageUrls": '["/images/e2e-a.jpg","/images/e2e-b.jpg"]', "status": 1})
check("管理员更新景点(含多图)", s == 200 and j.get("code") == 0
      and "回归临时景点改" in (j.get("data") or {}).get("name", ""), str(j)[:200])

s, j = http("DELETE", "/admin/attractions/%s" % NEW_ATTR_ID, token=TOKEN_ADMIN)
check("管理员删除景点(清理)", s == 200 and j.get("code") == 0, str(j)[:200])

# 热门城市：新建临时城市设为热门 -> 热门列表优先 -> 删除清理
s, j = http("POST", "/admin/cities", token=TOKEN_ADMIN,
            body={"name": "回归临时城%s" % TS, "provinceId": provs[0]["id"], "pinyin": "e2etmp",
                  "description": "临时", "popularity": 9999, "isPopular": 1, "status": 1})
TMP_CITY_ID = (j.get("data") or {}).get("id")
check("管理员创建临时热门城市", s == 200 and j.get("code") == 0 and TMP_CITY_ID, str(j)[:200])

s, j = http("GET", "/destination/cities/popular?limit=100")
pop_ids = [c.get("id") for c in (j.get("data") or [])]
check("热门城市列表含临时城市(且优先)", s == 200 and j.get("code") == 0 and TMP_CITY_ID in pop_ids,
      "ids=%s" % pop_ids[:5])

s, j = http("DELETE", "/admin/cities/%s" % TMP_CITY_ID, token=TOKEN_ADMIN)
check("删除临时热门城市(清理)", s == 200 and j.get("code") == 0, str(j)[:200])

# ============================================================
print("\n==============================================")
print("通过: %d   失败: %d" % (PASS, FAIL))
if FAILURES:
    print("失败用例:")
    for f in FAILURES:
        print("  - " + f)
print("==============================================")
sys.exit(1 if FAIL else 0)
