import request from '../index';

export const fileAPI = {
  // 上傳圖片
  upload(formData) {
    return request.post('/files', formData)
  }
}