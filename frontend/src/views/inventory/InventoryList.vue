<script setup>
import { onMounted, ref } from 'vue'
import { listInventory } from '@/api/inventory'

const keyword = ref('')
const loading = ref(false)
const rows = ref([])

const loadData = async () => {
  loading.value = true
  const res = await listInventory({ keyword: keyword.value })
  rows.value = res.data || []
  loading.value = false
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>当前库存</span>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>
    </template>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="物料、仓库、库位、批次" clearable style="width: 320px" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="materialCode" label="物料编码" />
      <el-table-column prop="materialName" label="物料名称" />
      <el-table-column prop="warehouseName" label="仓库" />
      <el-table-column prop="locationName" label="库位" />
      <el-table-column prop="batchNo" label="批次号" />
      <el-table-column prop="qty" label="库存数量" />
      <el-table-column prop="updateTime" label="更新时间" width="170" />
    </el-table>
  </el-card>
</template>

<style scoped>
.page-header,
.toolbar {
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
