# -*- coding: utf-8 -*-
"""H4 回归测试：PDF 导出在「按码点截断」重构后仍正常输出合法 PDF

修复点：ExportService 将 line.substring(0,100) 改为按码点安全截断，避免切断
emoji 等代理对导致 showText 编码失败。代理对边界难以通过 HTTP 注入（攻略内容
来自 DB 城市/景点名），故此处做导出冒烟测试，确认修复未破坏导出链路。
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
        code, payload = resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        code, payload = e.code, json.loads(e.read().decode())
    ok = expect is None or code == expect
    if ok: passed += 1
    else: failed += 1
    results.append(("PASS" if ok else "FAIL", method, path, code, str(payload)[:90]))
    return code, payload

def req_raw(path, token=None):
    url = BASE + path
    r = urllib.request.Request(url, method="GET")
    if token: r.add_header("Authorization", "Bearer " + token)
    resp = urllib.request.urlopen(r, timeout=30)
    return resp.status, resp.read()

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
U = {"email": f"h4{stamp}@test.com", "nick": f"H4{stamp}", "pwd": "Test123456"}
req("POST", "/auth/register", body={"nickname": U["nick"], "email": U["email"], "password": U["pwd"]}, expect=200)
_, lg = req("POST", "/auth/login", body={"account": U["email"], "password": U["pwd"]}, expect=200)
tok = lg["data"]["token"]

_, cities = req("GET", "/destination/cities?page=1&pageSize=3", expect=200)
ids = [c["id"] for c in cities["data"]["records"]]
_, pd = req("POST", "/plans", token=tok, body={
    "title": "H4导出测试", "travelStyle": "cultural", "cityIds": ids,
    "startDate": "2026-10-01", "endDate": "2026-10-03"}, expect=200)
pid = pd["data"]["id"]
req("POST", f"/plans/{pid}/generate-route", token=tok, body={"schemeCount": 3}, expect=200)

for q in ("", "?schemeIndex=1"):
    code, body = req_raw(f"/plans/{pid}/export{q}", tok)
    ok = code == 200 and body[:5] == b"%PDF-" and len(body) > 1000
    check(f"导出{q or '(推荐)'} 返回合法 PDF", ok, f"status={code}, bytes={len(body)}, head={body[:5]!r}")

req("DELETE", f"/plans/{pid}", token=tok, expect=200)

print("\n===== H4 结果 =====")
for r in results:
    print(" ".join(str(x) for x in r))
print(f"\nPASS={passed} FAIL={failed}")
exit(1 if failed else 0)
