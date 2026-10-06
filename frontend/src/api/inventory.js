import request from '@/utils/request'

export function listInventory(params) {
  return request.get('/api/inventory/list', { params })
}

export function traceInventory(params) {
  return request.get('/api/inventory/trace', { params })
}
