import axios from "axios";

// 建立 axios 實例，統一管理 API 請求
const api = axios.create({
  baseURL: "/api",
  timeout: 10000,
  withCredentials: true, // 攜帶 Cookie（Session 驗證需要）
});

// === 0507合併後，新增這段：Axios 請求攔截器 ===
api.interceptors.request.use(
  (config) => {
    // 根據當前的系統環境，決定要拿哪把鑰匙
    const systemContext = localStorage.getItem("system_context");
    const tokenKey =
      systemContext === "employee" ? "employee_token" : "customer_token";
    const token = localStorage.getItem(tokenKey);

    // 如果 token 存在，就把它塞進 Request Header 裡
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);
// ===================================

// ========== 認證相關 API（取代原本的 LoginServlet）==========
export const authAPI = {
  // 員工登入
  employeeLogin(account, password) {
    return api.post("/auth/employee/login", { account, password });
  },
  //員工登出
  employeelogout() {
    return api.post("/auth/employee/logout");
  },

  // 客戶登入
  customerLogin(account, password) {
    return api.post("/auth/customer/login", { account, password });
  },

  // 客戶 Google 登入
  customerGoogleLogin(credential) {
    return api.post("/auth/customer/google", { credential });
  },

  // 客戶註冊
  customerRegister(formData) {
    return api.post("/auth/customer/register", formData);
  },

  //客戶登出
  customerlogout() {
    return api.post("/auth/customer/logout");
  },
  //檢查登入狀態
  checkLogin() {
    return api.get("/auth/check");
  },

  // 忘記密碼：寄送重設信件
  forgotPassword(email) {
    return api.post("/auth/customer/forgot-password", { email });
  },

  // 重設密碼：驗證 Token 並更新密碼
  resetPassword(token, newPassword) {
    return api.post("/auth/customer/reset-password", { token, newPassword });
  },
};

export default api;
