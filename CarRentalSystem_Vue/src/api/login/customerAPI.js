import api from '../index.js';

// ========== 客戶管理 API（取代原本的 CustomerServlet）==========
export const customerAPI = {
  // 查詢全部（原本的 action=getAll）
  getAll() {
    return api.get('/customers')
  },
  // 根據 ID 查詢（原本的 action=getOne）
  getById(custId) {
    return api.get(`/customers/${custId}`)
  },
  // 模糊查詢（原本的 action=query）
  search(keyword) {
    return api.get('/customers/search', { params: { keyword } })
  },
  // 新增（原本的 action=insert）
  create(customer) {
    return api.post('/customers', customer)
  },
  // 修改（原本的 action=update）
  update(custId, customer) {
    return api.put(`/customers/${custId}`, customer)
  },
  // 刪除（原本的 action=delete）
  delete(custId) {
    return api.delete(`/customers/${custId}`)
  }
}
