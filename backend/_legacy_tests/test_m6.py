# -*- coding: utf-8 -*-
"""M6 管理后台后端自测"""
import json, urllib.request, urllib.error, urllib.parse, sys, time

BASE = "http://localhost:3000/api/v1"
results = []

def req(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            return resp.status, resp.read(), None
    except urllib.error.HTTPError as e:
        return e.code, e.read(), None

def check(name, cond, extra=""):
    results.append((name, cond))
    print(("PASS" if cond else "FAIL"), name, extra)

def j(b):
    return json.loads(b.decode("utf-8"))

# ---- 等待后端 ----
for i in range(30):
    try:
        s, b, _ = req("GET", "/destination/provinces")
        if s == 200: break
    except Exception: pass
    time.sleep(2)
else:
    print("backend not ready"); sys.exit(1)

ts = int(time.time())

# ---- 登录/注册 ----
s, b, _ = req("POST", "/auth/login", {"account": "admin@travelmate.com", "password": "Admin@123"})
admin_token = j(b)["data"]["token"]
s, b, _ = req("POST", "/auth/register", {
    "email": f"m6vic{ts}@test.com", "password": "Test123456", "nickname": f"M6受害者{ts}", "phone": ""})
victim_token = j(b)["data"]["token"]
victim_id = j(b)["data"]["user"]["id"]
s, b, _ = req("POST", "/auth/register", {
    "email": f"m6third{ts}@test.com", "password": "Test123456", "nickname": f"M6第三方{ts}", "phone": ""})
third_token = j(b)["data"]["token"]
check("T0 登录/注册", bool(admin_token) and bool(victim_token) and bool(third_token))

# ---- 权限 ----
s, b, _ = req("GET", "/admin/users", None, third_token)
check("T1 非管理员403", s == 403, f"got {s}")

# ---- 仪表盘 ----
s, b, _ = req("GET", "/admin/dashboard", None, admin_token)
d = j(b)["data"]
check("T2 仪表盘字段", s == 200 and "totalUsers" in d and "totalPlans" in d
      and "todayNew" in d and isinstance(d.get("trend"), list) and len(d.get("trend", [])) == 7,
      f"keys={list(d.keys())}")

# ---- 城市列表 ----
s, b, _ = req("GET", "/admin/cities?page=1&pageSize=5", None, admin_token)
recs = j(b)["data"]["records"]
check("T3 城市列表带省份名", s == 200 and len(recs) > 0 and any(r.get("provinceName") for r in recs))

# ---- 省份 ----
s, b, _ = req("GET", "/destination/provinces")
provs = j(b)["data"]
pid = provs[0]["id"]
s, b, _ = req("GET", "/destination/cities?page=1&pageSize=1")
any_city = j(b)["data"]["records"][0]["id"]

# ---- 城市创建/更新校验 ----
s, b, _ = req("POST", "/admin/cities", {"name": "", "provinceId": pid}, admin_token)
check("T4 空城市名400", s == 400, f"got {s}")
s, b, _ = req("POST", "/admin/cities", {"name": "测试城", "provinceId": 999999}, admin_token)
check("T5 省份不存在404", s == 404, f"got {s}")
s, b, _ = req("POST", "/admin/cities", {"name": "测试城", "provinceId": pid, "tags": "not-json"}, admin_token)
check("T6 非法tags JSON 400", s == 400, f"got {s}")
s, b, _ = req("POST", "/admin/cities", {
    "name": f"M6测试城{ts}", "pinyin": "m6cs", "provinceId": pid,
    "description": "测试用城市", "bestSeason": "全年", "popularity": 66,
    "tags": json.dumps(["测试", "临时"], ensure_ascii=False)}, admin_token)
city_id = j(b)["data"]["id"] if s == 200 else None
check("T7 创建城市", s == 200 and city_id is not None, f"cityId={city_id}")

s, b, _ = req("PUT", f"/admin/cities/{city_id}", {
    "name": f"M6测试城改{ts}", "pinyin": "m6cs2", "provinceId": pid,
    "description": "改", "popularity": 77, "tags": json.dumps(["改"], ensure_ascii=False)}, admin_token)
check("T8 更新城市", s == 200 and j(b)["data"]["name"] == f"M6测试城改{ts}", f"got {s}")
s, b, _ = req("PUT", f"/admin/cities/{city_id}", {
    "name": "x", "provinceId": pid, "popularity": -1}, admin_token)
check("T9 非法热度400", s == 400, f"got {s}")

# ---- 景点校验 ----
s, b, _ = req("POST", "/admin/attractions", {"name": "无城市景点"}, admin_token)
check("T10 缺cityId 400", s == 400, f"got {s}")
s, b, _ = req("POST", "/admin/attractions", {"name": "x", "cityId": 999999}, admin_token)
check("T11 城市不存在404", s == 404, f"got {s}")
s, b, _ = req("POST", "/admin/attractions", {"name": "x", "cityId": city_id, "category": "不存在的分类"}, admin_token)
check("T12 非法分类400", s == 400, f"got {s}")
s, b, _ = req("POST", "/admin/attractions", {"name": "x", "cityId": city_id, "rating": 9}, admin_token)
check("T13 评分超范围400", s == 400, f"got {s}")
s, b, _ = req("POST", "/admin/attractions", {
    "name": f"M6测试景点{ts}", "cityId": city_id, "category": "历史遗迹",
    "rating": 4.5, "ticketPrice": 60, "suggestDuration": 3,
    "openTime": "8:00-18:00", "tags": json.dumps(["古迹"], ensure_ascii=False)}, admin_token)
att_id = j(b)["data"]["id"] if s == 200 else None
check("T14 创建景点", s == 200 and att_id is not None, f"attId={att_id}")
s, b, _ = req("PUT", f"/admin/attractions/{att_id}", {
    "name": f"M6测试景点改{ts}", "cityId": city_id, "category": "博物馆",
    "rating": 4.0, "ticketPrice": 0, "suggestDuration": 2}, admin_token)
d = j(b)["data"]
check("T15 更新景点", s == 200 and d["name"] == f"M6测试景点改{ts}" and d["suggestDuration"] == 2, f"got {s}")
s, b, _ = req("GET", f"/admin/attractions?cityId={city_id}&page=1&pageSize=10", None, admin_token)
check("T16 按城市筛选景点", s == 200 and any(r["id"] == att_id for r in j(b)["data"]["records"]))

# ---- 帖子管理 ----
s, b, _ = req("GET", "/admin/posts?page=1&pageSize=5", None, admin_token)
precs = j(b)["data"]["records"]
check("T17 管理端帖子带作者名", s == 200 and (len(precs) == 0 or any(r.get("authorName") for r in precs)))

s, b, _ = req("POST", "/forum/posts", {"categoryId": 1, "title": f"M6被隐藏帖{ts}", "content": "内容"}, victim_token)
p1 = j(b)["data"]["id"] if s == 200 else None
check("T18 受害者发帖", s == 200 and p1 is not None)

s, b, _ = req("PUT", f"/admin/posts/{p1}/status", {"status": 2, "reason": "违规内容测试"}, admin_token)
check("T19 隐藏帖子", s == 200 and j(b)["data"]["status"] == 2, f"got {s}")
s, b, _ = req("GET", f"/forum/posts/{p1}")
check("T20 隐藏后公开不可见", s == 404, f"got {s}")
s, b, _ = req("PUT", f"/admin/posts/{p1}/status", {"status": 1}, admin_token)
check("T21 恢复发布", s == 200, f"got {s}")
s, b, _ = req("GET", f"/forum/posts/{p1}")
check("T22 恢复后公开可见", s == 200, f"got {s}")
s, b, _ = req("PUT", f"/admin/posts/{p1}/status", {"status": 3}, admin_token)
check("T23 非法状态400", s == 400, f"got {s}")

s, b, _ = req("POST", "/forum/posts", {"categoryId": 1, "title": f"M6批量帖A{ts}", "content": "a"}, victim_token)
p2 = j(b)["data"]["id"]
s, b, _ = req("POST", "/forum/posts", {"categoryId": 1, "title": f"M6批量帖B{ts}", "content": "b"}, victim_token)
p3 = j(b)["data"]["id"]
s, b, _ = req("POST", "/admin/posts/batch-hide", {"ids": [p2, p3], "reason": "批量隐藏测试"}, admin_token)
check("T24 批量隐藏", s == 200 and j(b)["data"]["count"] == 2, f"got {s}")
s, b, _ = req("GET", f"/forum/posts/{p2}")
check("T25 批量隐藏生效", s == 404, f"got {s}")
s, b, _ = req("POST", "/admin/posts/batch-delete", {"ids": [p2, p3]}, admin_token)
check("T26 批量删除", s == 200 and j(b)["data"]["count"] == 2, f"got {s}")
s, b, _ = req("POST", "/admin/posts/batch-delete", {"ids": []}, admin_token)
check("T27 空批量400", s == 400, f"got {s}")

# ---- 用户状态 ----
s, b, _ = req("PUT", f"/admin/users/{victim_id}/status", {"status": 0}, admin_token)
check("T28 禁用用户", s == 200 and j(b)["data"]["status"] == 0, f"got {s}")
s, b, _ = req("POST", "/auth/login", {"account": f"m6vic{ts}@test.com", "password": "Test123456"})
check("T29 禁用后无法登录", s != 200, f"got {s}")
s, b, _ = req("PUT", f"/admin/users/{victim_id}/status", {"status": 1}, admin_token)
check("T30 恢复启用", s == 200, f"got {s}")
s, b, _ = req("POST", "/auth/login", {"account": f"m6vic{ts}@test.com", "password": "Test123456"})
victim_token = j(b)["data"]["token"]
check("T31 恢复后可登录", s == 200, f"got {s}")

s, b, _ = req("GET", "/admin/users?page=1&pageSize=100", None, admin_token)
admin_roles = [r for r in j(b)["data"]["records"] if r["role"] != "user" and r["id"] != 6]
s, b, _ = req("PUT", "/admin/users/6/status", {"status": 0}, admin_token)
check("T32 不能操作自己", s == 400, f"got {s}")
if admin_roles:
    s, b, _ = req("DELETE", f"/admin/users/{admin_roles[0]['id']}", None, admin_token)
    check("T33 不能删管理员账号", s == 400, f"got {s}")
else:
    check("T33 不能删管理员账号", False, "no admin role found")

# ---- 删除用户级联 ----
s, b, _ = req("POST", "/plans", {
    "title": f"M6级联计划{ts}", "travelStyle": "leisure", "budgetLevel": "economy",
    "startDate": "2026-10-01", "endDate": "2026-10-05",
    "cityIds": [any_city], "budgetTotal": 3000}, victim_token)
plan_id = j(b)["data"]["id"] if s == 200 else None
check("T34 受害者建计划", s == 200 and plan_id is not None, f"planId={plan_id}")

s, b, _ = req("POST", "/forum/posts", {"categoryId": 1, "title": f"M6级联帖{ts}", "content": "x"}, victim_token)
p4 = j(b)["data"]["id"]
s, b, _ = req("POST", "/forum/comments", {"postId": p4, "content": "第三方评论"}, third_token)
check("T35 级联准备", p4 is not None and s == 200, f"got {s}")

s, b, _ = req("DELETE", f"/admin/users/{victim_id}", None, admin_token)
check("T36 删除用户", s == 200, f"got {s}")
s, b, _ = req("GET", f"/admin/users/{victim_id}", None, admin_token)
check("T37 删除后用户404", s == 404, f"got {s}")
s, b, _ = req("POST", "/auth/login", {"account": f"m6vic{ts}@test.com", "password": "Test123456"})
check("T38 删除后无法登录", s != 200, f"got {s}")
s, b, _ = req("GET", f"/forum/posts/{p4}")
check("T39 用户帖子级联删除", s == 404, f"got {s}")

# ---- 操作日志 ----
s, b, _ = req("GET", "/admin/logs?page=1&pageSize=10", None, admin_token)
logs = j(b)["data"]
check("T40 操作日志有记录", s == 200 and logs["total"] > 0, f"total={logs.get('total')}")
s, b, _ = req("GET", "/admin/logs?targetType=post&page=1&pageSize=50", None, admin_token)
lrecs = j(b)["data"]["records"]
check("T41 日志按类型筛选", s == 200 and all(r["targetType"] == "post" for r in lrecs), f"n={len(lrecs)}")

# ---- 删除城市级联景点 ----
s, b, _ = req("GET", f"/admin/attractions?cityId={city_id}&page=1&pageSize=10", None, admin_token)
has_att = len(j(b)["data"]["records"]) > 0
s, b, _ = req("DELETE", f"/admin/cities/{city_id}", None, admin_token)
check("T42 删除城市", s == 200, f"got {s}")
s, b, _ = req("GET", f"/admin/attractions?cityId={city_id}&page=1&pageSize=10", None, admin_token)
check("T43 景点级联删除", s == 200 and len(j(b)["data"]["records"]) == 0, f"had_att={has_att}")

# ---- 汇总 ----
passed = sum(1 for _, c in results if c)
print(f"\n===== M6 SUMMARY: {passed}/{len(results)} passed =====")
for name, cond in results:
    if not cond:
        print("FAILED:", name)
