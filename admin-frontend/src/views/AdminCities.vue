<template>
  <div class="section-card">
    <div class="page-header">
      <h2>城市管理</h2>
      <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
        <el-input v-model="keyword" placeholder="按城市名搜索" clearable style="width: 200px;" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-select v-model="provinceId" placeholder="按省份筛选" clearable style="width: 160px;" @change="handleSearch">
          <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="primary" @click="openDialog(null)">
          <el-icon><Plus /></el-icon>新增城市
        </el-button>
      </div>
    </div>

    <el-table :data="cities" stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="城市名" />
      <el-table-column prop="provinceName" label="省份" />
      <el-table-column prop="pinyin" label="拼音" width="120" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="popularity" label="热度" width="100" />
      <el-table-column label="热门" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.isPopular === 1" @change="() => togglePopular(row)" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑城市' : '新增城市'" width="560px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="城市名" prop="name">
          <el-input v-model="form.name" maxlength="191" show-word-limit />
        </el-form-item>
        <el-form-item label="省份" prop="provinceId">
          <el-select v-model="form.provinceId" placeholder="请选择省份" style="width: 100%;" filterable>
            <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="拼音">
          <el-input v-model="form.pinyin" maxlength="191" placeholder="如：beijing" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="特色美食">
          <el-input v-model="form.specialtyFood" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="乡土人情">
          <el-input v-model="form.localCulture" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="城市图片">
          <div class="city-image-upload">
            <div v-if="form.imageUrl" class="image-preview">
              <img :src="form.imageUrl" alt="城市图片" />
            </div>
            <div class="city-image-actions">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="handleUpload"
                accept=".jpg,.jpeg,.png,image/jpeg,image/png"
              >
                <el-button size="small">{{ form.imageUrl ? '更换图片' : '上传图片' }}</el-button>
              </el-upload>
              <el-button v-if="form.imageUrl" size="small" type="danger" plain @click="handleRemoveImage">移除</el-button>
            </div>
            <span class="upload-tip">支持 jpg/png 格式，最大 5MB</span>
          </div>
        </el-form-item>
        <el-form-item label="最佳季节">
          <el-input v-model="form.bestSeason" maxlength="191" placeholder="如：3-5月 9-11月" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tagsText" placeholder="逗号分隔，如：历史,文化,美食" />
        </el-form-item>
        <el-form-item label="热度">
          <el-input-number v-model="form.popularity" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="设为热门">
          <el-switch v-model="form.isPopular" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCities, createCity, updateCity, deleteCity, uploadCityImage, deleteCityImage } from '@/api/admin'
import { getProvinces } from '@/api/destination'

const loading = ref(false)
const submitLoading = ref(false)
const cities = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const provinces = ref([])
const keyword = ref('')
const provinceId = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ id: null, name: '', provinceId: null, pinyin: '', description: '', specialtyFood: '', localCulture: '', imageUrl: '', bestSeason: '', tagsText: '', popularity: 50, isPopular: 0, status: 1 })
const originalImageUrl = ref('')

const rules = {
  name: [{ required: true, message: '请输入城市名', trigger: 'blur' }],
  provinceId: [{ required: true, message: '请选择省份', trigger: 'change' }]
}

function tagsToJson(text) {
  const arr = (text || '').split(/[,，]/).map(s => s.trim()).filter(Boolean)
  return arr.length ? JSON.stringify(arr) : null
}

function tagsFromJson(json) {
  if (!json) return ''
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr.join(',') : ''
  } catch (e) { return '' }
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (provinceId.value) params.provinceId = provinceId.value
    const data = await listCities(params)
    cities.value = data.records || data.list || data || []
    total.value = data.total || cities.value.length
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleSearch() {
  page.value = 1
  load()
}

function openDialog(row) {
  isEdit.value = !!row
  if (row) {
    form.value = { ...row, tagsText: tagsFromJson(row.tags) }
    originalImageUrl.value = row.imageUrl || ''
  } else {
    form.value = { id: null, name: '', provinceId: null, pinyin: '', description: '', specialtyFood: '', localCulture: '', imageUrl: '', bestSeason: '', tagsText: '', popularity: 50, isPopular: 0, status: 1 }
    originalImageUrl.value = ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    const payload = {
      name: form.value.name,
      provinceId: form.value.provinceId,
      pinyin: form.value.pinyin || null,
      description: form.value.description || null,
      specialtyFood: form.value.specialtyFood || null,
      localCulture: form.value.localCulture || null,
      imageUrl: form.value.imageUrl || null,
      bestSeason: form.value.bestSeason || null,
      tags: tagsToJson(form.value.tagsText),
      popularity: form.value.popularity ?? 0,
      isPopular: form.value.isPopular ? 1 : 0,
      status: form.value.status
    }
    if (isEdit.value) {
      await updateCity(form.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createCity(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e) { /* ignore */ }
  finally { submitLoading.value = false }
}

async function togglePopular(row) {
  const next = row.isPopular === 1 ? 0 : 1
  try {
    await updateCity(row.id, {
      name: row.name,
      provinceId: row.provinceId,
      pinyin: row.pinyin || null,
      description: row.description || null,
      specialtyFood: row.specialtyFood || null,
      localCulture: row.localCulture || null,
      imageUrl: row.imageUrl || null,
      bestSeason: row.bestSeason || null,
      tags: row.tags || null,
      popularity: row.popularity ?? 0,
      lat: row.lat ?? null,
      lng: row.lng ?? null,
      status: row.status ?? 1,
      isPopular: next
    })
    row.isPopular = next
    ElMessage.success(next === 1 ? '已设为热门城市' : '已取消热门')
  } catch (e) { /* 拦截器已提示 */ }
}

function beforeUpload(file) {
  const name = (file.name || '').toLowerCase()
  const okType = file.type === 'image/jpeg' || file.type === 'image/png'
    || name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png')
  if (!okType) {
    ElMessage.error('仅支持 jpg/png 格式图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function handleUpload({ file }) {
  try {
    const data = await uploadCityImage(file)
    form.value.imageUrl = data.url
    ElMessage.success('图片上传成功')
  } catch (e) { /* 拦截器已提示 */ }
}

async function handleRemoveImage() {
  if (isEdit.value && form.value.id && originalImageUrl.value && form.value.imageUrl === originalImageUrl.value) {
    try { await deleteCityImage(form.value.id) } catch (e) { /* ignore */ }
  }
  form.value.imageUrl = ''
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除城市 "${row.name}" 吗？`, '警告', { type: 'warning' })
    await deleteCity(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) { /* ignore */ }
}

onMounted(async () => {
  load()
  try { provinces.value = await getProvinces() } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.city-image-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.image-preview {
  width: 160px;
  height: 100px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}
.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.city-image-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.upload-tip {
  font-size: 12px;
  color: #909399;
}
</style>
