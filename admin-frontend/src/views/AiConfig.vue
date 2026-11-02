<template>
  <div class="section-card">
    <div class="page-header">
      <h2>AI 配置</h2>
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" style="max-width: 640px;" v-loading="loading">
      <el-form-item label="平台" prop="provider">
        <el-input v-model="form.provider" placeholder="如：deepseek" maxlength="50" />
      </el-form-item>
      <el-form-item label="接口地址" prop="baseUrl">
        <el-input v-model="form.baseUrl" placeholder="如：https://api.deepseek.com" maxlength="255" />
      </el-form-item>
      <el-form-item label="API Key">
        <el-input v-model="form.apiKey" type="password" show-password placeholder="DeepSeek API Key" maxlength="255" />
      </el-form-item>
      <el-form-item label="模型" prop="model">
        <el-input v-model="form.model" placeholder="如：deepseek-chat" maxlength="100" />
      </el-form-item>
      <el-form-item label="温度">
        <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" />
        <span class="text-muted" style="margin-left: 12px;">0~2，越高越发散</span>
      </el-form-item>
      <el-form-item label="max_tokens">
        <el-input-number v-model="form.maxTokens" :min="1" :max="32768" :step="256" />
      </el-form-item>
      <el-form-item label="启用状态">
        <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitLoading" @click="submit">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiConfig, updateAiConfig } from '@/api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

const form = ref({
  provider: 'deepseek',
  baseUrl: 'https://api.deepseek.com',
  apiKey: '',
  model: 'deepseek-chat',
  temperature: 1.0,
  maxTokens: 4096,
  enabled: 1
})

const rules = {
  provider: [{ required: true, message: '请输入平台', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
  model: [{ required: true, message: '请输入模型', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await getAiConfig()
    if (data) {
      form.value = {
        provider: data.provider || 'deepseek',
        baseUrl: data.baseUrl || '',
        apiKey: data.apiKey || '',
        model: data.model || 'deepseek-chat',
        temperature: data.temperature != null ? Number(data.temperature) : 1.0,
        maxTokens: data.maxTokens || 4096,
        enabled: data.enabled == null ? 1 : data.enabled
      }
    }
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await updateAiConfig({
      provider: form.value.provider,
      baseUrl: form.value.baseUrl,
      apiKey: form.value.apiKey,
      model: form.value.model,
      temperature: form.value.temperature,
      maxTokens: form.value.maxTokens,
      enabled: form.value.enabled
    })
    ElMessage.success('保存成功')
  } catch (e) { /* ignore */ }
  finally { submitLoading.value = false }
}

onMounted(load)
</script>
