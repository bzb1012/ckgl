import request from './request'

export default {
  list: params => request.get('/plans', { params }),
  lines: () => request.get('/plans/lines'),
  alerts: () => request.get('/plans/alerts'),
  todayParts: date => request.get('/plans/today-parts', { params: { date } }),
  create: data => request.post('/plans', data),
  update: (id, data) => request.put(`/plans/${id}`, data),
  updateCompleted: (id, completed) => request.put(`/plans/${id}/completed`, { completed }),
  remove: id => request.delete(`/plans/${id}`)
}
