import { createRouter, createWebHistory } from 'vue-router'
import { isTokenExpired } from '@/utils/jwt'

const routes = [
  // 公开路由
  { path: '/', component: () => import('@/views/Home.vue') },
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/register', component: () => import('@/views/Register.vue') },
  { path: '/destination', component: () => import('@/views/DestinationBrowse.vue') },
  { path: '/city/:id', component: () => import('@/views/CityDetail.vue') },
  { path: '/forum', component: () => import('@/views/ForumList.vue') },
  { path: '/forum/:id', component: () => import('@/views/ForumPostDetail.vue') },
  { path: '/profile', component: () => import('@/views/UserProfile.vue'), meta: { requiresAuth: true } },

  // 需要登录的路由
  { path: '/forum/new', component: () => import('@/views/ForumPostEditor.vue'), meta: { requiresAuth: true } },
  { path: '/forum/:id/edit', component: () => import('@/views/ForumPostEditor.vue'), meta: { requiresAuth: true } },

  // AI 行程
  { path: '/itineraries', component: () => import('@/views/MyItineraries.vue'), meta: { requiresAuth: true } },
  { path: '/itineraries/new', component: () => import('@/views/AiGenerate.vue'), meta: { requiresAuth: true } },
  { path: '/itineraries/:id', component: () => import('@/views/ItineraryDetail.vue'), meta: { requiresAuth: true } },
  { path: '/favorites', component: () => import('@/views/MyFavorites.vue'), meta: { requiresAuth: true } },

  // 404
  { path: '/:pathMatch(.*)*', component: () => import('@/views/NotFound.vue') }
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

  // 有 token 但已过期，跳登录
  if (token && isTokenExpired(token)) {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
