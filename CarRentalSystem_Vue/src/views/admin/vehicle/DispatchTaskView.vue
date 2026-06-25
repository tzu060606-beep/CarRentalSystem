<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useDispatchStore } from '@/store/vehicle/dispatchLogStore';
import { dispatchAPI } from '@/api/vehicle/dispatchLogApi';
import { locationAPI } from '@/api/vehicle/locationApi';
import { carModelAPI } from '@/api/vehicle/carModelApi';
import { vehicleAPI } from '@/api/vehicle/vehicleApi';
import { driverAPI } from '@/api/vehicle/driverTestApi';
import { employeeAPI } from '@/api/login/employeeApi';
import DispatchFormModal from '@/components/admin/vehicle/DispatchFormModal.vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import AlertToast from '@/components/common/AlertToast.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';

const router = useRouter();
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

// 存後端傳來資料
// const dispatchLogs = ref([]);
// 使用storeToRefs 將 Pinia Store 中的 state 變成響應式的 ref
// 這樣只要 fetchDispatches()抓完整資料更新Store，畫面就會瞬間連動更新
const dispatchStore = useDispatchStore();
const { dispatchLogs: dispatchLogs } = storeToRefs(dispatchStore);
// 選單用的參考資料，for下拉選單<select>抓取
const locations = ref([]);
const carmodels = ref([]);
const vehicles = ref([]);
const drivers = ref([]);
const employees = ref([]);
const formModalRef = ref(null);

// Modal新增表單區：當子元件 Modal 存檔成功時，觸發此方法重新抓取清單
const refreshList = () => {
    dispatchStore.fetchDispatches();
    // fetchPageData();
    // fetchCounts();
}
// -----------------------------------------
// 1. 搜尋與篩選工具列
// -----------------------------------------
const searchQuery = ref('');
const filterCategory = ref('');   // 父選單：fromLocation / toLocation / modelName / driver / status
const filterValue = ref('');      // 單選用（出發/目的據點）
const selectedModels = ref([]);   // 多選用（型號）
const selectedDrivers = ref([]); // 多選用（司機）
const selectedStatuses = ref([]); // 多選用（狀態）

// 父選單切換時清空子層
watch(filterCategory, () => {
  filterValue.value = '';
  selectedModels.value = [];
  selectedDrivers.value = [];
  selectedStatuses.value = [];
});

// 動態產生型號清單（去重）
const uniqueModels = computed(() => {
  const names = dispatchLogs.value
    .map(log => log.vehicle?.carModel?.modelName || log.vehicle?.modelName)
    .filter(Boolean);
  return [...new Set(names)];
});

// 動態產生司機清單（去重）
const uniqueDrivers = computed(() => {
  const driverMap = new Map();
  dispatchLogs.value.forEach(log => {
    const d = log.driverBean;
    if (d && d.driverId && !driverMap.has(d.driverId)) {
      driverMap.set(d.driverId, d.employeeBean?.empName || `司機 #${d.driverId}`);
    }
  });
  return [...driverMap.entries()].map(([id, name]) => ({ id, name }));
});

// 多選切換
const toggleMultiSelect = (arr, val) => {
  const idx = arr.indexOf(val);
  if (idx >= 0) arr.splice(idx, 1);
  else arr.push(val);
};
const removeTag = (arr, val) => {
  const idx = arr.indexOf(val);
  if (idx >= 0) arr.splice(idx, 1);
};

// -----------------------------------------
// 2. 狀態對應表 (套用 Design System 顏色規範)
// -----------------------------------------
const statusMap = {
  'PENDING': { 
    text: '待調度', 
    // Almond Cream (#FCE7D5) + Dark text
    badge: 'bg-accent rounded-pill',
    actionBtn: '開始調度', 
    nextStatus: 'IN_PROCESS', 
    icon: 'fa-play',
    btnClass: 'btn-primary' // Sapphire Blue 動作按鈕
  },
  'IN_PROCESS': { 
    text: '調度中', 
    // Sapphire Blue (#0150AD) + White text
    badge: 'bg-primary text-white', 
    actionBtn: '完成調度', 
    nextStatus: 'FINISHED', 
    icon: 'fa-check',
    btnClass: 'btn-success text-white' // 成功色
  },
  'FINISHED': { 
    text: '已完成', 
    badge: 'bg-success bg-opacity-10 text-success', 
    actionBtn: null, nextStatus: null, icon: null 
  },
  'CANCEL': { 
    text: '已取消', 
    badge: 'bg-light text-muted border', 
    actionBtn: null, nextStatus: null, icon: null 
  }
};

