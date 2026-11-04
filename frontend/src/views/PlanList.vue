<template>
  <el-card>
    <!-- 库存预警横幅 -->
    <el-alert v-if="alerts.length" type="error" :closable="false" class="alert-banner">
      <template #title>
        <div class="alert-title">
          <span>{{ t('plans.alert', { n: alerts.length, total: totalLack }) }}</span>
          <el-button link type="primary" @click="showAlerts = !showAlerts">{{ showAlerts ? t('plans.hide') : t('plans.details') }}</el-button>
        </div>
      </template>
    </el-alert>
    <div v-if="showAlerts && alerts.length" class="alert-table">
      <el-table :data="alerts" border size="small">
        <el-table-column prop="partCode" :label="t('common.partCode')" min-width="110" />
        <el-table-column v-if="!isMobile" prop="partName" :label="t('common.partName')" min-width="110" />
        <el-table-column prop="unit" :label="t('common.unit')" width="70" />
        <el-table-column prop="demand" :label="t('plans.demand')" width="90" align="center" />
        <el-table-column prop="stock" :label="t('plans.stock')" width="90" align="center" />
        <el-table-column :label="t('plans.lack')" width="90" align="center">
          <template #default="{ row }">
            <span class="lack">{{ t('plans.lackN', { n: row.lack }) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="planCount" :label="t('plans.planCount')" width="90" align="center" />
      </el-table>
    </div>

    <!-- 产线快捷标签 -->
    <div class="line-tabs">
      <el-radio-group v-model="lineTab">
        <el-radio-button v-for="l in tabOptions" :key="l" :value="l">{{ tabLabel(l) }}</el-radio-button>
      </el-radio-group>
      <template v-if="lineTab !== '全部'">
        <el-date-picker v-model="lineDate" type="date" value-format="YYYY-MM-DD" :clearable="false"
          style="width: 150px" />
        <span class="line-hint">{{ t('plans.lineHint', { line: lineTab, date: lineDate }) }}</span>
      </template>
    </div>

    <div class="toolbar">
      <el-checkbox v-model="onlyUndone" @change="handleSearch">{{ t('plans.onlyUndone') }}</el-checkbox>
      <div class="spacer"></div>
      <el-button type="warning" @click="openTodayParts">{{ t('plans.todayParts') }}</el-button>
      <el-button @click="openImport">{{ t('plans.import') }}</el-button>
      <el-button type="success" @click="openCreate">{{ t('plans.add') }}</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe :row-class-name="rowClass">
      <el-table-column prop="planDate" :label="t('plans.planDate')" width="110" />
      <el-table-column prop="line" :label="t('plans.line')" width="80" align="center" />
      <el-table-column prop="productCode" :label="t('plans.productCode')" min-width="100" />
      <el-table-column v-if="!isMobile" prop="productName" :label="t('plans.productName')" min-width="100" />
      <el-table-column prop="quantity" :label="t('plans.planQty')" width="90" align="center" />
      <el-table-column :label="t('plans.completed')" width="150" align="center">
        <template #default="{ row }">
          <el-input-number :model-value="row.completed" :min="0" :max="row.quantity" size="small"
            :disabled="row.planDate < today" @change="val => changeCompleted(row, val)" />
        </template>
      </el-table-column>
      <el-table-column :label="t('plans.status')" width="90" align="center">
        <template #default="{ row }">
          <el-tooltip v-if="row.shortages.length" :content="tooltip(row)" placement="top">
            <el-tag type="danger">{{ t('plans.tagShort') }}</el-tag>
          </el-tooltip>
          <el-tag v-else-if="row.completed >= row.quantity || row.planDate < today" type="success">{{ t('plans.tagDone') }}</el-tag>
          <el-tag v-else>{{ t('plans.tagReady') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!isMobile" :label="t('plans.shortageDetail')" min-width="200">
        <template #default="{ row }">
          <template v-if="row.shortages.length">
            <div v-for="s in row.shortages" :key="s.partId" class="lack-line">
              {{ t('plans.shortLine', { code: s.partCode, demand: s.demand, stock: s.stock, lack: s.lack }) }}
            </div>
          </template>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column v-if="!isMobile" prop="remark" :label="t('common.remark')" min-width="110" show-overflow-tooltip />
      <el-table-column :label="t('common.actions')" :width="isMobile ? 160 : 180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ t('plans.details') }}</el-button>
          <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          <el-popconfirm :title="t('plans.delConfirm')" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">{{ t('common.del') }}</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetch" />

    <!-- 用料汇总（可选日期） -->
    <el-dialog v-model="todayVisible" :title="t('plans.todayTitle', { date: todayDate })" width="620px">
      <div class="today-picker">
        <span>{{ t('plans.planDate') }}</span>
        <el-date-picker v-model="todayDate" type="date" value-format="YYYY-MM-DD" :clearable="false"
          style="width: 160px" />
      </div>
      <el-table :data="todayList" v-loading="todayLoading" border size="small">
        <el-table-column prop="partCode" :label="t('common.partCode')" min-width="110" />
        <el-table-column v-if="!isMobile" prop="partName" :label="t('common.partName')" min-width="110" />
        <el-table-column prop="unit" :label="t('common.unit')" width="70" align="center" />
        <el-table-column prop="totalNeed" :label="t('plans.totalNeed')" width="100" align="center">
          <template #default="{ row }">
            <span class="lack">{{ row.totalNeed }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="completedNeed" :label="t('plans.completedNeed')" width="120" align="center" />
        <el-table-column prop="planCount" :label="t('plans.planCount')" width="90" align="center" />
        <el-table-column prop="stock" :label="t('plans.stock')" width="90" align="center" />
      </el-table>
      <div v-if="!todayLoading && !todayList.length" class="detail-tip">{{ t('plans.todayEmpty') }}</div>
      <div class="detail-tip">{{ t('plans.todayTip') }}</div>
    </el-dialog>

    <!-- 导入计划（每日计划 Excel） -->
    <el-dialog v-model="importVisible" :title="t('plans.importTitle')" width="980px">
      <div class="import-bar">
        <el-button @click="fileInput.click()">{{ t('plans.chooseFile') }}</el-button>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display: none" @change="onFileChange" />
        <span class="import-file">{{ importFileName || t('plans.noFile') }}</span>
        <div class="spacer"></div>
        <span class="import-count">{{ t('plans.importMatched', { ok: importMatchedCount, total: importRows.length }) }}</span>
      </div>
      <el-table v-loading="importParsing" :data="importRows" border size="small" max-height="380">
        <el-table-column prop="line" :label="t('plans.line')" width="70" align="center" />
        <el-table-column prop="planDate" :label="t('plans.planDate')" width="105" />
        <el-table-column prop="drawNo" :label="t('plans.drawNo')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="custModel" :label="t('plans.custModel')" min-width="110" show-overflow-tooltip />
        <el-table-column :label="t('plans.planQty')" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" size="small" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column :label="t('plans.product')" min-width="200">
          <template #default="{ row }">
            <el-select v-model="row.productId" filterable clearable size="small" style="width: 100%"
              :placeholder="t('plans.pickProduct')">
              <el-option v-for="p in products" :key="p.id" :label="`${p.code} / ${p.name}`" :value="p.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="t('plans.status')" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.productId ? 'success' : 'danger'" size="small">
              {{ row.productId ? t('plans.matched') : t('plans.unmatched') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="importRows.splice($index, 1)">{{ t('common.del') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="detail-tip">{{ t('plans.importTip') }}</div>
      <template #footer>
        <el-button @click="importVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importMatchedCount" @click="doImport">
          {{ t('plans.importConfirm', { n: importMatchedCount }) }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑计划 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? t('plans.editTitle') : t('plans.createTitle')" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="t('plans.planDate')" prop="planDate">
          <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('plans.line')" prop="line">
          <el-select v-model="form.line" filterable allow-create default-first-option :placeholder="t('plans.linePh')" style="width: 100%">
            <el-option v-for="l in lineOptions" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('plans.product')" prop="productId">
          <el-select v-model="form.productId" filterable :placeholder="t('plans.productPh')" style="width: 100%">
            <el-option v-for="p in products" :key="p.id" :label="`${p.code} / ${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('plans.planQty')" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
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

    <!-- 计划零件库存详情 -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="680px">
      <template v-if="detailPlan">
        <div class="detail-meta">
          <el-tag>{{ detailPlan.line }}</el-tag>
          <span>{{ detailPlan.planDate }}</span>
          <span>{{ detailPlan.productCode }} / {{ detailPlan.productName }}</span>
          <span>{{ detailMeta }}</span>
        </div>
        <el-table :data="detailPlan.parts" border size="small">
          <el-table-column prop="partCode" :label="t('common.partCode')" min-width="100" />
          <el-table-column v-if="!isMobile" prop="partName" :label="t('common.partName')" min-width="100" />
          <el-table-column prop="unit" :label="t('common.unit')" width="60" align="center" />
          <el-table-column prop="usage" :label="t('plans.usage')" width="80" align="center" />
          <el-table-column prop="need" :label="t('plans.need')" width="95" align="center" />
          <el-table-column prop="demand" :label="t('plans.totalDemand')" width="75" align="center" />
          <el-table-column prop="stock" :label="t('plans.stock')" width="80" align="center" />
          <el-table-column :label="t('plans.status')" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.enough" type="success">{{ t('plans.enough') }}</el-tag>
              <el-tag v-else type="danger">{{ t('plans.lackN', { n: row.lack }) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="detail-tip">
          {{ t('plans.tip') }}
        </div>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import planApi from '../api/plan'
import productApi from '../api/product'
import { useResponsive } from '../composables/useResponsive'
import { useI18n } from '../i18n'

const { t } = useI18n()
const { isMobile } = useResponsive()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const onlyUndone = ref(false)

// 产线快捷标签：全部 / 1线 / 2线 / 3线...（"全部"为内部哨兵值，展示文案走 i18n）
// 本地日期 YYYY-MM-DD（toISOString 是 UTC，东八区凌晨会差一天）
const localDate = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const today = localDate(new Date())
const lineTab = ref('全部')
const lineDate = ref(today)
const lineOptions = ref(['1线', '2线', '3线'])
const tabOptions = computed(() => ['全部', ...lineOptions.value])
const tabLabel = l => (l === '全部' ? t('plans.all') : l)

const alerts = ref([])
const showAlerts = ref(false)
const totalLack = computed(() => alerts.value.reduce((sum, a) => sum + a.lack, 0))

const products = ref([])

// 计划零件库存详情弹窗
const detailVisible = ref(false)
const detailPlan = ref(null)
const detailTitle = computed(() =>
  detailPlan.value ? t('plans.detailTitle', { line: detailPlan.value.line, code: detailPlan.value.productCode }) : ''
)
// 详情完成情况：过去日期的计划视为已完成
const detailMeta = computed(() => {
  const p = detailPlan.value
  if (!p) return ''
  const done = p.planDate < today ? p.quantity : p.completed
  return t('plans.metaLine', { qty: p.quantity, done, left: p.quantity - done })
})

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, planDate: '', line: '1线', productId: null, quantity: 1, remark: '' })
const rules = computed(() => ({
  planDate: [{ required: true, message: t('plans.dateRequired'), trigger: 'change' }],
  line: [{ required: true, message: t('plans.lineRequired'), trigger: 'change' }],
  productId: [{ required: true, message: t('plans.productRequired'), trigger: 'change' }],
  quantity: [{ required: true, message: t('plans.qtyRequired'), trigger: 'blur' }]
}))

const defaultDate = () => localDate(new Date(Date.now() + 86400000))

const fetch = async () => {
  loading.value = true
  try {
    const params = { onlyUndone: onlyUndone.value, page: query.page, size: query.size }
    // 产线标签看该线指定日期（默认今天）的计划，"全部"看所有计划
    if (lineTab.value !== '全部') {
      params.line = lineTab.value
      params.date = lineDate.value
    }
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

// 产线标签或日期变化时重新查询（日期手动输入时 change 事件不可靠，统一用 watch）
watch([lineTab, lineDate], () => {
  query.page = 1
  fetch()
})

const fetchAlerts = async () => {
  alerts.value = await planApi.alerts()
}

// 用料汇总弹窗（默认今天，可切换日期）
const todayVisible = ref(false)
const todayLoading = ref(false)
const todayList = ref([])
const todayDate = ref(today)

const fetchTodayParts = async () => {
  todayLoading.value = true
  try {
    todayList.value = await planApi.todayParts(todayDate.value)
  } finally {
    todayLoading.value = false
  }
}

const openTodayParts = () => {
  todayDate.value = today
  todayVisible.value = true
  fetchTodayParts()
}

// 日期变化自动重新查询（面板点选与手动输入均生效）
watch(todayDate, () => {
  if (todayVisible.value) fetchTodayParts()
})

const handleSearch = () => {
  query.page = 1
  fetch()
  fetchAlerts()
}

/** 缺料行醒目高亮 */
const rowClass = ({ row }) => (row.shortages.length ? 'warn-row' : '')

const tooltip = row => row.shortages.map(s => `${s.partCode} ${t('plans.lackN', { n: s.lack })}`).join(', ')

const changeCompleted = async (row, val) => {
  if (val == null) return
  await planApi.updateCompleted(row.id, val)
  row.completed = val
  ElMessage.success(t('plans.completedOk'))
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
    ElMessage.success(t('common.saveOk'))
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
  ElMessage.success(t('common.deleteOk'))
  fetch()
  fetchAlerts()
}

// ---- 导入计划（每日计划 Excel） ----
const importVisible = ref(false)
const importRows = ref([])
const importFileName = ref('')
const importParsing = ref(false)
const importing = ref(false)
const fileInput = ref(null)
const importMatchedCount = computed(() => importRows.value.filter(r => r.productId).length)

const pad2 = n => String(n).padStart(2, '0')
const norm = v => String(v ?? '').trim().toUpperCase().replace(/\s+/g, '')

/** 产品索引：编码 / 名称 归一化后匹配 */
const productIndex = computed(() => {
  const map = new Map()
  for (const p of products.value) {
    if (p.code) map.set(norm(p.code), p.id)
    if (p.name) map.set(norm(p.name), p.id)
  }
  return map
})

/** Excel 日期：数字为序列号，文本支持 2026-11-04 / 11-04 等写法 */
const excelDate = v => {
  if (v === null || v === undefined || v === '') return ''
  if (typeof v === 'number') {
    const d = new Date(Date.UTC(1899, 11, 30) + Math.round(v) * 86400000)
    return `${d.getUTCFullYear()}-${pad2(d.getUTCMonth() + 1)}-${pad2(d.getUTCDate())}`
  }
  const s = String(v).trim()
  const full = s.match(/(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})/)
  if (full) return `${full[1]}-${pad2(full[2])}-${pad2(full[3])}`
  const short = s.match(/(\d{1,2})[-/.月](\d{1,2})/)
  return short ? `${new Date().getFullYear()}-${pad2(short[1])}-${pad2(short[2])}` : ''
}

/** 工作表名 LINE 1 → 1线 */
const lineFromSheet = name => {
  const m = String(name).match(/(\d+)/)
  return m ? `${Number(m[1])}线` : ''
}

const loadProducts = async () => {
  if (products.value.length) return
  const data = await productApi.list({ page: 1, size: 100 })
  products.value = data.records
}

const openImport = async () => {
  await loadProducts()
  importRows.value = []
  importFileName.value = ''
  if (fileInput.value) fileInput.value.value = ''
  importVisible.value = true
}

const onFileChange = async e => {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  importParsing.value = true
  try {
    const XLSX = await import('xlsx')
    const wb = XLSX.read(await file.arrayBuffer(), { type: 'array' })
    const rows = []
    for (const name of wb.SheetNames) {
      const line = lineFromSheet(name)
      if (!line) continue
      const aoa = XLSX.utils.sheet_to_json(wb.Sheets[name], { header: 1, defval: '', raw: true })
      const hIndex = aoa.findIndex(r => r.some(c => String(c).includes('计划生产数量')))
      if (hIndex < 0) continue
      const header = aoa[hIndex].map(c => String(c).trim())
      const col = key => header.findIndex(h => h.includes(key))
      const iDraw = col('图纸号')
      const iModel = col('客户型号')
      const iQty = col('计划生产数量')
      const iDate = col('计划生产日期')
      for (let i = hIndex + 1; i < aoa.length; i++) {
        const r = aoa[i]
        const rowHead = String(r[0] ?? '')
        if (rowHead.includes('合计') || rowHead.includes('总计')) continue
        const drawNo = String(r[iDraw] ?? '').trim()
        const quantity = Number(r[iQty])
        const planDate = excelDate(r[iDate])
        if (!drawNo || !(quantity > 0) || !planDate) continue
        const custModel = String(r[iModel] ?? '').trim()
        const productId = productIndex.value.get(norm(drawNo)) || productIndex.value.get(norm(custModel)) || null
        rows.push({ line, planDate, drawNo, custModel, quantity, productId })
      }
    }
    importRows.value = rows
    importFileName.value = file.name
    if (!rows.length) ElMessage.warning(t('plans.importEmpty'))
  } catch (err) {
    ElMessage.error(t('plans.importEmpty'))
  } finally {
    importParsing.value = false
    e.target.value = ''
  }
}

/** 只导入已匹配（或手动选择产品）的行；后端按「产线 + 日期」覆盖原有计划 */
const doImport = async () => {
  const items = importRows.value
    .filter(r => r.productId && r.quantity > 0 && r.planDate)
    .map(r => ({ planDate: r.planDate, line: r.line, productId: r.productId, quantity: r.quantity, remark: r.custModel }))
  if (!items.length) return
  importing.value = true
  try {
    const res = await planApi.importPlans(items)
    ElMessage.success(t('plans.importOk', { n: res.imported, r: res.replaced }))
    importVisible.value = false
    query.page = 1
    fetch()
    fetchAlerts()
    fetchLines()
  } finally {
    importing.value = false
  }
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
.today-picker {
  display: flex;
  align-items: center;
  gap: 10px;
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
.import-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 13px;
}
.import-file {
  color: #909399;
}
.import-count {
  color: #606266;
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
