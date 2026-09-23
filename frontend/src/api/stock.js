import request from './request'

export default {
  list: params => request.get('/stocks', { params })
}
