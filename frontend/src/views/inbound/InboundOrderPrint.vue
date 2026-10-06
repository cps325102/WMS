<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { printOrder } from '@/api/inbound'

const route = useRoute()
const router = useRouter()
const detail = ref({ items: [] })
const printPage = () => window.print()

onMounted(async () => {
  const res = await printOrder(route.params.id)
  detail.value = res.data || { items: [] }
})
</script>

<template>
  <el-card>
    <div class="actions no-print">
      <el-button @click="router.push('/inbound/manage')">返回</el-button>
      <el-button type="primary" @click="printPage">打印入库单</el-button>
    </div>
    <div class="print-page">
      <h2>入库单</h2>
      <el-descriptions border :column="2">
        <el-descriptions-item label="入库单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="入库类型">{{ detail.inboundType }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="计划日期">{{ detail.planDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="制单人">{{ detail.createBy }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.items" border style="margin-top: 16px">
        <el-table-column prop="materialCode" label="物料编码" />
        <el-table-column prop="materialName" label="物料名称" />
        <el-table-column prop="spec" label="规格" />
        <el-table-column prop="planQty" label="计划数量" />
        <el-table-column prop="receivedQty" label="已入库" />
        <el-table-column prop="unit" label="单位" />
        <el-table-column prop="warehouseName" label="仓库" />
        <el-table-column prop="locationName" label="库位" />
        <el-table-column prop="batchNo" label="批次号" />
      </el-table>
      <div class="sign-row">
        <span>制单：</span>
        <span>收货：</span>
        <span>复核：</span>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}
.print-page {
  background: #fff;
  padding: 24px;
}
h2 {
  text-align: center;
  margin: 0 0 20px;
}
.sign-row {
  display: flex;
  justify-content: space-around;
  margin-top: 40px;
}
@media print {
  .no-print {
    display: none;
  }
}
</style>
