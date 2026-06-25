<script setup>
import { ref, reactive, onMounted } from 'vue';
import { customerAPI } from '../../../api/login/customerAPI';
import { authAPI } from '../../../api';

// 真實客戶資料
const profile = reactive({
  custId: null,
  custAccount: '',
  custName: '',
  custPhone: '',
  custEmail: '',
  custAddress: '',
  custLicense: '',
  custStatus: '',
  custAvatar: '',
  avatarBase64: ''
});

// 修改密碼的表單
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 狀態控制
const isSavingProfile = ref(false);
const isSavingPassword = ref(false);
const avatarError = ref('');  // 大頭貼上傳錯誤訊息

// 自訂大頭貼上傳處理（不使用共用元件以保持圓形樣式）
const onAvatarFileChange = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) {
    avatarError.value = '圖片大小不能超過 5MB';
    return;
  }
  avatarError.value = '';
  const reader = new FileReader();
  reader.onload = (ev) => { profile.avatarBase64 = ev.target.result; };
  reader.readAsDataURL(file);
};

const clearAvatar = () => {
  profile.avatarBase64 = '';
  profile.custAvatar = '';
  avatarError.value = '';
};

// 提示訊息控制
const alertMessage = ref('');
const alertType = ref('success'); // 'success' 或 'danger'

const showAlert = (message, type = 'success') => {
  alertMessage.value = message;
  alertType.value = type;
  // 5秒後自動關閉提示
  setTimeout(() => {
    alertMessage.value = '';
  }, 5000);
};

// 進入畫面時載入客戶資料
onMounted(async () => {
  const customerStr = localStorage.getItem('customer');
  if (customerStr) {
    const localCust = JSON.parse(customerStr);
    try {
      // 透過 API 取得最新資料
      const res = await customerAPI.getById(localCust.custId);
      const data = res.data;
      
      profile.custId = data.custId;
      profile.custAccount = data.custAccount;
      profile.custName = data.custName;
      profile.custPhone = data.custPhone;
      profile.custEmail = data.custEmail;
      profile.custAddress = data.custAddress || '';
      profile.custLicense = data.custLicense || '';
      profile.custStatus = data.custStatus;
      profile.custAvatar = data.custAvatar || '';
    } catch (error) {
      console.error('取得客戶資料失敗:', error);
      showAlert('取得資料失敗，請稍後再試。', 'danger');
    }
  }
});

// 表單驗證錯誤訊息
const errors = reactive({
  phone: '',
  email: '',
  license: ''
});

// 驗證電話格式 (10碼數字)
const validatePhone = () => {
  const phoneRegex = /^09\d{8}$/;
  if (!profile.custPhone) {
    errors.phone = '手機號碼為必填';
    return false;
  } else if (!phoneRegex.test(profile.custPhone)) {
    errors.phone = '手機格式錯誤，請輸入 10 碼數字（例如：0912345678）';
    return false;
  }
  errors.phone = '';
  return true;
};

// 驗證 Email 格式
const validateEmail = () => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!profile.custEmail) {
    errors.email = 'Email 為必填';
    return false;
  } else if (!emailRegex.test(profile.custEmail)) {
    errors.email = '請輸入有效的 Email 格式';
    return false;
  }
  errors.email = '';
  return true;
};
// 驗證駕照格式 (A123456789)
const validateLicense = () => {
  const licenseRegex = /^[A-Z]\d{9}$/;
  if (!profile.custLicense) {
    errors.license = '';  // 駕照非必填，空白不報錯
    return true;
  } else if (!licenseRegex.test(profile.custLicense)) {
    errors.license = '駕照格式錯誤，請輸入 1個英文字母 + 9個數字（例如：A123456789）';
    return false;
  }
  errors.license = '';
  return true;
};