// 各狀態筆數，給頁籤小標用
const pendingCount = computed(() => {
  if (!dispatchLogs.value || !Array.isArray(dispatchLogs.value)) return 0;
  return dispatchLogs.value.filter(log => log.status?.dbCode === 'PENDING').length;
});
const inprocessCount = computed(() => {
  if (!dispatchLogs.value || !Array.isArray(dispatchLogs.value)) return 0;
  return dispatchLogs.value.filter(log => log.status?.dbCode === 'IN_PROCESS').length;
});
const finishedCount = computed(() => {
  if (!dispatchLogs.value || !Array.isArray(dispatchLogs.value)) return 0;
  return dispatchLogs.value.filter(log => log.status?.dbCode === 'FINISHED').length;
});
const cancelCount = computed(() => {
  if (!dispatchLogs.value || !Array.isArray(dispatchLogs.value)) return 0;
  return dispatchLogs.value.filter(log => log.status?.dbCode === 'CANCEL').length;
});
// const statusCounts = ref({
//   PENDING: 0,
//   IN_PROCESS: 0,
//   FINISHED: 0,
//   CANCEL: 0,
// });

// 抓取數量方法
// const fetchCounts = async () => {
//   try {
//     const response = await dispatchAPI.getCounts();
//     statusCounts.value = response.data;
//   } catch (error) {
//     console.error("狀態獲取失敗：", error);
//   }
// }

// -----------------------------------------
// 3. 表單專用變數 (form state)
// -----------------------------------------

// 存前端表單輸入內容
const dispatchForm = ref({
    dispatchId: null,
    vehicleId: '',
    fromLocationId: '',
    toLocationId: '',
    driverId: '',
    scheduledStartTime: '',
    actualStartTime: '',
    actualEndTime: '',
    startMileage: '',
    endMileage: '',
    empId: '',
    reason: '',
    status: '',
    notes: '',
    // createdAt: '',
    // updatedAt: '',
    // isDeleted: false, //後端自動生成，前端送單時可略
});


