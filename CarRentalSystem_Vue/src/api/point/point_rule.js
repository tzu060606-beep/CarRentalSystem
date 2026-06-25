// src/api/index.js裡面已經統一設定好 axios 的基底網址和共用設定（例如 timeout、header），然後匯出成 request。
//你只需要 import request from './index'，然後寫相對路徑就好：
import request from '../index';

export const pointsRuleAPI = {
  // 新增規則
  insert(pointsRule) {
    return request.post('/rules', pointsRule)
  },
  // 修改規則
  updateById(id, pointsRule) {
    return request.put(`/rules/${id}`, pointsRule)
  },
  // 刪除規則
  // deleteById(id) {
  //   return request.delete(`/rules/${id}`)
  // },
  // 查詢全部規則
  getAll() {
    return request.get('/rules')
  },
  // 依 id 查詢單筆規則
  getById(id) {
    return request.get(`/rules/${id}`)
  },
  // 關鍵字搜尋規則
  getByKeyword(keyword) {
    return request.get('/rules/search', { params: { keyword: keyword } })
  }
}