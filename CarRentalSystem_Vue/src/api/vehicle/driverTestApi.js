// import request from '../index'; // 引用上面設定好的實例
import api from "../index.js";

export const driverAPI = {
    // 查全部
    getAll() {
        // 因為 baseURL 已經有 /api，這裡只要接後面的路徑
        return api.get('/dispatchlog/drivers');
    },
    // 查單筆
    get(driverId) {
        return api.get(`/dispatchlog/drivers/${driverId}`);
    },
    // // 新增
    // insert(data) {
    //     return api.post('/drivers', data);
    // },
    // // 修改
    // edit(data, driverId) {
    //     return api.put(`/drivers/${driverId}`, data);
    // },
};

export const employeeAPI = {
    // 查全部
    getAll() {
        // 因為 baseURL 已經有 /api，這裡只要接後面的路徑
        return api.get('/dispatchlog/employees');
    },
    // 查單筆
    get(empId) {
        return api.get(`/dispatchlog/employees/${empId}`);
    },
    // // 新增
    // insert(data) {
    //     return api.post('/employees', data);
    // },
    // // 修改
    // edit(data, empId) {
    //     return api.put(`/employees/${empId}`, data);
    // },
};