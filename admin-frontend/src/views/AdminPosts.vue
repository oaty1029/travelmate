<template>
  <div class="section-card">
    <div class="page-header">
      <h2>帖子管理</h2>
      <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
        <el-input v-model="keyword" placeholder="按作者或标题搜索" clearable style="width: 200px;" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-select v-model="categoryId" placeholder="按分类筛选" clearable style="width: 150px;" @change="handleSearch">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="warning" plain :disabled="!selection.length" @click="handleBatchHide">
          <el-icon><Hide /></el-icon>批量隐藏
        </el-button>
        <el-button type="danger" plain :disabled="!selection.length" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
      </div>
    </div>

    <el-table :data="posts" stripe v-loading="loading" style="width: 100%;" @selection-change="s => selection = s">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column prop="authorName" label="作者" width="120" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column prop="likeCount" label="点赞" width="80" />
      <el-table-column prop="commentCount" label="评论" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleView(row)">查看</el-button>
          <el-button size="small" :type="row.status === 2 ? 'success' : 'warning'" @click="handleToggleStatus(row)">
            {{ row.status === 2 ? '发布' : '隐藏' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPosts, updatePostStatus, deletePost, batchHidePosts, batchDeletePosts, getForumCategories } from '@/api/admin'

const loading = ref(false)
const posts = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selection = ref([])
const keyword = ref('')
const categoryId = ref(null)
const categories = ref([])
const USER_FRONTEND_URL = import.meta.env.VITE_USER_FRONTEND_URL || 'http://localhost:5173'

function statusLabel(s) {
  const map = { 0: '草稿', 1: '已发布', 2: '已隐藏' }
  return map[s] ?? String(s)
}
function statusTag(s) {
  const map = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (categoryId.value) params.categoryId = categoryId.value
    const data = await listPosts(params)
    posts.value = data.records || data.list || data || []
    total.value = data.total || posts.value.length
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleSearch() {
  page.value = 1
  load()
}

function handleView(row) {
  window.open(`${USER_FRONTEND_URL}/forum/${row.id}`, '_blank')
}

async function handleToggleStatus(row) {
  let reason = ''
  if (row.status !== 2) {
    try {
      const r = await ElMessageBox.prompt('请输入隐藏原因', '隐藏帖子', {
        inputPlaceholder: '如：违规内容',
        inputValidator: (v) => (v && v.trim() ? true : '原因不能为空')
      })
      reason = r.value.trim()
    } catch (e) { return }
  }
  const next = row.status === 2 ? 1 : 2
  try {
    await updatePostStatus(row.id, next, reason)
    ElMessage.success('操作成功')
    await load()
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除帖子 "${row.title}" 吗？评论将一并删除。`, '警告', { type: 'warning' })
    await deletePost(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) { /* ignore */ }
}

async function handleBatchHide() {
  let reason = ''
  try {
    const r = await ElMessageBox.prompt('请输入隐藏原因', `批量隐藏 ${selection.value.length} 篇帖子`, {
      inputPlaceholder: '如：违规内容',
      inputValidator: (v) => (v && v.trim() ? true : '原因不能为空')
    })
    reason = r.value.trim()
  } catch (e) { return }
  try {
    await batchHidePosts(selection.value.map(p => p.id), reason)
    ElMessage.success('批量隐藏成功')
    await load()
  } catch (e) { /* ignore */ }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selection.value.length} 篇帖子吗？评论将一并删除。`, '警告', { type: 'warning' })
    await batchDeletePosts(selection.value.map(p => p.id))
    ElMessage.success('批量删除成功')
    await load()
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  load()
  getForumCategories().then(list => { categories.value = list || [] }).catch(() => {})
})
</script>
