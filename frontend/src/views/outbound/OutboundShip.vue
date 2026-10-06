<template>
  <el-card>
    <template #header>
      <div class="page-header">
        <span>扫码出库</span>
        <el-button @click="router.push('/outbound/orders')">返回</el-button>
      </div>
    </template>

    <div class="camera-area" v-if="cameraActive">
      <div id="qr-reader" style="width: 100%;"></div>
      <p class="hint">将出库看板二维码对准摄像头</p>
    </div>

    <!-- 批量图片上传识别 -->
    <el-divider content-position="left">批量上传看板图片识别</el-divider>
    <div class="image-upload-section">
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        accept="image/*"
        multiple
        :on-change="onImageFilesChange"
        drag
        style="width: 100%;"
      >
        <el-icon :size="40"><UploadFilled /></el-icon>
        <div class="el-upload__text">将看板图片拖到此处或<em>点击批量上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持多选 jpg/png 图片，系统将逐一识别二维码</div>
        </template>
      </el-upload>
    </div>

    <!-- 批量识别结果表格 -->
    <div v-if="batchResults.length > 0" class="batch-results">
      <div class="batch-header">
        <span>已识别 {{ batchResults.length }} 个看板</span>
        <el-button type="danger" size="small" @click="clearBatchResults">清空</el-button>
      </div>
      <el-table :data="batchResults" border size="small" max-height="400">
        <el-table-column prop="kanbanCode" label="看板编号" width="190" />
        <el-table-column prop="materialName" label="物料" min-width="140" />
        <el-table-column prop="materialCode" label="物料编码" width="120" />
        <el-table-column prop="warehouseName" label="仓库" width="100" />
        <el-table-column prop="locationName" label="库位" width="130" />
        <el-table-column prop="batchNo" label="批次号" width="140" />
        <el-table-column label="可出库数" width="90" align="center">
          <template #default="{ row }">{{ row.receivedQty }}</template>
        </el-table-column>
        <el-table-column label="出库数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row._shipQty" :min="1" :max="row.receivedQty" size="small" style="width: 100%;" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :loading="row._shipping" @click="shipSingle(row)">出库</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="batch-actions">
        <el-button type="success" :loading="batchShipping" @click="shipAll">全部出库</el-button>
      </div>
    </div>

    <el-divider content-position="left">手动输入看板编号</el-divider>
    <div class="manual-input">
      <el-input v-model="kanbanCode" placeholder="请输入看板编号" clearable @keyup.enter="handleScan" size="large" />
      <el-button type="primary" size="large" @click="handleScan" style="margin-top: 12px;">查询</el-button>
      <el-button type="success" size="large" @click="toggleCamera" style="margin-top: 12px;">{{ cameraActive ? '关闭摄像头' : '开启摄像头' }}</el-button>
    </div>

    <!-- 单个扫描结果 -->
    <div v-if="scanResult" class="result">
      <el-descriptions border :column="1" size="large">
        <el-descriptions-item label="看板编号">{{ scanResult.kanbanCode }}</el-descriptions-item>
        <el-descriptions-item label="物料">{{ scanResult.materialName }}</el-descriptions-item>
        <el-descriptions-item label="物料编码">{{ scanResult.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ scanResult.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="库位">{{ scanResult.locationName }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ scanResult.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ scanResult.planQty }}</el-descriptions-item>
        <el-descriptions-item label="已入库">{{ scanResult.receivedQty }}</el-descriptions-item>
        <el-descriptions-item label="看板状态">{{ scanResult.status }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="120px" style="margin-top: 20px;">
        <el-form-item label="出库数量">
          <el-input-number v-model="shipQty" :min="1" :max="maxShipQty" size="large" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="shipping" @click="handleShip">确认出库</el-button>
          <el-button @click="resetScan">重新扫描</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-empty v-else-if="batchResults.length === 0" description="请扫描、上传或输入看板编号" />
  </el-card>
</template>

<script setup>
import { ref, computed, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { scanKanban, shipOutbound } from '@/api/outbound'
import { Html5Qrcode } from 'html5-qrcode'

const route = useRoute()
const router = useRouter()
const orderId = route.params.id

const cameraActive = ref(false)
const kanbanCode = ref('')
const scanResult = ref(null)
const shipQty = ref(1)
const shipping = ref(false)

// 批量图片识别
const batchResults = ref([])
const batchShipping = ref(false)

let html5QrCode = null

const maxShipQty = computed(() => {
  if (!scanResult.value) return 0
  return scanResult.value.receivedQty
})

// ---- 批量图片识别 ----

// 从图片文件解码二维码
const scanFileForQr = async (imageFile) => {
  const tempId = 'qr-decode-offscreen-' + Date.now() + '-' + Math.random().toString(36).slice(2, 6)
  const tempDiv = document.createElement('div')
  tempDiv.id = tempId
  tempDiv.style.cssText = 'position:fixed;left:-9999px;top:-9999px;width:1px;height:1px;'
  document.body.appendChild(tempDiv)

  const scanner = new Html5Qrcode(tempId)
  try {
    const result = await scanner.scanFile(imageFile, true)
    return result
  } finally {
    try { await scanner.stop() } catch (e) { /* ignore */ }
    document.body.removeChild(tempDiv)
  }
}

// 批量处理上传的图片
const onImageFilesChange = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return

  ElMessage.info(`正在识别 ${file.name} ...`)
  try {
    const decodedText = await scanFileForQr(file)
    // 去重：检查是否已在结果列表中
    if (batchResults.value.some(r => r.kanbanCode === decodedText)) {
      ElMessage.warning(`${decodedText} 已在列表中，跳过`)
      return
    }
    // 查询看板信息
    const res = await scanKanban(decodedText)
    if (res.code === 200 && res.data) {
      const kb = res.data
      batchResults.value.push({
        ...kb,
        _shipQty: kb.receivedQty || 1,
        _shipping: false
      })
      ElMessage.success(`${kb.kanbanCode} 识别成功`)
    } else {
      ElMessage.warning(`${decodedText} 看板不存在`)
    }
  } catch (err) {
    console.error('图片识别失败', err)
    ElMessage.error(`${file.name} 未能识别二维码`)
  }
}

const clearBatchResults = () => {
  batchResults.value = []
}

// 单个出库
const shipSingle = async (row) => {
  if (row._shipQty <= 0 || row._shipQty > row.receivedQty) {
    ElMessage.warning('出库数量无效')
    return
  }
  row._shipping = true
  try {
    await shipOutbound({
      kanbanCode: row.kanbanCode,
      shipQty: row._shipQty,
      operator: 'admin'
    })
    ElMessage.success(`${row.kanbanCode} 出库成功`)
    // 从列表中移除已完成项
    const idx = batchResults.value.indexOf(row)
    if (idx > -1) batchResults.value.splice(idx, 1)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '出库失败')
  } finally {
    row._shipping = false
  }
}

// 全部出库
const shipAll = async () => {
  if (batchResults.value.length === 0) {
    ElMessage.warning('没有待出库的看板')
    return
  }
  batchShipping.value = true
  let successCount = 0
  let failCount = 0
  const successCodes = new Set()
  for (const row of batchResults.value) {
    try {
      await shipOutbound({
        kanbanCode: row.kanbanCode,
        shipQty: row._shipQty,
        operator: 'admin',
        orderId: orderId
      })
      successCount++
      successCodes.add(row.kanbanCode)
    } catch (e) {
      failCount++
    }
  }
  // 一次性过滤移除成功的项（避免 splice 索引错位）
  batchResults.value = batchResults.value.filter(r => !successCodes.has(r.kanbanCode))
  ElMessage.success(`批量出库完成：成功 ${successCount}，失败 ${failCount}`)
  batchShipping.value = false
}

// ---- 摄像头扫码 ----

const toggleCamera = async () => {
  if (cameraActive.value) {
    await stopScan()
    cameraActive.value = false
  } else {
    cameraActive.value = true
    await nextTick()
    startScan()
  }
}

const startScan = async () => {
  if (!document.getElementById('qr-reader')) {
    ElMessage.error('扫码容器未找到')
    return
  }
  html5QrCode = new Html5Qrcode('qr-reader')
  const config = { fps: 10, qrbox: { width: 250, height: 250 }, aspectRatio: 1.0, showTorchButtonIfSupported: true }
  try {
    await html5QrCode.start(
      { facingMode: 'environment' },
      config,
      async (decodedText) => {
        ElMessage.success('识别成功，正在查询...')
        await stopScan()
        cameraActive.value = false
        kanbanCode.value = decodedText
        await handleScan()
      },
      (errorMessage) => {}
    )
    ElMessage.success('摄像头已启动，请对准出库看板二维码')
  } catch (err) {
    ElMessage.error('摄像头启动失败：' + (err.message || err))
    cameraActive.value = false
  }
}

const stopScan = async () => {
  if (html5QrCode && html5QrCode.isScanning) {
    try { await html5QrCode.stop() } catch (err) {}
    html5QrCode = null
  }
}

// ---- 手动输入 ----

const handleScan = async () => {
  if (!kanbanCode.value) {
    ElMessage.warning('请输入看板编号')
    return
  }
  try {
    const res = await scanKanban(kanbanCode.value)
    if (res.code === 200 && res.data) {
      scanResult.value = res.data
      shipQty.value = 1
      ElMessage.success('查询成功')
    } else {
      ElMessage.error(res.message || '看板不存在')
      scanResult.value = null
    }
  } catch (e) {
    ElMessage.error('看板不存在')
    scanResult.value = null
  }
}

const handleShip = async () => {
  if (!scanResult.value) return
  if (shipQty.value <= 0 || shipQty.value > maxShipQty.value) {
    ElMessage.warning('出库数量无效')
    return
  }
  shipping.value = true
  try {
    await shipOutbound({
      kanbanCode: kanbanCode.value,
      shipQty: shipQty.value,
      operator: 'admin'
    })
    ElMessage.success('出库成功')
    resetScan()
    router.push('/outbound/orders')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '出库失败')
  } finally {
    shipping.value = false
  }
}

const resetScan = () => {
  kanbanCode.value = ''
  scanResult.value = null
  shipQty.value = 1
}

onUnmounted(() => {
  stopScan()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.camera-area {
  margin-bottom: 16px;
  border-radius: 12px;
  overflow: hidden;
  background: #000;
  min-height: 300px;
}
#qr-reader {
  width: 100%;
  background: #000;
}
.hint {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
.image-upload-section {
  margin-bottom: 16px;
}
.batch-results {
  margin-bottom: 20px;
}
.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 600;
  font-size: 14px;
}
.batch-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.manual-input {
  margin-top: 8px;
}
.result {
  margin-top: 20px;
}
</style>
