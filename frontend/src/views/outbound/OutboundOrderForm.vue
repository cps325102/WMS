<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listBasic } from '@/api/basic'
import { createOutboundOrder, detailOutboundOrder, updateOutboundOrder, getMaterialsWithStock } from '@/api/outbound'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const materials = ref([])
const customers = ref([])
const form = reactive({ remark: '', items: [] })

const categoryTree = [
  { value: '', label: '全部物料 (145)', icon: 'Grid' },
  { value: '动力总成', label: '动力总成', icon: 'Folder', children: [
    { value: '发动机系统', label: '发动机系统 (26)' }, { value: '变速箱系统', label: '变速箱系统 (16)' },
    { value: '冷却系统', label: '冷却系统 (6)' }, { value: '燃油系统', label: '燃油系统 (5)' }, { value: '排气系统', label: '排气系统 (6)' },
  ]},
  { value: '底盘制动', label: '底盘与制动', icon: 'Folder', children: [
    { value: '底盘系统', label: '底盘系统 (14)' }, { value: '制动系统', label: '制动系统 (10)' }, { value: '转向系统', label: '转向系统 (6)' },
  ]},
  { value: '车身内外饰', label: '车身与内外饰', icon: 'Folder', children: [
    { value: '车身外饰', label: '车身外饰 (20)' }, { value: '车身内饰', label: '车身内饰 (10)' },
  ]},
  { value: '电子安全', label: '电子与安全', icon: 'Folder', children: [
    { value: '电器系统', label: '电器系统 (10)' }, { value: '安全系统', label: '安全系统 (10)' }, { value: '空调系统', label: '空调系统 (6)' },
  ]},
]

const loadOptions = async () => {
  try {
    const res = await getMaterialsWithStock()
    materials.value = (res.data || []).map(m => ({ ...m, code: m.materialCode || m.code || '', name: m.materialName || m.name || '' }))
  } catch { materials.value = [] }
  try {
    const res = await listBasic('customer')
    customers.value = (res.data || []).map(c => ({ ...c, name: c.name || c.customerName }))
  } catch { customers.value = [] }
}

// ===== 批量选择 =====
const batchVisible = ref(false)
const batchKeyword = ref('')
const batchCategory = ref('')
const batchTreeSearch = ref('')
const selectedRows = ref([])
const batchSetForm = reactive({ outboundType: '', customerId: '', planDate: '', defaultQty: 1 })
const batchTableRef = ref(null)

const filteredBatchTree = computed(() => {
  if (!batchTreeSearch.value) return categoryTree
  const kw = batchTreeSearch.value.toLowerCase()
  return categoryTree.map(g => {
    if (!g.children) return g.label.toLowerCase().includes(kw) || g.value.toLowerCase().includes(kw) ? g : null
    const matched = g.children.filter(c => c.label.toLowerCase().includes(kw) || c.value.toLowerCase().includes(kw))
    if (matched.length > 0) return { ...g, children: matched }
    if (g.label.toLowerCase().includes(kw)) return g
    return null
  }).filter(Boolean)
})

const batchMaterials = computed(() => {
  let list = materials.value
  if (batchCategory.value) list = list.filter(m => m.category === batchCategory.value)
  if (batchKeyword.value) {
    const kw = batchKeyword.value.toLowerCase()
    list = list.filter(m => (m.code||'').toLowerCase().includes(kw) || (m.name||'').toLowerCase().includes(kw) || (m.spec||'').toLowerCase().includes(kw))
  }
  return list
})

const selectBatchCategory = (val) => { batchCategory.value = val }
const addItem = () => { form.items.push({ outboundType: '', customerId: '', planDate: '', materialId: '', planQty: 1, unit: '', _stockQty: 0 }) }
const removeItem = i => form.items.splice(i, 1)

const custName = (id) => { const c = customers.value.find(i => i.id === id); return c ? c.name : '-' }

const onMaterialChange = row => {
  const m = materials.value.find(i => i.id === row.materialId)
  if (m) { row.unit = m.unit || ''; row._stockQty = m.qty || 0 }
}

const openBatchDialog = () => {
  Object.assign(batchSetForm, { outboundType: '', customerId: '', planDate: '', defaultQty: 1 })
  batchKeyword.value = ''; batchCategory.value = ''; batchTreeSearch.value = ''
  selectedRows.value = []; batchVisible.value = true
}

