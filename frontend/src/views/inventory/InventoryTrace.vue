<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>看板监控</span>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="看板编号/物料/单号" clearable style="width: 260px" @keyup.enter="loadData" />
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px" @change="loadData">
        <el-option label="未打印" value="未打印" />
        <el-option label="已打印" value="已打印" />
        <el-option label="部分入库" value="部分入库" />
        <el-option label="已入库" value="已完成" />
        <el-option label="已出库" value="已出库" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe max-height="600">
      <el-table-column prop="kanbanCode" label="看板编号" width="180" />
      <el-table-column prop="materialCode" label="物料编码" width="110" />
      <el-table-column prop="materialName" label="物料名称" min-width="130" show-overflow-tooltip />
      <el-table-column prop="orderNo" label="入库单号" width="160" />
      <el-table-column prop="packageInfo" label="包装" width="80" align="center" />
      <el-table-column prop="planQty" label="计划数量" width="90" align="center" />
      <el-table-column prop="receivedQty" label="已入库" width="90" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="printCount" label="打印次数" width="80" align="center" />
      <el-table-column prop="lastPrintTime" label="最近打印" width="160" />
    </el-table>

    <el-empty v-if="rows.length === 0 && !loading" description="暂无看板数据" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listKanbans } from '@/api/inbound'

const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const rows = ref([])

const loadData = async () => {
  loading.value = true
  const params = {}
  if (keyword.value) params.keyword = keyword.value
  if (statusFilter.value) params.status = statusFilter.value
  const res = await listKanbans(params)
  rows.value = res.data || []
  loading.value = false
}

const reset = () => {
  keyword.value = ''
  statusFilter.value = ''
  loadData()
}

const statusLabel = (s) => {
  const m = { '已完成': '已入库' }
  return m[s] || s
}
const statusType = (s) => {
  const m = { '未打印':'info','已打印':'warning','部分入库':'','已完成':'success','已出库':'danger' }
  return m[s] || 'info'
}

onMounted(loadData)
</script>

<style scoped>
.page-header, .toolbar { display:flex; align-items:center; justify-content:space-between; gap:12px; }
.toolbar { justify-content:flex-start; margin-bottom:16px; }
</style>
