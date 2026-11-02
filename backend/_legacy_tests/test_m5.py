# -*- coding: utf-8 -*-
"""M5 论坛模块后端自测"""
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

# ---- 登录两个用户 ----
ts = int(time.time())
s, b, _ = req("POST", "/auth/login", {"account": "admin@travelmate.com", "password": "Admin@123"})
admin_token = j(b)["data"]["token"]
s, b, _ = req("POST", "/auth/register", {
    "email": f"m5user{ts}@test.com", "password": "Test123456", "nickname": f"M5用户{ts}", "phone": ""})
user_token = j(b)["data"]["token"]
check("T0 登录/注册", bool(admin_token) and bool(user_token))

# ---- 分类 ----
s, b, _ = req("GET", "/forum/categories")
cats = j(b)["data"]
cat_id = cats[0]["id"] if cats else None
check("T1 分类列表", s == 200 and cat_id is not None, f"cats={len(cats)}")

# ---- 发帖 ----
post_body = {"categoryId": cat_id, "title": f"M5测试帖{ts}", "content": "这是测试内容，用来验证论坛模块。"}
s, b, _ = req("POST", "/forum/posts", post_body, admin_token)
post_id = j(b)["data"]["id"] if s == 200 else None
check("T2 发帖成功", s == 200 and post_id is not None, f"postId={post_id}")

s, b, _ = req("POST", "/forum/posts", {"categoryId": cat_id, "title": "  ", "content": "x"}, admin_token)
check("T3 空标题400", s == 400, f"got {s}")

s, b, _ = req("POST", "/forum/posts", {"categoryId": cat_id, "title": "x" * 101, "content": "y"}, admin_token)
check("T4 超长标题400", s == 400, f"got {s}")

s, b, _ = req("POST", "/forum/posts", {"categoryId": cat_id, "title": "ok", "content": "y" * 5001}, admin_token)
check("T5 超长内容400", s == 400, f"got {s}")

s, b, _ = req("POST", "/forum/posts", {"categoryId": 999999, "title": "ok", "content": "y"}, admin_token)
check("T6 分类不存在404", s == 404, f"got {s}")

# ---- 列表分页/筛选 ----
s, b, _ = req("GET", "/forum/posts?page=1&pageSize=5")
d = j(b)["data"]
check("T7 分页列表", s == 200 and "records" in d and "total" in d, f"total={d.get('total')}")

s, b, _ = req("GET", f"/forum/posts?categoryId={cat_id}&page=1&pageSize=5")
recs = j(b)["data"]["records"]
check("T8 分类筛选", s == 200 and all(r["categoryId"] == cat_id for r in recs), f"n={len(recs)}")

s, b, _ = req("GET", f"/forum/posts?keyword={urllib.parse.quote(f'M5测试帖{ts}')}")
recs = j(b)["data"]["records"]
check("T9 关键词搜索", s == 200 and any(r["id"] == post_id for r in recs), f"n={len(recs)}")

s, b, _ = req("GET", "/forum/posts?keyword=" + urllib.parse.quote("长" * 51))
check("T10 超长关键词400", s == 400, f"got {s}")

# ---- 详情 ----
s, b, _ = req("GET", f"/forum/posts/{post_id}")
check("T11 帖子详情", s == 200 and j(b)["data"]["id"] == post_id, f"got {s}")
s, b, _ = req("GET", "/forum/posts/999999")
check("T12 不存在404", s == 404, f"got {s}")

# ---- 编辑 ----
s, b, _ = req("PUT", f"/forum/posts/{post_id}", {"categoryId": cat_id, "title": "改过的标题", "content": "改过的内容"}, admin_token)
check("T13 编辑帖子", s == 200 and j(b)["data"]["title"] == "改过的标题", f"got {s}")
s, b, _ = req("PUT", f"/forum/posts/{post_id}", {"categoryId": cat_id, "title": "x", "content": "y"}, user_token)
check("T14 他人编辑403", s == 403, f"got {s}")

# ---- 评论 ----
s, b, _ = req("POST", "/forum/comments", {"postId": post_id, "content": "用户评论一条"}, user_token)
comment_id = j(b)["data"]["id"] if s == 200 else None
check("T15 评论成功", s == 200 and comment_id is not None, f"commentId={comment_id}")

s, b, _ = req("POST", "/forum/comments", {"postId": post_id, "content": "  "}, user_token)
check("T16 空评论400", s == 400, f"got {s}")
s, b, _ = req("POST", "/forum/comments", {"postId": post_id, "content": "c" * 1001}, user_token)
check("T17 超长评论400", s == 400, f"got {s}")
s, b, _ = req("POST", "/forum/comments", {"postId": 999999, "content": "c"}, user_token)
check("T18 评论不存在帖子404", s == 404, f"got {s}")

