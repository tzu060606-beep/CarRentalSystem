<script setup>
import { ref, onMounted, computed } from 'vue';
import { dispatchAPI } from '@/api/vehicle/dispatchLogApi';
import { vehicleAPI } from '@/api/vehicle/vehicleApi';
import { locationAPI } from '@/api/vehicle/locationApi';
import { carModelAPI } from '@/api/vehicle/carModelApi';
import { useVehicleStore } from '@/store/vehicle/vehicleStore';
import { useDispatchStore } from '@/store/vehicle/dispatchLogStore';
// 匯入FK API
import { driverAPI } from '@/api/vehicle/driverTestApi';
import { employeeAPI } from '@/api/vehicle/driverTestApi';
import { no } from 'element-plus/es/locale/index.mjs';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import BaseModal from '@/components/common/BaseModal.vue';
import AlertToast from '@/components/common/AlertToast.vue';

const toastRef = ref(null);
// ===================搜尋式下拉選單===================
// 1. 車輛搜尋專用狀態
const vehicleSearchQuery = ref('');
const showVehicleDropdown = ref(false);

// 車輛過濾邏輯：沒打字就回傳全部，有打字就比對「車牌」或「品牌」
const filteredVehicles = computed(() => {
    const keyword = vehicleSearchQuery.value.trim().toLowerCase();
    if (!keyword) return vehicles.value;

    return vehicles.value.filter(v => {
        const plate = v.plateNo?.toLowerCase() || '';
        const brand = v.carModel?.brand?.toLowerCase() || '';
        return plate.includes(keyword) || brand.includes(keyword);
    });
});

//當使用者點擊選單中的某台車時
const selectVehicle = (vehicle) => {
    dispatch.value.vehicleId = vehicle.vehicleId; //真正給後端的ID
    // 把輸入匡的文字換成使用者選的車，看起來就像select一樣
    vehicleSearchQuery.value = `${vehicle.plateNo} (${vehicle.carModel?.brand || '未知'})`;
    showVehicleDropdown.value = false //關閉選單
    errors.value.vehicleId = ''; //清除防呆紅字

    // 重要！！呼叫「handleVehicleChange」(自動帶入/解鎖司機)邏輯
    handleVehicleChange();
}

// 2. 員工(填單人)搜尋專用狀態
const empSearchQuery = ref('');
const showEmpDropdown = ref(false);

// 員工過濾邏輯：比對「姓名」或「ID」
const filteredEmployees = computed(() => {
    const keyword = empSearchQuery.value.trim().toLowerCase();
    if (!keyword) return employees.value; // 若使用者沒輸入關鍵字，則回傳完整員工陣列
    
    return employees.value.filter(emp => {
        const name = emp.empName?.toLowerCase() || '';
        const id = String(emp.empId).toLowerCase() || '';
        return name.includes(keyword) || id.includes(keyword);
    });
});

//當使用者電擊選單中的某位員工時
const selectEmployee = (emp) => {
    dispatch.value.empId = emp.empId;
    empSearchQuery.value = emp.empName; //顯示姓名在輸入框
    showEmpDropdown.value = false; //關閉選單
    errors.value.empId = ''; //清除錯誤訊息
}
// ==================================================

// 宣告向父元件發送的事件(存檔成功後，通知父元件重新抓資料)
const emit = defineEmits(['saved']);

// 綁定DOM元素與Bootstrap Modal實體
const modalElement = ref(null);
let bsModal = null;

// 表單模式轉換控制器(新增、編輯、檢視)
const isEdit = ref(false);
const isViewMode = ref(false);

// 全車輛備份
const allVehicles = ref([]);

// 下拉選單資料
const locations = ref([]);
const vehicles = ref([]);
const drivers = ref([]);
const employees = ref([]);

// 切換司機可否自選的開關
const isDriverDisabled = ref(true);

// 準備空調度單
const defaultDispatch = {
    dispatchId: '',
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
    status: 'PENDING',
    notes: '',
    // created_at ,
    // updated_at ,
    isDeleted: false,
};

// 響應式表單資料
const dispatch = ref({ ...defaultDispatch });

// 狀態轉換對應表 (for下拉選單，依後端狀態機邏輯)
const statusMap = {
    'PENDING': '待執行',
    'IN_PROCESS': '執行中',
    'FINISHED': '已完成',
    'CANCEL': '已取消',
};

// 錯誤訊息物件
const errors = ref({
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
})

