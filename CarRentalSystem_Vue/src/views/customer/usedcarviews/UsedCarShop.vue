<script setup>
import { ref, onMounted, computed } from 'vue';
import usedCarApi from '@/api/usedcar/usedCarApi';
import ecPay from '@/components/admin/usedcar/ecPay';
import { useFavoriteStore } from '@/store/usedCar/useFavoriteStore';
import { useRouter } from 'vue-router';
import api from '@/api/index.js';

const favoriteStore = useFavoriteStore();
const carList = ref([]);
const router = useRouter();

// =================【篩選器狀態定義】=================
const searchKeyword = ref('');       // 關鍵字搜尋
const selectedBrand = ref('不限');    // 廠牌篩選
const selectedPriceRange = ref('不限');// 價格快選
const minPriceInput = ref('');       // 自訂價格(低)
const maxPriceInput = ref('');       // 自訂價格(高)
const appliedMinPrice = ref(null);   // 確定套用的價格(低)
const appliedMaxPrice = ref(null);   // 確定套用的價格(高)

// 下拉進階篩選
const selectedAge = ref('不限');
const selectedLocation = ref('不限');
const selectedVehicleType = ref('不限');

// =================【篩選器靜態選單資料】=================
const hotBrands = [
    { name: '/不限', icon: 'bi-ban' },
    { name: 'Toyota/豐田Toyota', icon: 'bi-circle' },
    { name: 'Honda/本田Honda', icon: 'bi-h-square' },
    { name: 'Tesla/特斯拉Tesla', icon: 'bi-lightning-charge' },
];

const priceRanges = ['不限', '20萬以下', '20-30萬', '30-50萬', '50-80萬', '80-150萬', '150萬以上'];
const ageOptions = ['不限', '3年內', '3-5年', '5-10年', '10年以上'];
const locationOptions = ['不限', '台北市', '新竹市'];
const typeOptions = ['不限', '小型轎車', '中型轎車', '休旅車', '廂型車', '電動車'];

// 處理自訂價格按鈕點擊
const applyCustomPrice = () => {
    selectedPriceRange.value = '自訂';
    appliedMinPrice.value = minPriceInput.value ? parseFloat(minPriceInput.value) * 10000 : null;
    appliedMaxPrice.value = maxPriceInput.value ? parseFloat(maxPriceInput.value) * 10000 : null;
};

// 切換快選價格時，清空自訂輸入框
const handlePriceRangeChange = (range) => {
    selectedPriceRange.value = range;
    if (range !== '自訂') {
        minPriceInput.value = '';
        maxPriceInput.value = '';
        appliedMinPrice.value = null;
        appliedMaxPrice.value = null;
    }
};

// =================【頁面動作邏輯】=================
const goToAppointment = (id) => {
    router.push({
        name: 'ViewTable',
        query: { carId: id }
    });
};

// 點擊愛心收藏按鈕
const addToFavorite = (car) => {
    const currentToken = localStorage.getItem('token');

    // 訪客沒登入，提示去登入
    if (!currentToken) {
        if (window.confirm("請先登入後再使用收藏功能，是否前往登入？")) {
            router.push('/login');
        }
        return;
    }

    // 如果有登入，按愛心時順便蓋上目前帳號的 Token 印章
    favoriteStore.toggleFavorite(car);
    localStorage.setItem('fav_owner_token', currentToken);
};

