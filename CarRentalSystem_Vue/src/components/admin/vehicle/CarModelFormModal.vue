<script setup>
import { ref, onMounted } from 'vue';
import { vehicleAPI } from '@/api/vehicle/vehicleApi';
import { locationAPI } from '@/api/vehicle/locationApi';
import { carModelAPI } from '@/api/vehicle/carModelApi';
import { useVehicleStore } from '@/store/vehicle/vehicleStore';
import ImageUploader from '@/components/common/ImageUploader.vue';
import AlertToast from '@/components/common/AlertToast.vue';

// 宣告向父元件發送的事件(存檔成功後，通知父元件重新抓資料)
const emit = defineEmits(['saved']);

// 綁定DOM元素與Bootstrap Modal實體
const modalElement = ref(null);
let bsModal = null;

// 表單狀態
const isEdit = ref(false);
const toastRef = ref(null);

// 車款預設空資料 (新增時用)
const defaultCarModel = {
    modelId: '',
    brand: '',
    modelName: '',
    displacement: '',
    turningRadius: '',
    vehicleType: '',
    seats: '',
    luggageCapacity: '',
    fuelType: '',
    transmission: '',
    wheelchairAvailable: false,
    vehicleImgUrl: '',
    isDeleted: false,
  };

const carModel = ref({...defaultCarModel});

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
  // 加上 && editData.modelId，避免吃到滑鼠點擊事件(PointerEvent)
  if (editData && editData.modelId) {
    isEdit.value = true;
    // 編輯模式：把父元件傳來的資料「深拷貝」過來，避免還沒按儲存，畫面上的字就跟著變
    carModel.value = { ...editData };
    
    // 處理關聯物件的 ID，讓下拉選單能正確選中
    // if (editData.location) vehicle.value.locationId = editData.location.locationId;
    // if (editData.carModel) vehicle.value.modelId = editData.carModel.modelId;
    // if (editData.status) vehicle.value.status = editData.status.name || editData.status.dbCode || editData.status;
    
  } else {
    isEdit.value = false;
    // 新增模式：還原為空表單
    carModel.value = { ...defaultCarModel };

    // 順便清空可能殘留的錯誤訊息
    // errors.value.plateNo = '';
    // errors.value.locationId = '';
  }
  
  // 打開 Modal
  bsModal.show();
};

const errors = ref({
    brand: '', //必填防空白
    modelName: '', //必填防空白
    displacement: '',
    turningRadius: '',
    vehicleType: '', //必填防空白
    seats: '', //必填防空白
    luggageCapacity: '',
    fuelType: '',
    transmission: '',
    wheelchairAvailable: '',
    vehicleImgUrl: '',
    isDeleted: '',
});

//邏輯驗證1: brand
const validateBrand = () => {
    // 清空先前錯誤
    errors.value.brand = '';
    // 必填且不可空白
    const brand = carModel.value.brand.trim();
    if (!brand) {
        errors.value.brand = '品牌不可空白';
        return false;
    }
    return true;
};
//邏輯驗證2: modelName
const validateModelName = () => {
    // 清空先前錯誤
    errors.value.modelName = '';
    // 必填且不可空白
    const modelName = carModel.value.modelName ? String(carModel.value.modelName).trim() : '';
    if (!modelName) {
        errors.value.modelName = '型號不可空白';
        return false;
    }
    return true;
};
//邏輯驗證3: vehicleType
const validateVehicleType = () => {
    // 清空先前錯誤
    errors.value.vehicleType = '';
    // 必填且不可空白
    const vtype = carModel.value.vehicleType;
    if (!vtype || vtype === '') {
        errors.value.vehicleType = '車型不可空白';
        return false;
    }
    return true;
};
//邏輯驗證4: seats
const validateSeats = () => {
    // 清空先前錯誤
    errors.value.seats = '';
    // 必填且不可空白
    const seats = carModel.value.seats;
    if (!seats || seats === '') {
        errors.value.seats = '座位數不可空白';
        return false;
    }
    return true;
};


