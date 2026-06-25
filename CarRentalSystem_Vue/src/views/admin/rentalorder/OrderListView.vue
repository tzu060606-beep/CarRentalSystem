<script setup>
import { ref, onMounted, computed, watch } from "vue";
import { useRouter } from "vue-router";
import { rentalOrderAPI } from "@/api/rentalorder/rentalorderAPI";
import ActionButtons from "@/components/common/ActionButtons.vue";
import BaseModal from "@/components/common/BaseModal.vue";
import ConfirmDialog from "@/components/common/ConfirmDialog.vue";
import EmptyState from "@/components/common/EmptyState.vue";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";
import Pagination from "@/components/common/Pagination.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import Breadcrumb from "@/components/common/Breadcrumb.vue";
const orders = ref([]);
const router = useRouter();
const isLoading = ref(false);

// 前端篩選條件：目前仍使用 getAll() 抓回資料，再在畫面端篩選
const filters = ref({
  keyword: "",
  orderType: "",
  orderStatus: "",
  payStatus: "",
  dateField: "pickupTime",
  dateFrom: "",
  dateTo: "",
});

const sortState = ref({
  key: "orderTime",
  direction: "desc",
});

const pagination = ref({
  page: 1,
  pageSize: 10,
});

// --- Modal 相關狀態與方法 ---
const showModal = ref(false);
const selectedOrder = ref(null);
const showDeleteConfirm = ref(false);
const pendingDeleteOrderId = ref(null);
const pendingDeleteOrderType = ref(null);
const showRefundConfirm = ref(false);
const pendingRefundOrderId = ref(null);

const openDetails = (order) => {
  selectedOrder.value = order;
  showModal.value = true;
  document.body.style.overflow = "hidden"; // 防止背景滾動
};

const closeDetails = () => {
  showModal.value = false;
  selectedOrder.value = null;
  document.body.style.overflow = ""; // 恢復背景滾動
};

// --- 字典翻譯區 ---
const getPlanName = (id) => {
  const plans = {
    1: "日租-小型轎車",
    2: "日租-中型轎車",
    3: "日租-休旅車",
    4: "日租-廂型車",
    5: "日租-電動車",
    6: "長租-小型轎車",
    7: "長租-中型轎車",
    8: "長租-休旅車",
    9: "長租-廂型車",
    10: "長租-電動車",
    11: "其他 (客製化專案)"
  };
  return plans[id] || `未指定(${id})`;
};

const getLocationName = (id) => {
  if (id === 1) return "台北";
  if (id === 2) return "新竹";
  return "未指定";
};

