<template>
  <el-card>
    <!-- 库存预警横幅 -->
    <el-alert v-if="alerts.length" type="error" :closable="false" class="alert-banner">
      <template #title>
        <div class="alert-title">
          <span>库存预警：{{ alerts.length }} 种零件库存不足（需求 + 安全库存 {{ alerts[0].safetyStock || 100 }}），共缺 {{ totalLack }}</span>
          <el-button link type="primary" @click="showAlerts = !showAlerts">{{ showAlerts ? '收起' : '查看详情' }}</el-button>
        </div>
      </template>
    </el-alert>
    <div v-if="showAlerts && alerts.length" class="alert-table">
      <el-table :data="alerts" border size="small">
        <el-table-column prop="partCode" label="零件型号" min-width="110" />
        <el-table-column v-if="!isMobile" prop="partName" label="零件名称" min-width="110" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="demand" label="需求总量" width="90" align="center" />
        <el-table-column prop="safetyStock" label="安全库存" width="90" align="center" />
        <el-table-column prop="stock" label="当前库存" width="90" align="center" />
        <el-table-column label="缺口" width="90" align="center">
          <template #default="{ row }">
            <span class="lack">缺 {{ row.lack }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="planCount" label="涉及计划" width="90" align="center" />
      </el-table>
    </div>

    <!-- 产线快捷标签 -->
    <div class="line-tabs">
      <el-radio-group v-model="lineTab" @change="handleLineChange">
        <el-radio-button v-for="l in tabOptions" :key="l" :value="l">{{ l }}</el-radio-button>
      </el-radio-group>
      <span v-if="lineTab !== '全部'" class="line-hint">{{ lineTab }} · 今日（{{ today }}）计划</span>
    </div>

    <div class="toolbar">
      <el-checkbox v-model="onlyUndone" @change="handleSearch">只看未完成</el-checkbox>
      <div class="spacer"></div>
      <el-button type="success" @click="openCreate">新增计划</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe :row-class-name="rowClass">
      <el-table-column prop="planDate" label="计划日期" width="110" />
      <el-table-column prop="line" label="产线" width="80" align="center" />
      <el-table-column prop="productCode" label="产品编码" min-width="100" />
      <el-table-column v-if="!isMobile" prop="productName" label="产品名称" min-width="100" />
      <el-table-column prop="quantity" label="计划数量" width="90" align="center" />
      <el-table-column label="已完成数量" width="150" align="center">
        <template #default="{ row }">
          <el-input-number :model-value="row.completed" :min="0" :max="row.quantity" size="small"
            @change="val => changeCompleted(row, val)" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tooltip v-if="row.shortages.length" :content="tooltip(row)" placement="top">
            <el-tag type="danger">缺料</el-tag>
          </el-tooltip>
          <el-tag v-else-if="row.completed >= row.quantity" type="success">已完成</el-tag>
          <el-tag v-else>可生产</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!isMobile" label="缺料明细" min-width="200">
        <template #default="{ row }">
          <template v-if="row.shortages.length">
            <div v-for="s in row.shortages" :key="s.partId" class="lack-line">
              {{ s.partCode }}：需 {{ s.demand }} + 安全 {{ s.safetyStock }}，库存 {{ s.stock }}，缺 {{ s.lack }}
            </div>
          </template>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column v-if="!isMobile" prop="remark" label="备注" min-width="110" show-overflow-tooltip />
      <el-table-column label="操作" :width="isMobile ? 160 : 180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该计划吗？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetch" />

    <!-- 新增/编辑计划 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑计划' : '新增生产计划'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="计划日期" prop="planDate">
          <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="产线" prop="line">
          <el-select v-model="form.line" filterable allow-create default-first-option placeholder="选择或输入产线，如 1线" style="width: 100%">
            <el-option v-for="l in lineOptions" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品" prop="productId">
          <el-select v-model="form.productId" filterable placeholder="选择产品" style="width: 100%">
            <el-option v-for="p in products" :key="p.id" :label="`${p.code} / ${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
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

    <!-- 计划零件库存详情 -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="680px">
      <template v-if="detailPlan">
        <div class="detail-meta">
          <el-tag>{{ detailPlan.line }}</el-tag>
          <span>{{ detailPlan.planDate }}</span>
          <span>{{ detailPlan.productCode }} / {{ detailPlan.productName }}</span>
          <span>计划 {{ detailPlan.quantity }} · 已完成 {{ detailPlan.completed }} · 剩余 {{ detailPlan.quantity - detailPlan.completed }}</span>
        </div>
        <el-table :data="detailPlan.parts" border size="small">
          <el-table-column prop="partCode" label="零件型号" min-width="100" />
          <el-table-column v-if="!isMobile" prop="partName" label="零件名称" min-width="100" />
          <el-table-column prop="unit" label="单位" width="60" align="center" />
          <el-table-column prop="usage" label="单件用量" width="80" align="center" />
          <el-table-column prop="need" label="本计划需求" width="95" align="center" />
          <el-table-column prop="demand" label="总需求" width="75" align="center" />
          <el-table-column prop="stock" label="当前库存" width="80" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.enough" type="success">足够</el-tag>
              <el-tag v-else type="danger">缺 {{ row.lack }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="detail-tip">
          状态规则：当前库存 ≥ 总需求 + 安全库存 {{ detailPlan.parts[0]?.safetyStock || 100 }} 视为足够；总需求为所有未完成计划的需求之和
        </div>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import planApi from '../api/plan'
import productApi from '../api/product'
import { useResponsive } from '../composables/useResponsive'

const { isMobile } = useResponsive()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const onlyUndone = ref(false)

// 产线快捷标签：全部 / 1线 / 2线 / 3线...
const lineTab = ref('全部')
const lineOptions = ref(['1线', '2线', '3线'])
const tabOptions = computed(() => ['全部', ...lineOptions.value])
const today = new Date().toISOString().slice(0, 10)

const alerts = ref([])
const showAlerts = ref(false)
const totalLack = computed(() => alerts.value.reduce((sum, a) => sum + a.lack, 0))

const products = ref([])

// 计划零件库存详情弹窗
const detailVisible = ref(false)
const detailPlan = ref(null)
const detailTitle = computed(() => (detailPlan.value ? `零件库存详情 - ${detailPlan.value.line} ${detailPlan.value.productCode}` : ''))

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, planDate: '', line: '1线', productId: null, quantity: 1, remark: '' })
const rules = {
  planDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }],
  line: [{ required: true, message: '请选择产线', trigger: 'change' }],
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入计划数量', trigger: 'blur' }]
}

