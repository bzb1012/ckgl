import { createRouter, createWebHistory } from 'vue-router'
import { setLocale, t } from '../i18n'

// 页面路由定义（/en 前缀副本由下方自动生成）
const pages = [
  { path: '/admin', redirect: '/admin/parts' },
  { path: '/admin/parts', name: 'parts', component: () => import('../views/PartList.vue'), meta: { titleKey: 'menu.parts' } },
  { path: '/admin/products', name: 'products', component: () => import('../views/ProductList.vue'), meta: { titleKey: 'menu.products' } },
  { path: '/admin/warehouses', name: 'warehouses', component: () => import('../views/WarehouseList.vue'), meta: { titleKey: 'menu.warehouses' } },
  { path: '/stocks', name: 'stocks', component: () => import('../views/StockList.vue'), meta: { titleKey: 'menu.stocks' } },
  { path: '/plans', name: 'plans', component: () => import('../views/PlanList.vue'), meta: { titleKey: 'menu.plans' } },
  { path: '/records', name: 'records', component: () => import('../views/RecordList.vue'), meta: { titleKey: 'menu.records' } }
]

const routes = [
  { path: '/', redirect: '/records' },
  { path: '/en', redirect: '/en/records' },
  ...pages,
  // /en 前缀副本：redirect 与 meta 一并映射
  ...pages.map(r =>
    r.redirect
      ? { path: '/en' + r.path, redirect: '/en' + r.redirect }
      : { path: '/en' + r.path, name: r.name + 'En', component: r.component, meta: r.meta }
  )
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 语言由路径前缀决定：/en/** 英文，其余中文
router.beforeEach(to => {
  setLocale(to.path === '/en' || to.path.startsWith('/en/') ? 'en' : 'zh')
})

router.afterEach(to => {
  document.title = to.meta.titleKey ? `${t(to.meta.titleKey)} - ${t('app.title')}` : t('app.title')
})

export default router
