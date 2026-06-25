<script setup>
import { ref, watch } from 'vue';
import viewingappointmentApi from '@/api/usedcar/viewingappointmentApi';
import BaseModal from '@/components/common/BaseModal.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import AlertToast from '@/components/common/AlertToast.vue';

const props = defineProps({
    isVisible: Boolean,
    editData: Object, // 父組件點選的預約資料
    todayStr: String
});

const emit = defineEmits(['close', 'refresh', 'sync']);

const loading = ref(false);
const toast = ref(null);

const editForm = ref({
    apptId: '',
    apptTime: '',
    status: '',
    locationId: '',
    message: '',
    notes: ''
});

// 監聽傳入資料
watch(() => props.editData, (newVal) => {
    if (newVal) {
        // 直接在這裡處理格式，邏輯最短：
        const safeTime = newVal.apptTime ? newVal.apptTime.replace(' ', 'T').replace('TT', 'T').slice(0, 16) : '';

        editForm.value = {
            apptId: newVal.apptId,
            apptTime: safeTime,
            status: newVal.status?.dbCode || newVal.status,
            locationId: newVal.locationId,
            message: newVal.message || '',
            notes: newVal.notes || ''
        };
    }
}, { immediate: true });

// 🌟 新增：處理點擊「加到行事曆」的同時自動變更狀態邏輯
const handleSyncAndConfirm = () => {
    // 1. 先將前端表單物件的狀態當場改為 'CONFIRMED'
    editForm.value.status = 'CONFIRMED';

    // 2. 噴出提示，告訴管理員行事曆連動中、狀態已變更
    toast.value.show('已將狀態切換為【已確認】，正在連動 Google 行事曆...', 'info');

    // 3. 把改好狀態的 editForm 丟給父組件進行 Google API 同步與後端儲存
    emit('sync', editForm.value);
};

const submitUpdate = async () => {
    if (!editForm.value.apptTime) {
        toast.value.show('請選擇預約時間', 'warning');
        return;
    }

    try {
        loading.value = true;
        const id = editForm.value.apptId;

        const payload = {
            ...editForm.value,
            apptTime: editForm.value.apptTime.replace('T', ' '), // 轉回 SQL 格式
            locationId: Number(editForm.value.locationId)
        };

        await viewingappointmentApi.update(id, payload);
        toast.value.show('預約資訊更新成功！', 'success');

        setTimeout(() => {
            emit('refresh');
            emit('close');
        }, 800);
    } catch (error) {
        const msg = error.response?.data?.message || '更新失敗';
        toast.value.show(msg, 'danger');
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <BaseModal :isVisible="isVisible" :title="'📝 修改預約資訊 (編號: ' + editForm.apptId + ')'" size="lg"
        @close="$emit('close')">
        <template #body>
            <LoadingSpinner :isLoading="loading" overlay />
            <AlertToast ref="toast" />

            <form @submit.prevent>
                <div class="row g-4">
                    <!-- 預約日期時間 -->
                    <div class="col-md-6">
                        <label class="form-label fw-bold small text-secondary text-uppercase">預約時間 *</label>
                        <input type="datetime-local" v-model="editForm.apptTime"
                            class="form-control form-control-lg border-2 shadow-sm" required />
                    </div>

                    <!-- 處理狀態 -->
                    <div class="col-md-6">
                        <label class="form-label fw-bold small text-secondary text-uppercase">處理狀態 *</label>
                        <div class="d-flex gap-2"> <select v-model="editForm.status"
                                class="form-select form-select-lg border-2 shadow-sm">
                                <option value="PENDING">待處理 (PENDING)</option>
                                <option value="CONFIRMED">已確認 (CONFIRMED)</option>
                                <option value="COMPLETED">已完成 (COMPLETED)</option>
                                <option value="CANCELLED">已取消 (CANCELLED)</option>
                            </select>

                            <button v-if="editForm.status === 'PENDING' && props.editData && !props.editData.isSynced"
                                @click="handleSyncAndConfirm" class="btn btn-outline-primary shadow-sm flex-shrink-0"
                                style="white-space: nowrap;"> <i class="bi bi-google me-2"></i>加到行事曆
                            </button>
                        </div>
                    </div>

                    <!-- 看車地點 -->
                    <div class="col-md-12">
                        <label class="form-label fw-bold small text-secondary">變更看車地點</label>
                        <select v-model="editForm.locationId" class="form-select border-2">
                            <option value="1">1 | 台北總站</option>
                            <option value="2">2 | 新竹分站</option>
                            <option value="3">3 | 台中門市</option>
                        </select>
                    </div>

                    <!-- 客戶訊息 (僅限修改訊息，通常為備註客戶的新需求) -->
                    <div class="col-12">
                        <label class="form-label fw-bold small text-secondary">客戶預約訊息 (唯讀或修改)</label>
                        <textarea v-model="editForm.message" rows="3" class="form-control border-2 bg-light"></textarea>
                    </div>

                    <!-- 內部備註 -->
                    <div class="col-12">
                        <label class="form-label fw-bold small text-secondary text-uppercase">內部管理備註</label>
                        <textarea v-model="editForm.notes" rows="3" class="form-control border-2"
                            placeholder="記錄聯繫狀況或取消原因..."></textarea>
                    </div>
                </div>
            </form>
        </template>

        <template #footer>
            <div class="d-flex w-100 justify-content-between align-items-center">
                <span class="text-danger small fw-bold">* 為必填欄位</span>
                <div>
                    <button class="btn btn-link text-decoration-none text-secondary me-2"
                        @click="$emit('close')">取消</button>
                    <button class="btn btn-primary px-4 shadow-sm fw-bold" @click="submitUpdate" :disabled="loading">
                        <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                        儲存變更
                    </button>
                </div>
            </div>
        </template>
    </BaseModal>
</template>