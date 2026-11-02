<template>
  <div class="section-card" v-loading="loading">
    <div class="page-header">
      <h2>我的收藏</h2>
      <el-button type="primary" @click="$router.push('/forum')">
        <el-icon><ChatDotRound /></el-icon>去论坛看看
      </el-button>
    </div>

    <el-empty v-if="!loading && !list.length" description="还没有收藏任何行程，去论坛或首页看看吧！" />

    <el-row v-else :gutter="16">
      <el-col :xs="24" :sm="12" :md="8" v-for="it in list" :key="it.id">
        <el-card shadow="hover" class="it-card">
          <template #header>
            <div class="flex-row justify-between items-center">
              <strong style="font-size: 16px;">{{ it.title }}</strong>
              <el-tag size="small" type="info">收藏</el-tag>
            </div>
          </template>
          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="总天数">{{ it.totalDays ?? '-' }} 天</el-descriptions-item>
            <el-descriptions-item label="总预算">¥{{ it.totalBudget ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="收藏时间">{{ formatDate(it.createdAt) }}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 12px; padding-top: 12px; border-top: 1px dashed #ebeef5; display: flex; gap: 8px;">
            <el-button size="small" type="primary" @click="$router.push(`/itineraries/${it.id}`)">详情</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(it)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div v-if="total > pageSize" style="margin-top: 16px; text-align: right;">
      <el-pagination
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="handlePage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getMyFavorites, deleteItinerary } from '@/api/itinerary'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 9
const total = ref(0)

function formatDate(d) {
  return d ? dayjs(d).format('YYYY-MM-DD') : '-'
}

async function load() {
  loading.value = true
  try {
    const data = await getMyFavorites({ page: page.value, pageSize })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePage(p) {
  page.value = p
  load()
}

async function handleDelete(it) {
  try {
    await ElMessageBox.confirm(`确定删除收藏"${it.title}"吗？删除后不可恢复。`, '提示', { type: 'warning' })
    await deleteItinerary(it.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) { /* cancelled or error */ }
}

onMounted(load)
</script>

<style scoped>
.it-card {
  margin-bottom: 16px;
}
.it-card:hover {
  transform: translateY(-3px);
}
</style>
