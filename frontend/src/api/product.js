import request from './request'

export default {
  list: params => request.get('/products', { params }),
  detail: id => request.get(`/products/${id}`),
  create: data => request.post('/products', data),
  update: (id, data) => request.put(`/products/${id}`, data),
  remove: id => request.delete(`/products/${id}`)
}
