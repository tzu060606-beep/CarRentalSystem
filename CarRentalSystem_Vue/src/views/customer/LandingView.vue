<script setup>

import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { selectVehicleStore } from '@/store/rentalorder/selectVehicle'
import axios from 'axios'
import { storeToRefs } from 'pinia'

import heroImage from '@/assets/images/landing/hero-road-suv-landing-crop.png'
import yarisImage from '@/assets/images/landing/car-yaris.png'
import { useCarmodelStore } from '@/store/vehicle/carModelStore'
import CarModelDetailModal from '@/components/customer/vehicle/CarModelDetailModal.vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { rentalPlansAPI } from '@/api/rentalorder/rentalplan'


const router = useRouter()
const vehicleStore = selectVehicleStore()

const pickupLocationId = ref('')
const pickupTime = ref('')
const returnTime = ref('')

const locations = computed(() => vehicleStore.locations)
const isLoadingOptions = computed(() => vehicleStore.isInitialLoading)

const toDateTimeLocal = (date) => {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const createDefaultRentalTime = (daysToAdd) => {
  const date = new Date()
  date.setDate(date.getDate() + daysToAdd)
  date.setHours(10, 0, 0, 0)
  return toDateTimeLocal(date)
}

const initializeSearchForm = async () => {
  await vehicleStore.initializeOptions()

  const firstLocation = locations.value[0]
  pickupLocationId.value = vehicleStore.searchParams.pickupLocationId || firstLocation?.locationId || ''
  pickupTime.value = createDefaultRentalTime(1)
  returnTime.value = createDefaultRentalTime(2)
}

// 首頁服務亮點卡片，目前先用前端靜態資料，之後可改由 CMS 或設定檔提供。
const serviceHighlights = [
  {
    icon: 'fa-solid fa-car-side',
    title: '多樣車款',
    desc: '經濟型到豪華車款\n滿足各種出行需求',
    class: 'primary outline-car',
  },
  {
    icon: 'fa-regular fa-calendar-days',
    title: '彈性方案',
    desc: '短租長租都可以\n彈性選擇更划算',
    class: 'primary',
  },
  {
    icon: 'fa-regular fa-circle-check',
    title: '安心保障',
    desc: '完整保障與支援\n讓您出行無憂慮',
    class: 'accent',
  },
  {
    icon: 'fa-regular fa-map',
    title: '全台據點',
    desc: '全台多處服務據點\n取還車更方便',
    class: 'soft',
  },
]

// 精選車款 — 只從「已上架車款」隨機抓 6 款
const formModalRef = ref(null);
const carModelStore = useCarmodelStore();
const { carModels, isLoading } = storeToRefs(carModelStore);

// 租車方案價格對照
const rentalPlans = ref([]);
const priceMap = computed(() => {
  // 用 vehicleType 做 key，取日租 basePrice
  const map = {};
  for (const plan of rentalPlans.value) {
    const isLongTerm = plan.longTerm ?? plan.isLongTerm;
    if (isLongTerm) continue; // 只取日租
    const type = plan.appliedVehicleType || plan.vehicleType;
    if (type && !map[type]) {
      map[type] = Number(plan.basePrice ?? 0);
    }
  }
  return map;
});

// 已上架車款中隨機取 6 款
const featuredCars = computed(() => {
  const savedIds = JSON.parse(localStorage.getItem('publishedCarModels') || '[]');
  const published = carModels.value.filter(car => !car.isDeleted && savedIds.includes(car.modelId));
  // 洗牌取 6 筆
  const shuffled = [...published].sort(() => Math.random() - 0.5);
  return shuffled.slice(0, 6).map(car => ({
    ...car,
    price: priceMap.value[car.vehicleType] || car.price || 0,
  }));
});

// 抓取DOM元素的ref（對應 template中的ref = "scrollContainer"）
const scrollContainer = ref(null);
const scrollAmount = 320;
let autoScrollTimer = null;

// 左右滑動邏輯
const scrollLeft = () => {
  if (scrollContainer.value) {
    scrollContainer.value.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
  }
};
const scrollRight = () => {
  if (scrollContainer.value) {
    scrollContainer.value.scrollBy({ left: scrollAmount, behavior: 'smooth' });
  }
};

// 自動輪播（每 4 秒向右滑一次，到底則跳回開頭）
const startAutoScroll = () => {
  autoScrollTimer = setInterval(() => {
    const el = scrollContainer.value;
    if (!el) return;
    // 到底了就跳回開頭
    if (el.scrollLeft + el.clientWidth >= el.scrollWidth - 10) {
      el.scrollTo({ left: 0, behavior: 'smooth' });
    } else {
      el.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }
  }, 4000);
};
const stopAutoScroll = () => {
  if (autoScrollTimer) clearInterval(autoScrollTimer);
};

onUnmounted(() => stopAutoScroll());

const refreshList = () => {
  carModelStore.fetchCarModels();
};

// 下單去 → 前往 OrderSelectView 並帶上車型篩選
const goToOrder = (car) => {
  router.push({
    path: '/orders/custbooking',
    query: {
      fromHome: '1',
      vehicleType: car.vehicleType,
    },
  });
};



// const featuredCars = [
//   {
//     id: 1,
//     type: '經濟型',
//     name: 'Toyota Yaris',
//     seats: 5,
//     transmission: '自排',
//     price: 1200,
//     image: yarisImage,
//     imageClass: 'car-yaris',
//   },
//   {
//     id: 2,
//     type: '中型房車',
//     name: 'Honda Civic',
//     seats: 5,
//     transmission: '自排',
//     price: 1800,
//     image: yarisImage,
//     imageClass: 'car-civic',
//   },
//   {
//     id: 3,
//     type: '休旅車',
//     name: 'Toyota RAV4',
//     seats: 5,
//     transmission: '自排',
//     price: 2500,
//     image: yarisImage,
//     imageClass: 'car-rav4',
//   },
//   {
//     id: 4,
//     type: '豪華型',
//     name: 'BMW 5 Series',
//     seats: 5,
//     transmission: '自排',
//     price: 3800,
//     image: yarisImage,
//     imageClass: 'car-bmw',
//   },
// ]

const handleSearch = () => {
  if (!pickupLocationId.value || !pickupTime.value || !returnTime.value) return

  const isOneWayRental = false
  const pickupDate = pickupTime.value.slice(0, 10)

  vehicleStore.searchParams = {
    ...vehicleStore.searchParams,
    rentalMode: 'daily',
    pickupLocationId: pickupLocationId.value,
    returnLocationId: pickupLocationId.value,
    isOneWayRental,
    pickupTime: pickupTime.value,
    returnTime: returnTime.value,
    pickupDate,
  }

  router.push({
    path: '/orders/custbooking',
    query: {
      fromHome: '1',
      rentalMode: 'daily',
      pickupLocationId: pickupLocationId.value,
      returnLocationId: pickupLocationId.value,
      pickupTime: pickupTime.value,
      returnTime: returnTime.value,
      pickupDate,
      isOneWayRental: String(isOneWayRental),
    },
  })
}

onMounted(async () => {
  initializeSearchForm()
  //呼叫carmodelStore 的 action
  await carModelStore.fetchCarModels();
  // 載入租車方案以取得價格
  try {
    const res = await rentalPlansAPI.getAll();
    rentalPlans.value = Array.isArray(res.data) ? res.data : res.data?.data || [];
  } catch (e) {
    console.error('載入租車方案失敗', e);
  }
  // 啟動自動輪播
  startAutoScroll();
})
</script>

<template>
  <div class="landing-page">
    <section class="hero-section" :style="{ backgroundImage: `url(${heroImage})` }">
      <div class="hero-overlay"></div>
      <div class="container hero-container">
        <!-- Hero 文案區：若要對齊設計稿，主要調整這裡的文案、字級與間距。 -->
        <div class="hero-copy">
          <h1>
            輕鬆租車，<br>
            自在出行 <span>每一程</span>
          </h1>
          <p>多樣車款・彈性方案・安心保障</p>
          <div class="hero-badges" aria-label="服務特色">
            <span>
              <i class="fa-regular fa-circle-check" aria-hidden="true"></i>
              安心保障
            </span>
            <span>
              <i class="fa-regular fa-calendar-days" aria-hidden="true"></i>
              彈性方案
            </span>
            <span>
              <i class="fa-regular fa-map" aria-hidden="true"></i>
              全台據點
            </span>
          </div>
        </div>
      </div>

      <div class="booking-section">
        <div class="container booking-container">
          <!-- 搜尋列區塊：後續會串接 rental order 租車查詢 API。 -->
          <div class="booking-card">
            <div class="booking-field">
              <label for="pickup-location">取車地點</label>
              <div class="input-shell">
                <i class="fa-regular fa-map" aria-hidden="true"></i>
                <select id="pickup-location" v-model.number="pickupLocationId" class="form-select"
                  :disabled="isLoadingOptions" required>
                  <option value="">請選擇取車地點</option>
                  <option v-for="location in locations" :key="location.locationId" :value="location.locationId">
                    {{ location.locationName }}
                  </option>
                </select>
              </div>
            </div>

            <div class="booking-divider"></div>

            <div class="booking-field">
              <label for="pickup-date">取車日期</label>
              <div class="input-shell">
                <i class="fa-regular fa-calendar-days" aria-hidden="true"></i>
                <input id="pickup-date" v-model="pickupTime" type="datetime-local" class="form-control"
                  :disabled="isLoadingOptions" required>
              </div>
            </div>

            <div class="booking-divider"></div>

            <div class="booking-field">
              <label for="return-date">還車日期</label>
              <div class="input-shell">
                <i class="fa-regular fa-calendar-days" aria-hidden="true"></i>
                <input id="return-date" v-model="returnTime" type="datetime-local" class="form-control"
                  :disabled="isLoadingOptions" required>
              </div>
            </div>

            <button type="button" class="btn btn-primary search-button"
              :disabled="isLoadingOptions || !pickupLocationId || !pickupTime || !returnTime" @click="handleSearch">
              <font-awesome-icon icon="fa-solid fa-magnifying-glass" />
              搜尋車輛
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="highlights-section">
      <div class="container">
        <!-- Hero 下方服務亮點區塊。 -->
        <div class="highlights-grid">
          <article v-for="item in serviceHighlights" :key="item.title" class="highlight-item">
            <div class="highlight-icon" :class="item.class">
              <i :class="item.icon" aria-hidden="true"></i>
            </div>
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section id="featured-cars" class="featured-section">
      <div class="container featured-container">
        <div class="section-side">
          <h2>精選車款推薦</h2>
          <span class="title-rule"></span>
          <span class="view-all cursor-pointer" role="button" @click="router.push('/carmodels')">
            查看全部車款
            <font-awesome-icon icon="fa-solid fa-chevron-right" />
          </span>
        </div>

        <!-- 右側：新增輪播外層容器 -->
        <div class="vehicle-carousel-wrapper position-relative flex-grow-1 ms-4" @mouseenter="stopAutoScroll"
          @mouseleave="startAutoScroll">
          <!-- 左滑按鈕（沙色） -->
          <button class="scroll-nav-btn prev-btn" @click="scrollLeft" aria-label="向左滑動">
            <FontAwesomeIcon icon="fa-solid fa-chevron-left" />
          </button>

          <!-- 橫向滾動軌道 -->
          <div class="vehicle-carousel-track" ref="scrollContainer">
            <article v-for="car in featuredCars" :key="car.modelId" class="vehicle-card"
              @click="formModalRef.openModal(car)">
              <div class="vehicle-image-wrap">
                <img :src="car.vehicleImgUrl" :alt="car.modelName" :class="car.imageClass">
              </div>
              <div class="vehicle-body">
                <span class="vehicle-type">{{ car.vehicleType }}</span>
                <h3>{{ car.modelName }}</h3>
                <div class="vehicle-specs">
                  <span>
                    <i class="fa-regular fa-user" aria-hidden="true"></i>
                    {{ car.seats }} 人
                  </span>
                  <span>
                    <i class="fa-solid fa-suitcase-rolling" aria-hidden="true"></i>
                    {{ car.luggageCapacity }} 件
                  </span>
                </div>
                <div class="vehicle-footer d-flex justify-content-between align-items-center w-100 mt-3">
                  <p class="mb-0">
                    <span>TWD</span>
                    <span class="mx-1 fs-5 fw-bold">{{ car.price?.toLocaleString() || '----' }}</span>
                    <small>/ 日起</small>
                  </p>
                  <button type="button" class="btn btn-primary rounded-pill btn-sm px-2 text-nowrap fw-bold"
                    aria-label="下單去" @click.stop="goToOrder(car)">
                    下單去
                  </button>
                </div>
              </div>
            </article>
          </div>
          <!-- 右滑按鈕（沙色） -->
          <button class="scroll-nav-btn next-btn" @click="scrollRight" aria-label="向右滑動">
            <FontAwesomeIcon icon="fa-solid fa-chevron-right" />
          </button>


        </div>
      </div>
    </section>
  </div>
  <CarModelDetailModal ref="formModalRef" @saved="refreshList" />
</template>

<style scoped>
.landing-page {
  min-height: 100vh;
  margin-top: -92px;
  /* 抵消 padding，讓 hero 延伸到 navbar 下面 */
  background:
    radial-gradient(circle at 8% 32%, rgba(252, 231, 213, 0.9), transparent 32%),
    linear-gradient(108deg, #fff 0%, #f7fbff 45%, #eaf5ff 100%);
}

/* Hero 版面控制：
   - hero height 決定首屏能看到多少下方內容
   - background-position 控制車與道路在 Hero 內的垂直位置
   - booking-section 的 translateY 控制搜尋列覆蓋 Hero 的程度 */
.hero-section {
  position: relative;
  height: clamp(680px, 78svh, 820px);
  min-height: 680px;
  display: flex;
  align-items: center;
  overflow: visible;
  background-position: center -182px;
  background-repeat: no-repeat;
  background-size: max(100%, 1720px) auto;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(255, 246, 237, 0.94) 0%, rgba(255, 247, 239, 0.72) 24%, rgba(255, 255, 255, 0) 40%, rgba(255, 255, 255, 0) 100%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04) 0%, rgba(255, 255, 255, 0) 58%, rgba(247, 251, 255, 0.08) 82%, rgba(247, 251, 255, 0) 100%);
}

