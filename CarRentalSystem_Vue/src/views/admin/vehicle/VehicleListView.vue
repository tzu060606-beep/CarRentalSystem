<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
// 引入 Store 與 API
import { useVehicleStore } from '@/store/vehicle/vehicleStore';
import { locationAPI } from '@/api/vehicle/locationApi'; 
import { carModelAPI } from '@/api/vehicle/carModelApi';
import { vehicleAPI } from '@/api/vehicle/vehicleApi';
import VehicleFormModal from '@/components/admin/vehicle/VehicleFormModal.vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import AlertToast from '@/components/common/AlertToast.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';

const router = useRouter();
const vehicleStore = useVehicleStore();

const activeDropdown = ref(null);
const formModalRef = ref(null);

// --- alertToast & confirmDialog ---
const toastRef = ref(null);
const confirmDialog = ref({ isVisible: false, message: '', action: null });
const showConfirm = (message, action) => {
  confirmDialog.value = { isVisible: true, message, action };
};
const handleConfirm = async () => {
  confirmDialog.value.isVisible = false;
  if (confirmDialog.value.action) {
    await confirmDialog.value.action();
  }
};

// 切換選單的方法
const toggleDropdown = (id) => {
  // 如果點擊同一個就關閉，點擊不同的就打開那個
  activeDropdown.value = activeDropdown.value === id ? null : id;
};

// 存車輛
const vehicle = ref({
    vehicleId: null, //identity是否移除？NO
    plateNo: '',
    locationId: '',
    status: '',
    modelId: '',
    color: '',
    manufactureDate: '',
    issuedDate: '',
    inspectionDate: '',
    mileage: '',
    nextMaintainenceMileage: '',
    description: '',
});

// 存車款列表
const carModels = ref([]);
const locations = ref([]); // 存放下拉選單的據點資料

// ============== 上方「關鍵字搜尋」和「雙層選單搜尋區」==============
// --- 1. 篩選器綁定的變數 ---
const searchQuery = ref('');
const filterCategory = ref(''); // 第一層父選單
const filterValue = ref(''); // 第二層子選單

// 父選單「篩選依據」切換時，必須清空第二層「條件值」，避免邏輯錯亂
watch(filterCategory, () => {
  filterValue.value = '';
  currentPage.value = 1 ; // 條件切換時，強制將分頁切回第一頁
});

// --- 動態選項計算(車色) ---
const uniqueColors = computed(() => {
  const colors = vehicleStore.vehicles.map(v => v.color).filter(Boolean); //過濾掉null和空白
  return [...new Set(colors)];
});

// --- 2. 狀態與徽章顏色對應表 ---
const statusMap = {
  'CLEANING': { 
    text: '場內(整理中)', 
    colorClass: 'vehicle-badge-cleaning' 
  }, 
  'AVAILABLE': { 
    text: '場內(可接單)', 
    colorClass: 'vehicle-badge-available' 
  },
  'RENTING': { 
    text: '出租中', 
    colorClass: 'vehicle-badge-renting' 
  },
  'MAINTAINING': { 
    text: '維修中', 
    colorClass: 'vehicle-badge-maintaining' 
  }, 
  'DISPATCHING': { 
    text: '調度中', 
    colorClass: 'vehicle-badge-dispatching' 
  },
  'SHUTTLING': { 
    text: '專車接送中', 
    colorClass: 'vehicle-badge-shuttling' 
  }, 
  'RETIRED': { 
    text: '退役待處置', 
    colorClass: 'vehicle-badge-retired'
  },
};