// 儲存表單
const saveCarModel = async () => {
    // 確定表單驗證完成
    if (!validateBrand() || !validateModelName() || !validateVehicleType() || !validateSeats()) {
        // alert('請確認表單已正確填寫！');
        toastRef.value?.show('請確認表單已正確填寫！', 'warning');
        return;
    }

    // 發送API
    try {
        // 檢查圖片是否為新的 Base64 檔案，若是則先上傳取得 URL
        if (carModel.value.vehicleImgUrl && carModel.value.vehicleImgUrl.startsWith('data:image')) {
            
            // 呼叫我們剛剛寫在 API 檔的方法，Axios 會自動幫忙帶上 Token！
            const uploadRes = await carModelAPI.upload(carModel.value.vehicleImgUrl);
            
            // 根據你後端 FileUploadController 的寫法，成功時會回傳 ApiResponse 物件
            // 裡面的 data 是一個 Map，包含 url 和 fileName
            if (uploadRes.data && uploadRes.data.success) {
                // 成功取得網址，覆蓋掉原本超長的 base64 字串
                carModel.value.vehicleImgUrl = uploadRes.data.data.url; 
            } else {
                throw new Error(uploadRes.data?.message || '圖片伺服器處理失敗');
            }
        }

        // 複製現有表單，重新包裝為SpringBoot @ManyToOne看得懂的JSON格式
        const requestPayload = { ...carModel.value };

        // 防呆：把前端的空字串轉成 null，Java 才看得懂
        // if (!requestPayload.manufactureDate) requestPayload.manufactureDate = null;
        // if (!requestPayload.issuedDate) requestPayload.issuedDate = null;
        // if (!requestPayload.inspectionDate) requestPayload.inspectionDate = null;

        // 把收到的ID轉為物件
        // requestPayload.carModel = { modelId: requestPayload.modelId };
        // requestPayload.location = { locationId: requestPayload.locationId };
        // 刪掉舊的(controller看不懂的)屬性，讓新 JSON 物件保持乾淨
        // delete requestPayload.modelId;
        // delete requestPayload.locationId;
        // console.log("重新包裝(處理好關聯表)後傳給controller的JSON物件：", requestPayload);
        
        if (isEdit.value) {
            // 呼叫 edit API
            await carModelAPI.edit(requestPayload, carModel.value.modelId);
            console.log("模擬修改送出：", requestPayload);
        } else {
            // 呼叫 insert API
            await carModelAPI.insert(requestPayload);
            console.log("模擬新增送出：", requestPayload);
        }
        // alert('儲存成功！');
        toastRef.value?.show('儲存成功！', 'success');
        bsModal.hide(); // 關閉 Modal
        emit('saved');  // 通知父元件更新畫面！
    } catch (error) {
        // console.error("儲存失敗", error);
        toastRef.value?.show(`儲存失敗：\n${errorMsg}`, 'danger');

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

        alert(`儲存失敗：\n${errorMsg}`);
    }
};

// 一鍵填入測試資料
const fillTestData = () => {
    carModel.value.brand = 'LEXUS';
    carModel.value.modelName = 'ES 300h';
    carModel.value.vehicleType = '中型轎車';
    carModel.value.displacement = 2487;
    carModel.value.turningRadius = 5.8;
    carModel.value.seats = 5;
    carModel.value.luggageCapacity = 3;
    carModel.value.fuelType = '油電混合';
    carModel.value.transmission = '自排';
    carModel.value.wheelchairAvailable = false;
};

// 🌟 將 openModal 方法暴露出去，讓父元件可以 call 它
defineExpose({ openModal });

onMounted(async () => {
  // 初始化 Bootstrap Modal
  bsModal = new bootstrap.Modal(modalElement.value);
});

</script>

