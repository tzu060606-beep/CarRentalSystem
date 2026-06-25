// viewingappointment.js
export default [
  {
    path: "/viewingappointmentgetall",
    name: "viewingappointment",
    component: () =>
      import("@/views/admin/usedcarviews/ViewingappointmentView.vue"), // 建議用懶加載
  },
];