// --- 3. 核心亮點：前端即時多條件篩選 ---
// 使用 computed，只要 searchQuery, filterCategorytus, filterValue 一改變，畫面瞬間更新
const filteredVehicles = computed(() => {
  return vehicleStore.vehicles.filter(vehicle => {
    // 條件 A: 關鍵字模糊搜尋 (轉小寫比對車牌或型號)
    const keyword = searchQuery.value.toLowerCase().trim();
    const plateMatch = (vehicle.plateNo || '').toLowerCase().includes(keyword);
    const modelMatch = vehicle.carModel ? 
      (vehicle.carModel.brand + ' ' + vehicle.carModel.modelName).toLowerCase().includes(keyword) : false;
    const matchSearch = !keyword || plateMatch || modelMatch;
    
    // 條件 B: 動態條件篩選
    let matchDynamicFilter = true;
    if (filterCategory.value && filterValue.value) {
      const val = filterValue.value;
      switch (filterCategory.value) {
        case 'location':
          matchDynamicFilter = (vehicle.location?.locationId == val); // 相同為true,不同為false
          break;

        case 'status':
          const currentStatus = vehicle.status?.dbCode || vehicle.status;
          matchDynamicFilter = (currentStatus === val); // 相同為true,不同為false
          break;
      
        case 'color':
          matchDynamicFilter = (vehicle.color === val); // 相同為true,不同為false
          break;
      
        case 'inspectionDate':
          if (!vehicle.inspectionDate) {
            matchDynamicFilter = false;
            break;
          }
          //  計算差距天數：驗車日 - 今天 (單位：毫秒->天)
          const daysToInspect = ( new Date(vehicle.inspectionDate) - new Date() ) / (1000 * 60 * 60 * 24); 
          // 間距一：過期未驗＋一個月內
          if (val === 'overdue') matchDynamicFilter = daysToInspect <= 0; 
          // 間距一：過期未驗＋一個月內
          else if (val === '1m') matchDynamicFilter = daysToInspect > 0 && daysToInspect <= 30; 
          // 間距三：超過一個月～半年內
          else if (val === '6m') matchDynamicFilter = daysToInspect > 30 && daysToInspect <= 180;
          // 間距四：超過半年
          else if (val === 'safe') matchDynamicFilter = daysToInspect > 180;
          break;

        case 'mileage':
          const currentMile = Number(vehicle.mileage) || 0;
          if (val === 'low') matchDynamicFilter = currentMile <= 50000;
          else if (val === 'mid') matchDynamicFilter = currentMile > 50000 && currentMile <= 100000;
          else if (val === 'high') matchDynamicFilter = currentMile > 100000;
          break;
        
        case 'nextMaintainenceMileage':
          const currentMileage = Number(vehicle.mileage) || 0;
          const nextMileage = Number(vehicle.nextMaintainenceMileage) || 0;
          const gap = nextMileage - currentMileage; // 剩餘可跑里程

          // 分級一：已超標未檢
          if (val === 'urgent') matchDynamicFilter = gap <= 0;
          else if (val === 'soon') matchDynamicFilter = gap > 0 && gap <= 1000;
          else if (val === 'safe') matchDynamicFilter = gap > 1000;
          break;
      }
    }
    // A和B條件皆相符時，才會回傳結果
    return matchSearch && matchDynamicFilter;
  });
});
// 如果 searchQuery 有變動，一樣重置分頁
watch([searchQuery, filterCategory, filterValue], () => {
  currentPage.value = 1;
})
// =======================================================================

// 刪除車輛
const handleDelete = async (id) => {
  // Store 裡面的 deleteVehicle 已經包含 confirm 和 reload 了
  await vehicleStore.deleteVehicle(id);
};