// -----------------------------------------
// 4. 方法：改變狀態 
// -----------------------------------------
const changeStatus = async (log) => {
  const currentStatusInfo = statusMap[log.status.dbCode];

  // 將原本的 catch 邏輯抽成共用的錯誤處理函式
  const handleError = (error) => {
    console.error("狀態轉換失敗：", error);
    const responseData = error.response?.data;
    let errorMsg = responseData?.message;
    
    if (errorMsg) {
      if (responseData?.fieldErrors) {
          const detailedMessage = Object.values(responseData.fieldErrors).join('\n');
          errorMsg = `${errorMsg}\n\n詳細原因：\n${detailedMessage}`;
      }
      toastRef.value?.show(`操作失敗：\n${errorMsg}`, 'danger');
      // 詢問是否換車
      showConfirm(`是否要立刻為此調度單更換車輛？`, () => {
        formModalRef.value.openModal(log, 'edit');
      });
    }
  };

  // 2. 開始調度邏輯
  if (log.status.dbCode === 'PENDING') {
    showConfirm(`確定要將單號 #${log.dispatchId} (${log.vehicle.plateNo}) 標記為「開始調度」嗎？`, async () => {
      // ✅ 將 try...catch 放進 confirm 確定後執行的 Callback 裡
      try {
        await dispatchAPI.start(log.dispatchId);
        await dispatchStore.fetchDispatches();
        toastRef.value?.show('調度已成功開始！', 'success');
        refreshList();
      } catch (error) {
        handleError(error);
      }
    });

  // 3. 完成調度邏輯
  } else if (log.status.dbCode === 'IN_PROCESS') {
    showConfirm(`確定要將單號 #${log.dispatchId} 標記為「已完成」嗎？`, async () => {
      const inputMileage = prompt('調度完成！請輸入車輛結束里程數 (km)：', log.vehicle.mileage);
      if(inputMileage && !isNaN(inputMileage)) {
        const endMileage = parseInt(inputMileage, 10);
        // ✅ 同樣將 try...catch 放進 Callback 裡
        try {
          await dispatchAPI.finish(log.dispatchId, endMileage);
          await dispatchStore.fetchDispatches();
          toastRef.value?.show(`單號 #${log.dispatchId} 已結案！車輛狀態已連動更新`, 'success');
          refreshList();
        } catch (error) {
          handleError(error);
        }
      }
    });
  }
  // try {
  //   // 開始調度邏輯：綁後端API
  //   if (log.status.dbCode === 'PENDING') {
  //     // if(confirm(`確定要將單號 #${log.dispatchId} (${log.vehicle.plateNo}) 標記為「開始調度」嗎？`)) {
  //     showConfirm(`確定要將單號 #${log.dispatchId} (${log.vehicle.plateNo}) 標記為「開始調度」嗎？`, async () => {
  //       // 呼叫後端API
  //       await dispatchAPI.start(log.dispatchId);
  //       // 成功後，重新抓取清單以更新畫面
  //       await dispatchStore.fetchDispatches();
  //       // alert('調度已成功開始！');
  //       toastRef.value?.show('調度已成功開始！', 'success');
  //       refreshList();
  //     });
  //   // 完成調度邏輯
  //   } else if (log.status.dbCode === 'IN_PROCESS') {
  //     showConfirm(`確定要將單號 #${log.dispatchId} 標記為「已完成」嗎？`, async () => {
  //       const inputMileage = prompt('調度完成！請輸入車輛結束里程數 (km)：', log.vehicle.mileage);
  //       // 確保使用者有輸入數字才能送出
  //       if(inputMileage && !isNaN(inputMileage)) {
  //         const endMileage = parseInt(inputMileage, 10);
    
  //         //呼叫後端API
  //         await dispatchAPI.finish(log.dispatchId, endMileage);
  //         // 成功後，重新抓取清單以更新畫面
  //         await dispatchStore.fetchDispatches();
  //         // alert(`單號 #${log.dispatchId} 已結案！車輛狀態已連動更新`);
  //         toastRef.value?.show(`單號 #${log.dispatchId} 已結案！車輛狀態已連動更新`, 'success');
  //         refreshList();
  //       }
  //     });
  //   } 
  // } catch (error) {
  //   console.error("狀態轉換失敗：", error);

  //   const responseData = error.response?.data;
  //   // 預設的大標題訊息
  //   let errorMsg = responseData?.message;
  //   if (errorMsg) {
  //     // 檢查後端有沒有傳送詳細錯誤訊息(fieldErrors)
  //     if (responseData?.fieldErrors) {
  //         // Object.values會把JSON物件{dispatchId: A, vehicleId: B}變成陣列[A, B]
  //         // 再用 join('\n')把陣列[]用換行符號串接起來
  //         const detailedMessage = Object.values(responseData.fieldErrors).join('\n');
  
  //         // 把大標題跟詳細訊息合體
  //         errorMsg = `${errorMsg}\n\n詳細原因：\n${detailedMessage}`;
  //     }
  //     // alert(`操作失敗：\n${errorMsg}`);
  //     toastRef.value?.show(`操作失敗：\n${errorMsg}`, 'danger');

  //     // if (confirm(`是否要立刻為此調度單更換車輛？`)) {
  //     showConfirm(`是否要立刻為此調度單更換車輛？`, () => {
  //       formModalRef.value.openModal(log, 'edit');
  //     });
  //   }
  // }
};

const cancelDispatch = async (log) => {
  // if(confirm(`警告：確定要取消單號 #${log.dispatchId} 嗎？`)) {
  showConfirm(`警告：確定要取消單號 #${log.dispatchId} 嗎？`, async () => {
    try {
      await dispatchAPI.cancel(log.dispatchId);
      await dispatchStore.fetchDispatches();
      // alert('訂單已取消');
      toastRef.value?.show('訂單已取消', 'success');
    } catch (error) {
      console.error("取消失敗：", error);
      // alert('取消失敗，請稍後再試');
      toastRef.value?.show('取消失敗，請稍後再試', 'danger');
    }
  });
};

