<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authAPI } from '../../../api';

const router = useRouter();
const loading = ref(false);
const agree = ref(false);
const confirmPassword = ref('');

// 表單資料對應資料庫欄位 (使用駝峰命名配合後端 DTO)
const form = reactive({
  custName: '',
  custPhone: '',
  custEmail: '',
  custAccount: '',
  custPassword: '',
  custAddress: '',
  custStatus: '啟用' // 預設狀態
});

// 一鍵註冊 / 填入測試資料功能
const quickFill = () => {
  form.custName = '測試帳號';
  form.custAccount = 'test' + Math.floor(Math.random() * 1000); // 隨機產生帳號避免重複
  form.custEmail = `${form.custAccount}@onerent.com`;
  form.custPhone = '0912345678';
  form.custAddress = '台北市大安區忠孝東路四段 1 號';
  form.custPassword = 'password123';
  confirmPassword.value = 'password123';
  agree.value = true; // 自動勾選同意條款
};

const handleRegister = async () => {
  // 基礎驗證
  if (form.custPassword !== confirmPassword.value) {
    alert('密碼與確認密碼不符！');
    return;
  }

  loading.value = true;
  
  try {
    // 呼叫 API 送出資料
    const res = await authAPI.customerRegister(form);
    
    if (res.data.success) {
      alert('註冊成功！系統將為您自動登入。您已獲得點數100點，快去租車體驗吧！');
      
      // 註冊成功後，直接使用剛才填寫的帳號密碼打登入 API
      const loginRes = await authAPI.customerLogin(form.custAccount, form.custPassword);
      
      if (loginRes.data.success) {
        // 將 Token 與客戶資料存入 localStorage
        localStorage.setItem('customer_token', loginRes.data.token);
        if (loginRes.data.customer) {
          localStorage.setItem('customer', JSON.stringify(loginRes.data.customer));
        }
        // 跳轉至首頁
        router.push('/');
      } else {
        // 萬一自動登入失敗，跳到登入頁讓使用者自己登入
        router.push('/customer/login');
      }
    }
  } catch (error) {
    if (error.response && error.response.data) {
      alert('註冊失敗：' + error.response.data.message);
    } else {
      alert('註冊失敗，請稍後再試');
    }
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="register-wrapper d-flex align-items-center justify-content-center py-4 bg-light ">
    <!-- 外層卡片：保持 900px 寬度，左右對稱 -->
    <div class="card shadow-lg border-0 rounded-4 overflow-hidden mt-5" style="max-width: 900px; width: 100%;">
      <div class="bg-white p-4 p-md-5">
        
        <!-- 標題區：置中 -->
        <div class="mb-4 text-center">
          <h1 class="fw-bold text-primary mb-1">OneRent</h1>
          <h3 class="fw-bold text-dark mb-1">加入會員</h3>
          <p class="text-muted small">請填寫以下資訊完成註冊</p>
        </div>

        <!-- 表單區 -->
        <form @submit.prevent="handleRegister">
          <!-- 核心調整：利用 Bootstrap 網格將欄位均勻分佈在左右兩側 -->
          <div class="row g-3">
            
            <!-- 👈 左側欄位 -->
            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label small fw-bold mb-1">姓名</label>
                <div class="input-group">
                  <span class="input-group-text bg-light border-0"><i class="fas fa-user text-muted"></i></span>
                  <input type="text" v-model="form.custName" class="form-control bg-light border-0" required>
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label small fw-bold mb-1">手機號碼</label>
                <div class="input-group">
                  <span class="input-group-text bg-light border-0"><i class="fas fa-phone text-muted"></i></span>
                  <input type="tel" v-model="form.custPhone" class="form-control bg-light border-0" required>
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label small fw-bold mb-1">設定密碼</label>
                <input type="password" v-model="form.custPassword" class="form-control bg-light border-0" required>
              </div>
            </div>

            <!-- 👉 右側欄位 -->
            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label small fw-bold mb-1">登入帳號</label>
                <div class="input-group">
                  <span class="input-group-text bg-light border-0"><i class="fas fa-id-card text-muted"></i></span>
                  <input type="text" v-model="form.custAccount" class="form-control bg-light border-0" required>
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label small fw-bold mb-1">電子信箱</label>
                <div class="input-group">
                  <span class="input-group-text bg-light border-0"><i class="fas fa-envelope text-muted"></i></span>
                  <input type="email" v-model="form.custEmail" class="form-control bg-light border-0" required>
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label small fw-bold mb-1">確認密碼</label>
                <input type="password" v-model="confirmPassword" class="form-control bg-light border-0" required>
              </div>
            </div>

            <!-- 🗺️ 滿版欄位（通訊地址比較長，單獨放在兩欄下方撐滿） -->
            <div class="col-12 mb-2">
              <label class="form-label small fw-bold mb-1">通訊地址 (選填)</label>
              <div class="input-group">
                <span class="input-group-text bg-light border-0"><i class="fas fa-map-marker-alt text-muted"></i></span>
                <input type="text" v-model="form.custAddress" class="form-control bg-light border-0">
              </div>
            </div>
          </div>

          <!-- 同意條款 -->
          <div class="form-check my-3">
            <input class="form-check-input" type="checkbox" id="agree" v-model="agree" required>
            <label class="form-check-label small text-muted" for="agree">
              我已閱讀並同意 OneRent 的 <a href="#" class="text-primary text-decoration-none">隱私權保護政策</a>
            </label>
          </div>

          <!-- 功能按鈕區：左右並排 -->
          <div class="row g-2 mb-3">
            <div class="col-sm-6">
              <button type="submit" class="btn btn-primary w-100 py-2 fw-bold rounded-pill shadow-sm" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                建立會員帳號
              </button>
            </div>
            <div class="col-sm-6">
              <button type="button" @click="quickFill" class="btn btn-outline-secondary w-100 py-2 fw-bold rounded-pill shadow-sm">
                <i class="fas fa-magic me-2"></i>一鍵填入測試資料
              </button>
            </div>
          </div>
        </form>

        <!-- 底部的登入連結 -->
        <div class="text-center mt-3">
          <span class="small text-muted">已有帳號嗎？</span>
          <router-link to="/customer/login" class="small fw-bold ms-1 text-primary text-decoration-none">立即登入</router-link>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
/* 整體容器設定 */
.register-wrapper {
  min-height: 100vh;
  background-color: #f4f7f9;
  font-family: 'Noto Sans TC', sans-serif;
}

/* OneRent 品牌藍顏色覆蓋 */
.bg-primary {
  background-color: #00448a !important;
}

.text-primary {
  color: #00448a !important;
}

.btn-primary {
  background-color: #00448a !important;
  border-color: #00448a !important;
}

.btn-primary:hover {
  background-color: #003366 !important;
  border-color: #003366 !important;
}

/* 表單元件美化 */
.input-group-text {
  border: none;
}

.form-control:focus {
  background-color: #fff !important;
  border: 1px solid #00448a !important;
  box-shadow: none;
}

.card {
  border-radius: 20px;
}

/* 針對行動裝置微調 */
@media (max-width: 991.98px) {
  .card {
    margin: 20px;
  }
}
</style>