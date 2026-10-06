<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>扫码入库</span>
        <el-button type="primary" link @click="toggleCamera">
          {{ cameraActive ? '关闭摄像头' : '开启摄像头' }}
        </el-button>
      </div>
    </template>

    <div v-show="cameraActive" class="camera-wrapper">
      <div id="qr-reader" style="width: 100%;"></div>
      <p class="hint">将二维码对准摄像头，自动识别</p>
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
        <el-table-column label="计划/已入库" width="110" align="center">
          <template #default="{ row }">{{ row.receivedQty }} / {{ row.planQty }}</template>
        </el-table-column>
        <el-table-column label="入库数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row._receiveQty" :min="1" :max="row.planQty - row.receivedQty" size="small" style="width: 100%;" />
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="100">
          <template #default="{ row }">
            <el-input v-model="row._operator" size="small" placeholder="admin" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :loading="row._submitting" :disabled="row.receivedQty >= row.planQty" @click="receiveSingle(row)">入库</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="batch-actions">
        <el-button type="success" :loading="batchSubmitting" @click="receiveAll">全部入库</el-button>
      </div>
    </div>

    <el-divider content-position="left">手动输入看板编号</el-divider>
    <div class="manual-input">
      <el-input v-model="kanbanCode" placeholder="请输入看板编号" clearable @keyup.enter="scan" size="large" />
      <el-button type="primary" size="large" :loading="queryLoading" @click="scan" style="margin-top: 12px;">查询</el-button>
    </div>

    <!-- 单个查询结果 -->
    <div v-if="detail" class="result">
      <el-descriptions border :column="1" size="large">
        <el-descriptions-item label="看板编号">{{ detail.kanbanCode }}</el-descriptions-item>
        <el-descriptions-item label="物料">{{ detail.materialName }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ detail.planQty }}</el-descriptions-item>
        <el-descriptions-item label="已入库">{{ detail.receivedQty }}</el-descriptions-item>
        <el-descriptions-item label="待入库">{{ remainingQty }}</el-descriptions-item>
      </el-descriptions>
      <el-input-number v-model="receiveQty" :min="1" :max="remainingQty" size="large" style="width: 100%; margin: 16px 0;" />
      <el-input v-model="operator" placeholder="操作人" size="large" style="margin-bottom: 16px;" />
      <el-button type="success" size="large" style="width: 100%;" :loading="submitLoading" :disabled="remainingQty <= 0" @click="submit">
        确认入库
      </el-button>
    </div>
    <el-empty v-else-if="batchResults.length === 0" description="请扫描、上传或输入看板编号" />
  </el-card>
</template>