.hero-container {
  position: relative;
  z-index: 1;
  max-width: 1320px;
  padding-top: clamp(5rem, 8svh, 6.5rem);
  padding-bottom: clamp(5.5rem, 11svh, 7.5rem);
}

.hero-copy {
  width: min(520px, 100%);
  margin-left: 1rem;
}

.hero-copy h1 {
  margin: 0 0 1.35rem;
  color: var(--color-black);
  font-size: clamp(2.6rem, 5vw, 4.35rem);
  font-weight: 600 !important;
  line-height: 1.22;
  letter-spacing: 0;
}

.hero-copy h1 span {
  color: var(--color-primary);
  font-weight: 600 !important;
}

.hero-copy p {
  margin: 0 0 2rem;
  color: var(--color-text-primary);
  font-size: clamp(1.1rem, 1.8vw, 1.45rem);
  font-weight: 400 !important;
  letter-spacing: 0.08em;
}

.hero-badges {
  display: flex;
  align-items: center;
  gap: 2rem;
  flex-wrap: wrap;
}

.hero-badges span {
  display: inline-flex;
  align-items: center;
  gap: 0.8rem;
  color: var(--color-text-primary);
  font-size: var(--text-base);
  font-weight: 400 !important;
}

.hero-badges svg,
.hero-badges i {
  color: var(--color-primary);
  font-size: 1.8rem;
}

