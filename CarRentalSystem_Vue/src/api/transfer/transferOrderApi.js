import axios from 'axios'

const BASE_URL = 'http://localhost:8081/api/transfer'

/**
 * 取得所有調度訂單
 * @returns {Promise} 所有調度訂單列表
 */
export function getAllTransferOrders() {
  return axios.get(BASE_URL)
}

/**
 * 依 ID 取得單筆調度訂單
 * @param {number|string} id - 調度訂單 ID
 * @returns {Promise} 單筆調度訂單資料
 */
export function getTransferById(id) {
  return axios.get(`${BASE_URL}/${id}`)
}

/**
 * 新增調度訂單
 * @param {Object} data - 調度訂單資料
 * @returns {Promise} 新增結果
 */
export function createTransfer(data) {
  return axios.post(BASE_URL, data)
}

/**
 * 更新調度訂單
 * @param {number|string} id - 調度訂單 ID
 * @param {Object} data - 更新的調度訂單資料
 * @returns {Promise} 更新結果
 */
export function updateTransfer(id, data) {
  return axios.put(`${BASE_URL}/${id}`, data)
}

/**
 * 刪除調度訂單
 * @param {number|string} id - 調度訂單 ID
 * @returns {Promise} 刪除結果
 */
export function deleteTransfer(id) {
  return axios.delete(`${BASE_URL}/${id}`)
}
