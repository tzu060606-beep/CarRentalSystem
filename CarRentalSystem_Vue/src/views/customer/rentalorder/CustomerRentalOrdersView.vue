<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { customerRentalOrderAPI } from '@/api/rentalorder/customerRentalOrderAPI';
import StatusBadge from '@/components/common/StatusBadge.vue';

const router = useRouter();
const orders = ref([]);
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

const orderTypeText = (order) => {
  if (order.orderType) return order.orderType;
  return order.orderTypeCode === 'LONG_TERM' ? '長租' : '日租';
};

const statusText = (text, code) => {
  return text || code || '-';
};

//1. 新增通用的「剝殼機」，確保能拿到純字串
const getVal = (obj) => {
  if (!obj) return "";
  // 如果是物件，優先抓代碼；如果是字串，直接回傳
  const val = typeof obj === "object" ? obj.dbCode || obj.name || "" : obj;
  return String(val).trim();
};

// 2. 修改各個翻譯函式，先用剝殼機取得正確字串
const getPayStatus = (statusObj) => {
  const status = getVal(statusObj);
  const map = {
    UNPAID: "未付",
    DEPOSIT_PAID: "已付訂金",
    PAID: "已結清",
    FULLY_PAID: "已結清",
    REFUNDING: "退款中",
    REFUNDED: "已退款",
  };
  return map[status] || status || "未指定";
};

const getOrderStatus = (statusObj) => {
  const status = getVal(statusObj);
  const map = {
    RESERVED: "已預約",
    PICKED_UP: "已取車",
    RETURNED: "已歸還(待檢查)",
    OVERDUE: "逾期未還",
    CLOSED: "已結案",
    CANCELLED: "已取消",
  };
  return map[status] || status || "未指定";
};

const rentalOrderStatusBadgeMap = {
  已預約: { label: "已預約", variant: "primary" },
  已取車: { label: "已取車", variant: "warning" },
  待檢查: { label: "待檢查", variant: "info" },
  "已歸還(待檢查)": { label: "已歸還(待檢查)", variant: "info" },
  逾期未還: { label: "逾期未還", variant: "danger" },
  已結案: { label: "已結案", variant: "success" },
  已取消: { label: "已取消", variant: "danger" },
  未指定: { label: "未指定", variant: "danger" },
};

const rentalPayStatusBadgeMap = {
  未付: { label: "未付", variant: "danger" },
  未付款: { label: "未付款", variant: "danger" },
  已付訂金: { label: "已付訂金", variant: "warning" },
  已結清: { label: "已結清", variant: "success" },
  支付完成: { label: "支付完成", variant: "success" },
  退款中: { label: "退款中", variant: "warning" },
  已退款: { label: "已退款", variant: "info" },
  未指定: { label: "未指定", variant: "secondary" },
};

const sortedOrders = computed(() => {
  return [...orders.value].sort((a, b) => {
    return new Date(b.orderTime || 0).getTime() - new Date(a.orderTime || 0).getTime();
  });
});

const loadOrders = async () => {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    localStorage.setItem('system_context', 'customer');
    const response = await customerRentalOrderAPI.getMyOrders();
    orders.value = Array.isArray(response.data) ? response.data : response.data?.data || [];
  } catch (error) {
    console.error('Failed to load customer rental orders', error?.response?.data || error);
    errorMessage.value = error?.response?.data?.error || '無法取得您的租車訂單，請稍後再試。';
  } finally {
    isLoading.value = false;
  }
};

const goToDetail = (orderId) => {
  router.push(`/customer/member/order/${orderId}`);
};

onMounted(loadOrders);
</script>

