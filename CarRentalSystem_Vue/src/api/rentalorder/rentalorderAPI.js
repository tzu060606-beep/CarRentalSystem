import api from '@/api/index';

const BASE_URL = '/admin/rentalorders';

export const rentalOrderAPI = {
  getAll: () => api.get(`${BASE_URL}/all`),
  getById: (id) => api.get(`${BASE_URL}/query/${id}`),
  create: (data) => api.post(`${BASE_URL}/new`, data),
  update: (id, data) => api.put(`${BASE_URL}/${id}`, data),
  delete: (id) => api.delete(`${BASE_URL}/${id}`),
  search: (params) => api.post(`${BASE_URL}/search`, params),
  pickup: (id, data) => api.put(`${BASE_URL}/${id}/pickup`, data),
  returnOrder: (id, data) => api.put(`${BASE_URL}/${id}/return`, data),
  close: (id, data) => api.put(`${BASE_URL}/${id}/close`, data),
  cancel: (id) => api.put(`${BASE_URL}/${id}/cancel`),
  markRefunded: (id) => api.put(`${BASE_URL}/${id}/complete-refund`)
};
