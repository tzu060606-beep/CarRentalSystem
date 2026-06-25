// import request from '../index'; // 引用上面設定好的實例
import api from "../index.js";

export const locationAPI = {
    // 查全部
    getAll() {
        // 因為 baseURL 已經有 /api，這裡只要接後面的路徑
        return api.get('/location');
    },
    // 查單筆
    get(locationId) {
        return api.get(`/location/${locationId}`);
    },
    // 新增
    insert(data) {
        return api.post('/location', data);
    },
    // 修改
    edit(data, locationId) {
        return api.put(`/location/${locationId}`, data);
    },
    // 刪除
    delete(locationId) {
        return api.delete(`/location/${locationId}`);
    },
};