<template>
  <el-card>
    <div class="toolbar filters">
      <el-select v-model="query.warehouseId" clearable :placeholder="t('stocks.allWarehouses')" style="width: 160px" @change="onWarehouseChange">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-select v-model="query.locationId" clearable :placeholder="t('stocks.allLocations')" :disabled="!query.warehouseId" style="width: 150px" @change="fetch">
        <el-option v-for="l in locations" :key="l.id" :label="l.code" :value="l.id" />
      </el-select>
      <el-input v-model="query.keyword" :placeholder="t('stocks.searchPh')" clearable style="width: 220px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
      <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column v-if="!isMobile" prop="warehouseName" :label="t('common.warehouse')" min-width="120" />
      <el-table-column prop="locationCode" :label="t('stocks.locationCode')" min-width="110" />
      <el-table-column prop="partCode" :label="t('common.partCode')" min-width="130" />
      <el-table-column v-if="!isMobile" prop="partName" :label="t('common.partName')" min-width="130" />
      <el-table-column v-if="!isMobile" prop="unit" :label="t('common.unit')" width="80" />
      <el-table-column prop="quantity" :label="t('stocks.stockQty')" width="100">
        <template #default="{ row }">
          <span :style="{ color: row.quantity > 0 ? '#303133' : '#909399' }">{{ row.quantity }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="!isMobile" prop="updatedAt" :label="t('stocks.updatedAt')" width="180" :formatter="fmtTime" />
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetch" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import warehouseApi from '../api/warehouse'
import stockApi from '../api/stock'
import { fmtTime } from '../utils/format'
import { useResponsive } from '../composables/useResponsive'
import { useI18n } from '../i18n'

const { t } = useI18n()
const { isMobile } = useResponsive()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const warehouses = ref([])
const locations = ref([])
const query = reactive({ warehouseId: null, locationId: null, keyword: '', hideZero: true, page: 1, size: 10 })

const fetch = async () => {
  loading.value = true
  try {
    const data = await stockApi.list(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const onWarehouseChange = async val => {
  query.locationId = null
  query.page = 1
  if (val) {
    locations.value = await warehouseApi.locations(val)
  } else {
    locations.value = []
  }
  fetch()
}

const handleSearch = () => {
  query.page = 1
  fetch()
}

const resetQuery = () => {
  query.warehouseId = null
  query.locationId = null
  query.keyword = ''
  query.page = 1
  locations.value = []
  fetch()
}

onMounted(async () => {
  warehouses.value = await warehouseApi.list()
  fetch()
})
</script>
