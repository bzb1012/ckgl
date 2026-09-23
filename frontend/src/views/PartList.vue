<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="按型号/名称搜索" clearable style="width: 240px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.category" placeholder="全部分类" clearable filterable style="width: 140px" @change="handleSearch">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <div class="spacer"></div>
      <el-button type="success" @click="openCreate">新增零件</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="code" label="零件型号" min-width="140" />
      <el-table-column prop="name" label="零件名称" min-width="140" />
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column v-if="!isMobile" prop="unit" label="单位" width="90" />
      <el-table-column v-if="!isMobile" prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column v-if="!isMobile" prop="createdAt" label="创建时间" width="180" :formatter="fmtTime" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该零件吗？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetch" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑零件' : '新增零件'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="零件型号" prop="code">
          <el-input v-model="form.code" placeholder="如 M8-GB5783" />
        </el-form-item>
        <el-form-item label="零件名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="个 / 件 / 米" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择已有分类或直接输入新分类" filterable allow-create default-first-option clearable style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
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
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import partApi from '../api/part'
import { fmtTime } from '../utils/format'
import { useResponsive } from '../composables/useResponsive'

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
const rules = {
  code: [{ required: true, message: '请输入零件型号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入零件名称', trigger: 'blur' }]
}

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
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetch()
    fetchCategories()
  } finally {
    saving.value = false
  }
}

const handleDelete = async row => {
  await partApi.remove(row.id)
  ElMessage.success('删除成功')
  fetch()
}

onMounted(() => {
  fetch()
  fetchCategories()
})
</script>
