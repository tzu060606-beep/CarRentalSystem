// request 已經知道 base URL 是 http://localhost:8081/api
// src/api/index.js裡面已經統一設定好 axios 的基底網址和共用設定（例如 timeout、header），然後匯出成 request。
//你只需要 import request from './index'，然後寫相對路徑就好：
import request from '../index';

export const productAPI = {
  // 新增商品
  insert(product) {
    return request.post('/products', product)
  },
  // 修改商品
  updateById(id, product) {
    return request.put(`/products/${id}`, product)
  },
  // 刪除商品
  deleteById(id) {
    return request.delete(`/products/${id}`)
  },
  // 查詢全部商品
  getAll() {
    return request.get('/products')
  },
  // 依 id 查詢單筆商品
  getById(id) {
    return request.get(`/products/${id}`)
  },
  // 關鍵字搜尋商品
  getByKeyword(keyword) {
    return request.get('/products/search', { params: { keyword: keyword } })
  },
  //篩選商品（狀態、類別、排序，全部選填）
  getByFilters(status, category, sort) {
    return request.get('/products/filter', {
      params: { status: status, category: category, sort: sort }
    })
  }
}
//【狀態類別排序的查詢合併到上方方法】
// // 依狀態篩選商品
// export const getProductByIsActive = (isActive) => {
//   return request.get(`/products/active`, { params: { isActive: isActive } })
// }