const onBatchSelect = (selection) => {
  const selIds = new Set(selection.map(m => m.id))
  const kept = selectedRows.value.filter(r => selIds.has(r.materialId))
  const keptIds = new Set(kept.map(r => r.materialId))
  selection.forEach(m => {
    if (!keptIds.has(m.id)) kept.push({
      materialId: m.id, materialCode: m.code, materialName: m.name, spec: m.spec,
      unit: m.unit || '', planQty: batchSetForm.defaultQty, stockQty: m.qty || 0,
      outboundType: batchSetForm.outboundType, customerId: batchSetForm.customerId, planDate: batchSetForm.planDate,
    })
  })
  selectedRows.value = kept
}
const removeSelectedRow = i => selectedRows.value.splice(i, 1)

const selectAll = () => batchMaterials.value.forEach(r => batchTableRef.value?.toggleRowSelection(r, true))
const deselectAll = () => { batchTableRef.value?.clearSelection(); selectedRows.value = [] }

const applyBatchSet = () => {
  selectedRows.value.forEach(r => {
    if (batchSetForm.outboundType) r.outboundType = batchSetForm.outboundType
    if (batchSetForm.customerId) r.customerId = batchSetForm.customerId
    if (batchSetForm.planDate) r.planDate = batchSetForm.planDate
    r.planQty = batchSetForm.defaultQty
  })
  ElMessage.success('已批量应用设置')
}

const confirmBatchAdd = () => {
  if (!selectedRows.value.length) { ElMessage.warning('请至少选择一个物料'); return }
  for (let i = 0; i < selectedRows.value.length; i++) {
    const r = selectedRows.value[i]
    if (!r.planQty || r.planQty <= 0) { ElMessage.warning(`第${i+1}行数量必须大于0`); return }
    if (r.planQty > r.stockQty && r.stockQty > 0) { ElMessage.warning(`第${i+1}行超出库存(${r.stockQty})`); return }
  }
  selectedRows.value.forEach(r => form.items.push({
    materialId: r.materialId, planQty: r.planQty, unit: r.unit, _stockQty: r.stockQty,
    outboundType: r.outboundType, customerId: r.customerId, planDate: r.planDate,
  }))
  batchVisible.value = false
  ElMessage.success(`已添加 ${selectedRows.value.length} 条明细`)
}

const loadDetail = async () => {
  if (!isEdit.value) { addItem(); return }
  const res = await detailOutboundOrder(route.params.id)
  Object.assign(form, res.data)
  form.items = (res.data.items || []).map(item => ({ ...item, _stockQty: 0 }))
}

const submit = async () => {
  if (!form.items.length) { ElMessage.warning('请添加出库明细'); return }
  for (let i = 0; i < form.items.length; i++) {
    const it = form.items[i]
    if (!it.materialId) { ElMessage.warning(`第${i+1}行物料不能为空`); return }
    if (!it.planQty || it.planQty <= 0) { ElMessage.warning(`第${i+1}行数量必须大于0`); return }
  }
  const first = form.items[0]
  const data = { ...form, outboundType: first.outboundType || '销售出库', customerId: first.customerId, planDate: first.planDate, createBy: 'admin' }
  isEdit.value ? await updateOutboundOrder(route.params.id, data) : await createOutboundOrder(data)
  ElMessage.success('保存成功')
  router.push('/outbound/orders')
}

onMounted(async () => { await loadOptions(); await loadDetail() })
</script>