.booking-section {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2;
  transform: translateY(28%);
}

.booking-container {
  max-width: 1240px;
}

.booking-card {
  display: grid;
  grid-template-columns: 1.1fr 1px 1.1fr 1px 1.1fr auto;
  align-items: end;
  gap: 1.35rem;
  padding: 1.4rem 2rem;
  background: var(--color-bg-surface);
  border-radius: var(--radius-xl);
  box-shadow: 0 22px 46px rgba(11, 26, 48, 0.14);
}

.booking-field label {
  display: block;
  margin: 0 0 0.75rem 0.35rem;
  color: var(--color-text-primary);
  font-size: var(--text-base);
  font-weight: 600 !important;
}

.input-shell {
  position: relative;
}

.input-shell svg,
.input-shell i {
  position: absolute;
  left: 1.05rem;
  top: 50%;
  z-index: 2;
  color: var(--color-primary);
  font-size: 1.35rem;
  transform: translateY(-50%);
  pointer-events: none;
}

.input-shell .form-control,
.input-shell .form-select {
  height: 54px;
  padding-left: 3.1rem;
  color: var(--color-text-secondary);
  border-color: var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  font-weight: 500 !important;
}

.booking-divider {
  width: 1px;
  height: 42px;
  margin-bottom: 0.3rem;
  background: var(--color-border);
}

