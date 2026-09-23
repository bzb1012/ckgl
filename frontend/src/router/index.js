import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/records' },
  { path: '/admin', redirect: '/admin/parts' },
  { path: '/admin/parts', name: 'parts', component: () => import('../views/PartList.vue'), meta: { title: '零件管理' } },
  { path: '/admin/products', name: 'products', component: () => import('../views/ProductList.vue'), meta: { title: '产品管理' } },
  { path: '/admin/warehouses', name: 'warehouses', component: () => import('../views/WarehouseList.vue'), meta: { title: '仓库管理' } },
  { path: '/stocks', name: 'stocks', component: () => import('../views/StockList.vue'), meta: { title: '库存查询' } },
  { path: '/plans', name: 'plans', component: () => import('../views/PlanList.vue'), meta: { title: '生产计划' } },
  { path: '/records', name: 'records', component: () => import('../views/RecordList.vue'), meta: { title: '出入库' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.afterEach(to => {
  document.title = to.meta.title ? `${to.meta.title} - 仓库管理系统` : '仓库管理系统'
})

export default router
