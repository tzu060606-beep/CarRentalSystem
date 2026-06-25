<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useFavoriteStore } from '@/store/usedCar/useFavoriteStore';
import api from '@/api/index.js';

// 🌟 嚴格對齊說明書：引入所有用到的共用元件
import AlertToast from '@/components/common/AlertToast.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import Breadcrumb from '@/components/common/Breadcrumb.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const router = useRouter();
const favoriteStore = useFavoriteStore();
const toast = ref(null);

// 🌟 麵包屑路徑配置 (符合說明書第 13 項：最後一項不加 to)
const breadcrumbItems = [
    { label: '車輛商城', to: '/usedcarshop' },
    { label: '我的收藏' }
];

// 用於彈窗非同步動作控管
const pendingAction = ref(null);

// ConfirmDialog 狀態控管
const isConfirmOpen = ref(false);
const dialogConfig = ref({
    title: '',
    message: '',
    confirmVariant: 'danger' // 對齊說明書第 2 項規範的 variant 命名
});

// =================【進頁面身分防護網】=================
onMounted(() => {
    const currentToken = localStorage.getItem('token');
    const favOwnerToken = localStorage.getItem('fav_owner_token');

    // 第一關：嚴格檢查登入狀態
    if (!currentToken) {
        console.log("【收藏頁防護】未登入，禁止存取");
        favoriteStore.$reset();
        localStorage.removeItem('fav_owner_token');
        localStorage.removeItem('my_favorites');

        if (toast.value) toast.value.show('請先登入！', 'warning', 1500);
        setTimeout(() => {
            router.push('/login');
        }, 1500);
        return;
    }

    // 第二關：檢查身分一致性（換帳號防護）
    if (favOwnerToken && currentToken !== favOwnerToken) {
        console.warn('【收藏頁防護】偵測到帳號切換，正在清空前位使用者的收藏快取...');
        favoriteStore.$reset();
        localStorage.removeItem('my_favorites');
        localStorage.setItem('fav_owner_token', currentToken);
    } else if (!favOwnerToken) {
        localStorage.setItem('fav_owner_token', currentToken);
    }

    // 第三關：放行
    console.log("【收藏頁放行】身分驗證通過，正常載入收藏清單");
});

// 移除單一車輛
const handleRemove = (car) => {
    dialogConfig.value = {
        title: '移除收藏',
        message: `確定要將「${car.brand} ${car.modelName}」從收藏清單移除嗎？`,
        confirmVariant: 'danger'
    };
    pendingAction.value = () => {
        favoriteStore.toggleFavorite(car);
        toast.value.show(`已將「${car.brand} ${car.modelName}」移出收藏`, 'success');
    };
    isConfirmOpen.value = true;
};

// 清空全部收藏的邏輯
const handleClearAll = () => {
    dialogConfig.value = {
        title: '清空收藏',
        message: '確定要清空所有的收藏車輛嗎？此動作無法復原。',
        confirmVariant: 'danger'
    };
    pendingAction.value = () => {
        favoriteStore.favorites = [];
        localStorage.removeItem('my_favorites');
        toast.value.show('已成功清空所有收藏！', 'success');
    };
    isConfirmOpen.value = true;
};

// 當按下對話框的確認鈕時觸發
const handleDialogConfirm = () => {
    if (pendingAction.value) {
        pendingAction.value();
    }
    isConfirmOpen.value = false;
    pendingAction.value = null;
};

