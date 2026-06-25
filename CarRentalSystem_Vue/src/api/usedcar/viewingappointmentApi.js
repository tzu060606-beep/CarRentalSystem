// src/api/viewingappointmentApi.js
import request from "../index"; // 引用上面設定好的實例

export default {
  // 取得所有車輛
  getAll() {
    return request.get("viewingappointment/getall");
  },
  //取得單筆車輛資料
  getById(id) {
    return request.get(`/viewingappointment/${id}`);
  },
  // 刪除車輛
  delete(id) {
    console.log("現在使用的 request 是：", request);
    return request.delete(`/viewingappointment/${id}`);
  },

  //修改資料
  update(id, data) {
    return request.put(`/viewingappointment/${id}`, data);
  },

  //新增資料
  insert(data) {
    return request.post("/viewingappointment/insert", data);
  },
  findByCustId(custId) {
    return request.get(`/viewingappointment/customer/${custId}`);
  },
  // 你之後可以在這裡增加 update, insert 等方法
};
