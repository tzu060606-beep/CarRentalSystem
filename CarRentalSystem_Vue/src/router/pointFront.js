export default [
  // 商品列表獨立在外面（不在會員專區 sidebar 裡）
  {
    path: "/point/productlist",
    name: "ProductList",
    component: () => import("@/views/customer/point/ProductListView.vue"),
    meta: { requiresAuth: true, roles: ["CUSTOMER"] },
  },
  {
    path: "/point/myvoucher",
    name: "MyVoucher",
    component: () => import("@/views/customer/point/MyVoucherView.vue"),
    //  meta: { requiresAuth: true }//路由守衛自動攔截，待F1完成登入api後解開
    meta: { requiresAuth: true, roles: ["CUSTOMER"] },
  },
  {
    // 新增這條
    path: '/point/products/:id',
    name: 'ProductDetail',
    component:  () => import('@/views/customer/point/ProductDetailView.vue'),
  },
    {
    path: "/customer/member/products",
    name: "MemberProductList",
    component: () => import("@/views/customer/point/ProductListView.vue"),
  },
];
