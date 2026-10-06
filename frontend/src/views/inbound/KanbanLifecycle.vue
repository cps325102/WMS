<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>看板生命周期查询</span>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>
    </template>
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="看板码/订单号/物料名称"
        clearable
        style="width: 300px"
        @keyup.enter="loadData"
      />
      <el-select
        v-model="statusFilter"
        placeholder="全部状态"
        clearable
        style="width: 140px"
        @change="loadData"
      >
        <el-option label="未打印" value="未打印" />
        <el-option label="已打印" value="已打印" />
        <el-option label="部分入库" value="部分入库" />
        <el-option label="已完成" value="已完成" />
        <el-option label="已出库" value="已出库" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="kanbanCode" label="看板码" width="180" />
      <el-table-column prop="orderNo" label="入库单号" width="190" />
      <el-table-column prop="materialCode" label="物料编码" width="120" />
      <el-table-column prop="materialName" label="物料名称" min-width="150" />
      <el-table-column label="入库进度" width="140">
        <template #default="{ row }">
          <span>{{ row.receivedQty }} / {{ row.planQty }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="locationName" label="库位" width="150" />
      <el-table-column prop="batchNo" label="批次号" width="150" />
      <el-table-column label="打印次数" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.printCount > 0" type="warning">{{ row.printCount }}次</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button type="warning" link @click="doPrintKanban(scope.row)">打印</el-button>
          <el-button type="primary" link @click="viewTrace(scope.row)">追溯</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listKanbans, printKanban } from '@/api/inbound'

const router = useRouter()
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const rows = ref([])

const loadData = async () => {
  loading.value = true
  const params = { keyword: keyword.value }
  if (statusFilter.value) params.status = statusFilter.value
  try {
    const res = await listKanbans(params)
    rows.value = res.data || []
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const map = {
    '未打印': 'info',
    '已打印': 'warning',
    '部分入库': 'primary',
    '已完成': 'success',
    '已出库': 'info'
  }
  return map[status] || ''
}

const doPrintKanban = async (row) => {
  try {
    await printKanban(row.id)
    router.push(`/inbound/kanban/print/${row.id}`)
  } catch (error) {
    ElMessage.error('打印失败')
  }
}

const viewTrace = (row) => {
  router.push(`/inbound/kanban/trace/${row.kanbanCode}`)
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