.search-button {
  --bs-btn-font-weight: 400;
  height: 54px;
  min-width: 190px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.8rem;
  border-radius: var(--radius-md);
  font-size: 1.28rem;
  font-weight: 400 !important;
  box-shadow: 0 10px 20px rgba(1, 80, 173, 0.2);
}

.landing-page .booking-card .search-button.btn.btn-primary {
  --bs-btn-font-weight: 400;
  font-weight: 400 !important;
}

.search-button svg,
.search-button i {
  font-size: 1.45rem;
}

/* 這裡的上方留白需和 booking-section 的 translateY 一起調整。
   若搜尋列往下移，這裡通常也要同步增加，避免 highlights 與搜尋列互相擠壓。 */
.highlights-section {
  padding: clamp(7.5rem, 12svh, 9rem) 0 2.4rem;
}

.highlights-section .container,
.featured-container {
  max-width: 1240px;
}

.highlights-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
}

.highlight-item {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 1.6rem;
  min-height: 98px;
  padding: 0 2rem;
  border-right: 1px solid var(--color-border);
}

.highlight-item:last-child {
  border-right: 0;
}

.highlight-icon {
  width: 74px;
  height: 74px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--color-white);
  font-size: 2rem;
  background: var(--color-primary);
}

.highlight-icon.accent {
  background: var(--color-accent);
}

