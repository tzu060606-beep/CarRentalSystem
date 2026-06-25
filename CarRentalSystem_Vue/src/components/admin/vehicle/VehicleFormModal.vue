<script setup>
import { ref, onMounted } from 'vue';
import { vehicleAPI } from '@/api/vehicle/vehicleApi';
import { locationAPI } from '@/api/vehicle/locationApi';
import { carModelAPI } from '@/api/vehicle/carModelApi';
import CarModelFormModal from './CarModelFormModal.vue';
import router from '@/router';

import AlertToast from '@/components/common/AlertToast.vue';

// 宣告向父元件發送的事件(存檔成功後，通知父元件重新抓資料)
const emit = defineEmits(['saved']);

// 綁定DOM元素與Bootstrap Modal實體
const modalElement = ref(null);
let bsModal = null;

// 表單狀態
const isEdit = ref(false);
const locations = ref([]);
const carModels = ref([]);
const toastRef = ref(null);

// Modal新增表單區：當子元件 Modal 存檔成功時，觸發此方法重新抓取清單
const formModelModalRef = ref(false);
const refreshList = () => {
    router.push('/vehicle/detail/:id')
}

// 車輛預設空資料 (新增時用)
const defaultVehicle = {
  vehicleId: '',
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
  isDeleted: false
};

const vehicle = ref({...defaultVehicle});

// 車輛狀態對應表 (給下拉選單用)
const statusMap = {
  'CLEANING': '場內(整理中)', 
  'AVAILABLE': '場內(可接單)',
  'RENTING': '出租中',
  'MAINTAINING': '維修中', 
  'DISPATCHING': '調度中',
  'SHUTTLING': '專車接送中', 
  'RETIRED': '退役待處置'
};

