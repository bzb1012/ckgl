<template>
  <el-card>
    <div class="toolbar filters">
      <el-select v-model="query.type" clearable :placeholder="t('records.type')" style="width: 110px" @change="handleSearch">
        <el-option label="入库" value="IN" />
        <el-option label="出库" value="OUT" />
      </el-select>
      <el-select v-model="query.warehouseId" clearable :placeholder="t('records.allWarehouses')" style="width: 150px" @change="onWarehouseChange">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-select v-model="query.locationId" clearable :placeholder="t('records.allLocations')" :disabled="!query.warehouseId" style="width: 130px" @change="fetch">
        <el-option v-for="l in filterLocations" :key="l.id" :label="l.code" :value="l.id" />
      </el-select>
      <el-select v-model="query.partId" clearable filterable :placeholder="t('records.part')" style="width: 200px" @change="fetch">
        <el-option v-for="p in partOptions" :key="p.id" :label="`${p.code} (${p.name})`" :value="p.id" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
        :start-placeholder="t('records.startDate')" :end-placeholder="t('records.endDate')" style="width: 240px" @change="handleSearch" />
      <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
      <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
      <div class="spacer"></div>
      <el-button type="primary" @click="openDialog('IN')">{{ t('records.inbound') }}</el-button>
      <el-button type="warning" @click="openDialog('OUT')">{{ t('records.outbound') }}</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="type" :label="t('records.type')" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 'IN' ? 'success' : 'warning'">{{ row.type === 'IN' ? t('records.inbound') : t('records.outbound') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!isMobile" prop="warehouseName" :label="t('common.warehouse')" min-width="110" />
      <el-table-column prop="locationCode" :label="t('records.locationCode')" min-width="100" />
      <el-table-column prop="partCode" :label="t('common.partCode')" min-width="120" />
      <el-table-column v-if="!isMobile" prop="partName" :label="t('common.partName')" min-width="120" />
      <el-table-column prop="quantity" :label="t('common.quantity')" width="90" />
      <el-table-column v-if="!isMobile" prop="remark" :label="t('common.remark')" min-width="140" show-overflow-tooltip />
      <el-table-column v-if="!isMobile" prop="createdAt" :label="t('records.time')" width="180" :formatter="fmtTime" />
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetch" />

    <!-- 入库/出库登记 -->
    <el-dialog v-model="dialogVisible" :title="form.type === 'IN' ? t('records.inbound') : t('records.outbound')" width="500px" @closed="resetDialog">
      <el-alert v-if="form.type === 'OUT' && currentStock !== null"
        :title="t('records.currentStock', { n: currentStock })"
        :type="currentStock >= (form.quantity || 0) ? 'success' : 'error'" :closable="false" show-icon style="margin-bottom: 12px" />
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item :label="t('common.warehouse')">
          <el-select v-model="form.warehouseId" :placeholder="t('records.selectWarehouse')" style="width: 100%" @change="onFormWarehouseChange">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.location')" prop="locationId">
          <el-select v-model="form.locationId" :placeholder="t('records.selectWarehouseFirst')" :disabled="!form.warehouseId" style="width: 100%" @change="onFormLocationChange">
            <el-option v-for="l in formLocations" :key="l.id" :label="l.code" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('records.part')" prop="partId">
          <el-select v-model="form.partId" :placeholder="dialogPartPlaceholder" :disabled="dialogPartDisabled"
            filterable style="width: 100%" @change="fetchCurrentStock">
            <el-option v-for="p in dialogPartOptions" :key="p.id" :label="dialogPartLabel(p)" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.quantity')" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" :placeholder="t('records.remarkPh')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import recordApi from '../api/record'
import warehouseApi from '../api/warehouse'
import partApi from '../api/part'
import stockApi from '../api/stock'
import { fmtTime } from '../utils/format'
import { useResponsive } from '../composables/useResponsive'
import { useI18n } from '../i18n'

