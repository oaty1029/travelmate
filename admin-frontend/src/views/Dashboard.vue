<template>
  <div class="section-card">
    <div class="page-header">
      <h2>数据概览</h2>
    </div>

    <el-row :gutter="16">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="flex-row items-center gap-16">
            <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">
              <el-icon :size="32"><User /></el-icon>
            </div>
            <div>
              <el-statistic title="用户总数" :value="stats.totalUsers" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="flex-row items-center gap-16">
            <div class="stat-icon" style="background: #f0f9eb; color: #67c23a;">
              <el-icon :size="32"><Location /></el-icon>
            </div>
            <div>
              <el-statistic title="城市总数" :value="stats.totalCities" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="flex-row items-center gap-16">
            <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">
              <el-icon :size="32"><ChatDotRound /></el-icon>
            </div>
            <div>
              <el-statistic title="帖子总数" :value="stats.totalPosts" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="flex-row items-center gap-16">
            <div class="stat-icon" style="background: #fef0f0; color: #f56c6c;">
              <el-icon :size="32"><TrendCharts /></el-icon>
            </div>
            <div>
              <el-statistic title="今日新增" :value="stats.todayNew" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top: 24px;">
      <template #header><strong>近 7 天新增趋势</strong></template>
      <v-chart v-if="chartOption" :option="chartOption" style="height: 320px;" autoresize />
      <el-empty v-else description="暂无数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { getDashboard } from '@/api/admin'

use([CanvasRenderer, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const stats = ref({ totalUsers: 0, totalCities: 0, totalPosts: 0, todayNew: 0, trend: [] })

const chartOption = computed(() => {
  const trend = stats.value.trend || []
  const dates = trend.map(d => d.date)
  const users = trend.map(d => d.users || 0)
  const posts = trend.map(d => d.posts || 0)
  if (!dates.length) return null
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '新增帖子'] },
    grid: { left: 40, right: 20, top: 50, bottom: 40 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '新增用户', type: 'bar', data: users, itemStyle: { color: '#409eff' } },
      { name: '新增帖子', type: 'bar', data: posts, itemStyle: { color: '#e6a23c' } }
    ]
  }
})

async function load() {
  try {
    stats.value = await getDashboard() || stats.value
  } catch (e) { /* ignore */ }
}

onMounted(load)
</script>

<style scoped>
.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-card {
  margin-bottom: 16px;
}
</style>
