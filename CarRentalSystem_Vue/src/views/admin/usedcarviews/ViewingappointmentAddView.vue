<script setup>
import { ref } from 'vue';
import viewingappointmentApi from '@/api/usedcar/viewingappointmentApi';
import BaseModal from '@/components/common/BaseModal.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import AlertToast from '@/components/common/AlertToast.vue';

const props = defineProps({
    isVisible: Boolean,
    carList: Array,
    todayStr: String
});

const emit = defineEmits(['close', 'refresh', 'sync']);

const loading = ref(false);
const toast = ref(null);

const getInitialAddForm = () => ({
    usedCarId: '',
    custId: '',
    apptTime: '',
    status: 'PENDING',
    locationId: '',
    message: '',
    notes: ''
});

const addForm = ref(getInitialAddForm());

const fillTestData = () => {
    if (props.carList && props.carList.length > 0) {
        const targetCar = props.carList.find(c => c.status.dbCode === 'ACTIVE') || props.carList[0];

        const now = new Date();
        // 2. 將日期設定為明天
        now.setDate(now.getDate() + 1);

        // 3. 修正時區偏移（確保轉出來的 ISO 字串符合台灣時間）
        // getTimezoneOffset() 會回傳分鐘數，台灣是 -480 分鐘，所以這裡會變成 +480 分鐘
        const tzOffset = now.getTimezoneOffset() * 60000;
        const apptTime = new Date(now - tzOffset).toISOString().slice(0, 19);

        addForm.value = {
            ...getInitialAddForm(), // 先重置，確保乾淨
            usedCarId: targetCar.usedCarId,
            custId: 1,
            apptTime: apptTime,
            status: 'PENDING',
            locationId: 1,
            message: '測試預約：確認車況',
            notes: '一鍵填入測試資料'
        };
        toast.value.show('測試資料已填入', 'info');
    } else {
        toast.value.show('目前無可選車輛', 'warning');
    }
};

const submitAdd = async () => {
    if (!addForm.value.usedCarId || !addForm.value.apptTime) {
        toast.value.show('請檢查必填欄位', 'warning');
        return;
    }

    try {
        loading.value = true;

        // 簡化版 Payload：直接傳送，只需確保 ID 是數字
        const payload = {
            ...addForm.value,
            usedCarId: Number(addForm.value.usedCarId),
            custId: Number(addForm.value.custId),
            locationId: Number(addForm.value.locationId)
        };

        await viewingappointmentApi.insert(payload);

        toast.value.show('預約建立成功！', 'success');
        setTimeout(() => {
            emit('refresh');
            emit('close');
            addForm.value = getInitialAddForm();
        }, 1000);

    } catch (error) {
        const msg = error.response?.data?.message || '新增失敗';
        toast.value.show(msg, 'danger');
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <BaseModal :isVisible="isVisible" title="📅 新增看車預約" size="lg" @close="$emit('close')">
        <template #body>
            <LoadingSpinner :isLoading="loading" overlay />
            <AlertToast ref="toast" />

            <div class="mb-3">
                <button class="btn btn-outline-primary btn-sm shadow-sm" @click.prevent="fillTestData">
                    <i class="bi bi-magic me-1"></i> 一鍵填入
                </button>
            </div>

            <form @submit.prevent>
                <div class="row g-4">
                    <!-- 車輛選擇 -->
                    <div class="col-md-8">
                        <label class="form-label fw-bold small text-uppercase text-secondary">選擇預約車輛 *</label>
                        <select v-model="addForm.usedCarId" class="form-select form-select-lg border-2">
                            <option value="" disabled>-- 請選擇 --</option>
                            <option v-for="car in carList" :key="car.usedCarId" :value="car.usedCarId"
                                :disabled="car.status.dbCode !== 'ACTIVE'">
                                {{ car.usedCarId }} | {{ car.modelName }}
                                {{ car.status.dbCode !== 'ACTIVE' ? '(不可預約)' : '(可預約)' }}
                            </option>
                        </select>
                    </div>

                    <!-- 客戶 ID -->
                    <div class="col-md-4">
                        <label class="form-label fw-bold small text-uppercase text-secondary">客戶編號 *</label>
                        <input type="number" v-model="addForm.custId" class="form-control form-control-lg border-2" />
                    </div>

                    <!-- 日期時間 -->
                    <div class="col-md-6">
                        <label class="form-label fw-bold small text-secondary">預約日期時間 *</label>
                        <input type="datetime-local" v-model="addForm.apptTime" :min="todayStr" step="1"
                            class="form-control" />
                    </div>

                    <!-- 地點 -->
                    <div class="col-md-6">
                        <label class="form-label fw-bold small text-secondary">看車地點 *</label>
                        <select v-model="addForm.locationId" class="form-select">
                            <option value="" disabled>請選擇</option>
                            <option value="1">1 | 台北總站</option>
                            <option value="2">2 | 新竹分站</option>
                        </select>
                    </div>

                    <!-- 狀態單選 -->
                    <div class="col-12">
                        <label class="form-label fw-bold small text-secondary">預約狀態</label>
                        <select class="form-select form-select-lg" v-model="addForm.status">
                            <option value="PENDING">🕒 待處理 (Pending)</option>
                            <option value="CONFIRMED">✅ 已確認 (Confirmed)</option>
                            <option value="COMPLETED">🟦 已完成 (Completed)</option>
                            <option value="CANCELLED">❌ 已取消 (Cancelled)</option>
                        </select>
                    </div>

                    <!-- 訊息 (改用 Bootstrap 內建 border) -->
                    <div class="col-12">
                        <label class="form-label fw-bold small text-secondary">客戶預約訊息</label>
                        <textarea v-model="addForm.message" rows="3" class="form-control border border-secondary-subtle"
                            placeholder="輸入客戶需求..."></textarea>
                    </div>

                    <!-- 備註 -->
                    <div class="col-12">
                        <label class="form-label fw-bold small text-secondary">管理員備註</label>
                        <textarea v-model="addForm.notes" rows="2" class="form-control bg-light"
                            placeholder="僅供內部查看..."></textarea>
                    </div>
                </div>
            </form>
        </template>

        <template #footer>
            <div class="d-flex w-100 justify-content-between align-items-center">
                <span class="text-danger small">* 為必填欄位</span>
                <div>
                    <button class="btn btn-link text-decoration-none text-secondary me-2"
                        @click="$emit('close')">取消</button>
                    <button class="btn btn-primary px-4 shadow-sm" @click="submitAdd" :disabled="loading">
                        <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                        確認建立
                    </button>
                </div>
            </div>
        </template>
    </BaseModal>
</template>