<template>
  <el-card>
    <!-- 第一步：选仓库，展示货位卡片 -->
    <div class="toolbar">
      <el-select v-model="warehouseId" :placeholder="t('stockTake.selectWarehouse')" style="width: 200px" @change="onWarehouseChange">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <span v-if="currentLocation" class="loc-breadcrumb">
        <el-button @click="backToLocations">{{ t('stockTake.back') }}</el-button>
        <el-tag type="success" size="large">{{ currentLocation.code }}</el-tag>
      </span>
      <div class="spacer"></div>
      <template v-if="currentLocation">
        <el-button type="warning" @click="handleClear">{{ t('stockTake.clear') }}</el-button>
        <el-button type="primary" @click="addVisible = true">{{ t('stockTake.addPart') }}</el-button>
      </template>
    </div>

    <!-- 货位卡片 -->
    <div v-if="!currentLocation" v-loading="locLoading">
      <el-empty v-if="!warehouseId && !locLoading" :description="t('stockTake.pickWarehouse')" />
      <el-empty v-else-if="warehouseId && !locations.length && !locLoading" :description="t('stockTake.noLocations')" />
      <div v-else class="loc-grid">
        <div v-for="l in locations" :key="l.id" class="loc-card" @click="openLocation(l)">
          <div class="loc-code">{{ l.code }}</div>
        </div>
      </div>
    </div>

    <!-- 第二步：库位物料明细 -->
    <template v-else>
      <el-table :data="items" v-loading="loading" border size="default">
        <el-table-column prop="partCode" :label="t('common.partCode')" min-width="130" />
        <el-table-column v-if="!isMobile" prop="partName" :label="t('common.partName')" min-width="130" />
        <el-table-column v-if="!isMobile" prop="unit" :label="t('common.unit')" width="70" align="center" />
        <el-table-column :label="t('stockTake.quantity')" width="170" align="center">
          <template #default="{ row }">
            <el-input-number :model-value="row.quantity" :min="0" size="small" @change="val => changeQty(row, val)" />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="90" align="center">
          <template #default="{ row }">
            <el-popconfirm :title="t('stockTake.delConfirm', { code: row.partCode })" @confirm="handleRemovePart(row)">
              <template #reference>
                <el-button link type="danger">{{ t('common.del') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 添加物料弹窗 -->
    <el-dialog v-model="addVisible" :title="t('stockTake.addTitle')" width="440px">
      <el-form label-width="70px">
        <el-form-item :label="t('records.part')">
          <el-select v-model="addPartId" filterable :placeholder="t('stockTake.pickPart')" style="width: 100%">
            <el-option v-for="p in partOptions" :key="p.id" :label="`${p.code}（${p.name}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.quantity')">
          <el-input-number v-model="addQty" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleAddPart">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import warehouseApi from '../api/warehouse'
import partApi from '../api/part'
import stockApi from '../api/stock'
import { useResponsive } from '../composables/useResponsive'
import { useI18n } from '../i18n'

const { t } = useI18n()
const { isMobile } = useResponsive()

const warehouses = ref([])
const warehouseId = ref(null)
const locations = ref([])
const locLoading = ref(false)

const currentLocation = ref(null)
const items = ref([])
const loading = ref(false)
const saving = ref(false)

const addVisible = ref(false)
const addPartId = ref(null)
const addQty = ref(1)
const partOptions = ref([])

const onWarehouseChange = async val => {
  currentLocation.value = null
  items.value = []
  if (val) {
    locLoading.value = true
    try {
      locations.value = await warehouseApi.locations(val)
    } finally {
      locLoading.value = false
    }
  } else {
    locations.value = []
  }
}

const backToLocations = () => {
  currentLocation.value = null
  items.value = []
}

const openLocation = async l => {
  currentLocation.value = l
  loading.value = true
  try {
    items.value = await stockApi.takeItems(l.id)
  } finally {
    loading.value = false
  }
}

const changeQty = async (row, val) => {
  if (val == null) return
  await stockApi.takeSetQuantity(currentLocation.value.id, row.partId, val)
  row.quantity = val
  ElMessage.success(t('common.saveOk'))
}

const handleClear = async () => {
  await ElMessageBox.confirm(
    t('stockTake.clearConfirm', { code: currentLocation.value.code }),
    t('common.confirm'),
    { type: 'warning', confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel') }
  )
  const n = await stockApi.takeClear(currentLocation.value.id)
  ElMessage.success(t('stockTake.clearOk', { n }))
  items.value = items.value.map(i => ({ ...i, quantity: 0 }))
}

const handleAddPart = async () => {
  if (!addPartId.value) {
    ElMessage.warning(t('stockTake.pickFirst'))
    return
  }
  saving.value = true
  try {
    await stockApi.takeAddPart(currentLocation.value.id, addPartId.value, addQty.value)
    ElMessage.success(t('common.saveOk'))
    addVisible.value = false
    addPartId.value = null
    addQty.value = 1
    await openLocation(currentLocation.value)
  } finally {
    saving.value = false
  }
}

const handleRemovePart = async row => {
  await stockApi.takeRemovePart(currentLocation.value.id, row.partId)
  ElMessage.success(t('common.deleteOk'))
  items.value = items.value.filter(i => i.partId !== row.partId)
}

onMounted(async () => {
  warehouses.value = await warehouseApi.list()
  const data = await partApi.list({ page: 1, size: 1000 })
  partOptions.value = data.records
})
</script>

<style scoped>
.loc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 12px;
}
.loc-card {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 22px 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #f5f7fa;
}
.loc-card:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateY(-2px);
}
.loc-code {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.loc-breadcrumb {
  font-size: 15px;
}
</style>
