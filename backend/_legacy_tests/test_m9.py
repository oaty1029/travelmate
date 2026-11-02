# -*- coding: utf-8 -*-
"""M9 端到端回归：双用户全旅程 + 管理全流程 + 异常路径"""
import json, time, urllib.request, urllib.parse, urllib.error

BASE = "http://localhost:3000/api/v1"
passed = failed = 0
results = []

def req(method, path, token=None, body=None, expect=None, raw=False):
    global passed, failed
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if token: r.add_header("Authorization", "Bearer " + token)
    try:
        resp = urllib.request.urlopen(r, timeout=20)
        code = resp.status
        if raw:
            payload = resp
        else:
            payload = json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        code, payload = e.code, json.loads(e.read().decode())
    ok = expect is None or code == expect or (isinstance(expect, (list, tuple)) and code in expect)
    tag = "PASS" if ok else "FAIL"
    if ok: passed += 1
    else: failed += 1
    brief = str(payload)[:120] if not raw else f"ct={payload.headers.get('Content-Type','')}"
    results.append((tag, method, path, code, brief))
    return code, payload

def check(name, cond, detail=""):
    global passed, failed
    if cond: passed += 1; results.append(("PASS", "CHECK", name, 200, detail))
    else: failed += 1; results.append(("FAIL", "CHECK", name, 0, detail))

print("waiting for backend...")
for _ in range(30):
    try:
        req("GET", "/destination/cities?page=1&pageSize=1")
        break
    except Exception:
        time.sleep(2)

stamp = str(int(time.time()))[-6:]
A = {"email": f"m9a{stamp}@test.com", "nick": f"E2EA{stamp}", "pwd": "Test123456", "newpwd": "Test789012"}
B = {"email": f"m9b{stamp}@test.com", "nick": f"E2EB{stamp}", "pwd": "Test123456"}

# ============ 场景 A：用户全旅程 ============
# A01-A02 注册 + 登录
_, reg = req("POST", "/auth/register", body={"nickname": A["nick"], "email": A["email"], "password": A["pwd"]}, expect=200)
A["id"] = reg["data"]["user"]["id"]
_, lg = req("POST", "/auth/login", body={"account": A["email"], "password": A["pwd"]}, expect=200)
atok = lg["data"]["token"]
_, regB = req("POST", "/auth/register", body={"nickname": B["nick"], "email": B["email"], "password": B["pwd"]}, expect=200)
B["id"] = regB["data"]["user"]["id"]
_, lgB = req("POST", "/auth/login", body={"account": B["email"], "password": B["pwd"]}, expect=200)
btok = lgB["data"]["token"]

# A03-A07 目的地浏览
_, prov = req("GET", "/destination/provinces", expect=200)
check("省份列表非空", len(prov["data"]) > 0)
pid_prov = prov["data"][0]["id"]
_, cities = req("GET", "/destination/cities?page=1&pageSize=3", expect=200)
check("城市分页字段", "records" in cities["data"] and "total" in cities["data"])
c1, c2 = cities["data"]["records"][0], cities["data"]["records"][1]
_, cd = req("GET", f"/destination/cities/{c1['id']}", expect=200)
check("城市详情含景点", "attractions" in cd["data"] or "city" in cd["data"])
_, att = req("GET", f"/destination/attractions?cityId={c1['id']}&page=1&pageSize=5", expect=200)
check("景点分页", "records" in att["data"])
_, sch = req("GET", "/destination/cities/search?keyword=" + urllib.parse.quote(c1["name"]), expect=200)
check("搜索有结果", len(sch["data"]) > 0)

# A08 创建计划
_, pd = req("POST", "/plans", token=atok, body={
    "title": "M9端到端测试计划", "travelStyle": "adventure", "budgetLevel": "comfort",
    "budgetTotal": 8000, "cityIds": [c1["id"], c2["id"]],
    "startDate": "2026-10-01", "endDate": "2026-10-05",
    "companions": json.dumps({"count": 2, "remark": "E2E备注"}, ensure_ascii=False)}, expect=200)
pid = pd["data"]["id"]

# A09-A10 列表 + 详情
_, my = req("GET", "/plans", token=atok, expect=200)
check("计划列表 cityCount 字段", "cityCount" in my["data"][0])
_, det = req("GET", f"/plans/{pid}", token=atok, expect=200)
check("详情城市带名称", len(det["data"]["cities"]) == 2 and "name" in det["data"]["cities"][0])

# A11 编辑计划（改标题 + 加城市）
_, upd = req("PUT", f"/plans/{pid}", token=atok, body={
    "title": "M9端到端测试计划-已编辑", "cityIds": [c1["id"], c2["id"]]}, expect=200)
check("编辑标题生效", upd["data"]["title"] == "M9端到端测试计划-已编辑")

