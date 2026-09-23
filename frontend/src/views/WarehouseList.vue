<template>
  <el-card>
    <div class="toolbar">
      <div class="spacer"></div>
      <el-button type="success" @click="openCreate">新增仓库</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="code" label="仓库编码" min-width="120" />
      <el-table-column prop="name" label="仓库名称" min-width="140" />
      <el-table-column prop="locationCount" label="货位数" width="90" />
      <el-table-column v-if="!isMobile" prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column v-if="!isMobile" prop="createdAt" label="创建时间" width="180" :formatter="fmtTime" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openLocations(row)">货位管理</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该仓库吗？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑仓库 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑仓库' : '新增仓库'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="仓库编码" prop="code">
          <el-input v-model="form.code" placeholder="如 WH-01" />
        </el-form-item>
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 货位管理抽屉 -->
    <el-drawer v-model="drawerVisible" :title="`货位管理 - ${currentWarehouse?.name || ''}`" :size="isMobile ? '94%' : '560px'">
      <div class="toolbar">
        <el-input v-model="locForm.code" placeholder="货位编号，如 A-01" style="width: 150px" />
        <el-input v-model="locForm.remark" placeholder="备注" style="width: 150px" />
        <el-button type="primary" @click="handleAddLocation">新增货位</el-button>
      </div>
      <el-table :data="locations" v-loading="locLoading" border size="small">
        <el-table-column prop="code" label="货位编号" min-width="110" />
        <el-table-column v-if="!isMobile" prop="remark" label="备注" min-width="130" show-overflow-tooltip />
        <el-table-column v-if="!isMobile" prop="createdAt" label="创建时间" width="160" :formatter="fmtTime" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditLocation(row)">编辑</el-button>
            <el-popconfirm title="确定删除该货位吗？" @confirm="handleDeleteLocation(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 编辑货位 -->
    <el-dialog v-model="locDialogVisible" title="编辑货位" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="货位编号">
          <el-input v-model="locEditForm.code" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="locEditForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="locDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleUpdateLocation">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import warehouseApi from '../api/warehouse'
import { fmtTime } from '../utils/format'
import { useResponsive } from '../composables/useResponsive'

const { isMobile } = useResponsive()

const loading = ref(false)
const saving = ref(false)
const list = ref([])

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, code: '', name: '', remark: '' })
const rules = {
  code: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }]
}

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
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetch()
  } finally {
    saving.value = false
  }
}

const handleDelete = async row => {
  await warehouseApi.remove(row.id)
  ElMessage.success('删除成功')
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
    ElMessage.warning('请输入货位编号')
    return
  }
  await warehouseApi.addLocation(currentWarehouse.value.id, { code: locForm.code, remark: locForm.remark })
  ElMessage.success('货位已添加')
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
    ElMessage.warning('请输入货位编号')
    return
  }
  saving.value = true
  try {
    await warehouseApi.updateLocation(locEditForm.id, { code: locEditForm.code, remark: locEditForm.remark })
    ElMessage.success('保存成功')
    locDialogVisible.value = false
    fetchLocations()
  } finally {
    saving.value = false
  }
}

const handleDeleteLocation = async row => {
  await warehouseApi.removeLocation(row.id)
  ElMessage.success('删除成功')
  fetchLocations()
  fetch()
}

onMounted(fetch)
</script>