// -----------------------------------------
// 5. 前端即時過濾(Computed Properties)
//    利用vue的computed屬性，不需重新發API，就能在前端瞬間切換頁籤狀態 
// -----------------------------------------
const currentTab = ref('ALL');
const filteredLogs = computed(() => {
  if (!dispatchLogs || !dispatchLogs.value) return [];

  let result = dispatchLogs.value;

  // ── Tab 狀態篩選 ──
  if (currentTab.value !== 'ALL') {
    result = result.filter(log => log.status.dbCode === currentTab.value);
  }

  // ── 關鍵字模糊搜尋（車牌、品牌、型號、司機）──
  const kw = searchQuery.value.trim().toLowerCase();
  if (kw) {
    result = result.filter(log => {
      const plate = (log.vehicle?.plateNo || '').toLowerCase();
      const brand = (log.vehicle?.brand || log.vehicle?.carModel?.brand || '').toLowerCase();
      const model = (log.vehicle?.modelName || log.vehicle?.carModel?.modelName || '').toLowerCase();
      const driver = (log.driverBean?.employeeBean?.empName || '').toLowerCase();
      return plate.includes(kw) || brand.includes(kw) || model.includes(kw) || driver.includes(kw);
    });
  }

  // ── 父選單篩選（單選）──
  if (filterCategory.value && filterValue.value) {
    if (filterCategory.value === 'fromLocation') {
      result = result.filter(log => log.fromLocation?.locationId == filterValue.value);
    } else if (filterCategory.value === 'toLocation') {
      result = result.filter(log => log.toLocation?.locationId == filterValue.value);
    } else if (filterCategory.value === 'modelName') {
      result = result.filter(log => {
        const m = log.vehicle?.carModel?.modelName || log.vehicle?.modelName;
        return m === filterValue.value;
      });
    } else if (filterCategory.value === 'driver') {
      result = result.filter(log => log.driverBean?.driverId == filterValue.value);
    } else if (filterCategory.value === 'status') {
      result = result.filter(log => log.status?.dbCode === filterValue.value);
    }
  }

  // ── 多選篩選（型號）──
  if (filterCategory.value === 'modelName' && selectedModels.value.length > 0) {
    result = result.filter(log => {
      const m = log.vehicle?.carModel?.modelName || log.vehicle?.modelName;
      return selectedModels.value.includes(m);
    });
  }

  // ── 多選篩選（司機）──
  if (filterCategory.value === 'driver' && selectedDrivers.value.length > 0) {
    result = result.filter(log => {
      return selectedDrivers.value.includes(log.driverBean?.driverId);
    });
  }

  // ── 多選篩選（狀態）──
  if (filterCategory.value === 'status' && selectedStatuses.value.length > 0) {
    result = result.filter(log => {
      return selectedStatuses.value.includes(log.status?.dbCode);
    });
  }

  return result;
});

// 篩選條件變動時重置分頁
watch([searchQuery, filterCategory, filterValue, selectedModels, selectedDrivers, selectedStatuses], () => {
  currentPage.value = 1;
});

