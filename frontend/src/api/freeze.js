import request from '@/utils/request'

export function listFreezeRecords(params) {
  return request.get('/api/freeze/list', { params })
}

export function freezeDetail(id) {
  return request.get(`/api/freeze/detail/${id}`)
}

export function createFreeze(data) {
  return request.post('/api/freeze/create', data)
}

export function unfreeze(freezeId, params) {
  return request.post(`/api/freeze/unfreeze/${freezeId}`, null, { params })
}

export function listRepackRecords(params) {
  return request.get('/api/freeze/repack/list', { params })
}

export function repackInventory(data) {
  return request.post('/api/freeze/repack', data)
}
