<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('parts.searchPh')" clearable style="width: 240px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.category" :placeholder="t('parts.allCategories')" clearable filterable style="width: 140px" @change="handleSearch">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
      <div class="spacer"></div>
      <el-button type="success" @click="openCreate">{{ t('parts.add') }}</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="code" :label="t('parts.code')" min-width="140" />
      <el-table-column prop="name" :label="t('parts.name')" min-width="140" />
      <el-table-column prop="category" :label="t('common.category')" width="110" />
      <el-table-column v-if="!isMobile" prop="unit" :label="t('common.unit')" width="90" />
      <el-table-column v-if="!isMobile" prop="remark" :label="t('common.remark')" min-width="160" show-overflow-tooltip />
      <el-table-column v-if="!isMobile" prop="createdAt" :label="t('common.createdAt')" width="180" :formatter="fmtTime" />
      <el-table-column :label="t('common.actions')" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          <el-popconfirm :title="t('parts.delConfirm')" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">{{ t('common.del') }}</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetch" />

    <el-dialog v-model="dialogVisible" :title="form.id ? t('parts.editTitle') : t('parts.createTitle')" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="t('parts.code')" prop="code">
          <el-input v-model="form.code" :placeholder="t('parts.codePh')" />
        </el-form-item>
        <el-form-item :label="t('parts.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('common.unit')">
          <el-input v-model="form.unit" :placeholder="t('parts.unitPh')" />
        </el-form-item>
        <el-form-item :label="t('common.category')">
          <el-select v-model="form.category" :placeholder="t('parts.categoryPh')" filterable allow-create default-first-option clearable style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
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
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
const query = reactive({ keyword: '', category: '', page: 1, size: 10 })
const categories = ref([])
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, code: '', name: '', unit: '', category: '', remark: '' })
const rules = computed(() => ({
  code: [{ required: true, message: t('parts.codeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('parts.nameRequired'), trigger: 'blur' }]
}))

const fetch = async () => {
  loading.value = true
  try {
    const data = await partApi.list(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  categories.value = await partApi.categories()
}

const handleSearch = () => {
  query.page = 1
  fetch()
}

const openCreate = () => {
  Object.assign(form, { id: null, code: '', name: '', unit: '', category: '', remark: '' })
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, { id: row.id, code: row.code, name: row.name, unit: row.unit, category: row.category || '', remark: row.remark })
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { code: form.code, name: form.name, unit: form.unit, category: form.category || '', remark: form.remark }
    if (form.id) {
      await partApi.update(form.id, payload)
    } else {
      await partApi.create(payload)
    }
    ElMessage.success(t('common.saveOk'))
    dialogVisible.value = false
    fetch()
    fetchCategories()
  } finally {
    saving.value = false
  }
}

const handleDelete = async row => {
  await partApi.remove(row.id)
  ElMessage.success(t('common.deleteOk'))
  fetch()
}

onMounted(() => {
  fetch()
  fetchCategories()
})
</script>
