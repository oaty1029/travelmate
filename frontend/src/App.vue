<template>
  <div id="app-root">
    <header class="app-header">
      <div class="header-inner">
        <router-link to="/" class="app-logo">
          <el-icon><Compass /></el-icon>
          <span>TravelMate 旅伴</span>
        </router-link>
        <nav class="nav-links">
          <router-link to="/destination">
            <el-icon><Location /></el-icon>
            城市景点
          </router-link>
          <router-link to="/forum">
            <el-icon><ChatDotRound /></el-icon>
            论坛
          </router-link>
          <router-link to="/favorites">
            <el-icon><Star /></el-icon>
            我的收藏
          </router-link>
          <router-link to="/itineraries">
            <el-icon><Calendar /></el-icon>
            我的行程
          </router-link>
        </nav>
        <div class="user-area">
          <el-popover
            v-if="userStore.token"
            placement="bottom-end"
            :width="360"
            trigger="click"
            @show="loadNotifications"
          >
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" style="margin-right: 16px;">
                <el-icon :size="20" style="cursor: pointer;"><Bell /></el-icon>
              </el-badge>
            </template>
            <div style="max-height: 320px; overflow: auto;">
              <el-empty v-if="!notifications.length" description="暂无新通知" :image-size="60" />
              <div
                v-for="n in notifications"
                :key="n.id"
                class="notif-item"
                @click="handleNotifClick(n)"
              >
                <div style="font-weight: 600;">{{ n.title }}</div>
                <div class="text-muted" style="font-size: 12px; margin: 2px 0;">{{ n.content }}</div>
                <div class="text-muted" style="font-size: 12px;">{{ formatTime(n.createdAt) }}</div>
              </div>
            </div>
            <div v-if="notifications.length" style="text-align: right; margin-top: 8px;">
              <el-button link size="small" @click="handleReadAll">全部已读</el-button>
            </div>
          </el-popover>
          <template v-if="userStore.token">
            <el-dropdown trigger="click" @command="handleCommand">
              <div class="flex-row items-center gap-8" style="cursor: pointer;">
                <el-avatar :size="36" :src="userStore.userInfo?.avatarUrl">
                  {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span>{{ userStore.userInfo?.nickname || userStore.userInfo?.email }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon> 个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="favorites">
                    <el-icon><Star /></el-icon> 我的收藏
                  </el-dropdown-item>
                  <el-dropdown-item command="plans">
                    <el-icon><Calendar /></el-icon> 我的行程
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon> 退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="$router.push('/login')">登录</el-button>
            <el-button @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main-container">
      <router-view />
    </main>

    <footer class="app-footer">
      <p>© 2026 TravelMate 旅伴 - 智能旅游规划平台</p>
      <p>
        <router-link to="/">首页</router-link>
        |
        <router-link to="/destination">城市景点</router-link>
        |
        <router-link to="/forum">论坛</router-link>
      </p>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { getMyNotifications, markNotificationRead, markAllNotificationsRead } from '@/api/forum'

const router = useRouter()
const userStore = useUserStore()

const notifications = ref([])
const unreadCount = ref(0)

function formatTime(d) {
  return d ? dayjs(d).format('MM-DD HH:mm') : ''
}

async function loadNotifications() {
  if (!userStore.token) return
  try {
    notifications.value = await getMyNotifications()
    unreadCount.value = notifications.value.length
  } catch (e) { /* ignore */ }
}

// 定时轮询未读通知，保证他人评论/点赞后无需刷新即可提示
let notifTimer = null
function startNotifPolling() {
  stopNotifPolling()
  notifTimer = setInterval(loadNotifications, 30000)
}
function stopNotifPolling() {
  if (notifTimer) {
    clearInterval(notifTimer)
    notifTimer = null
  }
}

async function handleNotifClick(n) {
  try { await markNotificationRead(n.id) } catch (e) { /* ignore */ }
  notifications.value = notifications.value.filter(x => x.id !== n.id)
  unreadCount.value = notifications.value.length
  if (n.type === 'recommend' && n.itineraryId) {
    router.push(`/itineraries/${n.itineraryId}`)
  } else if (n.postId) {
    router.push(`/forum/${n.postId}`)
  }
}

async function handleReadAll() {
  try { await markAllNotificationsRead() } catch (e) { /* ignore */ }
  notifications.value = []
  unreadCount.value = 0
}

watch(() => userStore.token, (v) => {
  if (v) {
    loadNotifications()
    startNotifPolling()
  } else {
    stopNotifPolling()
  }
})

const handleCommand = (cmd) => {
  switch (cmd) {
    case 'profile':
      router.push('/profile')
      break
    case 'favorites':
      router.push('/favorites')
      break
    case 'plans':
      router.push('/itineraries')
      break
    case 'logout':
      userStore.logout()
      notifications.value = []
      unreadCount.value = 0
      router.push('/')
      break
  }
}

onMounted(() => {
  loadNotifications()
  if (userStore.token) startNotifPolling()
})
onBeforeUnmount(stopNotifPolling)
</script>

<style scoped>
.notif-item {
  padding: 10px 8px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}
.notif-item:hover {
  background: #f5f7fa;
}
</style>
