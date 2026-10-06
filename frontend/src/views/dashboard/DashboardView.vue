<template>
  <div>
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6" v-for="card in summaryCards" :key="card.label">
        <el-card shadow="hover">
          <div class="card-value">{{ card.value }}</div>
          <div class="card-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="12">
        <el-card>
          <template #header>入库趋势</template>
          <div style="height:200px;display:flex;align-items:flex-end;gap:8px;padding:0 4px">
            <div v-for="d in inboundTrend" :key="d.date" :title="d.date+':'+d.qty"
              :style="{height: Math.max(4, (d.qty/maxIn*200))+'px', background:'#409eff', flex:1, minWidth:'20px', borderRadius:'4px 4px 0 0', transition:'height 0.3s'}">
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>出库趋势</template>
          <div style="height:200px;display:flex;align-items:flex-end;gap:8px;padding:0 4px">
            <div v-for="d in outboundTrend" :key="d.date" :title="d.date+':'+d.qty"
              :style="{height: Math.max(4, (d.qty/maxOut*200))+'px', background:'#67c23a', flex:1, minWidth:'20px', borderRadius:'4px 4px 0 0', transition:'height 0.3s'}">
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <template #header>完成率</template>
          <div style="text-align:center">
            <div style="margin:8px 0">入库完成率: {{ completionRate.inboundCompletion || 0 }}%</div>
            <div style="margin:8px 0">出库完成率: {{ completionRate.outboundCompletion || 0 }}%</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>库存周转率</template>
          <div style="text-align:center;font-size:24px;padding:16px">{{ turnover.turnoverRate || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>待办任务</template>
          <div v-if="pendingTasks.length===0" style="color:#999;padding:8px">暂无待办任务</div>
          <div v-for="t in pendingTasks.slice(0,5)" :key="t.no" style="padding:4px 0;font-size:13px">
            <el-tag :type="t.type==='入库'?'warning':'primary'" size="small">{{ t.type }}</el-tag>
            {{ t.no }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <template #header>滞销物料 TOP10</template>
      <el-table :data="slowMoving" v-loading="loading" border stripe>
        <el-table-column prop="materialCode" label="物料编码" width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="150" />
        <el-table-column prop="qty" label="库存数量" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboardSummary, getInboundTrend, getOutboundTrend, getSlowMoving, getTurnover, getCompletionRate, getPendingTasks } from '@/api/dashboard'

const loading = ref(false)
const summaryCards = ref([])
const inboundTrend = ref([])
const outboundTrend = ref([])
const slowMoving = ref([])
const turnover = ref({})
const completionRate = ref({})
const pendingTasks = ref([])
const maxIn = ref(1)
const maxOut = ref(1)

onMounted(async () => {
  loading.value = true
  try {
    const [sumRes, inRes, outRes, slowRes, turnRes, compRes, taskRes] = await Promise.all([
      getDashboardSummary(), getInboundTrend(), getOutboundTrend(),
      getSlowMoving(), getTurnover(), getCompletionRate(), getPendingTasks()
    ])
    const s = sumRes.data || {}
    summaryCards.value = [
      { label: '待入库', value: s.pendingInbound || 0 },
      { label: '待出库', value: s.pendingOutbound || 0 },
      { label: '当前库存', value: s.totalInventory || 0 },
      { label: '待办任务', value: s.pendingTasks || 0 }
    ]
    inboundTrend.value = inRes.data || []
    outboundTrend.value = outRes.data || []
    slowMoving.value = slowRes.data || []
    turnover.value = turnRes.data || {}
    completionRate.value = compRes.data || {}
    pendingTasks.value = taskRes.data || []
    maxIn.value = Math.max(1, ...inboundTrend.value.map(d => Number(d.qty)))
    maxOut.value = Math.max(1, ...outboundTrend.value.map(d => Number(d.qty)))
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.card-value { font-size: 28px; font-weight: bold; color: #409eff; }
.card-label { font-size: 14px; color: #666; margin-top: 4px; }
</style>
