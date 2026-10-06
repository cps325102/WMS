import request from '@/utils/request'

export function getFifoConfig() {
  return request.get('/api/fifo/config')
}

export function listFifoConfigs() {
  return request.get('/api/fifo/list')
}

export function switchFifoMode(mode) {
  return request.post(`/api/fifo/switch/${mode}`)
}

export function updateFifoConfig(id, data) {
  return request.put(`/api/fifo/update/${id}`, data)
}
