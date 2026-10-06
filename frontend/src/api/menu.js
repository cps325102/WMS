import request from '@/utils/request'

export function getMenuList() {
  return request.get('/api/menu/list')
}
