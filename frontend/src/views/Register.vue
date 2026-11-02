<template>
  <div class="login-wrapper">
    <el-card class="login-card">
      <h2 class="text-center">注册 TravelMate</h2>
      <p class="text-center text-muted">加入我们，开启智能旅游规划</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" style="margin-top: 20px;">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="30" show-word-limit placeholder="请输入昵称" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large">
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号（选填）" size="large">
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码（至少 6 位）" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%;" :loading="loading" @click="handleRegister">
          注册并登录
        </el-button>
      </el-form>
      <div class="text-center" style="margin-top: 16px;">
        <span class="text-muted">已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = ref({ nickname: '', email: '', phone: '', password: '' })
const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 30, message: '昵称长度须在 2-30 个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
    { max: 100, message: '邮箱最长 100 个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度须在 6-64 位之间', trigger: 'blur' }
  ]
}

async function handleRegister() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const data = await register(form.value)
    const u = data.user
    const userInfo = { id: u.id, nickname: u.nickname, email: u.email, phone: u.phone, role: u.role, status: u.status, avatarUrl: u.avatarUrl }
    userStore.register(userInfo, data.token)
    ElMessage.success('注册成功，欢迎加入！')
    router.push('/')
  } catch (e) { /* interceptor handles */ }
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
