import { ref } from 'vue'

/**
 * 轻量国际化：语言由路由前缀驱动（/en/** 为英文，其余为中文），
 * 在 router.beforeEach 中调用 setLocale 同步。
 */
const locale = ref('zh')

export function setLocale(l) {
  locale.value = l === 'en' ? 'en' : 'zh'
}

const messages = {
  zh: {
    app: { title: '仓库管理系统' },
    menu: { records: '出入库', stocks: '库存查询', plans: '生产计划', parts: '零件管理', products: '产品管理', warehouses: '仓库管理' },
    common: {
      search: '查询', reset: '重置', save: '保存', cancel: '取消', confirm: '确定', close: '关闭',
      edit: '编辑', del: '删除', actions: '操作', remark: '备注', unit: '单位', category: '分类',
      quantity: '数量', createdAt: '创建时间', saveOk: '保存成功', deleteOk: '删除成功',
      partCode: '零件型号', partName: '零件名称', warehouse: '仓库', location: '货位'
    },
    records: {
      type: '类型', allWarehouses: '全部仓库', allLocations: '全部货位', part: '零件',
      startDate: '开始日期', endDate: '结束日期',
      inbound: '入库登记', outbound: '出库登记',
      locationCode: '货位编号', time: '时间',
      currentStock: '当前货位该零件库存：{n}',
      selectWarehouse: '请选择仓库', selectWarehouseFirst: '请先选择仓库',
      selectLocationFirst: '请先选择货位', selectLocation: '请选择货位', selectPart: '请选择零件型号',
      remarkPh: '可填写采购单号、用途等', stockTag: '库存 {n}',
      inboundOk: '入库成功', outboundOk: '出库成功'
    },
    parts: {
      searchPh: '按型号/名称搜索', allCategories: '全部分类', add: '新增零件',
      delConfirm: '确定删除该零件吗？', editTitle: '编辑零件', createTitle: '新增零件',
      code: '零件型号', codePh: '如 M8-GB5783', name: '零件名称',
      unitPh: '个 / 件 / 米', categoryPh: '选择已有分类或直接输入新分类',
      codeRequired: '请输入零件型号', nameRequired: '请输入零件名称'
    },
    products: {
      searchPh: '按编码/名称搜索', add: '新增产品',
      code: '产品编码', name: '产品名称', partCount: '零件数',
      viewParts: '查看零件', delConfirm: '确定删除该产品吗？',
      editTitle: '编辑产品', createTitle: '新增产品', codePh: '如 PRD-001',
      bindParts: '绑定零件', addPart: '添加零件',
      emptyParts: '暂未绑定零件，点击“添加零件”选择',
      usage: '使用数量', remove: '移除',
      pickerTitle: '选择零件', pickFirst: '请先勾选零件',
      partsTitle: '零件清单 - {name}', partsEmpty: '该产品暂未绑定零件',
      codeRequired: '请输入产品编码', nameRequired: '请输入产品名称'
    },
    warehouses: {
      add: '新增仓库', code: '仓库编码', name: '仓库名称', locCount: '货位数',
      locMgr: '货位管理', delConfirm: '确定删除该仓库吗？',
      editTitle: '编辑仓库', createTitle: '新增仓库', codePh: '如 WH-01',
      drawerTitle: '货位管理 - {name}', locCodePh: '货位编号，如 A-01', addLoc: '新增货位',
      locCode: '货位编号', locDelConfirm: '确定删除该货位吗？', locEditTitle: '编辑货位',
      codeRequired: '请输入仓库编码', nameRequired: '请输入仓库名称',
      locRequired: '请输入货位编号', locAdded: '货位已添加'
    },
    stocks: {
      allWarehouses: '全部仓库', allLocations: '全部货位', searchPh: '零件型号/名称/货位编号',
      locationCode: '货位编号', stockQty: '库存数量', updatedAt: '更新时间'
    },
    plans: {
      alert: '库存预警：{n} 种零件库存不足（需求 + 安全库存 {safety}），共缺 {total}',
      details: '查看详情', hide: '收起',
      demand: '需求总量', safety: '安全库存', stock: '当前库存', lack: '缺口', lackN: '缺 {n}',
      planCount: '涉及计划',
      all: '全部', lineHint: '{line} · 今日（{date}）计划',
      onlyUndone: '只看未完成', add: '新增计划',
      planDate: '计划日期', line: '产线', productCode: '产品编码', productName: '产品名称',
      planQty: '计划数量', completed: '已完成数量', status: '状态', shortageDetail: '缺料明细',
      tagShort: '缺料', tagDone: '已完成', tagReady: '可生产',
      shortLine: '{code}：需 {demand} + 安全 {safety}，库存 {stock}，缺 {lack}',
      editTitle: '编辑计划', createTitle: '新增生产计划',
      linePh: '选择或输入产线，如 1线', product: '产品', productPh: '选择产品',
      dateRequired: '请选择计划日期', lineRequired: '请选择产线', productRequired: '请选择产品', qtyRequired: '请输入计划数量',
      detailTitle: '零件库存详情 - {line} {code}',
      metaLine: '计划 {qty} · 已完成 {done} · 剩余 {left}',
      usage: '单件用量', need: '本计划需求', totalDemand: '总需求',
      enough: '足够',
      tip: '状态规则：当前库存 ≥ 总需求 + 安全库存 {safety} 视为足够；总需求为所有未完成计划的需求之和',
      completedOk: '已更新完成数量', delConfirm: '确定删除该计划吗？'
    }
  },
  en: {
    app: { title: 'Warehouse System' },
    menu: { records: 'Records', stocks: 'Inventory', plans: 'Production Plans', parts: 'Parts', products: 'Products', warehouses: 'Warehouses' },
    common: {
      search: 'Search', reset: 'Reset', save: 'Save', cancel: 'Cancel', confirm: 'OK', close: 'Close',
      edit: 'Edit', del: 'Delete', actions: 'Actions', remark: 'Remark', unit: 'Unit', category: 'Category',
      quantity: 'Qty', createdAt: 'Created', saveOk: 'Saved', deleteOk: 'Deleted',
      partCode: 'Part Code', partName: 'Part Name', warehouse: 'Warehouse', location: 'Location'
    },
    records: {
      type: 'Type', allWarehouses: 'All Warehouses', allLocations: 'All Locations', part: 'Part',
      startDate: 'Start date', endDate: 'End date',
      inbound: 'Inbound', outbound: 'Outbound',
      locationCode: 'Location', time: 'Time',
      currentStock: 'Stock at this location: {n}',
      selectWarehouse: 'Select warehouse', selectWarehouseFirst: 'Select warehouse first',
      selectLocationFirst: 'Select location first', selectLocation: 'Select location', selectPart: 'Select part',
      remarkPh: 'PO number, purpose, etc.', stockTag: 'Stock {n}',
      inboundOk: 'Inbound success', outboundOk: 'Outbound success'
    },
    parts: {
      searchPh: 'Search code / name', allCategories: 'All Categories', add: 'Add Part',
      delConfirm: 'Delete this part?', editTitle: 'Edit Part', createTitle: 'Add Part',
      code: 'Part Code', codePh: 'e.g. M8-GB5783', name: 'Part Name',
      unitPh: 'pcs / piece / m', categoryPh: 'Pick existing or type a new category',
      codeRequired: 'Part code is required', nameRequired: 'Part name is required'
    },
    products: {
      searchPh: 'Search code / name', add: 'Add Product',
      code: 'Product Code', name: 'Product Name', partCount: 'Parts',
      viewParts: 'View Parts', delConfirm: 'Delete this product?',
      editTitle: 'Edit Product', createTitle: 'Add Product', codePh: 'e.g. PRD-001',
      bindParts: 'Bound Parts', addPart: 'Add Parts',
      emptyParts: 'No parts bound. Click "Add Parts" to select.',
      usage: 'Usage Qty', remove: 'Remove',
      pickerTitle: 'Select Parts', pickFirst: 'Select parts first',
      partsTitle: 'Parts of {name}', partsEmpty: 'No parts bound to this product',
      codeRequired: 'Product code is required', nameRequired: 'Product name is required'
    },
    warehouses: {
      add: 'Add Warehouse', code: 'Warehouse Code', name: 'Warehouse Name', locCount: 'Locations',
      locMgr: 'Locations', delConfirm: 'Delete this warehouse?',
      editTitle: 'Edit Warehouse', createTitle: 'Add Warehouse', codePh: 'e.g. WH-01',
      drawerTitle: 'Locations - {name}', locCodePh: 'Location code, e.g. A-01', addLoc: 'Add Location',
      locCode: 'Location Code', locDelConfirm: 'Delete this location?', locEditTitle: 'Edit Location',
      codeRequired: 'Warehouse code is required', nameRequired: 'Warehouse name is required',
      locRequired: 'Location code is required', locAdded: 'Location added'
    },
    stocks: {
      allWarehouses: 'All Warehouses', allLocations: 'All Locations', searchPh: 'Part / name / location code',
      locationCode: 'Location', stockQty: 'Quantity', updatedAt: 'Updated'
    },
    plans: {
      alert: 'Stock alert: {n} part(s) below demand (+ safety stock {safety}), total shortage {total}',
      details: 'Details', hide: 'Hide',
      demand: 'Demand', safety: 'Safety', stock: 'Stock', lack: 'Shortage', lackN: 'Short {n}',
      planCount: 'Plans',
      all: 'All', lineHint: '{line} · today ({date}) plans',
      onlyUndone: 'Unfinished only', add: 'New Plan',
      planDate: 'Date', line: 'Line', productCode: 'Product Code', productName: 'Product Name',
      planQty: 'Planned', completed: 'Completed', status: 'Status', shortageDetail: 'Shortages',
      tagShort: 'Short', tagDone: 'Done', tagReady: 'Ready',
      shortLine: '{code}: need {demand} + safety {safety}, stock {stock}, short {lack}',
      editTitle: 'Edit Plan', createTitle: 'New Production Plan',
      linePh: 'Select or enter a line, e.g. 1', product: 'Product', productPh: 'Select product',
      dateRequired: 'Date is required', lineRequired: 'Line is required', productRequired: 'Product is required', qtyRequired: 'Quantity is required',
      detailTitle: 'Part Stock Detail - {line} {code}',
      metaLine: 'Planned {qty} · Done {done} · Left {left}',
      usage: 'Per Unit', need: 'This Plan', totalDemand: 'Total Demand',
      enough: 'OK',
      tip: 'Rule: stock >= total demand + safety stock {safety} is enough; total demand sums all unfinished plans',
      completedOk: 'Completed updated', delConfirm: 'Delete this plan?'
    }
  }
}

/** 按点路径取词 */
function lookup(obj, path) {
  let cur = obj
  for (const seg of path.split('.')) {
    cur = cur?.[seg]
    if (cur === undefined) return undefined
  }
  return cur
}

/**
 * 取词：t('parts.code')；支持参数替换 t('plans.lackN', { n: 5 })。
 * 当前语言缺词时回退中文，再回退 key 本身。
 */
export function t(path, params) {
  const val = lookup(messages[locale.value], path) ?? lookup(messages.zh, path) ?? path
  if (typeof val !== 'string') return path
  if (!params) return val
  return val.replace(/\{(\w+)\}/g, (_, k) => (params[k] !== undefined ? String(params[k]) : `{${k}}`))
}

export function useI18n() {
  return { t, locale }
}
