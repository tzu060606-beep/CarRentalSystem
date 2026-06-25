// import request from '../index'; // 引用上面設定好的實例
import api from "../index.js";

export const vehicleAPI = {
    // 查全部
    getAll() {
        // 因為 baseURL 已經有 /api，這裡只要接後面的路徑
        return api.get('/vehicle');
    },
    // 查單筆
    get(vehicleId) {
        return api.get(`/vehicle/${vehicleId}`);
    },
    // 新增
    insert(data) {
        return api.post('/vehicle', data);
    },
    // 修改
    edit(data, vehicleId) {
        return api.put(`/vehicle/${vehicleId}`, data);
    },
    // 刪除
    delete(vehicleId) {
        return api.delete(`/vehicle/${vehicleId}`);
    },
    // 抓取「調度」可用車輛
    availablefordispatch(reqStartTime) {
        return api.get('/vehicle/available/dispatch', {
            params: { reqStartTime: reqStartTime },
        });
    },
    // 轉換車輛狀態
    updateStatus(vehicleId, newStatus) {
        return api.patch(`/vehicle/${vehicleId}/status`, {
            newStatus: newStatus,
        });
    },

    // 查全部transfer order for車輛詳情頁面「排程區」
    getAllTransferOrder() {
        return api.get('/transferOrder');
    },
};