<template>
<div class="modal fade" tabindex="-1" aria-labelledby="addModelModalLabel" aria-hidden="true" ref="modalElement">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content border-0 shadow-lg rounded-4">
            
                <!-- Modal Header -->
                <div class="modal-header border-bottom-0 pb-0 px-4 pt-4">
                    <div>
                    <h4 v-if="isEdit" class="modal-title fw-bold">編輯車款</h4>
                    <h4 v-else class="modal-title fw-bold">新增車款</h4>
                    <p class="text-muted small mb-0">請填寫下方詳細資訊</p>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>

                <!-- Modal Body -->
                <div class="modal-body px-4 py-4">
                    <form @submit.prevent="saveCarModel">
                    
                    <!-- Section 1: Basic CarModel Info -->
                    <div class="mb-4">
                        <h6 class="text-primary fw-bold mb-3">
                        <i class="fa-solid fa-circle-info me-2"></i>基本資訊
                        </h6>
                        <div class="row g-3">
                            <!-- 品牌 -->
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-muted">品牌 <span class="text-danger">*</span></label>
                                <input 
                                    v-model="carModel.brand" 
                                    type="text" 
                                    class="form-control bg-light" 
                                    :class="{'is-invalid': errors.brand}" 
                                    placeholder="請填入品牌，如：TOYOTA" 
                                    @blur="validateBrand"
                                    required >
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.brand">{{ errors.brand }}</div>
                            </div>
                            
                            <!-- 型號 -->
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-muted">型號  <span class="text-danger">*</span></label>
                                <input v-model="carModel.modelName" type="text" class="form-control bg-light" placeholder="如：Yaris" @blur="validateModelName" required>
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.modelName">{{ errors.modelName }}</div>
                            </div>

                            <!-- 車型 -->
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-muted">車型 <span class="text-danger">*</span></label>
                                <select 
                                    v-model="carModel.vehicleType" 
                                    class="form-select bg-light" 
                                    :class="{'is-invalid': errors.vehicleType}" 
                                    @change="validateVehicleType" 
                                    required @blur="validateVehicleType">
                                <option value="" disabled>請選擇車型</option>
                                <option value="小型轎車">小型轎車</option>
                                <option value="中型轎車">中型轎車</option>
                                <option value="休旅車">休旅車</option>
                                <option value="廂型車">廂型車</option>
                                <option value="電動車">電動車</option>
                                </select>
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.vehicleType">{{ errors.vehicleType }}</div>
                            </div>

 
                        </div>
                    </div>

                    <!-- 分隔線 -->
                    <hr class="text-muted opacity-25">

                    <!-- Section 2: 規格 -->
                    <div class="mb-4">
                        <h6 class="text-primary fw-bold mb-3">
                        <i class="fa-solid fa-cube me-2"></i>規格
                        </h6>
                        <div class="row g-3">
                            <!-- 排氣量 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">排氣量 <span class="text-danger">*</span></label>
                                <input v-model="carModel.displacement" type="number" class="form-control bg-light">
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.displacement">{{ errors.displacement }}</div>
                            </div>

                            <!-- 迴轉半徑 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">迴轉半徑</label>
                                <input v-model="carModel.turningRadius" type="number" class="form-control bg-light">
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.turningRadius">{{ errors.turningRadius }}</div>
                            </div>

                            <!-- 座位數 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">座位數 <span class="text-danger">*</span></label>
                                <input v-model="carModel.seats" type="number" min="0" class="form-control bg-light" placeholder="0" @blur="validateSeats" required>
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.seats">{{ errors.seats }}</div>
                            </div>
                            <!-- 行李數 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">行李數</label>
                                <input v-model="carModel.luggageCapacity" type="number" min="0" class="form-control"
                                name="luggageCapacity" id="luggageCapacity">
                            </div>

                            <!-- 動力來源 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">動力來源</label>
                                <select 
                                    v-model="carModel.fuelType" 
                                    class="form-select bg-light" 
                                    :class="{'is-invalid': errors.fuelType}" >
                                <option value="" disabled>請選擇動力來源</option>
                                <option value="汽油">汽油</option>
                                <option value="柴油">柴油</option>
                                <option value="純電動力">純電動力</option>
                                <option value="油電混合">油電混合</option>
                                </select>
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.fuelType">{{ errors.fuelType }}</div>
                            </div>

                            <!-- 變速箱 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">變速箱</label>
                                <select 
                                    v-model="carModel.transmission" 
                                    class="form-select bg-light" 
                                    :class="{'is-invalid': errors.transmission}" >
                                <option value="" disabled>請選擇變速箱</option>
                                <option value="自排">自排</option>
                                <option value="手排">手排</option>
                                <!-- <option v-for="trans in carModel.transmission" :key="trans" :value="trans">
                                    {{ trans }}
                                </option> -->
                                </select>
                                <!-- v-if條件設定：若有錯誤訊息，則顯示在這行 -->
                                <div class="invalid-feedback" v-if="errors.transmission">{{ errors.transmission }}</div>
                            </div>

                            <!-- 可否容納輪椅/嬰兒車 -->
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-muted">可否容納輪椅/嬰兒車</label>
                                <br>
                                <input 
                                 class="form-check-input" 
                                 type="radio" 
                                 id="wheelchairYes" 
                                 name="wheelchairOptions" 
                                 :value="true"
                                 v-model="carModel.wheelchairAvailable">
                                <label class="form-check-label" for="wheelchairYes">是</label>
                                &nbsp;
                                <input 
                                 class="form-check-input" 
                                 type="radio" 
                                 id="wheelchairNo" 
                                 name="wheelchairOptions" 
                                 :value="false"
                                 v-model="carModel.wheelchairAvailable">
                                <label class="form-check-label" for="wheelchairNo">否</label>
                            </div>


                        </div>
                    </div>

                    <!-- 分隔線 -->
                    <hr class="text-muted opacity-25">

                    <!-- Section 3: 圖片 -->
                    <div class="mb-2">
                        <h6 class="text-primary fw-bold mb-3">
                        <i class="fa-solid fa-image me-2"></i>上傳圖片
                        </h6>
                        <div class="w-100">
                            <ImageUploader class="w-100 car-image-uploader" v-model="carModel.vehicleImgUrl" :previewUrl="carModel.vehicleImgUrl" :maxSizeMB="5" />
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
                        <button type="button" class="btn btn-primary fw-bold px-4" @click="saveCarModel">
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
/* 覆蓋 ImageUploader 的行內樣式，使其依照 16:9 顯示且不被裁切 */
:deep(.car-image-uploader .rounded-3.border) {
    height: auto !important;
    aspect-ratio: 16 / 9;
}

:deep(.car-image-uploader img.rounded-3.border) {
    object-fit: contain !important;
    background-color: var(--bs-light);
}
</style>
