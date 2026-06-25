<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useFavoriteStore } from '@/store/usedCar/useFavoriteStore';

// 🌟 引入共用元件清單 (說明書規範)
import Breadcrumb from '@/components/common/Breadcrumb.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';

const router = useRouter();
const favoriteStore = useFavoriteStore();
const carList = computed(() => favoriteStore.favorites || []);

// 預留後續非同步串接 API 的 Loading 狀態 (說明書第 4 項)
const isLoading = ref(false);

// 對比選擇的車輛 ID
const compareId1 = ref(null);
const compareId2 = ref(null);
const compareId3 = ref(null);

// 控制第三台車（車輛 C）的選單與欄位是否顯示
const showCar3 = ref(false);

// =================【進頁面防護網】=================
onMounted(() => {
    const currentToken = localStorage.getItem('token');
    const favOwnerToken = localStorage.getItem('fav_owner_token');

    if (!currentToken) {
        console.log("【比較頁防護】未登入，禁止存取");
        favoriteStore.$reset();
        localStorage.removeItem('fav_owner_token');
        localStorage.removeItem('my_favorites');
        router.push('/login');
        return;
    }

    if (favOwnerToken && currentToken !== favOwnerToken) {
        console.warn('【比較頁防護】偵測到帳號切換，清空快取...');
        favoriteStore.$reset();
        localStorage.removeItem('my_favorites');
        localStorage.setItem('fav_owner_token', currentToken);
    } else if (!favOwnerToken) {
        localStorage.setItem('fav_owner_token', currentToken);
    }
});

// 🌟 麵包屑資料配置 (符合說明書第 13 項：最後一項不加 to)
const breadcrumbItems = [
    { label: '車輛商城', to: '/usedcarshop' },
    { label: '我的收藏', to: '/usedcarshop/favorites' },
    { label: '車輛比較' }
];

// 根據 ID 找出對應的車輛資料
const car1 = computed(() => carList.value.find(c => c.usedCarId === compareId1.value));
const car2 = computed(() => carList.value.find(c => c.usedCarId === compareId2.value));
const car3 = computed(() => showCar3.value ? carList.value.find(c => c.usedCarId === compareId3.value) : null);

// 欄位定義
const compareFields = [
    {
        label: '品牌/型號',
        key: 'combinedName',
        customRender: (car) => `${car.brand} / ${car.modelName}`
    },
    { label: '車型', key: 'vehicleType' },
    { label: '年份', key: 'manufactureDate', format: (v) => v ? `${new Date(v).getFullYear()} 年` : '未知' },
    { label: '里程數', key: 'mileage', format: (v) => v ? `${v.toLocaleString()} 公里` : '0 公里' },
    { label: '動力', key: 'fuelType' },
    { label: '變速箱', key: 'transmission' },
    {
        label: '排氣量',
        key: 'displacement',
        customRender: (car) => car.displacement === 0 ? '純電動車' : (car.displacement ? `${car.displacement} cc` : '-')
    },
    { label: '車輛所在地', key: 'locationName' },
    { label: '座位數', key: 'seats' },
    { label: '預售價格', key: 'askingPrice', format: (v) => v ? `${parseFloat((v / 10000).toFixed(1))} 萬` : '-' }
];

// 輔助函數：顯示名稱與年份
const getCarOptionName = (car) => {
    const year = car.manufactureDate ? new Date(car.manufactureDate).getFullYear() : car.issuedDate ? new Date(car.issuedDate).getFullYear() : '';
    return year ? `${car.brand} ${car.modelName} (${year}年)` : `${car.brand} ${car.modelName}`;
};

// 處理移除第三台車
const removeCar3 = () => {
    compareId3.value = null;
    showCar3.value = false;
};

// 最低價高亮邏輯
const getLowestPriceId = computed(() => {
    const prices = [];
    if (car1.value?.askingPrice) prices.push({ id: compareId1.value, val: car1.value.askingPrice });
    if (car2.value?.askingPrice) prices.push({ id: compareId2.value, val: car2.value.askingPrice });
    if (car3.value?.askingPrice && showCar3.value) prices.push({ id: compareId3.value, val: car3.value.askingPrice });
    if (prices.length < 2) return null;
    return prices.reduce((min, p) => p.val < min.val ? p : min, prices[0]).id;
});

