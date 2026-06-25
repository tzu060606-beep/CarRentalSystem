// usedCar.js
export default [
  {
    path: "/usedcargetall",
    name: "usedcars",
    component: () => import("@/views/admin/usedcarviews/UsedCarView.vue"), // 建議用懶加載
  },
];
