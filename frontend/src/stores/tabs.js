import { defineStore } from 'pinia'

export const useTabsStore = defineStore('tabs', {
  state: () => ({
    tabs: [
      {
        title: '首页',
        path: '/home',
        closable: false
      }
    ],
    activePath: '/home'
  }),
  actions: {
    addTab(tab) {
      const exists = this.tabs.find(item => item.path === tab.path)
      if (!exists) {
        this.tabs.push({
          title: tab.title,
          path: tab.path,
          closable: tab.path !== '/home'
        })
      }
      this.activePath = tab.path
    },
    removeTab(path) {
      const index = this.tabs.findIndex(item => item.path === path)
      if (index === -1) return
      if (this.tabs[index].path === '/home') return
      this.tabs.splice(index, 1)
      if (this.activePath === path) {
        const nextTab = this.tabs[index - 1] || this.tabs[0]
        this.activePath = nextTab.path
      }
    },
    setActive(path) {
      this.activePath = path
    }
  }
})
