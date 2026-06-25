<script setup>
import { ref, watch, computed } from 'vue';
import salesrecordApi from '@/api/usedcar/salesrecordApi';
import BaseModal from '@/components/common/BaseModal.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import AlertToast from '@/components/common/AlertToast.vue';

const props = defineProps({
    isVisible: Boolean,
    selectedSales: Object,
    carOptions: Array,
    customerOptions: Array,
    empOptions: Array,
    todayStr: String
});

const emit = defineEmits(['close', 'refresh']);

const loading = ref(false);
const toast = ref(null);

// 統一宣告一次 editForm
const editForm = ref({
    saleId: '',
    usedCarId: '',
    custId: '',
    empId: '',
    buyerName: '',
    buyerPhone: '',
    buyerIdno: '',
    finalPrice: 0,
    paymentMethod: '',
    payStatus: '',
    saleDate: '',
    notes: ''
});

// 當 selectedSales 改變時，更新表單資料
watch(() => props.selectedSales, (val) => {
    if (val) {
        editForm.value = { ...val };
        editForm.value.paymentMethod = val.paymentMethod?.dbCode || '';
        editForm.value.payStatus = val.payStatus?.dbCode || '';
    }
}, { immediate: true });

// 校驗邏輯
const isPhoneValid = computed(() => {
    // 確保只取出純數字進行比對
    const cleanPhone = (editForm.value.buyerPhone || '').replace(/\D/g, '');
    return /^09\d{8}$/.test(cleanPhone);
});
const isIdnoValid = computed(() => /^[A-Z][12]\d{8}$/.test(editForm.value.buyerIdno));

const displayPhone = computed({
    get: () => {
        // 1. 永遠只拿純數字
        const val = (editForm.value.buyerPhone || '').replace(/\D/g, '');

        // 2. 格式化：根據純數字長度自動插入連字號
        if (val.length <= 4) return val;
        if (val.length <= 7) return val.slice(0, 4) + '-' + val.slice(4);
        return val.slice(0, 4) + '-' + val.slice(4, 7) + '-' + val.slice(7, 10);
    },
    set: (val) => {
        // 核心邏輯：將使用者輸入的任何字元，全部過濾掉「非數字」內容，
        // 並強制限制存入 editForm 的長度為 10 碼。
        editForm.value.buyerPhone = val.replace(/\D/g, '').slice(0, 10);
    }
});

const displayIdno = computed({
    get: () => editForm.value.buyerIdno || '',
    set: (val) => {
        editForm.value.buyerIdno = val.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 10);
    }
});

const submitUpdate = async () => {
    // 簡單防禦檢查
    if (!isPhoneValid.value || !isIdnoValid.value) {
        emit('show-toast', { message: '請修正表單中的錯誤格式', type: 'danger' });
        return;
    }

    loading.value = true;
    try {
        await salesrecordApi.update(editForm.value.saleId, editForm.value);
        emit('refresh'); // 通知父層重新整理列表
        emit('close');   // 關閉彈窗
    } catch (error) {
        emit('show-toast', { message: '更新失敗: ' + (error.response?.data?.message || '未知錯誤'), type: 'danger' });
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <BaseModal :isVisible="isVisible" :title="'📝 修改成交資料 (#' + editForm.saleId + ')'" size="lg" @close="$emit('close')">
        <template #body>
            <LoadingSpinner :isLoading="loading" overlay />

            <form @submit.prevent="submitUpdate" class="row g-4">
                <div class="col-md-4">
                    <label class="form-label fw-bold small">待售車輛 <span class="text-danger">*</span></label>
                    <select v-model="editForm.usedCarId" class="form-select border-2" required>
                        <option v-for="car in carOptions" :key="car.usedCarId" :value="car.usedCarId">
                            {{ car.brand }} {{ car.modelName }}
                        </option>
                    </select>
                </div>

                <div class="col-md-4">
                    <label class="form-label fw-bold small">客戶 <span class="text-danger">*</span></label>
                    <select v-model="editForm.custId" class="form-select border-2" required>
                        <option v-for="cust in customerOptions" :key="cust.custId" :value="cust.custId">
                            {{ cust.custName }}
                        </option>
                    </select>
                </div>

                <div class="col-md-4">
                    <label class="form-label fw-bold small">經手員工 <span class="text-danger">*</span></label>
                    <select v-model="editForm.empId" class="form-select border-2" required>
                        <option v-for="emp in empOptions" :key="emp.empId" :value="emp.empId">
                            {{ emp.empName }}
                        </option>
                    </select>
                </div>

                <div class="col-md-4">
                    <label class="form-label fw-bold small">買受人姓名</label>
                    <input v-model="editForm.buyerName" type="text" class="form-control border-2" required />
                </div>

                <div class="col-md-4">
                    <label class="form-label fw-bold small">聯繫電話</label>
                    <input v-model="displayPhone" type="text" class="form-control border-2"
                        :class="{ 'is-invalid': editForm.buyerPhone && !isPhoneValid }" maxlength="12"
                        placeholder="0911-001-001" />
                    <div class="invalid-feedback">請輸入 09 開頭的 10 碼數字</div>
                </div>

                <div class="col-md-4">
                    <label class="form-label fw-bold small">身分證字號</label>
                    <input v-model="editForm.buyerIdno" type="text" maxlength="10" class="form-control border-2"
                        :class="{ 'is-invalid': editForm.buyerIdno && !isIdnoValid }" />
                    <div class="invalid-feedback">請輸入正確身分證格式</div>
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small">成交總價</label>
                    <input v-model="editForm.finalPrice" type="number" class="form-control border-2" />
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small">付款方式</label>
                    <select v-model="editForm.paymentMethod" class="form-select border-2">
                        <option value="CASH">現金</option>
                        <option value="CREDIT_CARD">信用卡</option>
                        <option value="TRANSFER">轉帳</option>
                    </select>
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small">付款狀態</label>
                    <select v-model="editForm.payStatus" class="form-select border-2">
                        <option value="PENDING">待付款</option>
                        <option value="PAID">已付款</option>
                        <option value="CANCELLED">已取消</option>
                    </select>
                </div>
                <div class="col-12">
                    <label class="form-label fw-bold small">成交備註</label>
                    <textarea v-model="editForm.notes" class="form-control border-2" rows="3"></textarea>
                </div>
            </form>
        </template>

        <template #footer>
            <button class="btn btn-secondary" @click="$emit('close')">取消</button>
            <button class="btn btn-primary" @click="submitUpdate" :disabled="loading">確認儲存修改</button>
        </template>
    </BaseModal>
</template>