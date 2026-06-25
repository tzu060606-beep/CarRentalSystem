<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { customerRentalOrderAPI } from '@/api/rentalorder/customerRentalOrderAPI';

const route = useRoute();
const router = useRouter();
const order = ref(null);
const isLoading = ref(false);
const errorMessage = ref('');

const formatDateTime = (value) => {
  if (!value) return '-';
  const date = new Date(String(value).replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16);
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date);
};

const formatMoney = (value) => {
  const amount = Number(value || 0);
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0
  }).format(amount);
};

const formatMileage = (value) => {
  if (value === null || value === undefined || value === '') return '-';
  const amount = Number(value || 0);
  return `${amount.toLocaleString()} km`;
};

// 備註文字可能包含後端寫入的 $65900，顯示時一起套用金額格式
const formatTextMoney = (value) => {
  if (!value) return '無';
  return String(value)
    .replace(/\.00/g, '')
    .replace(/\$(\d+)/g, (_, amount) => formatMoney(amount));
};

const orderTypeText = computed(() => {
  if (!order.value) return '-';
  return order.value.orderType || (order.value.orderTypeCode === 'LONG_TERM' ? '長租' : '日租');
});

const loadOrder = async () => {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    localStorage.setItem('system_context', 'customer');
    const response = await customerRentalOrderAPI.getMyOrderDetail(route.params.orderId);
    order.value = response.data?.data || response.data;
  } catch (error) {
    console.error('Failed to load customer rental order detail', error?.response?.data || error);
    errorMessage.value = error?.response?.data?.error || '無法取得訂單詳情，請稍後再試。';
  } finally {
    isLoading.value = false;
  }
};

onMounted(loadOrder);
</script>

