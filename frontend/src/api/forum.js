import request from '@/utils/request'

export function getCategories() {
  return request.get('/forum/categories')
}

export function listPosts(params) {
  return request.get('/forum/posts', { params })
}

export function getPostDetail(id) {
  return request.get(`/forum/posts/${id}`)
}

export function getPostForEdit(id) {
  return request.get(`/forum/posts/${id}/edit`)
}

export function createPost(data) {
  return request.post('/forum/posts', data)
}

export function updatePost(id, data) {
  return request.put(`/forum/posts/${id}`, data)
}

export function deletePost(id) {
  return request.delete(`/forum/posts/${id}`)
}

export function addComment(data) {
  return request.post('/forum/comments', data)
}

export function getPostComments(postId) {
  return request.get(`/forum/posts/${postId}/comments`)
}

export function deleteComment(commentId) {
  return request.delete(`/forum/comments/${commentId}`)
}

export function toggleCommentLike(commentId) {
  return request.post(`/forum/comments/${commentId}/like`)
}

export function toggleCommentNotInterested(commentId) {
  return request.post(`/forum/comments/${commentId}/not-interested`)
}

export function toggleLike(postId) {
  return request.post('/forum/likes', { postId })
}

export function getMyNotifications() {
  return request.get('/forum/notifications')
}

export function markNotificationRead(id) {
  return request.put(`/forum/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.put('/forum/notifications/read-all')
}
