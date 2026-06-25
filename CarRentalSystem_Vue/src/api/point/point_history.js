// src/api/index.js裡面已經統一設定好 axios 的基底網址和共用設定（例如 timeout、header），然後匯出成 request。
//你只需要 import request from './index'，然後寫相對路徑就好：
import request from '../index';

/* 
  這張表是系統自動寫入的點數異動紀錄流水帳，不應該讓人手動操作：
  新增：由後端在兌換、租車、生日等時機自動寫入，不讓管理員手動新增
  修改：流水帳一旦寫入就是事實紀錄，改了會造成帳目不一致
  刪除：同上，不能刪除財務相關的歷史紀錄
  管理員只能查看，確認系統有正確記錄每次點數變動
  */

// 新增異動紀錄
// insert(pointsHistory) {
//   return request.post('/histories', pointsHistory)
// },

// 修改異動紀錄
// updateById(id, pointsHistory) {
//   return request.put(`/histories/${id}`, pointsHistory)
// },

// 刪除異動紀錄
// deleteById(id) {
//   return request.delete(`/histories/${id}`)
// },

export const pointsHistoryAPI = {
  // 查詢全部異動紀錄
  getAll() {
    return request.get('/histories')
  },
  // 依 id 查詢單筆異動紀錄
  getById(id) {
    return request.get(`/histories/${id}`)
  },
  // 關鍵字搜尋異動紀錄
  getByKeyword(keyword) {
    return request.get('/histories/search', { params: { keyword: keyword } })
  },
  // 查詢某位客戶的所有點數異動紀錄
  getByCustId(custId) {
    return request.get(`/histories/customer/${custId}`)
  }
}