// ================ 日期時間相關工具 ================
// 1. 格式化工具：給input type="datetime-local" 使用
// datetime-local 需要格式為 'YYYY-MM-DDTHH:mm'
const formatDateTimeForInput = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    // 為避免時區問題導致時間偏移，手動提取本地時間
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2,'0');
    const day = String(date.getDate()).padStart(2,'0');
    const hours = String(date.getHours()).padStart(2,'0');
    const minutes = String(date.getMinutes()).padStart(2,'0');

    return `${year}-${month}-${day}T${hours}:${minutes}`;
};

// 2. 計算當下時間字串(可給input作為min屬性)
const currentMinDateTime = ref('');
// ================================================

// 核心！！供父元件呼叫的開啟Modal方法，預設mode為「新增」
const openModal = (editData = null, mode = 'add') => {
    // 每次打開這個 Modal，就立刻抓一次「現在」時間
    const now = new Date();
    // 扣除時區偏差值，轉為正確的本地時間
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    // 裁切字串到分鐘(2026-05-13T14:30)
    currentMinDateTime.value = now.toISOString().slice(0, 16);

    // 重置模式，再依據mode參數轉換
    isEdit.value = false;
    isViewMode.value = false;
    if (mode === 'edit') {
        isEdit.value = true;
    } else if (mode === 'view') {
        isViewMode.value = true;
    }

    // 若資料存在 且 資料內ID也存在，則轉為編輯模式
   if (editData && editData.dispatchId) {
        // 深拷貝，避免直接改動到父元件的資料
        dispatch.value = {...editData};
    
        // 關鍵：處理後端傳來的關聯物件(剝皮後只取 ID 放回表單的 v-model 裡)
        // 這樣下拉選單才知道現在要選中哪一筆
        if (editData.vehicle) dispatch.value.vehicleId = editData.vehicle.vehicleId;
        if (editData.fromLocation) dispatch.value.fromLocationId = editData.fromLocation.locationId;
        if (editData.toLocation) dispatch.value.toLocationId = editData.toLocation.locationId;
        if (editData.driverBean) dispatch.value.driverId = editData.driverBean.driverId;
        if (editData.status) dispatch.value.status = editData.status.dbCode || editData.status;

        // 還原車輛、填單人「搜尋式下拉選單」輸入框的文字
        if (editData.vehicle) {
            vehicleSearchQuery.value = `${editData.vehicle.plateNo} (${editData.vehicle.carModel?.brand || ''})`;
        }
        if (editData.employeeBean) {
            empSearchQuery.value = editData.employeeBean.empName;
            dispatch.value.empId = editData.employeeBean.empId;
        }
        
        // 處理時間格式
        dispatch.value.scheduledStartTime = formatDateTimeForInput(editData.scheduledStartTime);
        dispatch.value.actualStartTime = formatDateTimeForInput(editData.actualStartTime);
        dispatch.value.actualEndTime = formatDateTimeForInput(editData.actualEndTime);
    } else {
    //  新增模式
        isEdit.value = false;
        // 還原為空表單
        dispatch.value = { ...defaultDispatch };
        // 清空「搜尋式下拉選單」搜尋框
        vehicleSearchQuery.value = '';
        empSearchQuery.value = '';
        // 清空錯誤訊息
        Object.keys(errors.value).forEach(key => errors.value[key] = '');
    } 
    // 一切就緒，開啟Bootstrap Modal
    bsModal.show();
};

// 監聽車輛選擇：選車會自動帶入司機，並做前端防呆
const handleVehicleChange = () => {
    // 1. 取得目前選中的車輛 ID
    const currentVehicleId = dispatch.value.vehicleId;
    if (!currentVehicleId) return;

    // 2. 核心：去司機列表中尋找，誰身上綁了這輛車
    const assignedDriver = drivers.value.find( d => d.vehicle?.vehicleId === currentVehicleId);

    // 若這個綁定司機存在(也就是這台車有專屬司機)
    if (assignedDriver) {
        dispatch.value.driverId = assignedDriver.driverId; //自動填入司機ID
        isDriverDisabled.value = true; //鎖定司機下拉選單
        errors.value.vehicleId = ''; //清除錯誤訊息
    
    // 若沒綁定    
    } else {
        dispatch.value.driverId = ''; //清空司機ID，讓使用者自選
        isDriverDisabled.value = false; //開放選單彈性自選
        errors.value.vehicleId = '';    
    }
}

