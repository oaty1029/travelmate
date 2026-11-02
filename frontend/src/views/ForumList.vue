<template>
  <div class="section-card">
    <div class="page-header">
      <h2>旅游论坛</h2>
      <div>
        <el-input v-model="keyword" placeholder="搜索帖子" style="width: 240px; margin-right: 12px;" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handlePostNew">
          <el-icon><Edit /></el-icon>发帖
        </el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover">
          <template #header><strong>帖子分类</strong></template>
          <el-menu :default-active="activeCategory" @select="handleCategory" style="border-right: none;">
            <el-menu-item index="">
              <el-icon><All /></el-icon>全部
            </el-menu-item>
            <el-menu-item v-for="c in categories" :key="c.id" :index="String(c.id)">
              {{ c.name }}
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="18" v-loading="loading">
        <el-card
          v-for="post in posts"
          :key="post.id"
          class="post-item"
          shadow="hover"
          @click="$router.push(`/forum/${post.id}`)"
        >
          <div class="flex-row items-center gap-16">
            <el-avatar :src="post.authorAvatar" :size="48">
              {{ post.authorName?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="flex-1">
              <div class="flex-row justify-between items-center">
                <h3 style="margin: 0; font-size: 16px;">{{ post.title }}</h3>
                <el-tag size="small" v-if="post.categoryName">{{ post.categoryName }}</el-tag>
              </div>
              <p class="text-muted" style="margin: 4px 0; font-size: 13px;">
                {{ post.authorName }} · {{ formatDate(post.createdAt) }}
              </p>
              <div class="flex-row gap-16 text-muted" style="font-size: 13px;">
                <span v-if="isOwner(post)"><el-icon><View /></el-icon> {{ post.viewCount || 0 }}</span>
                <span>
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor" style="vertical-align: -2px;">
                    <path :d="HEART_OUTLINE" />
                  </svg>
                  {{ post.likeCount || 0 }}
                </span>
                <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
        <el-empty v-if="!posts.length" description="暂无帖子" />

        <div style="margin-top: 16px; display: flex; justify-content: center;">
          <el-pagination
            v-if="total > 0"
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="load"
          />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { getCategories, listPosts } from '@/api/forum'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const categories = ref([])
const posts = ref([])
const keyword = ref('')
const activeCategory = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 爱心（描边）图标路径，用于列表点赞数
const HEART_OUTLINE = 'M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z'

function formatDate(d) {
  return d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '-'
}

// 查看人数仅帖主可见
function isOwner(post) {
  return userStore.userInfo?.id != null && post.userId != null &&
    Number(userStore.userInfo.id) === Number(post.userId)
}

async function load() {
  const params = { page: page.value, pageSize: pageSize.value }
  if (activeCategory.value) params.categoryId = activeCategory.value
  if (keyword.value) params.keyword = keyword.value
  loading.value = true
  try {
    const data = await listPosts(params)
    posts.value = data.records || data.list || data || []
    total.value = data.total || posts.value.length
  } catch (e) { /* ignore */ } finally {
    loading.value = false
  }
}

async function handleCategory(id) {
  activeCategory.value = id
  page.value = 1
  await load()
}

async function handleSearch() {
  page.value = 1
  await load()
}

function handlePostNew() {
  if (!userStore.token) {
    router.push({ path: '/login', query: { redirect: '/forum/new' } })
    return
  }
  router.push('/forum/new')
}

onMounted(async () => {
  try { categories.value = await getCategories() } catch (e) { /* ignore */ }
  await load()
})
</script>

<style scoped>
.post-item {
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}
.post-item:hover {
  transform: translateY(-2px);
}
</style>
