<template>
  <div class="print-workspace">
    <!-- 顶部工具栏 -->
    <div class="toolbar no-print">
      <div class="toolbar-left">
        <el-button @click="router.push('/inbound/manage')" size="small">← 返回</el-button>
        <span class="toolbar-title">批量打印看板</span>
      </div>
      <div class="toolbar-right">
        <el-button size="small" @click="showSettings = !showSettings">
          {{ showSettings ? '隐藏设置' : '打印设置' }}
        </el-button>
        <el-button size="small" @click="showMaterials = !showMaterials">
          {{ showMaterials ? '隐藏筛选' : '物料筛选' }}
        </el-button>
        <el-button type="primary" size="small" @click="printAll">打印全部</el-button>
        <el-button type="success" size="small" @click="saveAllImages" :loading="savingAll">保存全部图片</el-button>
      </div>
    </div>

    <!-- 打印设置 -->
    <div v-show="showSettings" class="settings-bar no-print">
      <div class="setting-item">
        <span class="setting-label">纸张：</span>
        <el-radio-group v-model="paperSize" size="small">
          <el-radio-button value="A3">A3</el-radio-button>
          <el-radio-button value="A4">A4</el-radio-button>
          <el-radio-button value="A5">A5</el-radio-button>
          <el-radio-button value="B5">B5</el-radio-button>
          <el-radio-button value="Letter">Letter</el-radio-button>
        </el-radio-group>
      </div>
      <div class="setting-item">
        <span class="setting-label">方向：</span>
        <el-radio-group v-model="orientation" size="small">
          <el-radio-button value="portrait">纵向</el-radio-button>
          <el-radio-button value="landscape">横向</el-radio-button>
        </el-radio-group>
      </div>
      <div class="setting-item">
        <span class="setting-label">每页：</span>
        <el-radio-group v-model="perPageCount" size="small">
          <el-radio-button :value="4">4</el-radio-button>
          <el-radio-button :value="6">6</el-radio-button>
          <el-radio-button :value="8">8</el-radio-button>
          <el-radio-button :value="9">9</el-radio-button>
          <el-radio-button :value="10">10</el-radio-button>
        </el-radio-group>
      </div>
      <div class="setting-item">
        <span class="setting-label">缩放：{{ kanbanScale }}%</span>
        <el-slider v-model="kanbanScale" :min="70" :max="130" :step="5" style="width:140px" />
      </div>
    </div>

    <!-- 物料筛选 -->
    <div v-show="showMaterials && groups.length > 0" class="material-bar no-print">
      <el-radio-group v-model="selectedMaterial" size="small" @change="onMaterialChange">
        <el-radio-button value="">全部 ({{ totalCount }})</el-radio-button>
        <el-radio-button v-for="g in groups" :key="g.materialCode" :value="g.materialCode">
          {{ g.materialCode }} ({{ g.kanbans.length }})
        </el-radio-button>
      </el-radio-group>
    </div>

    <el-empty v-if="groups.length === 0" description="未选择看板" />

    <!-- 动态 @page 规则：纸张尺寸 + 方向 -->
    <component :is="'style'" v-text="`@page { size: ${paperSize} ${orientation}; margin: 0; }`"></component>

    <!-- 纸张区域 -->
    <div v-if="pages.length > 0" class="paper-area">
      <div
        v-for="(page, pageIndex) in pages"
        :key="pageIndex"
        class="print-page"
        :style="pageStyle"
      >
        <div class="page-number no-print">{{ pageIndex + 1 }} / {{ pages.length }}</div>
        <div
          class="page-grid"
          :style="{
            gridTemplateColumns: `repeat(${gridConfig.cols}, 1fr)`,
            gridTemplateRows: `repeat(${gridConfig.rows}, 1fr)`,
            fontSize: (densityFontSize * kanbanScale / 100).toFixed(1) + 'px',
          }"
        >
          <div
            v-for="kanban in page"
            :key="kanban.id"
            :ref="el => setCardRef(kanban.id, el)"
            class="kanban-cell"
          >
            <div class="kanban-card">
              <div class="card-header">
                <span class="card-title">入库看板</span>
                <span class="card-code">{{ kanban.kanbanCode }}</span>
              </div>
              <div v-if="kanban.packageInfo" class="package-info">包装：{{ kanban.packageInfo }}</div>
              <table class="info-table">
                <colgroup>
                  <col class="col-lbl"><col class="col-val"><col class="col-lbl"><col class="col-val">
                </colgroup>
                <tr><td>物料</td><td>{{ kanban.materialName }}</td><td>入库单</td><td>{{ kanban.orderNo }}</td></tr>
                <tr><td>数量</td><td>{{ kanban.planQty }}</td><td>已入库</td><td>{{ kanban.receivedQty }}</td></tr>
                <tr><td>仓库</td><td>{{ kanban.warehouseName }}</td><td>库位</td><td>{{ kanban.locationName }}</td></tr>
                <tr><td>批次</td><td>{{ kanban.batchNo }}</td><td>状态</td><td>{{ kanban.status }}</td></tr>
              </table>
              <div class="qr">
                <img :src="qrCache[kanban.id]" alt="QR" v-if="qrCache[kanban.id]" />
              </div>
            </div>
          </div>
          <!-- 占位 -->
          <div v-for="n in (perPageCount - page.length)" :key="'e-'+n" class="kanban-cell placeholder"></div>
        </div>
      </div>
    </div>

    <div v-if="pages.length > 0" class="page-footer no-print">
      共 {{ pages.length }} 页 · {{ totalCount }} 张 · {{ paperSize }} {{ orientation === 'landscape' ? '横向' : '纵向' }} · 每页 {{ perPageCount }} 张
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listKanbans } from '@/api/inbound'
import QRCode from 'qrcode'
import html2canvas from 'html2canvas'

