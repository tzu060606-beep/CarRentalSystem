<script setup>
import { computed, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import { selectVehicleStore } from '@/store/rentalorder/selectVehicle';
import { useOrderConfirmStore } from '@/store/rentalorder/confirmOrder';
import DriverInfoForm from '@/components/customer/rentalorder/DriverInfoForm.vue';
import PaymentMethodForm from '@/components/customer/rentalorder/PaymentMethodForm.vue';
import OrderSummarySidebar from '@/components/customer/rentalorder/OrderSummarySidebar.vue';

const vehicleStore = selectVehicleStore();
const orderStore = useOrderConfirmStore();
const route = useRoute();
const router = useRouter();

const {
  driverInfo,
  paymentOptions,
  selectedPaymentMethod,
  cardInfo,
  orderPreview,
  isLoadingProfile,
  isLoadingPaymentOptions,
  isLoadingPreview,
  isSubmitting,
  errorMessage,
  successMessage
} = storeToRefs(orderStore);

const progressSteps = [
  { number: 1, label: '選擇車輛', state: 'completed' },
  { number: 2, label: '確認訂單', state: 'active' },
  { number: 3, label: '完成預約', state: 'pending' }
];

const isSubmitDisabled = computed(() => {
  return isSubmitting.value || isLoadingProfile.value || isLoadingPreview.value;
});

const submitEcpayForm = (checkout) => {
  const paymentUrl = checkout?.paymentUrl;
  const formData = checkout?.formData;

  if (!paymentUrl || !formData) {
    throw new Error('Missing ECPay checkout form data');
  }

  const form = document.createElement('form');
  form.method = 'POST';
  form.action = paymentUrl;
  form.style.display = 'none';

  Object.entries(formData).forEach(([name, value]) => {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = String(value ?? '');
    form.appendChild(input);
  });

  document.body.appendChild(form);
  form.submit();
};

const handleSubmit = async () => {
  try {
    if (orderStore.pendingPaymentGatewayRequest) {
      try {
        const checkout = await orderStore.createEcpayCheckout();
        if (checkout) {
          submitEcpayForm(checkout);
          return;
        }
      } catch (error) {
        if (error?.response?.status !== 400) {
          throw error;
        }

        console.warn('Discard stale ECPay checkout request and recreate order', error?.response?.data || error);
        orderStore.clearPendingEcpayRequest();
      }
    }

    const response = await orderStore.submitOrder();
    if (!response) return;

    if (orderStore.pendingPaymentGatewayRequest) {
      const checkout = await orderStore.createEcpayCheckout();
      if (checkout) {
        submitEcpayForm(checkout);
      }
      return;
    }

    router.push('/orders/custsuccess');
  } catch (error) {
    console.error('Submit order failed in view', error?.response?.data || error);
  }
};

const handleReselectVehicle = () => {
  router.push('/orders/custbooking');
};

onMounted(() => {
  vehicleStore.restoreOrderDraft();

  const selectedVehicle = vehicleStore.selectedVehicle || (
    route.query.vehicleId
      ? {
          id: route.query.vehicleId,
          modelId: route.query.modelId || null
        }
      : null
  );

  orderStore.initializeOrderConfirm(selectedVehicle);
});
</script>

<template>
  <main class="bg-light min-vh-100 py-5 rental-front-page">
    <div class="container">
      <div class="d-flex flex-wrap align-items-center justify-content-center gap-3 mb-5">
        <template v-for="(step, index) in progressSteps" :key="step.number">
          <div
            class="d-flex align-items-center gap-2 fw-bold"
            :class="step.state === 'pending' ? 'text-secondary' : 'text-primary'"
          >
            <span
              class="badge rounded-pill"
              :class="step.state === 'pending' ? 'bg-secondary-subtle text-secondary' : 'bg-primary text-white'"
            >
              {{ step.number }}
            </span>
            <span>{{ step.label }}</span>
          </div>
          <div v-if="index < progressSteps.length - 1" class="border-top flex-grow-1 d-none d-md-block"></div>
        </template>
      </div>

      <div v-if="errorMessage" class="alert alert-warning d-flex align-items-center gap-2" role="alert">
        <i class="fa-solid fa-circle-exclamation"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div v-if="successMessage" class="alert alert-success d-flex align-items-center gap-2" role="status">
        <i class="fa-solid fa-circle-check"></i>
        <span>{{ successMessage }}</span>
      </div>

      <div class="row g-4 align-items-start">
        <form class="col-lg-7 d-grid gap-4" @submit.prevent="handleSubmit">
          <DriverInfoForm
            v-model="driverInfo"
            :is-loading="isLoadingProfile"
          />

          <PaymentMethodForm
            v-model:selected-payment-method="selectedPaymentMethod"
            v-model:card-info="cardInfo"
            :payment-options="paymentOptions"
            :show-card-form="orderStore.shouldShowCardForm"
            :is-loading="isLoadingPaymentOptions"
          />

          <button
            class="btn btn-primary btn-lg w-100 fw-bold py-3 shadow-sm d-flex align-items-center justify-content-center gap-2"
            type="submit"
            :disabled="isSubmitDisabled"
          >
            <span v-if="isSubmitting" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            <span>{{ isSubmitting ? '訂單送出中...' : '確認押金付款並送出訂單' }}</span>
            <i v-if="!isSubmitting" class="fa-solid fa-arrow-right"></i>
          </button>
        </form>

        <div class="col-lg-5">
          <OrderSummarySidebar
            :order-summary="orderPreview"
            :is-loading="isLoadingPreview"
            @reselect="handleReselectVehicle"
          />
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.rental-front-page {
  padding-top: calc(92px + 3rem) !important;
}

@media (max-width: 991.98px) {
  .rental-front-page {
    padding-top: calc(76px + 3rem) !important;
  }
}
</style>
