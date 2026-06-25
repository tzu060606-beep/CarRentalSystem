<script setup>
import { computed } from 'vue';

const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  },
  isLoading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue']);

const createFieldModel = (field) => computed({
  get: () => props.modelValue[field] || '',
  set: (value) => {
    emit('update:modelValue', {
      ...props.modelValue,
      [field]: value
    });
  }
});

const fullName = createFieldModel('fullName');
const phoneNumber = createFieldModel('phoneNumber');
const email = createFieldModel('email');
const licenseNumber = createFieldModel('licenseNumber');
const orderRemark = createFieldModel('orderRemark');
</script>

<template>
  <section class="card rounded-3 shadow-sm border">
    <div class="card-body p-4">
      <h2 class="h4 fw-bold text-primary mb-4 d-flex align-items-center gap-2">
        <i class="fa-solid fa-user"></i>
        承租人資訊
      </h2>

      <div v-if="isLoading" class="alert alert-light border d-flex align-items-center gap-2" role="status">
        <span class="spinner-border spinner-border-sm text-primary" aria-hidden="true"></span>
        <span>正在載入會員資料...</span>
      </div>

      <div class="row g-3">
        <div class="col-md-6">
          <label class="form-label small fw-bold text-secondary" for="driverFullName">姓名</label>
          <input
            id="driverFullName"
            v-model="fullName"
            class="form-control rounded-3"
            type="text"
            autocomplete="name"
            placeholder="請輸入姓名"
            readonly
          >
        </div>

        <div class="col-md-6">
          <label class="form-label small fw-bold text-secondary" for="driverPhoneNumber">手機號碼</label>
          <input
            id="driverPhoneNumber"
            v-model="phoneNumber"
            class="form-control rounded-3"
            type="tel"
            autocomplete="tel"
            placeholder="請輸入手機號碼"
            readonly
          >
        </div>

        <div class="col-md-6">
          <label class="form-label small fw-bold text-secondary" for="driverEmail">Email</label>
          <input
            id="driverEmail"
            v-model="email"
            class="form-control rounded-3"
            type="email"
            autocomplete="email"
            placeholder="member@example.com"
            readonly
          >
        </div>

        <div class="col-md-6">
          <label class="form-label small fw-bold text-secondary" for="driverLicenseNumber">駕照號碼</label>
          <input
            id="driverLicenseNumber"
            v-model="licenseNumber"
            class="form-control rounded-3"
            type="text"
            autocomplete="off"
            placeholder="請輸入駕照號碼"
            readonly
          >
        </div>

        <div class="col-12">
          <label class="form-label small fw-bold text-secondary" for="orderRemark">訂單備註（選填）</label>
          <textarea
            id="orderRemark"
            v-model="orderRemark"
            class="form-control rounded-3"
            rows="3"
            maxlength="255"
            placeholder="可填寫取還車需求或其他備註，最多 255 字"
          ></textarea>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
</style>
