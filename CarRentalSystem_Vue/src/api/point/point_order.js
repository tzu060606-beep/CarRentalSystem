import request from '../index';

export const redemptionOrderAPI = {
  // 查詢全部兌換訂單
  getAll() {
    return request.get('/orders')
  },
  // 依 id 查詢單筆兌換訂單
  getById(id) {
    return request.get(`/orders/${id}`)
  },
  // 關鍵字搜尋兌換訂單
  getByKeyword(keyword) {
    return request.get('/orders/search', { params: { keyword: keyword } })
  },
  // 依訂單狀態篩選
  getByStatus(orderStatus) {
    return request.get('/orders/status', { params: { orderstatus: orderStatus } })
  },
  // 更新訂單狀態（只能改狀態）
  updateStatus(id, orderStatus) {
    return request.put(`/orders/${id}`, { orderStatus: orderStatus })
  },
  // 新增訂單
  insert(data) {
    return request.post('/orders', data)
  },
  //查全部訂單附帶票券資訊
  getAllWithVouchers() {
    return request.get('/orders/admin')
  }
}