const formatCurrency = (value) => {
  const amount = Number(value || 0);
  return `NT$ ${amount.toLocaleString()}`;
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

const getPayMethod = (methodObj) => {
  const method = getVal(methodObj);
  const map = {
    CREDIT_CARD: "信用卡",
    TRANSFER: "轉帳",
    MOBILE_PAY: "行動支付",
    CASH: "現金",
  };
  return map[method] || method || "未指定";
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
  未付款: { label: "未付款", variant: "danger" },
  已付訂金: { label: "已付訂金", variant: "warning" },
  支付完成: { label: "支付完成", variant: "success" },
  退款中: { label: "退款中", variant: "warning" },
  已退款: { label: "已退款", variant: "info" },
  未指定: { label: "未指定", variant: "secondary" },
};

const getOrderTypeText = (orderType) => {
  return ["SHORT_TERM", "日租", "日租 (SHORT_TERM)"].includes(getVal(orderType))
    ? "日租"
    : "長租";
};


const canProcessOrder = (order) => {
  return [
    "RESERVED",
    "PICKED_UP",
    "RETURNED",
    "OVERDUE",
    "已預約",
    "已取車",
    "待檢查",
    "逾期未還",
    "已歸還(待檢查)",
  ].includes(getVal(order.orderStatus));
};

const canMarkRefunded = (order) => {
  return getOrderStatus(order.orderStatus) === "已取消" && getPayStatus(order.payStatus) === "退款中";
};
const getSearchText = (order) => [
  order.orderId,
  order.customer?.custName,
  order.vehicle?.carModel?.modelName,
  order.vehicle?.plateNo,
  order.rentalPlan?.planName,
  order.pickupLocation?.locationName,
  order.returnLocation?.locationName,
].filter(Boolean).join(" ").toLowerCase();

const toDateValue = (value) => {
  if (!value) return "";
  return String(value).slice(0, 10);
};

const getSortValue = (order, key) => {
  const map = {
    orderId: Number(order.orderId || 0),
    customer: order.customer?.custName || "",
    vehicle: `${order.vehicle?.carModel?.modelName || ""} ${order.vehicle?.plateNo || ""}`,
    orderType: getOrderTypeText(order.orderType),
    plan: order.rentalPlan?.planName || getPlanName(order.rentalPlan?.planId),
    pickupLocation: order.pickupLocation?.locationName || "",
    returnLocation: order.returnLocation?.locationName || "",
    pickupTime: order.pickupTime || "",
    returnTime: order.returnTime || "",
    rentalFee: Number(order.rentalFee || 0),
    extraFee: Number(order.extraFee || 0),
    deposit: Number(order.deposit || 0),
    totalAmount: Number(order.totalAmount || 0),
    payStatus: getPayStatus(order.payStatus),
    depositPayMethod: getPayMethod(order.depositPayMethod),
    balancePayMethod: getPayMethod(order.balancePayMethod),
    orderStatus: getOrderStatus(order.orderStatus),
    orderTime: order.orderTime || "",
  };
  return map[key] ?? "";
};

const setSort = (key) => {
  if (sortState.value.key === key) {
    sortState.value.direction = sortState.value.direction === "asc" ? "desc" : "asc";
    return;
  }
  sortState.value.key = key;
  sortState.value.direction = "asc";
};

const getSortIcon = (key) => {
  if (sortState.value.key !== key) return "↕";
  return sortState.value.direction === "asc" ? "↑" : "↓";
};

const resetFilters = () => {
  filters.value = {
    keyword: "",
    orderType: "",
    orderStatus: "",
    payStatus: "",
    dateField: "pickupTime",
    dateFrom: "",
    dateTo: "",
  };
  sortState.value = {
    key: "orderTime",
    direction: "desc",
  };
  pagination.value.page = 1;
};

const filteredAndSortedOrders = computed(() => {
  const keyword = filters.value.keyword.trim().toLowerCase();
  const dateFrom = filters.value.dateFrom;
  const dateTo = filters.value.dateTo;

  const filtered = orders.value.filter((order) => {
    const matchesKeyword = !keyword || getSearchText(order).includes(keyword);
    const matchesType = !filters.value.orderType || getOrderTypeText(order.orderType) === filters.value.orderType;
    const orderStatusText = getOrderStatus(order.orderStatus);
    const matchesOrderStatus = !filters.value.orderStatus || orderStatusText === filters.value.orderStatus || orderStatusText.includes(filters.value.orderStatus);
    const matchesPayStatus = !filters.value.payStatus || getPayStatus(order.payStatus) === filters.value.payStatus;
    const targetDate = toDateValue(order[filters.value.dateField]);
    const matchesDateFrom = !dateFrom || targetDate >= dateFrom;
    const matchesDateTo = !dateTo || targetDate <= dateTo;

    return matchesKeyword && matchesType && matchesOrderStatus && matchesPayStatus && matchesDateFrom && matchesDateTo;
  });

  return [...filtered].sort((a, b) => {
    const aValue = getSortValue(a, sortState.value.key);
    const bValue = getSortValue(b, sortState.value.key);

    if (typeof aValue === "number" && typeof bValue === "number") {
      return sortState.value.direction === "asc" ? aValue - bValue : bValue - aValue;
    }

    return sortState.value.direction === "asc"
      ? String(aValue).localeCompare(String(bValue), "zh-TW")
      : String(bValue).localeCompare(String(aValue), "zh-TW");
  });
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredAndSortedOrders.value.length / pagination.value.pageSize))
);