// 儲存基本資料
const updateProfile = async () => {
  // 送出前先驗證
  const isPhoneValid = validatePhone();
  const isEmailValid = validateEmail();
  const isLicenseValid = validateLicense(); 
  if (!isPhoneValid || !isEmailValid|| !isLicenseValid) {
    showAlert('請確認資料格式是否正確。', 'danger');
    return;
  }

  isSavingProfile.value = true;
  
  try {
    if (profile.custLicense) {
      profile.custLicense = profile.custLicense.toUpperCase(); // 駕照自動轉大寫
    }
    
    // 準備傳給後端的物件
    const updateData = { ...profile };
    // 如果有新上傳的圖片，帶入 base64；否則帶入現有的 custAvatar（若已被清空則為空字串）
    updateData.avatarBase64 = profile.avatarBase64 || '';
    
    const res = await customerAPI.update(profile.custId, updateData);
    if (res.data.success) {
      showAlert('您的個人資料已成功更新！', 'success');
      
      // 從後端回傳的最新資料更新前端狀態
      const updatedCustomer = res.data.data;
      if (updatedCustomer) {
        // 更新本地 profile 的 custAvatar（確保顯示新頭像）
        profile.custAvatar = updatedCustomer.custAvatar || '';
        profile.avatarBase64 = ''; // 清空 base64 暫存
        
        // 更新 localStorage
        localStorage.setItem('customer', JSON.stringify(updatedCustomer));
        
        // 發送自定義事件通知 MemberSidebar 即時更新頭像
        window.dispatchEvent(new CustomEvent('customer-updated', { detail: updatedCustomer }));
      }
    }
  } catch (error) {
    showAlert('更新失敗：' + (error.response?.data?.message || '系統錯誤'), 'danger');
  } finally {
    isSavingProfile.value = false;
  }
};

// 儲存新密碼
const updatePassword = async () => {
  // 簡易前端驗證：檢查兩次新密碼是否相同
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showAlert('新密碼與確認密碼不符，請重新確認。', 'danger');
    return;
  }
  
  if (passwordForm.newPassword.length < 6) {
    showAlert('新密碼長度必須大於 6 個字元。', 'danger');
    return;
  }

  isSavingPassword.value = true;

  try {
    // 1. 先驗證「目前密碼」是否正確（透過登入 API 測試）
    const loginRes = await authAPI.customerLogin(profile.custAccount, passwordForm.currentPassword);
    
    if (loginRes.data.success) {
      // 2. 密碼驗證成功，發送修改密碼請求
      const updateData = { ...profile, custPassword: passwordForm.newPassword };
      const res = await customerAPI.update(profile.custId, updateData);
      
      if (res.data.success) {
        showAlert('密碼已成功變更！下次登入請使用新密碼。', 'success');
        
        // 清空密碼表單
        passwordForm.currentPassword = '';
        passwordForm.newPassword = '';
        passwordForm.confirmPassword = '';
      }
    }
  } catch (error) {
    // 登入驗證失敗或修改失敗
    if (error.response && error.response.status === 401) {
      showAlert('目前密碼錯誤，請重新確認。', 'danger');
    } else {
      showAlert('修改密碼失敗：' + (error.response?.data?.message || '系統錯誤'), 'danger');
    }
  } finally {
    isSavingPassword.value = false;
  }
};


</script>