// 抓取司機姓名
const getDriverName = (empId) => {
    if (!empId || !employees.value || employees.value.length === 0) return '未知姓名';
    const targetEmployee = employees.value.find(emp => emp.empId === empId);
    return targetEmployee ? targetEmployee.empName : '未知姓名';
}

// =============驗證邏輯區=============
const validateForm = () => {
    let isValid = true;
    // 先清空舊錯誤
    Object.keys(errors.value).forEach(key => errors.value[key] = '');
    
    // 1. 必填防空白
    if (!dispatch.value.vehicleId) {
        errors.value.vehicleId = '車輛不可空白';
        isValid = false;
    } else if (errors.value.vehicleId) {
        // 若vehicleId已有錯誤(含 抓到沒綁定司機的車輛)，驗證就不通過
        isValid = false;
    };
    if (!dispatch.value.fromLocationId) {
        errors.value.fromLocationId = '出發據點不可空白';
        isValid = false;
    };
    if (!dispatch.value.toLocationId) {
        errors.value.toLocationId = '目的據點不可空白';
        isValid = false;
    };
    if (!dispatch.value.empId) {
        errors.value.empId = '填單人不可空白';
        isValid = false;
    };
    if (!dispatch.value.scheduledStartTime) {
        errors.value.scheduledStartTime = '預計出發時間不可空白';
        isValid = false;
    };
    return isValid;
};

// 2. 「起訖據點」不可相同
const validateLocations = () => {
    // 先清空錯誤訊息
    if (dispatch.value.fromLocationId !== dispatch.value.toLocationId) {
        if (errors.value.toLocationId) {
            errors.value.toLocationId = '';
        }
        // 起訖都填完，再比對是否相同
    } else if (dispatch.value.fromLocationId && dispatch.value.toLocationId) {
        if (dispatch.value.fromLocationId === dispatch.value.toLocationId) {
            errors.value.toLocationId = '出發與目的據點不可相同';
        }
    }
} 

// 3. 「預計出發時間」不可是過去時間
const validateScheduledStartTime = () => {
    if (errors.value.scheduledStartTime) {
        errors.value.scheduledStartTime = '';
    }
    if (dispatch.value.scheduledStartTime) {
        const selectedTime = new Date(dispatch.value.scheduledStartTime).getTime();
        const now = Date.now();
        if (selectedTime < now) {
            errors.value.scheduledStartTime = '預計出發時間不可填寫過去時間';
        }
    }
}

// 4. 「車輛」不可有時間及狀態衝突(綁定在預計出發時間的@change)
const checkVehicleAvailability = async () => {
    // 
    validateScheduledStartTime();
    // 若「預計出發時間」被清空，車輛選單就變回全部車輛，並清除錯誤
    if (!dispatch.value.scheduledStartTime) {
        vehicles.value = allVehicles.value;
        if (errors.value.vehicleId === '此車輛在該時段已另有業務安排，不可調度') {
            errors.value.vehicleId = '';
        }
        return;
    }
    try {
        // 呼叫「抓可用車輛API」
        let timeStr = dispatch.value.scheduledStartTime;
        if (timeStr.length === 16) timeStr += ':00'; //補秒數給 Spring Boot

        const response = await vehicleAPI.availablefordispatch(timeStr);
        // 更新下拉選單，只剩下有空的車
        vehicles.value = response.data;
        // 核心防呆：若使用者「已先選車」
        if (dispatch.value.vehicleId) {
            // 檢查該車在該時段可否使用？
            const isAvailable = vehicles.value.some(v => v.vehicleId === dispatch.value.vehicleId);
            
            if (!isAvailable) {
                // 若有業務衝突，顯示錯誤訊息
                errors.value.vehicleId = '此車輛在該時段已另有業務安排，不可調度';
                dispatch.value.vehicleId = '';
                vehicleSearchQuery.value = '';
                handleVehicleChange();
                // alert('您選擇的車輛在該時段已另有業務安排，請重新指派車輛');
                toastRef.value?.show('您選擇的車輛在該時段已另有業務安排，請重新指派車輛', 'warning');
            } else {
                // 若無衝突，清空先前的錯誤訊息
                if (errors.value.vehicleId === '此車輛在該時段已另有業務安排，不可調度') {
                    errors.value.vehicleId = '';
                }
            }
        }
    } catch (error) {
        console.error("驗證可用車輛失敗：", error);
    }
}
// ===================================