// 日期格式化
const formatDate = (dateString) => {
  if (!dateString) return '無資料';
  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// Modal新增表單區:當子元件 Modal 存檔成功時，觸發此方法重新抓取清單
const refreshList = () => {
    vehicleStore.fetchVehicles();
};

// ============ 表頭排序區(必須在分頁區功能之下) ============
const sortKey = ref('plateNo'); //預設排序欄位(如：單號)
const sortOrder = ref('asc'); //排序方式：asc or desc

// 處理點擊表頭事件
const handleSort = (key) => {
  if (sortKey.value === key) {
    // 若點擊同一欄位，就切換排序方式(升/降冪)
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    // 若點擊不同欄位，依該欄位升冪排序
    sortKey.value = key;
    sortOrder.value = 'asc';
  }
};

// 計算排序後的資料
const sortedVehicles = computed (() => {
  // 複製一份新陣列，避免 mutate 原始資料
  const result = [...filteredVehicles.value];

  return result.sort((a, b) => {
    let valA = a[sortKey.value];
    let valB = b[sortKey.value];

    // 若依品牌排序，需抓內層物件
    if (sortKey.value === 'brand') {
      valA = a.carModel?.brand || '';
      valB = b.carModel?.brand || '';
    }

    // 若依型號排序，需抓內層物件
    if (sortKey.value === 'modelName') {
      valA = a.carModel?.modelName || '';
      valB = b.carModel?.modelName || '';
    }

    // 若依所在據點排序，需抓內層物件
    if (sortKey.value === 'location') {
      valA = a.location?.locationName || '';
      valB = b.location?.locationName || '';
    }

    // 若依狀態排序，需抓內層物件
    if (sortKey.value === 'status') {
      valA = a.status?.description || a.status?.dbCode || '';
      valB = b.status?.description || b.status?.dbCode || '';
    }

    // 若依里程排序，需先強制轉換為數字再比較
    if (sortKey.value === 'mileage') {
      valA = Number(valA) || 0;
      valB = Number(valB) || 0;
    }
    
    //  若是日期時間格式，轉換為時間戳記來比大小
    if (sortKey.value === 'inspectionDate') {
      // 加上||0，確保即使解析失敗或遇到null，也會變成數值0，不會噴出NaN搞壞排序
      valA = valA ? new Date(valA).getTime() || 0 : 0;
      valB = valB ? new Date(valB).getTime() || 0 : 0;
    }

    // 若依座位數排序，需先強制轉換為數字再比較
    // if (sortKey.value === 'seats') {
    //   valA = Number(a.carModel?.seats) || 0;
    //   valB = Number(b.carModel?.seats) || 0;
    // }
    
    // 通用防呆：若AB兩者完全相等，則不改變排序
    if (valA === valB) return 0;
    if (valA === null) return 1;
    if (valB === null) return -1;

    if (valA < valB) return sortOrder.value === 'asc' ? -1 : 1;
    if (valA > valB) return sortOrder.value === 'asc' ? 1 : -1;
    return 0;
  })
});
// =========================================

// =============== 分頁功能區 ===============
// 1. 定義當前頁碼與每頁筆數
const currentPage = ref(1);
const pageSize = 10;

// 2. 切換頁籤時，必須把頁碼重置回第1頁，否則會發生找不到資料的bug
// watch(currentTab, () => {
//   currentPage.value = 1;
//   // fetchPageData();
// });

// 3. 核心：計算出「當前這一頁」該顯示的資料陣列
const paginatedVehicles = computed(() => {
  if (!sortedVehicles.value) return [];
  const startIndex = (currentPage.value - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  // 用 slice 切割陣列，如第一頁就是 slice(0, 10)
  return sortedVehicles.value.slice(startIndex, endIndex);
  // return filteredVehicles.value.slice(startIndex, endIndex);
});

// 4. 計算總頁數（給分頁按鈕用）
const totalPages = computed(() => {
  if (!filteredVehicles.value || filteredVehicles.value.length === 0) return 1;
  return Math.ceil(filteredVehicles.value.length / pageSize);
});

// 5. 換頁方法
const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    // fetchPageData(); //頁碼改變，重新打API
  }
};
// =========================================