<template>
  <el-card>
    <template #header>
      <div class="page-header"><span>{{ isEdit ? '修改出库单' : '创建出库单' }}</span><el-button @click="router.push('/outbound/orders')">返回</el-button></div>
    </template>
    <el-form label-width="80px" inline>
      <el-form-item label="备注"><el-input v-model="form.remark" style="width:400px" placeholder="出库单备注" /></el-form-item>
    </el-form>

    <div class="table-title"><span>出库明细</span><div><el-button type="primary" @click="openBatchDialog">批量添加</el-button><el-button type="success" @click="addItem">新增明细</el-button></div></div>
    <el-table :data="form.items" border>
      <el-table-column label="出库类型" width="100"><template #default="{row}">{{ row.outboundType || '-' }}</template></el-table-column>
      <el-table-column label="客户" min-width="140"><template #default="{row}">{{ custName(row.customerId) }}</template></el-table-column>
      <el-table-column label="日期" width="110"><template #default="{row}">{{ row.planDate || '-' }}</template></el-table-column>
      <el-table-column label="物料" min-width="200" required>
        <template #default="{row}">
          <el-select v-model="row.materialId" filterable placeholder="搜索物料..." style="width:100%" @change="onMaterialChange(row)">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.code} ${m.name}`" :value="m.id" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="130"><template #default="{row}"><el-input-number v-model="row.planQty" :min="1" style="width:100%" /></template></el-table-column>
      <el-table-column label="单位" width="80"><template #default="{row}">{{ row.unit || '-' }}</template></el-table-column>
      <el-table-column label="可用库存" width="90"><template #default="{row}"><span :style="{color: row._stockQty < row.planQty ? '#f56c6c' : '#67c23a'}">{{ row._stockQty ?? '-' }}</span></template></el-table-column>
      <el-table-column label="操作" width="70"><template #default="{$index}"><el-button type="danger" link @click="removeItem($index)">删除</el-button></template></el-table-column>
    </el-table>
    <div class="footer"><el-button @click="router.push('/outbound/orders')">取消</el-button><el-button type="primary" @click="submit">保存</el-button></div>
  </el-card>

  <!-- 批量添加弹窗 -->
  <el-dialog v-model="batchVisible" title="批量添加物料" width="95%" top="3vh" destroy-on-close>
    <div class="batch-layout">
      <div class="batch-sidebar">
        <div class="batch-sidebar-title">物料分类</div>
        <div class="tree-search-box"><el-input v-model="batchTreeSearch" placeholder="搜索分类..." size="small" clearable><template #prefix><el-icon><Search /></el-icon></template></el-input></div>
        <div class="tree-item all-item" :class="{active:!batchCategory}" @click="selectBatchCategory('')">全部物料 (145)</div>
        <template v-for="g in filteredBatchTree.filter(x=>x.children)" :key="g.value">
          <div class="tree-group-title">{{ g.label }}</div>
          <div v-for="c in g.children" :key="c.value" class="tree-item" :class="{active:batchCategory===c.value}" @click="selectBatchCategory(c.value)">{{ c.label }}</div>
        </template>
      </div>
      <div class="batch-main">
        <div class="batch-top">
          <div class="batch-search-bar">
            <el-input v-model="batchKeyword" placeholder="物料编码/名称/规格" clearable style="width:300px"><template #prefix><el-icon><Search /></el-icon></template></el-input>
            <el-tag v-if="batchCategory" type="warning" closable @close="batchCategory=''">{{ batchCategory }}</el-tag>
            <span class="batch-count">共 {{ batchMaterials.length }} 种</span>
            <el-button size="small" @click="selectAll">全选</el-button>
            <el-button size="small" @click="deselectAll">取消全选</el-button>
            <div class="batch-set-bar">
              <el-select v-model="batchSetForm.outboundType" placeholder="出库类型" clearable size="small" style="width:110px">
                <el-option label="销售出库" value="销售出库" /><el-option label="生产领料" value="生产领料" /><el-option label="调拨出库" value="调拨出库" />
              </el-select>
              <el-select v-model="batchSetForm.customerId" placeholder="客户" clearable size="small" style="width:140px">
                <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
              <el-date-picker v-model="batchSetForm.planDate" placeholder="日期" size="small" value-format="YYYY-MM-DD" style="width:120px" />
              <span style="font-size:12px">数量:</span>
              <el-input-number v-model="batchSetForm.defaultQty" :min="1" size="small" style="width:80px" />
              <el-button size="small" type="primary" @click="applyBatchSet">批量应用</el-button>
            </div>
          </div>
          <el-table ref="batchTableRef" :data="batchMaterials" @selection-change="onBatchSelect" border stripe row-key="id" style="width:100%">
            <el-table-column type="selection" width="45" :reserve-selection="true" />
            <el-table-column prop="code" label="物料编码" width="130" />
            <el-table-column prop="name" label="物料名称" min-width="170" show-overflow-tooltip />
            <el-table-column prop="spec" label="规格型号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="category" label="类别" width="100" />
            <el-table-column prop="unit" label="单位" width="60" align="center" />
            <el-table-column label="库存" width="80" align="center"><template #default="{row}">{{ row.qty || 0 }}</template></el-table-column>
          </el-table>
        </div>
        <div class="batch-bottom" v-if="selectedRows.length">
          <div class="detail-title">已选物料明细 — 逐行独立设置 (已选 {{ selectedRows.length }} 种)</div>
          <el-table :data="selectedRows" border size="small" style="width:100%">
            <el-table-column prop="materialCode" label="编码" width="120" />
            <el-table-column prop="materialName" label="物料" min-width="130" show-overflow-tooltip />
            <el-table-column label="类型" width="100"><template #default="{row}"><el-select v-model="row.outboundType" size="small" style="width:100%" clearable><el-option label="销售出库" value="销售出库" /><el-option label="生产领料" value="生产领料" /><el-option label="调拨出库" value="调拨出库" /></el-select></template></el-table-column>
            <el-table-column label="客户" width="150"><template #default="{row}"><el-select v-model="row.customerId" size="small" style="width:100%" clearable><el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" /></el-select></template></el-table-column>
            <el-table-column label="日期" width="130"><template #default="{row}"><el-date-picker v-model="row.planDate" size="small" value-format="YYYY-MM-DD" style="width:100%" /></template></el-table-column>
            <el-table-column label="库存" width="60"><template #default="{row}">{{ row.stockQty }}</template></el-table-column>
            <el-table-column label="数量" width="100"><template #default="{row}"><el-input-number v-model="row.planQty" :min="1" :max="row.stockQty||99999" size="small" style="width:100%" /></template></el-table-column>
            <el-table-column label="操作" width="60"><template #default="{$index}"><el-button type="danger" link size="small" @click="removeSelectedRow($index)">移除</el-button></template></el-table-column>
          </el-table>
        </div>
      </div>
    </div>
    <template #footer><el-button @click="batchVisible=false">取消</el-button><el-button type="primary" @click="confirmBatchAdd">确定添加 ({{ selectedRows.length }})</el-button></template>
  </el-dialog>
</template>

<style scoped>
.page-header,.table-title,.footer{display:flex;align-items:center;justify-content:space-between}
.table-title{margin:12px 0}.table-title div{display:flex;gap:8px}
.footer{justify-content:flex-end;margin-top:16px;gap:12px}
.batch-layout{display:flex;gap:12px;height:75vh}
.batch-sidebar{width:200px;flex-shrink:0;background:#fafafa;border-radius:6px;overflow-y:auto;border:1px solid #ebeef5}
.batch-sidebar-title{padding:10px 14px;font-weight:700;font-size:14px;border-bottom:1px solid #ebeef5}
.tree-search-box{padding:6px 10px;border-bottom:1px solid #ebeef5}
.tree-group-title{font-size:11px;font-weight:700;color:#303133;padding:6px 10px 2px}
.tree-item{font-size:12px;padding:4px 16px;cursor:pointer;color:#606266;border-radius:3px;margin:1px 6px;transition:all .15s}
.tree-item:hover{background:#ecf5ff;color:#409eff}
.tree-item.active{background:#409eff;color:#fff;font-weight:600}
.tree-item.all-item{font-size:13px;font-weight:600;padding:6px 14px;color:#303133}
.tree-item.all-item.active{background:#409eff;color:#fff}
.batch-main{flex:1;min-width:0;display:flex;flex-direction:column;gap:6px;overflow:hidden}
.batch-top{flex:1;display:flex;flex-direction:column;min-height:0;overflow:hidden}
.batch-top .el-table{flex:1}
.batch-search-bar{display:flex;align-items:center;gap:8px;margin-bottom:4px;flex-wrap:wrap}
.batch-count{font-size:12px;color:#909399;white-space:nowrap}
.batch-set-bar{display:flex;align-items:center;gap:5px;margin-left:auto}
.batch-bottom{flex-shrink:0;max-height:45%;overflow:auto;border-top:2px solid #409eff;padding-top:6px}
.detail-title{font-weight:700;font-size:13px;margin-bottom:4px;color:#409eff}
</style>
