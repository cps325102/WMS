import request from '@/utils/request'

export function listBasic(type, params) {
  return request.get(`/api/basic/${type}/list`, { params })
}

export function saveBasic(type, data) {
  return request.post(`/api/basic/${type}/save`, data)
}

export function deleteBasic(type, id) {
  return request.delete(`/api/basic/${type}/delete/${id}`)
}

export function listMaterials(params) {
  return request.get('/api/basic/material/list', { params })
}
