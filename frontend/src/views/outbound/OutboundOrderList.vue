<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>出库单管理</span>
        <el-button type="primary" @click="router.push('/outbound/order/new')">创建出库单</el-button>
      </div>
    </template>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="单号/客户" clearable style="width: 200px" @keyup.enter="loadData" />
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
        <el-option label="待出库" value="待出库" />
        <el-option label="部分出库" value="部分出库" />
        <el-option label="已完成" value="已完成" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="orderNo" label="出库单号" width="190" />
      <el-table-column prop="outboundType" label="出库类型" />
      <el-table-column prop="customerName" label="客户" min-width="180" />
      <el-table-column prop="planDate" label="计划日期" width="130" />
      <el-table-column label="出库进度" width="140">
        <template #default="{ row }">
          <span>{{ row.totalShippedQty || 0 }} / {{ row.totalPlanQty || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="350" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="router.push(`/outbound/order/edit/${scope.row.id}`)">编辑</el-button>
          <el-button type="warning" link @click="router.push(`/outbound/ship/${scope.row.id}`)">扫码出库</el-button>
          <el-button type="success" link @click="executeOutbound(scope.row)">执行出库</el-button>
          <el-button type="danger" link @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 先进先出明细弹窗 -->
  <el-dialog v-model="fifoDialogVisible" title="先进先出出库明细" width="600px">
    <el-table :data="fifoDetails" border>
      <el-table-column prop="materialName" label="物料" min-width="150" />
      <el-table-column prop="batchNo" label="批次号" width="160" />
      <el-table-column prop="warehouseName" label="仓库" />
      <el-table-column prop="locationName" label="库位" />
      <el-table-column prop="deductQty" label="出库数量" width="100" />
    </el-table>
    <template #footer>
      <el-button @click="fifoDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOutboundOrders, deleteOutboundOrder, executeOutboundOrder } from '@/api/outbound'

const router = useRouter()
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const rows = ref([])
const fifoDialogVisible = ref(false)
const fifoDetails = ref([])

const loadData = async () => {
  loading.value = true
  const params = { keyword: keyword.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await listOutboundOrders(params)
  rows.value = res.data || []
  loading.value = false
}

const remove = async (row) => {
  await ElMessageBox.confirm('确认删除当前出库单吗？', '提示')
  await deleteOutboundOrder(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const executeOutbound = async (row) => {
  try {
    const res = await executeOutboundOrder(row.id)
    if (res.data && res.data.allocationDetails && res.data.allocationDetails.length > 0) {
      fifoDetails.value = res.data.allocationDetails
      fifoDialogVisible.value = true
    } else {
      ElMessage.success('出库成功')
    }
    loadData()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '执行失败')
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
</style>