// ============== 車輛退役邏輯 ==============
// 判斷是否達到退役標準(達標才可點按鈕)
const canRetire = (vehicle) => {
  const currentStatus = vehicle.status?.dbCode || vehicle.status;
  if (currentStatus === 'RETIRED'  || currentStatus === 'RENTING' || currentStatus === 'SHUTTLING' || currentStatus === 'DISPATCHING') return false; // 已退役者略過

  // 條件1: 里程數達120,00公里
  const mileage = Number(vehicle.mileage) || 0;
  if (mileage >= 120000) return true;

  // 條件2: 車齡達4.5年(以發照日 issuedDate 起算)
  if (vehicle.issuedDate) {
    const issuedDate = new Date(vehicle.issuedDate);
    const today = new Date();
    const ageInYears = (today - issuedDate) / (1000 * 60 * 60 * 24 * 365.25);

    if (ageInYears >= 4.5) return true;
  }
  return false; // 兩個退役標準都未達則不可點
};

// 執行除役動作
const handleRetire = async (vehicle) => {
// 防呆確認彈窗
const confirmMsg = `警告：確定要將車輛【${vehicle.plateNo}】轉換為「退役待處置」嗎？\n執行後將無法回復`;
console.log(vehicle);

  // if (confirm(confirmMsg)) {
  showConfirm(confirmMsg, async () => {
    try {
      await vehicleAPI.updateStatus(vehicle.vehicleId, 'RETIRED');
      // alert(`車輛【${vehicle.plateNo}】已轉為除役！`);
      toastRef.value?.show(`車輛【${vehicle.plateNo}】已轉為除役！`, 'success');
      // 重新抓取列表資料重整畫面
      refreshList();
    } catch (error) {
      console.error("除役操作失敗：", error);
      // const errMsg = error.response?.data?.fieldErrors || '請檢查系統連線或稍後再試';
      // alert(`除役失敗：${errMsg}`);

      // 1. 設定最基礎的防呆底線預設文字
      let finalAlertMsg = '發生未知的錯誤，請檢查系統連線或稍後再試。';

      // 2. 判斷是否有收到後端回傳的 response
      if (error.response && error.response.data) {
        const responseData = error.response.data;

        // A. 抓取「主要標題」 (可能是 Spring 預設的 message/error，或是我們自訂的 message)
        let mainMsg = responseData.message || responseData.error || '';

        // B. 抓取「詳細欄位錯誤」 (處理我們自訂的 fieldErrors 物件)
        let detailMsg = '';
        if (responseData.fieldErrors && typeof responseData.fieldErrors === 'object') {
          // Object.values 會把 { "plateNo": "不可空白", "mileage": "需大於0" } 
          // 變成陣列 ["不可空白", "需大於0"]，然後用換行符號(\n)串聯起來
          detailMsg = Object.values(responseData.fieldErrors).join('\n');
        }

        // C. 智慧組合訊息：有什麼就印什麼
        if (mainMsg && detailMsg) {
          finalAlertMsg = `${mainMsg}\n\n【詳細原因】\n${detailMsg}`;
        } else if (detailMsg) {
          finalAlertMsg = detailMsg;
        } else if (mainMsg) {
          finalAlertMsg = mainMsg;
        }
      } else if (error.message) {
        // 3. 如果連 response 都沒有 (例如 Axios 遇到純粹的 Network Error 斷網)
        finalAlertMsg = error.message;
      }
      // 4. 最終優雅地彈出警告
      alert(`操作失敗：\n${finalAlertMsg}`);
      toastRef.value?.show(`操作失敗：${finalAlertMsg}`, 'danger');
    }
  });
};

// =========================================

// 初始化載入資料
onMounted(async () => {
  vehicleStore.fetchVehicles(); // 抓取車輛清單

  // 抓取據點供篩選下拉選單使用
  try {
    const locRes = await locationAPI.getAll();
    const modelRes = await carModelAPI.getAll();
    locations.value = locRes.data;
    carModels.value = modelRes.data;
  } catch (error) {
    console.error("無法抓取據點列表：", error);
  }

  // 點擊畫面其他地方時，關閉選單 (選做，但能提升 UX)
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.dropdown')) {
      activeDropdown.value = null;
    }
  });
});
</script>