// 載入車輛清單並做帳號校正
const loadCarList = async () => {
    try {
        const currentToken = localStorage.getItem('token');
        const favOwnerToken = localStorage.getItem('fav_owner_token');

        // 🌟 【帳號切換主動防護】
        // 情況 1：如果當前根本沒人登入(currentToken 是空)，但本地卻留有上個人的最愛印章，清空它！
        // 情況 2：如果當前有人登入，但目前的 Token 跟當初存這份最愛的 Token 不一樣(換帳號了)，清空它！
        if (!currentToken && favOwnerToken) {
            console.log("【商城防護】訪客狀態，清空前人留下的收藏夾...");
            favoriteStore.$reset();
            localStorage.removeItem('fav_owner_token');
        } else if (currentToken && favOwnerToken && currentToken !== favOwnerToken) {
            console.warn("【商城防護】偵測到帳號已切換，自動重置收藏夾...");
            favoriteStore.$reset();
            localStorage.setItem('fav_owner_token', currentToken); // 綁定給新帳號
        } else if (currentToken && !favOwnerToken) {
            // 如果當前有人登入，但本地還沒有印章，順手幫他補上印章
            localStorage.setItem('fav_owner_token', currentToken);
        }

        // 撈取後端 Spring Boot 最新車況資料
        const response = await usedCarApi.getAllDetails();
        carList.value = response.data;

        // 用最新車況校正 Pinia 收藏夾狀態
        if (favoriteStore.favorites && favoriteStore.favorites.length > 0) {
            favoriteStore.favorites.forEach(favCar => {
                const realTimeCar = carList.value.find(c => c.usedCarId === favCar.usedCarId);
                if (realTimeCar) {
                    favCar.usedCarStatus = realTimeCar.usedCarStatus;
                    if (realTimeCar.status) {
                        favCar.status = realTimeCar.status;
                    }
                }
            });
        }

    } catch (error) {
        console.error("無法載入車輛清單", error);
    }
};

// =================【多條件複合篩選邏輯】=================
const displayCars = computed(() => {
    return carList.value.filter(car => {
        const statusCode = car.usedCarStatus?.dbCode;
        if (statusCode !== 'ACTIVE' && statusCode !== 'SOLD') return false;

        if (searchKeyword.value.trim()) {
            const keyword = searchKeyword.value.toLowerCase();
            const brandMatch = car.brand?.toLowerCase().includes(keyword);
            const modelMatch = car.modelName?.toLowerCase().includes(keyword);
            if (!brandMatch && !modelMatch) return false;
        }

        if (selectedBrand.value !== '不限') {
            const brandClean = selectedBrand.value.split('/')[0].toLowerCase();
            if (!car.brand?.toLowerCase().includes(brandClean)) return false;
        }

        const price = car.askingPrice || 0;
        if (selectedPriceRange.value !== '不限') {
            if (selectedPriceRange.value === '自訂') {
                if (appliedMinPrice.value && price < appliedMinPrice.value) return false;
                if (appliedMaxPrice.value && price > appliedMaxPrice.value) return false;
            } else {
                if (selectedPriceRange.value === '20萬以下' && price > 200000) return false;
                if (selectedPriceRange.value === '20-30萬' && (price < 200000 || price > 300000)) return false;
                if (selectedPriceRange.value === '30-50萬' && (price < 300000 || price > 500000)) return false;
                if (selectedPriceRange.value === '50-80萬' && (price < 500000 || price > 800000)) return false;
                if (selectedPriceRange.value === '80-150萬' && (price < 800000 || price > 1500000)) return false;
                if (selectedPriceRange.value === '150萬以上' && price < 1500000) return false;
            }
        }

        if (selectedAge.value !== '不限' && car.manufactureDate) {
            const currentYear = new Date().getFullYear();
            const carYear = new Date(car.manufactureDate).getFullYear();
            const age = currentYear - carYear;
            if (selectedAge.value === '3年內' && age > 3) return false;
            if (selectedAge.value === '3-5年' && (age < 3 || age > 5)) return false;
            if (selectedAge.value === '5-10年' && (age < 5 || age > 10)) return false;
            if (selectedAge.value === '10年以上' && age < 10) return false;
        }

        if (selectedLocation.value !== '不限') {
            if (!car.locationName?.includes(selectedLocation.value.replace('市', ''))) return false;
        }

        if (selectedVehicleType.value !== '不限') {
            if (car.vehicleType !== selectedVehicleType.value) return false;
        }

        return true;
    });
});

// 導覽列收藏小數字（只計算 ACTIVE 且 Token 正確時）
const favoriteCount = computed(() => {
    const currentToken = localStorage.getItem('token');
    const favOwnerToken = localStorage.getItem('fav_owner_token');
    if (!currentToken || currentToken !== favOwnerToken) return 0;
    return favoriteStore.favorites.filter(car => car.usedCarStatus?.dbCode === 'ACTIVE').length;
});

// 🌟 【統一唯一的掛載點】
onMounted(() => {
    loadCarList();
});
</script>

