<script setup>
import { ref, onMounted, computed,watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/api/index';
import { rentalOrderAPI } from '@/api/rentalorder/rentalorderAPI';
import {orderVehicleAPI} from '@/api/rentalorder/ordervehicleAPI';

const route = useRoute();
const router = useRouter();
const isEditMode = computed(() => !!route.params.id);

const minDateTime = ref(new Date().toISOString().substring(0, 16));

const form = ref({
  orderId: null,
  custId: null,
  vehicleId: null,
  orderType: '',
  planId: '',
  pickupLocationId: '',
  returnLocationId: '',
  pickupTime: '',
  returnTime: '',
  rentalFee: 0,
  extraFee: 0,
  deposit: 0,
  totalAmount: 0,
  payStatus: '',
  orderTime: '',
  orderStatus: '',
  depositPayMethod: '',
  balancePayMethod: '',
  invoiceId: null,
  contract: '',
  deleted: false
});

// 1. 為了下拉選單準備的變數
const customers = ref([]); // 存放原始 API 資料
const vehicles = ref([]); // 綁定搜尋輸入框
const searchQuery = ref(''); 
const showCustDropdown = ref(false); // 控制下拉選單顯示與否
const selectedCustName = ref(''); // 顯示已選擇的客戶名稱

// 2. 客戶搜尋過濾邏輯
const filteredCustomers = computed(() => {
  if (!searchQuery.value) {
    return customers.value;
  }

  const keyword = searchQuery.value.toLowerCase();
  // 過濾器，有符合打字的
  return customers.value.filter(customer => 
    (customer.custName && customer.custName.toLowerCase().includes(keyword)) ||
    (customer.custPhone && customer.custPhone.includes(keyword))
  );
});

// 3. 點選客戶後的動作（點選2.過濾後跑出來的下拉選單裡出現的客人名字」）
const selectCustomer = (cust) => {
  form.value.custId = cust.custId;
  selectedCustName.value = `${cust.custName} (${cust.custPhone})`;
  searchQuery.value = '';
  showCustDropdown.value = false;
};

// 格式轉換工具
// 修正後：保留 T，直接加上秒數 (因為 input datetime-local 預設只到分鐘)
const formatToBackendDate = (s) => {
  if (!s) return null;
  // 如果字串長度剛好到分鐘 (16碼，如 2026-05-06T06:54)，就補上秒數
  return s.length === 16 ? s + ':00' : s;
};
const formatToFrontendDate = (s) => s ? s.substring(0, 16).replace(' ', 'T') : '';
const validateNumber = (event, field) => { if (form.value[field] < 0) form.value[field] = 0; };

// 一鍵填充假資料
const fillMockData =async() => {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  // 【關鍵修正】：取得台灣時區差 (分鐘)，台灣是 -480 分鐘
  const offset = tomorrow.getTimezoneOffset(); 
  // 把這 480 分鐘減回去 (也就是加上 8 小時)
  tomorrow.setMinutes(tomorrow.getMinutes() - offset);
  const tomorrowStr = tomorrow.toISOString().substring(0, 16);

// ----後天
  const aftertomorrow = new Date();
  aftertomorrow.setDate(aftertomorrow.getDate() + 2);
  const offset2 = aftertomorrow.getTimezoneOffset();
  aftertomorrow.setMinutes(aftertomorrow.getMinutes() - offset);
  const aftertomorrowStr = aftertomorrow.toISOString().substring(0, 16);


  // 1. 填充主訂單資料
  //先把時間寫入 form，這會觸發 watch
  form.value.pickupTime = tomorrowStr;
  form.value.returnTime = aftertomorrowStr;

  // 主動呼叫 API 等待可用車輛名單回來
  await fetchAvailableVehicles(tomorrowStr, aftertomorrowStr);

  // 動態抓取名單上的第一台車！(如果沒車就留白)
  const availableCarId = vehicles.value.length > 0 ? vehicles.value[0].vehicleId : '';

  form.value = {
    ...form.value,
    custId: 1,
    vehicleId: availableCarId, //使用動態抓到的車輛 ID
    orderType: 'SHORT_TERM', // 設定為日租，只要設定這個，watch 就會自動把 planId 變成 1
    // planId: 1,
    pickupLocationId: 1,
    returnLocationId: 2,
    pickupTime: tomorrowStr,
    returnTime: aftertomorrowStr,
    rentalFee: 0,
    extraFee: 0,
    deposit: 0,
    totalAmount: 0,
    payStatus: 'DEPOSIT_PAID',
    orderStatus: 'RESERVED',
    depositPayMethod: 'CREDIT_CARD',
    balancePayMethod: '',
    // 暫時隱藏發票與合約欄位
    // contract: 'https://images.unsplash.com/photo-1554080353-a576cf803bda?auto=format&fit=crop&w=300',
    // invoiceId: 1234567,
    orderTime: minDateTime.value
  };

  // 模擬選擇客戶，讓名字顯示出來
  // 去 customers 清單裡面找 ID 是 1 的人，找不到就給個假名字
  const mockCustomer = customers.value.find(c => c.custId === 1);
  if (mockCustomer) {
    selectedCustName.value = `${mockCustomer.custName} (${mockCustomer.custPhone})`;
  } else {
    selectedCustName.value = '李小龍 (0912345678)'; // 預設備用
  }

  // 同時把日租明細的資料也填好
  shortTermForm.value = {
    actualPickupTime: '',
    actualReturnTime:'',
    startMileage: 45000,
    endMileage: '',
    fuelLevelReturn: 'FULL',
    noteDesc: '請問可以放嬰兒車嗎'
  };
};

// ------------處理方案的聯動--------------------

// 1. 定義所有的方案對照表 (加入 type 與 vehicleType 來做精準判斷)
const allPlans = [
  { id: 1, name: "(1)日租-小型轎車", type: "SHORT_TERM", vehicleType: "小型轎車" },
  { id: 2, name: "(2)日租-中型轎車", type: "SHORT_TERM", vehicleType: "中型轎車" },
  { id: 3, name: "(3)日租-休旅車", type: "SHORT_TERM", vehicleType: "休旅車" },
  { id: 4, name: "(4)日租-廂型車", type: "SHORT_TERM", vehicleType: "廂型車" },
  { id: 5, name: "(5)日租-電動車", type: "SHORT_TERM", vehicleType: "電動車" },
  { id: 6, name: "(6)長租-小型轎車", type: "LONG_TERM", vehicleType: "小型轎車" },
  { id: 7, name: "(7)長租-中型轎車", type: "LONG_TERM", vehicleType: "中型轎車" },
  { id: 8, name: "(8)長租-休旅車", type: "LONG_TERM", vehicleType: "休旅車" },
  { id: 9, name: "(9)長租-廂型車", type: "LONG_TERM", vehicleType: "廂型車" },
  { id: 10, name: "(10)長租-電動車", type: "LONG_TERM", vehicleType: "電動車" },
  { id: 11, name: "(11)其他(客製化)", type: "ALL", vehicleType: "ALL" },
];

// 2. 根據目前選的「訂單類型(orderType)」過濾方案選項
const filteredPlans = computed(() => {
  if (!form.value.orderType) return allPlans; // 沒選類型就顯示全部
  return allPlans.filter(p => p.type === form.value.orderType || p.type === 'ALL');
});


// 3. 【連動邏輯】監聽訂單類型的變化，自動把 planId 填成該類型的第一個方案
watch(() => form.value.orderType, (newType) => {
  if (newType && !isEditMode.value) { // 確保只在新建立時自動連動
    const defaultPlan = filteredPlans.value.find(p => p.type === newType);
    if (defaultPlan) {
      form.value.planId = defaultPlan.id;
    }
  } else if (!newType) {
    form.value.planId = '';
  }
});


// 短租，要給 AdminShortTermReqDto 與 AdminShortTermUpdateDto 定義資料-----------------

const shortTermForm = ref({
  detailId: null, // 更新時可能會用到[cite: 13]
  actualPickupTime: '',
  actualReturnTime: '',
  startMileage: 0,
  endMileage: 0,
  fuelLevelReturn: '',
  noteDesc: ''
});


// 長租，要給 AdminLongTermReqDto 與 AdminLongTermUpdateDto 定義-----------------

const longTermForm = ref({
  actualPickupTime: '',
  actualReturnTime: '',
  contractMonths: 0,
  monthlyPayment: 0,
  billingDay: 1,
  paidMonths: 0,
  // deliveryAddress: '',
  startMileage: 0,
  endMileage: 0,
  noteDesc: ''
});


//監聽更新方案是否有改
watch(() => form.value.planId, (newVal, oldVal) => {
  if (oldVal !== '') { // 確保不是初次載入
    form.value.rentalFee = 0; 
    form.value.extraFee = 0;
    // 這樣後端收到 0 就會知道要強制重算，不會被舊數字蓋掉
  }
});
// ----------------------------以下，為了解決一鍵填入時間時抓不到車輛(因為會變)
// 1. 抽離出來的獨立找車方法
const fetchAvailableVehicles = async (start, end) => {
  // 確保兩個時間都有填寫，才發送 API 請求
  if (!start || !end) return;
  
  try {
    console.log("偵測到時間變更，開始尋找可用車輛...");
    
    // 呼叫你剛剛寫好的後端 API (記得把時間格式轉好)
    const response = await orderVehicleAPI.getAvailable(formatToBackendDate(start), formatToBackendDate(end));
      
      // params: {
      //   pickupTime: formatToBackendDate(start),
      //   returnTime: formatToBackendDate(end)
      // }
      // });
      // params: { ... }：這是 GET 請求用來帶參數的方法（會把參數拼在網址後面，像 ?pickupTime=xxx&returnTime=yyy）。
    
    // 將下拉選單的選項，替換成真正可用的車輛
    vehicles.value = response.data;
    
  } catch (error) {
    console.error("查詢可用車輛失敗", error);
    vehicles.value = []; // 失敗時清空
  }
};

// 🌟 2. watch 改成呼叫上面的方法
watch(
  [() => form.value.pickupTime, () => form.value.returnTime, () => form.value.vehicleId],
  async ([newPickup, newReturn, newVehicle], [oldPickup, oldReturn, oldVehicle]) => {
    
    // 情境 A：如果是「時間」改變了，我們必須先去重新找車
    if (newPickup !== oldPickup || newReturn !== oldReturn) {
      if (newPickup && newReturn) {
        // 等待後端把這段時間可用的車輛名單找回來
        await fetchAvailableVehicles(newPickup, newReturn);
        
        // 防呆：檢查選中的車是否還在可用清單內
        const isCurrentCarStillAvailable = vehicles.value.some(v => v.vehicleId === form.value.vehicleId);
        if (!isCurrentCarStillAvailable) {
          form.value.vehicleId = ''; // 被租走了，清空選擇
          return; // 車子被清空了，就不用往下配對方案了，等他重選
        }
      }
    }
    // 情境 B：無論是「時間」改變還是「選車」改變，只要走到這裡，就代表車輛是合法的，可以配對方案了
    if (!isEditMode.value) { // 只有新增訂單時自動配對，避免覆蓋編輯中的舊資料
      const matchResult = getMatchedPlanInfo();
      if (matchResult) {
        form.value.orderType = matchResult.orderType;
        // 改由前端精準配對 planId，不再送 nul 給後端猜
        const matchedPlan = allPlans.find(p => p.type === matchResult.orderType && p.vehicleType === matchResult.vehicleType);
        if (matchedPlan) {
          form.value.planId = matchedPlan.id;
        } else {
          form.value.planId = 11; // 找不到對應車型就給客製化
        }
      }
    }
  }
);
// ----------------------------以上，為了解決一鍵填入時間時抓不到車輛(因為會變)

// ----------------------------以下為了解決方案自動綁定(因為會根據時間和車輛改變)

const getMatchedPlanInfo = () => {
  const { vehicleId, pickupTime, returnTime } = form.value;
  if (!vehicleId || !pickupTime || !returnTime) return null;
  // 1. 找出目前選中的車輛
  const selectedCar = vehicles.value.find(v => v.vehicleId === vehicleId);
  if (!selectedCar || !selectedCar.carModel) return null;
  // 2. 從 CarModel 拿 vehicleType（如：休旅車、小型轎車）
  const vehicleType = selectedCar.carModel.vehicleType;
  if (!vehicleType) return null;
  // 3. 計算天數，判斷長租或短租
  const diffMs = new Date(returnTime) - new Date(pickupTime);
  const diffDays = diffMs / (1000 * 60 * 60 * 24);
  const isLongTerm = diffDays >= 30;
  // 4. 回傳配對結果（planId 交給後端自動配對，前端只決定 orderType）
  return {
    orderType: isLongTerm ? 'LONG_TERM' : 'SHORT_TERM',
    vehicleType: vehicleType
  };
};



// ----------------------------以上為了解決方案自動綁定(因為會根據時間和車輛改變)
onMounted(async () => {
  // 4. 進畫面就去抓客戶跟車輛清單
  try {
    const [custRes, vehRes] = await Promise.all([
      // Promise.all並行執行，同時拿車輛和客戶
      // 修正：對齊 CustomerController
      api.get('/customers'), 
      
      // 修正：對齊 VehicleController (注意是單數 vehicle，沒有 s，也沒有 /all)
      api.get('/vehicle')    
    ]);
    customers.value = custRes.data;
    vehicles.value = vehRes.data;
  } catch (e) {
    console.warn("無法載入客戶或車輛清單", e);
  }
  // 測試客戶 API
  // try {
  //   const custRes = await api.get('/customers');
  //   customers.value = custRes.data;
  //   console.log("客戶載入成功");
  // } catch (e) {
  //   console.error("客戶 API 報錯：", e.response?.data);
  // }

  // // 測試車輛 API
  // try {
  //   const vehRes = await api.get('/vehicle');
  //   vehicles.value = vehRes.data;
  //   console.log("車輛載入成功");
  // } catch (e) {
  //   console.error("車輛 API 報錯：", e.response?.data);
  // }
  

  // 5. 編輯模式載入
  if (isEditMode.value) {
  try {
    // const res = await api.get(`/admin/rentalorders/query/${route.params.id}`);
    const res = await rentalOrderAPI.getById(route.params.id);
    const data = res.data;
    
    // 準確位置：onMounted 內，if (isEditMode.value) 區塊中
    // 這裡新增一個小工具函式來統一處理「代碼提取」
    const toCode = (val) => {
      if (!val) return '';
      // 如果是物件，抓 dbCode 或 name；如果是字串，轉成字串處理
      const s = typeof val === 'object' ? (val.dbCode || val.name || '') : String(val);
      
      // 關鍵字比對法：只要包含中文或英文關鍵字，就轉回對應的 Value
      if (s.includes('日租') || s.includes('SHORT_TERM')) return 'SHORT_TERM';
      if (s.includes('長租') || s.includes('LONG_TERM')) return 'LONG_TERM';
      if (s.includes('未付') || s.includes('UNPAID')) return 'UNPAID';
      if (s.includes('已付訂金') || s.includes('DEPOSIT_PAID')) return 'DEPOSIT_PAID';
      if (s.includes('支付完成') || s.includes('已結清') || s.includes('PAID')) return 'PAID';
      if (s.includes('現金') || s.includes('CASH')) return 'CASH';
      if (s.includes('信用卡') || s.includes('CREDIT_CARD')) return 'CREDIT_CARD';
      if (s.includes('轉帳') || s.includes('TRANSFER')) return 'TRANSFER';
      if (s.includes('行動支付') || s.includes('MOBILE_PAY')) return 'MOBILE_PAY';
      if (s.includes('已預約') || s.includes('RESERVED')) return 'RESERVED';
      if (s.includes('已取車') || s.includes('PICKED_UP')) return 'PICKED_UP';
      if (s.includes('待檢查') || s.includes('已歸還') || s.includes('RETURNED')) return 'RETURNED';
      if (s.includes('已結案') || s.includes('CLOSED')) return 'CLOSED';
      if (s.includes('已取消') || s.includes('CANCELLED')) return 'CANCELLED';
      if (s.includes('逾期') || s.includes('OVERDUE')) return 'OVERDUE';
      
      return s; // 如果都沒對上就回傳原值
    };

form.value = {
  ...data,
  custId: data.customer?.custId,
  vehicleId: data.vehicle?.vehicleId,
  planId: data.rentalPlan?.planId,
  pickupLocationId: data.pickupLocation?.locationId,
  returnLocationId: data.returnLocation?.locationId,

  // 🌟 使用 toCode 進行強制轉換，確保對齊 <select> 的 value
  orderType: toCode(data.orderType),
  payStatus: toCode(data.payStatus),
  depositPayMethod: toCode(data.depositPayMethod),
  balancePayMethod: toCode(data.balancePayMethod),
  orderStatus: toCode(data.orderStatus)
};
    // 編輯模式：反向顯示客戶名字
    if (data.customer) {
      selectedCustName.value = `${data.customer.custName} (${data.customer.custPhone})`;
    }

    form.value.pickupTime = formatToFrontendDate(form.value.pickupTime);
    form.value.returnTime = formatToFrontendDate(form.value.returnTime);
    form.value.orderTime = formatToFrontendDate(form.value.orderTime);
    
    // 同步日租/長租的詳細資料 (如果是日租)
    // 修正點：將 shortTermOrder 改為 shortTermDetail，longTermOrder 改為 longTermDetail
    if (form.value.orderType === 'SHORT_TERM' && data.shortTermDetail) {
      // 處理油量顯示邏輯
      const rawFuel = data.shortTermDetail.fuelLevelReturn;
      let cleanFuel = '';
      if (rawFuel) {
        // 提取代碼（處理物件或字串）並進行關鍵字比對
        cleanFuel = typeof rawFuel === 'object' ? (rawFuel.dbCode || rawFuel.name || '') : String(rawFuel);
        if (cleanFuel.includes('滿油')) cleanFuel = 'FULL';
        if (cleanFuel.includes('半油')) cleanFuel = 'HALF';
        if (cleanFuel.includes('空油')) cleanFuel = 'EMPTY';
      }


      shortTermForm.value = { 
        ...data.shortTermDetail,
        fuelLevelReturn: cleanFuel, // 這裡確保下拉選單能選中
        actualPickupTime: formatToFrontendDate(data.shortTermDetail.actualPickupTime),
        actualReturnTime: formatToFrontendDate(data.shortTermDetail.actualReturnTime)
      };
      
      // (選擇性防呆) 確保油量如果是空值，讓它顯示為「請選擇」
      // 若無資料則設為空字串，顯示「請選擇」
      if (!shortTermForm.value.fuelLevelReturn) {
          shortTermForm.value.fuelLevelReturn = '';
      }
    

    } else if (form.value.orderType === 'LONG_TERM' && data.longTermDetail) {
      longTermForm.value = {
        ...data.longTermDetail,
        actualPickupTime: formatToFrontendDate(data.longTermDetail.actualPickupTime),
        actualReturnTime: formatToFrontendDate(data.longTermDetail.actualReturnTime)
      };
    }
    } catch (e) {
      alert("載入失敗");
      goBack();
    }
    } else {
      form.value.orderTime = minDateTime.value;
    }
  });

const submitForm = async () => {
  // 暫時隱藏發票與合約欄位，先停用合約網址格式驗證
  // if (form.value.contract && !/^https?:\/\/.+/i.test(form.value.contract)) {
  //   alert("合約必須是有效的網址 (需包含 http:// 或 https://)！");
  //   return;
  // }

  try {
    // 1. 準備主訂單基本資料
    const payload = { ...form.value };
    payload.pickupTime = formatToBackendDate(payload.pickupTime);
    payload.returnTime = formatToBackendDate(payload.returnTime);
    payload.orderTime = formatToBackendDate(payload.orderTime);

    // 2. 確保送出的付款方式是後端認得的內容 (純中文)
    if (payload.depositPayMethod === 'CASH') payload.depositPayMethod = '現金';
    if (payload.depositPayMethod === 'CREDIT_CARD') payload.depositPayMethod = '信用卡';
    if (payload.depositPayMethod === 'TRANSFER') payload.depositPayMethod = '轉帳';
    if (payload.depositPayMethod === 'MOBILE_PAY') payload.depositPayMethod = '行動支付';

    if (payload.balancePayMethod === 'CASH') payload.balancePayMethod = '現金';
    if (payload.balancePayMethod === 'CREDIT_CARD') payload.balancePayMethod = '信用卡';
    if (payload.balancePayMethod === 'TRANSFER') payload.balancePayMethod = '轉帳';
    if (payload.balancePayMethod === 'MOBILE_PAY') payload.balancePayMethod = '行動支付';

    // 3. 把明細放進主訂單的肚子裡 (父子套娃結構，前後端完全統一！)
    if (payload.orderType === 'SHORT_TERM') {
      const shortTermData = { ...shortTermForm.value };
      shortTermData.actualPickupTime = formatToBackendDate(shortTermData.actualPickupTime);
      shortTermData.actualReturnTime = formatToBackendDate(shortTermData.actualReturnTime);
      payload.shortTerm = shortTermData; // 塞入肚子裡
    } else if (payload.orderType === 'LONG_TERM') {
      const longTermData = { ...longTermForm.value };
      longTermData.actualPickupTime = formatToBackendDate(longTermData.actualPickupTime);
      longTermData.actualReturnTime = formatToBackendDate(longTermData.actualReturnTime);
      payload.longTerm = longTermData;   // 塞入肚子裡
    }

    // 檢查即將送出的扁平化資料 (不再有 orders 包裝！)
    console.log("準備送出的資料 (已攤平):", payload);

    if (isEditMode.value) {
      // 【修改訂單】
      // await api.put(`/admin/rentalorders/${payload.orderId}`, payload);
      await rentalOrderAPI.update(payload.orderId, payload);
      alert("已更新！");
    } else {
      // 【新增訂單】
      // await api.post('/admin/rentalorders/new', payload);
      await rentalOrderAPI.create(payload);
      alert("已建立！");
    }

    goBack();
    
  } catch (e) {
    console.error("後端回傳的錯誤細節:", e.response?.data || e);
    const errorMsg = e.response?.data?.error || "存檔失敗，請按 F12 查看主控台";
    alert(errorMsg);
  }
};

const goBack = () => router.push('/orders/list');

</script>

<template>
  <div class="container py-4">
    <div class="card shadow-sm border">
      <div class="card-body p-4 p-md-5">
        <div class="d-flex justify-content-between align-items-center mb-4">
          <h3 class="mb-0 fw-bold">{{ isEditMode ? '編輯訂單內容' : '建立新訂單' }}</h3>
          <div class="btn-group">
            <button v-if="!isEditMode" type="button" class="btn btn-sm btn-outline-success" @click="fillMockData">
              ✨ 一鍵填充 (測試用)
            </button>
          </div>
        </div>

        <form @submit.prevent="submitForm">
          <div class="row g-3">
            <!-- <div class="col-md-3">
              <label class="form-label text-muted small mb-1">客戶編號 (ID)</label>
              <input type="number" class="form-control" v-model.number="form.custId" 
                     required min="1" placeholder="請填入客戶編號" @input="validateNumber($event, 'custId')">
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">車輛編號 (ID)</label>
              <input type="number" class="form-control" v-model.number="form.vehicleId" 
                     required min="1" placeholder="請填入車輛編號" @input="validateNumber($event, 'vehicleId')">
            </div> -->

             <div class="col-md-3">
              <label class="form-label text-muted small mb-1">取車時間</label>
              <input type="datetime-local" class="form-control" 
                     v-model="form.pickupTime" required :min="minDateTime">
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">還車時間</label>
              <input type="datetime-local" class="form-control" 
                     v-model="form.returnTime" required :min="minDateTime">
            </div>

            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">取車地點</label>
              <select class="form-select" v-model="form.pickupLocationId" required>
                <option value="">請選擇地點</option>
                <option :value="1">台北總店 (1)</option>
                <option :value="2">新竹分店 (2)</option>
              </select>
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">還車地點</label>
              <select class="form-select" v-model="form.returnLocationId" required>
                <option value="">請選擇地點</option>
                <option :value="1">台北總店 (1)</option>
                <option :value="2">新竹分店 (2)</option>
              </select>
            </div>

            <!-- 新增：搜尋型客戶選擇器 (方案 A) -->
            <div class="col-md-3 position-relative">
              <label class="form-label text-muted small mb-1">選擇客戶</label>
              
              <div v-if="form.custId" class="input-group">
                <input type="text" class="form-control bg-light" :value="selectedCustName" readonly>
                <button v-if="!isEditMode" class="btn btn-outline-secondary" type="button" @click="form.custId = null">
                  重新選擇
                </button>
              </div>
              
              <div v-else>
                <input 
                  type="text" 
                  class="form-control" 
                  v-model="searchQuery" 
                  placeholder="搜尋姓名或電話..." 
                  @focus="showCustDropdown = true"
                >
                <ul 
                  class="dropdown-menu w-100 show shadow-sm" 
                  style="max-height: 200px; overflow-y: auto; position: absolute; z-index: 1050;" 
                  v-show="showCustDropdown"
                >
                  <li v-if="filteredCustomers.length === 0">
                    <span class="dropdown-item text-muted">找不到符合的客戶</span>
                  </li>
                  <li v-for="customer in filteredCustomers" :key="customer.custId">
                    <a class="dropdown-item" href="#" @click.prevent="selectCustomer(customer)">
                      <strong>{{ customer.custName }}</strong> <span class="text-muted small">- {{ customer.custPhone }}</span>
                    </a>
                  </li>
                </ul>
              </div>
            </div>

            <!-- 🌟 新增：車輛選擇下拉選單 -->
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">選擇車輛</label>
              <select class="form-select" v-model="form.vehicleId" required>
                <option value="" disabled>請選擇車輛</option>
                <!-- 🌟 修正：使用 v.carModel?.modelName，並且車牌改成 v.plateNo -->
                <option v-for="v in vehicles" :key="v.vehicleId" :value="v.vehicleId">
                  {{ v.carModel?.modelName || '車型未定' }} - {{ v.plateNo }}
                </option>
              </select>
            </div>
            
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">訂單類型</label>
              <select class="form-select" v-model="form.orderType" required>
                <option value="">請選擇類型</option>
                <option value="SHORT_TERM">日租 (SHORT_TERM)</option>
                <option value="LONG_TERM">長租 (LONG_TERM)</option>
              </select>
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">專案方案</label>
              <select class="form-select" v-model="form.planId" required>
                <option value="">請選擇方案</option>
                <!-- 改用 v-for 動態渲染過濾後的方案 -->
                <option v-for="plan in filteredPlans" :key="plan.id" :value="plan.id">
                  {{ plan.name }}
                </option>
              </select>
            </div>

            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">租金 (Rental)</label>
              <input type="number" step="1" class="form-control" 
                     v-model.number="form.rentalFee" required min="0" placeholder="0">
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">額外費用 (Extra)</label>
              <input type="number" step="1" class="form-control" 
                     v-model.number="form.extraFee" required min="0" placeholder="0">
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">訂金 (Deposit)</label>
              <input type="number" step="1" class="form-control" 
                     v-model.number="form.deposit" required min="0" placeholder="0">
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">總金額 (Total)</label>
              <input type="number" step="1" class="form-control bg-light fw-bold" 
                     v-model.number="form.totalAmount" required min="0">
            </div>

            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">付款狀態</label>
              <select class="form-select" v-model="form.payStatus" >
                <option value="">請選擇付款狀態</option>
                <option value="UNPAID">未付訂金 (UNPAID)</option>
                <option value="DEPOSIT_PAID">已付訂金 (DEPOSIT_PAID)</option>
                <option value="PAID">支付完成 (PAID)</option>
                <option value="REFUNDING">退款中 (REFUNDING)</option>
                <option value="REFUNDED">已退款 (REFUNDED)</option>
              </select>
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">訂金支付</label>
              <select class="form-select" v-model="form.depositPayMethod">
                <option value="" >請選擇支付方式</option>
                <option value="CASH">現金</option>
                <option value="CREDIT_CARD">信用卡</option>
                <option value="TRANSFER">轉帳</option>
                <option value="MOBILE_PAY">行動支付</option>
              </select>
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">尾款支付</label>
              <select class="form-select" v-model="form.balancePayMethod" >
                <option value="" >請選擇支付方式</option>
                <option value="CASH">現金</option>
                <option value="CREDIT_CARD">信用卡</option>
                <option value="TRANSFER">轉帳</option>
                <option value="MOBILE_PAY">行動支付</option>
              </select>
            </div>
            <div class="col-md-3">
              <label class="form-label text-muted small mb-1">訂單狀態</label>
              <select class="form-select" v-model="form.orderStatus" required>
                <option value="" >請選擇訂單狀態</option>
                <option value="RESERVED">已預約 (RESERVED)</option>
                <option value="PICKED_UP">已取車 (PICKED_UP)</option>
                <option value="RETURNED">待檢查 (RETURNED)</option>
                <option value="OVERDUE">逾期未還 (OVERDUE)</option>
                <option value="CLOSED">已結案 (CLOSED)</option>
                <option value="CANCELLED">已取消 (CANCELLED)</option>
              </select>
            </div>

            <!-- 暫時隱藏發票與合約欄位
            <div class="col-md-4">
              <label class="form-label text-muted small mb-1">發票 ID (選填)</label>
              <input type="number" class="form-control" v-model.number="form.invoiceId" placeholder="僅限數字">
            </div>
            <div class="col-md-8">
              <label class="form-label text-muted small mb-1">合約圖片網址 (選填)</label>
              <input type="url" class="form-control" v-model="form.contract" 
                     placeholder="http://.../image.jpg">
            </div>
            -->
          </div>

          <hr class="my-4">

          <!-- ---------以下短租與長租地動態欄位 -->

        <!-- 短租專屬資訊區塊 -->
          <div v-if="form.orderType === 'SHORT_TERM'" class="p-4 bg-light rounded border border-info mb-4">
            <h5 class="fw-bold text-info mb-3"><i class="bi bi-car-front"></i> 日租詳細資訊</h5>
            <div class="row g-3">
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">實際取車時間</label>
                <input type="datetime-local" class="form-control" v-model="shortTermForm.actualPickupTime">
              </div>
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">實際還車時間</label>
                <input type="datetime-local" class="form-control" v-model="shortTermForm.actualReturnTime">
              </div>
              <div class="col-md-2">
                <label class="form-label text-muted small mb-1">出車里程</label>
                <input type="number" class="form-control" v-model.number="shortTermForm.startMileage" min="0">
              </div>
              <div class="col-md-2">
                <label class="form-label text-muted small mb-1">還車里程</label>
                <input type="number" class="form-control" v-model.number="shortTermForm.endMileage" min="0">
              </div>
              <div class="col-md-2">
                <label class="form-label text-muted small mb-1">還車油量</label>
                <select class="form-select" v-model="shortTermForm.fuelLevelReturn">
                  <option value="">請選擇</option>
                  <option value="FULL">滿油</option>
                  <option value="HALF">半油</option>
                  <option value="EMPTY">空油</option>
                </select>
              </div>
              <div class="col-12">
                <label class="form-label text-muted small mb-1">備註說明</label>
                <textarea class="form-control" v-model="shortTermForm.noteDesc" rows="2"></textarea>
              </div>
            </div>
          </div>

          <!-- 長租專屬資訊區塊 -->
          <div v-if="form.orderType === 'LONG_TERM'" class="p-4 bg-light rounded border border-success mb-4">
            <h5 class="fw-bold text-success mb-3"><i class="bi bi-building"></i> 長租詳細資訊</h5>
            <div class="row g-3">
              <!-- 第一排：4個欄位，各佔 col-md-3 (四分之一) -->
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">實際取車時間</label>
                <input type="datetime-local" class="form-control" v-model="longTermForm.actualPickupTime">
              </div>
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">實際還車時間</label>
                <input type="datetime-local" class="form-control" v-model="longTermForm.actualReturnTime">
              </div>
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">合約月數</label>
                <input type="number" class="form-control" v-model.number="longTermForm.contractMonths" min="1">
              </div>
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">每月租金</label>
                <input type="number" class="form-control" v-model.number="longTermForm.monthlyPayment" min="0">
              </div>
              
              <!-- 第二排：4個欄位，各佔 col-md-3 (四分之一) -->
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">請款日</label>
                <input type="number" class="form-control" v-model.number="longTermForm.billingDay" min="1" max="31">
              </div>
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">已付月數</label>
                <input type="number" class="form-control" v-model.number="longTermForm.paidMonths" min="0">
              </div>
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">出車里程</label>
                <input type="number" class="form-control" v-model.number="longTermForm.startMileage" min="0">
              </div>
              <div class="col-md-3">
                <label class="form-label text-muted small mb-1">還車里程</label>
                <input type="number" class="form-control" v-model.number="longTermForm.endMileage" min="0">
              </div>
              
              <!-- 第三排：備註說明 (佔滿整行 col-12) -->
              <div class="col-12">
                <label class="form-label text-muted small mb-1">備註說明</label>
                <textarea class="form-control" v-model="longTermForm.noteDesc" rows="2"></textarea>
              </div>
            </div>
          </div>

          <!-- ---------以上短租與長租地動態欄位 -->

          <div class="d-flex justify-content-end">
            <button type="button" class="btn btn-outline-secondary me-3 px-4" @click="goBack">取消</button>
            <button type="submit" class="btn btn-primary px-5">
              {{ isEditMode ? '確認更新' : '建立訂單' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>



<style scoped>

</style>


