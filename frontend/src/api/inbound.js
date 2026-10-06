import request from '@/utils/request'

export function createOrder(data) {
  return request.post('/api/inbound/order/create', data)
}

export function updateOrder(id, data) {
  return request.put(`/api/inbound/order/update/${id}`, data)
}

export function listOrders(params) {
  return request.get('/api/inbound/order/list', { params })
}

export function detailOrder(id) {
  return request.get(`/api/inbound/order/detail/${id}`)
}

export function deleteOrder(id) {
  return request.delete(`/api/inbound/order/delete/${id}`)
}

export function printOrder(id) {
  return request.get(`/api/inbound/order/print/${id}`)
}

export function generateKanban(orderId) {
  return request.post(`/api/inbound/kanban/generate/${orderId}`)
}

export function listKanbans(params) {
  return request.get('/api/inbound/kanban/list', { params })
}

export function printKanban(id) {
  return request.get(`/api/inbound/kanban/print/${id}`)
}

export function scanKanban(kanbanCode) {
  return request.get(`/api/inbound/kanban/scan/${kanbanCode}`)
}

export function receive(data) {
  return request.post('/api/inbound/receive', data)
}

export function kanbanTrace(kanbanCode) {
  return request.get(`/api/inbound/kanban/trace/${kanbanCode}`)
}
