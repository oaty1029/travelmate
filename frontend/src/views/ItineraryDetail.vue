<template>
  <div class="section-card" v-loading="loading">
    <template v-if="itinerary">
      <div class="page-header">
        <h2>{{ itinerary.title }}</h2>
        <div style="display: flex; gap: 8px;">
          <el-tag v-if="itinerary.copyFromId" type="info">收藏</el-tag>
          <el-tag v-if="itinerary.isRecommended" type="warning">官方推荐</el-tag>
        </div>
      </div>

      <template v-if="result">
        <el-descriptions :column="3" border style="margin-bottom: 16px;">
          <el-descriptions-item label="总天数">{{ result.summary?.totalDays }} 天</el-descriptions-item>
          <el-descriptions-item label="总预算">¥{{ result.summary?.totalBudget }}</el-descriptions-item>
          <el-descriptions-item label="亮点">{{ result.summary?.headline || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="day-nav">
          <strong>每日行程</strong>
          <el-pagination
            small
            background
            layout="prev, pager, next"
            :total="(result.days || []).length"
            :page-size="1"
            :current-page="currentDay"
            @current-change="v => currentDay = v"
          />
        </div>
        <el-card v-if="currentDayData" shadow="never" class="day-card">
          <template #header>
            <strong>第 {{ currentDayData.day }} 天</strong>
            <span class="text-muted" style="margin-left: 12px;">
              {{ currentDayData.date }} · {{ currentDayData.city }}<template v-if="currentDayData.theme"> · {{ currentDayData.theme }}</template>
            </span>
          </template>
          <div v-for="(item, i) in currentDayData.items" :key="i" class="item-row">
            <el-tag size="small" :type="itemType(item.type).tag" effect="plain" class="item-tag">
              {{ itemType(item.type).label }}
            </el-tag>
            <span class="item-time">{{ item.time }}</span>
            <span class="item-name">{{ item.name }}</span>
            <span v-if="item.price != null" class="item-price">¥{{ item.price }}</span>
            <div v-if="item.note" class="text-muted item-note">{{ item.note }}</div>
            <a v-if="item.link" :href="item.link" target="_blank" rel="noopener" class="item-link">第三方链接</a>
          </div>
        </el-card>

        <h4 style="margin: 20px 0 10px;">费用明细</h4>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="交通">¥{{ result.costBreakdown?.transport }}</el-descriptions-item>
          <el-descriptions-item label="住宿">¥{{ result.costBreakdown?.accommodation }}</el-descriptions-item>
          <el-descriptions-item label="餐饮">¥{{ result.costBreakdown?.meals }}</el-descriptions-item>
          <el-descriptions-item label="门票">¥{{ result.costBreakdown?.tickets }}</el-descriptions-item>
          <el-descriptions-item label="其他">¥{{ result.costBreakdown?.other }}</el-descriptions-item>
          <el-descriptions-item label="合计">¥{{ result.costBreakdown?.total }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="以上价格为 AI 估算，仅供参考，实际费用可能与估算略有出入（可略高或略低）。"
          style="margin-top: 12px;"
        />

        <template v-if="result.tips?.length">
          <h4 style="margin: 20px 0 10px;">小贴士</h4>
          <ul class="tips-list">
            <li v-for="(t, i) in result.tips" :key="i">{{ t }}</li>
          </ul>
        </template>
      </template>

      <div style="margin-top: 24px; padding-top: 16px; border-top: 1px dashed #ebeef5; display: flex; gap: 12px; flex-wrap: wrap;">
        <el-button v-if="isOwner" type="primary" @click="handleExport">
          <el-icon><Document /></el-icon>导出 PDF
        </el-button>
        <el-button v-if="!isOwner" type="primary" @click="handleCopy">
          <el-icon><Star /></el-icon>收藏此行程
        </el-button>
        <el-button v-if="isOwner" type="danger" plain @click="handleDelete">
          <el-icon><Delete /></el-icon>删除
        </el-button>
      </div>
    </template>

    <el-empty v-else-if="!loading" :description="loadError || '行程不存在'" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getItineraryDetail, deleteItinerary, copyItinerary, exportItineraryPdf } from '@/api/itinerary'

const route = useRoute()
const router = useRouter()

const itinerary = ref(null)
const loading = ref(true)
const loadError = ref('')
const currentDay = ref(1)

const result = computed(() => {
  const rj = itinerary.value?.resultJson
  if (!rj) return null
  if (typeof rj === 'string') {
    try { return JSON.parse(rj) } catch (e) { return null }
  }
  return rj
})

const currentDayData = computed(() => result.value?.days?.[currentDay.value - 1] || null)

// 本人可见 inputJson，他人查看时后端已置空
const isOwner = computed(() => !!itinerary.value?.inputJson)

function itemType(t) {
  const map = {
    attraction: { label: '景点', tag: 'primary' },
    meal: { label: '餐饮', tag: 'success' },
    hotel: { label: '住宿', tag: 'warning' },
    transport: { label: '交通', tag: 'info' }
  }
  return map[t] || { label: '其他', tag: 'info' }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    itinerary.value = await getItineraryDetail(route.params.id)
    currentDay.value = 1
  } catch (e) {
    itinerary.value = null
    loadError.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  try {
    const blob = await exportItineraryPdf(route.params.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `itinerary-${route.params.id}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) { /* interceptor handles message */ }
}

async function handleCopy() {
  try {
    const copy = await copyItinerary(route.params.id)
    ElMessage.success('已收藏到我的行程')
    router.push(`/itineraries/${copy.id}`)
  } catch (e) { /* interceptor handles message */ }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除此行程吗？删除后不可恢复。', '提示', { type: 'warning' })
    await deleteItinerary(route.params.id)
    ElMessage.success('删除成功')
    router.push('/itineraries')
  } catch (e) { /* cancelled or error */ }
}

onMounted(load)
</script>

<style scoped>
.day-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 0 10px;
}
.day-card {
  border: 1px solid #ebeef5;
}
.item-row {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.item-row:last-child {
  border-bottom: none;
}
.item-tag {
  margin-right: 10px;
}
.item-time {
  color: #909399;
  font-size: 13px;
  margin-right: 10px;
}
.item-name {
  font-weight: 600;
}
.item-price {
  float: right;
  color: #f56c6c;
  font-weight: 600;
}
.item-note {
  font-size: 12px;
  margin-top: 4px;
  padding-left: 58px;
}
.item-link {
  font-size: 12px;
  margin-left: 10px;
}
.tips-list {
  padding-left: 20px;
  color: #606266;
  line-height: 1.8;
}
</style>
