<script setup>
import { ref, watch } from 'vue';
import BaseModal from '@/components/common/BaseModal.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import usedCarApi from '@/api/usedcar/usedCarApi';
import { employeeAPI } from '@/api/login/employeeAPI';

const props = defineProps({
    isVisible: Boolean,
    selectedSales: Object
});

const emit = defineEmits(['close']);

const details = ref({
    car: null,
    employee: null
});

const loading = ref(false);

// 當 selectedSales 改變時，自動去撈詳細資料
watch(() => props.selectedSales, async (newVal) => {
    if (newVal && props.isVisible) {
        loading.value = true;
        try {
            // 同時並行撈取車輛與員工資料
            const [carRes, empRes] = await Promise.all([
                usedCarApi.getOneDetail(newVal.usedCarId),
                employeeAPI.getById(newVal.empId)
            ]);
            details.value.car = carRes.data.data || carRes.data;
            details.value.employee = empRes.data.data || empRes.data;
        } catch (error) {
            console.error("載入關聯資料失敗", error);
        } finally {
            loading.value = false;
        }
    }
}, { immediate: true });

</script>

<template>
    <BaseModal :isVisible="isVisible" :title="'🔍 成交紀錄詳情：#' + (selectedSales?.saleId || '')" size="lg"
        @close="$emit('close')">
        <template #body>
            <div v-if="selectedSales" class="container-fluid p-0">
                <div class="row g-4">
                    <!-- 左側：摘要卡片區 -->
                    <div class="col-md-5">
                        <!-- 使用 Bootstrap 內建 sticky-top 並透過 :style 指定 offset -->
                        <div class="sticky-top" :style="{ top: '10px' }">
                            <div class="card border-0 shadow-sm overflow-hidden mb-3">
                                <!-- 移除 border-bottom 的類別為 border-bottom-0 -->
                                <div class="card-header bg-success text-white py-3 text-center border-bottom-0">
                                    <div class="small opacity-75">成交總金額</div>
                                    <div class="fs-2 fw-bold">${{ selectedSales.finalPrice?.toLocaleString() }}</div>
                                </div>
                                <div class="card-body bg-light">
                                    <div class="d-flex justify-content-between align-items-center mb-2">
                                        <span class="small text-secondary">成交編號</span>
                                        <span class="badge bg-dark fs-6">#{{ selectedSales.saleId }}</span>
                                    </div>
                                    <div class="d-flex justify-content-between align-items-center">
                                        <span class="small text-secondary">成交日期</span>
                                        <span class="fw-bold">{{ selectedSales.saleDate }}</span>
                                    </div>
                                </div>
                            </div>

                            <!-- 付款狀態區：使用 :style 達成虛線效果 -->
                            <div class="p-3 bg-light rounded border text-center" :style="{ borderStyle: 'dashed' }">
                                <div class="fw-bold small text-secondary mb-2">付款方式</div>
                                <StatusBadge
                                    :status="selectedSales.paymentMethod?.dbCode || selectedSales.paymentMethod" :map="{
                                        'CASH': { label: '💵 現金支付', variant: 'success' },
                                        'CREDIT_CARD': { label: '💳 信用卡', variant: 'primary' },
                                        'TRANSFER': { label: '🏦 銀行轉帳', variant: 'info' }
                                    }" />
                            </div>

                            <div class="p-3 bg-light rounded border text-center" :style="{ borderStyle: 'dashed' }">
                                <div class="fw-bold small text-secondary mb-2">付款狀態</div>
                                <StatusBadge :status="selectedSales.payStatus?.dbCode || selectedSales.payStatus" :map="{
                                    'PENDING': { label: '💵 待付款', variant: 'warning' },
                                    'PAID': { label: '💳 已付款', variant: 'success' },
                                    'CANCELLED': { label: '🏦 已取消', variant: 'danger' }
                                }" />
                            </div>
                        </div>
                    </div>

                    <!-- 右側：資料詳情區 -->
                    <div class="col-md-7">
                        <!-- 買受人資訊 -->
                        <section class="mb-4">
                            <h6 class="fw-bold text-primary mb-3 border-start border-4 border-primary ps-2">買受人資訊</h6>
                            <div class="row g-3">
                                <div class="col-6">
                                    <label class="small text-secondary d-block">姓名</label>
                                    <span class="fw-bold">{{ selectedSales.buyerName }}</span>
                                    <span class="small text-muted ms-1">(ID: {{ selectedSales.custId }})</span>
                                </div>
                                <div class="col-6">
                                    <label class="small text-secondary d-block">聯繫電話</label>
                                    <span>{{ selectedSales.buyerPhone || '未提供' }}</span>
                                </div>
                                <div class="col-12">
                                    <label class="small text-secondary d-block">身分證 / 統一編號</label>
                                    <span>{{ selectedSales.buyerIdno || '未提供' }}</span>
                                </div>
                            </div>
                        </section>

                        <!-- 車輛與經辦資訊 -->
                        <section class="mb-4">
                            <h6 class="fw-bold text-primary mb-3 border-start border-4 border-primary ps-2">車輛與經辦資訊</h6>
                            <div class="bg-light p-3 rounded">
                                <div class="row g-3">
                                    <div class="col-6">
                                        <label class="small text-secondary d-block">售出車輛</label>
                                        <div v-if="loading" class="spinner-border spinner-border-sm text-primary"
                                            role="status"></div>
                                        <div v-else-if="details.car" class="d-flex align-items-center">
                                            <img :src="details.car.vehicleImgUrl" alt="車輛圖片"
                                                class="rounded shadow-sm me-2"
                                                style="width: 60px; height: 45px; object-fit: cover;">
                                            <div>
                                                <div class="fw-bold text-dark">{{ details.car.brand }} {{
                                                    details.car.modelName }}</div>
                                                <div class="small text-muted">#{{ selectedSales.usedCarId }}</div>
                                            </div>
                                        </div>
                                        <div v-else class="text-danger small">資料讀取失敗</div>
                                    </div>

                                    <div class="col-6">
                                        <label class="small text-secondary d-block">經辦員工</label>
                                        <div v-if="loading" class="spinner-border spinner-border-sm text-primary"
                                            role="status"></div>
                                        <div v-else>
                                            <span class="fw-bold text-dark">
                                                {{ details.employee ? details.employee.empName : '資料讀取失敗' }}
                                            </span>
                                            <div class="small text-muted">ID: {{ selectedSales.empId }}</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </section>

                        <!-- 備註資訊 -->
                        <section>
                            <h6 class="fw-bold text-primary mb-3 border-start border-4 border-primary ps-2">成交備註</h6>
                            <div class="card border-0 bg-warning bg-opacity-10 p-3">
                                <div class="d-flex align-items-start">
                                    <i class="bi bi-sticky text-warning me-2 mt-1"></i>
                                    <p class="small mb-0 text-dark" :style="{ whiteSpace: 'pre-wrap' }">
                                        {{ selectedSales.notes || '此筆交易無相關備註事項。' }}
                                    </p>
                                </div>
                            </div>
                        </section>
                    </div>
                </div>
            </div>
        </template>

        <template #footer>
            <div class="d-flex justify-content-between w-100 align-items-center text-muted small">
                <span>系統最後更新：{{ selectedSales?.updatedTime || 'N/A' }}</span>
                <button class="btn btn-secondary px-4 shadow-sm" @click="$emit('close')">關閉詳情</button>
            </div>
        </template>
    </BaseModal>
</template>