<script setup>
import { ref, computed, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { scanKanban, receive } from '@/api/inbound'
import { Html5Qrcode } from 'html5-qrcode'

const cameraActive = ref(false)
const kanbanCode = ref('')
const detail = ref(null)
const receiveQty = ref(1)
const operator = ref('admin')
const queryLoading = ref(false)
const submitLoading = ref(false)

// 批量图片识别
const batchResults = ref([])
const batchSubmitting = ref(false)

const remainingQty = computed(() => {
  if (!detail.value) return 0
  return detail.value.planQty - detail.value.receivedQty
})

let html5QrCode = null

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
  const config = {
    fps: 10,
    qrbox: { width: 250, height: 250 },
    aspectRatio: 1.0,
    showTorchButtonIfSupported: true,
  }
  try {
    await html5QrCode.start(
      { facingMode: 'environment' },
      config,
      async (decodedText) => {
        ElMessage.success('识别成功，正在查询...')
        await stopScan()
        cameraActive.value = false
        kanbanCode.value = decodedText
        await scan()
      },
      (errorMessage) => {}
    )
    ElMessage.success('摄像头已启动，请对准二维码')
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

// ---- 批量图片识别 ----

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

const onImageFilesChange = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return

  ElMessage.info(`正在识别 ${file.name} ...`)
  try {
    const decodedText = await scanFileForQr(file)
    // 去重
    if (batchResults.value.some(r => r.kanbanCode === decodedText)) {
      ElMessage.warning(`${decodedText} 已在列表中，跳过`)
      return
    }
    // 查询看板信息
    const res = await scanKanban(decodedText)
    if (res.code === 200 && res.data) {
      const kb = res.data
      const maxQty = (kb.planQty || 0) - (kb.receivedQty || 0)
      batchResults.value.push({
        ...kb,
        _receiveQty: Math.max(1, maxQty),
        _operator: 'admin',
        _submitting: false
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

// 单个入库
const receiveSingle = async (row) => {
  const maxQty = (row.planQty || 0) - (row.receivedQty || 0)
  if (row._receiveQty <= 0 || row._receiveQty > maxQty) {
    ElMessage.warning('入库数量无效')
    return
  }
  row._submitting = true
  try {
    await receive({
      kanbanCode: row.kanbanCode,
      receiveQty: row._receiveQty,
      operator: row._operator || 'admin'
    })
    ElMessage.success(`${row.kanbanCode} 入库成功`)
    // 刷新看板数据
    const res = await scanKanban(row.kanbanCode)
    if (res.code === 200 && res.data) {
      const kb = res.data
      row.receivedQty = kb.receivedQty
      row.status = kb.status
      const newMax = (kb.planQty || 0) - (kb.receivedQty || 0)
      row._receiveQty = Math.max(1, newMax)
      // 如果已完成，从列表中移除
      if (kb.receivedQty >= kb.planQty) {
        const idx = batchResults.value.indexOf(row)
        if (idx > -1) batchResults.value.splice(idx, 1)
      }
    }
  } catch (e) {
    ElMessage.error('入库失败：' + (e.response?.data?.message || e.message))
  } finally {
    row._submitting = false
  }
}

// 全部入库
const receiveAll = async () => {
  if (batchResults.value.length === 0) {
    ElMessage.warning('没有待入库的看板')
    return
  }
  batchSubmitting.value = true
  let successCount = 0
  let failCount = 0
  const successCodes = new Set()
  for (const row of batchResults.value) {
    const maxQty = (row.planQty || 0) - (row.receivedQty || 0)
    if (row._receiveQty > 0 && row._receiveQty <= maxQty) {
      try {
        await receive({
          kanbanCode: row.kanbanCode,
          receiveQty: row._receiveQty,
          operator: row._operator || 'admin'
        })
        successCount++
        // 刷新并标记已完成
        const res = await scanKanban(row.kanbanCode)
        if (res.code === 200 && res.data && res.data.receivedQty >= res.data.planQty) {
          successCodes.add(row.kanbanCode)
        }
      } catch (e) {
        failCount++
      }
    } else {
      failCount++
    }
  }
  // 一次性过滤移除已完成的项
  batchResults.value = batchResults.value.filter(r => !successCodes.has(r.kanbanCode))
  ElMessage.success(`批量入库完成：成功 ${successCount}，失败 ${failCount}`)
  batchSubmitting.value = false
}

// ---- 手动输入 ----

const scan = async () => {
  if (!kanbanCode.value) {
    ElMessage.warning('请输入看板编号')
    return
  }
  queryLoading.value = true
  try {
    const res = await scanKanban(kanbanCode.value)
    if (res.code === 200 && res.data) {
      detail.value = res.data
      receiveQty.value = 1
      ElMessage.success('查询成功')
    } else {
      ElMessage.error(res.message || '看板不存在')
      detail.value = null
    }
  } catch (e) {
    ElMessage.error('看板不存在或网络错误')
    detail.value = null
  } finally {
    queryLoading.value = false
  }
}

const submit = async () => {
  if (!detail.value) return
  if (receiveQty.value <= 0 || receiveQty.value > remainingQty.value) {
    ElMessage.warning('入库数量无效')
    return
  }
  submitLoading.value = true
  try {
    await receive({
      kanbanCode: kanbanCode.value,
      receiveQty: receiveQty.value,
      operator: operator.value
    })
    ElMessage.success('入库成功')
    await scan()
  } catch (e) {
    ElMessage.error('入库失败：' + (e.response?.data?.message || e.message))
  } finally {
    submitLoading.value = false
  }
}

onUnmounted(() => {
  stopScan()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.camera-wrapper {
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
