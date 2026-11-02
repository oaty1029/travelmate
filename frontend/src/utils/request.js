import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = sessionStorage.getItem('token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

// 防止 401 时重复提示、重复跳转
let redirectingToLogin = false

function handleUnauthorized(msg) {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('userInfo')
  if (!redirectingToLogin) {
    redirectingToLogin = true
    ElMessage.error(msg || '登录已过期，请重新登录')
    const current = router.currentRoute.value
    const redirect = current.path !== '/login' ? current.fullPath : '/'
    router.push({ path: '/login', query: { redirect } })
      .finally(() => { redirectingToLogin = false })
  }
}

request.interceptors.response.use(
  res => {
    // 二进制响应（如 PDF 导出）直接透传 Blob，跳过业务码解包
    if (res.config.responseType === 'blob') return res.data
    const data = res.data
    if (data.code === 0) return data.data
    // 兜底：HTTP 200 但业务码为 401（旧格式兼容）
    if (data.code === 401) {
      handleUnauthorized(data.message)
      return Promise.reject(data)
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(data)
  },
  err => {
    const status = err.response?.status
    let msg
    if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
      msg = '请求超时，请稍后重试'
    } else if (!err.response) {
      msg = '网络异常，请检查网络连接'
    } else if (status === 401) {
      // JWT 拦截器返回 HTTP 401，body 为 { code, message }
      handleUnauthorized(err.response?.data?.message)
      return Promise.reject(err)
    } else if (status >= 500) {
      msg = '服务器繁忙，请稍后重试'
    } else {
      msg = err.response?.data instanceof Blob
        ? '导出失败，请稍后重试'
        : (err.response?.data?.message || '请求失败，请稍后重试')
    }
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default request
