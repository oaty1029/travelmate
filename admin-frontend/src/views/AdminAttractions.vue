<template>
  <div class="section-card">
    <div class="page-header">
      <h2>景点管理</h2>
      <div style="display: flex; gap: 12px; align-items: center;">
        <el-select
          v-model="filterCityId"
          placeholder="按城市筛选"
          clearable
          filterable
          style="width: 220px;"
          @change="handleFilter"
        >
          <el-option v-for="c in cityOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="openDialog(null)">
          <el-icon><Plus /></el-icon>新增景点
        </el-button>
      </div>
    </div>

    <el-table :data="attractions" stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="景点名" show-overflow-tooltip />
      <el-table-column label="所属城市" width="140">
        <template #default="{ row }">{{ cityNameOf(row.cityId) }}</template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column prop="rating" label="评分" width="80" />
      <el-table-column label="门票" width="90">
        <template #default="{ row }">{{ row.ticketPrice != null && row.ticketPrice > 0 ? '¥' + row.ticketPrice : '免费' }}</template>
      </el-table-column>
      <el-table-column label="图片" width="90">
        <template #default="{ row }">{{ imagesOf(row).length }} 张</template>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑景点' : '新增景点'" width="640px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="景点名" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="所属城市" prop="cityId">
          <el-select v-model="form.cityId" placeholder="请选择城市" style="width: 100%;" filterable>
            <el-option v-for="c in cityOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%;">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-input-number v-model="form.rating" :min="0" :max="5" :step="0.1" :precision="1" />
        </el-form-item>
        <el-form-item label="门票(元)">
          <el-input-number v-model="form.ticketPrice" :min="0" :step="10" :precision="2" />
        </el-form-item>
        <el-form-item label="建议时长(时)">
          <el-input-number v-model="form.suggestDuration" :min="1" :max="24" />
        </el-form-item>
        <el-form-item label="开放时间">
          <el-input v-model="form.openTime" maxlength="100" placeholder="如：08:30-17:00" />
        </el-form-item>
        <el-form-item label="最佳季节">
          <el-input v-model="form.bestSeason" maxlength="100" placeholder="如：春秋季" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" maxlength="255" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="景点图片">
          <div style="width: 100%;">
            <div v-if="form.imageUrls.length" class="thumb-list">
              <div v-for="(u, i) in form.imageUrls" :key="i" class="thumb-item">
                <img :src="u" alt="景点图片" />
                <el-icon class="thumb-del" @click="removeImage(i)"><Close /></el-icon>
              </div>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-upload
                :show-file-list="false"
                :auto-upload="false"
                multiple
                accept=".jpg,.jpeg,.png,image/jpeg,image/png"
                :on-change="onFileChange"
              >
                <el-button size="small"><el-icon><Plus /></el-icon>选择图片（可多选）</el-button>
              </el-upload>
              <el-button v-if="pendingFiles.length" size="small" type="primary" :loading="uploading" @click="doUpload">
                上传已选 {{ pendingFiles.length }} 张
              </el-button>
            </div>
            <span class="upload-tip">支持 jpg/png，单张最大 5MB，可上传多张</span>
          </div>
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
import {
  listAttractions, createAttraction, updateAttraction, deleteAttraction, uploadImages, listCities
} from '@/api/admin'

const categories = ['自然景观', '历史遗迹', '博物馆', '宗教', '主题公园', '街区']

const loading = ref(false)
const submitLoading = ref(false)
const uploading = ref(false)
const attractions = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const cityOptions = ref([])
const filterCityId = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const pendingFiles = ref([])
const form = ref({
  id: null, name: '', cityId: null, category: '', description: '',
  rating: 4.0, ticketPrice: 0, suggestDuration: 2, openTime: '',
  bestSeason: '', address: '', imageUrls: [], status: 1
})

const rules = {
  name: [{ required: true, message: '请输入景点名', trigger: 'blur' }],
  cityId: [{ required: true, message: '请选择所属城市', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

function imagesOf(row) {
  if (!row?.imageUrls) return []
  if (Array.isArray(row.imageUrls)) return row.imageUrls
  try {
    const a = JSON.parse(row.imageUrls)
    return Array.isArray(a) ? a : []
  } catch (e) { return [] }
}

function cityNameOf(cityId) {
  const c = cityOptions.value.find(x => x.id === cityId)
  return c ? c.name : '-'
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filterCityId.value != null) params.cityId = filterCityId.value
    const data = await listAttractions(params)
    attractions.value = data.records || data.list || data || []
    total.value = data.total || attractions.value.length
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

async function loadCityOptions() {
  try {
    const data = await listCities({ page: 1, pageSize: 100 })
    cityOptions.value = data.records || data.list || data || []
  } catch (e) { /* ignore */ }
}

function handleFilter() {
  page.value = 1
  load()
}

function openDialog(row) {
  isEdit.value = !!row
  if (row) {
    form.value = {
      ...row,
      imageUrls: imagesOf(row)
    }
  } else {
    form.value = {
      id: null, name: '', cityId: null, category: '', description: '',
      rating: 4.0, ticketPrice: 0, suggestDuration: 2, openTime: '',
      bestSeason: '', address: '', imageUrls: [], status: 1
    }
  }
  pendingFiles.value = []
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
      cityId: form.value.cityId,
      name: form.value.name,
      category: form.value.category,
      description: form.value.description || null,
      rating: form.value.rating ?? null,
      ticketPrice: form.value.ticketPrice ?? null,
      suggestDuration: form.value.suggestDuration ?? null,
      openTime: form.value.openTime || null,
      bestSeason: form.value.bestSeason || null,
      address: form.value.address || null,
      imageUrls: form.value.imageUrls.length ? JSON.stringify(form.value.imageUrls) : null,
      status: form.value.status
    }
    if (isEdit.value) {
      await updateAttraction(form.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createAttraction(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e) { /* ignore */ }
  finally { submitLoading.value = false }
}

function onFileChange(file) {
  const name = (file.name || '').toLowerCase()
  const okType = (file.raw && (file.raw.type === 'image/jpeg' || file.raw.type === 'image/png'))
    || name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png')
  if (!okType) {
    ElMessage.error('仅支持 jpg/png 格式图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }
  pendingFiles.value.push(file.raw)
}

async function doUpload() {
  if (!pendingFiles.value.length) return
  uploading.value = true
  try {
    const urls = await uploadImages(pendingFiles.value)
    form.value.imageUrls.push(...urls)
    pendingFiles.value = []
    ElMessage.success('图片上传成功')
  } catch (e) { /* 拦截器已提示 */ }
  finally { uploading.value = false }
}

function removeImage(i) {
  form.value.imageUrls.splice(i, 1)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除景点 "${row.name}" 吗？`, '警告', { type: 'warning' })
    await deleteAttraction(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadCityOptions()
  load()
})
</script>

<style scoped>
.thumb-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.thumb-item {
  position: relative;
  width: 96px;
  height: 64px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}
.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.thumb-del {
  position: absolute;
  top: 2px;
  right: 2px;
  cursor: pointer;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  padding: 2px;
}
.upload-tip {
  font-size: 12px;
  color: #909399;
}
</style>
