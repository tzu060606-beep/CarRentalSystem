export default [
  {
    // 商城首頁（或列表頁）
    path: "/usedcarshop",
    name: "usedcarshop",
    component: () => import("@/views/customer/usedcarviews/UsedCarShop.vue"),
  },
  {
    // 車輛詳細頁，利用 :id 來抓取特定車輛
    path: "/usedcarshop/detail/:id",
    name: "usedcardetail",
    component: () => import("@/views/customer/usedcarviews/UsedCarDetail.vue"),
    props: true, // 這樣可以直接把 id 當作變數傳入元件
  },
  {
    path: "/usedcarshop/favorites",
    name: "usedcarfavorites",
    component: () =>
      import("@/views/customer/usedcarviews/UsedFavoriteList.vue"),
    // meta: { requiresAuth: true }, // 標記此頁面需要登入
  },
  {
    path: "/usedcarshop/compare",
    name: "usedcarcompare",
    component: () => import("@/views/customer/usedcarviews/UsedCarCompare.vue"),
    // meta: { requiresAuth: true }, // 標記此頁面需要登入
  },
];
