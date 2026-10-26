import request from './request'

export default {
  ledger: params => request.get('/production/ledger', { params }),
  addIn: data => request.post('/production/in', data),
  setIn: data => request.put('/production/in', data),
  addOut: data => request.post('/production/out', data),
  dailySummary: date => request.get('/production/daily-summary', { params: { date } }),
  settle: month => request.post('/production/settle', { month })
}
