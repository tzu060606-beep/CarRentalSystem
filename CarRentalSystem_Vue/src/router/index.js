import { createRouter, createWebHistory } from "vue-router";
import { authAPI } from "../api";
import usedCarRoutes from "./modules/usedCar";
import salesRecordRoutes from "./modules/salesrecord";
import viewingAppointmentRoutes from "./modules/viewingAppointment";
import usedCarShopRoutes from "./modules/usedCarShop";
import UsedCarCompare from "@/views/customer/usedcarviews/UsedCarCompare.vue";
import ViewTable from "@/views/customer/usedcarviews/ViewTable.vue";
import salesview from "../views/admin/usedcarviews/Salesviewhome.vue";
import UsedFavoriteList from "@/views/customer/usedcarviews/UsedFavoriteList.vue";
import ViewforCustomer from "@/views/customer/usedcarviews/ViewForCustomer.vue";
import ProductView from "@/views/admin/point/ProductView.vue";
import TransferOrderAddView from "../views/admin/transfer/TransferOrderAddView.vue";
import TransferOrderEditView from "../views/admin/transfer/TransferOrderEditView.vue";
import TransferOrderListView from "../views/admin/transfer/TransferOrderListView.vue";
import TransferOrderView from "../views/admin/transfer/TransferOrderView.vue";
import TransferRateAddView from "../views/admin/transfer/TransferRateAddView.vue";
import TransferRateEditView from "../views/admin/transfer/TransferRateEditView.vue";
import TransferRateListView from "../views/admin/transfer/TransferRateListView.vue";
import CarModelView from "@/views/admin/vehicle/CarModelView.vue";
import vehicleRoutes from "./vehicle.js";
import vehiclecustomroute from "./vehiclecustomroute";
import pointRoutes from "./point.js";
//前台Layout
import FrontLayout from "@/layouts/FrontLayout.vue";
import LandingView from "@/views/customer/LandingView.vue";
import rentalRoutes from "./rentalroute";
import rentalcustRoute from "./rentalcustroute";
import pointFrontRoutes from './pointFront.js';
import authRoutes from "./modules/auth";

