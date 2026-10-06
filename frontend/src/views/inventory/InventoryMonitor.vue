<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>库存监控</span>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>
    </template>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="物料编码/名称/仓库/库位/批次" clearable style="width: 300px" @keyup.enter="loadData" />
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
        <el-option label="已入库" value="已入库" />
        <el-option label="已耗尽" value="已耗尽" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="materialCode" label="物料编码" width="120" />
      <el-table-column prop="materialName" label="物料名称" min-width="150" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="locationName" label="库位" width="150" />
      <el-table-column prop="batchNo" label="批次号" width="150" />
      <el-table-column label="库存数量" width="120">
        <template #default="{ row }">
          <span :style="{ color: row.qty > 0 ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">{{ row.qty }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === '已入库' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="viewTrace(scope.row)">流水追溯</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="rows.length === 0 && !loading" description="暂无库存数据，请先完成入库操作" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listInventory } from '@/api/inventory'

const router = useRouter()
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const rows = ref([])

const loadData = async () => {
  loading.value = true
  const params = { keyword: keyword.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await listInventory(params)
  rows.value = res.data || []
  loading.value = false
}

const viewTrace = (row) => {
  // 只传递物料编码和批次号作为查询关键词，避免过于复杂
  const searchKeyword = `${row.materialCode} ${row.batchNo}`

  router.push({
    path: '/inventory/manage',
    query: {
      keyword: searchKeyword.trim()
    }
  })
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