const pageStartIndex = computed(() =>
  filteredAndSortedOrders.value.length === 0
    ? 0
    : (pagination.value.page - 1) * pagination.value.pageSize + 1
);

const pageEndIndex = computed(() =>
  Math.min(pagination.value.page * pagination.value.pageSize, filteredAndSortedOrders.value.length)
);

const pagedOrders = computed(() => {
  const start = (pagination.value.page - 1) * pagination.value.pageSize;
  return filteredAndSortedOrders.value.slice(start, start + pagination.value.pageSize);
});

watch(totalPages, (pages) => {
  if (pagination.value.page > pages) {
    pagination.value.page = pages;
  }
});

const goToPage = (page) => {
  pagination.value.page = Math.min(Math.max(page, 1), totalPages.value);
};

const changePageSize = () => {
  pagination.value.page = 1;
};

//3. 動態狀態標籤顏色 (加入剝殼機)
const getStatusBadgeClass = (statusObj) => {
  const s = getVal(statusObj);
  // 同時檢查英文與中文關鍵字
  if (s === "RESERVED" || s === "已預約") return "badge bg-primary";
  if (s === "PICKED_UP" || s === "已取車") return "badge bg-warning text-dark";
  if (s === "RETURNED" || s === "待檢查" || s.includes("待檢查"))
    return "badge bg-info text-dark";
  if (s === "CLOSED" || s === "已結案") return "badge bg-success";
  if (s === "CANCELLED" || s === "已取消") return "badge bg-danger";
  if (s === "OVERDUE" || s === "逾期") return "badge bg-danger";
  return "badge bg-secondary";
};

// --- API 呼叫 ---
const fetchOrders = async () => {
  isLoading.value = true;

  try {
    const response = await rentalOrderAPI.getAll();
    orders.value = response.data;
  } catch (error) {
    console.error("抓取失敗", error);
  } finally {
    isLoading.value = false;
  }
};

const deleteOrder = (id, type) => {
  pendingDeleteOrderId.value = id;
  pendingDeleteOrderType.value = type;
  showDeleteConfirm.value = true;
};

const cancelDeleteOrder = () => {
  showDeleteConfirm.value = false;
  pendingDeleteOrderId.value = null;
  pendingDeleteOrderType.value = null;
};

const confirmDeleteOrder = async () => {
  if (!pendingDeleteOrderId.value) return;

  try {
    await rentalOrderAPI.delete(pendingDeleteOrderId.value);
    alert("刪除成功！");
    fetchOrders();
  } catch (error) {
    console.error("刪除發生錯誤", error);
    alert("刪除發生錯誤");
  } finally {
    cancelDeleteOrder();
  }
};

const requestMarkRefunded = (orderId) => {
  pendingRefundOrderId.value = orderId;
  showRefundConfirm.value = true;
};

const cancelMarkRefunded = () => {
  showRefundConfirm.value = false;
  pendingRefundOrderId.value = null;
};

const confirmMarkRefunded = async () => {
  if (!pendingRefundOrderId.value) return;

  try {
    await rentalOrderAPI.markRefunded(pendingRefundOrderId.value);
    alert("已更新為已退款");
    fetchOrders();
  } catch (error) {
    console.error("更新退款狀態失敗", error);
    alert(error.response?.data?.error || "更新退款狀態失敗");
  } finally {
    cancelMarkRefunded();
  }
};
const goToCreate = () => router.push("/orders/new");
const goToEdit = (id) => router.push(`/orders/edit/${id}`);
const goToProcess = (id) => router.push(`/orders/process/${id}`);
const goToDetail = (id) => router.push(`/orders/detail/${id}`);

// 新增：專門給離開 Modal 時用的函式
const navigateFromModalToProcess = (id) => {
  closeDetails(); // 先關閉 Modal，否則跳頁後背景會卡住黑屏
  goToProcess(id); // 呼叫原本寫好的跳轉函式，前往 RentalProcess.vue
};

const navigateFromModalToDetail = (id) => {
  closeDetails();
  goToDetail(id); 
};

