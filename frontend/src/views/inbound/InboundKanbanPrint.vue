<template>
  <el-card>
    <div class="actions no-print">
      <el-button @click="router.push('/inbound/manage')">返回</el-button>
      <div class="nav-buttons" v-if="kanbanIdList.length > 1">
        <el-button :disabled="currentIndex <= 0" @click="goToPrev">上一张</el-button>
        <span class="page-indicator">第 {{ currentIndex + 1 }} / {{ kanbanIdList.length }} 张</span>
        <el-button :disabled="currentIndex >= kanbanIdList.length - 1" @click="goToNext">下一张</el-button>
      </div>
      <el-button type="primary" @click="printPage">打印看板</el-button>
      <el-button type="success" @click="saveAsImage">保存图片</el-button>
    </div>
    <div class="kanban-card" v-if="detail.kanbanCode">
      <h2>入库看板</h2>
      <div class="code-box">{{ detail.kanbanCode }}</div>
      <div v-if="detail.packageInfo" class="package-info">
        包装：{{ detail.packageInfo }}
      </div>
      <el-descriptions border :column="2">
        <el-descriptions-item label="入库单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="物料编码">{{ detail.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ detail.materialName }}</el-descriptions-item>
        <el-descriptions-item label="本包装数量">{{ detail.planQty }}</el-descriptions-item>
        <el-descriptions-item label="已入库数量">{{ detail.receivedQty }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="库位">{{ detail.locationName }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detail.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="打印次数">{{ detail.printCount }}</el-descriptions-item>
      </el-descriptions>
      <div class="qr">
        <img :src="qrCodeDataUrl" alt="二维码" v-if="qrCodeDataUrl" />
        <span v-else>加载二维码中...</span>
      </div>
    </div>
    <el-empty v-else description="看板不存在" />
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { printKanban } from '@/api/inbound'
import QRCode from 'qrcode'
import html2canvas from 'html2canvas'

const route = useRoute()
const router = useRouter()
const detail = ref({})
const qrCodeDataUrl = ref('')

// 从 query 中解析所有看板 ID 列表，支持上一张/下一张导航
const kanbanIdList = computed(() => {
  const ids = route.query.ids
  if (!ids) return []
  return ids.split(',').map(Number).filter(Boolean)
})

const currentId = computed(() => Number(route.params.id))
const currentIndex = computed(() => kanbanIdList.value.indexOf(currentId.value))

const goToPrev = () => {
  if (currentIndex.value > 0) {
    const prevId = kanbanIdList.value[currentIndex.value - 1]
    router.replace({ path: `/inbound/kanban/print/${prevId}`, query: { ids: route.query.ids } })
  }
}

const goToNext = () => {
  if (currentIndex.value < kanbanIdList.value.length - 1) {
    const nextId = kanbanIdList.value[currentIndex.value + 1]
    router.replace({ path: `/inbound/kanban/print/${nextId}`, query: { ids: route.query.ids } })
  }
}

const printPage = () => window.print()

const saveAsImage = async () => {
  if (!detail.value.kanbanCode) return
  const el = document.querySelector('.kanban-card')
  if (!el) {
    ElMessage.error('未找到看板卡片元素')
    return
  }
  try {
    const canvas = await html2canvas(el, {
      scale: 2,
      backgroundColor: '#ffffff',
      useCORS: true
    })
    const link = document.createElement('a')
    link.download = `${detail.value.kanbanCode}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    ElMessage.success('看板图片已保存')
  } catch (err) {
    console.error('保存图片失败', err)
    ElMessage.error('保存图片失败')
  }
}

const generateQRCode = async (text) => {
  try {
    const dataUrl = await QRCode.toDataURL(text, {
      width: 200,
      margin: 2,
      errorCorrectionLevel: 'M'
    })
    qrCodeDataUrl.value = dataUrl
  } catch (err) {
    console.error('生成二维码失败', err)
  }
}

const loadKanban = async (id) => {
  detail.value = {}
  qrCodeDataUrl.value = ''
  const res = await printKanban(id)
  detail.value = res.data || {}
  if (detail.value.kanbanCode) {
    await generateQRCode(detail.value.kanbanCode)
  }
}

onMounted(() => {
  loadKanban(currentId.value)
})

// 监听到路由参数变化（上一张/下一张）时重新加载
watch(currentId, (newId) => {
  if (newId) loadKanban(newId)
})
</script>

<style scoped>
.actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.nav-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: auto;
}
.page-indicator {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  min-width: 100px;
  text-align: center;
}
.kanban-card {
  width: 720px;
  margin: 0 auto;
  padding: 24px;
  border: 2px solid #333;
  background: #fff;
}
h2 {
  text-align: center;
  margin: 0 0 16px;
}
.code-box {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 16px;
  letter-spacing: 2px;
}
.package-info {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 12px;
}
.qr {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
.qr img {
  width: 200px;
  height: 200px;
  border: 1px solid #ccc;
  padding: 8px;
}
@media print {
  .no-print {
    display: none;
  }
  .kanban-card {
    border: 1px solid #000;
    box-shadow: none;
    page-break-after: avoid;
    break-inside: avoid;
  }
}
</style>