<template>
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="fw-bold m-0">二手車商城</h2>
        <div class="d-flex gap-2">
            <router-link to="/usedcar/appointment" class="btn btn-outline-primary">
                查看我的預約
            </router-link>
            <router-link to="/usedcarshop/favorites" class="btn btn-outline-danger shadow-sm">
                <font-awesome-icon :icon="['fas', 'heart']" class="me-2" />
                查看我的收藏 ({{ favoriteCount }})
            </router-link>
        </div>
    </div>

    <div class="card border-0 shadow-sm p-4 rounded-3 mb-4 bg-white border-light">
        <div class="row align-items-start g-3 pb-3 border-bottom">
            <div class="col-auto pt-1">
                <span class="fw-bold text-secondary fs-6">廠牌</span>
            </div>
            <div class="col">
                <div class="d-flex flex-wrap gap-2 align-items-center">
                    <!-- <button class="btn btn-sm rounded-pill px-3"
                        :class="selectedBrand === '不限' ? 'btn-warning text-white fw-bold' : 'btn-light text-dark'"
                        @click="selectedBrand = '不限'">
                        不限
                    </button>
                    <button class="btn btn-sm btn-warning rounded-pill text-white fw-bold px-3">熱門</button> -->

                    <div class="d-flex flex-wrap gap-3 ms-2 align-items-center">
                        <button v-for="brand in hotBrands" :key="brand.name"
                            class="btn btn-link btn-sm text-decoration-none p-0 align-middle"
                            :class="selectedBrand === brand.name ? 'text-warning fw-bold fs-6' : 'text-secondary'"
                            @click="selectedBrand = brand.name">
                            <i :class="['bi', brand.icon, 'me-1']"></i>{{ brand.name.split('/')[1] }}
                        </button>
                    </div>
                </div>
            </div>
            <!-- <div class="col-12 col-md-3">
                <div class="input-group input-group-sm">
                    <input v-model="searchKeyword" type="text" class="form-control border-end-0 shadow-none"
                        placeholder="請輸入車輛關鍵字...">
                    <span class="input-group-text bg-warning text-white border-start-0 border-warning px-3">
                        <i class="bi bi-search"></i>
                    </span>
                </div>
            </div> -->
        </div>

        <div class="row align-items-center g-3 py-3 border-bottom">
            <div class="col-auto">
                <span class="fw-bold text-secondary fs-6">價格</span>
            </div>
            <div class="col">
                <div class="d-flex flex-wrap gap-2 align-items-center">
                    <button v-for="range in priceRanges" :key="range" class="btn btn-sm rounded-pill px-3"
                        :class="selectedPriceRange === range ? 'btn-warning text-white fw-bold' : 'btn-light text-dark'"
                        @click="handlePriceRangeChange(range)">
                        {{ range }}
                    </button>

                    <div class="d-flex align-items-center ms-md-3 gap-2 col-12 col-md-auto mt-2 mt-md-0">
                        <input v-model="minPriceInput" type="number"
                            class="form-control form-control-sm text-center shadow-none row-cols-1" placeholder="-">
                        <span class="text-muted small">至</span>
                        <input v-model="maxPriceInput" type="number"
                            class="form-control form-control-sm text-center shadow-none row-cols-1" placeholder="-">
                        <span class="text-secondary small">萬</span>
                        <button @click="applyCustomPrice" class="btn btn-sm btn-outline-secondary px-3 ms-1">確定</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="row align-items-center g-3 pt-3">
            <div class="col-auto">
                <span class="fw-bold text-secondary fs-6">其他</span>
            </div>
            <div class="col">
                <div class="row row-cols-2 row-cols-md-5 g-2">
                    <div class="col">
                        <select v-model="selectedAge"
                            class="form-select form-select-sm text-secondary bg-light border-0 shadow-none">
                            <option value="不限">車齡 ▽</option>
                            <option v-for="opt in ageOptions.slice(1)" :key="opt" :value="opt">{{ opt }}</option>
                        </select>
                    </div>
                    <div class="col">
                        <select v-model="selectedLocation"
                            class="form-select form-select-sm text-secondary bg-light border-0 shadow-none">
                            <option value="不限">地區 ▽</option>
                            <option v-for="opt in locationOptions.slice(1)" :key="opt" :value="opt">{{ opt }}</option>
                        </select>
                    </div>
                    <div class="col">
                        <select v-model="selectedVehicleType"
                            class="form-select form-select-sm text-secondary bg-light border-0 shadow-none">
                            <option value="不限">車種 ▽</option>
                            <option v-for="opt in typeOptions.slice(1)" :key="opt" :value="opt">{{ opt }}</option>
                        </select>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div v-if="displayCars.length === 0" class="text-center py-5 bg-white border rounded-3 shadow-sm mt-3">
        <i class="bi bi-search display-3 text-muted opacity-50 d-block mb-3"></i>
        <h5 class="text-secondary fw-bold">找不到符合條件的精選車款</h5>
        <p class="text-muted small mb-0">請試著放寬篩選條件或重新搜尋關鍵字！</p>
    </div>

    <div v-else class="row g-4">
        <div v-for="car in displayCars" :key="car.usedCarId" class="col-md-4 mb-4">
            <div class="card h-100 shadow-sm" :class="{ 'opacity-75': car.usedCarStatus?.dbCode === 'SOLD' }">
                <div class="ratio ratio-4x3 bg-light">
                    <img :src="car.vehicleImgUrl" class="card-img-top object-fit-contain p-2" :alt="car.modelName">
                </div>

                <div class="card-body d-flex flex-column m-0">
                    <h5 class="card-title fw-bold text-truncate">
                        {{ car.brand }} {{ car.modelName }}
                    </h5>
                    <div class="row g-0 align-items-center mb-2">
                        <div class="col-7">
                            <div class="flex-wrap d-flex gap-1 text-muted small mb-1">
                                <span>{{ car.locationName }} /</span>
                                <span>{{ car.manufactureDate ? new Date(car.manufactureDate).getFullYear() : '未知' }}
                                    年</span>
                            </div>
                            <p class="card-text text-muted fs-6 small m-0">{{ car.mileage?.toLocaleString() }} 公里</p>
                        </div>

                        <div class="col-5 text-end">
                            <div v-if="car.usedCarStatus?.dbCode === 'SOLD'">
                                <span
                                    class="badge rounded-pill bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1 small">
                                    已售出
                                </span>
                            </div>
                            <p v-else class="card-text text-danger fw-bold m-0 d-inline-flex align-items-baseline">
                                <span class="fs-2 lh-1 fw-bold">
                                    {{ car.askingPrice ? parseFloat((car.askingPrice / 10000).toFixed(1)) : '電洽' }}
                                </span>
                                <span class="fs-6 ms-1 fw-normal text-muted">萬</span>
                            </p>
                        </div>
                    </div>

                    <div class="mt-auto">
                        <div class="d-grid gap-1">
                            <div class="d-flex gap-1 align-items-center">
                                <router-link :to="{ name: 'usedcardetail', params: { id: car.usedCarId } }"
                                    class="btn btn-sm btn-primary w-100 text-nowrap"
                                    style="padding-left: 4px; padding-right: 4px;">
                                    車輛詳情
                                </router-link>

                                <button @click="goToAppointment(car.usedCarId)"
                                    class="btn btn-sm btn-outline-primary w-100 text-nowrap"
                                    :disabled="car.usedCarStatus?.dbCode === 'SOLD'">
                                    <font-awesome-icon :icon="['fas', 'calendar-check']" class="me-1" />
                                    {{ car.usedCarStatus?.dbCode === 'SOLD' ? '停止預約' : '預約賞車' }}
                                </button>

                                <button @click="addToFavorite(car)"
                                    class="btn btn-sm btn-outline-secondary border-0 flex-shrink-0"
                                    :disabled="car.usedCarStatus?.dbCode === 'SOLD'">
                                    <font-awesome-icon :icon="['fas', 'heart']"
                                        :class="car.usedCarStatus?.dbCode === 'SOLD'
                                            ? (favoriteStore.isFavorite(car.usedCarId) ? 'text-danger opacity-25' : 'text-muted')
                                            : (favoriteStore.isFavorite(car.usedCarId) ? 'text-danger' : 'text-secondary')" />
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>