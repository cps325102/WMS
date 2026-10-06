import request from '@/utils/request'

export function getDashboardSummary() {
  return request.get('/api/dashboard/summary')
}

export function getInboundTrend(params) {
  return request.get('/api/dashboard/inbound-trend', { params })
}

export function getOutboundTrend(params) {
  return request.get('/api/dashboard/outbound-trend', { params })
}

export function getSlowMoving(params) {
  return request.get('/api/dashboard/slow-moving', { params })
}

export function getLocationHeatmap() {
  return request.get('/api/dashboard/heatmap')
}

export function getTurnover() {
  return request.get('/api/dashboard/turnover')
}

export function getCompletionRate() {
  return request.get('/api/dashboard/completion-rate')
}

export function getPendingTasks() {
  return request.get('/api/dashboard/pending-tasks')
}
