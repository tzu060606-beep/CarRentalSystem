<script setup>
import { ref, onMounted } from 'vue'
import StepProgressBar from '@/components/customer/transfer/StepProgressBar.vue'

const emit = defineEmits(['navigate'])
const props = defineProps({
  bookingData: { type: Object, required: true }
})

// 本地表單資料
const name = ref('')
const email = ref('')
const phoneCode1 = ref('+886')
const phone1 = ref('')
const phoneCode2 = ref('+886')
const phone2 = ref('')
const otherPhone = ref('')

const errors = ref({})

const validate = () => {
  errors.value = {}
  if (!name.value.trim()) errors.value.name = '請輸入姓名'
  if (!email.value.trim()) errors.value.email = '請輸入電子信箱'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) errors.value.email = '電子信箱格式不正確'
  if (!phone1.value.trim()) errors.value.phone1 = '請輸入聯絡手機'
  return Object.keys(errors.value).length === 0
}

onMounted(() => {
  try {
    const customerStr = localStorage.getItem('customer')
    if (customerStr) {
      const customer = JSON.parse(customerStr)
      name.value = customer.custName || ''
      email.value = customer.custEmail || customer.email || ''
      phone1.value = customer.custPhone || customer.phone || ''
      if (customer.custId) {
        props.bookingData.custId = customer.custId
      }
    }
  } catch (e) {
    console.error('無法讀取會員資料', e)
  }
})

const goNext = () => {
  if (!validate()) return
  // 寫入 bookingData
  props.bookingData.custName = name.value
  props.bookingData.email = email.value
  props.bookingData.custPhone = phone1.value
  props.bookingData.phone2 = phone2.value
  props.bookingData.otherPhone = otherPhone.value
  emit('navigate', 'step2')
}
</script>

<template>
  <div class="container-fluid py-4">
    <!-- Hero Banner -->
    <div class="card border-0 shadow-sm mb-4 bg-primary bg-gradient text-white rounded-4">
      <div class="card-body p-4 text-center">
        <i class="fa-solid fa-clipboard-list fa-2x mb-2 opacity-75"></i>
        <h2 class="fw-bold mb-1">接送服務線上預約</h2>
        <p class="mb-0 text-white-50">Online Booking</p>
      </div>
    </div>

    <!-- 步驟進度條 -->
    <StepProgressBar :currentStep="1" />

    <!-- 表單卡片 -->
    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <h4 class="fw-bold text-center mb-4">基本資料</h4>

        <div class="row g-3">
          <!-- 姓名 -->
          <div class="col-12">
            <div class="row align-items-center">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">姓名 <span class="text-danger">*</span></label>
              </div>
              <div class="col-md-9">
                <input v-model="name" type="text" class="form-control" :class="{ 'is-invalid': errors.name }" placeholder="請輸入姓名" />
                <div class="invalid-feedback">{{ errors.name }}</div>
              </div>
            </div>
          </div>

          <!-- 電子信箱 -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">電子信箱 <span class="text-danger">*</span></label>
              </div>
              <div class="col-md-9">
                <input v-model="email" type="email" class="form-control" :class="{ 'is-invalid': errors.email }" placeholder="example@mail.com" />
                <div class="invalid-feedback">{{ errors.email }}</div>
                <small class="text-muted d-block mt-1">我們會將本次預約的資料寄到您的電子信箱</small>
              </div>
            </div>
          </div>

          <!-- 聯絡手機 1 -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">聯絡手機 1 <span class="text-danger">*</span></label>
              </div>
              <div class="col-md-9">
                <div class="d-flex gap-2">
                  <select v-model="phoneCode1" class="form-select" style="max-width: 110px;">
                    <option value="+886">+886</option>
                    <option value="+86">+86</option>
                    <option value="+81">+81</option>
                    <option value="+1">+1</option>
                  </select>
                  <input v-model="phone1" type="tel" class="form-control" :class="{ 'is-invalid': errors.phone1 }" placeholder="0912-345-678" />
                </div>
                <div v-if="errors.phone1" class="text-danger small mt-1">{{ errors.phone1 }}</div>
                <small class="text-muted d-block mt-1">台灣手機號碼格式如下：09XX-XXX-XXX，無須去掉0</small>
              </div>
            </div>
          </div>

          <!-- 聯絡手機 2 -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">聯絡手機 2</label>
              </div>
              <div class="col-md-9">
                <div class="d-flex gap-2">
                  <select v-model="phoneCode2" class="form-select" style="max-width: 110px;">
                    <option value="+886">+886</option>
                    <option value="+86">+86</option>
                    <option value="+81">+81</option>
                    <option value="+1">+1</option>
                  </select>
                  <input v-model="phone2" type="tel" class="form-control" placeholder="0912-345-678" />
                </div>
                <small class="text-danger d-block mt-1">
                  建議您多留一支可以聯繫到您的電話號碼，以便客服人員盡快聯絡您。請勿留國際電話，若您未接到客服人員電話，則預約無法完成，敬請撥打客服中心聯繫。
                </small>
              </div>
            </div>
          </div>

          <!-- 其他電話 -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">其他電話</label>
              </div>
              <div class="col-md-9">
                <input v-model="otherPhone" type="tel" class="form-control" placeholder="02-2000-0000" />
                <small class="text-muted d-block mt-1">通話電話請填寫碼：02-2000-0000</small>
              </div>
            </div>
          </div>
        </div>

        <!-- 注意事項 -->
        <div class="alert alert-warning border-0 rounded-3 mt-4">
          <h6 class="fw-bold"><i class="fa-solid fa-triangle-exclamation me-1"></i>注意事項</h6>
          <p class="mb-2 small">
            <span class="text-danger fw-bold">1. 網路預約一定必須接收到預約成功簡訊及確認接送預約的確認簡訊，才算真正完成確認接送服務。</span>
          </p>
          <p class="mb-0 small text-muted">
            2. 在您填寫完成網路預約後2小時內，將會收到預約完成簡訊，我們將在出車前電話確認注意事項。若在2小時內無法收到確認簡訊，請直接撥國客服中心0800-222-568，台灣以外地區請撥打+886-2-8165-8700聯繫。
          </p>
        </div>

        <!-- 下一步按鈕 -->
        <div class="text-center mt-4 pt-3 border-top">
          <button class="btn btn-primary btn-lg px-5 rounded-pill" @click="goNext">
            下一步 <i class="fa-solid fa-arrow-right ms-1"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@media (max-width: 768px) {
  .row.align-items-center .col-md-3,
  .row.align-items-start .col-md-3 {
    margin-bottom: 0.25rem;
  }
}
</style>