// 儲存表單：將使用者填入資料送給後端
const saveDispatch = async () => {
    if (!validateForm()) {
        // alert('請確認表單必填欄位已正確填寫！');
        toastRef.value?.show('請確認表單必填欄位已正確填寫！', 'warning');
        return;
    }
    try {
        // 1. 複製一份乾淨資料
        const requestPayload = {...dispatch.value};

        // 防止傳入後端時因空直報錯，數字欄位若未填寫則改回傳null
        if (requestPayload.dispatchId === '') requestPayload.dispatchId = null; 
        if (requestPayload.vehicleId === '') requestPayload.vehicleId = null; 
        if (requestPayload.driverId === '') requestPayload.driverId = null; 
        if (requestPayload.startMileage === '') requestPayload.startMileage = null; 
        if (requestPayload.endMileage === '') requestPayload.endMileage = null; 
        if (requestPayload.fromLocationId === '') requestPayload.fromLocationId = null; 
        if (requestPayload.toLocationId === '') requestPayload.toLocationId = null; 
        if (requestPayload.empId === '') requestPayload.empId = null; 

        // 2. 把空字串轉為null (處理時間跟里程)
        const fieldToNull = ['actualStartTime', 'actualEndTime', 'startMileage', 'endMileage'];
        fieldToNull.forEach(field => {
            if (requestPayload[field] === '') requestPayload[field] = null;
        });

        // 3. 配合Spring Boot @ManyToOne，將ID組裝回JSON物件格式
        //    後端預計收到：{vehicle: {vehicleId: 1}, fromLocation: {locationId: 2} ...}
        if (requestPayload.vehicleId) {
            requestPayload.vehicle = { vehicleId: requestPayload.vehicleId };
        }
        if (requestPayload.fromLocationId) {
            requestPayload.fromLocation = { locationId: requestPayload.fromLocationId };
        }
        if (requestPayload.toLocationId) {
            requestPayload.toLocation = { locationId: requestPayload.toLocationId };
        }
        if (requestPayload.driverId) {
            requestPayload.driverBean = { driverId: requestPayload.driverId };
        }
        if (requestPayload.empId) {
            requestPayload.employeeBean = { empId: requestPayload.empId };
        }

        // 4. 清理掉多餘的純數字ID，避免因而和後端identity自動生成序號衝突而報錯
        delete requestPayload.vehicleId;
        delete requestPayload.fromLocationId;
        delete requestPayload.toLocationId;
        delete requestPayload.driverId;
        delete requestPayload.empId;

        console.log("準備送給後端的JSON：", requestPayload);
        
        // 5. 呼叫API
        if (isEdit.value) {
            // 修改
            await dispatchAPI.edit(requestPayload, dispatch.value.dispatchId);
        } else {
            // 新增
            // 清除「狀態」欄位輸入的資料，因後端service已寫好自動帶入
            delete requestPayload.status;
            await dispatchAPI.insert(requestPayload);
        }

        // alert('儲存成功！');
        toastRef.value?.show('儲存成功！', 'success');
        bsModal.hide();
        // 呼叫父元素重整列表
        emit('saved');

    } catch (error) {
        console.error("儲存失敗", error);

        // 1. 先抓取後端回傳的整包 data
        const responseData = error.response?.data;
        // 2. 預設的大標題訊息
        let errorMsg = responseData?.message || '發生未知的錯誤';
        // 3. 關鍵：檢查後端有沒有傳送詳細錯誤訊息(fieldErrors)
        if (responseData?.fieldErrors) {
            // Object.values會把JSON物件{dispatchId: A, vehicleId: B}變成陣列[A, B]
            // 再用 join('\n')把陣列[]用換行符號串接起來
            const detailedMessage = Object.values(responseData.fieldErrors).join('\n');

            // 把大標題跟詳細訊息合體
            errorMsg = `${errorMsg}\n\n詳細原因：\n${detailedMessage}`;
        }

        // alert(`儲存失敗：\n${errorMsg}`);
        toastRef.value?.show(`儲存失敗：\n${errorMsg}`, 'danger');
    }
};