const defaultDate = () => {
  const d = new Date(Date.now() + 86400000)
  return d.toISOString().slice(0, 10)
}

const fetch = async () => {
  loading.value = true
  try {
    const params = { onlyUndone: onlyUndone.value, page: query.page, size: query.size }
    // 产线标签只看该线今日计划，"全部"看所有计划
    if (lineTab.value !== '全部') params.line = lineTab.value
    const data = await planApi.list(params)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const fetchLines = async () => {
  lineOptions.value = await planApi.lines()
}

const handleLineChange = () => {
  query.page = 1
  fetch()
}

const fetchAlerts = async () => {
  alerts.value = await planApi.alerts()
}

const handleSearch = () => {
  query.page = 1
  fetch()
  fetchAlerts()
}

/** 缺料行醒目高亮 */
const rowClass = ({ row }) => (row.shortages.length ? 'warn-row' : '')

const tooltip = row => row.shortages.map(s => `${s.partCode} 缺 ${s.lack}`).join('；')

const changeCompleted = async (row, val) => {
  if (val == null) return
  await planApi.updateCompleted(row.id, val)
  row.completed = val
  ElMessage.success('已更新完成数量')
  fetchAlerts()
  // 已完成数量变化可能影响状态与其他计划的预警判断
  fetch()
}

const openDetail = row => {
  detailPlan.value = row
  detailVisible.value = true
}

const openCreate = async () => {
  Object.assign(form, { id: null, planDate: defaultDate(), line: lineTab.value === '全部' ? '1线' : lineTab.value, productId: null, quantity: 1, remark: '' })
  if (!products.value.length) {
    const data = await productApi.list({ page: 1, size: 100 })
    products.value = data.records
  }
  dialogVisible.value = true
}

const openEdit = async row => {
  Object.assign(form, { id: row.id, planDate: row.planDate, line: row.line, productId: row.productId, quantity: row.quantity, remark: row.remark })
  if (!products.value.length) {
    const data = await productApi.list({ page: 1, size: 100 })
    products.value = data.records
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { planDate: form.planDate, line: form.line, productId: form.productId, quantity: form.quantity, remark: form.remark }
    if (form.id) {
      await planApi.update(form.id, payload)
    } else {
      await planApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetch()
    fetchAlerts()
    fetchLines()
  } finally {
    saving.value = false
  }
}

const handleDelete = async row => {
  await planApi.remove(row.id)
  ElMessage.success('删除成功')
  fetch()
  fetchAlerts()
}

onMounted(() => {
  fetch()
  fetchAlerts()
  fetchLines()
})
</script>

<style scoped>
.alert-banner {
  margin-bottom: 14px;
}
.line-tabs {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.line-hint {
  color: #909399;
  font-size: 13px;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-size: 13px;
}
.detail-tip {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}
.alert-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}
.alert-table {
  margin-bottom: 14px;
}
.lack {
  color: #f56c6c;
  font-weight: 600;
}
.lack-line {
  color: #f56c6c;
  font-size: 12px;
  line-height: 20px;
}
</style>

<style>
/* 缺料计划整行醒目底色 */
.el-table .warn-row td.el-table__cell {
  background-color: #fef0f0 !important;
}
</style>
