import api from '@/api/index';

const CUSTOMER_RENTAL_ORDER_BASE_URL = '/customer/rentalorders';

export const customerRentalOrderAPI = {
  getMyOrders() {
    return api.get(CUSTOMER_RENTAL_ORDER_BASE_URL);
  },

  getMyOrderDetail(orderId) {
    return api.get(`${CUSTOMER_RENTAL_ORDER_BASE_URL}/${orderId}`);
  }
};
