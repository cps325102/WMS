import request from '@/utils/request'

export function getMonitorOverview() {
  return request.get('/api/monitor/overview')
}

export function getLocationSummary() {
  return request.get('/api/monitor/location-summary')
}

export function checkAlerts() {
  return request.get('/api/monitor/alerts/check')
}

export function listAlertLogs(params) {
  return request.get('/api/monitor/alerts/list', { params })
}

export function handleAlert(id, params) {
  return request.post(`/api/monitor/alerts/handle/${id}`, null, { params })
}

export function listAlertConfigs() {
  return request.get('/api/monitor/alerts/configs')
}

export function saveAlertConfig(data) {
  return request.post('/api/monitor/alerts/configs', data)
}

export function deleteAlertConfig(id) {
  return request.delete(`/api/monitor/alerts/configs/${id}`)
}
