<script setup>
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "@/api";
import { rentalOrderAPI } from '@/api/rentalorder/rentalorderAPI';


const route = useRoute();
const router = useRouter();

// 真實的訂單資料
const order = ref({
  orderId: null,
  customerName: "",
  carInfo: "",
  start: "",
  end: "",
  status: "RESERVED", // 對應後端的 orderStatus
  rentalFee: 0,
  extraFee: 0,
  totalAmount: 0,
  depositPayMethod: "",
  balancePayMethod: "",
});

const form = ref({
  startMileage: "",
  endMileage: "",
  fuel: "FULL", // 預設帶入滿油的代碼
  note: "",
  balancePayMethod: "",
});

const balancePaymentOptions = [
  { value: "現金", label: "現金" },
  { value: "信用卡", label: "信用卡" },
  { value: "轉帳", label: "轉帳" },
  { value: "行動支付", label: "行動支付" },
];

const toPaymentLabel = (value) => {
  if (!value) return "";
  const method = String(value).trim();
  const map = {
    CASH: "現金",
    CREDIT_CARD: "信用卡",
    TRANSFER: "轉帳",
    MOBILE_PAY: "行動支付",
    現金: "現金",
    信用卡: "信用卡",
    轉帳: "轉帳",
    行動支付: "行動支付",
  };
  return map[method] || method;
};

const stepIndex = computed(() => {
  return (
    {
      RESERVED: 0,
      PICKED_UP: 1,
      OVERDUE: 1, // 新增這行：逾期也算在「已取車」這個物理階段
      RETURNED: 2,
      CLOSED: 3,
    }[order.value.status] || 0
  );
  //   這邊的意思是，我們前面先建立了字典
  // 直接在後面加上 [order.value.status]，意思是：「請拿 order.value.status 目前的字串，去前面的字典裡查出對應的數字。」
  //當 order.value.status 是 "RESERVED" 時，就等於去查 ["RESERVED"]，結果就是 0
});

// ---------------------以下為各個按鈕方法----------------
const confirmPickup = async() => {
  if (!confirm("確定要執行取車作業嗎？")) return;
  try {
    // 呼叫剛剛寫好的後端 API
    // 把里程和備註包起來一起送
    await rentalOrderAPI.pickup(order.value.orderId, {
      startMileage: form.value.startMileage,
      note: form.value.note
    });
    alert("取車成功！");
    order.value.status = "PICKED_UP"; // 後端成功後，前端才切換畫面狀態
    // 將剛剛輸入的起始里程，同步給還車畫面使用，避免還沒 F5 重新整理時抓不到
    order.value.savedStartMileage = form.value.startMileage; 
  } catch (error) {
    alert("取車失敗：" + (error.response?.data?.error || error.message));
  }

};


const confirmCancel = async() => {
  if (!confirm("確定要取消此訂單嗎？（若已付訂金，系統將轉為退款中）")) return;
  try {
    // 假設你在 rentalorderAPI 中加了 cancel 方法
    await rentalOrderAPI.cancel(order.value.orderId); 
    alert("訂單已取消！");
    order.value.status = "CANCELLED"; 
  } catch (error) {
    alert("取消失敗：" + (error.response?.data?.error || error.message));
  }
};

const confirmReturn = async() => {

  // 防呆：結束里程必填，且不得小於起始里程
  if (form.value.endMileage === "" || form.value.endMileage === null) {
    alert("請輸入結束里程！");
    return;
  }
  if (order.value.savedStartMileage && form.value.endMileage < order.value.savedStartMileage) {
    alert(`結束里程 (${formatMileage(form.value.endMileage)}) 不得小於起始里程 (${formatMileage(order.value.savedStartMileage)})！`);
    return;
  }

  if (!confirm("確定要執行還車作業嗎？")) return;
  

  try {
    await rentalOrderAPI.returnOrder(order.value.orderId, {
      endMileage: form.value.endMileage,
      fuel: form.value.fuel,
      note: form.value.note
    });
    alert("還車成功！");
    // 還車後立刻重新整理畫面，才能抓到後端剛剛算好的「超時費、油費、超里程」與「最新總額」！
    window.location.reload();
  } catch (error) {
    alert("還車失敗：" + (error.response?.data?.error || error.message));
  }

};

