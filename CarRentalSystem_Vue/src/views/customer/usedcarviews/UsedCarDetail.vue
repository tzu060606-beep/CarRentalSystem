<script setup>
import { ref, onMounted, nextTick, shallowRef, computed } from 'vue'; // 🌟 引入 shallowRef
import { useRoute, useRouter } from 'vue-router';
import usedCarApi from '@/api/usedcar/usedCarApi';
import { Carousel } from 'bootstrap';
import { useFavoriteStore } from '@/store/usedCar/useFavoriteStore';

// 🌟 嚴格遵循 v2 規範：引入共用元件
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';

const route = useRoute();
const router = useRouter();
const favoriteStore = useFavoriteStore(); // 🌟 宣告收藏 Store

const car = ref({});
const loading = ref(true);
const showLoginConfirm = ref(false); // 🌟 控制登入對話框顯示


// 🌟 核心修正：必須確保 activeIndex 有被正確 ref 宣告並導出！
const activeIndex = ref(0); // 目前顯示的圖片索引（從 0 開始）
const carImages = ref([]); // 下方 5 張細節圖的網址陣列

// 🌟 用 shallowRef 來保存 Bootstrap 實例，確保跨非同步函式時狀態不遺失
const carouselInstance = shallowRef(null);

// =================【你可能有興趣的推薦邏輯】=================
const allCarList = ref([]); // 儲存所有車輛清單
const recommendedCars = ref([]); // 儲存隨機篩選後的 3 台車

// 隨機打亂陣列並取出指定數量的函式
const getRandomCars = (arr, num, currentId) => {
    // 排除當前點進來看的這台車，且狀態必須是 ACTIVE 或 SOLD
    const filtered = arr.filter(c => c.usedCarId !== currentId && c.usedCarStatus?.dbCode === 'ACTIVE');
    // 洗牌演算法 (Fisher-Yates Shuffle)
    for (let i = filtered.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [filtered[i], filtered[j]] = [filtered[j], filtered[i]];
    }
    return filtered.slice(0, num);
};
// =========================================================

// 寫一個輔助函數來動態取得 assets 的圖片路徑
const getAssetsImageUrl = (modelId, imgIndex) => {
    return new URL(`/src/assets/images/usedcar/car${modelId}_${imgIndex}.png`, import.meta.url).href;
};

// 🌟 切換圖片的函式
// 🌟 修正 2：如果監聽沒反應，點擊時強制雙管齊下（同時更新外框 + 動畫）
const changeImage = (index) => {
    console.log("👉 縮圖被點擊了！目標索引:", index);

    // 強制先讓外框變色，確保畫面有立即回饋
    // activeIndex.value = index;

    // 呼叫 Bootstrap 動畫滑動
    if (carouselInstance.value) {
        console.log("🎬 正在呼叫 Bootstrap Carousel 滑動到:", index);
        carouselInstance.value.to(index);
    } else {
        console.warn("⚠️ Bootstrap Carousel 實例尚未準備好，嘗試重新抓取");
        const carouselEl = document.getElementById('carCarousel');
        if (carouselEl) {
            carouselInstance.value = new Carousel(carouselEl, { ride: false, touch: true });
            carouselInstance.value.to(index);
        }
    }
};

// 🌟 本頁「預約賞車」按鈕點擊事件：直接帶入當前車輛 ID
const handleMainAppointment = () => {
    goToAppointment(car.value.usedCarId);
};



// 商城跳轉預約與詳情頁邏輯
const goToAppointment = (id) => {
    router.push({
        name: 'ViewTable',
        query: { carId: id }
    });
};

// 🌟 依規範改寫：點擊收藏如果未登入，觸發自訂 Confirm 彈窗
const addToFavorite = (carItem) => {
    const currentToken = localStorage.getItem('token');
    if (!currentToken) {
        showLoginConfirm.value = true; // 觸發共用 Dialog
        return;
    }
    favoriteStore.toggleFavorite(carItem);
    localStorage.setItem('fav_owner_token', currentToken);
};

// 確認前往登入
const handleLoginConfirm = () => {
    showLoginConfirm.value = false;
    router.push('/login');
};

