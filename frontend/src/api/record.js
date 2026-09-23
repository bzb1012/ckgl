import request from './request'

export default {
  inbound: data => request.post('/records/inbound', data),
  outbound: data => request.post('/records/outbound', data),
  list: params => request.get('/records', { params })
}
