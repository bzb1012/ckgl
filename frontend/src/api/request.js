import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.response.use(
  resp => {
    const r = resp.data
    // 后端统一返回 { code, message, data }，code=0 表示成功
    if (r && typeof r.code !== 'undefined') {
      if (r.code === 0) {
        return r.data
      }
      ElMessage.error(r.message || '操作失败')
      return Promise.reject(new Error(r.message || '操作失败'))
    }
    return r
  },
  err => {
    const msg = err.response?.data?.message || err.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default request
