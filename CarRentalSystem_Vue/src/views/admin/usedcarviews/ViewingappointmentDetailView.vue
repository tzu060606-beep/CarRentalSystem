<script setup>
import { ref, watch } from 'vue';
import BaseModal from '@/components/common/BaseModal.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { customerAPI } from '@/api/login/customerAPI';
import usedCarApi from '@/api/usedcar/usedCarApi';

const props = defineProps({
    isVisible: Boolean,
    selectedAppt: Object
});

const emit = defineEmits(['close', 'sync']);

// 客戶詳細資料狀態
const customerInfo = ref(null);
const carInfo = ref(null); // 新增車輛資料狀態

// 當 selectedAppt 變更時，抓取客戶資料
watch(() => props.selectedAppt, async (newVal) => {
    if (!newVal) return;

    // 同時抓客戶與車輛資料
    try {
        const [custRes, carRes] = await Promise.all([
            customerAPI.getById(newVal.custId).catch(() => ({ data: null })),
            usedCarApi.getOneDetail(newVal.usedCarId).catch(() => ({ data: null }))
        ]);

        customerInfo.value = custRes.data.data || custRes.data;
        carInfo.value = carRes.data.data || carRes.data;
    } catch (e) {
        console.error("資料獲取失敗", e);
    }
}, { immediate: true });
</script>

<template>
    <BaseModal :isVisible="isVisible" :title="'🔍 預約詳細資訊 (編號: ' + (selectedAppt?.apptId || '') + ')'" size="lg"
        @close="$emit('close')">
        <template #body>
            <div v-if="selectedAppt" class="container-fluid p-0">
                <div class="row g-4">
                    <div class="col-md-5">
                        <div class="card border-0 shadow-sm overflow-hidden mb-3">
                            <img v-if="carInfo?.vehicleImgUrl" :src="carInfo.vehicleImgUrl"
                                class="img-fluid object-fit-cover bg-light w-100" style="height: 250px;"
                                @error="$event.target.src = 'https://placehold.co/400x300?text=Image+Not+Found'">

                            <img v-else src="https://placehold.co/400x300?text=No+Image" class="img-fluid w-100">

                            <div class="card-body bg-light border-top">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="small text-secondary fw-bold">車輛 ID</span>
                                    <span class="text-dark fw-bold"># {{ selectedAppt.usedCarId }}</span>
                                </div>
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="small text-secondary fw-bold">車輛品牌/型號</span>
                                    <span class="text-dark fw-bold">{{ carInfo?.brand || '載入中...' }} / {{
                                        carInfo?.modelName || '載入中...' }}</span>
                                </div>
                                <div class="d-flex justify-content-between align-items-center">
                                    <span class="small text-secondary fw-bold">處理狀態</span>
                                    <StatusBadge :status="selectedAppt.status?.dbCode || selectedAppt.status" :map="{
                                        'PENDING': { label: '待處理', variant: 'warning' },
                                        'CONFIRMED': { label: '已確認', variant: 'success' },
                                        'COMPLETED': { label: '已完成', variant: 'primary' },
                                        'CANCELLED': { label: '已取消', variant: 'danger' }
                                    }" />
                                </div>
                            </div>
                        </div>

                        <div
                            class="p-3 bg-primary bg-opacity-10 rounded border border-primary border-opacity-25 text-center">
                            <label class="small text-primary fw-bold d-block mb-1 text-uppercase">
                                <font-awesome-icon icon="calendar-check" class="me-1" />預約看車時間
                            </label>
                            <h5 class="text-primary fw-bold mb-0">{{ selectedAppt.apptTime }}</h5>
                        </div>
                    </div>

                    <div class="col-md-7">
                        <section class="mb-4">
                            <h6 class="fw-bold text-primary mb-3 border-start border-4 border-primary ps-2">客戶資料</h6>
                            <div class="row g-3">
                                <div class="col-6">
                                    <label class="small text-secondary d-block">客戶姓名</label>
                                    <span class="fw-bold text-dark">{{ customerInfo?.custName || '載入中...' }}</span>
                                </div>
                                <div class="col-6">
                                    <label class="small text-secondary d-block">聯絡電話</label>
                                    <span class="fw-bold text-dark">{{ customerInfo?.custPhone || '載入中...' }}</span>
                                </div>
                                <div class="col-12">
                                    <label class="small text-secondary d-block">看車地點</label>
                                    <span class="text-dark">{{ selectedAppt.locationName || '未指定地點' }}</span>
                                </div>
                            </div>
                        </section>

                        <section class="mb-4">
                            <h6 class="fw-bold text-primary mb-3 border-start border-4 border-primary ps-2">預約內容</h6>
                            <div class="bg-light p-3 rounded border">
                                <label class="small text-secondary d-block mb-1">客戶留言訊息</label>
                                <p class="small text-dark mb-3" style="white-space: pre-wrap;">{{ selectedAppt.message
                                    || '無留言內容' }}</p>

                                <label class="small text-secondary d-block mb-1 border-top pt-2">內部管理備註</label>
                                <p class="small text-muted mb-0">{{ selectedAppt.notes || '尚無備註' }}</p>
                            </div>
                        </section>
                    </div>
                </div>
            </div>
        </template>

        <template #footer>
            <div class="d-flex justify-content-between w-100 align-items-center">
                <span class="text-muted small">建立時間：{{ selectedAppt?.createdTime }}</span>
                <button class="btn btn-secondary px-4 shadow-sm" @click="$emit('close')">
                    <font-awesome-icon icon="times" class="me-2" />關閉
                </button>
            </div>
        </template>
    </BaseModal>
</template>