const fetchDetail = async () => {
    try {
        loading.value = true;
        const id = route.params.id;
        // 1. 載入當前車輛詳情
        const response = await usedCarApi.getOneDetail(id);
        car.value = response.data;

        const vId = car.value.modelId;

        carImages.value = [
            getAssetsImageUrl(vId, 1),
            getAssetsImageUrl(vId, 2),
            getAssetsImageUrl(vId, 3),
            getAssetsImageUrl(vId, 4),
            getAssetsImageUrl(vId, 5)
        ];

        activeIndex.value = 0;

        // 2. 載入所有車輛用來做下方「隨機推薦」
        const allRes = await usedCarApi.getAllDetails();
        allCarList.value = allRes.data;
        // 隨機挑選 3 台
        recommendedCars.value = getRandomCars(allCarList.value, 3, car.value.usedCarId);

        // 3. Carousel 初始化
        // 等 DOM 渲染完後，初始化 Bootstrap Carousel 並綁定滑動監聽
        await nextTick();
        const carouselEl = document.getElementById('carCarousel');
        // console.log("🔍 檢查是否有抓到大圖 DOM 元件:", carouselEl);

        if (carouselEl) {
            // 初始化實例並存入 .value
            carouselInstance.value = new Carousel(carouselEl, {
                ride: false,
                touch: true
            });
            // console.log("✅ Bootstrap Carousel 實例初始化完畢！");

            // // 監聽大圖主動滑動事件（例如點左右箭頭或手指滑動時，自動同步下方外框）
            // carouselEl.addEventListener('slide.bs.carousel', (event) => {
            //     console.log("🔄 偵測到大圖主動滑動至:", event.to);
            //     activeIndex.value = event.to;
            // });
        }
    } catch (error) {
        console.error("載入詳情失敗", error);
    } finally {
        loading.value = false;
    }
};

const goToMap = () => {
    router.push({
        name: 'usedcarmap',
        query: {
            carName: `${car.value.brand} ${car.value.modelName}`,
            address: car.value.address,
            dealerName: car.value.locationName,
            lat: car.value.latitude,
            lng: car.value.longitude
        }
    });
};

// 🌟 監聽路由參數 id 的變化（當使用者點擊下方的推薦車款時，要能重新整理頁面資料）
import { watch } from 'vue';
watch(() => route.params.id, (newId) => {
    if (newId) fetchDetail();
});

onMounted(fetchDetail);
</script>

