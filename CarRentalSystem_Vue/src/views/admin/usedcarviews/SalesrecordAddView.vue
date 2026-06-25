<script setup>
import { ref, onMounted, computed } from 'vue';
import salesrecordApi from '@/api/usedcar/salesrecordApi';
import usedCarApi from '@/api/usedcar/usedCarApi';
import { customerAPI } from '@/api/login/customerAPI';
import { employeeAPI } from '@/api/login/employeeAPI';

const props = defineProps({
    isVisible: Boolean,
    todayStr: String
});

const emit = defineEmits(['close', 'refresh']);
const loading = ref(false);

// 定義正規表達式
const phoneRegex = /^09\d{8}$/;
const idnoRegex = /^[A-Z][12]\d{8}$/;

// 初始表單資料
const getInitialAddForm = () => ({
    usedCarId: '',
    custId: '',
    empId: '',
    buyerName: '',
    buyerPhone: '',
    buyerIdno: '',
    finalPrice: null,
    paymentMethod: '',
    payStatus: '',
    saleDate: props.todayStr || new Date().toISOString().split('T')[0],
    notes: ''
});

const addForm = ref(getInitialAddForm());

// 計算屬性：校驗邏輯
const isPhoneValid = computed(() => phoneRegex.test(addForm.value.buyerPhone));
const isIdnoValid = computed(() => idnoRegex.test(addForm.value.buyerIdno));

// 手機自動格式化邏輯
const displayPhone = computed({
    get: () => {
        let val = addForm.value.buyerPhone || '';
        if (val.length <= 4) return val;
        if (val.length <= 7) return val.slice(0, 4) + '-' + val.slice(4);
        return val.slice(0, 4) + '-' + val.slice(4, 7) + '-' + val.slice(7);
    },
    set: (val) => {
        addForm.value.buyerPhone = val.replace(/\D/g, '');
    }
});
//身分證自動邏輯
const displayIdno = computed({
    get: () => addForm.value.buyerIdno || '',
    set: (val) => {
        // 1. 強制轉大寫
        // 2. 移除除了數字和字母以外的所有符號
        // 3. 截斷字串長度至 10 (身分證總長)
        addForm.value.buyerIdno = val.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 10);
    }
});

// 下拉選單資料
const carOptions = ref([]);
const customerOptions = ref([]);
const empOptions = ref([]);

const fillDemoData = () => {
    if (carOptions.value.length > 0) addForm.value.usedCarId = carOptions.value[0].usedCarId;
    if (customerOptions.value.length > 0) addForm.value.custId = customerOptions.value[0].custId;
    if (empOptions.value.length > 0) addForm.value.empId = empOptions.value[0].empId;

    addForm.value.buyerName = '李小龍';
    addForm.value.buyerPhone = '0912345678';
    addForm.value.buyerIdno = 'A123456789';
    addForm.value.finalPrice = 500000;
    addForm.value.paymentMethod = 'CASH';
    addForm.value.payStatus = 'PENDING';
    addForm.value.notes = '已付訂金未付全款';
};

onMounted(async () => {
    try {
        const [cars, customers, emps] = await Promise.all([
            usedCarApi.getAllDetails(),
            customerAPI.getAll(),
            employeeAPI.getAll()
        ]);
        carOptions.value = (cars.data || []).filter(c => c.usedCarStatus?.dbCode === 'ACTIVE');
        customerOptions.value = customers.data || [];
        empOptions.value = emps.data || [];
    } catch (error) {
        console.error("選單資料載入失敗", error);
    }
});

const submitAdd = async () => {
    if (!isPhoneValid.value || !isIdnoValid.value) {
        alert("手機或身分證格式錯誤，請修正後再送出！");
        return;
    }
    try {
        loading.value = true;
        await salesrecordApi.insert({
            ...addForm.value,
            usedCarId: Number(addForm.value.usedCarId),
            custId: Number(addForm.value.custId),
            empId: Number(addForm.value.empId),
            finalPrice: Number(addForm.value.finalPrice)
        });
        alert('資料新增成功');
        emit('refresh');
        closeModal();
    } catch (error) {
        alert('新增失敗: ' + (error.response?.data?.message || '請檢查輸入格式'));
    } finally {
        loading.value = false;
    }
};

const closeModal = () => {
    addForm.value = getInitialAddForm();
    emit('close');
};
</script>

