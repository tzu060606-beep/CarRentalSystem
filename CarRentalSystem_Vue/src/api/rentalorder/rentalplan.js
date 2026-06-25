// 引入已經設定好基礎設定的那個 axios 實例
//當使用 import 名字 from '路徑' 且該路徑是 export default 時，你可以幫它取任何你喜歡的名字。
import api from '@/api/index'; 


// 直接使用該實例，並把完整的網址前綴寫在方法裡
const BASE_URL = '/admin/rentalplans';

export const rentalPlansAPI = {
  // 注意：這裡的路徑會自動拼接到 index.api 設定的 baseURL (/api) 之後
  // 最終發出：/api/admin/rentalplans/all
  getAll: () => api.get(`${BASE_URL}/all`),

  /*寫法介紹

        // 1.這是完全對應的傳統寫法
         getAll: function() {
        // 這裡必須寫 return，因為 request.get 會回傳一個 Promise 物件
        // 如果沒寫 return，Vue 元件呼叫這個 API 時會拿不到資料（變成空手而回）
        return request.get(`${BASE_URL}/all`);
  },

  /*

        // 這是 ES6 以後的物件方法簡寫 (與上面的 function() 等價)
        getById(id) {
            return request.get(`${BASE_URL}/${id}`);
        }
        

  */
  
  getById: (id) => api.get(`${BASE_URL}/${id}`),
  
  create: (data) => api.post(`${BASE_URL}/newplan`, data),
  
  update: (id, data) => api.put(`${BASE_URL}/${id}`, data),
  
  delete: (id) => api.delete(`${BASE_URL}/${id}`)
};