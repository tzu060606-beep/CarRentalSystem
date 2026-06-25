<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { carModelAPI } from '@/api/vehicle/carModelApi';
import { rentalPlansAPI } from '@/api/rentalorder/rentalplan';
import SearchBar from '@/components/common/SearchBar.vue';
import Pagination from '@/components/common/Pagination.vue';

const router = useRouter();
const carModels = ref([]);
const isLoading = ref(false);
const currentFilter = ref('All');
const keyword = ref('');
const currentPage = ref(1);
const pageSize = 6;

// 租車方案價格
const rentalPlans = ref([]);
const priceMap = computed(() => {
  const map = {};
  for (const plan of rentalPlans.value) {
    const isLongTerm = plan.longTerm ?? plan.isLongTerm;
    if (isLongTerm) continue; // 只取日租方案
    const type = plan.appliedVehicleType || plan.vehicleType;
    if (type && !map[type]) {
      map[type] = Number(plan.basePrice ?? 0);
    }
  }
  return map;
});
const getPrice = (car) => priceMap.value[car.vehicleType] || car.price || 0;

// 載入資料
onMounted(async () => {
  try {
    isLoading.value = true;
    const [carRes, planRes] = await Promise.all([
      carModelAPI.getAll(),
      rentalPlansAPI.getAll(),
    ]);
    // 從 localStorage 抓出有上架的 ID
    const savedIds = JSON.parse(localStorage.getItem('publishedCarModels') || '[]');
    
    // 只保留有上架且未軟刪除的資料
    carModels.value = carRes.data.filter(car => !car.isDeleted && savedIds.includes(car.modelId));
    rentalPlans.value = Array.isArray(planRes.data) ? planRes.data : planRes.data?.data || [];
  } catch (error) {
    console.error(error);
  } finally {
    isLoading.value = false;
  }
});

// 動態產生車型類別
const categories = computed(() => {
  const types = carModels.value.map(car => car.vehicleType);
  return ['All', ...new Set(types)];
});

// 過濾 + 搜尋
const filteredCarModels = computed(() => {
  let result = carModels.value;
  if (currentFilter.value !== 'All') {
    result = result.filter(car => car.vehicleType === currentFilter.value);
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase();
    result = result.filter(car =>
      car.brand?.toLowerCase().includes(kw) ||
      car.modelName?.toLowerCase().includes(kw)
    );
  }
  return result;
});

// 分頁
const totalPages = computed(() => Math.ceil(filteredCarModels.value.length / pageSize) || 1);
const paginatedCarModels = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredCarModels.value.slice(start, start + pageSize);
});

// 切換分類時回到第 1 頁
const setFilter = (cat) => {
  currentFilter.value = cat;
  currentPage.value = 1;
};

// 分類按鈕圖示對應
const categoryIcons = {
  'All':    '',
  '中型轎車':   'fa-solid fa-car-side',
  '小型轎車':   'fa-solid fa-car',
  '休旅車': 'fa-solid fa-van-shuttle',
  '廂型車': 'fa-solid fa-truck-field',
  '電動車': 'fa-solid fa-charging-station',
};
const getCategoryIcon = (cat) => categoryIcons[cat] || '';

// 排氣量顯示
const formatDisplacement = (val) => {
  if (!val) return '-';
  return val >= 1000 ? (val / 1000).toFixed(1) + 'L' : val + 'cc';
};

// 立即預訂 → 前往 OrderSelectView 並帶上車型篩選
const goToOrder = (car) => {
  router.push({
    path: '/orders/custbooking',
    query: {
      fromHome: '1',
      vehicleType: car.vehicleType,
    },
  });
};
</script>

