<template>
  <div v-loading="loading" class="section-card">
    <div v-if="city" class="city-header">
      <div class="flex-1">
        <h1>{{ city.name }}</h1>
        <p class="text-muted" v-if="city.provinceName">{{ city.provinceName }}</p>
        <div style="margin: 8px 0;">
          <el-tag v-if="city.bestSeason" type="success">最佳季节：{{ city.bestSeason }}</el-tag>
          <el-tag v-if="city.popularity" type="warning">热度 {{ city.popularity }}</el-tag>
        </div>
        <p>{{ city.description || '暂无介绍' }}</p>
      </div>
      <div class="header-right">
        <img v-if="city.imageUrl" :src="city.imageUrl" class="city-cover" :alt="city.name" />
        <el-button type="primary" @click="$router.push('/itineraries/new')">
          <el-icon><Promotion /></el-icon>AI 生成行程
        </el-button>
      </div>
    </div>

    <el-empty v-else-if="!loading" :description="loadError || '城市不存在'" />

    <template v-if="city">
      <div v-if="city.specialtyFood" class="info-block">
        <h3 class="section-title">特色美食</h3>
        <p class="text-muted" style="line-height: 1.8;">{{ city.specialtyFood }}</p>
      </div>
      <div v-if="city.localCulture" class="info-block">
        <h3 class="section-title">乡土人情</h3>
        <p class="text-muted" style="line-height: 1.8;">{{ city.localCulture }}</p>
      </div>

      <h3 class="section-title">景点</h3>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="8" v-for="a in attractions" :key="a.id">
          <el-card class="attraction-card" shadow="hover" @click="openAttraction(a)">
            <div v-if="imagesOf(a).length" class="attraction-cover">
              <img :src="imagesOf(a)[0]" :alt="a.name" />
            </div>
            <div v-else class="attraction-cover attraction-placeholder">
              <el-icon :size="32"><Picture /></el-icon>
            </div>
            <div class="attraction-info">
              <h4>{{ a.name }}</h4>
              <div style="margin: 6px 0;">
                <el-tag size="small" v-if="a.category">{{ a.category }}</el-tag>
                <el-tag size="small" type="warning" v-if="a.rating != null">评分 {{ a.rating }}</el-tag>
                <el-tag size="small" type="danger" v-if="a.ticketPrice != null && a.ticketPrice > 0">¥{{ a.ticketPrice }}</el-tag>
              </div>
              <div class="text-muted" style="font-size: 12px;" v-if="a.suggestDuration">
                建议游玩 {{ a.suggestDuration }} 小时
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!attractions.length" description="该城市暂无景点信息" :image-size="80" />
    </template>

    <el-dialog v-model="dialogVisible" :title="current?.name || '景点详情'" width="640px">
      <template v-if="current">
        <el-carousel v-if="imagesOf(current).length" height="300px" indicator-position="outside">
          <el-carousel-item v-for="(img, i) in imagesOf(current)" :key="i">
            <img :src="img" class="dialog-img" :alt="current.name" />
          </el-carousel-item>
        </el-carousel>
        <div v-else class="attraction-cover attraction-placeholder" style="height: 200px; margin-bottom: 16px;">
          <el-icon :size="40"><Picture /></el-icon>
        </div>
        <el-descriptions :column="2" border style="margin-top: 16px;">
          <el-descriptions-item label="分类">{{ current.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ current.rating ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="门票">
            {{ current.ticketPrice != null && current.ticketPrice > 0 ? '¥' + current.ticketPrice : '免费' }}
          </el-descriptions-item>
          <el-descriptions-item label="建议时长">
            {{ current.suggestDuration ? current.suggestDuration + ' 小时' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="开放时间" :span="2">{{ current.openTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最佳季节" :span="2">{{ current.bestSeason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ current.address || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="current.description" style="margin-top: 12px;">
          <h4>景点简介</h4>
          <p class="text-muted" style="line-height: 1.8;">{{ current.description }}</p>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCityDetail } from '@/api/destination'

const route = useRoute()
const city = ref(null)
const attractions = ref([])
const loading = ref(true)
const loadError = ref('')
const dialogVisible = ref(false)
const current = ref(null)

function imagesOf(a) {
  if (!a?.imageUrls) return []
  if (Array.isArray(a.imageUrls)) return a.imageUrls
  try {
    const arr = JSON.parse(a.imageUrls)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

function openAttraction(a) {
  current.value = a
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getCityDetail(route.params.id)
    city.value = data.city
    attractions.value = data.attractions || []
  } catch (e) {
    city.value = null
    loadError.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.city-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}
.city-header h1 {
  margin: 0 0 8px;
}
.header-right {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-end;
  flex-shrink: 0;
}
.city-cover {
  width: 240px;
  height: 150px;
  object-fit: cover;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.section-title {
  margin: 24px 0 12px;
  font-size: 18px;
  padding-left: 12px;
  border-left: 4px solid var(--el-color-primary);
}
.attraction-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}
.attraction-card:hover {
  transform: translateY(-3px);
}
.attraction-cover {
  height: 160px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}
.attraction-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.attraction-placeholder {
  background: linear-gradient(135deg, #eef2f7 0%, #e3e9f2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}
.attraction-info h4 {
  margin: 0 0 4px;
  font-size: 16px;
}
.dialog-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
