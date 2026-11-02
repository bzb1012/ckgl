<template>
  <el-card>
    <el-tabs v-model="tab">
      <!-- 生产入库：月度台账 -->
      <el-tab-pane :label="t('production.tabIn')" name="in">
        <div class="toolbar">
          <el-select v-model="category" clearable :placeholder="t('production.allCategories')" style="width: 160px" @change="loadLedger">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
          <el-date-picker v-model="month" type="month" value-format="YYYY-MM" :clearable="false" style="width: 150px" @change="loadLedger" />
          <el-input v-model="partKeyword" :placeholder="t('production.searchPartPh')" clearable style="width: 180px" />
          <div class="spacer"></div>
          <el-button @click="handleSettle">{{ t('production.settle') }}</el-button>
          <el-button @click="exportLedger">{{ t('production.exportExcel') }}</el-button>
          <el-button @click="triggerImport">{{ t('production.importBtn') }}</el-button>
          <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display: none" @change="onFilePicked" />
        </div>
        <div class="tip">{{ t('production.inTip') }}</div>

        <el-empty v-if="!ledgerItems.length && !ledgerLoading" :description="partKeyword ? t('production.noMatch') : t('production.emptyParts')" />
        <el-table v-else :data="ledgerItems" v-loading="ledgerLoading" border size="small" max-height="560">
          <el-table-column prop="partCode" fixed="left" width="140" :label="t('common.partCode')" />
          <el-table-column v-if="!isMobile" prop="partName" width="150" :label="t('common.partName')" />
          <el-table-column v-if="!isMobile" prop="unit" width="60" align="center" :label="t('common.unit')" />
          <el-table-column width="110" align="center" :label="t('production.lastMonth')">
            <template #default="{ row }">{{ row.opening }}</template>
          </el-table-column>
          <el-table-column width="120" align="center" :label="t('production.current')">
            <template #default="{ row }">
              <strong :class="{ negative: row.current < 0 }">{{ row.current }}</strong>
            </template>
          </el-table-column>
          <el-table-column v-for="d in ledger.days" :key="d" width="64" align="center" :label="t('production.day', { d })">
            <template #default="{ row }">
              <input
                class="cell-input"
                type="number"
                min="0"
                step="1"
                :value="row.dailyIn?.[d - 1] || ''"
                @change="e => changeDailyIn(row, d, e.target)"
                @keydown.enter.prevent="e => e.target.blur()" />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 产线出库：每日用料汇总 + 手动累加 -->
      <el-tab-pane :label="t('production.tabOut')" name="out" lazy>
        <div class="toolbar">
          <el-date-picker v-model="outDate" type="date" value-format="YYYY-MM-DD" :clearable="false" style="width: 150px" @change="loadSummary" />
          <div class="spacer"></div>
          <span class="tip">{{ t('production.outTip') }}</span>
          <el-button @click="openAutoRun">{{ t('production.autoRun') }}</el-button>
          <el-button @click="testNotify">{{ t('production.testNotify') }}</el-button>
          <el-button @click="openConfig">{{ t('production.settings') }}</el-button>
          <el-button @click="openExport">{{ t('production.exportExcel') }}</el-button>
        </div>

        <el-empty v-if="!outRows.length && !outLoading" :description="t('production.outEmpty')" />
        <el-table v-else :data="outRows" v-loading="outLoading" border size="default">
          <el-table-column prop="partCode" width="150" :label="t('common.partCode')" />
          <el-table-column v-if="!isMobile" prop="partName" min-width="140" :label="t('common.partName')" />
          <el-table-column v-if="!isMobile" prop="unit" width="60" align="center" :label="t('common.unit')" />
          <el-table-column prop="totalNeed" width="110" align="center" :label="t('production.need')" />
          <el-table-column width="110" align="center" :label="t('production.outToday')">
            <template #default="{ row }">
              <span :class="{ done: row.outToday >= row.totalNeed }">{{ row.outToday }}</span>
            </template>
          </el-table-column>
          <el-table-column width="160" align="center" :label="t('production.thisOut')">
            <template #default="{ row }">
              <el-input-number v-model="row.inputQty" :min="1" size="small" style="width: 120px" />
            </template>
          </el-table-column>
          <el-table-column width="90" align="center" :label="t('common.actions')">
            <template #default="{ row }">
              <el-button link type="primary" @click="submitOut(row)">{{ t('production.outBtn') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 产线出库导出：选择日期范围 -->
    <el-dialog v-model="exportVisible" :title="t('production.exportExcel')" width="480px">
      <el-form label-width="90px">
        <el-form-item :label="t('production.exportRange')">
          <el-date-picker v-model="exportRange" type="daterange" value-format="YYYY-MM-DD" :clearable="false" style="width: 100%" />
        </el-form-item>
      </el-form>
      <div class="tip">{{ t('production.exportTip') }}</div>
      <template #footer>
        <el-button @click="exportVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="exporting" @click="doExportOut">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 导入库存数据：预览可编辑，确认后覆盖所选月份的上月库存 -->
    <el-dialog v-model="importVisible" :title="t('production.importTitle')" width="620px">
      <div class="tip">{{ t('production.importTip', { month: month }) }}</div>
      <el-table :data="importRows" size="small" border max-height="380">
        <el-table-column :label="t('common.partCode')" width="150">
          <template #default="{ row }">
            <el-input v-model="row.partCode" size="small" @change="rematchRow(row)" />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.partName')" min-width="130">
          <template #default="{ row }">
            <span :class="{ 'import-miss': !row.matched }">{{ row.partName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.unit')" width="60" align="center">
          <template #default="{ row }">{{ row.unit || '—' }}</template>
        </el-table-column>
        <el-table-column :label="t('stocks.stockQty')" width="130" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="0" :controls="false" size="small" style="width: 100px" />
          </template>
        </el-table-column>
        <el-table-column width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="importRows.splice($index, 1)">{{ t('common.del') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="importVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="importing" @click="confirmImport">{{ t('production.importConfirm') }}</el-button>
      </template>
    </el-dialog>
    <!-- 定时出库/预警设置 -->
    <el-dialog v-model="configVisible" :title="t('production.settingsTitle')" width="520px">
      <el-form label-width="130px">
        <el-form-item :label="t('production.intervalQty')">
          <el-input-number v-model="configForm.intervalQty" :min="1" :step="100" />
        </el-form-item>
        <el-form-item :label="t('production.intervalMin')">
          <el-input-number v-model="configForm.intervalMin" :min="5" :step="5" />
        </el-form-item>
        <el-form-item :label="t('production.alertThreshold')">
          <el-input-number v-model="configForm.alertThreshold" :min="0" :step="100" />
        </el-form-item>
        <el-form-item :label="t('production.barkUrl')">
          <el-input v-model="configForm.barkUrl" :placeholder="t('production.barkPh')" clearable />
        </el-form-item>
      </el-form>
      <div class="tip">{{ t('production.settingsTip') }}</div>
      <template #footer>
        <el-button @click="configVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="savingConfig" @click="saveConfig">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import productionApi from '../api/production'
import partApi from '../api/part'
import { useResponsive } from '../composables/useResponsive'
import { useI18n } from '../i18n'

const { t } = useI18n()
const { isMobile } = useResponsive()

// 本地日期（避免 toISOString 的 UTC 偏移）
const localDate = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const todayStr = localDate(new Date())

const tab = ref('in')
const categories = ref([])

/* ---------- 入库台账 ---------- */
const category = ref('')
const month = ref(todayStr.slice(0, 7))
const ledger = ref({ month: '', days: 31, items: [] })
const ledgerLoading = ref(false)
const partKeyword = ref('')

/* 零件型号模糊筛选（本地过滤，不影响库存数据） */
const ledgerItems = computed(() => {
  const k = partKeyword.value.trim().toLowerCase()
  if (!k) {
    return ledger.value.items
  }
  return ledger.value.items.filter(i => (i.partCode || '').toLowerCase().includes(k))
})

const prevMonth = computed(() => {
  const [y, m] = month.value.split('-').map(Number)
  return m === 1 ? `${y - 1}-12` : `${y}-${String(m - 1).padStart(2, '0')}`
})

const loadLedger = async () => {
  ledgerLoading.value = true
  try {
    ledger.value = await productionApi.ledger({ category: category.value || undefined, month: month.value })
  } finally {
    ledgerLoading.value = false
  }
}

const handleSettle = async () => {
  await ElMessageBox.confirm(
    t('production.settleConfirm', { prev: prevMonth.value, month: month.value }),
    t('production.settleTitle'),
    { type: 'warning', confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel') }
  )
  const n = await productionApi.settle(month.value)
  ElMessage.success(t('production.settleOk', { prev: prevMonth.value, month: month.value, n }))
  await loadLedger()
}

/* ---------- 导入库存数据（覆盖所选月份的上月库存） ---------- */
const fileInput = ref(null)
const importVisible = ref(false)
const importRows = ref([])
const importing = ref(false)
const partCodeMap = ref({})

const triggerImport = () => {
  fileInput.value.value = ''
  fileInput.value.click()
}

/* 修改型号后重新匹配系统零件，回填名称/单位 */
const rematchRow = row => {
  const part = partCodeMap.value[row.partCode?.trim()]
  if (part) {
    row.partName = part.name
    row.unit = part.unit
    row.matched = true
  } else {
    row.matched = false
  }
}

const onFilePicked = async e => {
  const file = e.target.files && e.target.files[0]
  if (!file) {
    return
  }
  try {
    const XLSX = await import('xlsx')
    const wb = XLSX.read(await file.arrayBuffer())
    const aoa = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1, defval: '' })
    if (aoa.length < 2) {
      ElMessage.warning(t('production.importNoRows'))
      return
    }
    // 表头按关键字定位（兼容「库存汇总」导出与无单位列的模板），找不到时退回固定列位
    const head = aoa[0].map(c => String(c))
    const idx = (kw, fallback) => {
      const i = head.findIndex(h => h.includes(kw))
      return i >= 0 ? i : fallback
    }
    const codeIdx = idx('型号', 0)
    const nameIdx = idx('名称', 1)
    const qtyIdx = idx('数量', 2)
    const unitIdx = head.findIndex(h => h.includes('单位'))

    const merged = new Map()
    for (let i = 1; i < aoa.length; i++) {
      const r = aoa[i] || []
      const code = String(r[codeIdx] ?? '').trim()
      if (!code) {
        continue
      }
      const item = merged.get(code) || { partCode: code, partName: '', unit: '', quantity: 0 }
      item.partName = item.partName || String(r[nameIdx] ?? '')
      if (unitIdx >= 0) {
        item.unit = item.unit || String(r[unitIdx] ?? '')
      }
      item.quantity += Number(r[qtyIdx]) || 0
      merged.set(code, item)
    }
    if (!merged.size) {
      ElMessage.warning(t('production.importNoRows'))
      return
    }

    const page = await partApi.list({ page: 1, size: 100000 })
    const map = {}
    for (const p of (page.records || page || [])) {
      map[p.code] = p
    }
    partCodeMap.value = map
    importRows.value = [...merged.values()].map(r => {
      const row = { ...r, quantity: r.quantity || 0 }
      rematchRow(row)
      return row
    })
    importVisible.value = true
  } catch (err) {
    ElMessage.error(t('production.importParseFail'))
  }
}

const confirmImport = async () => {
  importing.value = true
  try {
    const items = importRows.value
      .filter(r => r.partCode && r.partCode.trim())
      .map(r => ({ partCode: r.partCode.trim(), quantity: r.quantity || 0 }))
    if (!items.length) {
      ElMessage.warning(t('production.importNoRows'))
      return
    }
    const res = await productionApi.importOpening({ month: month.value, items })
    if (res.notFound && res.notFound.length) {
      ElMessage.warning(t('production.importNotFound', { n: res.notFound.length, codes: res.notFound.join(', ') }))
    } else {
      ElMessage.success(t('production.importOk', { n: res.updated }))
    }
    importVisible.value = false
    await loadLedger()
  } finally {
    importing.value = false
  }
}

/* ---------- 定时出库 / 预警配置 / Bark 通知 ---------- */
const configVisible = ref(false)
const savingConfig = ref(false)
const configForm = ref({ intervalQty: 1000, intervalMin: 30, alertThreshold: 4000, barkUrl: '' })

const openConfig = async () => {
  configForm.value = await productionApi.getConfig()
  configVisible.value = true
}

const saveConfig = async () => {
  savingConfig.value = true
  try {
    await productionApi.updateConfig(configForm.value)
    ElMessage.success(t('common.saveOk'))
    configVisible.value = false
  } finally {
    savingConfig.value = false
  }
}

/* 立即执行一轮定时出库（含库存预警检查） */
const openAutoRun = async () => {
  const cfg = await productionApi.getConfig()
  await ElMessageBox.confirm(
    t('production.autoRunConfirm', { qty: cfg.intervalQty, min: cfg.intervalMin }),
    t('production.autoRun'),
    { type: 'warning', confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel') }
  )
  const res = await productionApi.runAutoOut()
  if (res.alerts && res.alerts.length) {
    ElMessageBox.alert(res.alerts.join('\n'), t('production.alertTitle'), { type: 'warning', customStyle: { whiteSpace: 'pre-line' } })
  } else {
    ElMessage.success(t('production.autoRunOk', { n: res.plans, m: res.outKinds }))
  }
  await loadSummary()
}

const testNotify = async () => {
  const msg = await productionApi.testAlert()
  ElMessage({ type: msg.includes('成功') ? 'success' : 'warning', message: msg, duration: 5000 })
}

/* ---------- 每日入库单元格编辑 ---------- */
const changeDailyIn = async (row, day, target) => {
  if (!row.dailyIn) {
    row.dailyIn = []
  }
  const input = target.value === '' ? 0 : Math.floor(Number(target.value))
  const quantity = Number.isFinite(input) && input > 0 ? input : 0
  if (quantity === (row.dailyIn[day - 1] || 0)) {
    target.value = quantity || ''
    return
  }
  const date = `${month.value}-${String(day).padStart(2, '0')}`
  await productionApi.setIn({ partId: row.partId, date, quantity })
  row.dailyIn[day - 1] = quantity
  row.inTotal = row.dailyIn.reduce((sum, n) => sum + (n || 0), 0)
  row.current = row.opening + row.inTotal - row.outTotal
  target.value = quantity || ''
  ElMessage.success(t('common.saveOk'))
}

/* ---------- 导出 Excel ---------- */
const makeSheet = (XLSX, head, rows, widths) => {
  const ws = XLSX.utils.aoa_to_sheet([head, ...rows])
  ws['!cols'] = widths
  return ws
}

const exportLedger = async () => {
  if (!ledgerItems.value.length) {
    ElMessage.warning(t('production.exportEmpty'))
    return
  }
  const XLSX = await import('xlsx')
  const days = Array.from({ length: ledger.value.days }, (_, i) => t('production.day', { d: i + 1 }))
  const head = [
    t('common.partCode'), t('common.partName'), t('common.unit'),
    t('production.lastMonth'), t('production.current'), ...days
  ]
  const rows = ledgerItems.value.map(r => [
    r.partCode, r.partName || '', r.unit || '', r.opening, r.current,
    ...Array.from({ length: ledger.value.days }, (_, i) => r.dailyIn?.[i] || 0)
  ])
  const widths = [
    { wch: 14 }, { wch: 18 }, { wch: 6 }, { wch: 10 }, { wch: 10 },
    ...Array.from({ length: ledger.value.days }, () => ({ wch: 5 }))
  ]
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, makeSheet(XLSX, head, rows, widths), t('production.fileNameIn').slice(0, 31))
  XLSX.writeFile(wb, `${t('production.fileNameIn')}-${month.value}.xlsx`)
}

const exportVisible = ref(false)
const exporting = ref(false)
const exportRange = ref([])

const openExport = () => {
  const day = outDate.value
  exportRange.value = [`${day.slice(0, 7)}-01`, day]
  exportVisible.value = true
}

const outHead = () => [
  t('common.partCode'), t('common.partName'), t('common.unit'),
  t('production.need'), t('production.outToday')
]
const outWidths = [{ wch: 14 }, { wch: 18 }, { wch: 6 }, { wch: 12 }, { wch: 10 }]
const outExportRows = rows => rows.map(r => [r.partCode, r.partName || '', r.unit || '', r.totalNeed, r.outTotal])

const doExportOut = async () => {
  if (!exportRange.value?.length) {
    return
  }
  const [start, end] = exportRange.value
  exporting.value = true
  try {
    const XLSX = await import('xlsx')
    const data = await productionApi.dailyRange({ start, end })
    if (!data.total?.length) {
      ElMessage.warning(t('production.exportEmpty'))
      return
    }
    const wb = XLSX.utils.book_new()
    const head = outHead()
    XLSX.utils.book_append_sheet(wb, makeSheet(XLSX, head, outExportRows(data.total), outWidths), t('production.exportTotal'))
    data.days.forEach(d => {
      XLSX.utils.book_append_sheet(wb, makeSheet(XLSX, head, outExportRows(d.rows), outWidths), d.date)
    })
    XLSX.writeFile(wb, `${t('production.fileNameOut')}-${start}_${end}.xlsx`)
    exportVisible.value = false
  } finally {
    exporting.value = false
  }
}

/* ---------- 产线出库 ---------- */
const outDate = ref(todayStr)
const outRows = ref([])
const outLoading = ref(false)

const loadSummary = async () => {
  outLoading.value = true
  try {
    const rows = await productionApi.dailySummary(outDate.value)
    outRows.value = rows.map(r => ({ ...r, inputQty: 1 }))
  } finally {
    outLoading.value = false
  }
}

const submitOut = async row => {
  if (!row.inputQty || row.inputQty < 1) {
    return
  }
  await productionApi.addOut({ partId: row.partId, date: outDate.value, quantity: row.inputQty })
  ElMessage.success(t('production.outOk', { n: row.outToday + row.inputQty }))
  await loadSummary()
}

onMounted(async () => {
  categories.value = await partApi.categories()
  await Promise.all([loadLedger(), loadSummary()])
})
</script>

<style scoped>
.tip {
  color: #909399;
  font-size: 12px;
  margin-bottom: 10px;
}
.toolbar .tip {
  margin-bottom: 0;
}
.negative {
  color: #f56c6c;
}
.done {
  color: #67c23a;
  font-weight: 600;
}
/* 台账单元格：用原生 input 代替 el-input-number，31 列 × 多行时渲染更快 */
.cell-input {
  width: 100%;
  height: 24px;
  padding: 0 2px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: inherit;
  font-size: 12px;
  text-align: center;
  outline: none;
  appearance: textfield;
}
.cell-input::-webkit-outer-spin-button,
.cell-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.cell-input:hover {
  border-color: var(--el-border-color);
}
.cell-input:focus {
  border-color: var(--el-color-primary);
  background: var(--el-fill-color-blank);
}
/* 导入预览中未匹配型号的行 */
.import-miss {
  color: var(--el-color-danger);
}
</style>
