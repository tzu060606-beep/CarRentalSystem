import request from '../index';

//URL、HTTP 方法、參數格式包在這支檔案中，元件只需要呼叫函式名稱
//好處是如果 URL 改了，只需要改 point_voucher.js 一個地方，不需要去找每個用到的元件。
// return 的意思是把這個請求的結果（Promise）傳回給呼叫方，讓呼叫方可以用 await 等待結果

export const voucherAPI = {
  // 使用兌換券，核銷
  use(voucherCode) {
    return request.put(`/vouchers/${voucherCode}/use`)
  },
  // 查詢全部兌換券
  getAll() {
    return request.get('/vouchers')
  },
  // 查單筆（by voucherCode）
  getByCode(voucherCode) {
    return request.get(`/vouchers/${voucherCode}`)
  },
  // 查某客戶的票券
  getByCustId(custId) {
    return request.get(`/vouchers/customer/${custId}`)
  },
  // 查某訂單的票券
  getByRedemptionId(redemptionId) {
    return request.get(`/vouchers/redemption/${redemptionId}`)
  }
  //篩選兌換券（狀態、類別、排序，全部選填）
  // getByFilters(status, category, sort) {
  //   return request.get('/products/filter', {
  //     params: { status: status, category: category, sort: sort }
  //   })
  // }
}