# A12-A13 路线生成 + 推荐
_, gen = req("POST", f"/plans/{pid}/generate-route", token=atok, body={"schemeCount": 3}, expect=200)
check("生成 3 个方案", len(gen["data"]) == 3)
_, routes = req("GET", f"/plans/{pid}/routes", token=atok, expect=200)
check("路线列表 3 条", len(routes["data"]) == 3)
req("POST", f"/plans/{pid}/set-recommend", token=atok, body={"schemeIndex": 2}, expect=200)
_, routes2 = req("GET", f"/plans/{pid}/routes", token=atok, expect=200)
rec = [r for r in routes2["data"] if r.get("isRecommended")]
check("推荐方案生效", len(rec) == 1 and rec[0]["schemeIndex"] == 2)

# A14-A16 费用 + 攻略 + PDF
_, cost = req("GET", f"/plans/{pid}/cost", token=atok, expect=200)
check("费用估算返回数据", cost["data"] is not None and len(cost["data"]) > 0)
_, guide = req("GET", f"/plans/{pid}/guide", token=atok, expect=200)
check("攻略非空", isinstance(guide["data"], str) and len(guide["data"]) > 50)
code, pdf = req("GET", f"/plans/{pid}/export?schemeIndex=0", token=atok, expect=200, raw=True)
body = pdf.read()
check("PDF 头+大小", body[:5] == b"%PDF-" and len(body) > 10000, f"{len(body)} bytes")

# A17-A19 分享
_, sh = req("POST", f"/plans/{pid}/share", token=atok, expect=200)
stok = sh["data"]["shareToken"]
_, rs = req("GET", f"/share/{urllib.parse.quote(stok)}", expect=200)
check("分享解析", rs["data"]["planId"] == pid)
_, sd = req("GET", f"/share/{urllib.parse.quote(stok)}/detail", expect=200)
check("分享详情脱敏", "userId" not in sd["data"]["plan"] and "remark" not in json.dumps(sd["data"]["plan"]["companions"], ensure_ascii=False))

# A20-A29 论坛
_, cats = req("GET", "/forum/categories", expect=200)
check("论坛分类非空", len(cats["data"]) > 0)
catid = cats["data"][0]["id"]
_, post = req("POST", "/forum/posts", token=atok, body={
    "title": "M9 E2E 帖子", "content": "端到端测试内容正文", "categoryId": catid}, expect=200)
postid = post["data"]["id"]
_, pdet = req("GET", f"/forum/posts/{postid}", expect=200)
check("帖子作者名填充", "authorName" in pdet["data"] and pdet["data"]["authorName"])
# B 用户评论 + 点赞（触发通知）
_, cm = req("POST", "/forum/comments", token=btok, body={"postId": postid, "content": "B的评论"}, expect=200)
cmid = cm["data"]["id"]
_, like = req("POST", "/forum/likes", token=btok, body={"postId": postid}, expect=200)
check("点赞返回 liked", like["data"].get("liked") is True)
_, notifs = req("GET", "/forum/notifications", token=atok, expect=200)
check("A 收到通知", len(notifs["data"]) >= 1)
if notifs["data"]:
    nid = notifs["data"][0]["id"]
    req("PUT", f"/forum/notifications/{nid}/read", token=atok, expect=200)
req("PUT", "/forum/notifications/read-all", token=atok, expect=200)
_, notifs2 = req("GET", "/forum/notifications", token=atok, expect=200)
check("全部已读", all(n.get("isRead") for n in notifs2["data"]))
req("PUT", f"/forum/posts/{postid}", token=atok, body={"title": "M9 E2E 帖子-改", "content": "编辑后的内容", "categoryId": catid}, expect=200)
req("DELETE", f"/forum/comments/{cmid}", token=btok, expect=200)

# A30-A33 个人中心 + 改密码
phone = f"139{stamp}00"
req("PUT", "/users/profile", token=atok, body={"nickname": A["nick"] + "改", "phone": phone}, expect=200)
_, prof = req("GET", "/users/profile", token=atok, expect=200)
check("资料更新生效", prof["data"]["phone"] == phone)
req("PUT", "/users/password", token=atok, body={"oldPassword": A["pwd"], "newPassword": A["newpwd"]}, expect=200)
req("POST", "/auth/login", body={"account": A["email"], "password": A["pwd"]}, expect=[400, 401])
_, lg2 = req("POST", "/auth/login", body={"account": A["email"], "password": A["newpwd"]}, expect=200)
atok = lg2["data"]["token"]

# ============ 场景 B：管理员全流程 ============
_, adm = req("POST", "/auth/login", body={"account": "admin@travelmate.com", "password": "Admin@123"}, expect=200)
adtok = adm["data"]["token"]
admin_id = adm["data"]["user"]["id"]

# B02 仪表盘
_, dash = req("GET", "/admin/dashboard", token=adtok, expect=200)
check("仪表盘字段齐全", all(k in dash["data"] for k in ["totalUsers", "totalPlans", "totalPosts", "todayNew", "trend"]))

