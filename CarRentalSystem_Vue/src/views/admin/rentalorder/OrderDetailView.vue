<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { rentalOrderAPI } from '@/api/rentalorder/rentalorderAPI';

const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(true);

// 提取並轉換後端代碼的小工具
const toCode = (val) => {
  if (!val) {
    return '';
  }
  const s = typeof val === 'object' ? (val.dbCode || val.name || '') : String(val);
  return s;
};
//這邊是根據後面看是否有設定dbCode或name的變數，可以自行取
//但因為我後端是@JsonValue，所以傳回來的確定直接是中文字串
/*可以直接這樣寫
const toCode = (val) => {
  if (!val) return '';
  const s = String(val); 
  return s;
  }
*/
// 時間格式化工具
const formatDateTime = (s) => {
  if (!s) {
    return '無';
    }
  return s.replace('T', ' ').substring(0, 16);
};

onMounted(async () => {
  try {
    const res = await rentalOrderAPI.getById(route.params.id);
    order.value = res.data;
  } catch (error) {
    alert("載入訂單失敗");
    router.push('/orders/list');
  } finally {
    loading.value = false;
  }
});

// 跳轉方法
const goBack = () => router.push('/orders/list');
const goEdit = () => router.push(`/orders/edit/${route.params.id}`);
const goProcess = () => router.push(`/orders/process/${route.params.id}`);

// 狀態 Badge 顏色轉換
const getStatusBadge = (status) => {
  const s = toCode(status);
  if (s.includes('RESERVED') || s.includes('預約')) return 'bg-warning text-dark';
  if (s.includes('PICKED_UP') || s.includes('取車')) return 'bg-primary';
  if (s.includes('RETURNED') || s.includes('待檢查')) return 'bg-info text-dark';
  if (s.includes('OVERDUE') || s.includes('逾期')) return 'bg-danger';
  if (s.includes('CLOSED') || s.includes('結案')) return 'bg-success';
  if (s.includes('CANCELLED') || s.includes('取消')) return 'bg-secondary';
  return 'bg-secondary';
};

const getPayStatusBadge = (status) => {
  const s = toCode(status);
  if (s.includes('UNPAID') || s.includes('未付')) return 'bg-danger';
  if (s.includes('DEPOSIT_PAID') || s.includes('訂金')) return 'bg-warning text-dark';
  if (s.includes('PAID') || s.includes('完成')) return 'bg-success';
  if (s.includes('REFUND')) return 'bg-secondary';
  return 'bg-secondary';
};

// 油量格式化
const formatFuel = (fuel) => {
  const f = toCode(fuel);
  if (f.includes('FULL') || f.includes('滿')) return '滿油';
  if (f.includes('HALF') || f.includes('半')) return '半油';
  if (f.includes('EMPTY') || f.includes('空')) return '空油';
  return f || '無';
};

//方案處理
const getPlanName = (id) => {
  const plans = {
    1: "日租-小型轎車", 2: "日租-中型轎車", 3: "日租-休旅車", 4: "日租-廂型車", 5: "日租-電動車",
    6: "長租-小型轎車", 7: "長租-中型轎車", 8: "長租-休旅車", 9: "長租-廂型車", 10: "長租-電動車", 11: "其他 (客製化)"
  };
  return plans[id] || `未指定(${id})`;
};

// ---------------------金錢格式處理器-------------
const formatCurrency = (value) => {
  const amount = Number(value || 0);
  return `NT$ ${amount.toLocaleString()}`;
};

// ---------------------里程格式處理器-------------
const formatMileage = (value) => {
  if (value === null || value === undefined || value === "") return "-";
  const amount = Number(value || 0);
  return `${amount.toLocaleString()} km`;
};

// 備註文字可能包含後端寫入的 $65900，顯示時一起套用金額格式
const formatTextCurrency = (value) => {
  if (!value) return "";
  return String(value)
    .replace(/\.00/g, "")
    .replace(/\$(\d+)/g, (_, amount) => formatCurrency(amount));
};

</script>

