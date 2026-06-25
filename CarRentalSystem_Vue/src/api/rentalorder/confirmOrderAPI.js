import api from '@/api/index';

const ORDER_BASE_URL = '/order';
const RENTAL_ORDER_BASE_URL = '/admin/rentalorders';
const RENTAL_PAYMENT_BASE_URL = '/rental/payment/ecpay';

export const confirmOrderAPI = {
  getOrderPreview(config) {
    return api.get(`${ORDER_BASE_URL}/preview`, config);
  },

  createRentalOrder(payload) {
    return api.post(`${RENTAL_ORDER_BASE_URL}/new`, payload);
  },

  createEcpayCheckout(payload, config = {}) {
    return api.post(`${RENTAL_PAYMENT_BASE_URL}/checkout`, payload, config);
  }
};
