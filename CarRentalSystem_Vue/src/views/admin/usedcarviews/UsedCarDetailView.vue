<script setup>
import BaseModal from '@/components/common/BaseModal.vue';
import StatusBadge from '@/components/common/StatusBadge.vue'; // 引入共用元件

const props = defineProps({
    isVisible: Boolean,
    selectedVehicle: Object
});

defineEmits(['close']);
</script>

<template>
    <BaseModal :isVisible="isVisible" :title="'🔍 車輛詳細資訊：' + (selectedVehicle?.plateNo || '')" size="lg"
        @close="$emit('close')">
        <template #body>
            <div v-if="selectedVehicle" class="container-fluid p-0">
                <div class="row g-4">
                    <!-- 左側：圖片區 -->
                    <div class="col-md-5">
                        <div class="sticky-top" style="top: 10px;">
                            <div class="card border-0 shadow-sm overflow-hidden">
                                <img v-if="selectedVehicle.vehicleImgUrl" :src="selectedVehicle.vehicleImgUrl"
                                    class="img-fluid object-fit-contain bg-light" style="height: 300px; width: 100%;">
                                <img v-else src="https://via.placeholder.com/400x300?text=No+Image" class="img-fluid">
                                <div class="card-body bg-light">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <span class="small text-secondary">車牌號碼</span>
                                        <span class="badge bg-dark fs-6">{{ selectedVehicle.plateNo }}</span>
                                    </div>
                                </div>
                            </div>

                            <!-- 狀態顯示區 -->
                            <div class="mt-3 p-3 bg-light rounded border border-dashed">
                                <div class="d-flex justify-content-between align-items-center">
                                    <span class="fw-bold small text-secondary">目前車輛狀態</span>
                                    <StatusBadge
                                        :status="selectedVehicle.usedCarStatus?.dbCode || selectedVehicle.usedCarStatus"
                                        :map="{
                                            ACTIVE: { label: '上架中', variant: 'success' },
                                            SOLD: { label: '已售出', variant: 'primary' },
                                            REMOVED: { label: '下架', variant: 'warning' }
                                        }" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 右側：資料詳情區 -->
                    <div class="col-md-7">
                        <!-- 基本規格 -->
                        <section class="mb-4">
                            <h6 class="fw-bold text-primary mb-3 border-start border-4 border-primary ps-2">基本規格</h6>
                            <div class="row g-3">
                                <div class="col-6">
                                    <label class="small text-secondary d-block">品牌/車款</label>
                                    <span class="fw-bold">{{ selectedVehicle.brand }} {{ selectedVehicle.modelName
                                        }}</span>
                                </div>
                                <div class="col-6">
                                    <label class="small text-secondary d-block">動力/排氣量</label>
                                    <span>{{ selectedVehicle.fuelType }} / {{ selectedVehicle.displacement }}cc</span>
                                </div>
                                <div class="col-6">
                                    <label class="small text-secondary d-block">座位/行李數</label>
                                    <span>{{ selectedVehicle.seats }}人座 / {{ selectedVehicle.luggageCapacity }}件</span>
                                </div>
                                <div class="col-6">
                                    <label class="small text-secondary d-block">車型</label>
                                    <span>{{ selectedVehicle.vehicleType }}</span>
                                </div>
                            </div>
                        </section>

                        <!-- 車況資訊 -->
                        <section class="mb-4">
                            <h6 class="fw-bold text-primary mb-3 border-start border-4 border-primary ps-2">營運與維護</h6>
                            <div class="bg-light p-3 rounded">
                                <div class="row g-3">
                                    <div class="col-6">
                                        <label class="small text-secondary d-block">累積里程</label>
                                        <span class="fw-bold">{{ selectedVehicle.mileage?.toLocaleString() }} km</span>
                                    </div>
                                    <div class="col-6">
                                        <label class="small text-secondary d-block">下次保養里程</label>
                                        <span class="text-danger fw-bold">{{
                                            selectedVehicle.nextMaintainenceMileage?.toLocaleString() }} km</span>
                                    </div>
                                    <div class="col-12">
                                        <label class="small text-secondary d-block">車況描述</label>
                                        <p class="small mb-0 text-dark">{{ selectedVehicle.description || '無描述' }}</p>
                                    </div>
                                </div>
                            </div>
                        </section>

                        <!-- 地點資訊 -->
                        <section>
                            <h6 class="fw-bold text-primary mb-3 border-start border-4 border-primary ps-2">所在據點</h6>
                            <div class="card border-0 bg-primary bg-opacity-10 p-3">
                                <div class="d-flex align-items-center mb-2">
                                    <i class="bi bi-geo-alt-fill text-primary me-2"></i>
                                    <span class="fw-bold text-dark">{{ selectedVehicle.locationName }}</span>
                                </div>
                                <div class="small text-secondary">
                                    <div>地址：{{ selectedVehicle.address }}</div>
                                    <div>電話：{{ selectedVehicle.phone }}</div>
                                </div>
                            </div>
                        </section>
                    </div>
                </div>
            </div>
        </template>

        <template #footer>
            <div class="d-flex justify-content-between w-100 align-items-center text-muted small">
                <span>建立時間：{{ selectedVehicle?.createdTime }}</span>
                <button class="btn btn-secondary px-4" @click="$emit('close')">關閉詳情</button>
            </div>
        </template>
    </BaseModal>
</template>