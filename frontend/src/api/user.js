import request from '@/utils/request'

export function getProfile() {
  return request.get('/users/profile')
}

export function updateProfile(data) {
  return request.put('/users/profile', data)
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/users/avatar', formData)
}

export function changePassword(data) {
  return request.put('/users/password', data)
}