<template>
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-lg-8">

        <!-- 標題 -->
        <div class="card shadow-sm border-0 rounded-3 mb-4">
          <div class="card-body d-flex align-items-center">
            <i class="fa-solid fa-user-gear text-primary fs-2 me-3"></i>

            <div>
              <h4 class="fw-bold mb-1">會員資料管理</h4>
              <p class="text-muted mb-0">
                您可以在此修改聯絡資訊
              </p>
            </div>
          </div>
        </div>

        <!-- 提示訊息 -->
        <div
          v-if="alertMessage"
          :class="`alert alert-${alertType} alert-dismissible fade show`"
        >
          <i
            :class="
              alertType === 'success'
                ? 'fa-solid fa-circle-check'
                : 'fa-solid fa-circle-exclamation'
            "
            class="me-2"
          ></i>

          {{ alertMessage }}

          <button
            type="button"
            class="btn-close"
            @click="alertMessage = ''"
          ></button>
        </div>

        <!-- 基本資料 -->
        <div class="card shadow-sm border-0 rounded-3 mb-4">
          <div class="card-body p-4">

            <h5 class="fw-bold border-bottom pb-2 mb-4">
              <i class="fa-solid fa-id-card text-primary me-2"></i>
              基本資料
            </h5>
              
            <form @submit.prevent="updateProfile">

              <!-- 大頭貼上傳（圓形，不使用共用元件） -->
              <div class="row mb-4">
                <div class="col-12 d-flex flex-column align-items-center">

                  <!-- 圓形頭像顯示區 -->
                  <div class="profile-avatar-circle">
                    <img
                      v-if="profile.avatarBase64 || profile.custAvatar"
                      :src="profile.avatarBase64 || profile.custAvatar"
                      alt="頭像預覽"
                      class="profile-avatar-img"
                    />
                    <span v-else class="profile-avatar-initial">
                      {{ profile.custName?.charAt(0)?.toUpperCase() ?? '?' }}
                    </span>
                  </div>

                  <!-- 操作按鈕 -->
                  <div class="mt-3 d-flex gap-2">
                    <label class="btn btn-sm btn-outline-primary" style="cursor: pointer;">
                      <i class="fa-solid fa-camera me-1"></i>
                      {{ profile.avatarBase64 || profile.custAvatar ? '更換照片' : '上傳照片' }}
                      <input type="file" accept="image/*" class="d-none" @change="onAvatarFileChange" />
                    </label>
                    <button
                      v-if="profile.avatarBase64 || profile.custAvatar"
                      type="button"
                      class="btn btn-sm btn-outline-danger"
                      @click="clearAvatar"
                    >
                      <i class="fa-solid fa-trash me-1"></i>移除照片
                    </button>
                  </div>

                  <p v-if="avatarError" class="text-danger small mt-1 mb-0">{{ avatarError }}</p>
                  <div class="text-muted small mt-1">建議尺寸 200×200px，最大 5MB</div>
                </div>
              </div>

              <div class="row g-3">

                <!-- 姓名 -->
                <div class="col-md-6">
                  <label class="form-label">姓名</label>

                  <div class="input-group">
                    <span class="input-group-text">
                      <i class="fa-solid fa-user"></i>
                    </span>

                    <input
                      v-model="profile.custName"
                      type="text"
                      class="form-control"
                      readonly
                    />
                  </div>
                </div>

                <!-- 駕照 -->
                <div class="col-md-6">
                  <label class="form-label">駕照號碼</label>

                  <div class="input-group has-validation">
                    <span class="input-group-text">
                      <i class="fa-solid fa-address-card"></i>
                    </span>

                    <input
                      v-model="profile.custLicense"
                      @blur="validateLicense"
                      type="text"
                      class="form-control"
                      :class="{ 'is-invalid': errors.license }"
                      placeholder="例如：A123456789"
                      maxlength="10"
                    />
                    <div class="invalid-feedback" v-if="errors.license">
                      {{ errors.license }}
                    </div>
                  </div>
                </div>

                <!-- 電話 -->
                <div class="col-md-6">
                  <label class="form-label">手機號碼</label>

                  <div class="input-group has-validation">
                    <span class="input-group-text">
                      <i class="fa-solid fa-phone"></i>
                    </span>

                    <input
                      v-model="profile.custPhone"
                      @blur="validatePhone"
                      type="tel"
                      class="form-control"
                      :class="{ 'is-invalid': errors.phone }"
                      placeholder="0912345678"
                    />
                    <div class="invalid-feedback" v-if="errors.phone">
                      {{ errors.phone }}
                    </div>
                  </div>
                </div>

                <!-- Email -->
                <div class="col-md-6">
                  <label class="form-label">Email</label>

                  <div class="input-group has-validation">
                    <span class="input-group-text">
                      <i class="fa-solid fa-envelope"></i>
                    </span>

                    <input
                      v-model="profile.custEmail"
                      @blur="validateEmail"
                      type="email"
                      class="form-control"
                      :class="{ 'is-invalid': errors.email }"
                      placeholder="example@mail.com"
                    />
                    <div class="invalid-feedback" v-if="errors.email">
                      {{ errors.email }}
                    </div>
                  </div>
                </div>

                <!-- 地址 -->
                <div class="col-12">
                  <label class="form-label">地址</label>

                  <div class="input-group">
                    <span class="input-group-text">
                      <i class="fa-solid fa-location-dot"></i>
                    </span>

                    <input
                      v-model="profile.custAddress"
                      type="text"
                      class="form-control"
                    />
                  </div>
                </div>

              </div>

              <div class="text-end mt-4">
                <button
                  class="btn btn-primary"
                  :disabled="isSavingProfile"
                >
                  <i
                    v-if="isSavingProfile"
                    class="fa-solid fa-spinner fa-spin me-1"
                  ></i>

                  <i
                    v-else
                    class="fa-solid fa-floppy-disk me-1"
                  ></i>

                  儲存資料
                </button>
              </div>

            </form>
          </div>
        </div>

        

      </div>
    </div>
  </div>
</template>

<style scoped>
/* 精緻化樣式微調 */
.fs-7 {
  font-size: 0.875rem;
}
.fs-8 {
  font-size: 0.75rem;
}
.bg-success-subtle {
  background-color: #d1e7dd;
}
/* 讓輸入框聚焦時的藍色陰影優雅一點 */
.form-control:focus {
  border-color: #86b7fe;
  box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.15);
}

/* ── 圓形大頭貼 ── */
.profile-avatar-circle {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #dee2e6;
  background-color: #e9ecef;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-initial {
  font-size: 3.5rem;
  font-weight: 700;
  color: #6c757d;
  line-height: 1;
}
</style>