.highlight-icon.soft {
  color: var(--color-primary);
  background: #d5e4fb;
}

.highlight-icon.outline-car i {
  color: transparent;
  -webkit-text-stroke: 2px var(--color-white);
  text-stroke: 2px var(--color-white);
}

.highlight-item h3 {
  margin: 0 0 0.4rem;
  color: var(--color-text-primary);
  font-size: 1.25rem;
  font-weight: 700 !important;
}

.highlight-item p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--text-base);
  font-weight: 500 !important;
  line-height: 1.55;
  white-space: pre-line;
}

.featured-section {
  padding: 0.8rem 0 5rem;
}

.featured-container {
  display: grid;
  grid-template-columns: 170px 1fr;
  gap: 2.2rem;
  align-items: start;
}

.section-side {
  padding-top: 1.6rem;
}

.section-side h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1.6rem;
  font-weight: 700 !important;
}

.title-rule {
  display: block;
  width: 42px;
  height: 3px;
  margin: 1rem 0 2rem;
  background: var(--color-primary);
  border-radius: var(--radius-full);
}

.view-all {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  color: var(--color-primary);
  font-size: var(--text-base);
  font-weight: 600 !important;
  text-decoration: none;
}

.vehicle-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

/* 確保卡片維持固定大小，不會被壓縮 */
.vehicle-card {
  flex: 0 0 auto;
  /* 不允許壓縮放大 */
  width: 250px;
  overflow: hidden;
  background: var(--color-bg-surface);
  border: 1px solid rgba(1, 80, 173, 0.05);
  border-radius: var(--radius-md);
  box-shadow: 0 18px 38px rgba(1, 80, 173, 0.08);
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
  cursor: pointer;
}

.vehicle-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 22px 44px rgba(1, 80, 173, 0.14);
}

.vehicle-image-wrap {
  height: 148px;
  display: flex;
  align-items: end;
  justify-content: center;
  padding: 0.45rem 0.55rem 0;
}

.vehicle-image-wrap img {
  width: 100%;
  max-width: 258px;
  height: 132px;
  object-fit: contain;
  object-position: center bottom;
  filter: drop-shadow(0 12px 12px rgba(11, 26, 48, 0.12));
}

.vehicle-image-wrap .car-civic {
  transform: scale(1.08);
  filter: grayscale(0.75) brightness(0.72) drop-shadow(0 12px 12px rgba(11, 26, 48, 0.12));
}

.vehicle-image-wrap .car-rav4 {
  transform: scale(1.16);
  filter: saturate(0.85) contrast(1.05) drop-shadow(0 12px 12px rgba(11, 26, 48, 0.12));
}

.vehicle-image-wrap .car-bmw {
  transform: scale(1.08);
  filter: grayscale(1) brightness(0.38) contrast(1.35) drop-shadow(0 12px 12px rgba(11, 26, 48, 0.14));
}

.vehicle-body {
  padding: 0.7rem 1.25rem 1.1rem;
}

.vehicle-type {
  display: inline-block;
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  margin-bottom: 0.35rem;
}

.vehicle-body h3 {
  margin: 0 0 0.75rem;
  color: var(--color-text-primary);
  font-size: 1.1rem;
  font-weight: 700 !important;
}

.vehicle-specs {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  font-weight: 500 !important;
}

