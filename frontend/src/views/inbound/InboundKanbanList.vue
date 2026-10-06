<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listKanbans } from '@/api/inbound'

const router = useRouter()
const keyword = ref('')
const materialFilter = ref('')
const loading = ref(false)
const rows = ref([])
const selectedKanbans = ref([])

// 将扁平看板列表转为按物料分组的树形数据
const treeData = computed(() => {
  const rawRows = materialFilter.value
    ? rows.value.filter(r => r.materialCode === materialFilter.value)
    : rows.value

  const map = new Map()
  rawRows.forEach(k => {
    const key = k.materialCode
    if (!map.has(key)) {
      map.set(key, {
        id: 'group-' + key,
        materialCode: key,
        materialName: k.materialName,
        orderNo: k.orderNo,
        batchNo: k.batchNo,
        children: [],
        _isGroup: true
      })
    }
    const group = map.get(key)
    group.children.push({
      ...k,
      _isGroup: false
    })
    // 更新组汇总
    group.totalPlanQty = (group.totalPlanQty || 0) + (k.planQty || 0)
    group.totalReceivedQty = (group.totalReceivedQty || 0) + (k.receivedQty || 0)
  })
  // 组内按库位排序
  map.forEach(g => {
    g.children.sort((a, b) => (a.locationName || '').localeCompare(b.locationName || ''))
  })
  return [...map.values()].sort((a, b) => a.materialCode.localeCompare(b.materialCode))
})

// 去重物料类型列表（用于筛选下拉）
const materialTypes = computed(() => {
  const codes = new Set()
  rows.value.forEach(r => codes.add(r.materialCode))
  return [...codes].map(code => ({
    code,
    name: rows.value.find(r => r.materialCode === code)?.materialName || code
  }))
})

const statusFilter = ref('')

const loadData = async () => {
  loading.value = true
  const params = { keyword: keyword.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await listKanbans(params)
  rows.value = res.data || []
  loading.value = false
}

// 处理选择变化 — 只收集叶子节点（看板行）
const handleSelectionChange = (val) => {
  selectedKanbans.value = val.filter(row => !row._isGroup)
}

// 判断行是否可选（分组行不可选）
const selectable = (row) => !row._isGroup

const batchPrint = () => {
  if (selectedKanbans.value.length === 0) {
    ElMessage.warning('请至少选择一个看板')
    return
  }
  const ids = selectedKanbans.value.map(k => k.id).join(',')
  router.push({ path: '/inbound/kanban/batch-print', query: { ids } })
}

// 暴露方法给父组件
defineExpose({
  setKeyword: (val) => { keyword.value = val },
  setMaterialFilter: (val) => { materialFilter.value = val },
  loadData
})

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>入库看板</span>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>
    </template>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="看板编号、入库单号、物料、状态" clearable style="width: 300px" @keyup.enter="loadData" />
      <el-select v-model="materialFilter" placeholder="按物料筛选" clearable style="width: 200px" @change="() => {}">
        <el-option v-for="mt in materialTypes" :key="mt.code" :label="`${mt.code} ${mt.name}`" :value="mt.code" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px" @change="loadData">
        <el-option label="未打印" value="未打印" />
        <el-option label="已打印" value="已打印" />
        <el-option label="部分入库" value="部分入库" />
        <el-option label="已完成" value="已完成" />
        <el-option label="已出库" value="已出库" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" :disabled="selectedKanbans.length === 0" @click="batchPrint">
        批量打印 ({{ selectedKanbans.length }})
      </el-button>
    </div>
    <el-table
      v-loading="loading"
      :data="treeData"
      border
      row-key="id"
      :tree-props="{ children: 'children' }"
      :selectable="selectable"
      default-expand-all
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column label="物料 / 看板编号" min-width="280">
        <template #default="{ row }">
          <template v-if="row._isGroup">
            <span class="group-label">
              <el-icon><Folder /></el-icon>
              <strong>{{ row.materialCode }}</strong>
              <span class="group-name">{{ row.materialName }}</span>
              <el-tag size="small" type="warning" effect="plain">{{ row.children.length }} 张看板</el-tag>
            </span>
          </template>
          <template v-else>
            <span class="kanban-code-cell">{{ row.kanbanCode }}</span>
            <span v-if="row.packageInfo" class="package-badge">{{ row.packageInfo }}</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="本包数量" width="100" align="center">
        <template #default="{ row }">
          <template v-if="row._isGroup">
            <span class="group-summary">{{ row.totalPlanQty }}</span>
          </template>
          <template v-else>
            {{ row.planQty }}
          </template>
        </template>
      </el-table-column>
      <el-table-column label="已入库" width="100" align="center">
        <template #default="{ row }">
          <template v-if="row._isGroup">
            <span class="group-summary">{{ row.totalReceivedQty }} / {{ row.totalPlanQty }}</span>
          </template>
          <template v-else>
            {{ row.receivedQty }}
          </template>
        </template>
      </el-table-column>
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="locationName" label="库位" width="130" />
      <el-table-column prop="batchNo" label="批次号" width="160" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <template v-if="row._isGroup">
            <el-tag size="small" type="info">分组</el-tag>
          </template>
          <template v-else>
            <el-tag size="small" :type="row.status === '已完成' ? 'success' : row.status === '已打印' ? 'warning' : row.status === '未打印' ? 'info' : ''">
              {{ row.status }}
            </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="!row._isGroup">
            <el-button type="warning" link @click="router.push(`/inbound/kanban/print/${row.id}`)">打印</el-button>
            <el-button type="primary" link @click="router.push(`/inbound/kanban/trace/${row.kanbanCode}`)">追溯</el-button>
          </template>
        </template>
      </el-table-column>
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
.group-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.group-name {
  color: #606266;
}
.kanban-code-cell {
  font-family: monospace;
  font-size: 13px;
  margin-left: 8px;
}
.package-badge {
  margin-left: 8px;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 1px 6px;
  border-radius: 4px;
}
.group-summary {
  font-weight: 600;
  color: #303133;
}
</style>