// 日期時間格式化
const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return '無資料';
  const date = new Date(dateTimeString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day},  ${hour}:${minute}`;
};

// ====== 表頭排序區(必須在分頁區功能之下) ======
const sortKey = ref('dispatchId'); //預設排序欄位(如：單號)
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
const sortedLogs = computed (() => {
  // 複製一份新陣列，避免 mutate 原始資料
  const result = [...filteredLogs.value];

  return result.sort((a, b) => {
    let valA = a[sortKey.value];
    let valB = b[sortKey.value];

    // 若依單號排序，需先強制轉換為數字再比較
    if (sortKey.value === 'dispatchId') {
      valA = Number(valA) || 0;
      valB = Number(valB) || 0;
    }

    // 若依調度路線(起始據點)排序，需抓內層物件
    if (sortKey.value === 'fromLocation') {
      valA = a.fromLocation?.locationName || '';
      valB = b.fromLocation?.locationName || '';
    }

    //  若是日期時間格式，轉換為時間戳記來比大小
    if (sortKey.value === 'scheduledStartTime') {
      // 加上||0，確保即使解析失敗或遇到null，也會變成數值0，不會噴出NaN搞壞排序
      valA = valA ? new Date(valA).getTime() || 0 : 0;
      valB = valB ? new Date(valB).getTime() || 0 : 0;
    }

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
// const totalPages = ref(1);
const pageSize = 10;
// const filteredLogs = ref([]);

// 抓取特定頁面資料的方法
// const fetchPageData = async () => {
//   try {
//     const response = await dispatchAPI.getAll(
//       currentPage.value,
//       pageSize,
//       currentTab.value, // 將當前葉謙狀態傳給後端
//     );
//     // 後端 Spring Boot 回傳的Page物件結構：
//     // 資料本體放在 response.data.content 裡面
//     filteredLogs.value = response.data.content;
//     // 總頁數放在 response.data.totalPages 裡面
//     totalPages.value = response.data.totalPages;
//   } catch (error) {
//     console.error("分頁資料載入失敗：", error);
//   }
// };

// 2. 切換頁籤時，必須把頁碼重置回第1頁，否則會發生找不到資料的bug
watch(currentTab, () => {
  currentPage.value = 1;
  // fetchPageData();
});

// 3. 核心：計算出「當前這一頁」該顯示的資料陣列
const paginatedLogs = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  // 用 slice 切割陣列，如第一頁就是 slice(0, 10)
  return sortedLogs.value.slice(startIndex, endIndex);
});

// 4. 計算總頁數（給分頁按鈕用）
const totalPages = computed(() => {
  return Math.ceil(filteredLogs.value.length / pageSize);
});

// 5. 換頁方法
const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    // fetchPageData(); //頁碼改變，重新打API
  }
};
// =========================================

// -----------------------------------------
// 6. 初始化載入資料
// -----------------------------------------
onMounted(async () => {
  // 觸發 Store 去後端撈「調度單清單」(會觸發自動更新上面的storeToRefs(dispatchLogs))
  dispatchStore.fetchDispatches(); 
  // fetchPageData();
  // fetchCounts();

  // 觸發一般的API去撈取「下拉選單」需要的基礎設施資料
  try {
    const locRes = await locationAPI.getAll();
    const mdlRes = await carModelAPI.getAll();
    const vehRes = await vehicleAPI.getAll();
    const driRes = await driverAPI.getAll();
    const empRes = await employeeAPI.getAll();
    locations.value = locRes.data;
    carmodels.value = mdlRes.data;
    vehicles.value = vehRes.data;
    drivers.value = driRes.data;
    empRes.value = empRes.data;
  } catch (error) {
    console.error("無法抓取據點或車輛列表：", error);
  }

  // 點擊畫面其他地方時，關閉選單 (選做，但能提升 UX)
  // document.addEventListener('click', (e) => {
  //   if (!e.target.closest('.dropdown')) {
  //     activeDropdown.value = null;
  //   }
  // });
});
</script>

<template>
  <div class="vehicle-container min-vh-100 py-4 px-md-4 ">
    
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&family=Manrope:wght@600;700;800&display=swap" rel="stylesheet">

    <div class="d-flex align-items-center mb-4 text-muted">
      <span class="vehicle-cursor-pointer text-primary v-hover-link fw-bold" role="button" @click="router.push('/dispatch/dashboard')">
        <font-awesome-icon icon="fa-solid fa-headset" style="width: 1.25rem; font-size: 0.9rem;"/> 車輛調度中心
      </span>
      <i class="fa-solid fa-chevron-right mx-2 small"></i>
      <span class="text-dark fw-bold">調度任務管理</span>
    </div>

    <!-- 搜尋與篩選工具列 -->
    <div class="card border-0 shadow-sm mb-4 rounded-4">
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
                placeholder="輸入車牌、品牌、型號或司機...">
            </div>
          </div>

          <!-- 父選單：篩選依據 -->
          <div class="col-md-3">
            <label class="form-label text-muted small fw-bold mb-1">篩選條件</label>
            <select v-model="filterCategory" class="form-select fw-bold bg-light">
              <option value="">全部調度單</option>
              <option value="fromLocation">出發據點</option>
              <option value="toLocation">目的據點</option>
              <option value="modelName">型號</option>
              <option value="driver">司機</option>
              <option value="status">狀態</option>
            </select>
          </div>

          <!-- 子選單 -->
          <div class="col-md-3">
            <label class="form-label text-muted small fw-bold mb-1">選擇項目</label>

            <!-- 預設：未選擇篩選條件 -->
            <select v-if="!filterCategory" class="form-select text-muted" disabled>
              <option>請先選擇篩選條件</option>
            </select>

            <!-- 出發據點：單選 -->
            <select v-else-if="filterCategory === 'fromLocation'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇出發據點</option>
              <option v-for="loc in locations" :key="loc.locationId" :value="loc.locationId">
                {{ loc.locationName }}
              </option>
            </select>

            <!-- 目的據點：單選 -->
            <select v-else-if="filterCategory === 'toLocation'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇目的據點</option>
              <option v-for="loc in locations" :key="loc.locationId" :value="loc.locationId">
                {{ loc.locationName }}
              </option>
            </select>

            <!-- 型號：單選 -->
            <select v-else-if="filterCategory === 'modelName'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇型號</option>
              <option v-for="model in uniqueModels" :key="model" :value="model">
                 {{ model }}
              </option>
            </select>

            <!-- 司機：單選 -->
            <select v-else-if="filterCategory === 'driver'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇司機</option>
              <option v-for="d in uniqueDrivers" :key="d.id" :value="d.id">
                {{ d.name }}
              </option>
            </select>

            <!-- 狀態：單選 -->
            <select v-else-if="filterCategory === 'status'" v-model="filterValue" class="form-select">
              <option value="" disabled>請選擇狀態</option>
              <option v-for="(info, key) in statusMap" :key="key" :value="key">
                {{ info.text }}
              </option>
            </select>
          </div>

          <!-- 建立調度單按鈕 -->
          <div class="col-md-2 text-end">
            <button class="btn btn-primary fw-bold shadow-sm d-flex align-items-center rounded-3" @click="formModalRef.openModal(null, 'add')">
              <i class="fa-solid fa-file-circle-plus me-2"></i>建立調度單
            </button>
          </div>

        </div>

        <!-- 多選 Badge 標籤區 -->
        <div class="d-flex flex-wrap gap-2 mt-3" v-if="selectedModels.length || selectedDrivers.length || selectedStatuses.length">
          <span v-for="m in selectedModels" :key="'m-'+m" class="badge vehicle-bg-primary-light text-primary border border-alice rounded-pill px-3 py-2 d-flex align-items-center gap-1">
            {{ m }}
            <i class="fa-solid fa-xmark vehicle-cursor-pointer ms-1 opacity-50" @click="removeTag(selectedModels, m)"></i>
          </span>
          <span v-for="d in selectedDrivers" :key="'d-'+d" class="badge vehicle-bg-primary-light text-primary border border-alice rounded-pill px-3 py-2 d-flex align-items-center gap-1">
            {{ uniqueDrivers.find(x => x.id === d)?.name || d }}
            <i class="fa-solid fa-xmark vehicle-cursor-pointer ms-1 opacity-50" @click="removeTag(selectedDrivers, d)"></i>
          </span>
          <span v-for="s in selectedStatuses" :key="'s-'+s" class="badge vehicle-bg-primary-light text-primary border border-alice rounded-pill px-3 py-2 d-flex align-items-center gap-1">
            {{ statusMap[s]?.text || s }}
            <i class="fa-solid fa-xmark vehicle-cursor-pointer ms-1 opacity-50" @click="removeTag(selectedStatuses, s)"></i>
          </span>
        </div>
      </div>
    </div>

    <div class="card bg-white border-0 shadow-sm rounded-4 overflow-hidden">
      
      <div class="card-header bg-white border-bottom-0 pt-3 pb-0 px-4">
        <ul class="nav nav-tabs vehicle-tabs gap-3">
          <li class="nav-item vehicle-cursor-pointer" @click="currentTab = 'ALL'">
            <a class="nav-link border-0 fw-bold pb-3" :class="currentTab === 'ALL' ? 'active-tab' : 'text-muted'">全部單據</a>
          </li>
          <li class="nav-item vehicle-cursor-pointer" @click="currentTab = 'PENDING'">
            <a class="nav-link border-0 fw-bold pb-3" :class="currentTab === 'PENDING' ? 'active-tab' : 'text-muted'">
              待調度 <span class="badge bg-accent ms-1 rounded-pill">{{ pendingCount }}</span>
            </a>
          </li>
          <li class="nav-item vehicle-cursor-pointer" @click="currentTab = 'IN_PROCESS'">
            <a class="nav-link border-0 fw-bold pb-3" :class="currentTab === 'IN_PROCESS' ? 'active-tab' : 'text-muted'">
              執行中 <span class="badge bg-primary text-white ms-1 rounded-pill">{{ inprocessCount }}</span>
            </a>
          </li>
          <li class="nav-item vehicle-cursor-pointer" @click="currentTab = 'FINISHED'">
            <a class="nav-link border-0 fw-bold pb-3" :class="currentTab === 'FINISHED' ? 'active-tab' : 'text-muted'">已完成</a>
          </li>
          <li class="nav-item vehicle-cursor-pointer" @click="currentTab = 'CANCEL'">
            <a class="nav-link border-0 fw-bold pb-3" :class="currentTab === 'CANCEL' ? 'active-tab' : 'text-muted'">已取消</a>
          </li>
        </ul>
      </div>

      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0 vehicle-table">
          <thead>
            <tr>
              <th @click="handleSort('dispatchId')" style="cursor: pointer;" class="px-4 py-3 fw-bold text-muted small text-uppercase vehicle-tracking-wider">
                單號 / 車輛
                <span v-if="sortKey === 'dispatchId'" class="ms-1">
                  <i class="fas" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down'"></i>
                </span>
                <span v-else class="ms-1 text-muted">
                  <i class="fas fa-sort" ></i>
                </span>
              </th>
              <th @click="handleSort('fromLocation')" style="cursor: pointer;" class="px-4 py-3 fw-bold text-muted small text-uppercase vehicle-tracking-wider">
                調度路線
                <span v-if="sortKey === 'fromLocation'" class="ms-1">
                  <i class="fas" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down' "></i>
                </span>
                <span v-else class="ms-1 text-muted">
                  <i class="fas fa-sort"></i>
                </span>
              </th>
              <th @click="handleSort('scheduledStartTime')" style="cursor: pointer;" class="px-4 py-3 fw-bold text-muted small text-uppercase vehicle-tracking-wider">
                排定時間 / 司機
                <span v-if="sortKey === 'scheduledStartTime'" class="ms-1">
                  <i class="fas" :class="sortOrder === 'asc' ? 'fa-sort-up' : 'fa-sort-down' "></i>
                </span>
                <span v-else class="ms-1 text-muted">
                  <i class="fas fa-sort"></i>
                </span>
              </th>
              <th class="px-4 py-3 fw-bold text-muted small text-uppercase vehicle-tracking-wider text-center">狀態</th>
              <th class="px-4 py-3 fw-bold text-muted small text-uppercase vehicle-tracking-wider text-center">操作</th>
              <th class="px-4 py-3 fw-bold text-muted small text-uppercase vehicle-tracking-wider text-end">查看詳情</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in paginatedLogs" :key="log.dispatchId" class="vehicle-table-row">
              
              <!-- 單號 / 車輛 -->
              <td class="px-4 py-3">
                <div class="fw-bold text-dark ">#{{ log.dispatchId }}</div>
                <div class="small fw-bold text-primary vehicle-font-mono mt-1 vehicle-cursor-pointer" role="button" @click.stop="router.push(`/vehicle/detail/${log.vehicle.vehicleId}`)">{{ log.vehicle.plateNo }}</div>
                <div class="small text-muted">{{ log.vehicle.carModel.brand }} {{ log.vehicle.carModel.modelName }}</div>
              </td>

              <!-- 調度路線 & 原因 -->
              <td class="px-4 py-3">
                <div class="d-flex align-items-center gap-2">
                  <span class="badge vehicle-bg-primary-light text-primary border border-alice rounded-3 fw-bold px-2 py-1">
                    {{ log.fromLocation.locationName }}
                  </span>
                  <i class="fa-solid fa-arrow-right-long text-muted opacity-50"></i>
                  <span class="badge vehicle-bg-primary-light text-primary border border-alice rounded-3 fw-bold px-2 py-1">
                    {{ log.toLocation.locationName }}
                  </span>
                </div>
                <div class="small text-muted mt-2"><i class="fa-solid fa-tag me-1 opacity-50"></i>{{ log.reason }}</div>
              </td>

              <!-- 指定時間＆司機 -->
              <td class="px-4 py-3">
                <div class="small fw-bold text-dark"><i class="fa-regular fa-clock me-1 opacity-50"></i>{{ formatDateTime(log.scheduledStartTime) }}</div>
                <div class="small text-muted mt-1"><i class="fa-solid fa-id-card me-1 opacity-50"></i>
                  {{ log.driverBean?.driverId }} - {{ log.driverBean?.employeeBean.empName || '尚未指派' }}
                </div>
              </td>

              <!-- 調度狀態(進度) -->
              <td class="px-4 py-3 text-center">
                <!-- 加上？.並給一個預設的灰色badge樣式 -->
                <span 
                  class="badge rounded-pill px-3 py-2 vehicle-status-pill shadow-sm" 
                  :class="statusMap[log.status.dbCode]?.badge || 'bg-secondary text-white'">
                  <!-- 加上？.並給一個預設文字，以便知道後端傳了什麼東西進來 -->
                  {{ statusMap[log.status.dbCode]?.text || log.status.description || '未知狀態'}}
                </span>
              </td>

              <!-- 轉換狀態按鈕區 -->
              <td class="px-4 py-3">
                <div class="d-flex gap-2 justify-content-end">
                  
                  <!-- 開始＆完成 -->
                  <!-- 加上？. 避免 undefined.actionBtn 報錯 -->
                  <button v-if="statusMap[log.status.dbCode]?.actionBtn" 
                          @click="changeStatus(log)"
                          class="btn btn-sm fw-bold d-flex align-items-center rounded-3"
                          :class="statusMap[log.status.dbCode]?.btnClass">
                    <i class="fa-solid me-1" :class="statusMap[log.status.dbCode].icon"></i>
                    {{ statusMap[log.status.dbCode].actionBtn }}
                  </button>

                  <!-- 取消 -->
                  <button v-if="log.status.dbCode === 'PENDING'" @click="cancelDispatch(log)" class="btn btn-sm btn-outline-secondary rounded-3 border text-muted vehicle-hover-danger" title="取消單據">
                    <i class="fa-solid fa-xmark"></i>
                  </button> 
                </div>
              </td>
              <td class="px-4 py-3 text-end">
                <!-- 查看單筆詳情 -->
                <button 
                  class="btn btn-sm btn-light border text-muted rounded-3 vehicle-hover-primary" 
                  title="查看明細" 
                  @click="formModalRef.openModal(log, 'view')">
                  <i class="fa-solid fa-ellipsis-vertical"></i>
                </button>
              </td>
            </tr>
            
            <tr v-if="filteredLogs.length === 0">
              <td colspan="5" class="text-center py-5 text-muted">
                <i class="fa-solid fa-clipboard-check fs-1 mb-3 opacity-25"></i>
                <p class="mb-0 ">此狀態目前沒有單據</p>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 分頁按鈕區: 總頁數大於1時才出現 -->        
        <div class="d-flex justify-content-center align-items-center position-relative mt-4" v-if="totalPages > 1">
          <nav aria-label="Page navigation">
            <ul class="pagination pagination-sm">
              <!-- 上一頁 -->
              <li class="page-item" :class="{ disabled: currentPage === 1 }">
                <a class="page-link vehicle-cursor-pointer" @click="changePage(currentPage - 1)">
                  <!-- <button class="btn btn-sm btn-outline-secondary" @click="changePage(currentPage - 1)"> -->
                    <FontAwesomeIcon icon="fa-solid fa-caret-left" />
                  </a>
                </li>
                <!-- 頁碼數字 -->
                <li v-for="page in totalPages" 
                class="page-item" 
                :key="page" 
                :class="{ active: currentPage === page }">
                <a class="page-link vehicle-cursor-pointer" @click="changePage(page)">{{ page }}</a>
              </li>
              <!-- 下一頁 -->
              <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                <a class="page-link vehicle-cursor-pointer" @click="changePage(currentPage + 1)">
                  <FontAwesomeIcon icon="fa-solid fa-caret-right" />
                </a>
              </li>
            </ul>            
          </nav>

          <span class="position-absolute end-0 me-4 text-muted small">共計 {{ filteredLogs.length }} 筆資料</span>

        </div>

      </div>
    </div>

  
  </div>
  <DispatchFormModal ref="formModalRef" @saved="refreshList" />
  <AlertToast ref="toastRef" />
    <ConfirmDialog 
      :isVisible="confirmDialog.isVisible" 
      :message="confirmDialog.message" 
      @confirm="handleConfirm" 
      @cancel="confirmDialog.isVisible = false" 
    />
</template>

<style scoped>
/* ==============================================================
   Design System Variables & Scoped Overrides
   ============================================================== */

.vehicle-container { background-color: color-mix(in srgb, var(--color-primary-light) 20%, transparent); }
.vehicle-text-accent { color: var(--color-accent) !important; }
.vehicle-bg-accent-light { background-color: var(--color-accent-light) !important; border-color: rgba(219, 167, 114, 0.3) !important; }
.vehicle-bg-primary-light { background-color: var(--color-primary-light) !important; }
.vehicle-text-almond-dark { color: #5c3a21 !important; }

.vehicle-hover-primary:hover { color: var(--color-primary) !important; border-color: var(--color-primary) !important; background-color: var(--color-primary-light) !important; }
.vehicle-hover-danger:hover { color: var(--color-danger) !important; border-color: var(--color-danger) !important; background-color: var(--color-danger-bg) !important; }

.vehicle-status-pill { font-family: var(--font-sans); text-transform: uppercase; letter-spacing: 0.05em; font-weight: var(--font-bold); font-size: var(--text-xs); }

.vehicle-table thead th { border-bottom: 2px solid var(--color-primary-light) !important; background-color: #fff; }
.vehicle-table tbody tr { transition: background-color var(--transition-fast); }
.vehicle-table tbody tr:hover { background-color: var(--color-primary-light) !important; }

.vehicle-tabs { border-bottom: 1px solid var(--color-border); }
.vehicle-tabs .nav-link { transition: color var(--transition-normal); }
.vehicle-tabs .active-tab { color: var(--color-primary) !important; border-bottom: 3px solid var(--color-primary) !important; }

.vehicle-font-mono { font-family: var(--font-mono); font-feature-settings: "tnum"; letter-spacing: 0.05em; }
.vehicle-cursor-pointer { cursor: pointer; }
.vehicle-tracking-tight { letter-spacing: -0.02em; }
.vehicle-tracking-wider { letter-spacing: 0.05em; }

/* Additions specifically for VehicleDetailView */
.hover-lift { transition: transform var(--transition-normal), box-shadow var(--transition-normal); }
.hover-lift:hover { transform: translateY(-4px); box-shadow: var(--shadow-md) !important; }
.sticky-col { position: sticky; left: 0; z-index: 1; background-color: var(--color-bg-surface); }
.fee-cell { transition: all var(--transition-fast); }
.fee-cell:not(.bg-light):hover { background-color: var(--color-bg-sunken) !important; box-shadow: inset 0 0 0 2px var(--color-primary); }
.fee-cell:hover .edit-icon { opacity: 1 !important; }
.cell-content { user-select: none; }
.edit-input:focus { box-shadow: none; outline: none; }

</style>
