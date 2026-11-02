import { createRouter, createWebHistory } from 'vue-router'
import { decodeJwt, isTokenExpired } from '@/utils/jwt'
import Layout from '@/views/Layout.vue'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  {
    path: '/',
    component: Layout,
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '数据概览' } },
      { path: 'ai-config', component: () => import('@/views/AiConfig.vue'), meta: { title: 'AI 配置' } },
      { path: 'users', component: () => import('@/views/AdminUsers.vue'), meta: { title: '用户管理' } },
      { path: 'cities', component: () => import('@/views/AdminCities.vue'), meta: { title: '城市管理' } },
      { path: 'attractions', component: () => import('@/views/AdminAttractions.vue'), meta: { title: '景点管理' } },
      { path: 'posts', component: () => import('@/views/AdminPosts.vue'), meta: { title: '帖子管理' } },
      { path: 'itineraries', component: () => import('@/views/AdminItineraries.vue'), meta: { title: '行程推荐' } },
      { path: 'logs', component: () => import('@/views/AdminLogs.vue'), meta: { title: '操作日志' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (token && isTokenExpired(token)) {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin) {
    const isAdminRole = r => r === 'admin'
    const payload = decodeJwt(token)
    if (!payload || !isAdminRole(payload.role)) {
      next('/login')
      return
    }
  }

  next()
})

export default router
