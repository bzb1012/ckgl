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
          <div class="spacer"></div>
          <el-button @click="handleSettle">{{ t('production.settle') }}</el-button>
        </div>
        <div class="tip">{{ t('production.inTip') }}</div>

        <el-empty v-if="!ledger.items.length && !ledgerLoading" :description="t('production.emptyParts')" />
        <el-table v-else :data="ledger.items" v-loading="ledgerLoading" border size="default" max-height="560">
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
              <el-input-number
                :model-value="row.dailyIn?.[d - 1] || undefined"
                :min="0"
                :controls="false"
                size="small"
                class="cell-input"
                @change="val => changeDailyIn(row, d, val)" />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 产线出库：每日用料汇总 + 手动累加 -->
      <el-tab-pane :label="t('production.tabOut')" name="out">
        <div class="toolbar">
          <el-date-picker v-model="outDate" type="date" value-format="YYYY-MM-DD" :clearable="false" style="width: 150px" @change="loadSummary" />
          <div class="spacer"></div>
          <span class="tip">{{ t('production.outTip') }}</span>
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

/* ---------- 每日入库单元格编辑 ---------- */
const changeDailyIn = async (row, day, val) => {
  if (!row.dailyIn) {
    row.dailyIn = []
  }
  const quantity = val == null ? 0 : val
  if (quantity === (row.dailyIn[day - 1] || 0)) {
    return
  }
  const date = `${month.value}-${String(day).padStart(2, '0')}`
  await productionApi.setIn({ partId: row.partId, date, quantity })
  row.dailyIn[day - 1] = quantity
  row.inTotal = row.dailyIn.reduce((sum, n) => sum + (n || 0), 0)
  row.current = row.opening + row.inTotal - row.outTotal
  ElMessage.success(t('common.saveOk'))
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
:deep(.cell-input) {
  width: 54px;
}
:deep(.cell-input .el-input__inner) {
  text-align: center;
  padding: 0 4px;
}
</style>