<template>
  <div class="vehicle-page">

    <!-- 頁面 Hero -->
    <section class="vehicle-hero-banner">
      <div class="container vehicle-hero-container">
        <h1>挑選您的完美座駕</h1>
        <p class="vehicle-hero-desc">
          探索我們為可靠性與舒適性而設計的多元車隊。從高效的城市小型車到頂級行政轎車，為您的物流需求找到完美的車輛。
        </p>
      </div>
    </section>

    <!-- 篩選 + 搜尋列 -->
    <section class="vehicle-toolbar-section">
      <div class="container vehicle-toolbar">
        <div class="vehicle-filter-bar">
          <button
            v-for="cat in categories" :key="cat"
            class="btn vehicle-filter-btn"
            :class="{ 'vehicle-filter-btn--active': currentFilter === cat }"
            @click="setFilter(cat)"
          >
            <i :class="getCategoryIcon(cat)" aria-hidden="true"></i>
            {{ cat === 'All' ? '所有車款' : cat }}
          </button>
        </div>
        <div class="vehicle-search-wrap">
          <SearchBar v-model="keyword" :showButton="false" placeholder="依品牌或型號搜尋..." />
        </div>
      </div>
    </section>

    <!-- 車款卡片列表 -->
    <section class="vehicle-list-section">
      <div class="container">

        <!-- 載入中 -->
        <div v-if="isLoading" class="d-flex justify-content-center py-5">
          <div class="spinner-border" style="color: var(--color-primary);" role="status"></div>
        </div>

        <!-- 無資料 -->
        <div v-else-if="paginatedCarModels.length === 0" class="text-center py-5">
          <i class="fa-solid fa-car-side vehicle-empty-icon"></i>
          <h4 class="mt-3" style="color: var(--color-text-primary);">目前沒有符合條件的車款</h4>
          <p style="color: var(--color-text-secondary);">請嘗試更換篩選條件或搜尋關鍵字</p>
        </div>

        <!-- 卡片 -->
        <div v-else class="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-4">
          <div class="col" v-for="car in paginatedCarModels" :key="car.modelId">
            <div class="vehicle-card h-100">

              <!-- 圖片 -->
              <div class="vehicle-card-img">
                <img v-if="car.vehicleImgUrl" :src="car.vehicleImgUrl" :alt="car.modelName">
                <div v-else class="vehicle-card-img-empty">
                  <i class="fa-solid fa-car-side"></i>
                </div>
              </div>

              <!-- 內容 -->
              <div class="vehicle-card-body">
                <!-- 第一列：車名 + 車型 badge -->
                <div class="d-flex justify-content-between align-items-start mb-1">
                  <h3 class="vehicle-card-name">{{ car.brand }} {{ car.modelName }}</h3>
                  <span class="badge bg-primary vehicle-card-type-badge">{{ car.vehicleType }}</span>
                </div>

                <!-- 描述（可選，若無則不顯示） -->
                <p class="vehicle-card-desc">{{ car.vehicleType }}車款，{{ car.fuelType || '汽油' }}動力。</p>

                <!-- 規格 Grid（2 欄 3 列） -->
                <div class="vehicle-spec-grid">
                  <div class="vehicle-spec-item">
                    <i class="fa-solid fa-user-group" aria-hidden="true"></i>
                    <span>{{ car.seats || '-' }} 人座</span>
                  </div>
                  <div class="vehicle-spec-item">
                    <i class="fa-solid fa-suitcase-rolling" aria-hidden="true"></i>
                    <span>{{ car.luggageCapacity || '-' }} 件行李</span>
                  </div>
                  <div class="vehicle-spec-item">
                    <i class="fa-solid fa-gears" aria-hidden="true"></i>
                    <span>{{ car.transmission || '-' }}</span>
                  </div>
                  <div class="vehicle-spec-item">
                    <i class="fa-solid fa-gas-pump" aria-hidden="true"></i>
                    <span>{{ car.fuelType || '-' }}</span>
                  </div>
                  <div class="vehicle-spec-item">
                    <i class="fa-solid fa-gauge-high" aria-hidden="true"></i>
                    <span>{{ formatDisplacement(car.displacement) }}</span>
                  </div>
                  <div class="vehicle-spec-item">
                    <i class="fa-solid fa-arrows-spin" aria-hidden="true"></i>
                    <span>{{ car.turningRadius ? car.turningRadius + 'm 迴轉半徑' : '-' }}</span>
                  </div>
                </div>

                <!-- 底部：價格 + 按鈕 -->
                <div class="vehicle-card-footer">
                  <div class="vehicle-card-price">
                    <strong>${{ getPrice(car).toLocaleString() || '--' }}</strong>
                    <small> /日起</small>
                  </div>
                  <button class="btn btn-primary vehicle-card-cta" @click="goToOrder(car)">
                    立即預訂
                    <i class="fa-solid fa-chevron-right ms-1"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分頁 -->
        <div v-if="!isLoading && totalPages > 1" class="d-flex justify-content-center mt-5">
          <Pagination
            :currentPage="currentPage"
            :totalPages="totalPages"
            @change="currentPage = $event"
          />
        </div>

      </div>
    </section>
  </div>
