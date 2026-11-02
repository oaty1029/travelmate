<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="logo">
        <el-icon :size="22"><Location /></el-icon>
        <span>TravelMate 管理后台</span>
      </div>
      <el-menu :default-active="activeMenu" router class="admin-menu">
        <el-menu-item index="/dashboard"><el-icon><Odometer /></el-icon><span>数据概览</span></el-menu-item>
        <el-menu-item index="/ai-config"><el-icon><Setting /></el-icon><span>AI 配置</span></el-menu-item>
        <el-menu-item index="/users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/cities"><el-icon><MapLocation /></el-icon><span>城市管理</span></el-menu-item>
        <el-menu-item index="/attractions"><el-icon><Picture /></el-icon><span>景点管理</span></el-menu-item>
        <el-menu-item index="/posts"><el-icon><ChatDotRound /></el-icon><span>帖子管理</span></el-menu-item>
        <el-menu-item index="/itineraries"><el-icon><Tickets /></el-icon><span>行程推荐</span></el-menu-item>
        <el-menu-item index="/logs"><el-icon><Document /></el-icon><span>操作日志</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <span class="header-title">{{ pageTitle }}</span>
        <el-dropdown @command="handleCommand">
          <span class="user-chip">
            <el-icon><UserFilled /></el-icon>{{ nickname }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '管理后台')

const nickname = computed(() => {
  try {
    const info = JSON.parse(sessionStorage.getItem('userInfo') || 'null')
    return info?.nickname || info?.email || '管理员'
  } catch (e) {
    return '管理员'
  }
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}
.admin-aside {
  background: #fff;
  border-right: 1px solid #eef2f6;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  font-weight: 700;
  color: var(--el-color-primary);
  border-bottom: 1px solid #eef2f6;
}
.admin-menu {
  border-right: none;
  flex: 1;
}
.admin-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #eef2f6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}
.admin-main {
  background: #f5f7fa;
}
</style>
