// [修正 1] 必須匯入 node 的路徑工具，否則底下的 fileURLToPath 會報錯
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],

  // [修正 2] resolve 應該與 plugins、server 平級，不能寫在 server 裡面
  resolve: {
    alias: {
      // 這就是解決你上一題 [UNRESOLVED_IMPORT] 的終極大絕招
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },

  server: {
    port: 5173,
    proxy: {
      // 當前端呼叫 /api/login 時，Vite 會幫你轉發到 http://localhost:8081/api/login
      "/api": {
        target: "http://localhost:8081",
        changeOrigin: true,
        // 如果你的 Spring Boot 控制器沒有寫 /api 前綴，這裡可以加上 rewrite 去掉它
        // rewrite: (path) => path.replace(/^\/api/, '')
      },
    },
  },
});
