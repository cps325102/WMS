<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>FIFO 先进先出配置</span>
      </div>
    </template>

    <div style="margin-bottom:16px">
      <div style="margin-bottom:8px">当前模式: 
        <el-tag :type="currentMode==='strict'?'danger':'success'" size="large">
          {{ currentMode==='strict'?'严格模式':'宽松模式' }}
        </el-tag>
      </div>
      <div style="color:#999;font-size:13px;margin-bottom:12px">
        {{ currentMode==='strict'?'严格模式：强制按批次先后顺序出库，不可调整':'宽松模式：优先推荐先进批次，允许手动调整' }}
      </div>
      <el-button v-if="currentMode==='strict'" type="warning" @click="switchMode('loose')">切换为宽松模式</el-button>
      <el-button v-else type="danger" @click="switchMode('strict')">切换为严格模式</el-button>
    </div>

    <el-divider />

    <div style="font-weight:bold;margin-bottom:8px">配置历史</div>
    <el-table :data="configs" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="mode" label="模式" width="120">
        <template #default="{row}">
          <el-tag :type="row.mode==='strict'?'danger':'success'">{{ row.mode==='strict'?'严格':'宽松' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="active" label="是否启用" width="100">
        <template #default="{row}">
          <el-tag :type="row.active==='Y'?'success':'info'">{{ row.active==='Y'?'是':'否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFifoConfig, listFifoConfigs, switchFifoMode } from '@/api/fifo'

const loading = ref(false)
const currentMode = ref('loose')
const configs = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const [cfgRes, listRes] = await Promise.all([getFifoConfig(), listFifoConfigs()])
    currentMode.value = cfgRes.data?.mode || 'loose'
    configs.value = listRes.data || []
  } finally {
    loading.value = false
  }
}

const switchMode = async (mode) => {
  await switchFifoMode(mode)
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page-header { display:flex; align-items:center; justify-content:space-between; }
</style>
