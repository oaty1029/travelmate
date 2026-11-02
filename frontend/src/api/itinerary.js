import request from '@/utils/request'

export function generateItinerary(data) {
  return request.post('/ai/generate', data)
}

export function saveItinerary(data) {
  return request.post('/itineraries', data)
}

export function getMyItineraries(params) {
  return request.get('/itineraries', { params })
}

export function getMyFavorites(params) {
  return request.get('/itineraries/favorites', { params })
}

export function getItineraryDetail(id) {
  return request.get(`/itineraries/${id}`)
}

export function deleteItinerary(id) {
  return request.delete(`/itineraries/${id}`)
}

export function copyItinerary(id) {
  return request.post(`/itineraries/${id}/copy`)
}

export function getRecommendedItineraries(params) {
  return request.get('/itineraries/recommended', { params })
}

export function exportItineraryPdf(id) {
  return request.get(`/itineraries/${id}/export`, { responseType: 'blob' })
}
