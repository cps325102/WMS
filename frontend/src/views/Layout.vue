<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMenuList } from '@/api/menu'
import { useTabsStore } from '@/stores/tabs'

const route = useRoute()
const router = useRouter()
const tabsStore = useTabsStore()
const menus = ref([])
const userInfo = computed(() => JSON.parse(localStorage.getItem('userInfo') || '{}'))

// 扁平化菜单（用于路径匹配）
const flatMenus = computed(() => {
  const result = []
  for (const item of menus.value) {
    if (item.children) {
      for (const child of item.children) {
        result.push(child)
      }
    } else {
      result.push(item)
    }
  }
  return result
})

const findMenuByPath = (path) => flatMenus.value.find(item => item.path === path)

const loadMenus = async () => {
  const res = await getMenuList()
  menus.value = res.data || []
}

const handleMenuClick = menu => {
  tabsStore.addTab(menu)
  router.push(menu.path)
}

const handleTabClick = pane => {
  tabsStore.setActive(pane.props.name)
  router.push(pane.props.name)
}

const handleTabRemove = path => {
  tabsStore.removeTab(path)
  router.push(tabsStore.activePath)
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
}

watch(
  () => route.path,
  path => {
    const menu = findMenuByPath(path)
    if (menu) {
      tabsStore.addTab(menu)
    } else if (route.meta.title) {
      tabsStore.addTab({ title: route.meta.title, path })
    } else if (path === '/home') {
      tabsStore.setActive('/home')
    }
  }
)

onMounted(async () => {
  await loadMenus()
  const menu = findMenuByPath(route.path)
  if (menu) {
    tabsStore.addTab(menu)
  } else if (route.meta.title) {
    tabsStore.addTab({ title: route.meta.title, path: route.path })
  }
})
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">WMS 仓库管理系统</div>
      <el-menu
        :default-active="tabsStore.activePath"
        class="menu"
        background-color="#304156"
        text-color="#d7dde8"
        active-text-color="#409eff"
      >
        <template v-for="item in menus" :key="item.title">
          <!-- 分组菜单 -->
          <el-sub-menu v-if="item.children" :index="item.title">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
              @click="handleMenuClick(child)"
            >
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 独立菜单项 -->
          <el-menu-item
            v-else
            :index="item.path"
            @click="handleMenuClick(item)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div>汽车物流信息系统方向开发实训</div>
        <div class="user-area">
          <span>{{ userInfo.nickname || '管理员' }}</span>
          <el-button type="primary" link @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <el-tabs
          v-model="tabsStore.activePath"
          type="card"
          class="tabs"
          @tab-click="handleTabClick"
          @tab-remove="handleTabRemove"
        >
          <el-tab-pane
            v-for="tab in tabsStore.tabs"
            :key="tab.path"
            :label="tab.title"
            :name="tab.path"
            :closable="tab.closable"
          />
        </el-tabs>
        <div class="content">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  flex-direction: row;
}

.aside {
  background: #304156;
  width: 220px;
  flex-shrink: 0;
  overflow-y: auto;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-weight: 700;
  background: #263445;
}

.menu {
  border-right: none;
}

.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  font-size: 18px;
  font-weight: 700;
  padding: 0 20px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  font-weight: 400;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: #f4f6f9;
}
@media (max-width: 768px) {
  .aside {
    width: 180px;
  }
  .header {
    font-size: 14px;
    padding: 0 10px;
  }
  .logo {
    font-size: 14px;
  }
}
.tabs {
  padding: 8px 12px 0;
  background: #fff;
}

.content {
  padding: 16px;
}
</style>
