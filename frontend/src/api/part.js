import request from './request'

export default {
  list: params => request.get('/parts', { params }),
  categories: () => request.get('/parts/categories'),
  detail: id => request.get(`/parts/${id}`),
  create: data => request.post('/parts', data),
  update: (id, data) => request.put(`/parts/${id}`, data),
  remove: id => request.delete(`/parts/${id}`)
}
