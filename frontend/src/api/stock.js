import request from './request'

export default {
  list: params => request.get('/stocks', { params }),
  // 盘点：按货位核对物料
  takeItems: locationId => request.get(`/stock-take/locations/${locationId}/items`),
  takeSetQuantity: (locationId, partId, quantity) => request.put(`/stock-take/locations/${locationId}/items/${partId}`, { quantity }),
  takeClear: locationId => request.delete(`/stock-take/locations/${locationId}/items`),
  takeAddPart: (locationId, partId, quantity) => request.post(`/stock-take/locations/${locationId}/items`, { partId, quantity }),
  takeRemovePart: (locationId, partId) => request.delete(`/stock-take/locations/${locationId}/items/${partId}`)
}
