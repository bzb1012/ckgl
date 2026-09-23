<template>
  <el-card>
    <div class="toolbar filters">
      <el-select v-model="query.type" clearable placeholder="类型" style="width: 110px" @change="handleSearch">
        <el-option label="入库" value="IN" />
        <el-option label="出库" value="OUT" />
      </el-select>
      <el-select v-model="query.warehouseId" clearable placeholder="全部仓库" style="width: 150px" @change="onWarehouseChange">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-select v-model="query.locationId" clearable placeholder="全部货位" :disabled="!query.warehouseId" style="width: 130px" @change="fetch">
        <el-option v-for="l in filterLocations" :key="l.id" :label="l.code" :value="l.id" />
      </el-select>
      <el-select v-model="query.partId" clearable filterable placeholder="零件" style="width: 200px" @change="fetch">
        <el-option v-for="p in partOptions" :key="p.id" :label="`${p.code}（${p.name}）`" :value="p.id" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
        start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px" @change="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" @click="openDialog('IN')">入库登记</el-button>
      <el-button type="warning" @click="openDialog('OUT')">出库登记</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 'IN' ? 'success' : 'warning'">{{ row.type === 'IN' ? '入库' : '出库' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!isMobile" prop="warehouseName" label="仓库" min-width="110" />
      <el-table-column prop="locationCode" label="货位编号" min-width="100" />
      <el-table-column prop="partCode" label="零件型号" min-width="120" />
      <el-table-column v-if="!isMobile" prop="partName" label="零件名称" min-width="120" />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column v-if="!isMobile" prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column v-if="!isMobile" prop="createdAt" label="时间" width="180" :formatter="fmtTime" />
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetch" />

    <!-- 入库/出库登记 -->
    <el-dialog v-model="dialogVisible" :title="form.type === 'IN' ? '入库登记' : '出库登记'" width="500px" @closed="resetDialog">
      <el-alert v-if="form.type === 'OUT' && currentStock !== null"
        :title="`当前货位该零件库存：${currentStock}`"
        :type="currentStock >= (form.quantity || 0) ? 'success' : 'error'" :closable="false" show-icon style="margin-bottom: 12px" />
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="仓库">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width: 100%" @change="onFormWarehouseChange">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="货位" prop="locationId">
          <el-select v-model="form.locationId" placeholder="请先选择仓库" :disabled="!form.warehouseId" style="width: 100%" @change="fetchCurrentStock">
            <el-option v-for="l in formLocations" :key="l.id" :label="l.code" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="零件" prop="partId">
          <el-select v-model="form.partId" placeholder="请选择零件型号" filterable style="width: 100%" @change="fetchCurrentStock">
            <el-option v-for="p in partOptions" :key="p.id" :label="`${p.code}（${p.name}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可填写采购单号、用途等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">提交</el-button>
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
const currentStock = ref(null)
const formRules = {
  locationId: [{ required: true, message: '请选择货位', trigger: 'change' }],
  partId: [{ required: true, message: '请选择零件', trigger: 'change' }],
  quantity: [{ required: true, message: '请填写数量', trigger: 'blur' }]
}

const buildParams = () => ({
  ...query,
  startTime: dateRange.value?.[0] || null,
  endTime: dateRange.value?.[1] || null
})

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
  currentStock.value = null
  dialogVisible.value = true
}

const resetDialog = () => {
  formRef.value?.resetFields()
  formLocations.value = []
  currentStock.value = null
}

const onFormWarehouseChange = async val => {
  form.locationId = null
  currentStock.value = null
  formLocations.value = val ? await warehouseApi.locations(val) : []
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
      ElMessage.success('入库成功')
    } else {
      await recordApi.outbound(payload)
      ElMessage.success('出库成功')
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
