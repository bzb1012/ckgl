<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside pc-only">
      <div class="logo">仓库管理系统</div>
      <el-menu router :default-active="$route.path" class="menu" background-color="#001529" text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/records">出入库</el-menu-item>
        <el-menu-item index="/stocks">库存查询</el-menu-item>
        <el-menu-item index="/plans">生产计划</el-menu-item>
        <template v-if="isAdminArea">
          <el-menu-item index="/admin/parts">零件管理</el-menu-item>
          <el-menu-item index="/admin/products">产品管理</el-menu-item>
          <el-menu-item index="/admin/warehouses">仓库管理</el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container class="body-col">
      <header class="m-header mobile-only">
        <span class="title">仓库管理系统</span>
        <button class="burger" @click="menuOpen = true" aria-label="打开菜单">
          <span></span><span></span><span></span>
        </button>
      </header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
    <el-drawer v-model="menuOpen" direction="ltr" size="220px" :with-header="false" class="mobile-menu-drawer">
      <div class="drawer-inner">
        <div class="drawer-logo">仓库管理系统</div>
        <el-menu router :default-active="$route.path" class="menu" background-color="#001529" text-color="#bfcbd9" active-text-color="#409EFF">
          <el-menu-item index="/records">出入库</el-menu-item>
          <el-menu-item index="/stocks">库存查询</el-menu-item>
          <el-menu-item index="/plans">生产计划</el-menu-item>
          <template v-if="isAdminArea">
            <el-menu-item index="/admin/parts">零件管理</el-menu-item>
            <el-menu-item index="/admin/products">产品管理</el-menu-item>
            <el-menu-item index="/admin/warehouses">仓库管理</el-menu-item>
          </template>
        </el-menu>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useResponsive } from './composables/useResponsive'

useResponsive()
const route = useRoute()
const menuOpen = ref(false)
// 管理区菜单项仅在 /admin 路径下显示，主页不可见
const isAdminArea = computed(() => route.path.startsWith('/admin'))
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