// 一鍵填入測試資料
const fillTestData = () => {
    if (vehicles.value.length > 0) {
        selectVehicle(vehicles.value[0]);
    }
    if (locations.value.length > 1) {
        dispatch.value.fromLocationId = locations.value[0].locationId;
        dispatch.value.toLocationId = locations.value[1].locationId;
    }
    if (employees.value.length > 0) {
        selectEmployee(employees.value[0]);
    }
    
    // Set scheduled time to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setMinutes(tomorrow.getMinutes() - tomorrow.getTimezoneOffset());
    dispatch.value.scheduledStartTime = tomorrow.toISOString().slice(0, 16);
    
    dispatch.value.reason = '一鍵測試調度單';
    dispatch.value.notes = '這是一筆自動填入的測試調度資料';
};

// 暴露給父元件使用
defineExpose({ openModal });

// 初始化
onMounted( async () => {
    //綁定 Bootstrap Modal
    bsModal = new bootstrap.Modal(modalElement.value);

    // 一次性抓取所有下拉選單需要的資料(並發請求，節省時間)
    try {
        const [locRes, vehRes, driRes, empRes] = await Promise.all([
            locationAPI.getAll(),
            vehicleAPI.getAll(),
            driverAPI.getAll(),
            employeeAPI.getAll(),
        ]);
        locations.value = locRes.data;
        allVehicles.value = vehRes.data; //備份，永遠不會變
        vehicles.value = vehRes.data;
        drivers.value = driRes.data;
        employees.value = empRes.data;
    } catch (error) {
        console.error("下拉選單載入失敗", error);
    }
})

</script>

