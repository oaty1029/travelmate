import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const userInfo = ref(
    JSON.parse(sessionStorage.getItem('userInfo') || 'null')
  )

  function login(user, newToken) {
    token.value = newToken
    userInfo.value = user
    sessionStorage.setItem('token', newToken)
    sessionStorage.setItem('userInfo', JSON.stringify(user))
  }

  function register(user, newToken) {
    // 注册后暂存登录状态
    token.value = newToken
    userInfo.value = user
    sessionStorage.setItem('token', newToken)
    sessionStorage.setItem('userInfo', JSON.stringify(user))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
  }

  function setUserInfo(user) {
    userInfo.value = user
    sessionStorage.setItem('userInfo', JSON.stringify(user))
  }

  function loadFromStorage() {
    token.value = sessionStorage.getItem('token') || ''
    const info = sessionStorage.getItem('userInfo')
    userInfo.value = info ? JSON.parse(info) : null
  }

  return { token, userInfo, login, register, logout, setUserInfo, loadFromStorage }
})
