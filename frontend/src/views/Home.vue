<template>
  <div>
    <section class="hero-section">
      <h1>中国境内智能旅游规划</h1>
      <p>输入你的偏好，AI 为你生成一份完整可执行的旅行行程</p>
      <div style="margin-top: 32px; display: flex; gap: 16px; justify-content: center; flex-wrap: wrap;">
        <el-button size="large" class="hero-primary" @click="$router.push('/itineraries/new')">
          <el-icon><Promotion /></el-icon>
          AI 生成行程
        </el-button>
        <el-button size="large" class="hero-secondary" @click="$router.push('/destination')">
          <el-icon><Location /></el-icon>
          浏览城市景点
        </el-button>
      </div>
    </section>

    <section class="section-card">
      <h2 class="page-header-title">热门城市</h2>
      <el-carousel
        v-if="popularCities.length"
        height="260px"
        :interval="4000"
        arrow="always"
        indicator-position="outside"
      >
        <el-carousel-item v-for="city in popularCities" :key="city.id">
          <div class="hot-slide" @click="$router.push(`/city/${city.id}`)">
            <img v-if="city.imageUrl" :src="city.imageUrl" :alt="city.name" class="hot-slide-img" />
            <div v-else class="hot-slide-img hot-slide-placeholder" :style="{ background: cityColor(city.id) }">
              <el-icon :size="48"><Location /></el-icon>
            </div>
            <div class="hot-slide-overlay">
              <h3>{{ city.name }}</h3>
              <p>{{ city.description || '热门旅游目的地' }}</p>
              <el-tag v-if="city.popularity" size="small" type="warning">热度 {{ city.popularity }}</el-tag>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
      <el-empty v-else description="暂无热门城市" :image-size="80" />
    </section>

    <section class="section-card">
      <h2 class="page-header-title">按省份浏览</h2>
      <el-row :gutter="12">
        <el-col :xs="12" :sm="8" :md="4" v-for="p in provinces" :key="p.id">
          <el-card class="province-card" shadow="hover" @click="$router.push({ path: '/destination', query: { provinceId: p.id } })">
            <div class="flex-col items-center gap-8">
              <el-icon :size="28" color="#14b8a6"><MapLocation /></el-icon>
              <span>{{ p.name }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!provinces.length" description="暂无省份数据" :image-size="80" />
    </section>

    <section class="section-card">
      <h2 class="page-header-title">推荐行程</h2>
      <el-carousel
        v-if="recommended.length"
        height="220px"
        :interval="5000"
        arrow="always"
        indicator-position="outside"
      >
        <el-carousel-item v-for="it in recommended" :key="it.id">
          <div class="rec-slide" :style="{ background: recGradient(it.id) }" @click="$router.push(`/itineraries/${it.id}`)">
            <el-tag class="rec-badge" type="warning" effect="dark" size="small">推荐</el-tag>
            <div class="rec-slide-content">
              <h3>{{ it.title }}</h3>
              <div class="rec-meta">
                <span>{{ it.totalDays ?? '-' }} 天</span>
                <span class="rec-dot">·</span>
                <span>¥{{ it.totalBudget ?? '-' }}</span>
              </div>
              <p v-if="it.recommendReason">{{ it.recommendReason }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
      <el-empty v-else description="暂无推荐行程" :image-size="80" />
    </section>

    <section class="section-card">
      <h2 class="page-header-title">为什么选择 TravelMate</h2>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="never" class="feature-card">
            <el-icon :size="48" color="#14b8a6"><Guide /></el-icon>
            <h3>智能路线规划</h3>
            <p>基于城市距离和景点评分的 AI 算法，生成多种行程方案</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="never" class="feature-card">
            <el-icon :size="48" color="#22c55e"><Money /></el-icon>
            <h3>费用精准估算</h3>
            <p>交通、住宿、餐饮、门票一站式估算，支持多档预算</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="never" class="feature-card">
            <el-icon :size="48" color="#f59e0b"><Document /></el-icon>
            <h3>自动生成攻略</h3>
            <p>一键生成结构化旅游攻略，可导出 PDF 方便携带</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="never" class="feature-card">
            <el-icon :size="48" color="#ef4444"><Share /></el-icon>
            <h3>社区分享交流</h3>
            <p>与百万旅行者分享经验，查看真实游记和评价</p>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPopularCities, getProvinces } from '@/api/destination'
import { getRecommendedItineraries } from '@/api/itinerary'

const popularCities = ref([])
const provinces = ref([])
const recommended = ref([])

const palette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#ff7a45']
function cityColor(id) {
  return palette[(id - 1) % palette.length] || palette[0]
}
function recGradient(id) {
  const c1 = palette[(id - 1) % palette.length] || palette[0]
  const c2 = palette[(id + 3) % palette.length] || palette[1]
  return `linear-gradient(135deg, ${c1} 0%, ${c2} 100%)`
}

onMounted(async () => {
  try {
    popularCities.value = await getPopularCities()
  } catch (e) { /* ignore */ }
  try {
    provinces.value = await getProvinces()
  } catch (e) { /* ignore */ }
  try {
    const data = await getRecommendedItineraries({ page: 1, pageSize: 6 })
    recommended.value = data?.records || []
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.hero-primary {
  background: #fff;
  border-color: #fff;
  color: var(--el-color-primary);
  font-weight: 600;
  padding: 14px 32px;
  font-size: 16px;
}
.hero-primary:hover {
  background: #f0fdfa;
  border-color: #f0fdfa;
  color: var(--el-color-primary-dark-2);
}
.hero-secondary {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.85);
  color: #fff;
  padding: 14px 32px;
  font-size: 16px;
}
.hero-secondary:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: #fff;
  color: #fff;
}
.hot-slide {
  position: relative;
  height: 260px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
}
.hot-slide-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.hot-slide-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.hot-slide-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20px 24px;
  background: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.65) 100%);
  color: #fff;
  text-align: left;
}
.hot-slide-overlay h3 {
  margin: 0 0 4px;
  font-size: 22px;
}
.hot-slide-overlay p {
  margin: 0 0 6px;
  opacity: 0.9;
  font-size: 13px;
}
.rec-slide {
  position: relative;
  height: 100%;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  color: #fff;
  display: flex;
  align-items: center;
  padding: 24px 32px;
  transition: transform 0.2s;
}
.rec-slide:hover {
  transform: scale(1.01);
}
.rec-badge {
  position: absolute;
  top: 16px;
  right: 16px;
}
.rec-slide-content {
  text-align: left;
}
.rec-slide-content h3 {
  margin: 0 0 12px;
  font-size: 22px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
}
.rec-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 8px;
}
.rec-meta .rec-dot {
  opacity: 0.7;
}
.rec-slide-content p {
  margin: 0;
  font-size: 13px;
  opacity: 0.92;
  max-width: 560px;
}
.page-header-title {
  margin: 0 0 16px;
  font-size: 20px;
  position: relative;
  padding-left: 14px;
  color: #1f2937;
}
.page-header-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--el-color-primary), var(--el-color-primary-dark-2));
}
.province-card {
  text-align: center;
  cursor: pointer;
  margin-bottom: 12px;
  transition: transform 0.2s;
}
.province-card:hover {
  transform: translateY(-3px);
}
.feature-card {
  text-align: center;
  padding: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.feature-card:hover {
  transform: translateY(-4px);
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--tm-shadow-card-hover);
}
.feature-card h3 {
  margin: 12px 0 8px;
  font-size: 16px;
}
.feature-card p {
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
}
</style>