<template>
<div class="modal fade" tabindex="-1" aria-labelledby="addDispatchModalLabel" aria-hidden="true" ref="modalElement">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content border-0 shadow-lg rounded-4">
            
                <!-- Modal Header -->
                <div class="modal-header border-bottom-0 pb-0 px-4 pt-4">
                    <div>
                    <h4 v-if="isViewMode" class="modal-title fw-bold">調度單詳情</h4>
                    <h4 v-else-if="isEdit" class="modal-title fw-bold">編輯調度單</h4>
                    <h4 v-else class="modal-title fw-bold">新增調度單</h4>
                    <p class="text-muted small mb-0" v-if="!isViewMode || isEdit">請指派車輛與任務時間</p>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>

                <!-- Modal Body -->
                <div class="modal-body px-4 py-4">
                    <form @submit.prevent="saveCarModel">

                                                <!-- Section 3: 時間紀錄 -->
                        <div class="mb-2">
                            <h6 class="text-primary fw-bold mb-3">
                            <i class="fa-solid fa-clock me-2"></i>時間紀錄
                            </h6>
                            <div class="row g-3">
                                <div class="col-md-12">
                                    <label class="form-label small fw-bold text-muted">預計出發時間 <span class="text-danger">*</span></label>
                                    <input 
                                     v-model="dispatch.scheduledStartTime" 
                                     type="datetime-local" 
                                     class="form-control bg-light"
                                     :min="currentMinDateTime"
                                     :class="{'is-invalid': errors.scheduledStartTime}"
                                     @change="checkVehicleAvailability"
                                     :disabled="isViewMode">
                                    <div class="invalid-feedback">{{ errors.scheduledStartTime }}</div>
                                </div>
                                <div class="col-md-6" :hidden="!isEdit && !isViewMode">
                                    <label class="form-label small fw-bold text-muted">實際出發時間</label>
                                    <input 
                                    v-model="dispatch.actualStartTime" 
                                    type="datetime-local" 
                                    class="form-control bg-light" readonly>
                                </div>
                                <div class="col-md-6" :hidden="!isEdit && !isViewMode">
                                    <label class="form-label small fw-bold text-muted">實際結束時間</label>
                                    <input 
                                    v-model="dispatch.actualEndTime" 
                                    type="datetime-local" 
                                    class="form-control bg-light" readonly>
                                </div>
                            </div>
                        </div>

                        <!-- 分隔線 -->
                        <hr class="text-muted opacity-25">

                        
                        <!-- Section 1: Basic CarModel Info -->
                        <div class="mb-4">
                            <h6 class="text-primary fw-bold mb-3">
                            <i class="fa-solid fa-car me-2"></i>資源指派
                            </h6>
                            <div class="row g-3">
                                <!-- 車輛 -->
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted">指派車輛 <span class="text-danger">*</span></label>
                                    <!-- 設定relative讓下拉選單可以絕對訂對在它正下方 -->
                                     <div class="position-relative">
                                        <input 
                                         type="text" 
                                         class="form-control bg-light cursor-text"
                                         :class="{'is-invalid':errors.vehicleId}"
                                         v-model="vehicleSearchQuery"
                                         @focus="showVehicleDropdown = true"
                                         @blur="showVehicleDropdown = false"
                                         placeholder="請輸入車牌或品牌搜尋..."
                                         :disabled="isViewMode">

                                         <!-- 搜尋式下拉選單 -->
                                        <ul 
                                         class="dropdown-menu w-100 shadow-sm"
                                         :class="{show: showVehicleDropdown}"
                                         style="max-height: 200px; overflow-y: auto; position: absolute; z-index: 1050; margin-top: 2px;">
                                            <li v-for="v in filteredVehicles" :key="v.vehicleId">
                                                <!-- mousedown.prevent，不可用click，否則輸入框的blur事件會先觸發導致選單瞬間消失，無法點 -->
                                                <a class="dropdown-item" href="#" @mousedown.prevent="selectVehicle(v)">
                                                    {{ v.plateNo }} ({{ v.carModel?.brand }})
                                                </a>
                                            </li>
                                            <li v-if="filteredVehicles.length === 0">
                                                <span class="dropdown-item text-muted">查無相符車輛</span>
                                            </li>
                                        </ul>
                                     </div>
                                    <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                    <div class="invalid-feedback" :class="{ 'd-block': errors.vehicleId }">{{ errors.vehicleId }}</div>
                                </div>
                                
                                <!-- 司機 -->
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted">指派司機 (若有專屬司機將自動綁定)</label>
                                    <select 
                                    v-model="dispatch.driverId" 
                                    class="form-select bg-light"
                                    :disabled="isDriverDisabled || isViewMode"> 
                                        <option value="" v-if="isDriverDisabled">請先選擇車輛</option>    
                                        <option value="" v-else>請選擇司機</option>    
                                        <option v-for="driver in drivers" :key="driver.driverId" :value="driver.driverId">
                                            {{ driver.driverId }} - {{ getDriverName(driver.empId) || '未知姓名' }}
                                        </option>
                                    </select>
                                    <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                    <div class="invalid-feedback" v-if="errors.driverId">{{ errors.driverId }}</div>
                                </div>

                                <!-- 員工(填單人) -->
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted">填單人 <span class="text-danger">*</span></label>
                                    <div class="position-relative">
                                        <input type="text" 
                                         class="form-control bg-light"
                                         :class="{'is-invalid': errors.empId}"
                                         v-model="empSearchQuery"
                                         @focus="showEmpDropdown = true"
                                         @blur="showEmpDropdown = false"
                                         placeholder="請輸入員編或姓名搜尋..."
                                         :disabled="isViewMode"
                                         required>

                                        <ul class="dropdown-menu w-100 shadow-sm"
                                          :class="{ show: showEmpDropdown }"
                                          style="max-height: 200px; overflow-y: auto; position: absolute; z-index: 1050; margin-top: 2px;">
                                            <li v-for="emp in filteredEmployees" :key="emp.empId">
                                                <a class="dropdown-item" href="#" @mousedown.prevent="selectEmployee(emp)">
                                                    {{ emp.empId }} - {{ emp.empName }}
                                                </a>
                                            </li>
                                            <li v-if="filteredEmployees.length === 0">
                                                <span class="dropdown-item text-muted">查無相符員工</span>
                                            </li>                                        
                                        </ul>
                                    </div>
                                    <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                    <div class="invalid-feedback" v-if="errors.empId">{{ errors.empId }}</div>
                                </div>

                                <!-- 原因 -->
                                <div class="col-md-12">
                                    <label class="form-label small fw-bold text-muted">調度原因 <span class="text-danger">*</span></label>
                                    <input type="text" 
                                     v-model="dispatch.reason" 
                                     class="form-control bg-light" 
                                     placeholder="例如：車輛定期保養、客戶A地租B地還"
                                     :disabled="isViewMode">
                                </div>
                            </div>
                        </div>

                        <!-- 分隔線 -->
                        <hr class="text-muted opacity-25">

                        <!-- Section 2: 路線與狀態 -->
                        <div class="mb-4">
                            <h6 class="text-primary fw-bold mb-3">
                            <i class="fa-solid fa-location-dot me-2"></i>路線與狀態
                            </h6>
                            <div class="row g-3">
                                <!-- 出發據點 -->
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-muted">出發據點 <span class="text-danger">*</span></label>
                                    <select 
                                     v-model="dispatch.fromLocationId" 
                                     class="form-select bg-light"
                                     :class="{'is-inValid': errors.fromLocationId}"
                                     @change="validateLocations"
                                     :disabled="isViewMode">
                                        <option value="" disabled>請選擇出發地</option>
                                        <option v-for="location in locations" :key="location.locationId" :value="location.locationId">
                                            {{ location.locationName }}
                                        </option>
                                    </select>
                                    <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                    <div class="invalid-feedback" v-if="errors.fromLocationId">{{ errors.fromLocationId }}</div>
                                </div>

                                <!-- 目的據點 -->
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-muted">目的據點<span class="text-danger">*</span></label>
                                    <select 
                                     v-model="dispatch.toLocationId" 
                                     class="form-select bg-light"
                                     :class="{'is-invalid': errors.toLocationId}"
                                     @change="validateLocations"
                                     :disabled="isViewMode">
                                        <option value="" disabled>請選擇目的地</option>
                                        <option v-for="location in locations" :key="location.locationId" :value="location.locationId">
                                            {{ location.locationName }}
                                        </option>
                                    </select>
                                    <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                    <div class="invalid-feedback" v-if="errors.toLocationId">{{ errors.toLocationId }}</div>
                                </div>

                                <!-- 目前狀態 -->
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-muted">目前狀態 </label>
                                    <select 
                                     v-model="dispatch.status" 
                                     class="form-select bg-light"
                                     :disabled="!isEdit">
                                        <option v-for="(label, key) in statusMap" :key="key" :value="key">
                                            {{ label }}
                                        </option>
                                    </select>
                                    <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                    <div class="invalid-feedback" v-if="errors.status">{{ errors.status }}</div>
                                </div>

                            </div>
                        </div>

                        <!-- 分隔線 -->
                        <hr class="text-muted opacity-25">


                        <!-- Section 4: 里程紀錄 -->
                        <div class="mb-2" :hidden="!isEdit && !isViewMode">
                            <h6 class="text-primary fw-bold mb-3">
                            <FontAwesomeIcon icon="fa-solid fa-gauge-high me-2" /> 里程紀錄
                            </h6>
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-muted">起始里程數 </label>
                                    <input 
                                     v-model="dispatch.startMileage" 
                                     type="number" 
                                     class="form-control bg-light"
                                     :class="{'is-invalid': errors.startMileage}"
                                     placeholder="僅供檢視"
                                     readonly> 
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-muted">抵達里程數</label>
                                    <input 
                                    v-model="dispatch.endMileage" 
                                    type="number" 
                                    class="form-control bg-light" 
                                    :class="{'is-invalid': errors.startMileage}"
                                    placeholder="僅供檢視"
                                    readonly>
                                </div>
                            </div>
                        </div>
                        <!-- 分隔線 -->
                        <hr class="text-muted opacity-25" :hidden="!isEdit && !isViewMode">


                        <div class="mb-2">
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-muted">調度備註</label>
                                <textarea rows="3" 
                                 v-model="dispatch.notes" 
                                 class="form-control" 
                                 placeholder="有什麼需要特別注意的嗎？"
                                 :disabled="isViewMode"></textarea>
                            </div>
                        </div>
                    </form>
                </div>

                <!-- Modal Footer -->
                <div class="modal-footer border-top-0 px-4 pb-4 pt-0 d-flex justify-content-between align-items-center">
                    <div>
                        <button type="button" class="btn btn-outline-secondary btn-sm" @click="fillTestData" v-if="!isViewMode">
                            <i class="fa-solid fa-wand-magic-sparkles me-1"></i>一鍵測試
                        </button>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="button" class="btn btn-outline-secondary btn-light fw-bold " data-bs-dismiss="modal" v-if="!isEdit">取消</button>
                        <button type="button" class="btn btn-light fw-bold" @click="openModal(dispatch, 'view')" v-else>取消</button>
                        <!-- 呼叫存檔方法 -->
                        <button type="button" class="btn btn-success fw-bold px-4" @click="openModal(dispatch, 'edit')" v-if="isViewMode">
                            <i class="fa-solid fa-pen me-2"></i>編輯
                        </button>
                        <button type="button" class="btn btn-primary fw-bold px-4" @click="saveDispatch" v-else>
                            <i class="fa-solid fa-floppy-disk me-2"></i>儲存
                        </button>
                    </div>
                </div>

            </div>
        </div>
    </div>
    <AlertToast ref="toastRef" />
</template>

<style scoped>

</style>
