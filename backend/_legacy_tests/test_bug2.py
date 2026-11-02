# -*- coding: utf-8 -*-
"""Bug2 回归测试：路线方案费用口径一致性 + 费用最优不高于时间最优 + 预算档次透传(H1)

验证点：
1. 每个方案的 totalCost 与 costBreakdown.total 一致（货币口径，不再是 TSP 分钟/元混合值）
2. 费用最优方案 totalCost <= 时间最优方案 totalCost（修复"费用最优反而更贵"）
3. costBreakdown.budgetLevel 透传计划预算档次（修复硬编码 comfort，H1）
4. totalDistance 为按路线顺序 distance_km 求和（>0 且不再等于 totalCost）
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
U = {"email": f"bug2{stamp}@test.com", "nick": f"Bug2{stamp}", "pwd": "Test123456"}

_, reg = req("POST", "/auth/register", body={"nickname": U["nick"], "email": U["email"], "password": U["pwd"]}, expect=200)
_, lg = req("POST", "/auth/login", body={"account": U["email"], "password": U["pwd"]}, expect=200)
tok = lg["data"]["token"]

def get_city(name):
    _, s = req("GET", "/destination/cities/search?keyword=" + urllib.parse.quote(name), expect=200)
    return s["data"][0]["id"]

bj, cd, xa = get_city("北京"), get_city("成都"), get_city("西安")

# 用 budgetLevel=economy 同时验证 H1（旧代码会硬编码 comfort）
_, pd = req("POST", "/plans", token=tok, body={
    "title": "Bug2费用口径测试", "travelStyle": "cultural", "budgetLevel": "economy",
    "budgetTotal": 5000, "cityIds": [bj, cd, xa],
    "startDate": "2026-10-01", "endDate": "2026-10-05"}, expect=200)
pid = pd["data"]["id"]

req("POST", f"/plans/{pid}/generate-route", token=tok, body={"schemeCount": 3}, expect=200)
_, routes = req("GET", f"/plans/{pid}/routes", token=tok, expect=200)
schemes = {s.get("schemeIndex"): s for s in routes["data"]}
check("生成 3 个方案", len(schemes) == 3)

# 1. totalCost 与 costBreakdown.total 一致（货币口径）
for idx, s in schemes.items():
    bd = json.loads(s.get("costBreakdown") or "{}")
    total = bd.get("total")
    check(f"方案{idx} totalCost==costBreakdown.total", s.get("totalCost") == total,
          f"totalCost={s.get('totalCost')} vs total={total}")

# 3. 预算档次透传（H1）
bd1 = json.loads(schemes.get(1, {}).get("costBreakdown") or "{}")
check("budgetLevel 透传 economy", bd1.get("budgetLevel") == "economy", f"got {bd1.get('budgetLevel')}")

# 2. 费用最优 <= 时间最优
time_cost = schemes.get(1, {}).get("totalCost")
money_cost = schemes.get(2, {}).get("totalCost")
check("费用最优 totalCost <= 时间最优", money_cost is not None and time_cost is not None and money_cost <= time_cost,
      f"money={money_cost} vs time={time_cost}")

# 4. totalDistance 为距离(>0)且不再等于 totalCost
dist_ok = all(s.get("totalDistance") is not None and s.get("totalDistance") > 0
              and s.get("totalDistance") != s.get("totalCost") for s in schemes.values())
check("totalDistance 为公里且独立于 totalCost", dist_ok,
      str([(k, s.get("totalDistance"), s.get("totalCost")) for k, s in schemes.items()]))

req("DELETE", f"/plans/{pid}", token=tok, expect=200)

print("\n===== Bug2 结果 =====")
for r in results:
    print(" ".join(str(x) for x in r))
print(f"\nPASS={passed} FAIL={failed}")
exit(1 if failed else 0)
