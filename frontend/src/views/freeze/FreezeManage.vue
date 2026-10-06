<template>
  <div class="freeze-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>封存与转包</span>
          <el-button type="primary" @click="loadAll">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="当前库存" name="stock">
          <div class="toolbar">
            <el-input v-model="inventoryKeyword" placeholder="物料/仓库/库位/批次" clearable style="width: 280px" @keyup.enter="loadInventory" />
            <el-button type="primary" @click="loadInventory">查询</el-button>
          </div>
          <el-table :data="inventories" v-loading="inventoryLoading" border stripe>
            <el-table-column prop="materialCode" label="物料编码" width="120" />
            <el-table-column prop="materialName" label="物料名称" min-width="150" />
            <el-table-column prop="warehouseName" label="仓库" width="140" />
            <el-table-column prop="locationName" label="库位" width="130" />
            <el-table-column prop="batchNo" label="批次号" width="150" />
            <el-table-column prop="qty" label="库存" width="90" />
            <el-table-column prop="frozenQty" label="已封存" width="90" />
            <el-table-column label="可用" width="90">
              <template #default="{ row }">{{ availableQty(row) }}</template>
            </el-table-column>
            <el-table-column prop="unit" label="单位" width="70" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="warning" link :disabled="availableQty(row) <= 0" @click="openFreeze(row)">封存</el-button>
                <el-button type="primary" link :disabled="availableQty(row) <= 0" @click="openRepack(row)">转包</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="封存记录" name="freeze">
          <div class="toolbar">
            <el-input v-model="keyword" placeholder="物料编码/名称/批次" clearable style="width:250px" @keyup.enter="loadFreezeRecords" />
            <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width:130px" @change="loadFreezeRecords">
              <el-option label="已冻结" value="已冻结" />
              <el-option label="已解冻" value="已解冻" />
            </el-select>
            <el-button type="primary" @click="loadFreezeRecords">查询</el-button>
          </div>
          <el-table :data="rows" v-loading="loading" border stripe>
            <el-table-column prop="freezeNo" label="封存单号" width="150" />
            <el-table-column prop="materialCode" label="物料编码" width="120" />
            <el-table-column prop="materialName" label="物料名称" min-width="130" />
            <el-table-column prop="batchNo" label="批次号" width="140" />
            <el-table-column prop="freezeQty" label="封存数量" width="100" />
            <el-table-column prop="unfrozenQty" label="已解封" width="90" />
            <el-table-column prop="freezeReason" label="原因" min-width="150" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{row}">
                <el-tag :type="row.status==='已冻结'?'danger':'info'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{row}">
                <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
                <el-button v-if="row.status==='已冻结'" type="warning" link @click="showUnfreeze(row)">解封</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="转包记录" name="repack">
          <div class="toolbar">
            <el-input v-model="repackKeyword" placeholder="物料/批次" clearable style="width:250px" @keyup.enter="loadRepackRecords" />
            <el-button type="primary" @click="loadRepackRecords">查询</el-button>
          </div>
          <el-table :data="repackRows" border stripe>
            <el-table-column prop="repackNo" label="转包单号" width="150" />
            <el-table-column prop="materialCode" label="物料编码" width="120" />
            <el-table-column prop="materialName" label="物料名称" min-width="140" />
            <el-table-column prop="fromLocationName" label="源库位" width="120" />
            <el-table-column prop="toLocationName" label="目标库位" width="120" />
            <el-table-column prop="fromBatchNo" label="源批次" width="140" />
            <el-table-column prop="toBatchNo" label="目标批次" width="140" />
            <el-table-column prop="repackQty" label="数量" width="90" />
            <el-table-column prop="packageQty" label="新包装量" width="100" />
            <el-table-column prop="operator" label="操作人" width="90" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showCreateDialog" title="新建封存" width="520px">
      <el-form :model="freezeForm" label-width="100px">
        <el-form-item label="库存">
          <div class="stock-summary">{{ selectedInventoryText }}</div>
        </el-form-item>
        <el-form-item label="可封存">{{ selectedAvailableQty }}</el-form-item>
        <el-form-item label="封存数量">
          <el-input-number v-model="freezeForm.freezeQty" :min="1" :max="selectedAvailableQty" />
        </el-form-item>
        <el-form-item label="封存原因">
          <el-input v-model="freezeForm.freezeReason" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog=false">取消</el-button>
        <el-button type="primary" @click="submitFreeze">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRepackDialog" title="库存转包" width="560px">
      <el-form :model="repackForm" label-width="100px">
        <el-form-item label="源库存">
          <div class="stock-summary">{{ selectedInventoryText }}</div>
        </el-form-item>
        <el-form-item label="转包数量">
          <el-input-number v-model="repackForm.repackQty" :min="1" :max="selectedAvailableQty" />
        </el-form-item>
        <el-form-item label="目标仓库">
          <el-select v-model="repackForm.toWarehouseId" filterable style="width:100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标库位">
          <el-select v-model="repackForm.toLocationId" filterable style="width:100%">
            <el-option v-for="l in filteredLocations" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="新批次">
          <el-input v-model="repackForm.toBatchNo" />
        </el-form-item>
        <el-form-item label="新包装量">
          <el-input-number v-model="repackForm.packageQty" :min="0" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="repackForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRepackDialog=false">取消</el-button>
        <el-button type="primary" @click="submitRepack">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="封存详情" width="620px">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="物料编码">{{ detailData.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ detailData.materialName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detailData.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="库位">{{ detailData.locationName }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detailData.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="封存数量">{{ detailData.freezeQty }}</el-descriptions-item>
        <el-descriptions-item label="原因">{{ detailData.freezeReason }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailData.status }}</el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <div class="sub-title">解封记录</div>
      <el-table :data="detailData?.unfreezeRecords||[]" border size="small">
        <el-table-column prop="unfreezeQty" label="解封数量" width="110" />
        <el-table-column prop="unfreezeReason" label="原因" min-width="160" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="createdAt" label="时间" width="160" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="showUnfreezeDialog" title="解封" width="400px">
      <el-form :model="unfreezeForm" label-width="100px">
        <el-form-item label="可解封数量">{{ canUnfreezeQty }}</el-form-item>
        <el-form-item label="解封数量">
          <el-input-number v-model="unfreezeForm.qty" :min="1" :max="canUnfreezeQty" />
        </el-form-item>
        <el-form-item label="解封原因">
          <el-input v-model="unfreezeForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUnfreezeDialog=false">取消</el-button>
        <el-button type="primary" @click="submitUnfreeze">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { listFreezeRecords, createFreeze, freezeDetail, unfreeze, listRepackRecords, repackInventory } from '@/api/freeze'
import { listInventory } from '@/api/inventory'
import { listBasic } from '@/api/basic'

const activeTab = ref('stock')
const loading = ref(false)
const inventoryLoading = ref(false)
const keyword = ref('')
const inventoryKeyword = ref('')
const repackKeyword = ref('')
const statusFilter = ref('')
const rows = ref([])
const inventories = ref([])
const repackRows = ref([])
const warehouses = ref([])
const locations = ref([])
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const showUnfreezeDialog = ref(false)
const showRepackDialog = ref(false)
const detailData = ref(null)
const selectedInventory = ref(null)
const currentFreezeId = ref(null)
const canUnfreezeQty = ref(0)
const freezeForm = ref({ freezeQty: 1, freezeReason: '' })
const repackForm = ref({ repackQty: 1, toWarehouseId: null, toLocationId: null, toBatchNo: '', packageQty: 0, reason: '转包' })
const unfreezeForm = ref({ qty: 1, reason: '' })

const availableQty = row => Math.max(Number(row?.qty || 0) - Number(row?.frozenQty || 0), 0)
const selectedAvailableQty = computed(() => availableQty(selectedInventory.value))
const selectedInventoryText = computed(() => {
  const row = selectedInventory.value
  if (!row) return ''
  return `${row.materialCode} ${row.materialName} / ${row.warehouseName}-${row.locationName} / ${row.batchNo}`
})
const filteredLocations = computed(() => {
  if (!repackForm.value.toWarehouseId) return locations.value
  return locations.value.filter(l => Number(l.warehouseId || l.warehouse_id) === Number(repackForm.value.toWarehouseId))
})

const loadInventory = async () => {
  inventoryLoading.value = true
  const res = await listInventory({ keyword: inventoryKeyword.value })
  inventories.value = res.data || []
  inventoryLoading.value = false
}

const loadFreezeRecords = async () => {
  loading.value = true
  const params = { keyword: keyword.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await listFreezeRecords(params)
  rows.value = res.data || []
  loading.value = false
}

const loadRepackRecords = async () => {
  const res = await listRepackRecords({ keyword: repackKeyword.value })
  repackRows.value = res.data || []
}

const loadBaseData = async () => {
  const [whRes, locRes] = await Promise.all([listBasic('warehouse'), listBasic('location')])
  warehouses.value = whRes.data || []
  locations.value = locRes.data || []
}

const loadAll = async () => {
  await Promise.all([loadInventory(), loadFreezeRecords(), loadRepackRecords()])
}

const openFreeze = (row) => {
  selectedInventory.value = row
  freezeForm.value = { inventoryId: row.id, freezeQty: selectedAvailableQty.value, freezeReason: '残损/预留封存' }
  showCreateDialog.value = true
}

const openRepack = (row) => {
  selectedInventory.value = row
  repackForm.value = {
    inventoryId: row.id,
    repackQty: selectedAvailableQty.value,
    toWarehouseId: row.warehouseId,
    toLocationId: row.locationId,
    toBatchNo: row.batchNo,
    packageQty: Number(row.packageQty || 0),
    reason: '转包'
  }
  showRepackDialog.value = true
}

const viewDetail = async (row) => {
  const res = await freezeDetail(row.id)
  detailData.value = res.data
  showDetailDialog.value = true
}

const showUnfreeze = (row) => {
  currentFreezeId.value = row.id
  const frozen = Number(row.afterFrozenQty || row.freezeQty)
  const unfrozen = Number(row.unfrozenQty || 0)
  canUnfreezeQty.value = Math.max(frozen - unfrozen, 0)
  unfreezeForm.value = { qty: canUnfreezeQty.value, reason: '恢复可用' }
  showUnfreezeDialog.value = true
}

const submitFreeze = async () => {
  await createFreeze(freezeForm.value)
  showCreateDialog.value = false
  await loadAll()
  activeTab.value = 'freeze'
}

const submitRepack = async () => {
  await repackInventory(repackForm.value)
  showRepackDialog.value = false
  await loadAll()
  activeTab.value = 'repack'
}

const submitUnfreeze = async () => {
  await unfreeze(currentFreezeId.value, unfreezeForm.value)
  showUnfreezeDialog.value = false
  await loadAll()
}

onMounted(async () => {
  await loadBaseData()
  await loadAll()
})
</script>

<style scoped>
.freeze-page {
  width: 100%;
}
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
.stock-summary {
  line-height: 22px;
}
.sub-title {
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
