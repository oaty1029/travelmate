<template>
  <div class="section-card">
    <div class="page-header">
      <h2>行程推荐</h2>
      <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
        <el-input v-model="keyword" placeholder="按作者或标题搜索" clearable style="width: 200px;" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-select v-model="recommended" placeholder="全部状态" clearable style="width: 160px;" @change="handleFilter">
          <el-option label="已推荐" :value="1" />
          <el-option label="未推荐" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <el-table :data="items" stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column prop="authorName" label="作者" width="120">
        <template #default="{ row }">{{ row.authorName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="totalDays" label="天数" width="70" />
      <el-table-column label="总预算" width="110">
        <template #default="{ row }">¥{{ row.totalBudget }}</template>
      </el-table-column>
      <el-table-column label="推荐状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isRecommended === 1 ? 'success' : 'info'">
            {{ row.isRecommended === 1 ? '已推荐' : '未推荐' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="recommendReason" label="推荐语" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.isRecommended !== 1" size="small" type="success" plain @click="handleRecommend(row)">推荐</el-button>
          <el-button v-else size="small" type="warning" plain @click="handleUnrecommend(row)">取消推荐</el-button>
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
import { listItineraries, recommendItinerary } from '@/api/admin'

const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const recommended = ref('')
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (recommended.value !== '' && recommended.value != null) params.recommended = recommended.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await listItineraries(params)
    items.value = data.records || data.list || data || []
    total.value = data.total || items.value.length
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleFilter() {
  page.value = 1
  load()
}

function handleSearch() {
  page.value = 1
  load()
}

async function handleRecommend(row) {
  let reason = ''
  try {
    const r = await ElMessageBox.prompt('请输入推荐语', `推荐行程「${row.title}」到首页`, {
      inputPlaceholder: '如：超值亲子游，适合周末出行'
    })
    reason = (r.value || '').trim()
  } catch (e) { return }
  try {
    await recommendItinerary(row.id, 1, reason)
    ElMessage.success('已推荐到首页')
    await load()
  } catch (e) { /* ignore */ }
}

async function handleUnrecommend(row) {
  try {
    await ElMessageBox.confirm(`确定取消推荐「${row.title}」吗？`, '提示', { type: 'warning' })
    await recommendItinerary(row.id, 0, null)
    ElMessage.success('已取消推荐')
    await load()
  } catch (e) { /* ignore */ }
}

onMounted(load)
</script>