const route = useRoute()
const router = useRouter()
const groups = ref([])
const qrCache = ref({})
const selectedMaterial = ref('')
const savingAll = ref(false)
const showSettings = ref(true)
const showMaterials = ref(true)

const paperSize = ref('A4')
const orientation = ref('portrait')   // portrait | landscape
const perPageCount = ref(4)
const kanbanScale = ref(100)

// 纸张原始尺寸 (portrait: w<h, landscape 时交换)
const paperDefs = {
  A3:     { w: 297, h: 420 },
  A4:     { w: 210, h: 297 },
  A5:     { w: 148, h: 210 },
  B5:     { w: 176, h: 250 },
  Letter: { w: 215.9, h: 279.4 },
}

// 当前纸张实际宽高（考虑方向）
const paperWH = computed(() => {
  const def = paperDefs[paperSize.value]
  if (orientation.value === 'landscape') {
    return { w: def.h, h: def.w }
  }
  return { w: def.w, h: def.h }
})

// 页面容器 style
const pageStyle = computed(() => ({
  width:  paperWH.value.w + 'mm',
  height: paperWH.value.h + 'mm',
}))

// 网格行列（按方向和密度优化）
const gridConfig = computed(() => {
  const n = perPageCount.value
  if (orientation.value === 'landscape') {
    const map = { 4: [2,2], 6: [3,2], 8: [4,2], 9: [3,3], 10: [5,2] }
    const [cols, rows] = map[n]
    return { cols, rows }
  }
  // portrait：优先多行，保证卡片有足够宽度
  const map = { 4: [2,2], 6: [2,3], 8: [2,4], 9: [3,3], 10: [2,5] }
  const [cols, rows] = map[n]
  return { cols, rows }
})

const densityFontSize = computed(() => {
  const map = { 4: 18, 6: 15, 8: 13, 9: 11, 10: 10 }
  return map[perPageCount.value]
})

const cardRefs = {}
const setCardRef = (id, el) => {
  if (el) cardRefs[id] = el.querySelector('.kanban-card') || el
}

const totalCount = computed(() => groups.value.reduce((s, g) => s + g.kanbans.length, 0))

const filteredGroups = computed(() => {
  if (!selectedMaterial.value) return groups.value
  return groups.value.filter(g => g.materialCode === selectedMaterial.value)
})

const pages = computed(() => {
  const all = filteredGroups.value.flatMap(g => g.kanbans)
  const n = perPageCount.value
  const r = []
  for (let i = 0; i < all.length; i += n) r.push(all.slice(i, i + n))
  return r
})

const onMaterialChange = () => window.scrollTo({ top: 0, behavior: 'smooth' })
const printAll = () => window.print()

const saveAllImages = async () => {
  const all = filteredGroups.value.flatMap(g => g.kanbans)
  if (!all.length) { ElMessage.warning('没有可保存的看板'); return }
  savingAll.value = true
  try {
    let ok = 0
    for (const k of all) {
      const el = cardRefs[k.id]; if (!el) continue
      try {
        const canvas = await html2canvas(el, { scale: 2, backgroundColor: '#fff', useCORS: true })
        const a = document.createElement('a')
        a.download = `${k.kanbanCode}.png`; a.href = canvas.toDataURL('image/png'); a.click()
        ok++; await new Promise(r => setTimeout(r, 300))
      } catch (e) { console.error(e) }
    }
    ElMessage.success(`已保存 ${ok} / ${all.length} 张`)
  } finally { savingAll.value = false }
}

const buildGroups = (kanbans) => {
  const map = new Map()
  kanbans.forEach(k => {
    const key = k.materialCode
    if (!map.has(key)) map.set(key, { materialCode: k.materialCode, materialName: k.materialName, kanbans: [] })
    map.get(key).kanbans.push(k)
  })
  map.forEach(g => g.kanbans.sort((a, b) => (a.locationName || '').localeCompare(b.locationName || '')))
  return [...map.values()].sort((a, b) => a.materialCode.localeCompare(b.materialCode))
}

