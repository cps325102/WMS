<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { kanbanTrace } from '@/api/inbound'

const route = useRoute()
const router = useRouter()
const detail = ref({ kanban: {}, records: [] })

const loadData = async () => {
  const res = await kanbanTrace(route.params.kanbanCode)
  detail.value = res.data || { kanban: {}, records: [] }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>看板追溯</span>
        <el-button @click="router.push('/inbound/manage')">返回</el-button>
      </div>
    </template>
    <el-descriptions border :column="3">
      <el-descriptions-item label="看板编号">{{ detail.kanban.kanbanCode }}</el-descriptions-item>
      <el-descriptions-item label="入库单号">{{ detail.kanban.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ detail.kanban.status }}</el-descriptions-item>
      <el-descriptions-item label="物料">{{ detail.kanban.materialName }}</el-descriptions-item>
      <el-descriptions-item label="计划数量">{{ detail.kanban.planQty }}</el-descriptions-item>
      <el-descriptions-item label="已入库数量">{{ detail.kanban.receivedQty }}</el-descriptions-item>
      <el-descriptions-item label="仓库">{{ detail.kanban.warehouseName }}</el-descriptions-item>
      <el-descriptions-item label="库位">{{ detail.kanban.locationName }}</el-descriptions-item>
      <el-descriptions-item label="批次号">{{ detail.kanban.batchNo }}</el-descriptions-item>
    </el-descriptions>
    <h3>扫码入库记录</h3>
    <el-table :data="detail.records" border>
      <el-table-column prop="recordNo" label="流水号" width="190" />
      <el-table-column prop="changeQty" label="变动数量" />
      <el-table-column prop="beforeQty" label="变动前" />
      <el-table-column prop="afterQty" label="变动后" />
      <el-table-column prop="operator" label="操作人" />
      <el-table-column prop="operateTime" label="操作时间" width="170" />
    </el-table>
  </el-card>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
h3 {
  margin-top: 20px;
}
</style>
