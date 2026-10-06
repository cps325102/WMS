import request from '@/utils/request'

export function listHistoryLogs(params) {
  return request.get('/api/history/logs', { params })
}

export function traceByBatch(batchNo) {
  return request.get(`/api/history/trace/batch/${batchNo}`)
}

export function traceByMaterial(materialCode) {
  return request.get(`/api/history/trace/material/${materialCode}`)
}

export function exportHistoryCsv(params) {
  return request.get('/api/history/export', { params })
}
