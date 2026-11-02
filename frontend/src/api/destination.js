import request from '@/utils/request'

export function getProvinces() {
  return request.get('/destination/provinces')
}

export function getCities(provinceId, params) {
  return request.get('/destination/cities', { params: { provinceId, ...params } })
}

export function getPopularCities() {
  return request.get('/destination/cities/popular')
}

export function getCityDetail(cityId) {
  return request.get(`/destination/cities/${cityId}`)
}

export function searchCities(keyword) {
  return request.get('/destination/cities/search', { params: { keyword } })
}

export function resolveKeyword(keyword) {
  return request.get('/destination/resolve', { params: { keyword } })
}
