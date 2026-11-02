<template>
  <div class="section-card" v-loading="pageLoading">
    <h2>{{ isEdit ? '编辑帖子' : '发表新帖' }}</h2>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="margin-top: 16px;">
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 300px;">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入帖子标题" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item label="封面 URL">
        <el-input v-model="form.coverUrl" placeholder="可选，封面图片链接" maxlength="191" />
      </el-form-item>
      <el-form-item label="正文" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="12"
          maxlength="5000"
          show-word-limit
          placeholder="支持 Markdown 或 HTML 格式"
        />
      </el-form-item>
      <el-form-item label="挂载行程">
        <el-select v-model="form.itineraryId" clearable placeholder="可选，挂载你的 AI 行程供他人查看" style="width: 320px;">
          <el-option v-for="it in itineraries" :key="it.id" :label="`${it.title}（${it.totalDays ?? '-'}天）`" :value="it.id" />
        </el-select>
        <div v-if="!itineraries.length" class="text-muted" style="font-size: 12px; margin-top: 4px;">
          还没有行程，可先到「AI 生成行程」创建
        </div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          <el-icon><Promotion /></el-icon>{{ isEdit ? '保存' : '发表' }}
        </el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPost, updatePost, getPostForEdit, getCategories } from '@/api/forum'
import { getMyItineraries } from '@/api/itinerary'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const pageLoading = ref(false)
const categories = ref([])
const itineraries = ref([])

const isEdit = computed(() => !!route.params.id)

const form = ref({ categoryId: null, title: '', content: '', coverUrl: '', itineraryId: null })
const rules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    if (isEdit.value) {
      await updatePost(route.params.id, { ...form.value })
      ElMessage.success('保存成功')
      router.push(`/forum/${route.params.id}`)
    } else {
      const data = await createPost({ ...form.value })
      ElMessage.success('发帖成功')
      router.push(`/forum/${data.id}`)
    }
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

onMounted(async () => {
  try { categories.value = await getCategories() } catch (e) { /* ignore */ }
  try {
    const data = await getMyItineraries({ page: 1, pageSize: 100 })
    itineraries.value = data?.records || []
  } catch (e) { /* ignore */ }
  if (isEdit.value) {
    pageLoading.value = true
    try {
      const p = await getPostForEdit(route.params.id)
      form.value = { categoryId: p.categoryId, title: p.title, content: p.content, coverUrl: p.coverUrl || '', itineraryId: p.itineraryId || null }
    } catch (e) {
      ElMessage.error('帖子不存在或无权访问')
      router.replace('/forum')
    } finally {
      pageLoading.value = false
    }
  }
})
</script>
