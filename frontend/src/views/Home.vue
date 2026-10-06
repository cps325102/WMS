<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover" @click="goToInventoryList">
          <div class="stat-title">当前库存</div>
          <div class="stat-value">{{ totalInventoryQty }} 件</div>
          <div class="stat-footer">点击查看详细库存</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" @click="goToInventoryTrace">
          <div class="stat-title">库存追溯</div>
          <div class="stat-value">{{ traceCount }} 条流水</div>
          <div class="stat-footer">点击查看最近变动</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="recent-trace">
      <template #header>
        <span>最近5条入库流水</span>
      </template>
      <el-table :data="recentTraces" border size="small">
        <el-table-column prop="recordNo" label="流水号" width="160" />
        <el-table-column prop="materialName" label="物料" />
        <el-table-column prop="changeQty" label="数量" width="80" />
        <el-table-column prop="operateTime" label="时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listInventory, traceInventory } from '@/api/inventory'

const router = useRouter()
const totalInventoryQty = ref(0)
const traceCount = ref(0)
const recentTraces = ref([])

const goToInventoryList = () => router.push('/inventory/manage')
const goToInventoryTrace = () => router.push('/inventory/manage')

onMounted(async () => {
  const invRes = await listInventory({ keyword: '' })
  const invList = invRes.data || []
  totalInventoryQty.value = invList.reduce((sum, item) => sum + (item.qty || 0), 0)

  const traceRes = await traceInventory({ keyword: '' })
  const traces = traceRes.data || []
  traceCount.value = traces.length
  recentTraces.value = traces.slice(0, 5)
})
</script>

<style scoped>
.stat-title {
  font-size: 14px;
  color: #606266;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin: 12px 0;
}
.stat-footer {
  font-size: 12px;
  color: #409eff;
}
.el-card {
  cursor: pointer;
  transition: all 0.3s;
}
.el-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.recent-trace {
  margin-top: 16px;
}
</style>