<template>
    <div v-if="isVisible" class="modal fade show d-block" tabindex="-1" style="background-color: rgba(0, 0, 0, 0.5);">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content shadow border-0">
                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold">➕ 新增成交紀錄</h5>
                    <button class="btn btn-outline-info me-auto" @click="fillDemoData">一鍵填入</button>
                    <button type="button" class="btn-close" @click="closeModal"></button>

                </div>

                <div class="modal-body px-4">
                    <form @submit.prevent="submitAdd">
                        <div class="row g-3">
                            <div class="col-md-4">
                                <label class="form-label fw-bold small">待售車輛 <span class="text-danger">*</span></label>
                                <select v-model="addForm.usedCarId" class="form-select" required>
                                    <option value="" disabled>請選擇車輛</option>
                                    <option v-for="car in carOptions" :key="car.usedCarId" :value="car.usedCarId">
                                        {{ car.brand }} {{ car.modelName }} (ID: {{ car.usedCarId }})
                                    </option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-bold small">客戶 <span class="text-danger">*</span></label>
                                <select v-model="addForm.custId" class="form-select" required>
                                    <option value="" disabled>請選擇客戶</option>
                                    <option v-for="cust in customerOptions" :key="cust.custId" :value="cust.custId"
                                        :disabled="cust.status === 'SUSPENDED'"
                                        :style="cust.status === 'SUSPENDED' ? 'color: #ccc;' : ''">
                                        ID {{ cust.custId }} : {{ cust.custName }} {{ cust.status === 'SUSPENDED' ?
                                            '(已停權)' :
                                            ''
                                        }}
                                    </option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-bold small">經手員工 <span class="text-danger">*</span></label>
                                <select v-model="addForm.empId" class="form-select" required>
                                    <option value="" disabled>請選擇員工</option>
                                    <option v-for="emp in empOptions" :key="emp.empId" :value="emp.empId"
                                        :disabled="emp.status === 'SUSPENDED'"
                                        :style="emp.status === 'SUSPENDED' ? 'color: #ccc;' : ''">
                                        ID {{ emp.empId }} : {{ emp.empName }} {{ emp.status === 'SUSPENDED' ? '(已停權)' :
                                            ''
                                        }}
                                    </option>
                                </select>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label fw-bold small">買受人姓名 <span class="text-danger">*</span></label>
                                <input v-model="addForm.buyerName" type="text" class="form-control" required />
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-bold small">聯繫電話 <span class="text-danger">*</span></label>
                                <input v-model="displayPhone" type="text" class="form-control border-2"
                                    :class="{ 'is-invalid': addForm.buyerPhone && !isPhoneValid }" maxlength="12"
                                    placeholder="0911-001-001" required />
                                <div class="invalid-feedback">請輸入正確的手機格式 (09xxxxxxxx)</div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-bold small">身分證字號 <span class="text-danger">*</span></label>
                                <input v-model="displayIdno" type="text" class="form-control"
                                    :class="{ 'is-invalid': addForm.buyerIdno && !isIdnoValid }"
                                    placeholder="A123456789" maxlength="10" required />
                                <div v-if="addForm.buyerIdno && !isIdnoValid" class="invalid-feedback">
                                    請輸入正確的身分證格式 (如 A123456789)
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label fw-bold small">成交價格 <span class="text-danger">*</span></label>
                                <input v-model="addForm.finalPrice" type="number" class="form-control" required />
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-bold small">成交日期 <span class="text-danger">*</span></label>
                                <input v-model="addForm.saleDate" type="date" class="form-control" required />
                            </div>

                            <div class="col-md-6">
                                <label class="form-label fw-bold small">付款方式 <span class="text-danger">*</span></label>
                                <select v-model="addForm.paymentMethod" class="form-select" required>
                                    <option value="" disabled>請選擇</option>
                                    <option value="CASH">現金</option>
                                    <option value="CREDIT_CARD">信用卡</option>
                                    <option value="TRANSFER">轉帳</option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-bold small">付款狀態 <span class="text-danger">*</span></label>
                                <select v-model="addForm.payStatus" class="form-select" required>
                                    <option value="" disabled>請選擇</option>
                                    <option value="PENDING">待付款</option>
                                    <option value="PAID">已付款</option>
                                    <option value="CANCELLED">已取消</option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-bold small">成交備註</label>
                                <textarea v-model="addForm.notes" class="form-control" rows="3"></textarea>
                            </div>
                        </div>
                    </form>
                </div>

                <div class="modal-footer bg-light">
                    <button class="btn btn-secondary" @click="closeModal">取消</button>
                    <button class="btn btn-primary" @click="submitAdd" :disabled="loading">
                        {{ loading ? '處理中...' : '確認新增' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>