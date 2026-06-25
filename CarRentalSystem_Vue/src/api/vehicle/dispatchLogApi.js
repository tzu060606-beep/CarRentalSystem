// import request from '../index'; // 引用上面設定好的實例
import api from "../index.js";
export const dispatchAPI = {
    // 查全部
    getAll() {
        // 因為 baseURL 已經有 /api，這裡只要接後面的路徑
        return api.get('/dispatchlog');
    },
    // 查單筆
    get(dispatchId) {
        return api.get(`/dispatchlog/${dispatchId}`);
    },
    // 新增
    insert(data) {
        return api.post('/dispatchlog', data);
    },
    // 修改
    edit(data, dispatchId) {
        return api.put(`/dispatchlog/${dispatchId}`, data);
    },
    // 刪除
    delete(dispatchId) {
        return api.delete(`/dispatchlog/${dispatchId}`);
    },
    // 開始調度
    start(dispatchId) {
        return api.post(`/dispatchlog/${dispatchId}/start`)
    },
    // 完成調度
    finish(dispatchId, endMileage) {
        // 這邊用 params 將 endMileage 轉成 Query String 送出
        return api.post(`/dispatchlog/${dispatchId}/finish`, null, { params: { endMileage } })
    },
    // 取消調度
    cancel(dispatchId) {
        return api.post(`/dispatchlog/${dispatchId}/cancel`)
    },
    // 依vehicleId查調度單
    searchByVehicle(vehicleId) {
        return api.get(`/dispatchlog/vehicle/${vehicleId}`)
    },
    // 查狀態筆數
    getCounts(dispatchId) {
        return api.get(`/dispatchlog/counts`)
    },
    // 分頁查詢(可by狀態)
    // getAll(page = 1, size = 10, status = 'ALL') {
    //     return api.get('/dispatchlog/page', {
    //         params: {
    //             page: page - 1, // Spring Boot頁碼從0起算，所以前端回傳參數給後端時要-1
    //             size: size,
    //             status: status === "ALL" ? '' : status,
    //         }
    //     });
    // },
};