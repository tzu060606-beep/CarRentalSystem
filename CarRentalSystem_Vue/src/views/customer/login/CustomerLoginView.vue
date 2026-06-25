<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authAPI } from '../../../api';
import { GoogleLogin } from 'vue3-google-login';


const router = useRouter();
const isLoading = ref(false);

const loginForm = reactive({
  accountOrEmail: '',
  password: '',
  rememberMe: false
});

const handleLogin = async () => {
  isLoading.value = true;
  
  try {
    // 真正呼叫 authAPI 裡的 customerLogin，並傳入帳號密碼
    const res = await authAPI.customerLogin(loginForm.accountOrEmail, loginForm.password);
    
    // 如果後端回傳登入成功
    if (res.data.success) {
      
      // 儲存 Token 與客戶資料到 localStorage
      // 把後端給的 Token 存進去，以後每次打 API 都要靠這把鑰匙
      localStorage.setItem('customer_token', res.data.token);
      
      // 把客戶的基本資料也存進去 (要先轉成字串 JSON.stringify)
      // 假設後端有回傳 customer 這個物件，我們存起來以後可以用來顯示名字
      if (res.data.customer) {
        localStorage.setItem('customer', JSON.stringify(res.data.customer));
      }

      // 跳轉到前台首頁
      router.push('/');
    }

  } catch (error) {
    // 捕捉後端回傳的 401 錯誤，並彈出提示
    if (error.response && error.response.data) {
      alert('登入失敗：' + error.response.data.message);
    } else {
      alert('發生錯誤，請稍後再試！');
    }
  } finally {
    isLoading.value = false;
  }
};
// 一鍵登入(Demo Customer 填入李小龍的帳號密碼)
const fillDemoCustomer = () => {
  loginForm.accountOrEmail = 'lee001';
  loginForm.password = 'hashed_c_001';
};

// 處理 Google 登入回傳的 Credential
const handleGoogleLogin = async (response) => {
  isLoading.value = true;
  try {
    const res = await authAPI.customerGoogleLogin(response.credential);
    
    if (res.data.success) {
      localStorage.setItem('customer_token', res.data.token);
      
      if (res.data.customer) {
        localStorage.setItem('customer', JSON.stringify(res.data.customer));
      }
      
      // _______判斷是否為新會員並跳轉_______
      if (res.data.isNewUser) {
        alert('歡迎使用 Google 登入！請先補齊您的基本資料以利後續租車。');
        router.push('/customer/member/profile'); 
      } else {
        router.push('/'); 
      }
    }
  } catch (error) {
    if (error.response && error.response.data) {
      alert('Google 登入失敗：' + error.response.data.message);
    } else {
      alert('Google 登入發生錯誤，請稍後再試！');
    }
  } finally {
    isLoading.value = false;
  }
};

//密碼輸入窗的眼睛遮罩控制
const showPassword = ref(false);
</script>

<template>
  <!-- 使用 d-flex 與 vh-100 達成全螢幕居中 -->
  <div class="container-fluid min-vh-100 d-flex align-items-center justify-content-center bg-light py-5">
    
    <!-- 登入卡片，設定最大寬度並加上陰影 -->
    <div class="card shadow-sm border-0 rounded-4 p-4" style="max-width: 400px; width: 100%;">
      
      <!-- 品牌 Logo 區塊 -->
      <div class="text-center mb-4">
        <h2 class="fw-bold text-secondary mb-1">One Rent</h2>
        <p class="text-muted small">歡迎回來，輕鬆租車，自在出行</p>
      </div>

      <!-- 登入表單 -->
      <form @submit.prevent="handleLogin">
        
        <!-- 帳號輸入框 -->
        <div class="mb-3">
          <label for="email" class="form-label fw-medium">帳號 / Email</label>
          <input 
            type="text" 
            id="email" 
            v-model="loginForm.accountOrEmail" 
            class="form-control form-control-lg rounded-3" 
            placeholder="請輸入註冊信箱" 
            required
          />
        </div>

        <!-- 密碼輸入框 -->
<div class="mb-3">
  <label for="password" class="form-label fw-medium">密碼</label>
  <div class="input-group">
    <input
      :type="showPassword ? 'text' : 'password'"
      id="password"
      v-model="loginForm.password"
      class="form-control form-control-lg rounded-start-3"
      placeholder="請輸入密碼"
      required
    />
    <span
      class="input-group-text"
      style="cursor: pointer;"
      @click="showPassword = !showPassword"
    >
      <i :class="showPassword ? 'fa-solid fa-eye-slash' : 'fa-solid fa-eye'"></i>
    </span>
  </div>
</div>

        <!-- 記住我與忘記密碼 -->
        <div class="d-flex justify-content-between align-items-center mb-4">
          <div class="form-check">
            <input class="form-check-input" type="checkbox" id="rememberMe" v-model="loginForm.rememberMe">
            <label class="form-check-label small" for="rememberMe">記住我</label>
          </div>
          <router-link to="/customer/forgot-password" class="text-decoration-none small">忘記密碼？</router-link>
        </div>

        <!-- Demo Customer 與 Google 登入 -->
        <div class="d-grid gap-2 mb-4">
          <!-- Google 登入按鈕 -->
          <div class="d-flex justify-content-center">
            <GoogleLogin :callback="handleGoogleLogin" />
          </div>

          <button
            type="button"
            class="btn btn-outline-secondary rounded-3 py-2 fw-semibold"
            @click="fillDemoCustomer"
          >
            <i class="fa-solid fa-user-check me-2"></i>
            Demo Customer
          </button>
         </div>         

        <!-- 登入按鈕：使用 btn-primary 對應首頁深藍色 -->
        <button 
          type="submit" 
          :disabled="isLoading" 
          class="btn btn-primary btn-lg w-100 rounded-3 fw-bold py-2"
        >
          <span v-if="isLoading" class="spinner-border spinner-border-sm me-2"></span>
          {{ isLoading ? '登入中...' : '登入' }}
        </button>
      </form>

      <!-- 底部連結 -->
      <div class="text-center mt-4">
        <span class="text-muted small">還沒有帳號嗎？</span>
        <router-link to="/customer/register" class="text-decoration-none small fw-bold ms-1 ">立即註冊</router-link>
      </div>
    </div>
  </div>
</template>



<style scoped>
/* 自定義主色調，以符合 OneRent 品牌藍 (參考首頁顏色) */
.btn-primary, .text-primary {
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

/* 讓背景帶一點首頁的藍灰色調 */
.bg-light {
  background-color: #f8fafd !important;
}
</style>