<script setup>
import { computed } from 'vue';
// defineProps 是 Vue 用來接收「父元件（外層頁面）」傳進來的資料的語法。
// 我們把接收到的資料存進 props 這個變數裡。
const props = defineProps({
  orderSummary: {
    type: Object,
    required: true
  },
  isLoading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['reselect']);
//在畫面上，有一個或「重新選擇車輛」的按鈕。
// 當使用者點擊該按鈕時，程式碼會執行 emit('reselect')，
// 這時父元件就會聽到廣播，進而把畫面切換回上一步。


// ---------------------------
// 金錢格式化工具
const formatCurrency = (value) => {
  const amount = Number(value || 0);
  return `NT$ ${amount.toLocaleString()}`;
};

/*
.toLocaleString()：
這是 JavaScript 內建的超好用方法，它會自動幫數字加上「千分位逗號」（例如把 4000 變成 4,000）。
在 HTML 模板中，可以寫 {{ formatCurrency(order.rentalFee) }}，畫面上會顯示 NT$ 4,000。
*/

const vehicle = computed(() => props.orderSummary.vehicle || {});
const vehicleName = computed(() => vehicle.value.name || [vehicle.value.brand, vehicle.value.model].filter(Boolean).join(' '));
/*

.filter(Boolean)：它的作用是「過濾掉陣列裡面所有空的、null、undefined 的髒東西」。假設只有型號沒有品牌，它會把空的品牌濾掉。
.join(' ')：最後，把陣列裡剩下的字串，用「空白鍵 (' ')」拼接起來

*/

</script>

<template>
  <aside>
    <div class="card rounded-3 shadow-sm border overflow-hidden sticky-top">
      <div v-if="isLoading" class="card-body p-4 text-center" role="status">
        <div class="spinner-border text-primary" aria-hidden="true"></div>
        <p class="text-secondary fw-bold mt-3 mb-0">正在載入訂單明細...</p>
      </div>

      <template v-else>
        <div v-if="vehicle.image" class="ratio ratio-16x9 bg-light">
          <img
            :src="vehicle.image"
            class="w-100 h-100 object-fit-cover"
            :alt="vehicle.alt || vehicleName"
          >
        </div>

        <div class="card-body p-4">
          <h2 class="h4 fw-bold text-primary mb-4">{{ vehicleName || '尚未選擇車輛' }}</h2>

          <button class="btn btn-outline-primary w-100 mb-4 fw-bold" type="button" @click="emit('reselect')">
            <i class="fa-solid fa-arrow-left me-2"></i>
            重新選擇車輛
          </button>

          <div v-if="!vehicleName" class="alert alert-warning" role="alert">
            請先完成車輛選擇，再確認訂單內容。
          </div>

          <div class="d-grid gap-3">
            <div v-if="orderSummary.vehicleTypeText" class="d-flex align-items-start gap-3">
              <i class="fa-solid fa-car-side text-primary mt-1"></i>
              <div>
                <p class="small fw-bold text-secondary mb-1">車型</p>
                <p class="fw-semibold mb-0">{{ orderSummary.vehicleTypeText }}</p>
              </div>
            </div>

            <div v-if="orderSummary.rentalPeriodText" class="d-flex align-items-start gap-3">
              <i class="fa-solid fa-calendar-days text-primary mt-1"></i>
              <div>
                <p class="small fw-bold text-secondary mb-1">租用期間</p>
                <p class="fw-semibold mb-0">{{ orderSummary.rentalPeriodText }}</p>
                <p v-if="orderSummary.durationText" class="small text-primary mb-0 mt-1">
                  {{ orderSummary.durationText }}
                </p>
              </div>
            </div>

            <div v-if="orderSummary.pickupLocationText" class="d-flex align-items-start gap-3">
              <i class="fa-solid fa-location-dot text-primary mt-1"></i>
              <div>
                <p class="small fw-bold text-secondary mb-1">取車地點</p>
                <p class="fw-semibold mb-0">{{ orderSummary.pickupLocationText }}</p>
              </div>
            </div>

            <div v-if="orderSummary.returnLocationText" class="d-flex align-items-start gap-3">
              <i class="fa-solid fa-flag-checkered text-primary mt-1"></i>
              <div>
                <p class="small fw-bold text-secondary mb-1">還車地點</p>
                <p class="fw-semibold mb-0">{{ orderSummary.returnLocationText }}</p>
              </div>
            </div>
          </div>

          <div v-if="orderSummary.priceItems.length > 0" class="border-top mt-4 pt-4 d-grid gap-2">
            <div
              v-for="item in orderSummary.priceItems"
              :key="item.id || item.label"
              class="d-flex justify-content-between align-items-center"
            >
              <span class="text-secondary">{{ item.label }}</span>
              <span class="fw-semibold" :class="Number(item.amount) < 0 ? 'text-danger' : 'text-dark'">
                {{ formatCurrency(item.amount) }}
              </span>
            </div>
          </div>

          <div class="border-top mt-4 pt-4">
            <div class="d-flex justify-content-between align-items-end gap-3">
              <div>
                <p class="h4 fw-bold text-primary mb-1">應付總額</p>
                <p v-if="orderSummary.includedTaxText" class="small text-secondary mb-0">
                  {{ orderSummary.includedTaxText }}
                </p>
              </div>
              <p class="h2 fw-bold text-primary mb-0 text-end">{{ formatCurrency(orderSummary.totalAmount) }}</p>
            </div>

            <div v-if="orderSummary.refundableDeposit > 0" class="bg-light border rounded-3 p-3 mt-3 d-flex justify-content-between align-items-center">
              <span class="small fw-bold text-secondary">應付押金</span>
              <span class="fw-bold text-primary">{{ formatCurrency(orderSummary.refundableDeposit) }}</span>
            </div>
          </div>

          <div v-if="orderSummary.trustMarkers.length > 0" class="row g-2 border-top mt-4 pt-3">
            <div v-for="marker in orderSummary.trustMarkers" :key="marker.id || marker.label" class="col-6">
              <div class="small text-secondary d-flex align-items-center gap-2">
                <i :class="marker.icon || 'fa-solid fa-circle-check'"></i>
                <span>{{ marker.label }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </aside>
</template>

<style scoped>
.sticky-top {
  top: 112px;
  z-index: 1;
}

@media (max-width: 991.98px) {
  .sticky-top {
    top: 96px;
  }
}
</style>
