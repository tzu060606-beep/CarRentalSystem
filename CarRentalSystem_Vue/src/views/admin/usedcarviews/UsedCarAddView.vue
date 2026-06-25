<script setup>
import { ref, computed } from 'vue';
import usedCarApi from '@/api/usedcar/usedCarApi';
import BaseModal from '@/components/common/BaseModal.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import AlertToast from '@/components/common/AlertToast.vue';
import usedCar from '@/router/modules/usedCar';

const props = defineProps({
    isVisible: Boolean,
    availableVehicles: Array,
    usedCars: Array, // 🌟 接收來自父組件的 carList
    todayStr: String
});

const emit = defineEmits(['close', 'refresh']);

const loading = ref(false);
const toast = ref(null); // Toast 實例

// 定義初始資料
const getInitialAddForm = () => ({
    selectedCar: null,
    askingPrice: '',
    conditionDesc: '',
    listDate: new Date().toISOString().split('T')[0], // 預設今天
    expireDate: '',
    status: 'ACTIVE',
    createdTime: new Date().toISOString()
});

const addForm = ref(getInitialAddForm());

// 一鍵填入測試資料
const fillTestData = () => {
    if (props.availableVehicles && props.availableVehicles.length > 0) {
        // 尋找第一台符合 RETIRED 狀態的車
        const targetCar = props.availableVehicles.find(v => v.status.dbCode === 'RETIRED') || props.availableVehicles[0];
        addForm.value = {
            selectedCar: targetCar,
            askingPrice: 588000,
            conditionDesc: '原廠保養，無事故紀錄，內裝極新。',
            listDate: new Date().toISOString().split('T')[0],
            expireDate: '2026-12-31',
            status: 'ACTIVE',
            createdTime: new Date().toISOString()
        };
        toast.value.show('測試資料已填入', 'info');
    } else {
        toast.value.show('目前無可選車輛', 'warning');
    }
};

// 篩選出只符合「RETIRED (退役)」的車輛
// const retiredVehicles = computed(() => {
//     // 🌟 關鍵修正：改用 props.availableVehicles 來讀取父組件傳進來的陣列
//     if (!props.availableVehicles) return [];

//     return props.availableVehicles.filter(v => v.status?.dbCode === 'RETIRED');
// });
const retiredVehicles = computed(() => {
    if (!props.availableVehicles) return [];

    // 先篩選出基礎狀態為「RETIRED」的公務車
    const retiredOnly = props.availableVehicles.filter(v => v.status?.dbCode === 'RETIRED');

    // 如果父組件有把 carList 傳進來，就比對 vehicleId，重複的直接剃除
    if (props.usedCars && props.usedCars.length > 0) {
        return retiredOnly.filter(v => {
            // 檢查這台退役車的 vehicleId 是否已經存在於現有的二手車清單中
            const isAlreadyAdded = props.usedCars.some(uc => uc.vehicleId === v.vehicleId);
            return !isAlreadyAdded;
        });
    }

    return retiredOnly;
});

// 提交新增
const submitAdd = async () => {
    if (!addForm.value.selectedCar) {
        toast.value.show('請選擇要入庫的車輛', 'warning');
        return;
    }
    if (!addForm.value.askingPrice || addForm.value.askingPrice <= 0) {
        toast.value.show('請輸入正確的預售價格', 'warning');
        return;
    }

    try {
        loading.value = true;
        const payload = {
            ...addForm.value,
            vehicleId: addForm.value.selectedCar.vehicleId,
            askingPrice: Number(addForm.value.askingPrice),
            status: (addForm.value.status === 'RETIRED' || !addForm.value.status) ? 'ACTIVE' : addForm.value.status
        };

        await usedCarApi.insert(payload);
        toast.value.show('車輛新增入庫成功！', 'success');

        // 延遲關閉，讓使用者看到成功訊息
        setTimeout(() => {
            emit('refresh');
            emit('close');
            addForm.value = getInitialAddForm();
        }, 1000);

    } catch (error) {
        const msg = error.response?.data?.message || '資料庫寫入失敗';
        toast.value.show('新增失敗：' + msg, 'danger');
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <BaseModal :isVisible="isVisible" title="✨ 二手車入庫申請" size="lg" @close="$emit('close')">
        <template #body>
            <!-- 狀態反饋元件 -->
            <LoadingSpinner :isLoading="loading" overlay />
            <AlertToast ref="toast" />


            <button class="btn btn-outline-primary btn-sm shadow-sm" @click.prevent="fillTestData">
                <i class="bi bi-magic me-1"></i> 一鍵填入
            </button>

            <form @submit.prevent>
                <div class="row g-4">
                    <!-- 車輛選擇 -->
                    <div class="col-md-7">
                        <label class="form-label fw-bold small text-uppercase text-secondary">
                            選擇待售車輛 <span class="text-danger">*</span>
                        </label>
                        <select v-model="addForm.selectedCar" class="form-select form-select-lg border-2">
                            <option :value="null" disabled>請選擇一台車輛...</option>
                            <option v-for="v in retiredVehicles" :key="v.vehicleId" :value="v">
                                {{ v.vehicleId }} | {{ v.modelName }} ({{ v.status.description }})
                            </option>
                        </select>

                        <div class="form-text mt-2 text-muted">
                            <i class="bi bi-info-circle me-1"></i> 目前僅列出系統中狀態為「退役」的車輛。
                        </div>
                    </div>

                    <!-- 預售價格 -->
                    <div class="col-md-5">
                        <label class="form-label fw-bold small text-uppercase text-secondary">
                            預售價格 (TWD) <span class="text-danger">*</span>
                        </label>
                        <div class="input-group input-group-lg border-2">
                            <span class="input-group-text bg-white">$</span>
                            <input type="number" v-model="addForm.askingPrice" class="form-control" placeholder="0"
                                step="1000" min="0" />
                        </div>
                    </div>

                    <!-- 上架日期 -->
                    <div class="col-md-4">
                        <label class="form-label fw-bold small text-secondary">上架日期</label>
                        <input type="date" v-model="addForm.listDate" class="form-control" />
                    </div>

                    <!-- 刊登截止日 -->
                    <div class="col-md-4">
                        <label class="form-label fw-bold small text-secondary">刊登截止日</label>
                        <input type="date" v-model="addForm.expireDate" class="form-control" :min="addForm.listDate" />
                    </div>

                    <!-- 初始狀態 -->
                    <div class="col-md-4">
                        <label class="form-label fw-bold small text-secondary">入庫後狀態</label>
                        <select v-model="addForm.status" class="form-select">
                            <option value="ACTIVE">即刻上架</option>
                            <option value="SOLD">設為已售出</option>
                            <option value="REMOVED">暫時下架</option>
                        </select>
                    </div>

                    <!-- 車況描述 -->
                    <div class="col-12">
                        <label class="form-label fw-bold small text-secondary">車況描述與備註</label>
                        <textarea v-model="addForm.conditionDesc" rows="4" class="form-control border-dashed"
                            placeholder="請描述車輛目前的詳細狀況、選配內容等..."></textarea>
                    </div>
                </div>
            </form>
        </template>

        <template #footer>
            <div class="d-flex w-100 justify-content-between align-items-center">
                <span class="text-danger small ">* 為必填欄位</span>
                <div>
                    <button class="btn btn-link text-decoration-none text-secondary me-2"
                        @click="$emit('close')">取消</button>
                    <button class="btn btn-primary px-4 shadow-sm" @click="submitAdd" :disabled="loading">
                        <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                        確認新增入庫
                    </button>
                </div>
            </div>
        </template>
    </BaseModal>
</template>