// import request from '../index'; // 引用上面設定好的實例
import api from "../index.js";

export const carModelAPI = {
    // 查全部
    getAll() {
        // 因為 baseURL 已經有 /api，這裡只要接後面的路徑
        return api.get('/carmodel');
    },
    // 查單筆
    get(modelId) {
        return api.get(`/carmodel/${modelId}`);
    },
    // 新增
    insert(data) {
        return api.post('/carmodel', data);
    },
    // 修改
    edit(data, modelId) {
        return api.put(`/carmodel/${modelId}`, data);
    },
    // 刪除
    delete(modelId) {
        return api.delete(`/carmodel/${modelId}`);
    },
    // 上傳圖片
    upload(base64String) {
        return api.post('/files/base64', { base64: base64String });
    },
};