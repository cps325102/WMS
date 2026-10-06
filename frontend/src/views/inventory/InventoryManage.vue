<template>
  <el-card>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="当前库存" name="list">
        <InventoryList />
      </el-tab-pane>
      <el-tab-pane label="库存追溯" name="trace">
        <InventoryTrace />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import InventoryList from './InventoryList.vue'
import InventoryTrace from './InventoryTrace.vue'

const activeTab = ref('list')
</script>