<template>
  <div class="section-card">
    <div class="page-header">
      <h2>城市景点</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索城市名称"
        style="width: 300px;"
        clearable
        maxlength="50"
        @keyup.enter="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover">
          <template #header>
            <strong>按省份</strong>
          </template>
          <el-menu
            :default-active="activeProvinceId"
            class="province-menu"
            @select="handleSelectProvince"
          >
            <el-menu-item index="">
              <el-icon><MapLocation /></el-icon>全部城市
            </el-menu-item>
            <el-menu-item v-for="p in provinces" :key="p.id" :index="String(p.id)">
              {{ p.name }}
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="18" v-loading="loading">
        <div class="city-scroll">
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" v-for="city in cities" :key="city.id">
            <el-card class="city-card" shadow="hover" @click="$router.push(`/city/${city.id}`)">
              <div v-if="city.imageUrl" class="city-image">
                <img :src="city.imageUrl" :alt="city.name" />
              </div>
              <div v-else class="city-placeholder" :style="{ background: cityColor(city.id) }">
                <el-icon :size="36"><Location /></el-icon>
              </div>
              <div class="city-info">
                <h3>{{ city.name }}</h3>
                <p>{{ city.description || '热门旅游目的地' }}</p>
                <div style="margin-top: 8px;">
                  <el-tag size="small" v-if="city.provinceName">{{ city.provinceName }}</el-tag>
                  <el-tag size="small" type="warning" v-if="city.popularity">热度 {{ city.popularity }}</el-tag>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        </div>
        <el-empty v-if="!loading && !cities.length" :description="loadError || '暂无城市'" />
        <div v-if="!searchMode && total > pageSize" style="margin-top: 16px; display: flex; justify-content: flex-end;">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next, total"
            @current-change="loadCities"
          />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProvinces, getCities, searchCities } from '@/api/destination'

const route = useRoute()
const provinces = ref([])
const cities = ref([])
const keyword = ref('')
const activeProvinceId = ref('')
const loading = ref(false)
const loadError = ref('')
const page = ref(1)
const pageSize = 12
const total = ref(0)
const searchMode = ref(false)

const palette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#ff7a45']
function cityColor(id) {
  return palette[(id - 1) % palette.length] || palette[0]
}

async function loadCities() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getCities(activeProvinceId.value || undefined, { page: page.value, pageSize })
    cities.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    cities.value = []
    loadError.value = '城市加载失败'
  } finally {
    loading.value = false
  }
}

async function handleSelectProvince(id) {
  activeProvinceId.value = id
  keyword.value = ''
  searchMode.value = false
  page.value = 1
  await loadCities()
}

async function handleSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    searchMode.value = false
    page.value = 1
    await loadCities()
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    cities.value = await searchCities(kw)
    total.value = cities.value.length
    searchMode.value = true
    activeProvinceId.value = ''
  } catch (e) {
    cities.value = []
    loadError.value = '搜索失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    provinces.value = await getProvinces()
  } catch (e) { /* ignore */ }
  if (route.query.provinceId) {
    activeProvinceId.value = String(route.query.provinceId)
  }
  await loadCities()
})
</script>

<style scoped>
.province-menu {
  border-right: none;
  max-height: calc(100vh - 260px);
  min-height: 200px;
  overflow-y: auto;
}
.province-menu::-webkit-scrollbar {
  width: 6px;
}
.province-menu::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}
.province-menu::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
.province-menu::-webkit-scrollbar-track {
  background: transparent;
}
.city-scroll {
  max-height: calc(100vh - 260px);
  overflow-y: auto;
  padding-right: 4px;
}
.city-scroll::-webkit-scrollbar {
  width: 6px;
}
.city-scroll::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}
.city-scroll::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
.city-scroll::-webkit-scrollbar-track {
  background: transparent;
}
.province-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 2px 0;
}
.province-menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
