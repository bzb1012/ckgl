/** 时间格式化：把后端 LocalDateTime 的 ISO 字符串转为 yyyy-MM-dd HH:mm:ss */
export function fmtTime(row, column, val) {
  return val ? String(val).replace('T', ' ').slice(0, 19) : ''
}