<template>
  <div class="container py-4">
    <!-- 載入中畫面 -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <div class="mt-3 text-muted">讀取訂單明細中...</div>
    </div>

    <div v-else-if="order">
      <!-- 標題與操作區 -->
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h3 class="fw-bold mb-1">
            <i class="fa-solid fa-file-invoice text-primary me-2"></i>訂單明細 #{{ order.orderId }}
          </h3>
          <span class="badge fs-6 mt-1" :class="getStatusBadge(order.orderStatus)">
            {{ toCode(order.orderStatus) }}
          </span>
        </div>
        <div class="btn-group shadow-sm">
          <button class="btn btn-outline-secondary" @click="goBack">
            <i class="fa-solid fa-arrow-left me-1"></i> 返回列表
          </button>
          <button class="btn btn-primary" @click="goEdit">
            <i class="fa-solid fa-pen-to-square me-1"></i> 編輯訂單
          </button>
          <button class="btn btn-warning" @click="goProcess">
            <i class="fa-solid fa-wrench me-1"></i> 取還車作業
          </button>
        </div>
      </div>

      <div class="row g-4">
        <!-- 左側：客戶與車輛 -->
        <div class="col-md-6">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-header bg-light fw-bold py-3">
              <i class="fa-solid fa-user me-2 text-secondary"></i>客戶與車輛資訊
            </div>
            <div class="card-body">
              <div class="row mb-3">
                <div class="col-sm-4 text-muted small">客戶名稱</div>
                <div class="col-sm-8 fw-semibold">{{ order.customer?.custName || '無' }}</div>
              </div>
              <div class="row mb-3">
                <div class="col-sm-4 text-muted small">聯絡電話</div>
                <div class="col-sm-8 fw-semibold">{{ order.customer?.custPhone || '無' }}</div>
              </div>
              <hr class="text-muted opacity-25">
              <div class="row mb-3">
                <div class="col-sm-4 text-muted small">預約車輛</div>
                <div class="col-sm-8 fw-semibold">
                  {{ order.vehicle?.carModel?.modelName || '未知車型' }} <br>
                  <span class="badge bg-secondary mt-1">{{ order.vehicle?.plateNo || '無車牌' }}</span>
                </div>
              </div>
              <div class="row mb-3">
                <div class="col-sm-4 text-muted small">專案方案</div>
               <div class="col-sm-8 fw-semibold">{{ order.rentalPlan?.name || getPlanName(order.rentalPlan?.planId) || '無' }}</div>
              </div>
              <div class="row">
                <div class="col-sm-4 text-muted small">訂單類型</div>
                <div class="col-sm-8 fw-semibold">{{ toCode(order.orderType) }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右側：時間與地點 -->
        <div class="col-md-6">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-header bg-light fw-bold py-3">
              <i class="fa-solid fa-calendar-days me-2 text-secondary"></i>時間與地點
            </div>
            <div class="card-body">
              <div class="row mb-3">
                <div class="col-sm-4 text-muted small">預計取車</div>
                <div class="col-sm-8 fw-semibold text-primary">
                  {{ formatDateTime(order.pickupTime) }}
                </div>
              </div>
              <div class="row mb-3">
                <div class="col-sm-4 text-muted small">預計還車</div>
                <div class="col-sm-8 fw-semibold text-primary">
                  {{ formatDateTime(order.returnTime) }}
                </div>
              </div>
              <hr class="text-muted opacity-25">
              <div class="row mb-3">
                <div class="col-sm-4 text-muted small">取車地點</div>
                <div class="col-sm-8 fw-semibold">
                  <i class="fa-solid fa-location-dot text-danger me-1"></i>
                  {{ order.pickupLocation?.name || '台北總店' }}
                </div>
              </div>
              <div class="row">
                <div class="col-sm-4 text-muted small">還車地點</div>
                <div class="col-sm-8 fw-semibold">
                  <i class="fa-solid fa-location-dot text-danger me-1"></i>
                  {{ order.returnLocation?.name || '台北總店' }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 下方：費用與付款 -->
        <div class="col-12">
          <div class="card shadow-sm border-0">
            <div class="card-header bg-light fw-bold py-3">
              <i class="fa-solid fa-sack-dollar me-2 text-secondary"></i>費用與付款
            </div>
            <div class="card-body">
              <div class="row g-3 align-items-center">
                <div class="col-md-2 col-6 text-center border-end">
                  <div class="text-muted small mb-1">基本租金</div>
                  <div class="fw-bold fs-5">{{formatCurrency(order.rentalFee)}}</div>
                </div>
                <div class="col-md-2 col-6 text-center border-end">
                  <div class="text-muted small mb-1">額外費用</div>
                  <div class="fw-bold fs-5">{{formatCurrency(order.extraFee)}}</div>
                 <small 
                  v-if="order.extraFee > 0 && order.pickupLocation?.locationId !== order.returnLocation?.locationId" 
                  class="text-secondary d-block mt-1" 
                  style="font-size: 0.8rem;">
                  (含甲租乙還調度費)
                  </small>
                </div>
                <div class="col-md-2 col-6 text-center border-end">
                  <div class="text-muted small mb-1">押金/訂金</div>
                  <div class="fw-bold fs-5">{{formatCurrency(order.deposit)}}</div>
                </div>
                <div class="col-md-3 col-6 text-center border-end">
                  <div class="text-muted small mb-1">應付總額</div>
                  <div class="fw-bold fs-4 text-danger">{{formatCurrency(order.totalAmount)}}</div>
                </div>
                <div class="col-md-3 col-12 text-md-start text-center ps-md-4">
                  <div class="mb-2">
                    <span class="text-muted small me-2">付款狀態</span>
                    <span class="badge" :class="getPayStatusBadge(order.payStatus)">{{ toCode(order.payStatus) }}</span>
                  </div>
                  <div class="small text-muted">
                    訂金：<span class="fw-semibold text-dark">{{ toCode(order.depositPayMethod) || '無' }}</span><br>
                    尾款：<span class="fw-semibold text-dark">{{ toCode(order.balancePayMethod) || '無' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 動態顯示：日租明細 -->
        <div class="col-12" v-if="(toCode(order.orderType) === 'SHORT_TERM' || toCode(order.orderType).includes('日租')) && order.shortTermDetail">
          <div class="card shadow-sm border-info border-top border-3">
            <div class="card-header bg-white fw-bold text-info border-0 pt-3 pb-0 fs-5">
              <i class="fa-solid fa-car-side me-2"></i>日租明細紀錄
            </div>
            <div class="card-body">
              <div class="row g-3">
                <div class="col-md-3 col-6">
                  <div class="text-muted small">實際取車時間</div>
                  <div class="fw-semibold">{{ formatDateTime(order.shortTermDetail.actualPickupTime) }}</div>
                </div>
                <div class="col-md-3 col-6">
                  <div class="text-muted small">實際還車時間</div>
                  <div class="fw-semibold">{{ formatDateTime(order.shortTermDetail.actualReturnTime) }}</div>
                </div>
                <div class="col-md-2 col-4">
                  <div class="text-muted small">出車里程</div>
                  <div class="fw-semibold"><i class="fa-solid fa-gauge me-1 text-muted"></i>{{ formatMileage(order.shortTermDetail.startMileage) }}</div>
                </div>
                <div class="col-md-2 col-4">
                  <div class="text-muted small">還車里程</div>
                  <div class="fw-semibold"><i class="fa-solid fa-gauge me-1 text-muted"></i>{{ formatMileage(order.shortTermDetail.endMileage) }}</div>
                </div>
                <div class="col-md-2 col-4">
                  <div class="text-muted small">還車油量</div>
                  <div class="fw-semibold"><i class="fa-solid fa-gas-pump me-1 text-muted"></i>{{ formatFuel(order.shortTermDetail.fuelLevelReturn) }}</div>
                </div>
                <div class="col-12 mt-3" v-if="order.shortTermDetail.noteDesc">
                  <div class="text-muted small mb-1">備註說明</div>
                  <div class="p-3 bg-light rounded border">{{ formatTextCurrency(order.shortTermDetail.noteDesc) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 動態顯示：長租明細 -->
        <div class="col-12" v-if="(toCode(order.orderType) === 'LONG_TERM' || toCode(order.orderType).includes('長租')) && order.longTermDetail">
          <div class="card shadow-sm border-success border-top border-3">
            <div class="card-header bg-white fw-bold text-success border-0 pt-3 pb-0 fs-5">
              <i class="fa-solid fa-building me-2"></i>長租明細紀錄
            </div>
            <div class="card-body">
              <div class="row g-3">
                <!-- 第一排：4個欄位，統一把 col 設為 3 -->
                <div class="col-md-3 col-6">
                  <div class="text-muted small">實際取車時間</div>
                  <div class="fw-semibold">{{ formatDateTime(order.longTermDetail.actualPickupTime) }}</div>
                </div>
                <div class="col-md-3 col-6">
                  <div class="text-muted small">實際還車時間</div>
                  <div class="fw-semibold">{{ formatDateTime(order.longTermDetail.actualReturnTime) }}</div>
                </div>
                <div class="col-md-3 col-6">
                  <div class="text-muted small">合約月數</div>
                  <div class="fw-semibold">{{ order.longTermDetail.contractMonths || '-' }} 個月</div>
                </div>
                <div class="col-md-3 col-6">
                  <div class="text-muted small">每月租金</div>
                  <div class="fw-semibold">{{ order.longTermDetail.monthlyPayment != null ? formatCurrency(order.longTermDetail.monthlyPayment) : '-' }}</div>
                </div>
                <!-- 第二排：4個欄位，統一把 col 設為 3 -->
                <div class="col-md-3 col-6 mt-4">
                  <div class="text-muted small">請款日</div>
                  <div class="fw-semibold">每月 {{ order.longTermDetail.billingDay || '-' }} 日</div>
                </div>
                <div class="col-md-3 col-6 mt-4">
                  <div class="text-muted small">已付月數</div>
                  <div class="fw-semibold">{{ order.longTermDetail.paidMonths || 0 }} 個月</div>
                </div>
                <div class="col-md-3 col-6 mt-4">
                  <div class="text-muted small">出車里程</div>
                  <div class="fw-semibold"><i class="fa-solid fa-gauge me-1 text-muted"></i>{{ formatMileage(order.longTermDetail.startMileage) }}</div>
                </div>
                <div class="col-md-3 col-6 mt-4">
                  <div class="text-muted small">還車里程</div>
                  <div class="fw-semibold"><i class="fa-solid fa-gauge me-1 text-muted"></i>{{ formatMileage(order.longTermDetail.endMileage) }}</div>
                </div>
                
                <!-- 備註說明佔滿一行 -->
                <div class="col-12 mt-3" v-if="order.longTermDetail.noteDesc">
                  <div class="text-muted small mb-1">備註說明</div>
                  <div class="p-3 bg-light rounded border">{{ formatTextCurrency(order.longTermDetail.noteDesc) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 暫時隱藏發票與合約欄位
        最下方：其他資訊 (發票/合約)
        <div class="col-12">
          <div class="card shadow-sm border-0">
            <div class="card-body py-3">
              <div class="row align-items-center">
                <div class="col-md-6">
                  <span class="text-muted small me-2"><i class="fa-solid fa-receipt me-1"></i>發票 ID：</span>
                  <span class="fw-semibold">{{ order.invoiceId || '未開立' }}</span>
                </div>
                <div class="col-md-6 text-md-end mt-2 mt-md-0">
                  <span class="text-muted small me-2"><i class="fa-solid fa-file-signature me-1"></i>合約連結：</span>
                  <a v-if="order.contract" :href="order.contract" target="_blank" class="fw-semibold text-decoration-none btn btn-sm btn-outline-primary">
                    <i class="fa-solid fa-link me-1"></i>檢視合約圖片
                  </a>
                  <span v-else class="fw-semibold text-muted">無合約圖片</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        -->
      </div>
    </div>

    <!-- 找不到資料 -->
    <div v-else class="text-center py-5">
      <div class="display-1 text-muted mb-3"><i class="fa-solid fa-folder-open"></i></div>
      <h4 class="text-muted">找不到此訂單資料</h4>
      <button class="btn btn-outline-secondary mt-3" @click="goBack">返回列表</button>
    </div>
  </div>
</template>

<style scoped>
/* 遵循規範：所有樣式皆由 Bootstrap class 處理，不撰寫額外 CSS */
</style>



