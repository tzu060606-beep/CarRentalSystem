<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authAPI } from '../../../api';

const route = useRoute();
const router = useRouter();

const token = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const isLoading = ref(false);
const alertMessage = ref('');
const alertType = ref('success');
const isSuccess = ref(false);

onMounted(() => {
  // 從 URL 的 ?token=xxx 取得 token
  token.value = route.query.token || '';
  if (!token.value) {
    alertMessage.value = '無效的重設連結，缺少 Token 參數。';
    alertType.value = 'danger';
  }
});

const handleSubmit = async () => {
  // 前端驗證
  if (newPassword.value.length < 6) {
    alertMessage.value = '新密碼長度必須大於 6 個字元';
    alertType.value = 'danger';
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    alertMessage.value = '兩次密碼輸入不一致';
    alertType.value = 'danger';
    return;
  }

  isLoading.value = true;
  alertMessage.value = '';

  try {
    const res = await authAPI.resetPassword(token.value, newPassword.value);
    if (res.data.success) {
      isSuccess.value = true;
      alertMessage.value = res.data.message;
      alertType.value = 'success';
      // 3 秒後自動跳轉登入頁
      setTimeout(() => {
        router.push('/customer/login');
      }, 3000);
    }
  } catch (error) {
    alertMessage.value = error.response?.data?.message || '重設失敗，請稍後再試';
    alertType.value = 'danger';
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <div class="container-fluid min-vh-100 d-flex align-items-center justify-content-center bg-light mt-5">
    <div class="card shadow-sm border-0 rounded-4 p-4" style="max-width: 420px; width: 100%;">

      <!-- 標題 -->
      <div class="text-center mb-4">
        <div class="bg-primary bg-opacity-10 rounded-circle d-inline-flex align-items-center justify-content-center mb-3" style="width: 64px; height: 64px;">
          <i class="fa-solid fa-key text-primary fs-3"></i>
        </div>
        <h4 class="fw-bold">重設密碼</h4>
        <p class="text-muted small mb-0">請設定您的新登入密碼</p>
      </div>

      <!-- 提示訊息 -->
      <div v-if="alertMessage" :class="`alert alert-${alertType} small`">
        <i :class="alertType === 'success' ? 'fa-solid fa-circle-check' : 'fa-solid fa-circle-exclamation'" class="me-2"></i>
        {{ alertMessage }}
      </div>

      <!-- 成功狀態 -->
      <div v-if="isSuccess" class="text-center py-3">
        <i class="fa-solid fa-circle-check text-success display-4 mb-3"></i>
        <p class="text-muted small">密碼已重設成功！<br>即將自動跳轉至登入頁面...</p>
      </div>

      <!-- 表單 -->
      <form v-else-if="token" @submit.prevent="handleSubmit">
        <div class="mb-3">
          <label class="form-label fw-medium">新密碼</label>
          <div class="input-group">
            <span class="input-group-text"><i class="fa-solid fa-lock text-muted"></i></span>
            <input
              v-model="newPassword"
              type="password"
              class="form-control form-control-lg rounded-end-3"
              placeholder="請輸入新密碼 (至少 6 碼)"
              required
            />
          </div>
        </div>

        <div class="mb-4">
          <label class="form-label fw-medium">確認新密碼</label>
          <div class="input-group">
            <span class="input-group-text"><i class="fa-solid fa-circle-check text-muted"></i></span>
            <input
              v-model="confirmPassword"
              type="password"
              class="form-control form-control-lg rounded-end-3"
              placeholder="請再次輸入新密碼"
              required
            />
          </div>
        </div>

        <button
          type="submit"
          class="btn btn-primary btn-lg w-100 rounded-3 fw-bold py-2 mb-3"
          :disabled="isLoading"
        >
          <span v-if="isLoading" class="spinner-border spinner-border-sm me-2"></span>
          {{ isLoading ? '處理中...' : '確認重設密碼' }}
        </button>
      </form>

      <!-- 無效 Token 時的返回連結 -->
      <div v-if="!token" class="text-center mt-2">
        <router-link to="/customer/forgot-password" class="btn btn-outline-primary rounded-3">
          <i class="fa-solid fa-arrow-left me-2"></i>重新申請
        </router-link>
      </div>

    </div>
  </div>
</template>

<style scoped>
.btn-primary {
  background-color: #00448a !important;
  border-color: #00448a !important;
}
.btn-primary:hover {
  background-color: #003366 !important;
  border-color: #003366 !important;
}
.text-primary {
  color: #00448a !important;
}
.bg-light {
  background-color: #f8fafd !important;
}
</style>
