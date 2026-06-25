import request from "../index";

export default {
  // 取得所有車輛
  getAll() {
    return request.get("/salesrecord/getall");
  },
  //取得單筆車輛資料
  getById(id) {
    return request.get(`/salesrecord/${id}`);
  },
  // 刪除車輛
  delete(id) {
    return request.delete(`/salesrecord/${id}`);
  },
  //修改資料
  update(id, data) {
    return request.put(`/salesrecord/${id}`, data);
  },

  //新增資料
  insert(data) {
    return request.post("/salesrecord/insert", data);
  },
  // 你之後可以在這裡增加 update, insert 等方法
};
