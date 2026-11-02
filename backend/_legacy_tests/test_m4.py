# -*- coding: utf-8 -*-
"""M4 计划模块后端自测"""
import json, urllib.request, urllib.error, sys, time

BASE = "http://localhost:3000/api/v1"
results = []

def req(method, path, body=None, token=None, raw=False):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            b = resp.read()
            ct = resp.headers.get("Content-Type", "")
            return resp.status, b, ct
    except urllib.error.HTTPError as e:
        return e.code, e.read(), e.headers.get("Content-Type", "")

def check(name, cond, extra=""):
    results.append((name, cond))
    print(("PASS" if cond else "FAIL"), name, extra)

# ---- 等待后端就绪 ----
for i in range(30):
    try:
        s, b, _ = req("GET", "/destination/provinces")
        if s == 200:
            break
    except Exception:
        pass
    time.sleep(2)
else:
    print("backend not ready"); sys.exit(1)

# ---- 登录 ----
s, b, _ = req("POST", "/auth/login", {"account": "admin@travelmate.com", "password": "Admin@123"})
d = json.loads(b.decode("utf-8"))
token = d["data"]["token"]
check("T0 管理员登录", s == 200 and token is not None)

# ---- 取城市 ----
s, b, _ = req("GET", "/destination/cities", None, token)
cities = json.loads(b.decode("utf-8"))["data"]["records"]
c1, c2, c3 = cities[0]["id"], cities[1]["id"], cities[2]["id"]
bad_city = 999999
print("cities:", c1, c2, c3)

# ---- 创建计划 ----
base_plan = {
    "title": "M4测试计划",
    "travelStyle": "leisure",
    "budgetLevel": "comfort",
    "budgetTotal": 8000,
    "startDate": "2026-10-01",
    "endDate": "2026-10-07",
    "companions": json.dumps({"count": 2, "remark": "测试"}),
    "cityIds": [c1, c2, c3]
}

# T1 正常创建
s, b, _ = req("POST", "/plans", base_plan, token)
d = json.loads(b.decode("utf-8"))
plan_id = d["data"]["id"] if s == 200 and d.get("data") else None
check("T1 创建计划成功", s == 200 and plan_id is not None and d["data"]["status"] == "draft", f"planId={plan_id}")

# T2 标题为空 → 400
p = dict(base_plan); p["title"] = "   "
s, b, _ = req("POST", "/plans", p, token)
check("T2 空标题400", s == 400, f"got {s}")

# T3 cityIds 缺失 → 400
p = dict(base_plan); del p["cityIds"]
s, b, _ = req("POST", "/plans", p, token)
check("T3 缺cityIds 400", s == 400, f"got {s}")

# T4 cityIds 含不存在城市 → 400
p = dict(base_plan); p["cityIds"] = [c1, bad_city]
s, b, _ = req("POST", "/plans", p, token)
check("T4 不存在城市400", s == 400, f"got {s}")

# T5 开始日期晚于结束日期 → 400
p = dict(base_plan); p["startDate"] = "2026-10-10"
s, b, _ = req("POST", "/plans", p, token)
check("T5 日期范围400", s == 400, f"got {s}")

# T6 非法 travelStyle → 400
p = dict(base_plan); p["travelStyle"] = "fancy"
s, b, _ = req("POST", "/plans", p, token)
check("T6 非法风格400", s == 400, f"got {s}")

# T7 负预算 → 400
p = dict(base_plan); p["budgetTotal"] = -100
s, b, _ = req("POST", "/plans", p, token)
check("T7 负预算400", s == 400, f"got {s}")

# T8 companions 非法 JSON → 400
p = dict(base_plan); p["companions"] = "not json"
s, b, _ = req("POST", "/plans", p, token)
check("T8 非法companions 400", s == 400, f"got {s}")

# T9 城市去重：cityIds=[c1,c1,c2] → 详情应只有2个城市
p = dict(base_plan); p["title"] = "去重测试"; p["cityIds"] = [c1, c1, c2]
s, b, _ = req("POST", "/plans", p, token)
d = json.loads(b.decode("utf-8"))
dedup_id = d["data"]["id"]
s2, b2, _ = req("GET", f"/plans/{dedup_id}", None, token)
dd = json.loads(b2.decode("utf-8"))["data"]
check("T9 城市去重", s == 200 and len(dd["cities"]) == 2, f"cities={len(dd['cities'])}")
req("DELETE", f"/plans/{dedup_id}", None, token)

# ---- 列表 ----
s, b, _ = req("GET", "/plans", None, token)
plans = json.loads(b.decode("utf-8"))["data"]
mine = [x for x in plans if x["id"] == plan_id][0]
check("T10 列表含cityCount", mine.get("cityCount") == 3, f"cityCount={mine.get('cityCount')}")

# ---- 详情 ----
s, b, _ = req("GET", f"/plans/{plan_id}", None, token)
d = json.loads(b.decode("utf-8"))["data"]
check("T11 详情结构plan/cities/routes", s == 200 and "plan" in d and "cities" in d and "routes" in d)
check("T12 城市带名称", len(d["cities"]) == 3 and d["cities"][0].get("name"), f"c0={d['cities'][0] if d['cities'] else None}")
check("T13 plan字段契约", d["plan"].get("travelStyle") == "leisure" and d["plan"].get("budgetTotal") == 8000)

