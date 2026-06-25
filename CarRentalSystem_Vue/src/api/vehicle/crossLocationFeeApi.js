import api from "../index.js";

export const crossLocationFeeAPI = {
    // 查全部
    getAll() {
        return api.get('/crosslocationfee');
    },
    // 查單筆
    get(feeId) {
        return api.get(`/crosslocationfee/${feeId}`);
    },
    // 依起迄據點查費率
    getFeeByRoute(fromId, toId) {
        return api.get(`/crosslocationfee/${fromId}/${toId}`);
    },
    // 新增
    insert(data) {
        return api.post('/crosslocationfee', data);
    },
    // 修改
    edit(data, feeId) {
        return api.put(`/crosslocationfee/${feeId}`, data);
    },
    // 刪除
    delete(feeId) {
        return api.delete(`/crosslocationfee/${feeId}`);
    },
};
