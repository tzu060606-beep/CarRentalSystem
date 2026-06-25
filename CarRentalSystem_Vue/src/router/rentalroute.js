// src/router/orderRoutes.js

export default [
  {
    path: "/orders",
    name: "RentalDashboard",
    component: () => import('@/views/admin/rentalorder/RentalDashboardView.vue'),
  },
  {
    path: "/orders/list",
    name: "OrderList",
    component: () => import('@/views/admin/rentalorder/OrderListView.vue'), 
  },
  {
    path: "/orders/new",
    name: "OrderCreate",
    component: () => import('@/views/admin/rentalorder/OrderFormView.vue'),
  },
  {
    path: "/orders/edit/:id",
    name: "OrderEdit",
    component: () => import('@/views/admin/rentalorder/OrderFormView.vue'),
  },
  {
    path: "/plans",
    name: "PlanList",
    component: () => import('@/views/admin/rentalorder/PlanListView.vue'),
  },

  {
    path: '/orders/process/:id',
    name: 'RentalProcess',
    component: () => import('@/views/admin/rentalorder/RentalProcess.vue')
  },
  {
    path: '/orders/detail/:id', 
    name: 'OrderDetail', 
    component: () => import('@/views/admin/rentalorder/OrderDetailView.vue')
  },

];
