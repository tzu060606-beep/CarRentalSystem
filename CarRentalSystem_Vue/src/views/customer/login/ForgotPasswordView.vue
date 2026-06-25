<script setup>
import { ref } from 'vue';
import { authAPI } from '../../../api';

const email = ref('');
const isLoading = ref(false);
const alertMessage = ref('');
const alertType = ref('success');
const isSent = ref(false);

const handleSubmit = async () => {
  if (!email.value) {
    alertMessage.value = '請輸入您的 Email';
    alertType.value = 'danger';
    return;
  }

  isLoading.value = true;
  alertMessage.value = '';

  try {
    const res = await authAPI.forgotPassword(email.value);
    if (res.data.success) {
      isSent.value = true;
      alertMessage.value = res.data.message;
      alertType.value = 'success';
    }
  } catch (error) {
    alertMessage.value = error.response?.data?.message || '發生錯誤，請稍後再試';
    alertType.value = 'danger';
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <div class="container-fluid min-vh-100 d-flex align-items-center justify-content-center bg-light ">
    <div class="card shadow-sm border-0 rounded-4 p-4" style="max-width: 420px; width: 100%;">

      <!-- 標題 -->
      <div class="text-center mb-4">
        <div class="bg-primary bg-opacity-10 rounded-circle d-inline-flex align-items-center justify-content-center mb-3" style="width: 64px; height: 64px;">
          <i class="fa-solid fa-envelope-open-text text-primary fs-3"></i>
        </div>
        <h4 class="fw-bold">忘記密碼</h4>
        <p class="text-muted small mb-0">
          請輸入您註冊時使用的 Email，<br>我們將寄送密碼重設連結給您。
        </p>
      </div>

      <!-- 提示訊息 -->
      <div v-if="alertMessage" :class="`alert alert-${alertType} small`">
        <i :class="alertType === 'success' ? 'fa-solid fa-circle-check' : 'fa-solid fa-circle-exclamation'" class="me-2"></i>
        {{ alertMessage }}
      </div>

      <!-- 已寄出狀態 -->
      <div v-if="isSent" class="text-center py-3">
        <i class="fa-solid fa-paper-plane text-success display-4 mb-3"></i>
        <p class="text-muted small">
          請至您的信箱查收信件，並在 <strong>15 分鐘內</strong> 點擊信中的連結重設密碼。
        </p>
        <router-link to="/customer/login" class="btn btn-outline-primary rounded-3 mt-2">
          <i class="fa-solid fa-arrow-left me-2"></i>返回登入
        </router-link>
      </div>

      <!-- 表單 -->
      <form v-else @submit.prevent="handleSubmit">
        <div class="mb-4">
          <label for="email" class="form-label fw-medium">Email 信箱</label>
          <div class="input-group">
            <span class="input-group-text"><i class="fa-solid fa-envelope text-muted"></i></span>
            <input
              v-model="email"
              type="email"
              id="email"
              class="form-control form-control-lg rounded-end-3"
              placeholder="請輸入您的 Email"
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
          {{ isLoading ? '寄送中...' : '發送重設連結' }}
        </button>

        <div class="text-center">
          <router-link to="/customer/login" class="text-decoration-none small">
            <i class="fa-solid fa-arrow-left me-1"></i>返回登入頁
          </router-link>
        </div>
      </form>

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