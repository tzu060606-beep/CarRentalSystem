// salesrecord.js
export default [
  {
    path: "/salesrecordgetall",
    name: "salesrecord",
    component: () => import("@/views/admin/usedcarviews/SalesrecordView.vue"), // 建議用懶加載
  },
];