// 日期格式化工具(複製VehicleListView.vue)
const formatDate = (dateString) => {
  if (!dateString) return '無資料';
  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// 專給表單綁定用的日期格式化 (沒有日期時，回傳空字串)
const formatForInput = (dateString) => {
  // 💡 關鍵差異：這裡回傳 '' (空字串)，絕對不能回傳 '無資料'
  if (!dateString) return ''; 
  
  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// 🌟 核心魔法：供父元件呼叫的方法
const openModal = (editData = null) => {
  // 加上 && editData.vehicleId，避免吃到滑鼠點擊事件(PointerEvent)
  if (editData && editData.vehicleId) {
    isEdit.value = true;
    // 編輯模式：把父元件傳來的資料「深拷貝」過來，避免還沒按儲存，畫面上的字就跟著變
    vehicle.value = { ...editData };
    
    // 處理關聯物件的 ID，讓下拉選單能正確選中
    if (editData.location) vehicle.value.locationId = editData.location.locationId;
    if (editData.carModel) vehicle.value.modelId = editData.carModel.modelId;
    if (editData.status) vehicle.value.status = editData.status.name || editData.status.dbCode || editData.status;
    
    // 處理日期格式 (否則 input type="date" 顯示不出舊資料)
    vehicle.value.manufactureDate = formatForInput(editData.manufactureDate);
    vehicle.value.issuedDate = formatForInput(editData.issuedDate);
    vehicle.value.inspectionDate = formatForInput(editData.inspectionDate);
    
  } else {
    isEdit.value = false;
    // 新增模式：還原為空表單
    vehicle.value = { ...defaultVehicle };

    // 順便清空可能殘留的錯誤訊息
    errors.value.plateNo = '';
    errors.value.locationId = '';
  }
  
  // 打開 Modal
  bsModal.show();
};

const errors = ref({
    plateNo: '', //必填防空白.正則
    locationId: '', //必填1或2
    status: '', //必填
    modelId: '', //必填
    color: '',
    manufactureDate: '',
    issuedDate: '', //必填
    inspectionDate: '',
    mileage: '', //必填
    nextMaintainenceMileage: '',
    description: '',
    isDeleted: '',
});

//邏輯驗證1: 車牌plateNo
const validatePlateNo = () => {
    // 清空先前錯誤
    errors.value.plateNo = '';
    // 必填且不可空白
    const plate = vehicle.value.plateNo.trim();
    if (!plate) {
        errors.value.plateNo = '車牌號碼不可空白';
        return false;
    }
    // 正則
    const plateRegex = /^[A-Z]{3}-[0-9]{4}$/;
    if ( !plateRegex.test(plate) ) {
        errors.value.plateNo = '格式錯誤，請按【三碼大寫字母-四碼數字】填寫(如：RAC-0001)';
        return false;
    }
    return true;
};
//邏輯驗證2: locationId
const validateLocationId = () => {
    // 清空先前錯誤
    errors.value.locationId = '';
    // 必填且不可空白
    const lid = vehicle.value.locationId;
    if (!lid || lid === '') {
        errors.value.locationId = '據點不可空白';
        return false;
    }
    return true;
};


// 儲存表單
const saveVehicle = async () => {
    // 確定表單驗證完成
    if (!validatePlateNo() || !validateLocationId()) {
        // alert('請確認表單已正確填寫！');
        toastRef.value?.show('請確認表單已正確填寫！', 'warning');
        return;
    }

    // 發送API
    try {
        // 複製現有表單，重新包裝為SpringBoot @ManyToOne看得懂的JSON格式
        const requestPayload = { ...vehicle.value };

        // 防呆：把前端的空字串轉成 null，Java 才看得懂
        if (!requestPayload.manufactureDate) requestPayload.manufactureDate = null;
        if (!requestPayload.issuedDate) requestPayload.issuedDate = null;
        if (!requestPayload.inspectionDate) requestPayload.inspectionDate = null;

        // 把收到的ID轉為物件
        requestPayload.carModel = { modelId: requestPayload.modelId };
        requestPayload.location = { locationId: requestPayload.locationId };
        // 刪掉舊的(controller看不懂的)屬性，讓新 JSON 物件保持乾淨
        delete requestPayload.modelId;
        delete requestPayload.locationId;
        console.log("重新包裝(處理好關聯表)後傳給controller的JSON物件：", requestPayload);
        
        if (isEdit.value) {
            // 呼叫 edit API
            await vehicleAPI.edit(requestPayload, vehicle.value.vehicleId);
            console.log("模擬修改送出：", requestPayload);
        } else {
            // 呼叫 insert API
            await vehicleAPI.insert(requestPayload);
            console.log("模擬新增送出：", requestPayload);
        }
        // alert('儲存成功！');
        toastRef.value?.show('儲存成功！', 'success');
        bsModal.hide(); // 關閉 Modal
        emit('saved');  // 通知父元件更新畫面！
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
    vehicle.value.plateNo = 'TUA-' + Math.floor(1000 + Math.random() * 9000);
    vehicle.value.color = '珍珠白';
    
    if (carModels.value.length > 0) {
        vehicle.value.modelId = carModels.value[0].modelId;
    }
    if (locations.value.length > 0) {
        vehicle.value.locationId = locations.value[0].locationId;
    }
    
    vehicle.value.status = 'AVAILABLE';
    vehicle.value.mileage = 15000;
    vehicle.value.nextMaintainenceMileage = 20000;
    
    // Dates
    const today = new Date();
    const threeYearsAgo = new Date(today.setFullYear(today.getFullYear() - 3));
    vehicle.value.manufactureDate = formatForInput(threeYearsAgo.toISOString());
    vehicle.value.issuedDate = formatForInput(threeYearsAgo.toISOString());
    
    const nextMonth = new Date();
    nextMonth.setMonth(nextMonth.getMonth() + 1);
    vehicle.value.inspectionDate = formatForInput(nextMonth.toISOString());
    
    vehicle.value.description = '這是一台自動生成的測試車輛';
};

// 🌟 將 openModal 方法暴露出去，讓父元件可以 call 它
defineExpose({ openModal });

onMounted(async () => {
  // 初始化 Bootstrap Modal
  bsModal = new bootstrap.Modal(modalElement.value);

  // 抓取下拉選單資料
  try {
    const [locRes, modelRes] = await Promise.all([
      locationAPI.getAll(),
      carModelAPI.getAll()
    ]);
    locations.value = locRes.data;
    carModels.value = modelRes.data;
  } catch (err) {
    console.error("下拉選單載入失敗", err);
  }
});

</script>

<template>
<div class="modal fade" tabindex="-1" aria-labelledby="addVehicleModalLabel" aria-hidden="true" ref="modalElement">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content border-0 shadow-lg rounded-4">
            
                <!-- Modal Header -->
                <div class="modal-header border-bottom-0 pb-0 px-4 pt-4">
                    <div>
                    <h4 v-if="isEdit" class="modal-title fw-bold">編輯車輛詳情</h4>
                    <h4 v-else class="modal-title fw-bold">新增車輛</h4>
                    <p class="text-muted small mb-0">請填寫下方詳細資訊</p>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>

                <!-- Modal Body -->
                <div class="modal-body px-4 py-4">
                    <form @submit.prevent="saveVehicle">
                    
                    <!-- Section 1: Basic Vehicle Info -->
                    <div class="mb-4">
                        <h6 class="text-primary fw-bold mb-3">
                        <i class="fa-solid fa-circle-info me-2"></i>基本資訊
                        </h6>
                        <div class="row g-3">
                            <!-- 車牌號碼 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">車牌號碼 <span class="text-danger">*</span></label>
                                <input 
                                    v-model="vehicle.plateNo" 
                                    type="text" 
                                    class="form-control bg-light" 
                                    :class="{'is-invalid': errors.plateNo}" 
                                    placeholder="如：ABC-1234" 
                                    required @blur="validatePlateNo">
                                <!-- v-if條件設定：若有車牌錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.plateNo">{{ errors.plateNo }}</div>
                            </div>
                        
                            <!-- 車色 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">車色</label>
                                <input v-model="vehicle.color" type="text" class="form-control bg-light" placeholder="如：Sapphire Blue">
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.color">{{ errors.color }}</div>
                            </div>

                            <!-- 車款 (帶有 + New Model 按鈕) -->
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-muted">車款 <span class="text-danger">*</span></label>
                                <div class="d-flex gap-2">
                                    <select v-model="vehicle.modelId" name="modelId" id="modelId" class="form-select"  required>
                                        <option value="" disabled>請選擇車款型號</option>
                                        <!-- fetch抓取後端carmodel列表後，利用v-for引用[品牌＋型號]為下拉選單選項 -->
                                        <option v-for="carModel in carModels" :key="carModel.modelId" :value="carModel.modelId">
                                            {{ carModel.brand }} - {{ carModel.modelName }}
                                        </option>
                                    </select>
                                    <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                    <div class="invalid-feedback" v-if="errors.modelId">{{ errors.modelId }}</div>
                                    <button type="button" class="btn btn-outline-primary text-nowrap d-flex align-items-center" @click="formModelModalRef.openModal()"> 
                                        <i class="fa-solid fa-plus me-1"></i>新增車款
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 分隔線 -->
                    <hr class="text-muted opacity-25">

                    <!-- Section 2: Operational Status -->
                    <div class="mb-4">
                        <h6 class="text-primary fw-bold mb-3">
                        <i class="fa-solid fa-location-dot me-2"></i>營運狀態
                        </h6>
                        <div class="row g-3">
                            <!-- 所在據點 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">所在據點 <span class="text-danger">*</span></label>
                                <select 
                                    v-model="vehicle.locationId" 
                                    class="form-select bg-light" 
                                    :class="{'is-invalid': errors.locationId}" 
                                    @change="validateLocationId" 
                                    required @blur="validateLocationId">
                                <option value="" disabled>請選擇據點</option>
                                <option v-for="loc in locations" :key="loc.locationId" :value="loc.locationId">
                                    {{ loc.locationName }}
                                </option>
                                </select>
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.locationId">{{ errors.locationId }}</div>
                            </div>

                            <!-- 狀態 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">車輛狀態 <span class="text-danger">*</span></label>
                                <select v-model="vehicle.status" class="form-select bg-light" required>
                                <option value="" disabled>請選擇狀態</option>
                                <!-- 這裡借用剛剛的 statusMap -->
                                <option v-for="(label, key) in statusMap" :key="key" :value="key">
                                    {{ label }}
                                </option>
                                </select>
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.status">{{ errors.status }}</div>
                            </div>

                            <!-- 目前里程數 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">里程數(km) <span class="text-danger">*</span></label>
                                <input v-model="vehicle.mileage" type="number" min="0" class="form-control bg-light" placeholder="0" required>
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.mileage">{{ errors.mileage }}</div>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">下次維修里程數</label>
                                <input v-model="vehicle.nextMaintainenceMileage" type="number" min="0" class="form-control"
                                name="nextMaintainenceMileage" id="nextMaintainenceMileage">
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.nextMaintainenceMileage">{{ errors.nextMaintainenceMileage }}</div>
                            </div>
                        </div>
                    </div>

                    <!-- 分隔線 -->
                    <hr class="text-muted opacity-25">

                    <!-- Section 3: Dates -->
                    <div class="mb-2">
                        <h6 class="text-primary fw-bold mb-3">
                        <i class="fa-solid fa-calendar-check me-2"></i>重要日期
                        </h6>
                        <div class="row g-3">
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-muted">出廠日期</label>
                                <input v-model="vehicle.manufactureDate" type="date" class="form-control bg-light">
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.manufactureDate">{{ errors.manufactureDate }}</div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-muted">發照日期 <span class="text-danger">*</span></label>
                                <input v-model="vehicle.issuedDate" type="date" class="form-control bg-light" required>
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.issuedDate">{{ errors.issuedDate }}</div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-muted">法定驗車日</label>
                                <input v-model="vehicle.inspectionDate" type="date" class="form-control bg-light">
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.inspectionDate">{{ errors.inspectionDate }}</div>
                            </div>
                        </div>
                    </div>

                    <!-- 分隔線 -->
                    <hr class="text-muted opacity-25">

                    <!-- Section 3: Dates -->
                    <div class="mb-2">
                        <div class="col-md-12">
                            <div>
                                <label class="form-label small fw-bold text-muted">車況備注</label>
                            </div>
                            <textarea v-model="vehicle.description" class="form-control" name="description" id="dexcription"></textarea>
                         </div>
                    </div>

                    </form>
                </div>

                <!-- Modal Footer -->
                <div class="modal-footer border-top-0 px-4 pb-4 pt-0 d-flex justify-content-between align-items-center">
                    <div>
                        <button type="button" class="btn btn-outline-secondary btn-sm" @click="fillTestData">
                            <i class="fa-solid fa-wand-magic-sparkles me-1"></i>一鍵測試
                        </button>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="button" class="btn btn-light fw-bold" data-bs-dismiss="modal">取消</button>
                        <!-- 呼叫存檔方法 -->
                        <button type="button" class="btn btn-primary fw-bold px-4" @click="saveVehicle">
                            <i class="fa-solid fa-floppy-disk me-2"></i>儲存
                        </button>
                    </div>
                </div>

            </div>
        </div>
    </div>
    <CarModelFormModal ref="formModelModalRef" @saved="refreshList" />
    <AlertToast ref="toastRef" />
</template>

<style scoped>

</style>
