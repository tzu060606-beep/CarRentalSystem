import request from "../index";

export default {
  // 取得所有車輛
  getAll() {
    return request.get("/usedCars/getall");
  },
  //取得單筆車輛資料
  getById(id) {
    return request.get(`/usedCars/${id}`);
  },
  // 刪除車輛
  delete(id) {
    return request.delete(`/usedCars/${id}`);
  },
  //修改資料
  update(id, data) {
    return request.put(`/usedCars/${id}`, data);
  },

  //新增資料
  insert(data) {
    return request.post("/usedCars/insert", data);
  },

  //取得單筆車輛詳細資料
  getAllDetails() {
    return request.get(`/usedCars/details`);
  },
  //取得全部車輛詳細資料
  getOneDetail(id) {
    return request.get(`/usedCars/detail/${id}`);
  },
  // 你之後可以在這裡增加 update, insert 等方法
};