// 當按下對話框的取消鈕時觸發
const handleDialogCancel = () => {
    isConfirmOpen.value = false;
    pendingAction.value = null;
};
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
                        <div class="col-12 col-md-5 col-lg-6 text-nowrap">
                            <h2 class="fw-bold mb-1 text-dark">
                                <font-awesome-icon icon="heart" class="text-danger me-2" />我的收藏清單
                            </h2>
                            <p class="text-muted small mb-0">管理您感興趣的車輛，隨時掌握最新狀態</p>
                        </div>

                        <div class="col-12 col-md-7 col-lg-6">
                            <div
                                class="d-flex flex-wrap align-items-center gap-2 justify-content-start justify-content-md-end">
                                <router-link to="/usedcar/appointment"
                                    class="btn btn-outline-primary btn-sm px-3 shadow-sm rounded-pill fw-medium text-nowrap">
                                    <font-awesome-icon icon="calendar-check" class="me-1" />我的看車預約
                                </router-link>

                                <router-link to="/usedcarshop/compare"
                                    class="btn btn-outline-dark btn-sm px-3 shadow-sm rounded-pill fw-medium text-nowrap">
                                    <font-awesome-icon icon="arrows-left-right" class="me-1" />前往車輛比較
                                </router-link>
                            </div>
                        </div>
                    </div>

                    <div v-if="favoriteStore.favorites.length > 0">
                        <div v-for="car in favoriteStore.favorites" :key="car.usedCarId"
                            class="card mb-4 shadow-sm border border-light-subtle rounded-4 overflow-hidden bg-white">
                            <div class="row g-0 align-items-center">

                                <div class="col-12 col-md-3 position-relative bg-light">
                                    <router-link :to="{ name: 'usedcardetail', params: { id: car.usedCarId } }"
                                        class="d-block overflow-hidden transition-scale">
                                        <div class="ratio ratio-4x3">
                                            <img :src="car.vehicleImgUrl" class="w-100 h-100 object-fit-cover"
                                                :alt="car.modelName">
                                        </div>
                                    </router-link>

                                    <div v-if="car.usedCarStatus?.dbCode !== 'ACTIVE'"
                                        class="position-absolute top-0 start-0 w-100 h-100 bg-dark bg-opacity-50 d-flex align-items-center justify-content-center text-white fw-bold fs-5"
                                        style="pointer-events: none;"> <span
                                            class="badge bg-danger px-3 py-2 rounded-pill shadow-sm">
                                            <font-awesome-icon icon="ban" class="me-1" /> 已售出
                                        </span>
                                    </div>
                                </div>

                                <div class="col-12 col-md-6">
                                    <div class="card-body p-4">
                                        <div class="d-flex align-items-center mb-3 flex-wrap gap-2">
                                            <span class="badge bg-primary rounded-pill">{{ car.brand }}</span>
                                            <router-link :to="{ name: 'usedcardetail', params: { id: car.usedCarId } }"
                                                class="text-decoration-none text-dark car-title-link">
                                                <h5 class="card-title fw-bold mb-0 fs-5 d-inline">{{ car.modelName }}
                                                </h5>
                                            </router-link>
                                        </div>

                                        <div class="d-flex gap-2 text-secondary mb-3 small fw-medium">
                                            <span><font-awesome-icon icon="location-dot" class="me-1" />{{
                                                car.locationName }}</span>
                                            <span class="text-secondary-subtle">|</span>
                                            <span><font-awesome-icon icon="calendar-days" class="me-1" />{{
                                                car.manufactureDate ? new Date(car.manufactureDate).getFullYear() : '未知'
                                                }} 年</span>
                                        </div>

                                        <div class="row g-2 mb-0">
                                            <div class="col-6 col-lg-5">
                                                <div
                                                    class="p-2 border border-light-subtle rounded-3 bg-light text-center small text-muted">
                                                    <font-awesome-icon icon="gauge-high" class="me-1 text-primary" />
                                                    {{ car.mileage ? car.mileage.toLocaleString() : 0 }} km
                                                </div>
                                            </div>
                                            <div class="col-6 col-lg-5">
                                                <div
                                                    class="p-2 border border-light-subtle rounded-3 bg-light text-center small text-muted fw-medium">
                                                    <font-awesome-icon icon="bolt-lightning" class="me-1 text-warning"
                                                        v-if="car.displacement === 0" />
                                                    <font-awesome-icon icon="car" class="me-1 text-secondary" v-else />
                                                    {{ car.displacement === 0 ? '純電動車' : `${car.displacement} c.c.` }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="col-12 col-md-3 border-start border-light-subtle p-4">
                                    <div class="d-grid gap-2 text-center text-md-start">
                                        <div class="mb-3">
                                            <span class="text-muted small d-block mb-1">預售價格</span>
                                            <span
                                                :class="car.usedCarStatus?.dbCode === 'ACTIVE' ? 'text-danger' : 'text-secondary text-decoration-line-through'"
                                                class="fs-3 fw-bold">
                                                $ {{ car.askingPrice ? parseFloat((car.askingPrice / 10000).toFixed(1))
                                                    : '電洽' }} 萬
                                            </span>
                                        </div>

                                        <router-link v-if="car.usedCarStatus?.dbCode === 'ACTIVE'"
                                            :to="{ name: 'ViewTable', query: { carId: car.usedCarId } }"
                                            class="btn btn-success btn-sm fw-bold py-2 rounded-pill shadow-sm">
                                            <font-awesome-icon icon="calendar-check" class="me-2" />我要預約
                                        </router-link>

                                        <button v-else disabled
                                            class="btn btn-secondary btn-sm py-2 rounded-pill opacity-75">
                                            <font-awesome-icon icon="ban" class="me-2" />此車已售出
                                        </button>

                                        <button @click="handleRemove(car)"
                                            class="btn btn-link text-secondary btn-sm mt-1 text-decoration-none hover-danger">
                                            <font-awesome-icon icon="trash-can" class="me-1" />移除收藏
                                        </button>
                                    </div>
                                </div>

                            </div>
                        </div>

                        <div class="text-end mt-4 pt-2">
                            <button @click="handleClearAll"
                                class="btn btn-link text-muted btn-sm text-decoration-none hover-danger">
                                <font-awesome-icon icon="trash-can" class="me-1" /> 清空全部收藏車輛
                            </button>
                        </div>
                    </div>

                    <div v-else class="bg-white rounded-4 border border-light-subtle shadow-sm my-4">
                        <EmptyState icon="heart-crack" message="目前沒有收藏車輛"
                            subMessage="快去商城看看有沒有心儀的車款吧！加入收藏後即可進行多車規格比對功能喔！">
                            <RouterLink to="/usedcarshop"
                                class="btn btn-primary btn-sm px-4 rounded-pill shadow-sm fw-bold mt-2">
                                立即去選購
                            </RouterLink>
                        </EmptyState>
                    </div>

                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.hover-danger:hover {
    color: #dc3545 !important;
}

/* 🌟 精緻細節：滑入車名時變色並出現底線，增加點擊暗示 */
.car-title-link:hover .card-title {
    color: #0d6efd !important;
    text-decoration: underline;
}
</style>