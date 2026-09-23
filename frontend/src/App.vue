<template>
  <el-config-provider :locale="elLocale">
    <el-container class="layout">
      <el-aside width="200px" class="aside pc-only">
        <div class="logo">{{ t('app.title') }}</div>
        <el-menu router :default-active="$route.path" class="menu" background-color="#001529" text-color="#bfcbd9" active-text-color="#409EFF">
          <el-menu-item :index="p('/records')">{{ t('menu.records') }}</el-menu-item>
          <el-menu-item :index="p('/stocks')">{{ t('menu.stocks') }}</el-menu-item>
          <el-menu-item :index="p('/plans')">{{ t('menu.plans') }}</el-menu-item>
          <template v-if="isAdminArea">
            <el-menu-item :index="p('/admin/parts')">{{ t('menu.parts') }}</el-menu-item>
            <el-menu-item :index="p('/admin/products')">{{ t('menu.products') }}</el-menu-item>
            <el-menu-item :index="p('/admin/warehouses')">{{ t('menu.warehouses') }}</el-menu-item>
          </template>
        </el-menu>
        <div class="lang-switch">
          <el-button link @click="toggleLang">{{ isEn ? '中文' : 'English' }}</el-button>
        </div>
      </el-aside>
      <el-container class="body-col">
        <header class="m-header mobile-only">
          <span class="title">{{ t('app.title') }}</span>
          <div class="m-header-right">
            <button class="lang-btn" @click="toggleLang">{{ isEn ? '中' : 'EN' }}</button>
            <button class="burger" @click="menuOpen = true" aria-label="Menu">
              <span></span><span></span><span></span>
            </button>
          </div>
        </header>
        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
      <el-drawer v-model="menuOpen" direction="ltr" size="220px" :with-header="false" class="mobile-menu-drawer">
        <div class="drawer-inner">
          <div class="drawer-logo">{{ t('app.title') }}</div>
          <el-menu router :default-active="$route.path" class="menu" background-color="#001529" text-color="#bfcbd9" active-text-color="#409EFF">
            <el-menu-item :index="p('/records')">{{ t('menu.records') }}</el-menu-item>
            <el-menu-item :index="p('/stocks')">{{ t('menu.stocks') }}</el-menu-item>
            <el-menu-item :index="p('/plans')">{{ t('menu.plans') }}</el-menu-item>
            <template v-if="isAdminArea">
              <el-menu-item :index="p('/admin/parts')">{{ t('menu.parts') }}</el-menu-item>
              <el-menu-item :index="p('/admin/products')">{{ t('menu.products') }}</el-menu-item>
              <el-menu-item :index="p('/admin/warehouses')">{{ t('menu.warehouses') }}</el-menu-item>
            </template>
          </el-menu>
          <div class="lang-switch">
            <el-button link @click="toggleLang">{{ isEn ? '中文' : 'English' }}</el-button>
          </div>
        </div>
      </el-drawer>
    </el-container>
  </el-config-provider>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { useResponsive } from './composables/useResponsive'
import { useI18n } from './i18n'

useResponsive()
const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const isEn = computed(() => locale.value === 'en')
// Element Plus 内置文案（分页/日期/空数据等）跟随语言
const elLocale = computed(() => (isEn.value ? en : zhCn))
const menuOpen = ref(false)
// 英文模式下菜单/跳转路径加 /en 前缀
const p = path => (isEn.value ? '/en' + path : path)
// 管理区菜单项仅在 /admin 路径下显示（兼容 /en/admin），主页不可见
const isAdminArea = computed(() => route.path.replace(/^\/en/, '').startsWith('/admin'))
// 中英文切换：保持当前页面，仅切换 /en 前缀
const toggleLang = () => {
  const target = isEn.value ? route.path.replace(/^\/en/, '') || '/' : '/en' + route.path
  router.replace(target)
}
// 切换页面后自动收起移动端菜单抽屉
watch(() => route.path, () => { menuOpen.value = false })
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html,
body,
#app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}
.layout {
  height: 100%;
}
.aside {
  background-color: #001529;
}
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.menu {
  border-right: none;
}
.lang-switch {
  padding: 12px 20px;
}
.main {
  background-color: #f0f2f5;
  padding: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.toolbar .spacer {
  flex: 1;
}
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

/* ---------- 移动端适配（由 html.is-mobile 根类名驱动，?mobile=1 可在桌面视口强制预览） ---------- */
.mobile-only {
  display: none;
}
html.is-mobile .pc-only {
  display: none !important;
}
html.is-mobile .mobile-only {
  display: flex;
}
.body-col {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  flex: 1;
  min-width: 0;
}
html.is-mobile .body-col {
  overflow: auto;
}
.m-header {
  height: 48px;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background-color: #001529;
  color: #fff;
  flex-shrink: 0;
}
.m-header .title {
  font-size: 16px;
  font-weight: 600;
}
.m-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.lang-btn {
  height: 26px;
  padding: 0 8px;
  font-size: 12px;
  color: #fff;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 4px;
  cursor: pointer;
}
.burger {
  width: 36px;
  height: 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
}
.burger span {
  display: block;
  width: 20px;
  height: 2px;
  background-color: #fff;
  border-radius: 1px;
}
html.is-mobile .main {
  padding: 10px;
}
html.is-mobile .el-table {
  font-size: 13px;
}
html.is-mobile .el-dialog {
  max-width: 94vw;
}
html.is-mobile .el-pagination {
  flex-wrap: wrap;
  row-gap: 4px;
  justify-content: flex-end;
}
/* 筛选区两列排布 */
html.is-mobile .filters .el-select {
  width: 48% !important;
}
html.is-mobile .filters .el-input {
  width: 48% !important;
}
html.is-mobile .filters .el-date-editor {
  width: 100% !important;
  margin-right: 0 !important;
}
/* 移动端菜单抽屉 */
.mobile-menu-drawer .el-drawer__body {
  padding: 0;
  background-color: #001529;
}
.drawer-inner {
  height: 100%;
  background-color: #001529;
}
.drawer-logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.drawer-inner .menu {
  border-right: none;
}
.drawer-inner .el-menu-item.is-active {
  background-color: rgba(64, 158, 255, 0.15);
}
</style>
