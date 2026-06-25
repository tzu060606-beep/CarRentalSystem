//   登入相關的路由

export default [
  {
    path: "/customer/login",
    name: "customerLogin",
    component: () => import("@/views/customer/login/CustomerLoginView.vue"),
  },
  {
    path: "/customer/register",
    name: "customerRegister",
    component: () => import("@/views/customer/login/CustomerRegisterView.vue"),
  },
  {
    path: "/customer/forgot-password",
    name: "customerForgotPassword",
    component: () => import("@/views/customer/login/ForgotPasswordView.vue"),
  },
  {
    path: "/customer/reset-password",
    name: "customerResetPassword",
    component: () => import("@/views/customer/login/ResetPasswordView.vue"),
  },
  //會員中心 - 個人資料
  {
    //用MemberLayout包住
    path: "/customer/member",
    component: () => import("@/layouts/MemberLayout.vue"),
    meta: { requiresAuth: true, roles: ["CUSTOMER"] },
    children: [
      //會員中心-個人資料路徑
      {
        path: "profile",
        name: "CustomerProfile",
        component: () =>
          import("@/views/customer/login/CustomerProfileView.vue"),
      },
      //會員中心-點數專區路徑
      {
        path: 'pointcenter',
        name: 'MemberPoint',
        component: () => import('@/views/customer/point/PointDashboardView.vue'),
      },
      {
        path: "voucher",
        name: "MemberVoucher",
        component: () => import("@/views/customer/point/MyVoucherView.vue"),
      },
      {
        path: 'history',
        name: 'MemberHistory',
        component: () => import('@/views/customer/point/PointHistoryView.vue'),
      },
      {
        path: 'products',
        name: 'MemberProducts',
        component: () => import('@/views/customer/point/ProductListView.vue'),
      },
      {
        path: "order",
        name: "customerRentalOrders",
        component: () => import("@/views/customer/rentalorder/CustomerRentalOrdersView.vue"),
      },
      {
        path: "order/:orderId",
        name: "customerRentalOrderDetail",
        component: () => import("@/views/customer/rentalorder/CustomerRentalOrderDetailView.vue"),
      },
      // 會員中心 - 專車接送訂單(芊)
      {
        path: "transfer",
        name: "CustomerTransferHistory",
        component: () => import("@/views/customer/transfer/TransferOrderHistoryView.vue"),
      },
    ],
  },

  // // 會員中心 - 點數紀錄
  // {
  //   path: "/customer/point",
  //   name: "CustomerPoint",
  //   // 待阿華建立vue
  //   component: () => import("@/views/customer/point/ProductListView.vue"),
  //   meta: { requiresAuth: true, roles: ["CUSTOMER"] },
  // },

  // // 會員中心 - 二手車買賣紀錄
  // {
  //   path: "/customer/usedCar",
  //   name: " ",
  //   // 待偉峻建立vue
  //   component: () => import("@/views/customer/point/XXX.vue"),
  //   meta: { requiresAuth: true, roles: ["CUSTOMER"] },
  // },
  // 會員專區：用 MemberLayout 包住
];