const confirmComplete = async() => {
  // 結案時必須選擇尾款付款方式，後端才會一併寫入 balancePayMethod
  if (!form.value.balancePayMethod) {
    alert("請先選擇尾款付款方式");
    return;
  }

  const payMethod = toPaymentLabel(form.value.balancePayMethod);
  if (!confirm(`確定要以「${payMethod}」收取尾款並結案嗎？`)) return;

  try {
    await rentalOrderAPI.close(order.value.orderId, {
      balancePayMethod: payMethod
    });
    alert("結案成功！");
    order.value.status = "CLOSED"; // 🌟 記得！後端結案的英文是 CLOSED
    order.value.balancePayMethod = payMethod;
  } catch (error) {
    alert("結案失敗：" + (error.response?.data?.error || error.message));
  }
};
// ---------------------以上為各個按鈕方法----------------

// ---------------------金錢格式處理器-------------
const formatCurrency = (value) => {
  const amount = Number(value || 0);
  return `NT$ ${amount.toLocaleString()}`;
};

// ---------------------里程格式處理器-------------
const formatMileage = (value) => {
  if (value === null || value === undefined || value === "") return "未記錄";
  const amount = Number(value || 0);
  return `${amount.toLocaleString()} km`;
};


// --------------掛載，拿資料----------------

// 新增：反向翻譯機 (處理後端傳來的中文)
const translateStatusToEnglish = (chineseStr) => {
  if (!chineseStr) return "RESERVED"; // 防呆預設值
  const s = String(chineseStr);

  if (s.includes("預約")) return "RESERVED";
  if (s.includes("取車")) return "PICKED_UP";
  if (s.includes("待檢查") || s.includes("歸還")) return "RETURNED";
  if (s.includes("逾期")) return "OVERDUE";
  if (s.includes("結案")) return "CLOSED";
  if (s.includes("取消")) return "CANCELLED";

  return "RESERVED"; // 都沒對上就給個預設值
};