// ---------------------底下是掛載區域--------------
onMounted(fetchOrders);


</script>

<template>
  <div class="container-fluid py-4">
    
    <LoadingSpinner :is-loading="isLoading" overlay text="訂單資料載入中..." />

    <Breadcrumb :items="[
      { label: '首頁', to: '/orders' },
      { label: '租訂系統', to: '/orders' },
      { label: '訂單管理' }
    ]" />
    
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h3 class="mb-0 fw-bold">訂單管理</h3>
      <button class="btn btn-primary px-4" @click="goToCreate">新增訂單</button>
    </div>

    <div class="bg-white border rounded shadow-sm p-3 mb-3">
      <div class="row g-3 align-items-end">
        <div class="col-lg-3 col-md-6">
          <label class="form-label small fw-bold text-secondary">關鍵字</label>
          <SearchBar
            :model-value="filters.keyword"
            :show-button="false"
            placeholder="訂單編號、客戶、車款、車牌"
            @update:model-value="value => { filters.keyword = value.trim(); pagination.page = 1 }"
          />
        </div>
        <div class="col-lg-2 col-md-3">
          <label class="form-label small fw-bold text-secondary">訂單類型</label>
          <select v-model="filters.orderType" class="form-select" @change="pagination.page = 1">
            <option value="">全部</option>
            <option value="日租">日租</option>
            <option value="長租">長租</option>
          </select>
        </div>
        <div class="col-lg-2 col-md-3">
          <label class="form-label small fw-bold text-secondary">訂單狀態</label>
          <select v-model="filters.orderStatus" class="form-select" @change="pagination.page = 1">
            <option value="">全部</option>
            <option value="已預約">已預約</option>
            <option value="已取車">已取車</option>
            <option value="已歸還(待檢查)">已歸還(待檢查)</option>
            <option value="逾期未還">逾期未還</option>
            <option value="已結案">已結案</option>
            <option value="已取消">已取消</option>
          </select>
        </div>
        <div class="col-lg-2 col-md-3">
          <label class="form-label small fw-bold text-secondary">付款狀態</label>
          <select v-model="filters.payStatus" class="form-select" @change="pagination.page = 1">
            <option value="">全部</option>
            <option value="未付">未付</option>
            <option value="已付訂金">已付訂金</option>
            <option value="已結清">已結清</option>
            <option value="退款中">退款中</option>
            <option value="已退款">已退款</option>
          </select>
        </div>
        <div class="col-lg-3 col-md-6">
          <label class="form-label small fw-bold text-secondary">日期欄位</label>
          <select v-model="filters.dateField" class="form-select" @change="pagination.page = 1">
            <option value="pickupTime">預計取車時間</option>
            <option value="returnTime">預計還車時間</option>
            <option value="orderTime">訂單時間</option>
          </select>
        </div>
        <div class="col-lg-2 col-md-3">
          <label class="form-label small fw-bold text-secondary">起始日期</label>
          <input v-model="filters.dateFrom" type="date" class="form-control" @change="pagination.page = 1" />
        </div>
        <div class="col-lg-2 col-md-3">
          <label class="form-label small fw-bold text-secondary">結束日期</label>
          <input v-model="filters.dateTo" type="date" class="form-control" @change="pagination.page = 1" />
        </div>
        <div class="col-lg-2 col-md-3">
          <button class="btn btn-outline-secondary w-100" type="button" @click="resetFilters">
            清除篩選
          </button>
        </div>
        <div class="col-lg-6 col-md-9 text-md-end text-secondary small">
          顯示 {{ filteredAndSortedOrders.length }} / {{ orders.length }} 筆
        </div>
      </div>
    </div>

    <div class="bg-white border rounded shadow-sm overflow-hidden">
      <div class="table-responsive">
        <table class="table table-hover align-middle text-nowrap mb-0">
          <thead class="table-dark">
            <tr>
              <th class="sortable-th" @click="setSort('orderId')">訂單編號 <span>{{ getSortIcon('orderId') }}</span></th>
              <th class="sortable-th" @click="setSort('customer')">客戶 <span>{{ getSortIcon('customer') }}</span></th>
              <th class="sortable-th" @click="setSort('vehicle')">車款/車牌 <span>{{ getSortIcon('vehicle') }}</span></th>
              <th class="sortable-th" @click="setSort('orderType')">類型 <span>{{ getSortIcon('orderType') }}</span></th>
              <th class="sortable-th" @click="setSort('plan')">方案 <span>{{ getSortIcon('plan') }}</span></th>
              <th class="sortable-th" @click="setSort('pickupLocation')">取車地點 <span>{{ getSortIcon('pickupLocation') }}</span></th>
              <th class="sortable-th" @click="setSort('returnLocation')">還車地點 <span>{{ getSortIcon('returnLocation') }}</span></th>
              <th class="sortable-th" @click="setSort('pickupTime')">預計取車時間 <span>{{ getSortIcon('pickupTime') }}</span></th>
              <th class="sortable-th" @click="setSort('returnTime')">預計還車時間 <span>{{ getSortIcon('returnTime') }}</span></th>
              <th class="sortable-th" @click="setSort('rentalFee')">租金 <span>{{ getSortIcon('rentalFee') }}</span></th>
              <th class="sortable-th" @click="setSort('extraFee')">額外費用 <span>{{ getSortIcon('extraFee') }}</span></th>
              <th class="sortable-th" @click="setSort('deposit')">訂金 <span>{{ getSortIcon('deposit') }}</span></th>
              <th class="sortable-th" @click="setSort('totalAmount')">總金額 <span>{{ getSortIcon('totalAmount') }}</span></th>
              <th class="sortable-th" @click="setSort('payStatus')">付款狀態 <span>{{ getSortIcon('payStatus') }}</span></th>
              <th class="sortable-th" @click="setSort('depositPayMethod')">訂金付法 <span>{{ getSortIcon('depositPayMethod') }}</span></th>
              <th class="sortable-th" @click="setSort('balancePayMethod')">尾款付法 <span>{{ getSortIcon('balancePayMethod') }}</span></th>
              <th class="sortable-th" @click="setSort('orderStatus')">訂單狀態 <span>{{ getSortIcon('orderStatus') }}</span></th>
              <th class="sortable-th" @click="setSort('orderTime')">訂單時間 <span>{{ getSortIcon('orderTime') }}</span></th>
              <!-- 暫時隱藏發票與合約欄位
              <th>發票 ID</th>
              <th>合約</th>
              -->
              <th class="text-center sticky-end-header">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in pagedOrders" :key="order.orderId">
              <td class="fw-bold">
                <a
                  href="#"
                  @click.prevent="openDetails(order)"
                  class="text-primary text-decoration-none"
                >
                  #{{ order.orderId }}
                </a>
              </td>
              <td>{{ order.customer?.custName }}</td>
              <td>
                {{ order.vehicle?.carModel?.modelName }}
                <span class="text-muted small ms-1" v-if="order.vehicle?.plateNo">
                  ({{ order.vehicle?.plateNo }})
                </span>
              </td>
              <td>
                <span
                  :class="[
                    'badge',
                    getOrderTypeText(order.orderType) === '日租'
                      ? 'bg-info text-dark'
                      : 'bg-primary',
                  ]"
                >
                  {{ getOrderTypeText(order.orderType) }}
                </span>
              </td>
              <td>{{ order.rentalPlan?.planName || getPlanName(order.rentalPlan?.planId) }}</td>
              <td>{{ order.pickupLocation?.locationName }}</td>
              <td>{{ order.returnLocation?.locationName }}</td>
              <td>{{ order.pickupTime }}</td>
              <td>{{ order.returnTime }}</td>
              <td>{{ formatCurrency(order.rentalFee) }}</td>
              <td>{{ formatCurrency(order.extraFee) }}</td>
              <td>{{ formatCurrency(order.deposit) }}</td>
              <td class="fw-bold">{{ formatCurrency(order.totalAmount) }}</td>
              <td><StatusBadge :status="getPayStatus(order.payStatus)" :map="rentalPayStatusBadgeMap" /></td>
              <td>{{ getPayMethod(order.depositPayMethod) }}</td>
              <td>{{ getPayMethod(order.balancePayMethod) }}</td>
              <td><StatusBadge :status="getOrderStatus(order.orderStatus)" :map="rentalOrderStatusBadgeMap" /></td>
              <td>{{ order.orderTime }}</td>
              <!-- 暫時隱藏發票與合約欄位
              <td>{{ order.invoiceId || "無" }}</td>
              <td>
                <a
                  v-if="order.contract"
                  :href="order.contract"
                  target="_blank"
                  class="text-decoration-none"
                  >查看</a
                >
                <span v-else class="text-muted">無</span>
              </td>
              -->
              <td class="text-center sticky-end-cell">
                <div class="d-flex justify-content-start align-items-center gap-2 flex-nowrap operation-actions">
                  <div class="operation-status-slot">
                    <button
                      v-if="canProcessOrder(order)"
                      class="btn btn-sm btn-primary px-2 text-nowrap w-100"
                      @click="goToProcess(order.orderId)"
                    >
                      作業
                    </button>

                    <button
                      v-else-if="canMarkRefunded(order)"
                      class="btn btn-sm btn-outline-info px-2 text-nowrap w-100"
                      @click="requestMarkRefunded(order.orderId)"
                    >
                      確認已退款
                    </button>
                  </div>

                  <ActionButtons
                    edit-text="編輯"
                    delete-text="刪除"
                    @edit="goToEdit(order.orderId)"
                    @delete="deleteOrder(order.orderId, order.orderType)"
                  />
                </div>
              </td>
            </tr>
            <tr v-if="filteredAndSortedOrders.length === 0">
              <td colspan="19">
                <EmptyState
                  icon="folder-open"
                  message="沒有符合條件的訂單"
                  sub-message="請調整篩選條件或重新整理資料"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 p-3 border-top bg-light">
        
        <div>
          <Pagination
            :current-page="pagination.page"
            :total-pages="totalPages"
            @change="goToPage"
          />
        </div>

        <div class="text-secondary small">
          顯示 {{ pageStartIndex }} - {{ pageEndIndex }} 筆，共 {{ filteredAndSortedOrders.length }} 筆
        </div>

        
        <div class="d-flex align-items-center gap-2">
          <span class="text-secondary small">每頁</span>
          <select v-model.number="pagination.pageSize" class="form-select form-select-sm" style="width: 90px" @change="changePageSize">
            <option :value="10">10 筆</option>
            <option :value="20">20 筆</option>
            <option :value="50">50 筆</option>
          </select>
        </div>
      </div>
    </div>

    <BaseModal
      v-if="selectedOrder"
      :is-visible="showModal"
      :title="'訂單詳情：#' + selectedOrder.orderId"
      size="lg"
      @close="closeDetails"
    >
      <template #body>
        <ul class="list-group list-group-flush">
          <li class="list-group-item"><strong>客戶 ID：</strong> {{ selectedOrder.customer?.custName }}</li>
          <li class="list-group-item"><strong>車輛 ID：</strong> {{ selectedOrder.vehicle?.carModel?.modelName }}</li>
          <li class="list-group-item"><strong>訂單類型：</strong> {{ getVal(selectedOrder.orderType) === "SHORT_TERM" ? "日租" : "長租" }}</li>
          <li class="list-group-item"><strong>方案：</strong> {{ selectedOrder.rentalPlan?.planName }}</li>
          <li class="list-group-item"><strong>取車地點：</strong> {{ selectedOrder.pickupLocation?.locationName }}</li>
          <li class="list-group-item"><strong>還車地點：</strong> {{ selectedOrder.returnLocation?.locationName }}</li>
          <li class="list-group-item"><strong>取車時間：</strong> {{ selectedOrder.pickupTime }}</li>
          <li class="list-group-item"><strong>還車時間：</strong> {{ selectedOrder.returnTime }}</li>
          <li class="list-group-item"><strong>租金：</strong> {{ formatCurrency(selectedOrder.rentalFee) }}</li>
          <li class="list-group-item"><strong>額外費用：</strong> {{ formatCurrency(selectedOrder.extraFee) }}</li>
          <li class="list-group-item"><strong>訂金：</strong> {{ formatCurrency(selectedOrder.deposit) }}</li>
          <li class="list-group-item"><strong>總金額：</strong> {{ formatCurrency(selectedOrder.totalAmount) }}</li>
          <li class="list-group-item"><strong>付款狀態：</strong> <StatusBadge :status="getPayStatus(selectedOrder.payStatus)" :map="rentalPayStatusBadgeMap" /></li>
          <li class="list-group-item"><strong>訂金付法：</strong> {{ getPayMethod(selectedOrder.depositPayMethod) }}</li>
          <li class="list-group-item"><strong>尾款付法：</strong> {{ getPayMethod(selectedOrder.balancePayMethod) }}</li>
          <li class="list-group-item"><strong>訂單狀態：</strong> <StatusBadge :status="getOrderStatus(selectedOrder.orderStatus)" :map="rentalOrderStatusBadgeMap" /></li>
          <li class="list-group-item"><strong>訂單建立時間：</strong> {{ selectedOrder.orderTime }}</li>
          <!-- 暫時隱藏發票與合約欄位
          <li class="list-group-item"><strong>發票 ID：</strong> {{ selectedOrder.invoiceId || "無" }}</li>
          <li class="list-group-item">
            <strong>合約：</strong>
            <a v-if="selectedOrder.contract" :href="selectedOrder.contract" target="_blank">點擊查看</a>
            <span v-else>無</span>
          </li>
          -->
        </ul>
      </template>

      <!-- 以下是moder的footer -->
      <template #footer>
        <div class="d-flex justify-content-between w-100">
          <button type="button" class="btn btn-outline-secondary px-4" @click="closeDetails">關閉</button>
          <div class="d-flex gap-2">
            <button type="button" class="btn btn-info text-white px-3" @click="navigateFromModalToDetail(selectedOrder.orderId)">
              <i class="fa-regular fa-file-lines me-1"></i> 完整明細
            </button>
            <button
              v-if="['RESERVED', 'PICKED_UP', 'RETURNED', 'OVERDUE', '已預約', '已取車', '待檢查', '逾期未還', '已歸還(待檢查)'].includes(getVal(selectedOrder.orderStatus))"
              type="button"
              class="btn btn-primary px-3"
              @click="navigateFromModalToProcess(selectedOrder.orderId)"
            >
              <i class="fa-solid fa-rocket me-1"></i>取還車作業
            </button>
          </div>
        </div>
      </template>
      <!-- 以上是model的footer -->
    </BaseModal>

    <ConfirmDialog
      :is-visible="showDeleteConfirm"
      title="刪除訂單"
      :message="`確定要刪除訂單 #${pendingDeleteOrderId} 嗎？`"
      confirm-text="刪除"
      cancel-text="取消"
      confirm-variant="danger"
      @confirm="confirmDeleteOrder"
      @cancel="cancelDeleteOrder"
    />
    <ConfirmDialog
      :is-visible="showRefundConfirm"
      title="確認已退款"
      :message="`確定訂單 #${pendingRefundOrderId} 已完成退款嗎？`"
      confirm-text="確認已退款"
      cancel-text="取消"
      confirm-variant="primary"
      @confirm="confirmMarkRefunded"
      @cancel="cancelMarkRefunded"
    />
  </div>
</template>

<style scoped>
.sortable-th {
  cursor: pointer;
  user-select: none;
}

.sortable-th span {
  display: inline-block;
  min-width: 1em;
  margin-left: 0.25rem;
  opacity: 0.8;
}

.operation-actions {
  min-width: max-content;
}

.operation-status-slot {
  width: 88px;
  flex: 0 0 88px;
}

.operation-actions :deep(.btn) {
  white-space: nowrap;
}
</style>