# C03 越权：A 访问 B 的计划（B 无计划？创建 B 计划 → A 访问 403）
_, bplan = req("POST", "/plans", token=btok, body={"title": "B的计划", "cityIds": [c1["id"]]}, expect=200)
req("GET", f"/plans/{bplan['data']['id']}", token=atok, expect=403)

# B03 禁用/启用 A
req("PUT", f"/admin/users/{A['id']}/status", token=adtok, body={"status": 0}, expect=200)
req("POST", "/auth/login", body={"account": A["email"], "password": A["newpwd"]}, expect=[401, 403])
req("PUT", f"/admin/users/{A['id']}/status", token=adtok, body={"status": 1}, expect=200)
_, lg3 = req("POST", "/auth/login", body={"account": A["email"], "password": A["newpwd"]}, expect=200)
atok = lg3["data"]["token"]

# B04 城市/景点 CRUD（用完即删）
_, newcity = req("POST", "/admin/cities", token=adtok, body={
    "name": f"M9测试城{stamp}", "pinyin": "m9test", "provinceId": pid_prov,
    "description": "E2E 测试城市", "tags": '["测试"]', "popularity": 10, "status": 1}, expect=200)
ncid = newcity["data"]["id"]
req("PUT", f"/admin/cities/{ncid}", token=adtok, body={"name": f"M9测试城{stamp}改", "pinyin": "m9test2", "provinceId": pid_prov, "popularity": 20}, expect=200)
_, newatt = req("POST", "/admin/attractions", token=adtok, body={
    "name": "M9测试景点", "cityId": ncid, "category": "自然景观", "rating": 4.5,
    "ticketPrice": 60, "suggestDuration": 3, "tags": '["测试"]', "status": 1}, expect=200)
naid = newatt["data"]["id"]
req("PUT", f"/admin/attractions/{naid}", token=adtok, body={"name": "M9测试景点改", "cityId": ncid, "rating": 4.8}, expect=200)
req("DELETE", f"/admin/attractions/{naid}", token=adtok, expect=200)
req("DELETE", f"/admin/cities/{ncid}", token=adtok, expect=200)

# B05 帖子管理
req("PUT", f"/admin/posts/{postid}/status", token=adtok, body={"status": 2, "reason": "E2E隐藏"}, expect=200)
_, ap = req("GET", "/admin/posts?page=1&pageSize=50", token=adtok, expect=200)
hid = [p for p in ap["data"]["records"] if p["id"] == postid]
check("帖子已隐藏", len(hid) == 1 and hid[0]["status"] == 2)
req("PUT", f"/admin/posts/{postid}/status", token=adtok, body={"status": 1}, expect=200)
_, bh = req("POST", "/admin/posts/batch-hide", token=adtok, body={"ids": [postid], "reason": "批量E2E"}, expect=200)
check("批量隐藏计数", bh["data"]["count"] == 1)
_, bd = req("POST", "/admin/posts/batch-delete", token=adtok, body={"ids": [postid]}, expect=200)
check("批量删除计数", bd["data"]["count"] == 1)
req("GET", f"/forum/posts/{postid}", expect=404)

# B06 操作日志
_, logs = req("GET", "/admin/logs?page=1&pageSize=10&targetType=post", token=adtok, expect=200)
check("日志筛选", len(logs["data"]["records"]) > 0 and all(r["targetType"] == "post" for r in logs["data"]["records"]))

# B07 受保护账号
req("PUT", f"/admin/users/{admin_id}/status", token=adtok, body={"status": 0}, expect=400)
req("DELETE", f"/admin/users/{admin_id}", token=adtok, expect=400)

# B08 删除 B 用户（级联）+ 登录验证
req("DELETE", f"/admin/users/{B['id']}", token=adtok, expect=200)
req("POST", "/auth/login", body={"account": B["email"], "password": B["pwd"]}, expect=[400, 401])

# ============ 场景 C：异常路径 ============
req("GET", "/plans", expect=401)
req("GET", "/admin/users", expect=401)
req("GET", "/destination/cities/search?keyword=" + urllib.parse.quote("长" * 60), expect=400)
req("POST", "/plans", token=atok, body={"title": "无城市计划"}, expect=400)
req("GET", "/share/deadbeefdeadbeef/detail", expect=404)
req("POST", "/forum/comments", token=atok, body={"postId": 999999, "content": "x"}, expect=404)
req("POST", "/auth/register", body={"nickname": A["nick"] + "重复", "email": A["email"], "password": "Test123456"}, expect=[400, 409])

# A 清理：删除自己的计划 → 分享链接失效
req("DELETE", f"/plans/{pid}", token=atok, expect=200)
req("GET", f"/share/{urllib.parse.quote(stok)}/detail", expect=404)

print(f"\n===== RESULT: {passed} passed, {failed} failed =====")
for tag, m, p, c, msg in results:
    print(f"[{tag}] {m} {p} -> {c} {msg}")
