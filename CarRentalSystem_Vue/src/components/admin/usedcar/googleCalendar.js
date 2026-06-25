import axios from "axios";
import { ref } from "vue";

/**
 * 封裝 Google 行事曆功能的 Hook
 */
export function useGoogleCalendar() {
  const isSyncing = ref(false);

  // 建立 Axios 實例，確保設定一致
  const calendarApi = axios.create({
    baseURL: "http://localhost:8081/api",
    withCredentials: true, // 必須開啟，否則 Session (JSESSIONID) 無法帶入
    timeout: 10000,
  });

  // 請求攔截器：處理 JWT Token 注入
  calendarApi.interceptors.request.use(
    (config) => {
      const systemContext = localStorage.getItem("system_context");
      const tokenKey =
        systemContext === "employee" ? "employee_token" : "customer_token";
      const token = localStorage.getItem(tokenKey);

      // 防呆：有 Token 才加 Header，避免傳入 "Bearer null"
      if (token && token !== "null" && token !== "undefined") {
        config.headers["Authorization"] = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error),
  );

  // 核心功能：同步事件
  const syncToGoogle = async (eventData) => {
    isSyncing.value = true;
    try {
      await calendarApi.post("/calendar/create", eventData);
      return { success: true };
    } catch (error) {
      // 處理 401 Unauthorized
      if (error.response?.status === 401) {
        console.warn("偵測到未授權，準備導向 Google 登入...");
        const confirmAuth = confirm(
          "尚未連動 Google 行事曆，是否立即前往授權？",
        );
        if (confirmAuth) {
          window.location.href =
            "http://localhost:8081/oauth2/authorization/google";
        }
        return { success: false, reason: "AUTH_REQUIRED" };
      }

      // 處理其他錯誤
      console.error("同步失敗:", error);
      throw error;
    } finally {
      isSyncing.value = false;
    }
  };

  return {
    syncToGoogle,
    isSyncing,
  };
}
