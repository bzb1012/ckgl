import request from './request'

export default {
  list: () => request.get('/warehouses'),
  create: data => request.post('/warehouses', data),
  update: (id, data) => request.put(`/warehouses/${id}`, data),
  remove: id => request.delete(`/warehouses/${id}`),
  locations: id => request.get(`/warehouses/${id}/locations`),
  addLocation: (id, data) => request.post(`/warehouses/${id}/locations`, data),
  updateLocation: (id, data) => request.put(`/locations/${id}`, data),
  removeLocation: id => request.delete(`/locations/${id}`)
}
