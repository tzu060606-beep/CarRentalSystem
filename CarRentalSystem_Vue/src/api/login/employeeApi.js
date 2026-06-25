import api from '../index.js'; // 引入已經設定好 Token 的 axios 實例

export const employeeAPI = {
    // 查全部
    getAll() {
        return api.get('/employees');
    },
    // 查單筆
    getById(empId) {
        return api.get(`/employees/${empId}`);
    },
    // 關鍵字搜尋
    search(keyword) {
        return api.get('/employees/search', { params: { keyword } });
    },
    // 新增
    create(data) {
        return api.post('/employees', data);
    },
    // 修改
    update(empId, data) {
        return api.put(`/employees/${empId}`, data);
    },
    // 刪除 (軟刪除)
    delete(empId) {
        return api.delete(`/employees/${empId}`);
    }
};
