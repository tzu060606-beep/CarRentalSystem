<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import viewingappointmentApi from '@/api/usedcar/viewingappointmentApi';
import usedCarApi from '@/api/usedcar/usedCarApi';
import api from '@/api/index.js';

// 🌟 引入專案規範共用元件
import AlertToast from '@/components/common/AlertToast.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import Breadcrumb from '@/components/common/Breadcrumb.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const route = useRoute();
const router = useRouter();
const myAppointments = ref([]);
const isLoading = ref(true);
const activeTab = ref('ALL');

// 🌟 麵包屑配置：取代舊的返回商城按鈕 (最後一項不加 to)
const breadcrumbItems = [
    { label: '車輛商城', to: '/usedcarshop' },
    { label: '看車預約' }
];

// 彈窗與 Toast 控管
const toast = ref(null);
const isConfirmOpen = ref(false);
const pendingAction = ref(null);
const dialogConfig = ref({
    title: '',
    message: '',
    confirmVariant: 'danger'
});

const formatDateTime = (dateStr) => {
    if (!dateStr) return '時間尚未排定';
    let normalizedDate = dateStr.toString().trim().replace(' ', 'T').replace(' T', 'T');
    const date = new Date(normalizedDate);
    return isNaN(date.getTime()) ? dateStr : date.toLocaleString('zh-TW', {
        year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false
    });
};

const isCarSold = (appt) => {
    if (!appt.carInfo) return false;
    const statusValue = appt.carInfo.usedCarStatus;
    const statusCode = (statusValue && typeof statusValue === 'object') ? statusValue.dbCode : statusValue;
    return statusCode?.toUpperCase() === 'SOLD';
};

const filteredAppointments = computed(() => {
    switch (activeTab.value) {
        case 'ACTIVE':
            return myAppointments.value.filter(a => a.status?.dbCode !== 'CANCELLED' && a.status?.dbCode !== 'COMPLETED' && !isCarSold(a));
        case 'CANCELLED':
            return myAppointments.value.filter(a => a.status?.dbCode === 'CANCELLED' || isCarSold(a));
        case 'COMPLETED':
            return myAppointments.value.filter(a => a.status?.dbCode === 'COMPLETED' && !isCarSold(a));
        default:
            return myAppointments.value;
    }
});

const getBadgeClass = (appt) => {
    if (isCarSold(appt)) return 'bg-dark text-white border border-dark';
    let dbCode = appt.status?.dbCode || appt.status;
    dbCode = dbCode?.toUpperCase();
    switch (dbCode) {
        case 'PENDING': return 'bg-warning-subtle text-warning-emphasis border border-warning';
        case 'CONFIRMED': return 'bg-primary-subtle text-primary-emphasis border border-primary';
        case 'COMPLETED': return 'bg-success-subtle text-success-emphasis border border-success';
        case 'CANCELLED': return 'bg-light text-secondary border';
        default: return 'bg-info-subtle text-info-emphasis border border-info';
    }
};

const fetchAppointments = async () => {
    isLoading.value = true;
    try {
        const urlCustId = route.query.custId || route.params.id;
        const loggedInCustId = localStorage.getItem('customerId');
        const currentCustId = urlCustId || loggedInCustId || 1;

        const response = await viewingappointmentApi.findByCustId(currentCustId);
        if (response.data && response.data.length > 0) {
            const enrichedData = await Promise.all(
                response.data.map(async (appt) => {
                    const carRes = await usedCarApi.getOneDetail(appt.usedCarId).catch(() => ({ data: null }));
                    return {
                        ...appt,
                        carInfo: carRes.data,
                        queueCount: appt.queueCount
                    };
                })
            );
            myAppointments.value = enrichedData;
        } else {
            myAppointments.value = [];
        }
    } catch (error) {
        console.error("讀取失敗", error);
    } finally {
        isLoading.value = false;
    }
};

// 🌟 修改：改用系統共用 ConfirmDialog 彈窗
const handleCancel = (apptId) => {
    dialogConfig.value = {
        title: '取消預約',
        message: '確定要取消此筆看車預約嗎？',
        confirmVariant: 'danger'
    };
    pendingAction.value = async () => {
        try {
            const targetAppt = myAppointments.value.find(a => a.apptId === apptId);
            const updateData = { ...targetAppt, status: 'CANCELLED' };
            await viewingappointmentApi.update(apptId, updateData);
            toast.value.show('預約已成功取消', 'success');
            await fetchAppointments();
        } catch (error) {
            console.error("更新失敗:", error);
            toast.value.show('取消失敗，請稍後再試', 'danger');
        }
    };
    isConfirmOpen.value = true;
};

const handleDialogConfirm = () => {
    if (pendingAction.value) pendingAction.value();
    isConfirmOpen.value = false;
    pendingAction.value = null;
};

