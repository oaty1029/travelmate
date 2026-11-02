# -*- coding: utf-8 -*-
"""Bug4 回归测试：计划状态流转（草稿→已规划→已完成）+ 非法状态拒绝

前端修复点：PlanEdit 增加状态选择并提交 status（后端本已支持）。
此脚本锁定后端状态契约，防止回归；UI 部分配合手动验证（见交付说明）。
"""
import json, time, urllib.request, urllib.error

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
        resp = urllib.request.urlopen(r, timeout=20)
        code = resp.status
        payload = json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        code, payload = e.code, json.loads(e.read().decode())
    ok = expect is None or code == expect
    tag = "PASS" if ok else "FAIL"
    if ok: passed += 1
    else: failed += 1
    results.append((tag, method, path, code, str(payload)[:100]))
    return code, payload

def check(name, cond, detail=""):
    global passed, failed
    if cond: passed += 1; results.append(("PASS", "CHECK", name, 200, detail))
    else: failed += 1; results.append(("FAIL", "CHECK", name, 0, detail))

print("waiting for backend...")
for _ in range(30):
    try:
        req("GET", "/destination/provinces", expect=200)
        break
    except Exception:
        time.sleep(2)

stamp = str(int(time.time()))[-6:]
U = {"email": f"bug4{stamp}@test.com", "nick": f"Bug4{stamp}", "pwd": "Test123456"}

req("POST", "/auth/register", body={"nickname": U["nick"], "email": U["email"], "password": U["pwd"]}, expect=200)
_, lg = req("POST", "/auth/login", body={"account": U["email"], "password": U["pwd"]}, expect=200)
tok = lg["data"]["token"]

_, cities = req("GET", "/destination/cities?page=1&pageSize=2", expect=200)
ids = [c["id"] for c in cities["data"]["records"]]

_, pd = req("POST", "/plans", token=tok, body={
    "title": "Bug4状态流转测试", "cityIds": ids,
    "startDate": "2026-10-01", "endDate": "2026-10-03"}, expect=200)
pid = pd["data"]["id"]
check("新建计划默认草稿", pd["data"]["status"] == "draft", f"got {pd['data']['status']}")

_, u1 = req("PUT", f"/plans/{pid}", token=tok, body={"status": "planned"}, expect=200)
check("状态改为已规划", u1["data"]["status"] == "planned", f"got {u1['data']['status']}")

_, det = req("GET", f"/plans/{pid}", token=tok, expect=200)
check("详情状态同步为已规划", det["data"]["plan"]["status"] == "planned", f"got {det['data']['plan']['status']}")

_, u2 = req("PUT", f"/plans/{pid}", token=tok, body={"status": "completed"}, expect=200)
check("状态改为已完成", u2["data"]["status"] == "completed", f"got {u2['data']['status']}")

req("PUT", f"/plans/{pid}", token=tok, body={"status": "invalid"}, expect=400)
check("非法状态被拒绝(400)", True)

req("DELETE", f"/plans/{pid}", token=tok, expect=200)

print("\n===== Bug4 结果 =====")
for r in results:
    print(" ".join(str(x) for x in r))
print(f"\nPASS={passed} FAIL={failed}")
exit(1 if failed else 0)
