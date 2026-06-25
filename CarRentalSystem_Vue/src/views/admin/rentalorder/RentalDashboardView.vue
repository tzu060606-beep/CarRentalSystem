<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { rentalOrderAPI } from "@/api/rentalorder/rentalorderAPI";

const router = useRouter();
const orders = ref([]);
const isLoading = ref(false);
const errorMessage = ref("");

const getVal = (obj) => {
  if (!obj) return "";
  const val = typeof obj === "object" ? obj.dbCode || obj.name || obj.value || "" : obj;
  return String(val).trim();
};

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

const getOrderTypeText = (orderType) => {
  return ["SHORT_TERM", "日租", "日租 (SHORT_TERM)"].includes(getVal(orderType))
    ? "日租"
    : "長租";
};

const formatCurrency = (value) => {
  const amount = Number(value || 0);
  return `NT$ ${amount.toLocaleString()}`;
};

const formatDateTime = (value) => {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
};

const toDateValue = (value) => {
  if (!value) return "";
  return String(value).slice(0, 10);
};

const todayValue = () => {
  const date = new Date();
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

const isToday = (value) => toDateValue(value) === todayValue();

const isBeforeNow = (value) => {
  if (!value) return false;
  const date = new Date(String(value).replace(" ", "T"));
  return !Number.isNaN(date.getTime()) && date.getTime() < Date.now();
};

const getOrderAmount = (order) => {
  const totalAmount = Number(order.totalAmount || 0);
  if (totalAmount > 0) return totalAmount;
  return Number(order.rentalFee || 0) + Number(order.extraFee || 0) + Number(order.deposit || 0);
};

const getVehicleText = (order) => {
  const modelName = order.vehicle?.carModel?.modelName || "-";
  const plateNo = order.vehicle?.plateNo;
  return plateNo ? `${modelName} (${plateNo})` : modelName;
};

const loadOrders = async () => {
  isLoading.value = true;
  errorMessage.value = "";

  try {
    const response = await rentalOrderAPI.getAll();
    orders.value = Array.isArray(response.data) ? response.data : response.data?.data || [];
  } catch (error) {
    console.error("租訂 Dashboard 載入失敗", error);
    errorMessage.value = "無法取得租車訂單資料，請稍後再試。";
  } finally {
    isLoading.value = false;
  }
};

const todayPickupOrders = computed(() =>
  orders.value.filter((order) => isToday(order.pickupTime))
);

const todayReturnOrders = computed(() =>
  orders.value.filter((order) => isToday(order.returnTime))
);

const activeOrders = computed(() =>
  orders.value.filter((order) => ["PICKED_UP", "OVERDUE", "已取車", "逾期未還"].includes(getVal(order.orderStatus)))
);

const pendingOrders = computed(() =>
  orders.value.filter((order) => ["RESERVED", "RETURNED", "OVERDUE", "已預約", "待檢查", "已歸還(待檢查)", "逾期未還"].includes(getVal(order.orderStatus)))
);

const depositPaidOrders = computed(() =>
  orders.value.filter((order) => ["DEPOSIT_PAID", "已付訂金"].includes(getVal(order.payStatus)))
);

const monthRevenue = computed(() => {
  const currentMonth = todayValue().slice(0, 7);
  return orders.value
    .filter((order) => toDateValue(order.orderTime || order.pickupTime).startsWith(currentMonth))
    .filter((order) => !["CANCELLED", "已取消"].includes(getVal(order.orderStatus)))
    .reduce((sum, order) => sum + getOrderAmount(order), 0);
});

const overdueOrders = computed(() =>
  orders.value.filter((order) => {
    const status = getVal(order.orderStatus);
    if (["OVERDUE", "逾期未還"].includes(status)) return true;
    return ["PICKED_UP", "已取車"].includes(status) && isBeforeNow(order.returnTime);
  })
);

const returnedOrders = computed(() =>
  orders.value.filter((order) => ["RETURNED", "待檢查", "已歸還(待檢查)"].includes(getVal(order.orderStatus)))
);

const recentOrders = computed(() => {
  return [...orders.value]
    .sort((a, b) => String(b.orderTime || "").localeCompare(String(a.orderTime || "")))
    .slice(0, 8);
});

const statusSummary = computed(() => {
  const items = [
    { key: "RESERVED", label: "已預約" },
    { key: "PICKED_UP", label: "已取車" },
    { key: "RETURNED", label: "已歸還(待檢查)" },
    { key: "OVERDUE", label: "逾期未還" },
    { key: "CLOSED", label: "已結案" },
    { key: "CANCELLED", label: "已取消" },
  ];

  return items.map((item) => ({
    ...item,
    count: orders.value.filter((order) => getOrderStatus(order.orderStatus) === item.label || getVal(order.orderStatus) === item.key).length,
  }));
});

const payStatusSummary = computed(() => {
  const items = [
    { key: "UNPAID", label: "未付" },
    { key: "DEPOSIT_PAID", label: "已付訂金" },
    { key: "PAID", label: "已結清" },
    { key: "REFUNDING", label: "退款中" },
    { key: "REFUNDED", label: "已退款" },
  ];

  return items.map((item) => ({
    ...item,
    count: orders.value.filter((order) => getPayStatus(order.payStatus) === item.label || getVal(order.payStatus) === item.key).length,
  }));
});

const dashboardCards = computed(() => [
  {
    title: "今日取車",
    value: todayPickupOrders.value.length,
    icon: "fa-calendar-check",
    note: "預計今日取車",
    className: "text-primary bg-primary-subtle",
  },
  {
    title: "今日還車",
    value: todayReturnOrders.value.length,
    icon: "fa-calendar-day",
    note: "預計今日還車",
    className: "text-info bg-info-subtle",
  },
  {
    title: "進行中訂單",
    value: activeOrders.value.length,
    icon: "fa-car-side",
    note: "已取車或逾期",
    className: "text-warning bg-warning-subtle",
  },
  {
    title: "待處理訂單",
    value: pendingOrders.value.length,
    icon: "fa-list-check",
    note: "預約、已歸還(待檢查)、逾期",
    className: "text-danger bg-danger-subtle",
  },
  {
    title: "已付訂金",
    value: depositPaidOrders.value.length,
    icon: "fa-wallet",
    note: "尚待尾款結清",
    className: "text-success bg-success-subtle",
  },
  {
    title: "本月租車金額",
    value: formatCurrency(monthRevenue.value),
    icon: "fa-sack-dollar",
    note: "不含已取消訂單",
    className: "text-secondary bg-secondary-subtle",
  },
]);

const todoItems = computed(() => [
  {
    title: "今日待取車",
    count: todayPickupOrders.value.filter((order) => ["RESERVED", "已預約"].includes(getVal(order.orderStatus))).length,
    help: "已預約且預計今日取車",
    icon: "fa-key",
  },
  {
    title: "今日待還車",
    count: todayReturnOrders.value.filter((order) => ["PICKED_UP", "已取車"].includes(getVal(order.orderStatus))).length,
    help: "已取車且預計今日還車",
    icon: "fa-flag-checkered",
  },
  {
    title: "已歸還(待檢查) / 待結案",
    count: returnedOrders.value.length,
    help: "車輛已歸還，待費用結算",
    icon: "fa-clipboard-check",
  },
  {
    title: "逾期未還",
    count: overdueOrders.value.length,
    help: "需要優先追蹤",
    icon: "fa-triangle-exclamation",
  },
]);

const getStatusBadgeClass = (statusObj) => {
  const status = getVal(statusObj);
  if (status === "RESERVED" || status === "已預約") return "badge bg-primary";
  if (status === "PICKED_UP" || status === "已取車") return "badge bg-warning text-dark";
  if (status === "RETURNED" || status === "待檢查" || status === "已歸還(待檢查)") return "badge bg-info text-dark";
  if (status === "OVERDUE" || status === "逾期未還") return "badge bg-danger";
  if (status === "CLOSED" || status === "已結案") return "badge bg-success";
  if (status === "CANCELLED" || status === "已取消") return "badge bg-secondary";
  return "badge bg-light text-dark";
};

const goToOrderAction = (order) => {
  const status = getVal(order.orderStatus);
  if (["RESERVED", "PICKED_UP", "RETURNED", "OVERDUE", "已預約", "已取車", "待檢查", "已歸還(待檢查)", "逾期未還"].includes(status)) {
    router.push(`/orders/process/${order.orderId}`);
    return;
  }
  router.push(`/orders/detail/${order.orderId}`);
};

const getOrderActionLabel = (order) => {
  const status = getVal(order.orderStatus);
  return ["RESERVED", "PICKED_UP", "RETURNED", "OVERDUE", "已預約", "已取車", "待檢查", "已歸還(待檢查)", "逾期未還"].includes(status)
    ? "處理"
    : "詳情";
};

onMounted(loadOrders);
</script>

<template>
  <div class="container-fluid py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
      <div>
        <h2 class="fw-bold mb-1 text-primary">租訂系統總覽</h2>
        <p class="text-secondary mb-0">掌握今日取還車、待結案訂單與付款狀態。</p>
      </div>
      <div class="d-flex flex-wrap gap-2">
        <button class="btn btn-outline-primary" type="button" @click="loadOrders">
          <i class="fa-solid fa-rotate-right me-2"></i>
          重新整理
        </button>
        <button class="btn btn-primary" type="button" @click="router.push('/orders/list')">
          <i class="fa-solid fa-list me-2"></i>
          查看所有訂單
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="alert alert-warning border-warning-subtle rounded-3">
      <i class="fa-solid fa-circle-exclamation me-2"></i>
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="card border-0 shadow-sm">
      <div class="card-body p-5 text-center text-secondary">
        <span class="spinner-border spinner-border-sm text-primary me-2"></span>
        正在載入租訂資料...
      </div>
    </div>

    <template v-else>
      <div class="row g-3 mb-4">
        <div v-for="card in dashboardCards" :key="card.title" class="col-12 col-sm-6 col-xl-2">
          <div class="card h-100 border-0 shadow-sm">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-start mb-3">
                <span class="text-secondary fw-bold small">{{ card.title }}</span>
                <span class="rounded-circle d-inline-flex align-items-center justify-content-center" :class="card.className" style="width: 36px; height: 36px;">
                  <i class="fa-solid" :class="card.icon"></i>
                </span>
              </div>
              <div class="h3 fw-bold mb-1">{{ card.value }}</div>
              <div class="small text-secondary">{{ card.note }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="row g-4 mb-4">
        <div class="col-12 col-xl-8">
          <section class="card border-0 shadow-sm h-100">
            <div class="card-header bg-white d-flex justify-content-between align-items-center">
              <h3 class="h5 fw-bold mb-0">最近訂單</h3>
              <button class="btn btn-link text-decoration-none fw-bold" type="button" @click="router.push('/orders/list')">
                查看全部
              </button>
            </div>
            <div class="table-responsive">
              <table class="table align-middle mb-0">
                <thead class="table-light">
                  <tr>
                    <th>訂單編號</th>
                    <th>客戶</th>
                    <th>車款 / 車牌</th>
                    <th>取車時間</th>
                    <th>狀態</th>
                    <th class="text-end">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="recentOrders.length === 0">
                    <td colspan="6" class="text-center text-secondary py-5">目前沒有租車訂單</td>
                  </tr>
                  <tr v-for="order in recentOrders" :key="order.orderId">
                    <td class="fw-bold text-primary">#{{ order.orderId }}</td>
                    <td>{{ order.customer?.custName || "-" }}</td>
                    <td>
                      <div class="fw-semibold">{{ getVehicleText(order) }}</div>
                      <div class="small text-secondary">{{ getOrderTypeText(order.orderType) }}</div>
                    </td>
                    <td class="text-nowrap">{{ formatDateTime(order.pickupTime) }}</td>
                    <td>
                      <span :class="getStatusBadgeClass(order.orderStatus)">
                        {{ getOrderStatus(order.orderStatus) }}
                      </span>
                    </td>
                    <td class="text-end">
                      <button class="btn btn-sm btn-outline-primary fw-bold" type="button" @click="goToOrderAction(order)">
                        {{ getOrderActionLabel(order) }}
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <div class="col-12 col-xl-4">
          <section class="card border-0 shadow-sm h-100">
            <div class="card-header bg-white">
              <h3 class="h5 fw-bold mb-0">待辦事項</h3>
            </div>
            <div class="list-group list-group-flush">
              <div v-for="todo in todoItems" :key="todo.title" class="list-group-item py-3">
                <div class="d-flex align-items-start gap-3">
                  <span class="rounded-circle bg-primary-subtle text-primary d-inline-flex align-items-center justify-content-center flex-shrink-0" style="width: 36px; height: 36px;">
                    <i class="fa-solid" :class="todo.icon"></i>
                  </span>
                  <div class="flex-grow-1">
                    <div class="d-flex justify-content-between gap-3">
                      <div class="fw-bold">{{ todo.title }}</div>
                      <div class="fw-bold text-primary">{{ todo.count }}</div>
                    </div>
                    <div class="small text-secondary">{{ todo.help }}</div>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <div class="row g-4">
        <div class="col-12 col-xl-6">
          <section class="card border-0 shadow-sm h-100">
            <div class="card-header bg-white">
              <h3 class="h5 fw-bold mb-0">訂單狀態分布</h3>
            </div>
            <div class="card-body">
              <div v-for="item in statusSummary" :key="item.key" class="mb-3">
                <div class="d-flex justify-content-between mb-1">
                  <span class="fw-semibold">{{ item.label }}</span>
                  <span class="text-secondary">{{ item.count }}</span>
                </div>
                <div class="progress" style="height: 8px;">
                  <div
                    class="progress-bar"
                    role="progressbar"
                    :style="{ width: orders.length ? `${(item.count / orders.length) * 100}%` : '0%' }"
                    :aria-valuenow="item.count"
                    aria-valuemin="0"
                    :aria-valuemax="orders.length"
                  ></div>
                </div>
              </div>
            </div>
          </section>
        </div>

        <div class="col-12 col-xl-6">
          <section class="card border-0 shadow-sm h-100">
            <div class="card-header bg-white d-flex justify-content-between align-items-center">
              <h3 class="h5 fw-bold mb-0">付款狀態分布</h3>
              <button class="btn btn-sm btn-outline-primary" type="button" @click="router.push('/plans')">
                方案管理
              </button>
            </div>
            <div class="card-body">
              <div v-for="item in payStatusSummary" :key="item.key" class="mb-3">
                <div class="d-flex justify-content-between mb-1">
                  <span class="fw-semibold">{{ item.label }}</span>
                  <span class="text-secondary">{{ item.count }}</span>
                </div>
                <div class="progress" style="height: 8px;">
                  <div
                    class="progress-bar bg-success"
                    role="progressbar"
                    :style="{ width: orders.length ? `${(item.count / orders.length) * 100}%` : '0%' }"
                    :aria-valuenow="item.count"
                    aria-valuemin="0"
                    :aria-valuemax="orders.length"
                  ></div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </template>
  </div>
</template>