<template>
  <div class="container-fluid py-4 vehicle-page-bg">
    
    <div class="d-flex align-items-center mb-4 text-muted">
      <span class="cursor-pointer vehicle-breadcrumb-link fw-bold" role="button" @click="router.push('/dispatch/dashboard')">
        <font-awesome-icon icon="fa-solid fa-headset" style="width: 1.25rem; font-size: 0.9rem;"/> 車輛調度中心
      </span>
      <i class="fa-solid fa-chevron-right mx-2 small"></i>
      <span class="text-dark fw-bold">車輛管理</span>
    </div>

    <!-- Page Header -->
    <div class="mb-4 d-flex justify-content-between align-items-end">
      <div>
        <h2 class="fw-bold mb-1 vehicle-page-title">車輛管理</h2>
        <p class="mb-0 small vehicle-page-subtitle">車輛狀態、維修保養、排程管理</p>
      </div>

    </div>

    <!-- 搜尋與篩選工具列 -->
    <div class="card border-0 mb-4 vehicle-search-card">
      <div class="card-body p-4">
        <div class="row g-3 align-items-end">
          
          <!-- 模糊搜尋 -->
          <div class="col-md-4">
            <label class="form-label text-muted small fw-bold mb-1">關鍵字查詢</label>
            <div class="input-group">
              <span class="input-group-text bg-white border-end-0">
                <i class="fa-solid fa-magnifying-glass text-muted"></i>
              </span>
              <input v-model="searchQuery" type="text" 
               class="form-control border-start-0 ps-0" 
               placeholder="輸入車牌或車款型號...">
            </div>
          </div>

          <!-- 選單搜尋區：父選單查詢依據欄位 -->
          <div class="col-md-3">
            <label class="form-label text-muted small fw-bold mb-1">篩選條件</label>
            <select v-model="filterCategory" class="form-select fw-bold bg-light">
              <option value="">全部車輛</option>
              <option value="location">所在據點</option>
              <option value="status">車輛狀態</option>
              <option value="color">車色</option>
              <option value="inspectionDate">預計驗車日</option>
              <option value="mileage">總里程數</option>
              <option value="nextMaintainenceMileage">保養狀態(距下次保養)</option>
            </select>
          </div>

          <!-- 選單搜尋區：子選單查詢值 -->
          <div class="col-md-3">
            <label class="form-label text-muted small fw-bold mb-1">選擇項目</label>
            <select v-if="!filterCategory" v-model="filterValue" class="form-select text-muted" disabled>
              <option>請先選擇篩選條件</option>
            </select>

            <select v-else-if="filterCategory === 'location'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇據點</option>
              <option v-for="loc in locations" :key="loc.locationId" :value="loc.locationId">
                {{ loc.locationName }}
              </option>
            </select>

            <select v-else-if="filterCategory === 'status'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇車輛狀態</option>
              <option v-for="(info, key) in statusMap" :key="key" :value="key">
                {{ info.text }}
              </option>
            </select>

            <select v-else-if="filterCategory === 'color'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇車色</option>
              <option v-for="color in uniqueColors" :key="color" :value="color">
                {{ color }}
              </option>
            </select>

            <select v-else-if="filterCategory === 'inspectionDate'" v-model="filterValue" class="form-select">
              <option value="" disabled>距離驗車日尚有...</option>
              <option value="overdue" class="text-danger fw-bold">已逾期</option>
              <option value="1m" class="text-danger fw-bold">一個月內</option>
              <option value="6m">半年內</option>
              <option value="safe">半年以上</option>
            </select>

            <select v-else-if="filterCategory === 'mileage'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇區間</option>
              <option value="low">50,000 公里以內 (低里程)</option>
              <option value="mid">50,001 - 100,000 公里</option>
              <option value="high">100,000 公里以上 (高里程)</option>
            </select>

            <select v-else-if="filterCategory === 'nextMaintainenceMileage'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇區間</option>
              <option value="urgent" class="text-danger">已超標</option>
              <option value="soon" class="text-warning">即將保養</option> 
              <option value="safe">暫不需保養</option>
            </select>
          </div>

          <!-- 新增的按鈕：利用 data-bs-toggle 直接呼叫 Modal -->
          <div class="col-md-2 text-end">
            <!-- <button class="btn btn-primary w-100 h-100 d-flex justify-content-center align-items-center" @click="formModalRef.openModal()">
                <i class="fa-solid fa-plus"></i>  新增車輛
            </button> -->
            <button class="btn fw-bold d-flex align-items-center vehicle-btn-primary" @click="formModalRef.openModal(null, 'add')">
             <i class="fa-solid fa-file-circle-plus me-2"></i>新增車輛
            </button>
          </div>

        </div>
      </div>
    </div>

    <!-- 資料表格區塊 -->
    <div class="card border-0 overflow-hidden vehicle-table-card">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th class="py-3 px-4 text-muted small vehicle-cursor-pointer text-nowrap" @click="handleSort('plateNo')">
                車牌 
                <span v-if="sortKey === 'plateNo'" class="ms-1">
                  <i class="fa-solid" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down'"></i>
                </span>
                <span v-else class="ms-1 opacity-50"><i class="fa-solid fa-sort"></i></span>
              </th>
              <th class="py-3 px-4 text-muted small vehicle-cursor-pointer text-nowrap" @click="handleSort('brand')">
                品牌
                <span v-if="sortKey === 'brand'" class="ms-1">
                  <i class="fa-solid" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down'"></i>
                </span>
                <span v-else class="ms-1 opacity-50"><i class="fa-solid fa-sort"></i></span>
              </th>
              <th class="py-3 px-4 text-muted small vehicle-cursor-pointer text-nowrap" @click="handleSort('modelName')">
                型號
                <span v-if="sortKey === 'modelName'" class="ms-1">
                  <i class="fa-solid" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down'"></i>
                </span>
                <span v-else class="ms-1 opacity-50"><i class="fa-solid fa-sort"></i></span>
              </th>
              <th class="py-3 px-4 text-muted small vehicle-cursor-pointer text-nowrap" @click="handleSort('status')">
                狀態
                <span v-if="sortKey === 'status'" class="ms-1">
                  <i class="fa-solid" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down'"></i>
                </span>
                <span v-else class="ms-1 opacity-50"><i class="fa-solid fa-sort"></i></span>
              </th>
              <th class="py-3 px-4 text-muted small vehicle-cursor-pointer text-nowrap" @click="handleSort('location')">
                所在據點
                <span v-if="sortKey === 'location'" class="ms-1">
                  <i class="fa-solid" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down'"></i>
                </span>
                <span v-else class="ms-1 opacity-50"><i class="fa-solid fa-sort"></i></span>
              </th>
              <th class="py-3 px-4 text-muted small vehicle-cursor-pointer text-nowrap" @click="handleSort('mileage')">
                里程數
                <span v-if="sortKey === 'mileage'" class="ms-1">
                  <i class="fa-solid" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down'"></i>
                </span>
                <span v-else class="ms-1 opacity-50"><i class="fa-solid fa-sort"></i></span>
              </th>
              <th class="py-3 px-4 text-muted small vehicle-cursor-pointer text-nowrap" @click="handleSort('inspectionDate')">
                下次驗車日
                <span v-if="sortKey === 'inspectionDate'" class="ms-1">
                  <i class="fa-solid" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down'"></i>
                </span>
                <span v-else class="ms-1 opacity-50"><i class="fa-solid fa-sort"></i></span>
              </th>

              <th class="py-3 px-4 text-center text-muted small text-nowrap">操作</th>
              <th class="py-3 px-4 text-end text-muted small">查看詳情</th>
            </tr>
          </thead>
          <tbody class="border-top-0">
            <!-- 改用 paginatedVehicles 迴圈 -->
            <tr v-for="vehicle in paginatedVehicles" :key="vehicle.vehicleId">
              <td class="py-3 px-4 fw-bold">{{ vehicle.plateNo }}</td>
              <td class="py-3 px-4">
                {{ vehicle.carModel?.brand }}
              </td>
              <td class="py-3 px-4">
                {{ vehicle.carModel?.modelName }}
              </td>
              <td class="py-3 px-4">
                <!-- 動態綁定 Bootstrap Badge 顏色 -->
                <span class="badge rounded-pill px-3 py-2" :class="statusMap[vehicle.status.dbCode]?.colorClass || 'text-bg-secondary'">
                  {{ statusMap[vehicle.status]?.text || vehicle.status.description }}
                </span>
              </td>
              <td class="py-3 px-4 text-secondary">
                {{ vehicle.location?.locationName || '無資料' }}
              </td>
              <td class="py-3 px-4">
                {{ Number(vehicle.mileage).toLocaleString() }}
              </td>
              <td class="py-3 px-4">
                {{ formatDate(vehicle.inspectionDate) }}
              </td>

              <!-- 操作按鈕區 -->
              <td class="py-3 px-4">
                <div class="d-flex justify-content-center gap-2">
                  <button class="btn btn-sm btn-outline-warning fw-bold shadow-sm"
                  :hidden="vehicle.status?.dbCode == 'RETIRED' || vehicle.status?.dbCode == 'MAINTAINING'"
                   @click=""
                   title="點擊可建立維修單，車輛狀態不會跳轉">
                    <FontAwesomeIcon icon="fa-solid fa-wrench" /> 保養
                  </button>
                  <button class="btn btn-sm fw-bold shadow-sm"
                  v-if="vehicle.status?.dbCode != 'RETIRED'"
                   :class="canRetire(vehicle) ? 'btn-outline-danger' : 'btn-outline-secondary opacity-50'"
                   :disabled="!canRetire(vehicle)"
                   @click="handleRetire(vehicle)"
                   :title="canRetire(vehicle) ? '達到退役標準，可點擊執行，車輛狀態將立即跳轉' : '未達退役標準'">
                    <FontAwesomeIcon icon="fa-solid fa-ban" v-if="canRetire(vehicle)" /> 
                    {{ canRetire(vehicle) ? '除役' : '未達除役標準' }}
                  </button>
                </div>
              </td>
              <td class="py-3 px-4 text-end">
                <!-- Bootstrap 5 Dropdown 取代原本的單一按鈕 -->
                <div class="dropdown" @click.stop="toggleDropdown(vehicle.vehicleId)">
                  <button class="btn btn-sm btn-light" type="button" @click.prevent="router.push('/vehicle/detail/' + vehicle.vehicleId)">
                    <i class="fa-solid fa-ellipsis-vertical"></i>
                  </button>
                </div>
              </td>
            </tr>

            <!-- 查無資料的顯示 -->
            <tr v-if="filteredVehicles.length === 0">
              <td colspan="6" class="text-center py-5 text-muted">
                <i class="fa-solid fa-car-burst fs-1 mb-3 text-light"></i>
                <p class="mb-0">沒有找到符合條件的車輛</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 分頁 Footer (純切版，需另外寫邏輯) -->
      <div class="card-footer bg-white border-top py-3 px-4 d-flex justify-content-center align-items-center position-relative" v-if="totalPages > 1">

        <nav aria-label="Page navigation">
          <ul class="pagination pagination-sm mb-0">
            
            <li class="page-item" :class="{ disabled: currentPage === 1 }">
              <a class="page-link cursor-pointer" @click="changePage(currentPage - 1)">
                <i class="fa-solid fa-caret-left"></i>
              </a>
            </li>

            <li v-for="page in totalPages" 
            class="page-item" 
            :key="page" 
            :class="{ active: currentPage === page }">
              <a class="page-link cursor-pointer" @click="changePage(page)">{{ page }}</a>
            </li>

            <li class="page-item" :class="{ disabled: currentPage === totalPages }">
              <a class="page-link cursor-pointer" @click="changePage(currentPage + 1)">
                <i class="fa-solid fa-caret-right"></i>
              </a>
            </li>
            
          </ul>
        </nav>

        <span class="position-absolute end-0 me-4 text-muted small">
          共計 {{ filteredVehicles.length }} 筆資料
        </span>

      </div>

      <div class="card-footer bg-white border-top py-3 px-4 text-end" v-else>
        <span class="text-muted small">共計 {{ filteredVehicles.length }} 筆資料</span>
      </div>
    </div>

  </div>
  <VehicleFormModal ref="formModalRef" @saved="refreshList" />
  <AlertToast ref="toastRef" />
  <ConfirmDialog 
      :isVisible="confirmDialog.isVisible" 
      :message="confirmDialog.message" 
      @confirm="handleConfirm" 
      @cancel="confirmDialog.isVisible = false" 
    />
