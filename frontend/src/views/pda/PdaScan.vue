<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>PDA 扫码</span>
        <el-tag v-if="connected" type="success">已连接</el-tag>
        <el-tag v-else type="danger">未连接</el-tag>
      </div>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="扫码查询" name="scan">
        <div style="display:flex;gap:8px;margin-bottom:12px">
          <el-input v-model="scanCode" placeholder="扫描看板码/条码" size="large" style="width:300px" @keyup.enter="doScan" />
          <el-button type="primary" @click="doScan" size="large">扫描</el-button>
        </div>
        <el-card v-if="scanResult" shadow="hover">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="看板码">{{ scanResult.kanbanCode }}</el-descriptions-item>
            <el-descriptions-item label="物料名称">{{ scanResult.materialName }}</el-descriptions-item>
            <el-descriptions-item label="批次号">{{ scanResult.batchNo }}</el-descriptions-item>
            <el-descriptions-item label="计划数量">{{ scanResult.planQty }}</el-descriptions-item>
            <el-descriptions-item label="已收数量">{{ scanResult.receivedQty }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ scanResult.status }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="拣货任务" name="pick">
        <el-button type="primary" @click="loadPickTasks" style="margin-bottom:12px">刷新</el-button>
        <el-table :data="pickTasks" border stripe v-loading="loading">
          <el-table-column prop="orderNo" label="出库单" width="150" />
          <el-table-column prop="materialCode" label="物料编码" width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="130" />
          <el-table-column prop="batchNo" label="批次号" width="150" />
          <el-table-column prop="locationName" label="库位" width="120" />
          <el-table-column prop="qty" label="可用库存" width="100" />
          <el-table-column prop="planQty" label="需求数量" width="100" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="库存查询" name="inv">
        <div style="display:flex;gap:8px;margin-bottom:12px">
          <el-input v-model="invKeyword" placeholder="物料编码/名称/批次/库位" style="width:250px" @keyup.enter="loadInv" />
          <el-button type="primary" @click="loadInv">查询</el-button>
        </div>
        <el-table :data="inventoryList" border stripe v-loading="loading">
          <el-table-column prop="materialCode" label="物料编码" width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="130" />
          <el-table-column prop="batchNo" label="批次号" width="150" />
          <el-table-column prop="locationName" label="库位" width="120" />
          <el-table-column prop="qty" label="数量" width="100">
            <template #default="{row}">
              <span style="font-weight:bold;color:#409eff">{{ row.qty }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="离线缓存" name="cache">
        <div style="display:flex;gap:8px;margin-bottom:12px">
          <el-input v-model="deviceId" placeholder="设备ID" style="width:200px" />
          <el-button type="primary" @click="loadCache">查看缓存</el-button>
          <el-button type="success" @click="syncData">同步数据</el-button>
        </div>
        <el-table :data="cacheList" border stripe>
          <el-table-column prop="opType" label="操作类型" width="120" />
          <el-table-column prop="syncStatus" label="同步状态" width="120">
            <template #default="{row}">
              <el-tag :type="row.syncStatus==='synced'?'success':'warning'">{{ row.syncStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="缓存时间" width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'

const activeTab = ref('scan')
const connected = ref(true)
const scanCode = ref('')
const scanResult = ref(null)
const loading = ref(false)
const pickTasks = ref([])
const invKeyword = ref('')
const inventoryList = ref([])
const deviceId = ref('pda-001')
const cacheList = ref([])

const doScan = async () => {
  if (!scanCode.value) return
  const res = await request.get(`/api/pda/scan/${scanCode.value}`)
  scanResult.value = res.data || null
}

const loadPickTasks = async () => {
  loading.value = true
  const res = await request.get('/api/pda/pick-tasks', { params: { deviceId: deviceId.value } })
  pickTasks.value = res.data || []
  loading.value = false
}

const loadInv = async () => {
  loading.value = true
  const res = await request.get('/api/pda/inventory', { params: { keyword: invKeyword.value } })
  inventoryList.value = res.data || []
  loading.value = false
}

const loadCache = async () => {
  const res = await request.get(`/api/pda/cache/${deviceId.value}`)
  cacheList.value = res.data || []
}

const syncData = async () => {
  await request.post(`/api/pda/sync/${deviceId.value}`)
  loadCache()
}
</script>

<style scoped>
.page-header { display:flex; align-items:center; justify-content:space-between; }
</style>