const handleDialogCancel = () => {
    isConfirmOpen.value = false;
    pendingAction.value = null;
};

onMounted(fetchAppointments);
</script>

<template>
    <div class="bg-light min-vh-100 py-5">
        <AlertToast ref="toast" />
        <ConfirmDialog :isVisible="isConfirmOpen" :title="dialogConfig.title" :message="dialogConfig.message"
            :confirmVariant="dialogConfig.confirmVariant" @confirm="handleDialogConfirm" @cancel="handleDialogCancel" />

        <div class="container">
            <div class="row justify-content-center">
                <div class="col-12 col-lg-9">

                    <Breadcrumb :items="breadcrumbItems" class="mb-4" />

                    <div class="row align-items-end mb-4 border-bottom pb-3 g-3">
                        <div class="col-12 col-md-6 text-nowrap">
                            <h2 class="fw-bold mb-1 text-dark d-inline-block">
                                <font-awesome-icon icon="calendar-check" class="text-primary me-2" />我的看車預約
                            </h2>
                            <p class="text-secondary small mb-0">管理您的二手車預約看車紀錄</p>
                        </div>

                        <div class="col-12 col-md-6">
                            <div
                                class="d-flex flex-wrap align-items-center gap-2 justify-content-start justify-content-md-end">
                                <router-link to="/usedcarshop/favorites"
                                    class="btn btn-outline-danger btn-sm px-3 shadow-sm rounded-pill fw-medium text-nowrap">
                                    <font-awesome-icon icon="heart" class="me-1" />我的收藏
                                </router-link>
                                <span
                                    class="badge bg-secondary-subtle text-secondary-emphasis rounded-pill px-3 py-2 small fw-semibold text-nowrap">
                                    共 {{ filteredAppointments.length }} 筆紀錄
                                </span>
                            </div>
                        </div>
                    </div>

                    <div
                        class="nav nav-pills mb-4 p-1 bg-white border border-light-subtle rounded-3 d-inline-flex gap-1 shadow-sm">
                        <button class="btn btn-sm px-3 rounded-2 fw-medium"
                            :class="activeTab === 'ALL' ? 'bg-light shadow-sm fw-bold text-dark border-0' : 'text-secondary border-0 bg-transparent'"
                            @click="activeTab = 'ALL'">全部</button>
                        <button class="btn btn-sm px-3 rounded-2 fw-medium"
                            :class="activeTab === 'ACTIVE' ? 'bg-light shadow-sm fw-bold text-primary border-0' : 'text-secondary border-0 bg-transparent'"
                            @click="activeTab = 'ACTIVE'">進行中</button>
                        <button class="btn btn-sm px-3 rounded-2 fw-medium"
                            :class="activeTab === 'COMPLETED' ? 'bg-light shadow-sm fw-bold text-success border-0' : 'text-secondary border-0 bg-transparent'"
                            @click="activeTab = 'COMPLETED'">已完成</button>
                        <button class="btn btn-sm px-3 rounded-2 fw-medium"
                            :class="activeTab === 'CANCELLED' ? 'bg-light shadow-sm fw-bold text-danger border-0' : 'text-secondary border-0 bg-transparent'"
                            @click="activeTab = 'CANCELLED'">已取消</button>
                    </div>

                    <div v-if="isLoading"
                        class="text-center my-5 py-5 bg-white rounded-4 border border-light-subtle shadow-sm">
                        <div class="spinner-border text-primary" role="status"></div>
                        <p class="text-muted small mt-2">資料載入中...</p>
                    </div>

                    <div v-else-if="filteredAppointments.length > 0" class="d-flex flex-column gap-3">
                        <div v-for="appt in filteredAppointments" :key="appt.apptId"
                            class="card bg-white border border-light-subtle shadow-sm rounded-4 overflow-hidden position-relative">

                            <div class="card-body p-3 p-md-4">
                                <div class="row align-items-center g-3">

                                    <div class="col-4 col-sm-3 col-md-2">
                                        <router-link :to="{ name: 'usedcardetail', params: { id: appt.usedCarId } }"
                                            class="d-block bg-light rounded-3 overflow-hidden border ratio ratio-4x3 position-relative">
                                            <img v-if="appt.carInfo?.vehicleImgUrl" :src="appt.carInfo.vehicleImgUrl"
                                                class="w-100 h-100 object-fit-cover"
                                                :class="{ 'opacity-50': isCarSold(appt) }"
                                                :alt="appt.carInfo?.modelName">
                                            <font-awesome-icon icon="car" v-else
                                                class="text-secondary fs-2 position-absolute top-50 start-50 translate-middle" />

                                            <div v-if="isCarSold(appt)"
                                                class="position-absolute top-50 start-50 translate-middle bg-dark text-white text-nowrap px-2 py-0.5 rounded-1 fw-bold small shadow-sm z-3">
                                                已售出
                                            </div>
                                        </router-link>
                                    </div>

                                    <div class="col-8 col-sm-9 col-md-7 col-lg-8">
                                        <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                                            <router-link :to="{ name: 'usedcardetail', params: { id: appt.usedCarId } }"
                                                class="text-decoration-none">
                                                <h5 class="fw-bold mb-0 title-text"
                                                    :class="{ 'text-muted text-decoration-line-through': isCarSold(appt) }">
                                                    {{ appt.carInfo?.brand }} {{ appt.carInfo?.modelName || '' }}
                                                </h5>
                                            </router-link>

                                            <span
                                                :class="['badge px-2 py-1 rounded-2 small fw-semibold', getBadgeClass(appt)]">
                                                <template v-if="isCarSold(appt)">
                                                    <font-awesome-icon icon="triangle-exclamation" class="me-1" />車輛已售出
                                                </template>
                                                <template v-else>
                                                    {{ appt.status?.description || '未知狀態' }}
                                                </template>
                                            </span>

                                            <span
                                                v-if="!isCarSold(appt) && appt.status?.dbCode !== 'CANCELLED' && appt.status?.dbCode !== 'COMPLETED' && appt.queueCount > 0"
                                                class="badge bg-warning-subtle text-warning-emphasis border border-warning rounded-pill px-2 py-1 small fw-medium">
                                                <font-awesome-icon icon="users" class="me-1" />您前面還有 {{ appt.queueCount
                                                }} 人順位
                                            </span>
                                        </div>

                                        <div :class="{ 'opacity-75': isCarSold(appt) }">
                                            <div class="d-flex flex-wrap gap-2 text-secondary small mb-2">
                                                <span class="text-primary-emphasis fw-medium">單號：#{{ appt.apptId
                                                    }}</span>
                                                <span v-if="appt.carInfo?.manufactureYear">• {{
                                                    appt.carInfo.manufactureYear }}年製</span>
                                                <span v-if="appt.carInfo?.milage">• {{ appt.carInfo.milage }} km</span>
                                            </div>

                                            <div
                                                class="d-flex flex-wrap text-secondary small mt-2 row-gap-1 column-gap-4">
                                                <div class="d-flex align-items-center">
                                                    <font-awesome-icon icon="calendar-days" class="text-primary me-2" />
                                                    <span><strong>時間：</strong>{{ formatDateTime(appt.apptTime) }}</span>
                                                </div>
                                                <div class="d-flex align-items-center">
                                                    <font-awesome-icon icon="location-dot" class="text-danger me-2" />
                                                    <span><strong>地點：</strong>{{ appt.carInfo?.locationName || '現場洽詢'
                                                        }}</span>
                                                </div>
                                            </div>
                                        </div>

                                        <div v-if="isCarSold(appt)" class="mt-2 text-danger small fw-semibold">
                                            <font-awesome-icon icon="circle-info" class="me-1" />此車輛已由其他客戶搶先訂購。
                                        </div>
                                    </div>

                                    <div class="col-12 col-md-3 col-lg-2 text-md-end pt-2 pt-md-0">
                                        <template v-if="isCarSold(appt)">
                                            <button
                                                class="btn btn-light btn-sm px-3 py-1.5 rounded-3 fw-medium w-100 w-md-auto text-muted border border-light-subtle"
                                                disabled>
                                                已無法取消
                                            </button>
                                        </template>
                                        <template
                                            v-else-if="appt.status?.dbCode !== 'CANCELLED' && appt.status?.dbCode !== 'COMPLETED'">
                                            <button @click="handleCancel(appt.apptId)"
                                                class="btn btn-outline-danger btn-sm px-3 py-1.5 rounded-3 fw-medium w-100 w-md-auto">
                                                取消預約
                                            </button>
                                        </template>
                                    </div>

                                </div>
                            </div>

                        </div>
                    </div>

                    <div v-else class="bg-white rounded-4 border border-light-subtle shadow-sm my-4">
                        <EmptyState icon="calendar-xmark" message="目前沒有預約紀錄" subMessage="您可以前往車輛列表瀏覽，並挑選心儀車款進行現場看車預約喔！">
                            <router-link to="/usedcarshop"
                                class="btn btn-primary btn-sm px-4 rounded-pill shadow-sm fw-bold mt-2">
                                前往車輛商城
                            </router-link>
                        </EmptyState>
                    </div>

                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.title-text {
    color: #212529;
    transition: color 0.2s ease-in-out;
}

.title-text:hover {
    color: #0d6efd !important;
}

.text-decoration-line-through.title-text:hover {
    color: #6c757d !important;
}
</style>