<template>
  <main class="rental-customer-order-detail-page bg-light min-vh-100">
    <div class="container px-4">
      <div class="d-flex justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 class="h3 fw-bold text-primary mb-2">租車訂單詳情</h1>
          <p class="text-secondary mb-0">查看預約內容、付款狀態與取還車資訊。</p>
        </div>
        <button class="btn btn-outline-primary fw-bold" type="button" @click="router.push('/customer/member/order')">
          <i class="fa-solid fa-arrow-left me-2"></i>
          返回訂單列表
        </button>
      </div>

      <div v-if="errorMessage" class="alert alert-warning rounded-3">
        <i class="fa-solid fa-circle-exclamation me-2"></i>
        {{ errorMessage }}
      </div>

      <section v-if="isLoading" class="bg-white border rounded-3 shadow-sm p-5 text-center text-secondary">
        <span class="spinner-border spinner-border-sm text-primary me-2"></span>
        正在載入訂單詳情...
      </section>

      <template v-else-if="order">
        <section class="rental-summary-band bg-white border rounded-3 shadow-sm mb-4">
          <div class="row g-0">
            <div class="col-lg-4 rental-vehicle-block">
              <img
                v-if="order.vehicleImageUrl"
                :src="order.vehicleImageUrl"
                :alt="order.vehicleName"
                class="rental-vehicle-image"
              >
              <div v-else class="rental-vehicle-placeholder">
                <i class="fa-solid fa-car-side"></i>
              </div>
            </div>
            <div class="col-lg-8 p-4">
              <div class="d-flex flex-wrap justify-content-between gap-3 mb-3">
                <div>
                  <div class="text-primary fw-bold mb-1">訂單編號 #{{ order.orderId }}</div>
                  <h2 class="h4 fw-bold mb-1">{{ order.vehicleName || '-' }}</h2>
                  <p class="text-secondary mb-0">
                    {{ order.plateNo ? `車牌 ${order.plateNo}` : '車牌未提供' }}
                  </p>
                </div>
                <div class="text-lg-end">
                  <span class="rental-status-pill me-2">{{ order.orderStatus || order.orderStatusCode || '-' }}</span>
                  <span class="rental-pay-pill">{{ order.payStatus || order.payStatusCode || '-' }}</span>
                </div>
              </div>

              <div class="rental-detail-grid">
                <div>
                  <div class="rental-detail-label">訂單日期</div>
                  <div class="rental-detail-value">{{ formatDateTime(order.orderTime) }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">訂單類型</div>
                  <div class="rental-detail-value">{{ orderTypeText }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">車型</div>
                  <div class="rental-detail-value">{{ order.vehicleType || '-' }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">訂金付款方式</div>
                  <div class="rental-detail-value">{{ order.depositPayMethod || '-' }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">尾款付款方式</div>
                  <div class="rental-detail-value">{{ order.balancePayMethod || '-' }}</div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <div class="row g-4">
          <div class="col-lg-7">
            <section class="info-section bg-white border rounded-3 shadow-sm p-4 mb-4">
              <h2 class="h5 fw-bold text-primary mb-4">
                <i class="fa-solid fa-calendar-days me-2"></i>
                取還車資訊
              </h2>
              <div class="rental-detail-grid">
                <div>
                  <div class="rental-detail-label">取車時間</div>
                  <div class="rental-detail-value">{{ formatDateTime(order.pickupTime) }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">還車時間</div>
                  <div class="rental-detail-value">{{ formatDateTime(order.returnTime) }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">取車地點</div>
                  <div class="rental-detail-value">{{ order.pickupLocationName || '-' }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">還車地點</div>
                  <div class="rental-detail-value">{{ order.returnLocationName || '-' }}</div>
                </div>
              </div>
            </section>

            <section class="info-section bg-white border rounded-3 shadow-sm p-4">
              <h2 class="h5 fw-bold text-primary mb-4">
                <i class="fa-solid fa-clipboard-list me-2"></i>
                訂單備註與流程紀錄
              </h2>
              <div class="rental-detail-grid">
                <div>
                  <div class="rental-detail-label">實際取車時間</div>
                  <div class="rental-detail-value">{{ formatDateTime(order.actualPickupTime) }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">實際還車時間</div>
                  <div class="rental-detail-value">{{ formatDateTime(order.actualReturnTime) }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">出車里程</div>
                  <div class="rental-detail-value">{{ formatMileage(order.startMileage) }}</div>
                </div>
                <div>
                  <div class="rental-detail-label">還車里程</div>
                  <div class="rental-detail-value">{{ formatMileage(order.endMileage) }}</div>
                </div>
              </div>
              <hr>
              <div class="rental-detail-label mb-2">備註</div>
              <p class="mb-0 text-secondary">{{ formatTextMoney(order.processNote || order.orderRemark) }}</p>
            </section>
          </div>

          <div class="col-lg-5">
            <section class="info-section bg-white border rounded-3 shadow-sm p-4">
              <h2 class="h5 fw-bold text-primary mb-4">
                <i class="fa-solid fa-wallet me-2"></i>
                金額資訊
              </h2>
              <div class="rental-amount-row">
                <span>租車費用</span>
                <strong>{{ formatMoney(order.rentalFee) }}</strong>
              </div>
              <div class="rental-amount-row">
                <span>其他費用</span>
                <strong>{{ formatMoney(order.extraFee) }}</strong>
              </div>
              <div class="rental-amount-row">
                <span>應付押金</span>
                <strong>{{ formatMoney(order.deposit) }}</strong>
              </div>
              <div class="rental-amount-row">
                <span>剩餘尾款</span>
                <strong>{{ formatMoney(order.remainingBalance) }}</strong>
              </div>
              <hr>
              <div class="rental-amount-total">
                <span>訂單總額</span>
                <strong>{{ formatMoney(order.totalAmount) }}</strong>
              </div>
            </section>
          </div>
        </div>
      </template>
    </div>
  </main>
</template>

<style scoped>
.rental-customer-order-detail-page {
  padding-top: calc(92px + 3rem);
  padding-bottom: 4rem;
}

.rental-summary-band {
  overflow: hidden;
}

.rental-vehicle-block {
  min-height: 260px;
  background: #f5f9ff;
}

.rental-vehicle-image,
.rental-vehicle-placeholder {
  width: 100%;
  height: 100%;
  min-height: 260px;
  object-fit: contain;
}

.rental-vehicle-placeholder {
  display: grid;
  place-items: center;
  color: #8ab6e8;
  font-size: 4rem;
}

.rental-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem 1.5rem;
}

.rental-detail-label {
  color: #67a4ff;
  font-weight: 700;
  margin-bottom: 0.35rem;
}

.rental-detail-value {
  color: #172033;
  font-weight: 700;
}

.rental-status-pill,
.rental-pay-pill {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: #eaf4ff;
  color: #0057b8;
  font-weight: 700;
}

.rental-pay-pill {
  background: #eef8f0;
  color: #16803a;
}

.rental-amount-row,
.rental-amount-total {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 0;
}

.rental-amount-total {
  color: #0057b8;
  font-size: 1.3rem;
  font-weight: 800;
}

@media (max-width: 991.98px) {
  .rental-customer-order-detail-page {
    padding-top: calc(76px + 2rem);
  }
}

@media (max-width: 575.98px) {
  .rental-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>

