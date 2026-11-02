import request from '@/utils/request'

export function getDashboard() {
  return request.get('/admin/dashboard')
}

// ========== AI 配置 ==========

export function getAiConfig() {
  return request.get('/admin/ai-config')
}

export function updateAiConfig(data) {
  return request.put('/admin/ai-config', data)
}

// ========== 用户管理 ==========

export function listUsers(params) {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

export function deleteUser(id) {
  return request.delete(`/admin/users/${id}`)
}

// ========== 城市管理 ==========

export function listCities(params) {
  return request.get('/admin/cities', { params })
}

export function createCity(data) {
  return request.post('/admin/cities', data)
}

export function updateCity(id, data) {
  return request.put(`/admin/cities/${id}`, data)
}

export function deleteCity(id) {
  return request.delete(`/admin/cities/${id}`)
}

export function uploadCityImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/city/upload', formData)
}

export function deleteCityImage(id) {
  return request.delete(`/city/${id}/image`)
}

// ========== 景点管理 ==========

export function listAttractions(params) {
  return request.get('/admin/attractions', { params })
}

export function createAttraction(data) {
  return request.post('/admin/attractions', data)
}

export function updateAttraction(id, data) {
  return request.put(`/admin/attractions/${id}`, data)
}

export function deleteAttraction(id) {
  return request.delete(`/admin/attractions/${id}`)
}

export function uploadImages(files) {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return request.post('/admin/images', formData)
}

// ========== 帖子管理 ==========

export function listPosts(params) {
  return request.get('/admin/posts', { params })
}

export function updatePostStatus(id, status, reason) {
  return request.put(`/admin/posts/${id}/status`, { status, reason })
}

export function batchHidePosts(ids, reason) {
  return request.post('/admin/posts/batch-hide', { ids, reason })
}

export function batchDeletePosts(ids) {
  return request.post('/admin/posts/batch-delete', { ids })
}

export function deletePost(id) {
  return request.delete(`/admin/posts/${id}`)
}

export function getForumCategories() {
  return request.get('/forum/categories')
}

// ========== 行程推荐 ==========

export function listItineraries(params) {
  return request.get('/admin/itineraries', { params })
}

export function recommendItinerary(id, recommended, reason) {
  return request.put(`/admin/itineraries/${id}/recommend`, { recommended, reason })
}

// ========== 操作日志 ==========

export function listLogs(params) {
  return request.get('/admin/logs', { params })
}
