# -*- coding: utf-8 -*-
"""M8 自测：分享脱敏、权限守卫、回归"""
import json, time, urllib.request, urllib.parse, urllib.error

BASE = "http://localhost:3000/api/v1"
passed = failed = 0
results = []

def req(method, path, token=None, body=None, expect=None):
    global passed, failed
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if token: r.add_header("Authorization", "Bearer " + token)
    try:
        resp = urllib.request.urlopen(r, timeout=15)
        code, payload = resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        code, payload = e.code, json.loads(e.read().decode())
    ok = expect is None or code == expect
    tag = "PASS" if ok else "FAIL"
    if ok: passed += 1
    else: failed += 1
    results.append((tag, method, path, code, str(payload)[:180]))
    return code, payload

print("waiting for backend...")
for _ in range(30):
    try:
        req("GET", "/destination/cities?page=1&pageSize=1")
        break
    except Exception:
        time.sleep(2)

# T0/T1: 注册新用户 + 登录管理员
stamp = str(int(time.time()))
email = f"m8u{stamp}@test.com"
code, reg = req("POST", "/auth/register", body={
    "nickname": f"M8测试{stamp[-6:]}", "email": email, "password": "Test123456"})
utok = reg["data"]["token"] if code == 200 else None

code, adm = req("POST", "/auth/login", body={
    "account": "admin@travelmate.com", "password": "Admin@123"})
atok = adm["data"]["token"] if code == 200 else None

# T2: 取一个城市
_, cityd = req("GET", "/destination/cities?page=1&pageSize=1")
cid = cityd["data"]["records"][0]["id"]

# T3: 创建计划（带私密备注）
_, pd = req("POST", "/plans", token=utok, body={
    "title": "M8脱敏测试计划", "cityIds": [cid],
    "companions": json.dumps({"count": 2, "remark": "私密备注不要泄露"}, ensure_ascii=False)})
pid = pd["data"]["id"]

# T4: 创建分享链接
_, sh = req("POST", f"/plans/{pid}/share", token=utok)
token = sh["data"]["shareToken"]
print("shareToken:", token)

# T5: 公开解析分享 token（无登录）
code, rs = req("GET", f"/share/{urllib.parse.quote(token)}", expect=200)
assert rs["data"]["planId"] == pid, "resolve planId mismatch"

# T6: 公开分享详情 — 脱敏断言
code, sd = req("GET", f"/share/{urllib.parse.quote(token)}/detail", expect=200)
plan = sd["data"]["plan"]
assert plan["title"] == "M8脱敏测试计划", "title mismatch"
assert "userId" not in plan, "userId 泄漏!"
comp = json.loads(plan["companions"])
assert comp == {"count": 2}, f"companions 泄漏备注: {comp}"
results.append(("PASS" if True else "FAIL", "CHECK", "脱敏", 200, f"companions={comp}, keys={sorted(plan.keys())}"))
passed += 1

# T7: 无效 token → 404
req("GET", "/share/nonexistenttoken123/detail", expect=404)

# T8-T12: 越权访问他人计划（管理员访问普通用户计划）
req("GET", f"/plans/{pid}", token=atok, expect=403)
req("GET", f"/plans/{pid}/cost", token=atok, expect=403)
req("GET", f"/plans/{pid}/routes", token=atok, expect=403)
req("POST", f"/plans/{pid}/set-recommend", token=atok, body={"schemeIndex": 1}, expect=403)
req("PUT", f"/plans/{pid}", token=atok, body={"title": "越权改名"}, expect=403)

# T13: 本人访问回归 200（cost 无路线时业务 400，只要非 401/403 即通过）
req("GET", f"/plans/{pid}", token=utok, expect=200)
req("GET", f"/plans/{pid}/cost", token=utok, expect=400)

# T14: 管理接口回归（admin 用户列表仍返回原始手机号/邮箱，前端展示层脱敏）
code, ul = req("GET", "/admin/users?page=1&pageSize=5", token=atok, expect=200)
first = ul["data"]["records"][0]
assert "phone" in first and "email" in first, "admin 列表字段缺失"

# T15: 未登录访问分享详情不需要 token（已覆盖 T6），管理接口无 token → 401
req("GET", "/admin/users", expect=401)

print(f"\n===== RESULT: {passed} passed, {failed} failed =====")
for tag, m, p, c, msg in results:
    print(f"[{tag}] {m} {p} -> {c} {msg}")
