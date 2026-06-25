<script setup>
// ============================================================
// LoginView.vue — 員工登入頁面
// 保留原有 authAPI 邏輯，套用 Stitch 設計的 OneRent 品牌外觀
// ============================================================
import { ref } from 'vue'
import { useRouter } from 'vue-router'
// ##新增 ref、useRouter

import { authAPI } from '../../../api'

const router = useRouter()

// --- 表單狀態 ---
const account = ref('')
const password = ref('')
const errorMsg = ref('')
const loading = ref(false)

// --- 登入邏輯（完整保留原始功能） ---
const handleLogin = async () => {
  errorMsg.value = ''
  loading.value = true

  try {
    //呼叫後端 API（取代原本 form submit 到 LoginServlet）
    const res = await authAPI.login(account.value, password.value)

    if (res.data.success) {
     // 🌟 重點修改區塊開始：儲存 Token 🌟
      // 請依據你們後端的實際 JSON 欄位名稱調整，這裡假設後端把 token 放在 res.data.token
      const token = res.data.token
      if (token) {
        localStorage.setItem('token', token)
        console.log('Token 儲存成功！準備導向首頁')
      } else {
        console.warn('注意：登入成功，但前端沒抓到 Token！請檢查後端回傳的 JSON 格式。')
      }

      // 🌟 重點修改區塊結束 🌟

      // 登入成功：導向首頁（取代原本的 response.sendRedirect）
      router.push('/')
    }
  } catch (err) {
     // 登入失敗：顯示錯誤訊息（取代原本的 request.setAttribute("errorMessage")
    if (err.response && err.response.data && err.response.data.message) {
      errorMsg.value = err.response.data.message
    } else if (err.response) {
      errorMsg.value = '伺服器錯誤 (' + err.response.status + ')，請檢查後端 Console'
    } else if (err.request) {
      errorMsg.value = '無法連接後端伺服器，請確認 Spring Boot 已啟動 (port 8080)'
    } else {
      errorMsg.value = '登入失敗：' + err.message
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <!-- ##頁面全改 -->
  <!-- 登入頁面：全螢幕置中 -->
  <div class="d-flex align-items-center justify-content-center min-vh-100 p-3" style="background-color: var(--color-bg-page); font-family: var(--font-sans);">
    <!-- 登入卡片 -->
    <div class="bg-white position-relative overflow-hidden w-100" style="max-width: 420px; border-radius: var(--radius-lg); box-shadow: 0 4px 12px rgba(1, 80, 173, 0.15); padding: 2rem;">
      <!-- 頂部品牌色裝飾條 -->
      <div class="position-absolute top-0 start-0 w-100" style="height: 6px; background: linear-gradient(to right, var(--color-primary), var(--color-secondary));"></div>

      <!-- 品牌標題區 -->
      <div class="text-center mb-4 mt-2">
        <div class="d-flex justify-content-center mb-3">
          <i class="fa-solid fa-car-side text-primary" style="font-size: 2rem;"></i>
        </div>
        <h1 class="h2 fw-bold text-primary mb-1">OneRent</h1>
        <p class="text-lead text-secondary mb-0">員工登入</p>
      </div>

      <!-- 錯誤訊息 -->
      <div v-if="errorMsg" class="alert alert-danger d-flex align-items-center gap-2 mb-3 py-2 px-3 text-small" role="alert" style="background-color: var(--color-danger-bg); color: var(--color-danger); border: 1px solid var(--color-red-200);">
        <i class="fa-solid fa-circle-exclamation"></i>
        <span>{{ errorMsg }}</span>
      </div>

      <!-- 登入表單 -->
      <form @submit.prevent="handleLogin">
        <!-- 帳號輸入框 -->
        <div class="mb-3">
          <label for="login-account" class="form-label d-block text-caption fw-semibold text-secondary text-uppercase mb-2" style="letter-spacing: 0.05em;">ACCOUNT</label>
          <div class="position-relative">
            <div class="position-absolute text-muted" style="top: 50%; left: 0.75rem; transform: translateY(-50%); z-index: 2; pointer-events: none;">
              <i class="fa-solid fa-user"></i>
            </div>
            <input
              id="login-account"
              v-model="account"
              type="text"
              class="form-control bg-white"
              style="padding-left: 2.5rem; padding-top: 0.5rem; padding-bottom: 0.5rem; font-size: var(--text-base); transition: border-color 0.2s, box-shadow 0.2s;"
              onfocus="this.style.borderColor='var(--color-primary)'; this.style.boxShadow='0 0 0 3px rgba(1, 80, 173, 0.15)'"
              onblur="this.style.borderColor='var(--color-border)'; this.style.boxShadow='none'"
              placeholder="Enter your account id"
              required
            />
          </div>
        </div>

        <!-- 密碼輸入框 -->
        <div class="mb-4">
          <label for="login-password" class="form-label d-block text-caption fw-semibold text-secondary text-uppercase mb-2" style="letter-spacing: 0.05em;">PASSWORD</label>
          <div class="position-relative">
            <div class="position-absolute text-muted" style="top: 50%; left: 0.75rem; transform: translateY(-50%); z-index: 2; pointer-events: none;">
              <i class="fa-solid fa-lock"></i>
            </div>
            <input
              id="login-password"
              v-model="password"
              type="password"
              class="form-control bg-white"
              style="padding-left: 2.5rem; padding-top: 0.5rem; padding-bottom: 0.5rem; font-size: var(--text-base); transition: border-color 0.2s, box-shadow 0.2s;"
              onfocus="this.style.borderColor='var(--color-primary)'; this.style.boxShadow='0 0 0 3px rgba(1, 80, 173, 0.15)'"
              onblur="this.style.borderColor='var(--color-border)'; this.style.boxShadow='none'"
              placeholder="Enter your password"
              required
            />
          </div>
        </div>

        <!-- 登入按鈕 -->
        <button
          type="submit"
          class="btn w-100 d-flex align-items-center justify-content-center gap-2 text-white fw-semibold border-0"
          style="font-size: var(--text-lg); background: linear-gradient(to right, var(--color-primary), var(--color-primary-hover)); padding: 0.6rem 1.5rem; border-radius: var(--radius-md); box-shadow: var(--shadow-sm); transition: all 0.25s;"
          :disabled="loading"
          id="login-submit"
          onmouseover="if(!this.disabled){this.style.boxShadow='0 4px 12px rgba(1, 80, 173, 0.22)'; this.style.transform='translateY(-1px)'; this.querySelector('.btn-arrow') && (this.querySelector('.btn-arrow').style.transform='translateX(4px)')}"
          onmouseout="if(!this.disabled){this.style.boxShadow='var(--shadow-sm)'; this.style.transform='none'; this.querySelector('.btn-arrow') && (this.querySelector('.btn-arrow').style.transform='none')}"
        >
          <i v-if="loading" class="fa-solid fa-spinner fa-spin"></i>
          <span>{{ loading ? '登入中...' : '登入' }}</span>
          <i v-if="!loading" class="fa-solid fa-arrow-right btn-arrow" style="transition: transform 0.2s;"></i>
        </button>
      </form>

      <!-- 底部文字 -->
      <div class="text-center mt-4">
        <p class="text-small text-muted mb-0">Secure Access Required</p>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