# ---- 更新 ----
s, b, _ = req("PUT", f"/plans/{plan_id}", {"title": "M4测试计划改"}, token)
check("T14 更新标题", s == 200 and json.loads(b.decode("utf-8"))["data"]["title"] == "M4测试计划改", f"got {s}")

s, b, _ = req("PUT", f"/plans/{plan_id}", {"title": "  "}, token)
check("T15 更新空标题400", s == 400, f"got {s}")

s, b, _ = req("PUT", f"/plans/{plan_id}", {"status": "xxx"}, token)
check("T16 非法状态400", s == 400, f"got {s}")

s, b, _ = req("PUT", f"/plans/{plan_id}", {"startDate": "2026-11-01"}, token)
check("T17 更新后日期范围400", s == 400, f"got {s}")

# 清空城市
s, b, _ = req("PUT", f"/plans/{plan_id}", {"cityIds": []}, token)
check("T18 清空城市", s == 200, f"got {s}")
s, b, _ = req("GET", f"/plans/{plan_id}", None, token)
dd = json.loads(b.decode("utf-8"))["data"]
check("T19 清空后城市数0", len(dd["cities"]) == 0, f"cities={len(dd['cities'])}")

# 回填城市
s, b, _ = req("PUT", f"/plans/{plan_id}", {"cityIds": [c2, c1]}, token)
s, b, _ = req("GET", f"/plans/{plan_id}", None, token)
dd = json.loads(b.decode("utf-8"))["data"]
check("T20 回填城市顺序", [x["id"] for x in dd["cities"]] == [c2, c1], f"cities={[x['id'] for x in dd['cities']]}")

# ---- 路线生成 ----
s, b, _ = req("POST", f"/plans/{plan_id}/generate-route", None, token)
routes = json.loads(b.decode("utf-8"))["data"]
check("T21 生成路线3方案", s == 200 and len(routes) == 3, f"routes={len(routes)}")

s, b, _ = req("POST", f"/plans/{plan_id}/generate-route", {"schemeCount": 0}, token)
check("T22 方案数0→400", s == 400, f"got {s}")
s, b, _ = req("POST", f"/plans/{plan_id}/generate-route", {"schemeCount": 6}, token)
check("T23 方案数6→400", s == 400, f"got {s}")

s, b, _ = req("GET", f"/plans/{plan_id}/routes", None, token)
check("T24 获取方案列表", s == 200 and len(json.loads(b.decode("utf-8"))["data"]) == 3, f"got {s}")

# 费用
s, b, _ = req("GET", f"/plans/{plan_id}/cost", None, token)
check("T25 费用估算", s == 200 and "data" in json.loads(b.decode("utf-8")), f"got {s}")

# 攻略
s, b, _ = req("GET", f"/plans/{plan_id}/guide", None, token)
check("T26 攻略生成", s == 200, f"got {s}")

# 推荐方案
s, b, _ = req("POST", f"/plans/{plan_id}/set-recommend", {"schemeIndex": 1}, token)
check("T27 设置推荐", s == 200, f"got {s}")
s, b, _ = req("POST", f"/plans/{plan_id}/set-recommend", {"schemeIndex": 99}, token)
check("T28 非法方案索引400", s == 400, f"got {s}")

# PDF 导出
s, b, ct = req("GET", f"/plans/{plan_id}/export", None, token, raw=True)
check("T29 PDF导出", s == 200 and b[:4] == b"%PDF", f"got {s} len={len(b)}")

# 分享
s, b, _ = req("POST", f"/plans/{plan_id}/share", None, token)
share_token = json.loads(b.decode("utf-8"))["data"]["shareToken"]
check("T30 创建分享链接", s == 200 and share_token, f"token={share_token[:8]}...")

s, b, _ = req("GET", f"/share/{share_token}")
check("T31 分享解析", s == 200 and json.loads(b.decode("utf-8"))["data"]["planId"] == plan_id, f"got {s}")

s, b, _ = req("GET", f"/share/{share_token}/detail")
sd = json.loads(b.decode("utf-8"))["data"]
check("T32 分享详情(公开)", s == 200 and sd.get("plan") and len(sd.get("cities", [])) == 2, f"got {s}")

# ---- 权限 ----
ts = int(time.time())
s, b, _ = req("POST", "/auth/register", {
    "email": f"m4user{ts}@test.com", "password": "Test123456",
    "nickname": f"M4用户{ts}", "phone": ""})
d2 = json.loads(b.decode("utf-8"))
token2 = d2.get("data", {}).get("token")
s, b, _ = req("GET", f"/plans/{plan_id}", None, token2)
check("T33 他人访问403", s == 403, f"got {s}")
s, b, _ = req("DELETE", f"/plans/{plan_id}", None, token2)
check("T34 他人删除403", s == 403, f"got {s}")

# ---- 删除级联 ----
s, b, _ = req("DELETE", f"/plans/{plan_id}", None, token)
check("T35 删除计划", s == 200, f"got {s}")
s, b, _ = req("GET", f"/plans/{plan_id}", None, token)
check("T36 删除后详情404", s == 404, f"got {s}")
s, b, _ = req("GET", f"/share/{share_token}")
check("T37 删除后分享失效404", s == 404, f"got {s}")

# ---- 汇总 ----
passed = sum(1 for _, c in results if c)
print(f"\n===== M4 SUMMARY: {passed}/{len(results)} passed =====")
for name, cond in results:
    if not cond:
        print("FAILED:", name)