.vehicle-specs span {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.vehicle-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.vehicle-footer p {
  margin: 0;
  color: var(--color-primary);
  font-size: 1.45rem;
  font-weight: 700 !important;
  line-height: 1;
}

.vehicle-footer p span,
.vehicle-footer p small {
  font-size: var(--text-sm);
  font-weight: 600 !important;
}

.vehicle-footer p small {
  color: var(--color-text-secondary);
}

/* .vehicle-footer button {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border: 0;
  border-radius: 50%;
  color: var(--color-text-primary);
  background: #fbf1ec;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.vehicle-footer button:hover {
  color: var(--color-white);
  background: var(--color-primary);
} */
/* --- 輪播區塊外層 --- */
.vehicle-carousel-wrapper {
  position: relative;
  /* 讓左右按鈕絕對定位在這個外層 */
  flex-grow: 1;
  min-width: 0;
  /* 防止flex子元素被內容撐破 */
  padding: 0 10px;
  /* 確保留有空間給左右凸出的按鈕 */
}

/* 橫向滾動軌道 */
.vehicle-carousel-track {
  display: flex;
  overflow-x: auto;
  /* 允許橫向滾動 */
  gap: 20px;
  /* 卡片間距 */
  scroll-behavior: smooth;
  /* 平滑滾動效果 */
  padding: 15px 0;
  /* 上下留白避免卡片陰影被截斷 */

  /* 隱藏預設卷軸，讓畫面更乾淨 */
  scrollbar-width: none;
  /* Firefox */
  -ms-overflow-style: none;
  /* IE & edge */

  /* 邊緣淡出 */
  /* to right(由左至右) 0% 透明開始，到5%變實體純黑顯示，維持到95%實體純黑，最後100%變透明 */
  -webkit-mask-image: linear-gradient(to right, transparent 0%, black 5%, black 95%, transparent 100%);
  mask-image: linear-gradient(to right, transparent 0%, black 5%, black 95%, transparent 100%);
}

/* 隱藏Chrome, Safari的卷軸 */
.vehicle-carousel-track::-webkit-scrollbar {
  display: none;
}

/* 輪播左右導航按鈕 */
.scroll-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #fce7d5;
  border: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  color: #fff;
}

.scroll-nav-btn:hover {
  background-color: #dba772;
  color: #fff;
}

/* 按鈕定位到最邊緣 */
.prev-btn {
  left: -10px;
}

.next-btn {
  right: -10px;
}

/* --- 圓角長方形「下單去」按鈕 --- */
/* .vehicle-footer .btn-order {
  background-color: transparent;
  border: 1px solid #0150ad;
  border-radius: 10px;
  padding: 4px 16px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap; /避免文字拆行 /
  color: #0150ad;
}
.vehicle-footer .btn-order:hover {
  background-color: #0056b3;
  color: #ffffff;
} */

@media (max-width: 1199.98px) {
  .booking-card {
    grid-template-columns: 1fr 1fr;
  }

  .booking-divider {
    display: none;
  }

  .search-button {
    grid-column: 1 / -1;
  }

  .highlights-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .highlight-item:nth-child(2) {
    border-right: 0;
  }

  .vehicle-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 991.98px) {
  .hero-section {
    height: clamp(640px, 80svh, 780px);
    min-height: 640px;
    background-position: 62% -146px;
    background-size: max(124%, 1460px) auto;
  }

  .hero-overlay {
    background:
      linear-gradient(90deg, rgba(255, 246, 237, 0.96) 0%, rgba(255, 247, 239, 0.78) 45%, rgba(255, 255, 255, 0) 100%),
      linear-gradient(180deg, rgba(255, 255, 255, 0) 65%, rgba(247, 251, 255, 0) 100%);
  }

  .featured-container {
    grid-template-columns: 1fr;
  }

  .section-side {
    padding-top: 0;
  }
}

@media (max-width: 767.98px) {
  .hero-section {
    height: auto;
    min-height: 620px;
    align-items: flex-start;
    background-position: 64% 88px;
    background-size: auto 70%;
  }

  .hero-container {
    padding-top: 7rem;
    padding-bottom: 2rem;
  }

  .hero-copy {
    margin-left: 0;
  }

  .hero-badges {
    gap: 1rem;
  }

  .booking-card {
    grid-template-columns: 1fr;
    padding: 1.25rem;
  }

  .vehicle-image-wrap {
    height: 136px;
  }

  .vehicle-image-wrap img {
    max-width: 236px;
    height: 122px;
  }

  .booking-section {
    position: relative;
    transform: none;
    margin-top: 1.5rem;
    padding-bottom: 1.5rem;
  }

  .highlights-section {
    padding-top: 2.5rem;
  }

  .highlights-grid {
    grid-template-columns: 1fr;
  }

  .highlight-item {
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
    padding: 1.1rem 0;
  }

  .highlight-item:last-child {
    border-bottom: 0;
  }

  .vehicle-grid {
    grid-template-columns: 1fr;
  }
}
</style>
