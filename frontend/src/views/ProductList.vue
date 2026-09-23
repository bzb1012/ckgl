<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('products.searchPh')" clearable style="width: 240px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
      <div class="spacer"></div>
      <el-button type="success" @click="openCreate">{{ t('products.add') }}</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="code" :label="t('products.code')" min-width="130" />
      <el-table-column prop="name" :label="t('products.name')" min-width="140" />
      <el-table-column prop="partCount" :label="t('products.partCount')" width="90" />
      <el-table-column v-if="!isMobile" prop="remark" :label="t('common.remark')" min-width="150" show-overflow-tooltip />
      <el-table-column v-if="!isMobile" prop="createdAt" :label="t('common.createdAt')" width="180" :formatter="fmtTime" />
      <el-table-column :label="t('common.actions')" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openViewParts(row)">{{ t('products.viewParts') }}</el-button>
          <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          <el-popconfirm :title="t('products.delConfirm')" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">{{ t('common.del') }}</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetch" />

    <!-- 新增/编辑产品 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? t('products.editTitle') : t('products.createTitle')" width="760px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="t('products.code')" prop="code">
          <el-input v-model="form.code" :placeholder="t('products.codePh')" />
        </el-form-item>
        <el-form-item :label="t('products.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('products.bindParts')">
          <div style="width: 100%">
            <el-button type="primary" plain size="small" @click="openPartPicker">{{ t('products.addPart') }}</el-button>
            <el-table :data="form.parts" border size="small" style="margin-top: 8px" :empty-text="t('products.emptyParts')">
              <el-table-column prop="partCode" :label="t('common.partCode')" min-width="130" />
              <el-table-column v-if="!isMobile" prop="partName" :label="t('common.partName')" min-width="130" />
              <el-table-column v-if="!isMobile" prop="category" :label="t('common.category')" width="100" />
              <el-table-column v-if="!isMobile" prop="unit" :label="t('common.unit')" width="80" />
              <el-table-column :label="t('products.usage')" width="170">
                <template #default="{ row }">
                  <el-input-number v-model="row.quantity" :min="1" size="small" />
                </template>
              </el-table-column>
              <el-table-column :label="t('common.actions')" width="80">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="form.parts.splice($index, 1)">{{ t('products.remove') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 选择零件 -->
    <el-dialog v-model="pickerVisible" :title="t('products.pickerTitle')" width="640px" append-to-body>
      <div class="toolbar">
        <el-select v-model="pickerQuery.category" :placeholder="t('parts.allCategories')" clearable filterable style="width: 130px" @change="handlePickerFilter">
          <el-option v-for="c in partCategories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input v-model="pickerQuery.keyword" :placeholder="t('parts.searchPh')" clearable style="width: 200px" @keyup.enter="handlePickerFilter" @clear="handlePickerFilter" />
        <el-button type="primary" @click="handlePickerFilter">{{ t('common.search') }}</el-button>
      </div>
      <el-table :data="partList" border size="small" v-loading="pickerLoading" @selection-change="selection = $event">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="code" :label="t('common.partCode')" min-width="130" />
        <el-table-column prop="name" :label="t('common.partName')" min-width="130" />
        <el-table-column prop="category" :label="t('common.category')" width="100" />
        <el-table-column prop="unit" :label="t('common.unit')" width="80" />
      </el-table>
      <el-pagination class="pager" v-model:current-page="pickerQuery.page" :page-size="pickerQuery.size"
        :total="partTotal" layout="total, prev, pager, next" @change="fetchParts" />
      <template #footer>
        <el-button @click="pickerVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmPick">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 查看零件清单 -->
    <el-dialog v-model="viewVisible" :title="t('products.partsTitle', { name: viewProduct?.name || '' })" width="560px">
      <el-table :data="viewParts" border size="small" v-loading="viewLoading" :empty-text="t('products.partsEmpty')">
        <el-table-column prop="partCode" :label="t('common.partCode')" min-width="130" />
        <el-table-column v-if="!isMobile" prop="partName" :label="t('common.partName')" min-width="130" />
        <el-table-column v-if="!isMobile" prop="category" :label="t('common.category')" width="100" />
        <el-table-column v-if="!isMobile" prop="unit" :label="t('common.unit')" width="80" />
        <el-table-column prop="quantity" :label="t('products.usage')" width="100" />
      </el-table>
      <template #footer>
        <el-button @click="viewVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import productApi from '../api/product'
import partApi from '../api/part'
import { fmtTime } from '../utils/format'
import { useResponsive } from '../composables/useResponsive'
import { useI18n } from '../i18n'

const { t } = useI18n()
const { isMobile } = useResponsive()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 10 })

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, code: '', name: '', remark: '', parts: [] })
const rules = computed(() => ({
  code: [{ required: true, message: t('products.codeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('products.nameRequired'), trigger: 'blur' }]
}))

const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerQuery = reactive({ keyword: '', category: '', page: 1, size: 8 })
const partList = ref([])
const partTotal = ref(0)
const selection = ref([])
const partCategories = ref([])

const viewVisible = ref(false)
const viewLoading = ref(false)
const viewProduct = ref(null)
const viewParts = ref([])

/** 查看产品绑定的零件清单（只读） */
const openViewParts = async row => {
  viewProduct.value = row
  viewParts.value = []
  viewVisible.value = true
  viewLoading.value = true
  try {
    const detail = await productApi.detail(row.id)
    viewParts.value = detail.parts || []
  } finally {
    viewLoading.value = false
  }
}

const fetch = async () => {
  loading.value = true
  try {
    const data = await productApi.list(query)
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

const fetchParts = async () => {
  pickerLoading.value = true
  try {
    const data = await partApi.list({ keyword: pickerQuery.keyword, category: pickerQuery.category, page: pickerQuery.page, size: pickerQuery.size })
    partList.value = data.records
    partTotal.value = data.total
  } finally {
    pickerLoading.value = false
  }
}

const fetchPartCategories = async () => {
  partCategories.value = await partApi.categories()
}

/** 选零件弹窗：筛选条件变化，回到第一页重新查询 */
const handlePickerFilter = () => {
  pickerQuery.page = 1
  fetchParts()
}

const openPartPicker = async () => {
  pickerQuery.keyword = ''
  pickerQuery.category = ''
  pickerQuery.page = 1
  selection.value = []
  pickerVisible.value = true
  fetchPartCategories()
  fetchParts()
}

/** 将勾选零件合并进已绑列表（按 partId 去重，默认数量 1） */
const confirmPick = () => {
  if (!selection.value.length) {
    ElMessage.warning(t('products.pickFirst'))
    return
  }
  for (const p of selection.value) {
    if (!form.parts.some(x => x.partId === p.id)) {
      form.parts.push({ partId: p.id, partCode: p.code, partName: p.name, category: p.category || '', unit: p.unit, quantity: 1 })
    }
  }
  pickerVisible.value = false
}

const openCreate = () => {
  Object.assign(form, { id: null, code: '', name: '', remark: '', parts: [] })
  dialogVisible.value = true
}

const openEdit = async row => {
  const detail = await productApi.detail(row.id)
  Object.assign(form, {
    id: detail.id,
    code: detail.code,
    name: detail.name,
    remark: detail.remark,
    parts: (detail.parts || []).map(p => ({
      partId: p.partId, partCode: p.partCode, partName: p.partName, category: p.category || '', unit: p.unit, quantity: p.quantity
    }))
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      code: form.code,
      name: form.name,
      remark: form.remark,
      parts: form.parts.map(p => ({ partId: p.partId, quantity: p.quantity }))
    }
    if (form.id) {
      await productApi.update(form.id, payload)
    } else {
      await productApi.create(payload)
    }
    ElMessage.success(t('common.saveOk'))
    dialogVisible.value = false
    fetch()
  } finally {
    saving.value = false
  }
}

const handleDelete = async row => {
  await productApi.remove(row.id)
  ElMessage.success(t('common.deleteOk'))
  fetch()
}

onMounted(fetch)
</script>
