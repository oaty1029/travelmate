<template>
  <div class="section-card" v-loading="loading">
    <template v-if="post">
      <el-card shadow="hover">
        <div class="flex-row justify-between items-start">
          <h1 style="margin-top: 0;">{{ post.title }}</h1>
          <div v-if="isOwner" class="flex-row gap-8">
            <el-button size="small" @click="$router.push(`/forum/${post.id}/edit`)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDeletePost">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </div>
        </div>
        <div class="flex-row items-center gap-16 text-muted" style="margin-bottom: 16px;">
          <el-avatar :size="32" :src="post.authorAvatar">{{ post.authorName?.charAt(0) || 'U' }}</el-avatar>
          <span>{{ post.authorName }}</span>
          <span>{{ formatDate(post.createdAt) }}</span>
          <el-tag v-if="post.categoryName">{{ post.categoryName }}</el-tag>
          <span v-if="isOwner"><el-icon><View /></el-icon> {{ post.viewCount || 0 }}</span>
        </div>
        <div class="post-body" v-html="sanitizedContent"></div>

        <div v-if="attached" class="attached-itinerary">
          <el-divider content-position="left">附带的行程</el-divider>
          <div class="flex-row justify-between items-center" style="margin-bottom: 8px;">
            <strong>{{ attached.summary?.title || '行程' }}</strong>
            <div>
              <el-button size="small" type="primary" plain @click="$router.push(`/itineraries/${post.itineraryId}`)">查看完整行程</el-button>
              <el-button size="small" type="warning" @click="handleCopyItinerary">收藏此行程</el-button>
            </div>
          </div>
          <el-descriptions :column="3" size="small" border>
            <el-descriptions-item label="总天数">{{ attached.summary?.totalDays }} 天</el-descriptions-item>
            <el-descriptions-item label="总预算">¥{{ attached.summary?.totalBudget }}</el-descriptions-item>
            <el-descriptions-item label="亮点">{{ attached.summary?.headline || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div v-for="day in attached.days" :key="day.day" style="margin-top: 10px;">
            <div style="font-weight: 600; margin-bottom: 4px;">
              第 {{ day.day }} 天 · {{ day.date }} · {{ day.city }}<template v-if="day.theme"> · {{ day.theme }}</template>
            </div>
            <div v-for="(item, i) in day.items" :key="i" class="attach-item">
              <el-tag size="small" effect="plain" :type="itemTag(item.type).type">{{ itemTag(item.type).label }}</el-tag>
              <span class="text-muted" style="margin: 0 8px; font-size: 12px;">{{ item.time }}</span>
              <span>{{ item.name }}</span>
              <span v-if="item.price != null" style="float: right; color: #f56c6c;">¥{{ item.price }}</span>
            </div>
          </div>
        </div>

        <div style="margin-top: 16px; display: flex; gap: 12px;">
          <el-button :type="isLiked ? 'danger' : 'default'" @click="handleLike">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path :d="isLiked ? HEART_FILLED : HEART_OUTLINE" />
            </svg>
            <span style="margin-left: 4px;">{{ isLiked ? '已点赞' : '点赞' }} ({{ post.likeCount || 0 }})</span>
          </el-button>
          <el-button @click="handleShare">
            <el-icon><Share /></el-icon> 分享
          </el-button>
        </div>
      </el-card>

      <h3 style="margin-top: 24px;">评论（{{ totalCommentCount }}）</h3>

      <el-card shadow="hover" style="margin-bottom: 16px;">
        <div class="flex-row gap-12 items-start">
          <el-avatar :size="36" :src="userStore.userInfo?.avatarUrl">
            {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <div style="flex: 1;">
            <el-input
              v-model="commentText"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              :placeholder="userStore.token ? '发表评论...' : '请先登录后发表评论'"
              :disabled="!userStore.token"
            />
            <div style="margin-top: 8px; text-align: right;">
              <el-button type="primary" :disabled="!userStore.token" @click="handleSubmitComment">
                <el-icon><Promotion /></el-icon>发表
              </el-button>
            </div>
          </div>
        </div>
      </el-card>

      <div v-for="c in comments" :key="c.id" class="comment-item">
        <div class="flex-row gap-12 items-start">
          <el-avatar :size="32" :src="c.authorAvatar">{{ c.authorName?.charAt(0) || 'U' }}</el-avatar>
          <div style="flex: 1;">
            <div class="flex-row items-center gap-8">
              <strong>{{ c.authorName }}</strong>
              <span class="text-muted" style="font-size: 12px;">{{ formatDate(c.createdAt) }}</span>
              <el-button
                v-if="canDeleteComment(c)"
                link
                size="small"
                type="danger"
                style="margin-left: auto;"
                @click="handleDeleteComment(c)"
              >删除</el-button>
            </div>
            <p class="comment-content">{{ c.content }}</p>
            <div class="comment-actions">
              <span class="react-btn" :class="{ active: c.liked }" @click="toggleCommentLike(c)">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path :d="c.liked ? HEART_FILLED : HEART_OUTLINE" />
                </svg>
                <span class="count">{{ c.likeCount || 0 }}</span>
              </span>
              <span class="react-btn" :class="{ active: c.notInterested }" @click="toggleCommentNotInterested(c)">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path :d="HEART_OUTLINE" />
                  <path d="M7 3.5 L10 7.5 L6.5 10 L12.5 14.5 L9 20.5" fill="none" stroke="#fff" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
              <el-button link size="small" @click="openReply(c)">回复</el-button>
            </div>

            <div v-if="c.replyCount > 0" class="reply-toggle" @click="toggleReplies(c)">
              {{ expandedReplies[c.id] ? '收起' : `展开 ${c.replyCount} 条评论` }}
            </div>

            <div v-if="expandedReplies[c.id]" class="replies">
              <div v-for="r in c.replies" :key="r.id" class="comment-item reply-item">
                <div class="flex-row gap-12 items-start">
                  <el-avatar :size="28" :src="r.authorAvatar">{{ r.authorName?.charAt(0) || 'U' }}</el-avatar>
                  <div style="flex: 1;">
                    <div class="flex-row items-center gap-8">
                      <strong>{{ r.authorName }}</strong>
                      <span class="text-muted" style="font-size: 12px;">{{ formatDate(r.createdAt) }}</span>
                      <el-button
                        v-if="canDeleteComment(r)"
                        link
                        size="small"
                        type="danger"
                        style="margin-left: auto;"
                        @click="handleDeleteComment(r)"
                      >删除</el-button>
                    </div>
                    <p class="comment-content">
                      <template v-if="r.replyToUserName">回复 <span class="reply-to">@{{ r.replyToUserName }}</span>：</template>{{ r.content }}
                    </p>
                    <div class="comment-actions">
                      <span class="react-btn" :class="{ active: r.liked }" @click="toggleCommentLike(r)">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                          <path :d="r.liked ? HEART_FILLED : HEART_OUTLINE" />
                        </svg>
                        <span class="count">{{ r.likeCount || 0 }}</span>
                      </span>
                      <span class="react-btn" :class="{ active: r.notInterested }" @click="toggleCommentNotInterested(r)">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                          <path :d="HEART_OUTLINE" />
                          <path d="M7 3.5 L10 7.5 L6.5 10 L12.5 14.5 L9 20.5" fill="none" stroke="#fff" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                        </svg>
                      </span>
                      <el-button link size="small" @click="openReply(r)">回复</el-button>
                    </div>

                    <div v-if="replyTo?.id === r.id" class="reply-editor">
                      <el-input
                        v-model="replyText"
                        type="textarea"
                        :rows="2"
                        maxlength="1000"
                        show-word-limit
                        :placeholder="`回复 @${replyTo.authorName}...`"
                      />
                      <div class="reply-editor-actions">
                        <el-button size="small" @click="closeReply">取消</el-button>
                        <el-button size="small" type="primary" @click="submitReply(replyTo)">回复</el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="replyTo?.id === c.id" class="reply-editor">
              <el-input
                v-model="replyText"
                type="textarea"
                :rows="2"
                maxlength="1000"
                show-word-limit
                :placeholder="`回复 @${replyTo.authorName}...`"
              />
              <div class="reply-editor-actions">
                <el-button size="small" @click="closeReply">取消</el-button>
                <el-button size="small" type="primary" @click="submitReply(replyTo)">回复</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!comments.length" description="还没有评论，沙发留给你！" />
    </template>
    <el-empty v-else-if="!loading" description="帖子不存在或已被删除">
      <el-button @click="$router.push('/forum')">返回论坛</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getPostDetail, getPostComments, addComment, deleteComment, toggleLike, toggleCommentLike as toggleCommentLikeApi, toggleCommentNotInterested as toggleCommentNotInterestedApi, deletePost } from '@/api/forum'
import { copyItinerary } from '@/api/itinerary'
import { useUserStore } from '@/stores/user'
import { sanitizeHtml } from '@/utils/sanitize'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const post = ref(null)
const loading = ref(false)

// 爱心 / 裂开爱心 SVG 路径
const HEART_FILLED = 'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z'
const HEART_OUTLINE = 'M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z'

const attached = computed(() => post.value?.itinerary || null)

// XSS 防护：对帖子内容做 HTML 消毒
const sanitizedContent = computed(() => sanitizeHtml(post.value?.content))
const comments = ref([])
const commentText = ref('')
const isLiked = ref(false)
const replyTo = ref(null)
const replyText = ref('')
const expandedReplies = reactive({})

const totalCommentCount = computed(() =>
  comments.value.reduce((sum, c) => sum + 1 + (c.replyCount || 0), 0)
)

const isOwner = computed(() =>
  userStore.userInfo?.id != null && post.value?.userId != null &&
  Number(userStore.userInfo.id) === Number(post.value.userId)
)

function canDeleteComment(c) {
  if (!userStore.userInfo?.id) return false
  const uid = Number(userStore.userInfo.id)
  return uid === Number(c.userId) || uid === Number(post.value?.userId)
}

function formatDate(d) {
  return d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '-'
}

function itemTag(t) {
  const map = {
    attraction: { label: '景点', type: 'primary' },
    meal: { label: '餐饮', type: 'success' },
    hotel: { label: '住宿', type: 'warning' },
    transport: { label: '交通', type: 'info' }
  }
  return map[t] || { label: '其他', type: 'info' }
}

function requireLogin() {
  if (!userStore.token) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

async function handleCopyItinerary() {
  if (!requireLogin()) return
  try {
    const copy = await copyItinerary(post.value.itineraryId)
    ElMessage.success('已收藏到我的行程')
    router.push(`/itineraries/${copy.id}`)
  } catch (e) { /* interceptor handles message */ }
}

async function load() {
  loading.value = true
  try {
    const data = await getPostDetail(route.params.id)
    post.value = data
    isLiked.value = !!data.liked
  } catch (e) {
    post.value = null
  }
  try {
    comments.value = await getPostComments(route.params.id)
  } catch (e) { comments.value = [] }
  loading.value = false
}

async function handleLike() {
  if (!requireLogin()) return
  try {
    const data = await toggleLike(route.params.id)
    isLiked.value = data.liked
    post.value.likeCount = (post.value.likeCount || 0) + (data.liked ? 1 : -1)
  } catch (e) { /* ignore */ }
}

async function toggleCommentLike(c) {
  if (!requireLogin()) return
  try {
    const data = await toggleCommentLikeApi(c.id)
    c.liked = data.liked
    c.notInterested = data.notInterested
    c.likeCount = data.likeCount
    c.notInterestedCount = data.notInterestedCount
  } catch (e) { /* ignore */ }
}

async function toggleCommentNotInterested(c) {
  if (!requireLogin()) return
  try {
    const data = await toggleCommentNotInterestedApi(c.id)
    c.liked = data.liked
    c.notInterested = data.notInterested
    c.likeCount = data.likeCount
    c.notInterestedCount = data.notInterestedCount
  } catch (e) { /* ignore */ }
}

function openReply(c) {
  if (!requireLogin()) return
  replyTo.value = c
  replyText.value = ''
}

function closeReply() {
  replyTo.value = null
  replyText.value = ''
}

function toggleReplies(c) {
  expandedReplies[c.id] = !expandedReplies[c.id]
}

async function submitReply(target) {
  if (!replyText.value.trim()) { ElMessage.warning('回复不能为空'); return }
  try {
    await addComment({ postId: Number(route.params.id), parentId: Number(target.id), content: replyText.value.trim() })
    ElMessage.success('回复发表成功')
    closeReply()
    await load()
  } catch (e) { /* ignore */ }
}

async function handleSubmitComment() {
  if (!commentText.value.trim()) { ElMessage.warning('评论不能为空'); return }
  try {
    await addComment({ postId: Number(route.params.id), content: commentText.value.trim() })
    ElMessage.success('评论发表成功')
    commentText.value = ''
    await load()
  } catch (e) { /* ignore */ }
}

async function handleDeleteComment(c) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
  } catch (e) { return }
  try {
    await deleteComment(c.id)
    ElMessage.success('评论已删除')
    await load()
  } catch (e) { /* ignore */ }
}

async function handleDeletePost() {
  try {
    await ElMessageBox.confirm('删除后帖子及评论将不可恢复，确定删除？', '提示', { type: 'warning' })
  } catch (e) { return }
  try {
    await deletePost(post.value.id)
    ElMessage.success('帖子已删除')
    router.push('/forum')
  } catch (e) { /* ignore */ }
}

async function handleShare() {
  try {
    await ElMessageBox.prompt('已将链接复制到剪贴板', '分享链接', {
      confirmButtonText: '复制',
      showCancelButton: false,
      inputValue: location.href
    })
    navigator.clipboard?.writeText(location.href).then(
      () => ElMessage.success('已复制'),
      () => ElMessage.info('请手动复制')
    )
  } catch (e) { /* ignore */ }
}

onMounted(load)
</script>

<style scoped>
.post-body :deep(h1),
.post-body :deep(h2),
.post-body :deep(h3) {
  margin: 16px 0 8px;
}
.post-body :deep(p) {
  margin: 8px 0;
  line-height: 1.8;
}
.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}
.comment-content {
  margin: 4px 0 0;
  color: #303133;
  line-height: 1.6;
}
.reply-to {
  color: #409eff;
}
.comment-actions {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.react-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #909399;
  font-size: 13px;
  user-select: none;
  padding: 2px 4px;
  transition: color 0.2s;
}
.react-btn:hover {
  color: #ef4444;
}
.react-btn.active {
  color: #ef4444;
}
.react-btn .count {
  min-width: 12px;
}
.reply-toggle {
  margin-top: 8px;
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
  user-select: none;
}
.reply-toggle:hover {
  text-decoration: underline;
}
.replies {
  margin-top: 8px;
  padding-left: 12px;
  border-left: 2px solid #eef2f6;
  background: #fafbfc;
  border-radius: 0 6px 6px 0;
  padding: 4px 12px;
}
.reply-item {
  border-bottom: 1px dashed #ebeef5;
}
.reply-item:last-child {
  border-bottom: none;
}
.reply-editor {
  margin-top: 8px;
}
.reply-editor-actions {
  margin-top: 6px;
  text-align: right;
}
.attached-itinerary {
  margin-top: 16px;
  padding: 12px;
  background: #f7f9fc;
  border-radius: 6px;
}
.attach-item {
  padding: 4px 0;
}
</style>
