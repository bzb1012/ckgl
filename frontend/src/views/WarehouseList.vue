<template>
  <el-card>
    <div class="toolbar">
      <div class="spacer"></div>
      <el-button type="success" @click="openCreate">{{ t('warehouses.add') }}</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="code" :label="t('warehouses.code')" min-width="120" />
      <el-table-column prop="name" :label="t('warehouses.name')" min-width="140" />
      <el-table-column prop="locationCount" :label="t('warehouses.locCount')" width="90" />
      <el-table-column v-if="!isMobile" prop="remark" :label="t('common.remark')" min-width="160" show-overflow-tooltip />
      <el-table-column v-if="!isMobile" prop="createdAt" :label="t('common.createdAt')" width="180" :formatter="fmtTime" />
      <el-table-column :label="t('common.actions')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openLocations(row)">{{ t('warehouses.locMgr') }}</el-button>
          <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          <el-popconfirm :title="t('warehouses.delConfirm')" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">{{ t('common.del') }}</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑仓库 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? t('warehouses.editTitle') : t('warehouses.createTitle')" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="t('warehouses.code')" prop="code">
          <el-input v-model="form.code" :placeholder="t('warehouses.codePh')" />
        </el-form-item>
        <el-form-item :label="t('warehouses.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 货位管理抽屉 -->
    <el-drawer v-model="drawerVisible" :title="t('warehouses.drawerTitle', { name: currentWarehouse?.name || '' })" :size="isMobile ? '94%' : '560px'">
      <div class="toolbar">
        <el-input v-model="locForm.code" :placeholder="t('warehouses.locCodePh')" style="width: 150px" />
        <el-input v-model="locForm.remark" :placeholder="t('common.remark')" style="width: 150px" />
        <el-button type="primary" @click="handleAddLocation">{{ t('warehouses.addLoc') }}</el-button>
      </div>
      <el-table :data="locations" v-loading="locLoading" border size="small">
        <el-table-column prop="code" :label="t('warehouses.locCode')" min-width="110" />
        <el-table-column v-if="!isMobile" prop="remark" :label="t('common.remark')" min-width="130" show-overflow-tooltip />
        <el-table-column v-if="!isMobile" prop="createdAt" :label="t('common.createdAt')" width="160" :formatter="fmtTime" />
        <el-table-column :label="t('common.actions')" width="120">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditLocation(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm :title="t('warehouses.locDelConfirm')" @confirm="handleDeleteLocation(row)">
              <template #reference>
                <el-button link type="danger" size="small">{{ t('common.del') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 编辑货位 -->
    <el-dialog v-model="locDialogVisible" :title="t('warehouses.locEditTitle')" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item :label="t('warehouses.locCode')">
          <el-input v-model="locEditForm.code" />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="locEditForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="locDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleUpdateLocation">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import warehouseApi from '../api/warehouse'
import { fmtTime } from '../utils/format'
import { useResponsive } from '../composables/useResponsive'
import { useI18n } from '../i18n'

const { t } = useI18n()
const { isMobile } = useResponsive()

const loading = ref(false)
const saving = ref(false)
const list = ref([])

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, code: '', name: '', remark: '' })
const rules = computed(() => ({
  code: [{ required: true, message: t('warehouses.codeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('warehouses.nameRequired'), trigger: 'blur' }]
}))

const drawerVisible = ref(false)
const currentWarehouse = ref(null)
const locations = ref([])
const locLoading = ref(false)
const locForm = reactive({ code: '', remark: '' })
const locDialogVisible = ref(false)
const locEditForm = reactive({ id: null, code: '', remark: '' })

const fetch = async () => {
  loading.value = true
  try {
    list.value = await warehouseApi.list()
  } finally {
    loading.value = false
  }
}

const fetchLocations = async () => {
  if (!currentWarehouse.value) return
  locLoading.value = true
  try {
    locations.value = await warehouseApi.locations(currentWarehouse.value.id)
  } finally {
    locLoading.value = false
  }
}

const openCreate = () => {
  Object.assign(form, { id: null, code: '', name: '', remark: '' })
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, { id: row.id, code: row.code, name: row.name, remark: row.remark })
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { code: form.code, name: form.name, remark: form.remark }
    if (form.id) {
      await warehouseApi.update(form.id, payload)
    } else {
      await warehouseApi.create(payload)
    }
    ElMessage.success(t('common.saveOk'))
    dialogVisible.value = false
    fetch()
  } finally {
    saving.value = false
  }
}

const handleDelete = async row => {
  await warehouseApi.remove(row.id)
  ElMessage.success(t('common.deleteOk'))
  fetch()
}

const openLocations = row => {
  currentWarehouse.value = row
  Object.assign(locForm, { code: '', remark: '' })
  drawerVisible.value = true
  fetchLocations()
}

const handleAddLocation = async () => {
  if (!locForm.code.trim()) {
    ElMessage.warning(t('warehouses.locRequired'))
    return
  }
  await warehouseApi.addLocation(currentWarehouse.value.id, { code: locForm.code, remark: locForm.remark })
  ElMessage.success(t('warehouses.locAdded'))
  Object.assign(locForm, { code: '', remark: '' })
  fetchLocations()
  fetch()
}

const openEditLocation = row => {
  Object.assign(locEditForm, { id: row.id, code: row.code, remark: row.remark })
  locDialogVisible.value = true
}

const handleUpdateLocation = async () => {
  if (!locEditForm.code.trim()) {
    ElMessage.warning(t('warehouses.locRequired'))
    return
  }
  saving.value = true
  try {
    await warehouseApi.updateLocation(locEditForm.id, { code: locEditForm.code, remark: locEditForm.remark })
    ElMessage.success(t('common.saveOk'))
    locDialogVisible.value = false
    fetchLocations()
  } finally {
    saving.value = false
  }
}

const handleDeleteLocation = async row => {
  await warehouseApi.removeLocation(row.id)
  ElMessage.success(t('common.deleteOk'))
  fetchLocations()
  fetch()
}

onMounted(fetch)
</script>