<template>
    <LoadingSpinner :isLoading="loading" text="精選車輛資料載入中..." class="mt-5" />

    <ConfirmDialog :isVisible="showLoginConfirm" title="需要會員登入" message="請先登入後再使用收藏功能，是否現在前往登入頁面？" confirmText="前往登入"
        confirmVariant="primary" @confirm="handleLoginConfirm" @cancel="showLoginConfirm = false" />

    <div v-if="!loading" class="container mt-4">

        <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-end mb-4 border-bottom pb-3">
            <div>
                <h1 class="fw-bold text-dark mb-0">
                    {{ car.brand }} {{ car.modelName }}
                    <span v-if="car.usedCarStatus?.dbCode !== 'ACTIVE'" class="badge bg-danger ms-2 fs-6 rounded-pill">
                        <font-awesome-icon :icon="['fas', 'ban']" class="me-1" /> 已售出
                    </span>
                </h1>
                <div class="flex-wrap d-flex gap-2">
                    <p class="card-text fs-6 small m-0 text-muted">{{ car.locationName }} /</p>
                    <p class="card-text fs-6 small m-0 text-muted">
                        {{ car.manufactureDate ? new Date(car.manufactureDate).getFullYear() : '未知' }} 年
                    </p>
                </div>
            </div>
            <div class="text-md-end mt-2 mt-md-0">
                <span class="text-muted small d-block">預售價格</span>
                <h1 class="text-danger fw-bold mb-0 display-5">
                    {{ parseFloat((car.askingPrice / 10000).toFixed(1)) }} <span
                        class="fs-4 text-muted fw-normal">萬</span>
                </h1>
            </div>
        </div>

        <div class="row g-4">
            <div class="col-md-7">
                <div v-if="carImages.length > 0" id="carCarousel"
                    class="carousel slide carousel-dark bg-light rounded shadow-sm mb-3" data-bs-ride="false">
                    <div class="carousel-inner" style="height: 400px;">
                        <div v-for="(img, index) in carImages" :key="index" class="carousel-item h-100"
                            :class="index === 0 ? 'active' : ''">
                            <div class="d-flex align-items-center justify-content-center h-100">
                                <img :src="img" class="d-block img-fluid rounded"
                                    style="max-height: 100%; max-width: 100%; object-fit: contain;" />
                            </div>
                        </div>
                    </div>

                    <button class="carousel-control-prev" type="button" data-bs-target="#carCarousel"
                        data-bs-slide="prev">
                        <span class="carousel-control-prev-icon bg-dark bg-opacity-25 rounded-circle p-3"
                            aria-hidden="true"></span>
                        <span class="visually-hidden">Previous</span>
                    </button>

                    <button class="carousel-control-next" type="button" data-bs-target="#carCarousel"
                        data-bs-slide="next">
                        <span class="carousel-control-next-icon bg-dark bg-opacity-25 rounded-circle p-3"
                            aria-hidden="true"></span>
                        <span class="visually-hidden">Next</span>
                    </button>
                </div>

                <div v-if="carImages.length > 0" class="row row-cols-5 g-2">
                    <div v-for="(img, index) in carImages" :key="index" class="col">
                        <button type="button"
                            class="w-100 bg-white rounded text-center border border-secondary border-opacity-25 shadow-sm d-flex align-items-center justify-content-center p-0"
                            style="height: 80px; cursor: pointer;" @click="changeImage(index)">
                            <img :src="img" class="img-fluid rounded"
                                style="max-height: 100%; max-width: 100%; object-fit: contain; pointer-events: none;" />
                        </button>
                    </div>
                </div>
            </div>

            <div class="col-md-5">
                <div class="card border-0 shadow-sm bg-white p-4 h-100 d-flex flex-column justify-content-between">

                    <div class="row row-cols-2 g-3 mb-4">
                        <div class="col border-start border-warning border-3 ps-2">
                            <span class="text-muted small d-block">目前里程 (公里)</span>
                            <span class="fw-bold">{{ car.mileage?.toLocaleString() || '0' }}</span>
                        </div>
                        <div class="col border-start border-warning border-3 ps-2">
                            <span class="text-muted small d-block">顏色</span>
                            <span class="fw-bold">{{ car.color || '未知' }}</span>
                        </div>
                        <div class="col border-start border-warning border-3 ps-2">
                            <span class="text-muted small d-block">車種</span>
                            <span class="fw-bold">{{ car.vehicleType || '未知' }}</span>
                        </div>
                        <div class="col border-start border-warning border-3 ps-2">
                            <span class="text-muted small d-block">排氣量/馬力</span>
                            <span class="fw-bold">{{ car.displacement }} cc</span>
                        </div>
                        <div class="col border-start border-warning border-3 ps-2">
                            <span class="text-muted small d-block">變速系統</span>
                            <span class="fw-bold">{{ car.transmission || '手自排' }}</span>
                        </div>
                        <div class="col border-start border-warning border-3 ps-2">
                            <span class="text-muted small d-block">動力</span>
                            <span class="fw-bold">{{ car.fuelType || '未知' }}</span>
                        </div>
                        <div class="col border-start border-warning border-3 ps-2">
                            <span class="text-muted small d-block">座位數</span>
                            <span class="fw-bold">{{ car.seats || '未知' }}</span>
                        </div>
                        <div class="col border-start border-warning border-3 ps-2">
                            <span class="text-muted small d-block">車況描述</span>
                            <span class="fw-bold text-truncate d-block" :title="car.conditionDesc">{{ car.conditionDesc
                                || '無' }}</span>
                        </div>
                    </div>

                    <div class="border-top pt-3 mt-auto">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <div>
                                <span class="text-muted small d-block fs-6">詳細地址 :</span>
                                <span class="small fw-semibold">{{ car.address }}</span>
                            </div>
                            <button @click="goToMap" class="btn btn-outline-primary btn-sm text-nowrap ms-2">
                                <font-awesome-icon icon="geo-alt" class="me-1" /> 查看位置
                            </button>
                        </div>
                        <div class="small fs-6 mb-4">
                            <span class="text-muted me-1">聯絡電話 :</span>
                            <span class="fw-bold text-dark">{{ car.phone }}</span>
                        </div>

                        <div class="d-grid gap-2">
                            <button v-if="car.usedCarStatus?.dbCode === 'ACTIVE'" @click="handleMainAppointment"
                                class="btn btn-success btn-lg w-100 rounded-pill fw-bold py-3 shadow-sm">
                                <font-awesome-icon :icon="['fas', 'calendar-check']" class="me-2" />
                                線上預約現場賞車
                            </button>

                            <button v-else disabled
                                class="btn btn-secondary btn-lg w-100 rounded-pill fw-bold py-3 opacity-75">
                                <font-awesome-icon :icon="['fas', 'ban']" class="me-2" />
                                此車已售出，不開放預約
                            </button>
                            <button @click="addToFavorite(car)" class="btn btn-md rounded-pill fw-medium py-2"
                                :class="car.usedCarStatus?.dbCode !== 'ACTIVE' ? 'btn-light text-muted border-0' : 'btn-outline-secondary'"
                                :disabled="car.usedCarStatus?.dbCode !== 'ACTIVE'">

                                <font-awesome-icon icon="heart" class="me-2" :class="car.usedCarStatus?.dbCode !== 'ACTIVE'
                                    ? 'text-black-50'
                                    : (favoriteStore.isFavorite(car.usedCarId) ? 'text-danger' : '')" />

                                {{ car.usedCarStatus?.dbCode !== 'ACTIVE' ? '此車已售出無法收藏' :
                                    (favoriteStore.isFavorite(car.usedCarId) ? '取消收藏此車輛' : '加入我的收藏清單') }}
                            </button>
                        </div>
                    </div>

                </div>
            </div>
        </div>

        <div class="border-top pt-4 mt-5">
            <h3 class="fw-bold mb-4">你可能有興趣</h3>
            <div v-if="recommendedCars.length === 0" class="text-center py-4 text-muted">
                暫無其他推薦車款
            </div>
            <div v-else class="row g-4">
                <div v-for="item in recommendedCars" :key="item.usedCarId" class="col-md-4 mb-4">
                    <div class="card h-100 shadow-sm" :class="{ 'opacity-75': item.usedCarStatus?.dbCode === 'SOLD' }">
                        <div class="ratio ratio-4x3 bg-light">
                            <img :src="item.vehicleImgUrl" class="card-img-top object-fit-contain p-2"
                                :alt="item.modelName">
                        </div>

                        <div class="card-body d-flex flex-column m-0">
                            <h5 class="card-title fw-bold text-truncate">
                                {{ item.brand }} {{ item.modelName }}
                            </h5>
                            <div class="row g-0 align-items-center mb-2">
                                <div class="col-7">
                                    <div class="flex-wrap d-flex gap-1 text-muted small mb-1">
                                        <span>{{ item.locationName }} /</span>
                                        <span>{{ item.manufactureDate ? new Date(item.manufactureDate).getFullYear() :
                                            '未知' }} 年</span>
                                    </div>
                                    <p class="card-text text-muted fs-6 small m-0">{{ item.mileage?.toLocaleString() }}
                                        公里</p>
                                </div>

                                <div class="col-5 text-end">
                                    <div v-if="item.usedCarStatus?.dbCode === 'SOLD'">
                                        <span
                                            class="badge rounded-pill bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1 small">
                                            已售出
                                        </span>
                                    </div>
                                    <p v-else
                                        class="card-text text-danger fw-bold m-0 d-inline-flex align-items-baseline">
                                        <span class="fs-2 lh-1 fw-bold">
                                            {{ item.askingPrice ? parseFloat((item.askingPrice / 10000).toFixed(1)) :
                                                '電洽' }}
                                        </span>
                                        <span class="fs-6 ms-1 fw-normal text-muted">萬</span>
                                    </p>
                                </div>
                            </div>

                            <div class="mt-auto">
                                <div class="d-grid gap-1">
                                    <div class="d-flex gap-1 align-items-center">
                                        <router-link :to="{ name: 'usedcardetail', params: { id: item.usedCarId } }"
                                            class="btn btn-sm btn-primary w-100 text-nowrap"
                                            style="padding-left: 4px; padding-right: 4px;">
                                            車輛詳情
                                        </router-link>

                                        <button @click="goToAppointment(item.usedCarId)"
                                            class="btn btn-sm btn-outline-primary w-100 text-nowrap"
                                            :disabled="item.usedCarStatus?.dbCode === 'SOLD'">
                                            <font-awesome-icon :icon="['fas', 'calendar-check']" class="me-1" />
                                            {{ item.usedCarStatus?.dbCode === 'SOLD' ? '停止預約' : '預約賞車' }}
                                        </button>

                                        <button @click="addToFavorite(item)"
                                            class="btn btn-sm btn-outline-secondary border-0 flex-shrink-0"
                                            :disabled="item.usedCarStatus?.dbCode !== 'ACTIVE'"> <font-awesome-icon
                                                :icon="['fas', 'heart']"
                                                :class="item.usedCarStatus?.dbCode !== 'ACTIVE'
                                                    ? 'text-muted'
                                                    : (favoriteStore.isFavorite(item.usedCarId) ? 'text-danger' : 'text-secondary')" />
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>