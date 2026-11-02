import request from '@/utils/request'

export function getProvinces() {
  return request.get('/destination/provinces')
}
