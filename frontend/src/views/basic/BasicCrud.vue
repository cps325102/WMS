<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteBasic, listBasic, saveBasic } from '@/api/basic'

const route = useRoute()
const keyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const rows = ref([])
const form = reactive({})
const categoryFilter = ref('')
const treeSearch = ref('')

// 树形目录筛选
const filteredCategoryTree = computed(() => {
  if (!treeSearch.value) return categoryTree
  const kw = treeSearch.value.toLowerCase()
  return categoryTree.map(group => {
    if (!group.children) {
      return group.label.toLowerCase().includes(kw) || (group.value && group.value.toLowerCase().includes(kw)) ? group : null
    }
    const matched = group.children.filter(c =>
      c.label.toLowerCase().includes(kw) || c.value.toLowerCase().includes(kw)
    )
    if (matched.length > 0) {
      return { ...group, children: matched }
    }
    // 检查组名是否匹配
    if (group.label.toLowerCase().includes(kw) || group.value.toLowerCase().includes(kw)) {
      return group
    }
    return null
  }).filter(Boolean)
})

const materialCategories = [
  { value: '发动机系统', label: '发动机系统 (26)' },
  { value: '变速箱系统', label: '变速箱系统 (16)' },
  { value: '底盘系统', label: '底盘系统 (14)' },
  { value: '制动系统', label: '制动系统 (10)' },
  { value: '转向系统', label: '转向系统 (6)' },
  { value: '冷却系统', label: '冷却系统 (6)' },
  { value: '燃油系统', label: '燃油系统 (5)' },
  { value: '排气系统', label: '排气系统 (6)' },
  { value: '空调系统', label: '空调系统 (6)' },
  { value: '车身外饰', label: '车身外饰 (20)' },
  { value: '车身内饰', label: '车身内饰 (10)' },
  { value: '电器系统', label: '电器系统 (10)' },
  { value: '安全系统', label: '安全系统 (10)' },
]

// 分类树数据
const categoryTree = [
  { value: '', label: '全部物料 (145)' },
  { value: '动力总成', label: '动力总成', children: [
    { value: '发动机系统', label: '发动机系统 (26)' },
    { value: '变速箱系统', label: '变速箱系统 (16)' },
    { value: '冷却系统', label: '冷却系统 (6)' },
    { value: '燃油系统', label: '燃油系统 (5)' },
    { value: '排气系统', label: '排气系统 (6)' },
  ]},
  { value: '底盘制动', label: '底盘与制动', children: [
    { value: '底盘系统', label: '底盘系统 (14)' },
    { value: '制动系统', label: '制动系统 (10)' },
    { value: '转向系统', label: '转向系统 (6)' },
  ]},
  { value: '车身内外饰', label: '车身与内外饰', children: [
    { value: '车身外饰', label: '车身外饰 (20)' },
    { value: '车身内饰', label: '车身内饰 (10)' },
  ]},
  { value: '电子安全', label: '电子与安全', children: [
    { value: '电器系统', label: '电器系统 (10)' },
    { value: '安全系统', label: '安全系统 (10)' },
    { value: '空调系统', label: '空调系统 (6)' },
  ]},
]

const configs = {
  material: {
    title: '物料管理',
    fields: [
      { prop: 'code', label: '物料编码' },
      { prop: 'name', label: '物料名称' },
      { prop: 'spec', label: '规格型号' },
      { prop: 'unit', label: '单位' },
      { prop: 'category', label: '所属类别' },
      { prop: 'package_qty', label: '包装容量' },
    ]
  },
  supplier: {
    title: '供应商管理',
    fields: [
      { prop: 'code', label: '供应商编码' },
      { prop: 'name', label: '供应商名称' },
      { prop: 'contact_name', label: '联系人' },
      { prop: 'contact_phone', label: '联系电话' },
      { prop: 'address', label: '地址' },
      { prop: 'province', label: '省份' },
      { prop: 'city', label: '城市' },
    ]
  },
  customer: {
    title: '客户管理',
    fields: [
      { prop: 'code', label: '客户编码' },
      { prop: 'name', label: '客户名称' },
      { prop: 'contact_name', label: '联系人' },
      { prop: 'contact_phone', label: '联系电话' },
      { prop: 'address', label: '地址' },
      { prop: 'province', label: '省份' },
      { prop: 'city', label: '城市' },
    ]
  },
  warehouse: {
    title: '仓库管理',
    fields: [
      { prop: 'code', label: '仓库编码' },
      { prop: 'name', label: '仓库名称' },
      { prop: 'address', label: '地址' },
      { prop: 'city', label: '所在城市' },
      { prop: 'type', label: '仓库类型' },
    ]
  },
  location: {
    title: '库位管理',
    fields: [
      { prop: 'warehouse_id', label: '所属仓库ID' },
      { prop: 'code', label: '库位编码' },
      { prop: 'name', label: '库位名称' },
      { prop: 'area', label: '区域' },
      { prop: 'shelf', label: '货架' },
      { prop: 'layer', label: '层' },
    ]
  }
}

