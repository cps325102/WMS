<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>库存流水查询</span>
        <el-button type="success" @click="exportCsv">导出CSV</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-select v-model="businessType" placeholder="业务类型" clearable style="width:120px" @change="loadData">
        <el-option label="入库" value="入库" />
        <el-option label="出库" value="出库" />
        <el-option label="冻结" value="冻结" />
        <el-option label="解冻" value="解冻" />
        <el-option label="盘点" value="盘点" />
      </el-select>
      <el-input v-model="keyword" placeholder="物料编码/名称/批次/单号" clearable style="width:250px" @keyup.enter="loadData" />
      <el-input v-model="operator" placeholder="操作人" clearable style="width:120px" @keyup.enter="loadData" />
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe max-height="500">
      <el-table-column prop="logType" label="类型" width="80">
        <template #default="{row}">
          <el-tag :type="row.logType==='入库'?'success':row.logType==='出库'?'primary':'info'" size="small">{{ row.logType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="businessNo" label="单据编号" width="150" />
      <el-table-column prop="materialCode" label="物料编码" width="120" />
      <el-table-column prop="materialName" label="物料名称" min-width="130" />
      <el-table-column prop="batchNo" label="批次号" width="150" />
      <el-table-column prop="changeQty" label="变动数量" width="100">
        <template #default="{row}">
          <span :style="{color:Number(row.changeQty)>0?'#67c23a':'#f56c6c',fontWeight:'bold'}">{{ row.changeQty }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="operator" label="操作人" width="100" />
      <el-table-column prop="operateTime" label="操作时间" width="160" />
      <el-table-column prop="remark" label="备注" min-width="120" />
    </el-table>

    <el-empty v-if="rows.length===0 && !loading" description="暂无数据" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listHistoryLogs, exportHistoryCsv } from '@/api/history'

const loading = ref(false)
const businessType = ref('')
const keyword = ref('')
const operator = ref('')
const dateRange = ref(null)
const rows = ref([])

const loadData = async () => {
  loading.value = true
  const params = {}
  if (businessType.value) params.businessType = businessType.value
  if (keyword.value) params.keyword = keyword.value
  if (operator.value) params.operator = operator.value
  if (dateRange.value) {
    params.startTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  }
  const res = await listHistoryLogs(params)
  rows.value = res.data || []
  loading.value = false
}

const exportCsv = async () => {
  const params = {}
  if (businessType.value) params.businessType = businessType.value
  if (keyword.value) params.keyword = keyword.value
  if (operator.value) params.operator = operator.value
  if (dateRange.value) {
    params.startTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  }
  const res = await exportHistoryCsv(params)
  const blob = new Blob(['\uFEFF' + (res.data || '')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'history.csv'; a.click()
  URL.revokeObjectURL(url)
}

onMounted(loadData)
</script>

<style scoped>
.page-header, .toolbar { display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap; }
.toolbar { justify-content:flex-start; margin-bottom:12px; }
</style>
