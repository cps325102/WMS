import request from '@/utils/request'

export function listOutboundOrders(params) {
  return request.get('/api/outbound/order/list', { params })
}

export function detailOutboundOrder(id) {
  return request.get(`/api/outbound/order/detail/${id}`)
}

export function createOutboundOrder(data) {
  return request.post('/api/outbound/order/create', data)
}

export function updateOutboundOrder(id, data) {
  return request.put(`/api/outbound/order/update/${id}`, data)
}

export function deleteOutboundOrder(id) {
  return request.delete(`/api/outbound/order/delete/${id}`)
}

export function shipOutbound(data) {
  return request.post('/api/outbound/ship', data)
}

export function getAvailableInventory(materialId) {
  return request.get(`/api/outbound/inventory/available/${materialId}`)
}

export function scanKanban(kanbanCode) {
  return request.get(`/api/inbound/kanban/scan/${kanbanCode}`)
}
export function executeOutboundOrder(id) {
  return request.post(`/api/outbound/order/execute/${id}`)
}
export function getMaterialsWithStock() {
  return request.get('/api/outbound/materials/with-stock')
}