const type = computed(() => route.meta.type)
const config = computed(() => configs[type.value])

const displayRows = computed(() => {
  if (type.value !== 'material' || !categoryFilter.value) return rows.value
  return rows.value.filter(r => r.category === categoryFilter.value)
})

const resetForm = () => {
  Object.keys(form).forEach(key => delete form[key])
}

const loadData = async () => {
  loading.value = true
  const res = await listBasic(type.value, { keyword: keyword.value })
  rows.value = res.data || []
  loading.value = false
}

const handleCategoryClick = (val) => {
  categoryFilter.value = val
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = row => {
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

const submit = async () => {
  await saveBasic(type.value, { ...form })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const remove = async row => {
  await ElMessageBox.confirm('确认删除当前数据吗？', '提示')
  await deleteBasic(type.value, row.id)
  ElMessage.success('删除成功')
  loadData()
}

watch(() => route.path, () => {
  categoryFilter.value = ''
  resetForm()
  loadData()
})

onMounted(() => {
  resetForm()
  loadData()
})
</script>

<template>
  <div class="crud-layout" v-if="type === 'material'">
    <!-- 左侧分类树（仅物料管理） -->
    <div class="category-sidebar">
      <div class="sidebar-title">物料分类</div>
      <div class="tree-search-box">
        <el-input v-model="treeSearch" placeholder="搜索分类..." size="small" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <el-menu :default-active="categoryFilter" @select="handleCategoryClick">
        <el-menu-item index="" v-show="!treeSearch">
          <el-icon><Grid /></el-icon>
          <span>全部物料 (145)</span>
        </el-menu-item>
        <el-sub-menu v-for="group in filteredCategoryTree.slice(treeSearch ? 0 : 1)" :key="group.value" :index="group.value">
          <template #title>
            <el-icon><Folder /></el-icon>
            <span>{{ group.label }}</span>
          </template>
          <el-menu-item v-for="child in group.children" :key="child.value" :index="child.value">
            {{ child.label }}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>
    <!-- 右侧主内容 -->
    <div class="crud-main">
      <el-card>
        <template #header>
          <div class="page-header">
            <span>{{ config.title }} <el-tag v-if="categoryFilter" size="small" type="warning" style="margin-left:8px">{{ categoryFilter }}</el-tag></span>
            <el-button type="primary" @click="openCreate">新增</el-button>
          </div>
        </template>
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="物料编码/名称/规格" clearable style="width: 260px" @keyup.enter="loadData" />
          <el-button type="primary" @click="loadData">查询</el-button>
        </div>
        <el-table v-loading="loading" :data="displayRows" border max-height="600">
          <el-table-column prop="id" label="ID" width="85" />
          <el-table-column prop="code" label="物料编码" width="130" />
          <el-table-column prop="name" label="物料名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="spec" label="规格型号" min-width="180" show-overflow-tooltip />
          <el-table-column prop="category" label="所属类别" width="120" />
          <el-table-column prop="unit" label="单位" width="70" />
          <el-table-column prop="package_qty" label="包装容量" width="90" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="scope">
              <el-button type="primary" link @click="openEdit(scope.row)">编辑</el-button>
              <el-button type="danger" link @click="remove(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
  <!-- 非物料类型的普通布局 -->
  <el-card v-else>
    <template #header>
      <div class="page-header">
        <span>{{ config.title }}</span>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </template>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="请输入关键字" clearable style="width: 260px" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border max-height="600">
      <el-table-column prop="id" label="ID" width="85" />
      <el-table-column v-for="field in config.fields" :key="field.prop" :prop="field.prop" :label="field.label" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="danger" link @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
  <!-- 编辑弹窗 -->
  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑' : '新增'" width="520px">
    <el-form label-width="100px">
      <el-form-item v-for="field in config.fields" :key="field.prop" :label="field.label">
        <el-input-number v-if="field.prop === 'package_qty'" v-model="form[field.prop]" :min="1" style="width:100%" />
        <el-input v-else v-model="form[field.prop]" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.crud-layout { display: flex; gap: 16px; height: calc(100vh - 140px); }
.category-sidebar {
  width: 240px; flex-shrink: 0; background: #fff; border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08); overflow-y: auto; display:flex; flex-direction:column;
}
.sidebar-title {
  padding: 14px 16px; font-size: 15px; font-weight: 700; color: #303133;
  border-bottom: 1px solid #ebeef5; flex-shrink: 0;
}
.tree-search-box {
  padding: 8px 12px; border-bottom: 1px solid #ebeef5; flex-shrink: 0;
}
.crud-main { flex: 1; min-width: 0; overflow: hidden; }
.page-header, .toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.toolbar { justify-content: flex-start; margin-bottom: 12px; }
</style>
