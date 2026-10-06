<template>
  <el-card>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="入库单列表" name="orders">
        <InboundOrderList />
      </el-tab-pane>
      <el-tab-pane label="入库看板" name="kanbans">
        <InboundKanbanList ref="kanbanListRef" />
      </el-tab-pane>
      <el-tab-pane label="扫码入库" name="scan">
        <InboundScan />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import InboundOrderList from './InboundOrderList.vue'
import InboundKanbanList from './InboundKanbanList.vue'
import InboundScan from './InboundScan.vue'

const route = useRoute()
const activeTab = ref('orders')
const kanbanListRef = ref(null)

// 处理标签切换
const handleTabChange = (tab) => {
  // 可以在这里添加切换逻辑
}

// 加载看板数据的方法
const loadKanbanData = async (keywordParam) => {
  await nextTick()
  setTimeout(() => {
    if (kanbanListRef.value) {
      console.log('设置关键字并加载数据:', keywordParam)
      kanbanListRef.value.setKeyword(keywordParam)
      kanbanListRef.value.loadData()
    }
  }, 300)
}

// 监听路由变化
watch(() => route.query, (newQuery) => {
  if (newQuery.tab === 'kanbans' && newQuery.keyword) {
    activeTab.value = 'kanbans'
    loadKanbanData(newQuery.keyword)
  }
}, { immediate: false })

onMounted(async () => {
  // 从 URL 参数中读取 tab 和 keyword
  const tabParam = route.query.tab
  const keywordParam = route.query.keyword

  if (tabParam) {
    activeTab.value = tabParam
  }

  // 如果有关键字参数且切换到看板标签，需要传递关键字
  if (keywordParam && activeTab.value === 'kanbans') {
    await loadKanbanData(keywordParam)
  }
})
</script>