onMounted(async () => {
  const ids = route.query.ids
  if (!ids) return
  const idSet = new Set(ids.split(',').map(Number))
  const res = await listKanbans({})
  const all = (res.data || []).filter(k => idSet.has(k.id))
  groups.value = buildGroups(all)

  const qrSize = perPageCount.value <= 4 ? 160 : perPageCount.value <= 6 ? 120 : 90
  for (const g of groups.value) {
    for (const k of g.kanbans) {
      QRCode.toDataURL(k.kanbanCode, { width: qrSize, margin: 1, errorCorrectionLevel: 'M' })
        .then(url => { qrCache.value[k.id] = url }).catch(() => {})
    }
  }
})
</script>

<style scoped>
/* ============ 工作区 ============ */
.print-workspace {
  min-height: 100vh; background: #e8eaed; display: flex; flex-direction: column;
}

/* ============ 工具栏 ============ */
.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 16px; background: #fff; border-bottom: 1px solid #dcdfe6; flex-shrink: 0;
}
.toolbar-left  { display: flex; align-items: center; gap: 10px; }
.toolbar-right { display: flex; align-items: center; gap: 6px; }
.toolbar-title { font-size: 15px; font-weight: 600; color: #303133; }

/* ============ 设置栏 ============ */
.settings-bar {
  display: flex; align-items: center; gap: 18px; padding: 8px 16px;
  background: #f5f7fa; border-bottom: 1px solid #e4e7ed; flex-shrink: 0; flex-wrap: wrap;
}
.setting-item  { display: flex; align-items: center; gap: 6px; }
.setting-label { font-size: 12px; font-weight: 600; color: #606266; white-space: nowrap; }

/* ============ 物料栏 ============ */
.material-bar { padding: 6px 16px; background: #fafafa; border-bottom: 1px solid #ebeef5; flex-shrink: 0; }

/* ============ 纸张区 ============ */
.paper-area {
  flex: 1; overflow-y: auto; padding: 20px 16px;
  display: flex; flex-direction: column; align-items: center; gap: 28px;
}

/* ============ 虚拟页面 ============ */
.print-page {
  background: #fff; box-shadow: 0 2px 12px rgba(0,0,0,0.15);
  position: relative; overflow: hidden; flex-shrink: 0;
}
.page-number {
  position: absolute; bottom: 4mm; right: 5mm;
  font-size: 10px; color: #bbb; z-index: 1;
}

/* ============ 页面网格 ============ */
.page-grid {
  display: grid; width: 100%; height: 100%;
  padding: 4mm; gap: 2mm; box-sizing: border-box;
}

/* ============ 看板单元格 ============ */
.kanban-cell { display: flex; overflow: hidden; }
.kanban-cell.placeholder { visibility: hidden; }

/* ============ 看板卡片 ============ */
.kanban-card {
  width: 100%; height: 100%;
  border: 1px solid #555; padding: 0.3em 0.4em;
  background: #fff; display: flex; flex-direction: column; box-sizing: border-box;
}

.card-header {
  display: flex; align-items: baseline; justify-content: center; gap: 0.5em;
  margin-bottom: 0.1em; flex-shrink: 0;
}
.card-title { font-size: 1em; font-weight: 700; }
.card-code  { font-size: 1.1em; font-weight: 700; letter-spacing: 0.02em; }

.package-info {
  text-align: center; font-size: 0.78em; font-weight: 600;
  color: #409eff; margin-bottom: 0.15em; flex-shrink: 0;
}

/* ============ 信息表 ============ */
.info-table {
  width: 100%; border-collapse: collapse; font-size: 0.78em; line-height: 1.2;
  flex-shrink: 0;
}
.info-table td {
  border: 0.5px solid #bbb; padding: 0.1em 0.2em; vertical-align: middle;
}
.info-table td:nth-child(1),
.info-table td:nth-child(3) {
  background: #f5f7fa; font-weight: 600; white-space: nowrap; width: 1%;
}

/* ============ QR ============ */
.qr {
  display: flex; justify-content: center; align-items: center;
  margin-top: auto; padding-top: 0.2em; flex-shrink: 0;
}
.qr img { width: 3.8em; height: 3.8em; }

/* ============ 页脚 ============ */
.page-footer {
  text-align: center; color: #999; font-size: 12px;
  padding: 10px; background: #fff; border-top: 1px solid #ebeef5; flex-shrink: 0;
}

/* ============ 打印 ============ */
@media print {
  html, body { margin: 0 !important; padding: 0 !important; }
  .no-print  { display: none !important; }
  .print-workspace { background: #fff; min-height: auto; display: block; }
  .paper-area { padding: 0; gap: 0; display: block; overflow: visible; }
  .print-page {
    width: 100% !important; height: 100vh !important;
    margin: 0; box-shadow: none; page-break-after: always; overflow: hidden;
  }
  .print-page:last-child { page-break-after: auto; }
  .kanban-card { border: 1px solid #000; break-inside: avoid; page-break-inside: avoid; }
  .page-number, .page-footer { display: none; }
}
</style>