</template>

<style scoped>
/* ============================================================
   VehicleListView — OneRent Design System
   所有色碼、間距、圓角皆使用 variables-3.css 定義的 CSS 變數
   自訂 class 一律加上 vehicle- 前綴
   ============================================================ */

/* ── 頁面底色 ── */
.vehicle-page-bg {
  background-color: var(--color-bg-page);
  min-height: 100vh;
}

/* ── 麵包屑連結 ── */
.vehicle-breadcrumb-link {
  color: var(--color-primary);
  transition: color var(--transition-fast);
}
.vehicle-breadcrumb-link:hover {
  color: var(--color-primary-hover);
}

/* ── 頁面標題 ── */
.vehicle-page-title {
  color: var(--color-primary);
  letter-spacing: -0.02em;
  font-family: var(--font-sans);
}
.vehicle-page-subtitle {
  color: var(--color-text-secondary);
}

/* ── 搜尋工具列卡片 ── */
.vehicle-search-card {
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
}

/* ── 主要操作按鈕（新增車輛）── */
.vehicle-btn-primary {
  background-color: var(--color-primary);
  color: var(--color-text-inverse);
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
}
.vehicle-btn-primary:hover {
  background-color: var(--color-primary-hover);
  color: var(--color-text-inverse);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* ── 表格外層卡片 ── */
.vehicle-table-card {
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
}

/* ── 表格 hover 效果（Alice Blue 品牌淡藍）── */
.vehicle-table-card .table-hover tbody tr:hover {
  background-color: var(--color-primary-light) !important;
}

/* ── 排序欄位游標 ── */
.vehicle-cursor-pointer {
  cursor: pointer;
  user-select: none;
}

/* ── 車輛狀態 Badge（淺底配深字）── */
/* 清理中：沙色 */
.vehicle-badge-cleaning {
  background-color: var(--color-accent-light);
  color: var(--color-sandy-800);
}
/* 可接單：綠 */
.vehicle-badge-available {
  background-color: var(--color-success-bg);
  color: var(--color-success);
}
/* 出租中：藍 */
.vehicle-badge-renting {
  background-color: var(--color-primary-light);
  color: var(--color-primary);
}
/* 維修中：紅 */
.vehicle-badge-maintaining {
  background-color: var(--color-danger-bg);
  color: var(--color-danger);
}
/* 調度中：黃 */
.vehicle-badge-dispatching {
  background-color: var(--color-warning-bg);
  color: var(--color-amber-800);
}
/* 專車接送中：青綠 */
.vehicle-badge-shuttling {
  background-color: var(--color-teal-bg);
  color: var(--color-teal);
}
/* 退役待處置：灰底深灰字 */
.vehicle-badge-retired {
  background-color: var(--color-gray-200);
  color: var(--color-gray-700);
}
</style>
