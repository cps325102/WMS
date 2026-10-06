<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>AI智能预警</span>
        <div class="header-actions">
          <el-button @click="openConfig">参数配置</el-button>
          <el-button type="primary" @click="checkNow">立即计算</el-button>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <el-select v-model="typeFilter" placeholder="预警类型" clearable style="width:140px" @change="loadData">
        <el-option label="缺货预警" value="缺货预警" />
        <el-option label="呆滞预警" value="呆滞预警" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width:130px" @change="loadData">
        <el-option label="待处理" value="待处理" />
        <el-option label="已处理" value="已处理" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="alertType" label="预警类型" width="110">
        <template #default="{row}">
          <el-tag :type="row.alertType==='缺货预警'?'danger':'warning'">{{ row.alertType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="level" label="级别" width="80">
        <template #default="{row}">
          <el-tag :type="row.level==='高'?'danger':'warning'">{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="materialCode" label="物料编码" width="120" />
      <el-table-column prop="materialName" label="物料名称" min-width="130" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="batchNo" label="批次号" width="140" />
      <el-table-column prop="availableQty" label="可用库存" width="100" />
      <el-table-column prop="thresholdQty" label="安全阈值" width="100" />
      <el-table-column prop="dailyAvgOut" label="日均出库" width="100" />
      <el-table-column prop="message" label="判断依据" min-width="190" />
      <el-table-column prop="suggestion" label="AI建议" min-width="190" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status==='已处理'?'info':'danger'">{{ row.status||'待处理' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{row}">
          <el-button v-if="row.status!=='已处理'" type="primary" link @click="handleAlert(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="rows.length===0 && !loading" description="暂无预警" />

    <el-dialog v-model="showConfigDialog" title="预警参数配置" width="760px">
      <div class="toolbar">
        <el-select v-model="configForm.materialId" filterable placeholder="选择物料" style="width:260px">
          <el-option v-for="m in materials" :key="m.id" :label="`${m.code || m.materialCode} ${m.name || m.materialName}`" :value="m.id" />
        </el-select>
        <el-input-number v-model="configForm.leadTimeDays" :min="1" />
        <el-input-number v-model="configForm.stagnantDays" :min="30" />
        <el-switch v-model="configForm.enabled" :active-value="1" :inactive-value="0" />
        <el-button type="primary" @click="saveConfig">保存</el-button>
      </div>
      <el-table :data="configs" border size="small">
        <el-table-column prop="materialCode" label="物料编码" width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="leadTimeDays" label="到货周期(天)" width="120" />
        <el-table-column prop="stagnantDays" label="呆滞天数" width="100" />
        <el-table-column label="启用" width="80">
          <template #default="{row}">{{ Number(row.enabled) === 0 ? '否' : '是' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button type="primary" link @click="editConfig(row)">编辑</el-button>
            <el-button type="danger" link @click="removeConfig(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listAlertLogs, checkAlerts, handleAlert as handleAlertApi, listAlertConfigs, saveAlertConfig, deleteAlertConfig } from '@/api/monitor'
import { listMaterials } from '@/api/basic'

const loading = ref(false)
const typeFilter = ref('')
const statusFilter = ref('')
const rows = ref([])
const showConfigDialog = ref(false)
const configs = ref([])
const materials = ref([])
const configForm = ref({ materialId: null, leadTimeDays: 7, stagnantDays: 180, enabled: 1 })

const loadData = async () => {
  loading.value = true
  const params = {}
  if (typeFilter.value) params.type = typeFilter.value
  if (statusFilter.value) params.status = statusFilter.value
  const res = await listAlertLogs(params)
  rows.value = res.data || []
  loading.value = false
}

const checkNow = async () => {
  await checkAlerts()
  await loadData()
}

const handleAlert = async (row) => {
  await handleAlertApi(row.id, { handler: 'admin', action: '已确认并安排处理', remark: row.suggestion || '已处理' })
  await loadData()
}

const openConfig = async () => {
  showConfigDialog.value = true
  await loadConfigs()
  if (materials.value.length === 0) {
    const res = await listMaterials()
    materials.value = res.data || []
  }
}

const loadConfigs = async () => {
  const res = await listAlertConfigs()
  configs.value = res.data || []
}

const saveConfig = async () => {
  await saveAlertConfig(configForm.value)
  configForm.value = { materialId: null, leadTimeDays: 7, stagnantDays: 180, enabled: 1 }
  await loadConfigs()
}

const editConfig = (row) => {
  configForm.value = { ...row }
}

const removeConfig = async (row) => {
  await deleteAlertConfig(row.id)
  await loadConfigs()
}

onMounted(loadData)
</script>

<style scoped>
.page-header,
.toolbar,
.header-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.toolbar {
  justify-content: flex-start;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
</style>
