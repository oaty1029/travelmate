<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="login-brand">
        <el-icon :size="30" color="#14b8a6"><Location /></el-icon>
        <h2>TravelMate 管理后台</h2>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="submit">
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="账号（邮箱/手机号/昵称）" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password size="large">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%;" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const loading = ref(false)
const form = ref({ account: '', password: '' })

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const data = await login({ account: form.value.account, password: form.value.password })
    const user = data.user
    if (!user || user.role !== 'admin') {
      ElMessage.error('非管理员账号，禁止登录')
      return
    }
    sessionStorage.setItem('token', data.token)
    sessionStorage.setItem('userInfo', JSON.stringify(user))
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } catch (e) { /* 拦截器已提示 */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #14b8a6 0%, #22c55e 100%);
}
.login-card {
  width: 400px;
  padding: 8px 12px;
}
.login-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  margin-bottom: 24px;
}
.login-brand h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
}
</style>