<template>
  <main class="rental-customer-orders-page min-vh-100">
    <div class="container-fluid px-4 px-xl-5">
      <div class="page-heading d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
        <div>
          <h1 class="h3 fw-bold text-primary mb-2">我的租車訂單</h1>
          <p class="text-secondary mb-0">查看您所有日租與長租訂單的預約、付款與取還車狀態。</p>
        </div>
        <button class="btn btn-outline-primary fw-bold" type="button" @click="loadOrders">
          <i class="fa-solid fa-rotate-right me-2"></i>
          重新整理
        </button>
      </div>

      <div v-if="errorMessage" class="alert alert-warning border-warning-subtle rounded-3">
        <i class="fa-solid fa-circle-exclamation me-2"></i>
        {{ errorMessage }}
      </div>

      <section class="rental-orders-panel bg-white border rounded-3 shadow-sm">
        <div v-if="isLoading" class="p-5 text-center text-secondary">
          <span class="spinner-border spinner-border-sm text-primary me-2"></span>
          正在載入租車訂單...
        </div>

        <div v-else-if="sortedOrders.length === 0" class="empty-state text-center p-5">
          <i class="fa-solid fa-file-circle-xmark display-5 text-secondary mb-3"></i>
          <h2 class="h5 fw-bold mb-2">目前沒有租車訂單</h2>
          <p class="text-secondary mb-4">完成預約後，訂單會出現在這裡。</p>
          <button class="btn btn-primary fw-bold" type="button" @click="router.push('/orders/custbooking')">
            前往選車
          </button>
        </div>

        <div v-else class="rental-table-responsive">
          <rental-table class="rental-table align-middle mb-0">
            <thead>
              <tr>
                <th>訂單日期</th>
                <th>車款</th>
                <th>訂單類型</th>
                <th>取車時間</th>
                <th>還車時間</th>
                <th>取車地點</th>
                <th>訂單狀態</th>
                <th>付款狀態</th>
                <th class="text-end">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in sortedOrders" :key="order.orderId">
                <td class="text-nowrap">{{ formatDateTime(order.orderTime) }}</td>
                <td>
                  <div class="fw-bold">{{ order.vehicleName || '-' }}</div>
                  <div v-if="order.plateNo" class="small text-secondary">車牌 {{ order.plateNo }}</div>
                </td>
                <td>{{ orderTypeText(order) }}</td>
                <td class="text-nowrap">{{ formatDateTime(order.pickupTime) }}</td>
                <td class="text-nowrap">{{ formatDateTime(order.returnTime) }}</td>
                <td>{{ order.pickupLocationName || '-' }}</td>
                <td>
                  <StatusBadge
                    :status="getOrderStatus(statusText(order.orderStatus, order.orderStatusCode))"
                    :map="rentalOrderStatusBadgeMap"
                  />
                </td>
                <td>
                  <StatusBadge
                    :status="getPayStatus(statusText(order.payStatus, order.payStatusCode))"
                    :map="rentalPayStatusBadgeMap"
                  />
                </td>
                <td class="text-end">
                  <button class="btn btn-primary btn-sm fw-bold text-nowrap" type="button" @click="goToDetail(order.orderId)">
                    查看詳情
                  </button>
                </td>
              </tr>
            </tbody>
          </rental-table>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.rental-customer-orders-page {
  padding-top: calc(92px + 3rem);
  padding-bottom: 4rem;
  background: var(--color-bg-page);
}

.rental-orders-panel {
  max-width: 100%;
  overflow: hidden;
}

.rental-orders-panel .rental-table-responsive {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.rental-orders-panel .rental-table {
  width: 100%;
  min-width: 920px;
}

.rental-table th {
  background: #f5f9ff;
  color: #0057b8;
  font-weight: 700;
  white-space: nowrap;
  padding: 1rem;
}

.rental-table td {
  padding: 1rem;
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
  white-space: nowrap;
}

.rental-pay-pill {
  background: #eef8f0;
  color: #16803a;
}

@media (max-width: 991.98px) {
  .rental-customer-orders-page {
    padding-top: calc(76px + 2rem);
  }

  .rental-customer-orders-page .container-fluid {
    padding-left: 1rem;
    padding-right: 1rem;
  }
}

@media (max-width: 767.98px) {
  .rental-table th,
  .rental-table td {
    padding: 0.85rem;
  }
}
</style>


