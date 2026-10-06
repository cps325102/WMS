import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('@/views/Login.vue') },
  // 打印页面 — 独立渲染，不嵌套在 Layout 中
  { path: '/inbound/order/print/:id', component: () => import('@/views/inbound/InboundOrderPrint.vue'), meta: { title: '打印入库单' } },
  { path: '/inbound/kanban/print/:id', component: () => import('@/views/inbound/InboundKanbanPrint.vue'), meta: { title: '打印看板' } },
  { path: '/inbound/kanban/batch-print', component: () => import('@/views/inbound/InboundKanbanBatchPrint.vue'), meta: { title: '批量打印看板' } },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      { path: 'home', component: () => import('@/views/Home.vue') },
      { path: 'menu', component: () => import('@/views/MenuPage.vue') },
      { path: 'basic/material', component: () => import('@/views/basic/BasicCrud.vue'), meta: { title: '物料管理', type: 'material' } },
      { path: 'basic/supplier', component: () => import('@/views/basic/BasicCrud.vue'), meta: { title: '供应商管理', type: 'supplier' } },
      { path: 'basic/customer', component: () => import('@/views/basic/BasicCrud.vue'), meta: { title: '客户管理', type: 'customer' } },
      { path: 'basic/warehouse', component: () => import('@/views/basic/BasicCrud.vue'), meta: { title: '仓库管理', type: 'warehouse' } },
      { path: 'basic/location', component: () => import('@/views/basic/BasicCrud.vue'), meta: { title: '库位管理', type: 'location' } },
      { path: 'inbound/order/new', component: () => import('@/views/inbound/InboundOrderForm.vue'), meta: { title: '创建入库单' } },
      { path: 'inbound/order/edit/:id', component: () => import('@/views/inbound/InboundOrderForm.vue'), meta: { title: '修改入库单' } },
      { path: 'inbound/kanban/trace/:kanbanCode', component: () => import('@/views/inbound/KanbanTrace.vue'), meta: { title: '看板追溯' } },
      { path: 'inbound/kanban/lifecycle', component: () => import('@/views/inbound/KanbanLifecycle.vue'), meta: { title: '看板生命周期' } },
      { path: 'inventory/monitor', component: () => import('@/views/inventory/InventoryMonitor.vue'), meta: { title: '库存监控' } },
      { path: 'inventory/manage', component: () => import('@/views/inventory/InventoryTrace.vue'), meta: { title: '看板监控' } },
      { path: 'inventory/alerts', component: () => import('@/views/inventory/InventoryAlerts.vue'), meta: { title: 'AI智能预警' } },
      { path: 'inbound/manage', component: () => import('@/views/inbound/InboundManage.vue') },
      { path: 'outbound/orders', component: () => import('@/views/outbound/OutboundOrderList.vue'), meta: { title: '出库单管理' } },
      { path: 'outbound/order/new', component: () => import('@/views/outbound/OutboundOrderForm.vue'), meta: { title: '创建出库单' } },
      { path: 'outbound/order/edit/:id', component: () => import('@/views/outbound/OutboundOrderForm.vue'), meta: { title: '修改出库单' } },
      { path: 'outbound/ship/:id', component: () => import('@/views/outbound/OutboundShip.vue'), meta: { title: '扫码出库' } },
      { path: 'outbound/fifo-config', component: () => import('@/views/outbound/FifoConfig.vue'), meta: { title: 'FIFO配置' } },
      { path: 'freeze/manage', component: () => import('@/views/freeze/FreezeManage.vue'), meta: { title: '封存解封' } },
      { path: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '看板可视化' } },
      { path: 'history/logs', component: () => import('@/views/history/HistoryLogs.vue'), meta: { title: '库存流水' } },
      { path: 'pda/scan', component: () => import('@/views/pda/PdaScan.vue'), meta: { title: 'PDA扫码' } },
    ]
  }
]


const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/home')
    return
  }
  next()
})

export default router
