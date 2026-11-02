<template>
  <div class="login-wrapper">
    <el-card class="login-card">
      <h2 class="text-center">登录 TravelMate</h2>
      <p class="text-center text-muted">欢迎回来，开启你的旅程规划</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" style="margin-top: 20px;">
        <el-form-item label="账号 / 邮箱" prop="account">
          <el-input v-model="form.account" placeholder="请输入账号或邮箱" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%;" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
      <div class="text-center" style="margin-top: 16px;">
        <span class="text-muted">还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = ref({ account: '', password: '' })
const rules = {
  account: [{ required: true, message: '请输入账号或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const data = await login(form.value)
    const u = data.user
    const userInfo = { id: u.id, nickname: u.nickname, email: u.email, phone: u.phone, role: u.role, status: u.status, avatarUrl: u.avatarUrl }
    userStore.login(userInfo, data.token)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) { /* interceptor handles message */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
}
.login-card {
  width: 400px;
  padding: 24px;
}
</style>