// 狀態顯示文字與顏色，需與訂單列表一致
const getOrderStatusText = (status) => {
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

const getOrderStatusBadgeClass = (status) => {
  if (status === "RESERVED") return "badge bg-primary";
  if (status === "PICKED_UP") return "badge bg-warning text-dark";
  if (status === "RETURNED") return "badge bg-info text-dark";
  if (status === "OVERDUE") return "badge bg-danger";
  if (status === "CLOSED") return "badge bg-success";
  if (status === "CANCELLED") return "badge bg-secondary";
  return "badge bg-secondary";
};
//專門用來過濾出「只含有明細的括號內容」，並且把「甲地乙還」自動補上去
const extractedFeeNote = computed(() => {
  let note = '';
  
  // 1. 如果有跨站還車，直接先加上提示
  if (order.value.pickupLocation && order.value.returnLocation && order.value.pickupLocation !== order.value.returnLocation) {
    note += '甲地乙還 $500 ';
  }

  // 2. 加上後端算出來的超里程/油費/超時
  if (order.value.savedNote) {
    const match = order.value.savedNote.match(/\[還車結算加收：(.*?)\]/);
    if (match && match[1]) {
      note += match[1];
    }
  }

  if (note) {
    // 移除小數點 .00，並把明細中的 $65900 轉成 NT$ 65,900
    const formattedNote = note
      .replace(/\.00/g, '')
      .replace(/\$(\d+)/g, (_, amount) => formatCurrency(amount));

    return `[還車結算明細：${formattedNote}]`;
  }
  return '';
});

// 過濾出扣除結算明細後的「原本備註」
const originalNote = computed(() => {
  if (!order.value.savedNote) return '';
  return order.value.savedNote.replace(/\[還車結算加收：.*?\]/, '').trim();
});

//修改：進畫面抓資料時，套用翻譯機
onMounted(async () => {
  const orderId = route.params.id; //從網址列抓取訂單編號。例如網址是 /orders/process/10，它就會抓到 10。
  if (orderId) {
    try {
      const response = await rentalOrderAPI.getById(orderId);
      const data = response.data;

      order.value = {
        orderId: data.orderId,
        customerName: data.customer?.custName || "未知客戶",
        carInfo: `${data.vehicle?.carModel?.modelName} / ${data.vehicle?.plateNo}`,
        start: data.pickupTime,
        end: data.returnTime,

        // 用翻譯機把中文轉回英文代碼
        status: translateStatusToEnglish(data.orderStatus),

        //把取車跟還車的地點抓出來，用來判斷甲租乙還
        pickupLocation: data.pickupLocation?.locationId || null,
        returnLocation: data.returnLocation?.locationId || null,

        rentalFee: data.rentalFee,
        extraFee: data.extraFee,
        totalAmount: data.totalAmount,
        depositPayMethod: toPaymentLabel(data.depositPayMethod),
        balancePayMethod: toPaymentLabel(data.balancePayMethod),
      };

      form.value.balancePayMethod = order.value.balancePayMethod || "";

      // 如果訂單還在「已預約」狀態，才自動帶入起步里程 (避免蓋掉已取車的資料)
      if (order.value.status === 'RESERVED') {
        form.value.startMileage = data.vehicle?.mileage || '';
      }

      //找出該訂單在「取車時」存進資料庫的起始里程，供還車時顯示與驗證
      const detail = data.shortTermDetail || data.longTermDetail;
      if (detail && detail.startMileage) {
        order.value.savedStartMileage = detail.startMileage;
      } else {
        order.value.savedStartMileage = 0; // 若沒紀錄就給 0 防呆
      }

      // 抓取舊的備註文字，顯示在畫面上
      if (detail && detail.noteDesc) {
        order.value.savedNote = detail.noteDesc;
      } else {
        order.value.savedNote = '';
      }

    } catch (error) {
      console.error("抓取訂單失敗", error);
      alert("載入失敗");
      router.push("/orders/list");
    }
  }
});

//-------------------------------
</script>

<template>
  <div class="container-fluid py-4">
    <!-- STEP 1：頁面標題 -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold mb-0">處理租賃訂單</h2>
      <!-- 隨時可以跳回列表的按鈕 -->
      <button class="btn btn-outline-secondary" @click="router.push('/orders/list')">
        <i class="fa-solid fa-arrow-left me-1"></i> 返回訂單列表
      </button>

      <span :class="getOrderStatusBadgeClass(order.status)" class="fs-6">{{ getOrderStatusText(order.status) }}</span>
    </div>

    <!-- STEP 2：訂單基本資訊 -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-body">
        <div class="row g-4">
          <div class="col-md-3">
            <div class="text-secondary small mb-1">客戶姓名</div>

            <div class="fw-semibold">
              {{ order.customerName }}
            </div>
          </div>

          <div class="col-md-3">
            <div class="text-secondary small mb-1">車輛資訊</div>

            <div class="fw-semibold">
              {{ order.carInfo }}
            </div>
          </div>

          <div class="col-md-3">
            <div class="text-secondary small mb-1">預計取車</div>

            <div class="fw-semibold">
              {{ order.start }}
            </div>
          </div>

          <div class="col-md-3">
            <div class="text-secondary small mb-1">預計還車</div>

            <div class="fw-semibold">
              {{ order.end }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- STEP 3：Bootstrap Stepper -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-body">
        <div class="row text-center">
          <div class="col">
            <div
              class="rounded-circle mx-auto mb-2 d-flex align-items-center justify-content-center"
              :class="
                stepIndex >= 0
                  ? 'bg-primary text-white'
                  : 'bg-light text-secondary'
              "
              style="width: 50px; height: 50px"
            >
              1
            </div>

            <div class="fw-semibold">已預約</div>
          </div>

          <div class="col">
            <div
              class="rounded-circle mx-auto mb-2 d-flex align-items-center justify-content-center"
              :class="
                stepIndex >= 1
                  ? 'bg-warning text-dark'
                  : 'bg-light text-secondary'
              "
              style="width: 50px; height: 50px"
            >
              2
            </div>

            <div class="fw-semibold">已取車</div>
          </div>

          <div class="col">
            <div
              class="rounded-circle mx-auto mb-2 d-flex align-items-center justify-content-center"
              :class="
                stepIndex >= 2
                  ? 'bg-info text-dark'
                  : 'bg-light text-secondary'
              "
              style="width: 50px; height: 50px"
            >
              3
            </div>

            <div class="fw-semibold">已歸還(待檢查)</div>
          </div>

          <div class="col">
            <div
              class="rounded-circle mx-auto mb-2 d-flex align-items-center justify-content-center"
              :class="
                stepIndex >= 3
                  ? 'bg-success text-white'
                  : 'bg-light text-secondary'
              "
              style="width: 50px; height: 50px"
            >
              4
            </div>

            <div class="fw-semibold">已結案</div>
          </div>
        </div>
      </div>
    </div>

    <!-- STEP 4：動態作業區 -->
    <div class="card border-0 shadow-sm">
      <div class="card-body">
        <!-- 取車 -->
        <div v-if="order.status === 'RESERVED'">
          <h5 class="fw-bold mb-4">取車作業</h5>

          <div class="mb-3">
            <label class="form-label"> 起始里程 </label>

            <input
              v-model="form.startMileage"
              type="number"
              class="form-control"
              placeholder="請輸入起始里程"
            />
          </div>

          <div class="mb-4">
            <label class="form-label"> 取車備註 </label>

            <textarea
              v-model="form.note"
              class="form-control"
              rows="4"
              placeholder="請輸入備註"
            ></textarea>
          </div>

          <div class="d-flex justify-content-end gap-3">
            <button class="btn btn-outline-danger px-4" @click="confirmCancel">
              取消訂單
            </button>
            <button class="btn btn-warning px-4" @click="confirmPickup">
              確認取車
            </button>
          </div>
        </div>

        <!-- 還車 -->
        <div v-if="order.status === 'PICKED_UP' || order.status === 'OVERDUE'">
          <h5 class="fw-bold mb-4">🔁 還車作業</h5>

          <div v-if="order.status === 'OVERDUE'" class="alert alert-danger d-flex align-items-center mb-4" role="alert">
            <i class="bi bi-exclamation-triangle-fill fs-4 me-3"></i>
            <div>
              <strong>注意！此訂單已逾期未還！</strong>
              <br>請在結算時注意收取逾時費用。
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label d-flex justify-content-between w-100">
              <span>結束里程 <span class="text-danger">*</span></span>
              <!--  動態顯示起始里程 -->
              <span class="text-primary small fw-bold">
                <i class="fa-solid fa-circle-info me-1"></i>起始里程：{{ order.savedStartMileage !== 0 ? formatMileage(order.savedStartMileage) : '未記錄' }}
              </span>
            </label>
            <input
              v-model="form.endMileage"
              type="number"
              class="form-control"
              placeholder="請輸入結束里程"
            />
          </div>
          <div class="mb-3">
            <label class="form-label"> 油量 </label>

            <select v-model="form.fuel" class="form-select">
              <option value="FULL">滿油</option>
              <option value="HIGH">四分之三</option>
              <option value="HALF">一半</option>
              <option value="LOW">四分之一</option>
              <option value="EMPTY">見底</option>
            </select>
          </div>

          <div class="mb-4">
            <label class="form-label">還車備註</label>

            <textarea
              v-model="form.note"
              class="form-control"
              rows="4"
            ></textarea>
          </div>

          <div class="text-end">
            <button class="btn btn-primary px-4" @click="confirmReturn">
              確認還車
            </button>
          </div>
        </div>

        <!--  結算 -->
         <div v-if="order.status === 'RETURNED'">
          <h5 class="fw-bold mb-4">費用結算</h5>
          <ul class="list-group mb-4">
            <li class="list-group-item d-flex justify-content-between align-items-center">
              <span>基礎租金 <small class="text-muted">(含預定加時費)</small></span>
              <span class="fw-semibold fs-5">{{ formatCurrency(order.rentalFee)}}</span>
            </li>
            <li class="list-group-item d-flex justify-content-between align-items-center">
              <span>
                額外費用 
                <small class="text-muted d-block mt-1" style="font-size: 0.8rem;">
                  (含甲租乙還、油資、超里程、實際逾時)
                </small>
              </span>
              <span class="fw-semibold fs-5 text-warning">{{ formatCurrency(order.extraFee)}}</span>
            </li>
          </ul>
          <!-- 將結算明細直接顯示在這裡！ -->
          <div v-if="extractedFeeNote || originalNote" class=" border small mb-4">
            <div class="mb-1">
              <i class="fa-solid fa-file-invoice-dollar me-2 text-danger"></i><strong>結算明細與備註：</strong>
            </div>
            
            <div v-if="extractedFeeNote" class="mt-2 text-dark fw-bold">{{ extractedFeeNote }}</div>
            
            <div v-if="originalNote" class="mt-1 text-muted" style="white-space: pre-wrap;">
              <i class="fa-solid fa-pen me-1"></i>原備註：{{ originalNote }}
            </div>
          </div>
          <div v-else class="alert alert-light border text-muted small mb-4">
            <i class="fa-solid fa-check me-2 text-success"></i>此筆訂單無額外結算備註。
          </div>
          <!-- 新增：尾款付款方式會跟著結案 API 一起送到後端保存 -->
          <div class="mb-4">
            <label class="form-label fw-bold">尾款付款方式</label>
            <select class="form-select" v-model="form.balancePayMethod">
              <option disabled value="">請選擇尾款付款方式</option>
              <option
                v-for="option in balancePaymentOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </option>
            </select>
            <div class="form-text">
              結案時會將尾款付款方式寫入訂單，並將付款狀態更新為已付清。
            </div>
          </div>
          <div class="d-flex justify-content-between align-items-center">
            <h4 class="text-danger fw-bold mb-0">應付總額：{{ formatCurrency(order.totalAmount)}}</h4>
            <button class="btn btn-success px-4" @click="confirmComplete">
              確認結案
            </button>
          </div>
        </div>

        <!-- 完成 -->
        <div v-if="order.status === 'CLOSED'" class="text-center py-5">
          <div class="display-1 text-success mb-3">
            <i class="fa-solid fa-circle-check"></i>
          </div>
          <h3 class="fw-bold text-success mb-2">訂單已完成結案</h3>
          <p class="text-muted">此筆租賃訂單已順利完成並結清費用。</p>
          
          <!-- 結案後的動向按鈕 -->
          <div class="mt-4">
            <button class="btn btn-outline-primary mx-2" @click="router.push('/orders/list')">
              <i class="fa-solid fa-list me-1"></i> 回到訂單列表
            </button>
            <button class="btn btn-primary mx-2" @click="router.push(`/orders/detail/${order.orderId}`)">
              <i class="fa-solid fa-file-invoice me-1"></i> 查看此訂單明細
            </button>
          </div>
        </div>

        <!-- 取消 -->

        <div v-if="order.status === 'CANCELLED'" class="text-center py-5 bg-light rounded border">
          <div class="display-5 mb-3">🚫</div>
          <h3 class="fw-bold text-danger mb-2">訂單已取消</h3>
          <p class="text-muted">此訂單已被終止，無須進行後續取還車作業。</p>
        </div>

      </div>
    </div>
  </div>
</template>
