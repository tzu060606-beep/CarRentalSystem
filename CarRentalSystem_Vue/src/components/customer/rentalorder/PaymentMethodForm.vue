<script setup>
import { computed } from 'vue';

const props = defineProps({
  paymentOptions: {
    type: Array,
    default: () => []
  },
  selectedPaymentMethod: {
    type: [String, Number],
    default: ''
  },
  cardInfo: {
    type: Object,
    required: true
  },
  showCardForm: {
    type: Boolean,
    default: false
  },
  isLoading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  'update:selectedPaymentMethod',
  'update:cardInfo'
]);

const selectedPaymentModel = computed({
  get: () => props.selectedPaymentMethod,
  set: (value) => emit('update:selectedPaymentMethod', value)
});

const createCardFieldModel = (field) => computed({
  get: () => props.cardInfo[field] || '',
  set: (value) => {
    emit('update:cardInfo', {
      ...props.cardInfo,
      [field]: value
    });
  }
});

const cardNumber = createCardFieldModel('cardNumber');
const expiryDate = createCardFieldModel('expiryDate');
const cvc = createCardFieldModel('cvc');
</script>

<template>
  <section class="card rounded-3 shadow-sm border">
    <div class="card-body p-4">
      <h2 class="h4 fw-bold text-primary mb-4 d-flex align-items-center gap-2">
        <i class="fa-solid fa-wallet"></i>
        押金付款方式
      </h2>

      <div v-if="isLoading" class="alert alert-light border d-flex align-items-center gap-2" role="status">
        <span class="spinner-border spinner-border-sm text-primary" aria-hidden="true"></span>
        <span>正在載入押金付款方式...</span>
      </div>

      <div v-else-if="paymentOptions.length === 0" class="alert alert-warning mb-0" role="alert">
        目前沒有可用的押金付款方式，請稍後再試。
      </div>

      <div v-else class="row g-3">
        <div v-for="option in paymentOptions" :key="option.id" class="col-6 col-md-3">
          <input
            :id="`payment-${option.id}`"
            class="btn-check"
            name="paymentMethod"
            type="radio"
            :value="option.id"
            v-model="selectedPaymentModel"
            :disabled="option.disabled"
          >
          <label
            class="btn w-100 h-100 rounded-3 border py-3 d-flex flex-column align-items-center justify-content-center gap-2"
            :class="selectedPaymentMethod === option.id ? 'btn-primary' : 'btn-light text-secondary'"
            :for="`payment-${option.id}`"
          >
            <i :class="[option.icon, 'fs-3']"></i>
            <span class="small fw-bold">{{ option.label }}</span>
          </label>
        </div>
      </div>

      <div v-if="showCardForm" class="border-top mt-4 pt-4">
        <div class="mb-3">
          <label class="form-label small fw-bold text-secondary" for="cardNumber">信用卡號</label>
          <div class="input-group">
            <input
              id="cardNumber"
              class="form-control rounded-start-3"
              type="text"
              inputmode="numeric"
              autocomplete="cc-number"
              placeholder="0000 0000 0000 0000"
              v-model="cardNumber"
            >
            <span class="input-group-text rounded-end-3">
              <i class="fa-solid fa-lock text-secondary"></i>
            </span>
          </div>
        </div>

        <div class="row g-3">
          <div class="col-6">
            <label class="form-label small fw-bold text-secondary" for="cardExpiryDate">有效期限</label>
            <input
              id="cardExpiryDate"
              class="form-control rounded-3"
              type="text"
              inputmode="numeric"
              autocomplete="cc-exp"
              placeholder="月 / 年"
              v-model="expiryDate"
            >
          </div>

          <div class="col-6">
            <label class="form-label small fw-bold text-secondary" for="cardCvc">安全碼</label>
            <input
              id="cardCvc"
              class="form-control rounded-3"
              type="text"
              inputmode="numeric"
              autocomplete="cc-csc"
              placeholder="123"
              v-model="cvc"
            >
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
</style>