const { t } = useI18n()
const { isMobile } = useResponsive()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const warehouses = ref([])
const filterLocations = ref([])
const partOptions = ref([])
const dateRange = ref(null)
const query = reactive({ type: null, warehouseId: null, locationId: null, partId: null, page: 1, size: 10 })

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ type: 'IN', warehouseId: null, locationId: null, partId: null, quantity: 1, remark: '' })
const formLocations = ref([])
const stockParts = ref([])
const currentStock = ref(null)
const formRules = computed(() => ({
  locationId: [{ required: true, message: t('records.selectLocation'), trigger: 'change' }],
  partId: [{ required: true, message: t('records.selectPart'), trigger: 'change' }],
  quantity: [{ required: true, message: t('common.quantity'), trigger: 'blur' }]
}))

const buildParams = () => ({
  ...query,
  startTime: dateRange.value?.[0] || null,
  endTime: dateRange.value?.[1] || null
})

/** 弹窗零件下拉：出库时只展示所选货位有库存的零件，入库时展示全部 */
const dialogPartOptions = computed(() => (form.type === 'OUT' ? stockParts.value : partOptions.value))
const dialogPartDisabled = computed(() => (form.type === 'OUT' ? !form.locationId : false))
const dialogPartPlaceholder = computed(() =>
  form.type === 'OUT' ? (form.locationId ? t('records.selectPart') : t('records.selectLocationFirst')) : t('records.selectPart')
)
const dialogPartLabel = p => (form.type === 'OUT' ? `${p.code} (${p.name}) ${t('records.stockTag', { n: p.quantity })}` : `${p.code} (${p.name})`)

const fetch = async () => {
  loading.value = true
  try {
    const data = await recordApi.list(buildParams())
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  fetch()
}

const onWarehouseChange = async val => {
  query.locationId = null
  query.page = 1
  filterLocations.value = val ? await warehouseApi.locations(val) : []
  fetch()
}

const resetQuery = () => {
  query.type = null
  query.warehouseId = null
  query.locationId = null
  query.partId = null
  dateRange.value = null
  query.page = 1
  filterLocations.value = []
  fetch()
}

const openDialog = type => {
  Object.assign(form, { type, warehouseId: null, locationId: null, partId: null, quantity: 1, remark: '' })
  formLocations.value = []
  stockParts.value = []
  currentStock.value = null
  dialogVisible.value = true
}

const resetDialog = () => {
  formRef.value?.resetFields()
  formLocations.value = []
  stockParts.value = []
  currentStock.value = null
}

const onFormWarehouseChange = async val => {
  form.locationId = null
  form.partId = null
  currentStock.value = null
  stockParts.value = []
  formLocations.value = val ? await warehouseApi.locations(val) : []
}

/** 出库时货位变化：拉取该货位有库存的零件作为下拉选项 */
const onFormLocationChange = async val => {
  currentStock.value = null
  if (form.type === 'OUT') {
    form.partId = null
    if (val) {
      const data = await stockApi.list({ locationId: val, page: 1, size: 1000 })
      stockParts.value = data.records.map(r => ({ id: r.partId, code: r.partCode, name: r.partName, quantity: r.quantity }))
    } else {
      stockParts.value = []
    }
  }
  fetchCurrentStock()
}

/** 出库时查询该货位该零件的当前库存 */
const fetchCurrentStock = async () => {
  currentStock.value = null
  if (form.type === 'OUT' && form.locationId && form.partId) {
    const data = await stockApi.list({ locationId: form.locationId, partId: form.partId, page: 1, size: 1 })
    currentStock.value = data.records[0]?.quantity ?? 0
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { locationId: form.locationId, partId: form.partId, quantity: form.quantity, remark: form.remark }
    if (form.type === 'IN') {
      await recordApi.inbound(payload)
      ElMessage.success(t('records.inboundOk'))
    } else {
      await recordApi.outbound(payload)
      ElMessage.success(t('records.outboundOk'))
    }
    dialogVisible.value = false
    fetch()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  warehouses.value = await warehouseApi.list()
  const data = await partApi.list({ page: 1, size: 1000 })
  partOptions.value = data.records
  fetch()
})
</script>
