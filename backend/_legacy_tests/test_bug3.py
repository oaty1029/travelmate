# -*- coding: utf-8 -*-
"""Bug3 回归测试：点击"重新生成方案"应产出与旧方案不同的路线（算法不再完全确定）

验证点：连续两次 generate-route 后，至少一个方案的 routeOrder 发生变化。
背景：旧实现 TspSolver 固定从第 0 个城市最近邻出发且无随机，重新生成结果恒等。
"""
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
        resp = urllib.request.urlopen(r, timeout=20)
        code = resp.status
        payload = json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        code, payload = e.code, json.loads(e.read().decode())
    ok = expect is None or code == expect
    tag = "PASS" if ok else "FAIL"
    if ok: passed += 1
    else: failed += 1
    results.append((tag, method, path, code, str(payload)[:120]))
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
U = {"email": f"bug3{stamp}@test.com", "nick": f"Bug3{stamp}", "pwd": "Test123456"}

_, reg = req("POST", "/auth/register", body={"nickname": U["nick"], "email": U["email"], "password": U["pwd"]}, expect=200)
_, lg = req("POST", "/auth/login", body={"account": U["email"], "password": U["pwd"]}, expect=200)
tok = lg["data"]["token"]

_, cities = req("GET", "/destination/cities?page=1&pageSize=5", expect=200)
ids = [c["id"] for c in cities["data"]["records"]]
check("取得 5 个城市", len(ids) == 5, str(ids))

_, pd = req("POST", "/plans", token=tok, body={
    "title": "Bug3重新生成测试", "travelStyle": "leisure", "budgetLevel": "comfort",
    "budgetTotal": 6000, "cityIds": ids,
    "startDate": "2026-10-01", "endDate": "2026-10-06"}, expect=200)
pid = pd["data"]["id"]

def gen_and_fetch():
    req("POST", f"/plans/{pid}/generate-route", token=tok, body={"schemeCount": 3}, expect=200)
    _, routes = req("GET", f"/plans/{pid}/routes", token=tok, expect=200)
    return {s.get("schemeIndex"): s.get("routeOrder") for s in routes["data"]}

o1 = gen_and_fetch()
o2 = gen_and_fetch()
o3 = gen_and_fetch()
check("每次均生成 3 个方案", len(o1) == 3 and len(o2) == 3 and len(o3) == 3)

# 至少一个方案在三代间发生变化（时间/体验方案随机化，费用方案可能稳定）
differ = any(
    (o1.get(i) != o2.get(i)) or (o2.get(i) != o3.get(i))
    for i in (1, 2, 3)
)
check("重新生成产出不同路线", differ,
      f"gen1={o1} gen2={o2} gen3={o3}")

req("DELETE", f"/plans/{pid}", token=tok, expect=200)

print("\n===== Bug3 结果 =====")
for r in results:
    print(" ".join(str(x) for x in r))
print(f"\nPASS={passed} FAIL={failed}")
exit(1 if failed else 0)
