# -*- coding: utf-8 -*-
"""H3 回归测试：天数分配不超额（DayAllocator）

旧实现：leisure=1.2 / cultural=1.1 / food=1.05 会把额外天数放大，floor 后
sum(stayDays) 可能超过 totalDays（如 2 城 7 天 leisure → 4+4=8 天）。
修复后：额外天数严格按景点权重比例分配，总和恒等于 totalDays。

验证方式：生成路线后，daySchedules 是键 "1".."N" 的 JSON 对象，N 必须 == totalDays。
"""
import json, time, datetime, urllib.request, urllib.error

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
        code, payload = resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        code, payload = e.code, json.loads(e.read().decode())
    ok = expect is None or code == expect
    if ok: passed += 1
    else: failed += 1
    results.append(("PASS" if ok else "FAIL", method, path, code, str(payload)[:90]))
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
U = {"email": f"h3{stamp}@test.com", "nick": f"H3{stamp}", "pwd": "Test123456"}
req("POST", "/auth/register", body={"nickname": U["nick"], "email": U["email"], "password": U["pwd"]}, expect=200)
_, lg = req("POST", "/auth/login", body={"account": U["email"], "password": U["pwd"]}, expect=200)
tok = lg["data"]["token"]

_, cities = req("GET", "/destination/cities?page=1&pageSize=2", expect=200)
ids = [c["id"] for c in cities["data"]["records"]]
check("取得 2 个城市", len(ids) == 2, str(ids))

def end_date_for(start, total_days):
    d = datetime.date.fromisoformat(start) + datetime.timedelta(days=total_days - 1)
    return d.isoformat()

combos = [("leisure", 7), ("leisure", 8), ("cultural", 7),
          ("cultural", 8), ("food", 7), ("food", 8)]

for style, total in combos:
    _, pd = req("POST", "/plans", token=tok, body={
        "title": f"H3-{style}-{total}", "travelStyle": style, "cityIds": ids,
        "startDate": "2026-10-01", "endDate": end_date_for("2026-10-01", total)}, expect=200)
    pid = pd["data"]["id"]
    req("POST", f"/plans/{pid}/generate-route", token=tok, body={"schemeCount": 3}, expect=200)
    _, routes = req("GET", f"/plans/{pid}/routes", token=tok, expect=200)
    for s in routes["data"]:
        try:
            ds = json.loads(s.get("daySchedules") or "{}")
            days = len(ds)
        except Exception:
            days = -1
        check(f"[{style} {total}天] {s.get('schemeName')} 天数={total}", days == total,
              f"实际 {days} 天, routeOrder={s.get('routeOrder')}")
    req("DELETE", f"/plans/{pid}", token=tok, expect=200)

print("\n===== H3 结果 =====")
for r in results:
    print(" ".join(str(x) for x in r))
print(f"\nPASS={passed} FAIL={failed}")
exit(1 if failed else 0)