</template>

<style scoped>
/* ============================================================
   CustomCarModelView — OneRent Design System
   ============================================================ */

.vehicle-page {
  min-height: 100vh;
  background-color: var(--color-bg-page);
  font-family: var(--font-sans);
}

/* ── Hero Banner ── */
.vehicle-hero-banner {
  background: linear-gradient(to right, #fce7d5, #daebfc);
  padding: var(--space-12) 0;
}

.vehicle-hero-container {
  max-width: var(--container-max);
}

.vehicle-hero-banner h1 {
  color: var(--color-text-inverse);
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  margin: 0 0 var(--space-3);
}

.vehicle-hero-desc {
  color: #454d56;
  font-size: var(--text-base);
  font-weight: var(--font-normal);
  line-height: var(--leading-normal);
  max-width: 640px;
  margin: 0;
}

/* ── 篩選工具列 ── */
.vehicle-toolbar-section {
  background-color: var(--color-bg-surface);
  border-bottom: 1px solid var(--color-border);
  padding: var(--space-5) 0;
}

.vehicle-toolbar {
  max-width: var(--container-max);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.vehicle-filter-bar {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.vehicle-filter-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-bg-surface);
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  transition: all var(--transition-fast);
  cursor: pointer;
}

.vehicle-filter-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.vehicle-filter-btn--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-text-inverse);
}

.vehicle-filter-btn--active:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: var(--color-text-inverse);
}

.vehicle-search-wrap {
  width: 100%;
  max-width: 320px;
}

/* ── 列表區 ── */
.vehicle-list-section {
  padding: var(--space-10) 0 var(--space-16);
}

.vehicle-list-section > .container {
  max-width: var(--container-max);
}

/* ── 卡片 ── */
.vehicle-card {
  display: flex;
  flex-direction: column;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
}

.vehicle-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

/* ── 圖片區 ── */
.vehicle-card-img {
  position: relative;
  height: 200px;
  background: var(--color-blue-900);
  overflow: hidden;
}

.vehicle-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.vehicle-card:hover .vehicle-card-img img {
  transform: scale(1.05);
}

.vehicle-card-img-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-blue-300);
  font-size: var(--text-4xl);
  opacity: 0.3;
}

/* ── 卡片內容 ── */
.vehicle-card-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: var(--space-5);
}

.vehicle-card-name {
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  line-height: var(--leading-tight);
}

.vehicle-card-type-badge {
  flex-shrink: 0;
  white-space: nowrap;
}

.vehicle-card-desc {
  margin: 0 0 var(--space-4);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
}

/* ── 規格 Grid（2 欄 3 列） ── */
.vehicle-spec-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-2) var(--space-4);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.vehicle-spec-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.vehicle-spec-item i {
  width: 16px;
  text-align: center;
  color: var(--color-primary);
  font-size: var(--text-sm);
  flex-shrink: 0;
}

/* ── 底部 ── */
.vehicle-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}

.vehicle-card-price {
  color: var(--color-text-primary);
  line-height: 1;
}

.vehicle-card-price small {
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}

.vehicle-card-price strong {
  color: var(--color-primary);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
}

.vehicle-card-cta {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  border-radius: var(--radius-md);
  font-weight: var(--font-semibold);
  font-size: var(--text-sm);
  padding: var(--space-2) var(--space-5);
}

/* ── 空狀態 ── */
.vehicle-empty-icon {
  font-size: 3rem;
  color: var(--color-text-muted);
  opacity: 0.25;
}

/* ── RWD ── */
@media (max-width: 767.98px) {
  .vehicle-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .vehicle-search-wrap {
    max-width: 100%;
  }

  .vehicle-hero-banner {
    padding: var(--space-8) 0;
  }

  .vehicle-hero-banner h1 {
    font-size: var(--text-3xl);
  }
}
</style>