export default [
  {
    path: '/rental-intro',
    name: 'rentalIntro',
    component: () => import('@/views/customer/rentalorder/DailyLongTermIntroView.vue')
  },
  {
    path: '/orders/custbooking',
    component: () => import('@/views/customer/rentalorder/OrderSelectView.vue')
  },
  {
    path: '/orders/custconfirm',
    name: 'custconfirm',
    component: () => import('@/views/customer/rentalorder/OrderConfirmView.vue'),
    meta: { requiresAuth: true, roles: ['CUSTOMER'] }
  },
  {
    path: '/orders/custsuccess',
    name: 'custsuccess',
    component: () => import('@/views/customer/rentalorder/OrderSuccessView.vue'),
    meta: { requiresAuth: true, roles: ['CUSTOMER'] }
  },

];