s, b, _ = req("GET", f"/forum/posts/{post_id}/comments")
comments = j(b)["data"]
check("T19 评论列表", s == 200 and len(comments) == 1, f"n={len(comments)}")
s, b, _ = req("GET", "/forum/posts/999999/comments")
check("T20 评论列表404", s == 404, f"got {s}")

# ---- 删除评论 ----
# 他人（第三方）不能删
s, b, _ = req("DELETE", f"/forum/comments/{comment_id}", None, admin_token)
check("T21 帖子作者可删评论", s == 200, f"got {s}")
s, b, _ = req("GET", f"/forum/posts/{post_id}/comments")
check("T22 删除后评论空", len(j(b)["data"]) == 0)

# 再造一条评论，测试评论作者删除 + 无权
s, b, _ = req("POST", "/forum/comments", {"postId": post_id, "content": "再评论一条"}, user_token)
c2 = j(b)["data"]["id"]
s, b, _ = req("DELETE", f"/forum/comments/{c2}", None, user_token)
check("T23 评论作者可删", s == 200, f"got {s}")
s, b, _ = req("POST", "/forum/comments", {"postId": post_id, "content": "第三条"}, user_token)
c3 = j(b)["data"]["id"]
s, b, _ = req("DELETE", f"/forum/comments/{c3}", None, admin_token)
check("T24 帖子作者删评论", s == 200, f"got {s}")
# 注册第三方验证 403
s, b, _ = req("POST", "/auth/register", {
    "email": f"m5third{ts}@test.com", "password": "Test123456", "nickname": f"M5第三方{ts}", "phone": ""})
third_token = j(b)["data"]["token"]
s, b, _ = req("POST", "/forum/comments", {"postId": post_id, "content": "第四条"}, user_token)
c4 = j(b)["data"]["id"]
s, b, _ = req("DELETE", f"/forum/comments/{c4}", None, third_token)
check("T25 无关用户删评论403", s == 403, f"got {s}")
req("DELETE", f"/forum/comments/{c4}", None, user_token)

# ---- 点赞 ----
s, b, _ = req("POST", "/forum/likes", {"postId": post_id}, user_token)
check("T26 点赞liked=true", s == 200 and j(b)["data"]["liked"] is True, f"got {s}")
s, b, _ = req("POST", "/forum/likes", {"postId": post_id}, user_token)
check("T27 取消赞liked=false", s == 200 and j(b)["data"]["liked"] is False, f"got {s}")
s, b, _ = req("POST", "/forum/likes", {}, user_token)
check("T28 postId缺失400", s == 400, f"got {s}")

# ---- 通知 ----
s, b, _ = req("POST", "/forum/comments", {"postId": post_id, "content": "触发通知的评论"}, user_token)
s, b, _ = req("POST", "/forum/likes", {"postId": post_id}, user_token)
s, b, _ = req("GET", "/forum/notifications", None, admin_token)
notifs = j(b)["data"]
check("T29 收到通知", s == 200 and len(notifs) >= 2, f"n={len(notifs)}")
nid = notifs[0]["id"]

s, b, _ = req("PUT", f"/forum/notifications/{nid}/read", None, admin_token)
check("T30 标记已读", s == 200, f"got {s}")
s, b, _ = req("GET", "/forum/notifications", None, admin_token)
remaining = j(b)["data"]
check("T31 已读后不再返回", all(n["id"] != nid for n in remaining), f"n={len(remaining)}")

s, b, _ = req("PUT", f"/forum/notifications/{nid}/read", None, user_token)
check("T32 他人通知已读403", s == 403, f"got {s}")

s, b, _ = req("PUT", "/forum/notifications/read-all", None, admin_token)
check("T33 全部已读", s == 200, f"got {s}")
s, b, _ = req("GET", "/forum/notifications", None, admin_token)
check("T34 全部已读后空", len(j(b)["data"]) == 0)

# ---- 删除帖子级联 ----
s, b, _ = req("DELETE", f"/forum/posts/{post_id}", None, user_token)
check("T35 他人删帖403", s == 403, f"got {s}")
s, b, _ = req("DELETE", f"/forum/posts/{post_id}", None, admin_token)
check("T36 删帖成功", s == 200, f"got {s}")
s, b, _ = req("GET", f"/forum/posts/{post_id}")
check("T37 删帖后404", s == 404, f"got {s}")
s, b, _ = req("GET", f"/forum/posts/{post_id}/comments")
check("T38 级联删评论", s == 404)

# ---- 汇总 ----
passed = sum(1 for _, c in results if c)
print(f"\n===== M5 SUMMARY: {passed}/{len(results)} passed =====")
for name, cond in results:
    if not cond:
        print("FAILED:", name)