// 路由設定（取代原本 JSP 頁面之間的跳轉邏輯）
const routes = [
  {
    path: "/employee/login",
    name: "EmployeeLoginView",
    component: () => import("../views/admin/login/EmployeeLoginView.vue"),
  },
  {
    path: "/employee",
    component: () => import("../layouts/MainLayout.vue"),
    meta: { requiresAuth: true, roles: ["ADMIN", "USER"] }, // 後台需要登入，且限定特定角色
    children: [
      {
        path: "",
        name: "Dashboard",
        component: () => import("../views/DashboardView.vue"),
      },
      {
        path: "/customers",
        name: "CustomerList",
        component: () => import("../views/admin/login/CustomerListView.vue"),
      },
      {
        path: "/customers/add",
        name: "CustomerAdd",
        component: () => import("../views/admin/login/CustomerAddView.vue"),
      },
      {
        path: "/customers/:id/edit",
        name: "CustomerEdit",
        component: () => import("../views/admin/login/CustomerEditView.vue"),
      },
      {
        path: "/customers/search",
        name: "CustomerSearch",
        component: () => import("../views/admin/login/CustomerSearchView.vue"),
      },
      {
        path: "/employeesEdit",
        name: "EmployeeEdit",
        component: () => import("../views/admin/login/EmployeeEditView.vue"),
      },
      {
        path: "/usedcar",
        name: "usedcar",
        component: () =>
          import("../views/admin/usedcarviews/Salesviewhome.vue"),
      },
      ...usedCarRoutes,
      ...salesRecordRoutes,
      ...viewingAppointmentRoutes,
      ...usedCarShopRoutes,
      {
        path: "/usedcar/viewtable",
        name: "ViewTable",
        component: () => import("../views/customer/usedcarviews/ViewTable.vue"),
      },
      {
        path: "/usedcar/appointment",
        name: "ViewForCustomer",
        component: () =>
          import("../views/customer/usedcarviews/ViewForCustomer.vue"),
        // meta: { requiresAuth: true }, // 標記此頁面需要登入
      },
      {
        path: "/usedcar/detail/:id", // :id 是動態變數
        name: "usedcardetail",
        component: () =>
          import("../views/customer/usedcarviews/UsedCarDetail.vue"),
        // meta: { requiresAuth: true }, // 標記此頁面需要登入
      },
      {
        path: "/usedcar/success",
        name: "AppointmentSuccess",
        component: () =>
          import("../views/customer/usedcarviews/AppointmentSuccess.vue"),
        // meta: { requiresAuth: true }, // 標記此頁面需要登入
      },
      {
        path: "/usedcar/map",
        name: "usedcarmap",
        component: () => import("../views/customer/usedcarviews/GoogleMap.vue"),
      },
      {
        path: "/transferOrder",
        name: "transferOrderList",
        component: TransferOrderListView,
      },
      {
        path: "/transferOrder/add",
        name: "transferOrderAdd",
        component: TransferOrderAddView,
      },
      {
        path: "/transferOrder/edit/:id",
        name: "transferOrderEdit",
        component: TransferOrderEditView,
      },
      {
        path: "/transferRate",
        name: "transferRateList",
        component: TransferRateListView,
      },
      {
        path: "/transferRate/add",
        name: "transferRateAdd",
        component: TransferRateAddView,
      },
      {
        path: "/transferRate/edit/:id",
        name: "transferRateEdit",
        component: TransferRateEditView,
      },
      ...pointRoutes,
      ...rentalRoutes,
      ...vehicleRoutes,
    ],
  },

  // 前台首頁路由
  {
    path: "/",
    component: FrontLayout,
    children: [
      {
        path: "",
        name: "Landing",
        component: LandingView,
      },
      ...authRoutes,
      ...rentalcustRoute,

      // 專車接送介紹
      {
        path: "transfer",
        name: "CustomerTransferIntro",
        component: () => import("@/views/customer/transfer/TransferLandingPageView.vue"),
      },
      // 專車接送預約重定向
      {
        path: "transfer/booking",
        redirect: "/transfer/booking/airport"
      },
      // 機場接送
      {
        path: "transfer/booking/airport",
        name: "CustomerTransferBookingAirport",
        component: () => import("@/views/customer/transfer/TransferBookingView.vue"),
        meta: { requiresAuth: true, roles: ["CUSTOMER"] },
        props: { defaultPage: 'terms' }
      },
      // 其他接送
      {
        path: "transfer/booking/other",
        name: "CustomerTransferBookingOther",
        component: () => import("@/views/customer/transfer/TransferBookingView.vue"),
        meta: { requiresAuth: true, roles: ["CUSTOMER"] },
        props: { defaultPage: 'charterRequest' }
      },
      ...vehiclecustomroute,
      ...pointFrontRoutes,
      // TODO: 之後各模組前台頁面加在這裡
    ],
  },
  {
    //測試用NotFound畫面
    path: "/:pathMatch(.*)*",
    name: "PointNotFound",
    component: () => import("@/components/common/NotFoundView.vue"),
  },
  {
    //NotFound畫面
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: () => import("@/components/common/NotFoundView.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
});

// 路由守衛
router.beforeEach(async (to, from) => {
  // 動態判斷並設定目前的環境 (前台或後台) 給 API 攔截器決定送出哪把鑰匙
  const isEmployeeContext =
    to.matched[0] &&
    (to.matched[0].path === "/employee" ||
      to.matched[0].path === "/employee/login");
  localStorage.setItem(
    "system_context",
    isEmployeeContext ? "employee" : "customer",
  );

  // 1. 檢查目標路由是否需要驗證 (只要該路由或其父路由有設定 requiresAuth: true 就會成立)
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    // 先檢查對應環境的 Token 是否存在，不存在就直接導向登入頁，不用浪費一次 API 呼叫
    const requiredToken = isEmployeeContext ? "employee_token" : "customer_token";
    if (!localStorage.getItem(requiredToken)) {
      return isEmployeeContext ? "/employee/login" : "/customer/login";
    }

    try {
      // 呼叫後端 API 檢查登入狀態
      const res = await authAPI.checkLogin();

      if (res.data.loggedIn) {
        // 2. 取得當前登入者的角色 (例如 'admin', 'emp', 或 'CUSTOMER')
        const userRole = res.data.role;

        // ───【 新增：客戶補全資料攔截邏輯 】───
        if (userRole === "CUSTOMER" && res.data.customer) {
          const cust = res.data.customer;
          // 判斷手機號碼是否為空 (這代表是剛用 Google 註冊、還沒填資料的新人)
          const isProfileIncomplete =
            !cust.custPhone || cust.custPhone.trim() === "";

          // 如果資料不完整，且使用者現在「不是」要去填寫資料頁面，就強制導向去填資料
          if (isProfileIncomplete && to.path !== "/customer/member/profile") {
            alert("歡迎使用 Google 登入！請先補齊您的基本資料以利後續租車。");
            return "/customer/member/profile"; // 強制導向填寫資料頁
          }
        }
        // ──────────────────────────────────────

        // 3. 找出這個頁面允許進入的角色清單 (從 meta.roles 取得)
        // 因為 meta 可能設定在父路由或子路由，我們透過 matched 尋找
        const requiredRoles =
          to.meta.roles ||
          to.matched.find((record) => record.meta.roles)?.meta.roles;

        // 如果這個頁面有設定角色限制
        if (requiredRoles) {
          if (requiredRoles.includes(userRole)) {
            return true; // 權限符合，放行
          } else {
            // 已登入但權限不足
            alert(
              `您沒有權限訪問此頁面！(您的角色：${userRole}，需要：${requiredRoles.join(", ")})`,
            );
            // 權限不足時，依照目的地決定跳回前台登入或後台登入，絕不交叉跳轉
            return isEmployeeContext ? "/employee/login" : "/customer/login";
          }
        }

        // 如果只有 requiresAuth: true 但沒設定 roles，代表只要有登入都可以進去 (例如前台)
        return true;
      } else {
        // 沒登入的情況，依照目的地跳轉到對應的登入頁面
        return isEmployeeContext ? "/employee/login" : "/customer/login";
      }
    } catch (error) {
      // API 檢查失敗 (Token 失效等)，視同未登入，依照目的地跳轉到對應的登入頁面
      return isEmployeeContext ? "/employee/login" : "/customer/login";
    }
  } else {
    // 不需要登入的頁面，直接放行
    return true;
  }
});

export default router;