// 最低里程高亮邏輯
const getLowestMileageId = computed(() => {
    const mileages = [];
    if (car1.value?.mileage !== undefined) mileages.push({ id: compareId1.value, val: car1.value.mileage });
    if (car2.value?.mileage !== undefined) mileages.push({ id: compareId2.value, val: car2.value.mileage });
    if (car3.value?.mileage !== undefined && showCar3.value) mileages.push({ id: compareId3.value, val: car3.value.mileage });
    if (mileages.length < 2) return null; // 🌟 嫌疑犯 1 修正處
    return mileages.reduce((min, m) => m.val < min.val ? m : min, mileages[0]).id;
});
</script>

<template>
    <div class="bg-light min-vh-100 py-5">
        <LoadingSpinner :isLoading="isLoading" overlay />

        <div class="container">

            <Breadcrumb :items="breadcrumbItems" class="mb-4" />

            <div class="d-flex flex-column flex-md-row align-items-md-center justify-content-between mb-4 gap-3">
                <div>
                    <h2 class="fw-bold text-dark mb-1">
                        <font-awesome-icon icon="arrows-left-right" class="text-primary me-2" />車輛規格比較
                    </h2>
                    <p class="text-secondary small mb-0">對比您收藏的車輛數據，助您挑選最劃算的愛車</p>
                </div>

                <button v-if="!showCar3 && carList.length > 2" @click="showCar3 = true"
                    class="btn btn-outline-primary btn-sm rounded-pill px-3 shadow-sm align-self-start align-self-md-center fw-medium">
                    <font-awesome-icon icon="plus" class="me-1" /> 加入第三台車進行比較
                </button>
            </div>

            <div class="row g-3 mb-4 bg-white p-4 rounded-4 shadow-sm border border-light-subtle">
                <div class="col-12" :class="showCar3 ? 'col-md-4' : 'col-md-6'">
                    <label class="form-label fw-bold text-secondary small">選擇車輛 A</label>
                    <div class="input-group">
                        <span class="input-group-text bg-primary text-white border-0 fw-bold">A</span>
                        <select v-model="compareId1" class="form-select border-start-0">
                            <option :value="null">請選擇收藏車輛...</option>
                            <option v-for="car in carList" :key="car.usedCarId" :value="car.usedCarId"
                                :disabled="car.usedCarId === compareId2 || car.usedCarId === compareId3">
                                {{ getCarOptionName(car) }}
                            </option>
                        </select>
                    </div>
                </div>

                <div class="col-12" :class="showCar3 ? 'col-md-4' : 'col-md-6'">
                    <label class="form-label fw-bold text-secondary small">選擇車輛 B</label>
                    <div class="input-group">
                        <span class="input-group-text bg-info text-white border-0 fw-bold">B</span>
                        <select v-model="compareId2" class="form-select border-start-0">
                            <option :value="null">請選擇收藏車輛...</option>
                            <option v-for="car in carList" :key="car.usedCarId" :value="car.usedCarId"
                                :disabled="car.usedCarId === compareId1 || car.usedCarId === compareId3">
                                {{ getCarOptionName(car) }}
                            </option>
                        </select>
                    </div>
                </div>

                <div v-if="showCar3" class="col-12 col-md-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <label class="form-label fw-bold text-secondary small mb-2">選擇車輛 C</label>
                        <button @click="removeCar3" class="btn text-danger btn-sm p-0 mb-2 small fw-medium">
                            <font-awesome-icon icon="circle-xmark" class="me-1" />移除
                        </button>
                    </div>
                    <div class="input-group">
                        <span class="input-group-text bg-secondary text-white border-0 fw-bold">C</span>
                        <select v-model="compareId3" class="form-select border-start-0">
                            <option :value="null">請選擇收藏車輛...</option>
                            <option v-for="car in carList" :key="car.usedCarId" :value="car.usedCarId"
                                :disabled="car.usedCarId === compareId1 || car.usedCarId === compareId2">
                                {{ getCarOptionName(car) }}
                            </option>
                        </select>
                    </div>
                </div>
            </div>

            <div v-if="car1 || car2 || car3"
                class="card border-0 shadow-sm border border-light-subtle rounded-4 overflow-hidden mb-5">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0" style="min-width: 700px;">
                        <thead class="table-dark">
                            <tr>
                                <th style="width: 20%" class="ps-4 text-start">比較項目</th>
                                <th :style="{ width: showCar3 ? '26.6%' : '40%' }" class="text-center">車輛 A</th>
                                <th :style="{ width: showCar3 ? '26.6%' : '40%' }" class="text-center">車輛 B</th>
                                <th v-if="showCar3" style="width: 26.6%" class="text-center">車輛 C</th>
                            </tr>
                        </thead>
                        <tbody class="border-top-0">
                            <tr class="table-light">
                                <td class="fw-bold ps-4 text-start text-secondary small">外觀照片</td>
                                <td>
                                    <div class="p-2 d-flex justify-content-center align-items-center bg-white rounded border border-light shadow-sm"
                                        style="height: 140px;">
                                        <img v-if="car1?.vehicleImgUrl" :src="car1.vehicleImgUrl"
                                            class="img-fluid rounded h-100" style="object-fit: contain;">
                                        <span v-else class="text-muted small">
                                            <font-awesome-icon icon="image"
                                                class="d-block fs-4 mb-1 mx-auto text-black-50" />未選擇
                                        </span>
                                    </div>
                                </td>
                                <td>
                                    <div class="p-2 d-flex justify-content-center align-items-center bg-white rounded border border-light shadow-sm"
                                        style="height: 140px;">
                                        <img v-if="car2?.vehicleImgUrl" :src="car2.vehicleImgUrl"
                                            class="img-fluid rounded h-100" style="object-fit: contain;">
                                        <span v-else class="text-muted small">
                                            <font-awesome-icon icon="image"
                                                class="d-block fs-4 mb-1 mx-auto text-black-50" />未選擇
                                        </span>
                                    </div>
                                </td>
                                <td v-if="showCar3">
                                    <div class="p-2 d-flex justify-content-center align-items-center bg-white rounded border border-light shadow-sm"
                                        style="height: 140px;">
                                        <img v-if="car3?.vehicleImgUrl" :src="car3.vehicleImgUrl"
                                            class="img-fluid rounded h-100" style="object-fit: contain;">
                                        <span v-else class="text-muted small">
                                            <font-awesome-icon icon="image"
                                                class="d-block fs-4 mb-1 mx-auto text-black-50" />未選擇
                                        </span>
                                    </div>
                                </td>
                            </tr>

                            <tr v-for="field in compareFields" :key="field.key">
                                <td class="fw-bold ps-4 text-start text-muted table-light small"
                                    style="font-size: 0.9rem;">{{ field.label }}</td>

                                <td class="text-center" :class="{
                                    'text-danger fw-bold table-danger': (field.key === 'askingPrice' && compareId1 === getLowestPriceId),
                                    'text-success fw-bold table-success': (field.key === 'mileage' && compareId1 === getLowestMileageId)
                                }">
                                    {{ car1 ? (field.customRender ? field.customRender(car1) : field.format ?
                                        field.format(car1[field.key]) : car1[field.key]) : '-' }}
                                </td>

                                <td class="text-center" :class="{
                                    'text-danger fw-bold table-danger': (field.key === 'askingPrice' && compareId2 === getLowestPriceId),
                                    'text-success fw-bold table-success': (field.key === 'mileage' && compareId2 === getLowestMileageId)
                                }">
                                    {{ car2 ? (field.customRender ? field.customRender(car2) : field.format ?
                                        field.format(car2[field.key]) : car2[field.key]) : '-' }}
                                </td>

                                <td v-if="showCar3" class="text-center" :class="{
                                    'text-danger fw-bold table-danger': (field.key === 'askingPrice' && compareId3 === getLowestPriceId),
                                    'text-success fw-bold table-success': (field.key === 'mileage' && compareId3 === getLowestMileageId)
                                }">
                                    {{ car3 ? (field.customRender ? field.customRender(car3) : field.format ?
                                        field.format(car3[field.key]) : car3[field.key]) : '-' }}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div v-else-if="carList.length > 0" class="bg-white rounded-4 border border-light-subtle shadow-sm">
                <EmptyState icon="scale-balanced" message="請在上方選擇收藏車輛進行對比" subMessage="下拉選擇兩台以上的車款，系統會為您自動捕捉最佳性價比！" />
            </div>

            <div v-if="carList.length === 0" class="bg-white rounded-4 border border-light-subtle shadow-sm">
                <EmptyState icon="heart-crack" message="目前收藏夾空空如也" subMessage="必須先將喜歡的車款加入收藏，才能使用精準對比功能喔！">
                    <RouterLink to="/usedcarshop"
                        class="btn btn-primary btn-lg px-5 rounded-pill shadow-sm fw-bold fs-6 mt-2">
                        立即去選購
                    </RouterLink>
                </EmptyState>
            </div>
        </div>
    </div>
</template>