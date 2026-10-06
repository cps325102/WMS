<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>入库单管理</span>
        <el-button type="primary" @click="router.push('/inbound/order/new')">创建入库单</el-button>
      </div>
    </template>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="单号/供应商" clearable style="width: 200px" @keyup.enter="loadData" />
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
        <el-option label="待入库" value="待入库" />
        <el-option label="部分入库" value="部分入库" />
        <el-option label="已完成" value="已完成" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border row-key="id" @expand-change="loadItemsForRow">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content" v-loading="row._loadingItems">
            <el-empty v-if="!row._items || row._items.length === 0" description="暂无明细" :image-size="40" />
            <div v-else v-for="group in row._materialGroups" :key="group.materialCode" class="material-sub-group">
              <h5 class="sub-group-title">
                <el-icon><Folder /></el-icon>
                {{ group.materialCode }} {{ group.materialName }}
                <el-tag size="small" type="warning" effect="plain">{{ group.items.length }} 项</el-tag>
                <span class="sub-summary">计划 {{ group.totalPlanQty }} / 已入库 {{ group.totalReceivedQty }}</span>
              </h5>
              <el-table :data="group.items" border size="small">
                <el-table-column prop="materialCode" label="物料编码" width="120" />
                <el-table-column prop="materialName" label="物料名称" min-width="140" />
                <el-table-column prop="spec" label="规格" width="100" />
                <el-table-column label="计划数量" width="100" align="center">
                  <template #default="{ row: r }">{{ r.planQty }} {{ r.unit }}</template>
                </el-table-column>
                <el-table-column label="已入库" width="100" align="center">
                  <template #default="{ row: r }">
                    <span :class="{ 'done': r.receivedQty >= r.planQty }">{{ r.receivedQty }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="warehouseName" label="仓库" width="120" />
                <el-table-column prop="locationName" label="库位" width="130" />
                <el-table-column prop="batchNo" label="批次号" width="150" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row: r }">
                    <el-tag size="small" :type="r.status === '已完成' ? 'success' : r.status === '部分入库' ? 'warning' : 'info'">
                      {{ r.status }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="入库单号" width="190" />
      <el-table-column prop="inboundType" label="入库类型" />
      <el-table-column prop="supplierName" label="供应商" min-width="160" />
      <el-table-column prop="planDate" label="计划日期" width="130" />
      <el-table-column label="入库进度" width="140">
        <template #default="{ row }">
          <span>{{ row.totalReceivedQty || 0 }} / {{ row.totalPlanQty || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="router.push(`/inbound/order/edit/${scope.row.id}`)">编辑</el-button>
          <el-button type="success" link @click="createKanban(scope.row)">生成看板</el-button>
          <el-button type="warning" link @click="printKanbans(scope.row)">打印看板</el-button>
          <el-button type="danger" link @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOrders, deleteOrder, generateKanban, listKanbans, detailOrder } from '@/api/inbound'

const router = useRouter()
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const rows = ref([])

const loadData = async () => {
  loading.value = true
  const params = { keyword: keyword.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await listOrders(params)
  rows.value = (res.data || []).map(row => ({
    ...row,
    _items: null,
    _materialGroups: null,
    _loadingItems: false
  }))
  loading.value = false
}

// 展开行时加载明细并按物料分组
const loadItemsForRow = async (row, expandedRows) => {
  const isExpanding = expandedRows.some(r => r.id === row.id)
  if (!isExpanding) return
  if (row._items) return  // 已加载
  row._loadingItems = true
  try {
    const res = await detailOrder(row.id)
    const items = res.data?.items || []
    row._items = items
    // 按物料编码分组
    row._materialGroups = buildMaterialGroups(items)
  } catch (e) {
    console.error('加载明细失败', e)
  } finally {
    row._loadingItems = false
  }
}

const buildMaterialGroups = (items) => {
  const map = new Map()
  items.forEach(item => {
    const key = item.materialCode
    if (!map.has(key)) {
      map.set(key, {
        materialCode: key,
        materialName: item.materialName,
        items: [],
        totalPlanQty: 0,
        totalReceivedQty: 0
      })
    }
    const g = map.get(key)
    g.items.push(item)
    g.totalPlanQty += (item.planQty || 0)
    g.totalReceivedQty += (item.receivedQty || 0)
  })
  map.forEach(g => {
    g.items.sort((a, b) => (a.locationName || '').localeCompare(b.locationName || ''))
  })
  return [...map.values()].sort((a, b) => a.materialCode.localeCompare(b.materialCode))
}

const remove = async (row) => {
  await ElMessageBox.confirm('确认删除当前入库单吗？', '提示')
  await deleteOrder(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const createKanban = async (row) => {
  try {
    const res = await generateKanban(row.id)
    const count = (res.data || []).length
    if (count > 0) {
      ElMessage.success(`已生成 ${count} 张看板`)
    } else {
      ElMessage.info('看板已存在，无需重复生成')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '生成看板失败')
  }
}

const printKanbans = async (row) => {
  try {
    const genRes = await generateKanban(row.id)
    let kanbans = genRes.data || []
    if (kanbans.length === 0) {
      const listRes = await listKanbans({ keyword: row.orderNo })
      kanbans = listRes.data || []
    }
    if (kanbans.length === 0) {
      ElMessage.warning('该入库单暂无可打印的看板')
      return
    }
    if (kanbans.length === 1) {
      router.push(`/inbound/kanban/print/${kanbans[0].id}`)
    } else {
      ElMessageBox.confirm(
        `该入库单有 ${kanbans.length} 个看板，是否逐个打印？`,
        '提示',
        {
          confirmButtonText: '逐个打印',
          cancelButtonText: '查看列表',
          type: 'info'
        }
      ).then(() => {
        const allIds = kanbans.map(k => k.id).join(',')
        router.push({ path: `/inbound/kanban/print/${kanbans[0].id}`, query: { ids: allIds } })
      }).catch((action) => {
        if (action === 'cancel') {
          router.replace({
            path: '/inbound/manage',
            query: { tab: 'kanbans', keyword: row.orderNo, _t: Date.now() }
          })
        }
      })
    }
  } catch (e) {
    console.error('打印看板失败', e)
    ElMessage.error('获取看板信息失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header, .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.toolbar {
  justify-content: flex-start;
  margin-bottom: 12px;
}
.expand-content {
  padding: 12px 20px;
  background: #fafafa;
}
.material-sub-group {
  margin-bottom: 16px;
}
.material-sub-group:last-child {
  margin-bottom: 0;
}
.sub-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px;
  font-size: 14px;
}
.sub-summary {
  font-weight: normal;
  font-size: 13px;
  color: #909399;
  margin-left: auto;
}
.done {
  color: #67c23a;
  font-weight: 600;
}
</style>
