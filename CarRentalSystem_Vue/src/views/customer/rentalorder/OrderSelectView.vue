<script setup>
import { computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { selectVehicleStore } from '@/store/rentalorder/selectVehicle';
import OrderVehicleCard from '@/components/customer/rentalorder/OrderVehicleCard.vue';

const store = selectVehicleStore();
const route = useRoute();
const router = useRouter();

const isCustomerLoggedIn = () => {
  return Boolean(localStorage.getItem('customer_token'));
};

const toDateTimeLocal = (date) => {
  const pad = (value) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const todayDate = computed(() => toDateTimeLocal(new Date()).slice(0, 10));
const minPickupTime = computed(() => toDateTimeLocal(new Date()));
const minReturnTime = computed(() => store.searchParams.pickupTime || minPickupTime.value);

const handleSelectVehicle = (car) => {
  if (store.selectVehicle(car) === false) return;

  const confirmRoute = {
    path: '/orders/custconfirm',
    query: {
      vehicleId: car.id || car.vehicleId,
      modelId: car.modelId || ''
    }
  };

  if (!isCustomerLoggedIn()) {
    localStorage.setItem('system_context', 'customer');
    router.push('/customer/login');
    return;
  }

  router.push(confirmRoute);
};

watch(
  () => [
    store.filters.vehicleTypes,
    store.filters.powerSources,
    store.filters.priceRanges,
    store.searchParams.pickupLocationId
    /*
    前三個 (vehicleTypes, powerSources, priceRanges) 是側邊欄的 Checkbox 陣列（車型、動力、價格）。
    */

  ],
  () => store.resetPagination(), //把當前頁碼重置為 1，確保畫面從第一頁開始顯示
  { deep: true }

  /*
  如果沒有加 { deep: true }，Vue 預設只會做「淺層檢查」，它會覺得：「陣列還是那個陣列啊，沒事！」然後完全不觸發重置分頁的動作。

加上 { deep: true } 後，Vue 就會開啟深度掃描，連陣列裡面多了一個字、少了一個字都會抓出來，確保使用者的每一次點擊都能完美連動！
  
  */
  
);

watch(
  () => [store.searchParams.pickupLocationId, store.searchParams.isOneWayRental],
  () => {
    // 未啟用甲租乙還時，取車門市變更後同步鎖定還車門市。
    store.syncReturnLocationWithPickup();
  }
);

watch(
  () => [
    store.searchParams.rentalMode,
    store.searchParams.pickupLocationId,
    store.searchParams.pickupTime,
    store.searchParams.returnTime,
    store.searchParams.pickupDate,
    store.searchParams.rentalMonths
  ],
  () => {
    store.normalizeSearchTimes();
    store.invalidateVehicleSearch();
  }
);

onMounted(async () => {
  await store.initializeOptions();

  const shouldFetchFromHome = store.applySearchParamsFromQuery(route.query);
  if (shouldFetchFromHome) {
    await store.fetchVehicles();
  }
});
</script>

<template>
  <div class="bg-primary-subtle bg-opacity-25 min-vh-100 pb-5 rental-front-page">
    <main class="container-fluid px-4 py-5" style="max-width: 1280px;">
      <div class="row g-4 align-items-start">
        <aside class="col-lg-3 d-flex flex-column gap-4">
          <!-- aside網頁的「側邊欄」與「輔助資訊」，它是告訴瀏覽器和搜尋引擎：「這區塊的東西是輔助性質的」 -->
           <!-- 在 <aside>（側邊欄）裡面放了兩個 <section> 一個是租車條件、篩選條件 -->
          <section class="card border-0 rounded-4 shadow-sm bg-primary" data-bs-theme="dark">
            <!-- data-bs-theme="dark" 開啟深色模式，文字變白 -->
            <div class="card-body p-4 text-white">
              <h2 class="h3 mb-4 d-flex align-items-center gap-2 text-white">
                <i class="fa-solid fa-magnifying-glass"></i>
                租車條件
              </h2>

              <div class="btn-group w-100 bg-primary bg-opacity-75 rounded-3 p-1 mb-4" role="group" aria-label="租車模式">
                <!-- btn-group按鈕組。讓裡面的兩個按鈕（日租、長租）黏在一起，形成一個整體感。 -->
                <input
                  id="dailyRental"
                  class="btn-check"
                  type="radio"
                  value="daily"
                  :checked="store.searchParams.rentalMode === 'daily'"
                  @change="store.setRentalMode('daily')"
                >
                <!-- type="radio" 單選，daily是定義在store -->
                <label class="btn btn-sm btn-outline-light border-0 fw-bold" for="dailyRental">日租</label>

                <input
                  id="longTermRental"
                  class="btn-check"
                  type="radio"
                  value="longTerm"
                  :checked="store.searchParams.rentalMode === 'longTerm'"
                  @change="store.setRentalMode('longTerm')"
                >
                <label class="btn btn-sm btn-outline-light border-0 fw-bold" for="longTermRental">長租</label>
              </div>

              <div class="mb-3">
                <label class="form-label small fw-bold text-white-50" for="pickupLocation">取車地點</label>
                <select
                  id="pickupLocation"
                  v-model.number="store.searchParams.pickupLocationId"
                  class="form-select bg-primary border border-light border-opacity-25 text-white"
                  :disabled="store.isInitialLoading"
                >
                  <option class="text-dark" value="">請選擇取車地點</option>
                  <option
                    v-for="location in store.locations"
                    :key="location.locationId"
                    class="text-dark"
                    :value="location.locationId"
                  >
                    {{ location.locationName }}
                  </option>
                </select>
              </div>

              <div class="form-check form-switch mb-3">
                <input
                  id="oneWayRental"
                  v-model="store.searchParams.isOneWayRental"
                  class="form-check-input"
                  type="checkbox"
                  role="switch"
                  @change="store.syncReturnLocationWithPickup"
                >
                <label class="form-check-label fw-bold" for="oneWayRental">甲租乙還</label>
                <p class="small text-white-50 mb-0 mt-1">
                  開啟後可選擇不同還車門市，調度費會在結帳頁統一計算。
                </p>
              </div>

              <div class="mb-3">
                <label class="form-label small fw-bold text-white-50" for="returnLocation">還車地點</label>
                <select
                  id="returnLocation"
                  v-model.number="store.searchParams.returnLocationId"
                  class="form-select bg-primary border border-light border-opacity-25 text-white"
                  :disabled="store.isInitialLoading || !store.searchParams.isOneWayRental"
                >
                  <option class="text-dark" value="">請選擇還車地點</option>
                  <option
                    v-for="location in store.locations"
                    :key="location.locationId"
                    class="text-dark"
                    :value="location.locationId"
                  >
                    {{ location.locationName }}
                  </option>
                </select>
              </div>

              <template v-if="store.searchParams.rentalMode === 'daily'">
                <div class="mb-3">
                  <label class="form-label small fw-bold text-white-50" for="pickupTime">取車時間</label>
                  <input
                    id="pickupTime"
                    v-model="store.searchParams.pickupTime"
                    class="form-control bg-primary border border-light border-opacity-25 text-white"
                    type="datetime-local"
                    :min="minPickupTime"
                  >
                </div>

                <div class="mb-4">
                  <label class="form-label small fw-bold text-white-50" for="returnTime">還車時間</label>
                  <input
                    id="returnTime"
                    v-model="store.searchParams.returnTime"
                    class="form-control bg-primary border border-light border-opacity-25 text-white"
                    type="datetime-local"
                    :min="minReturnTime"
                  >
                </div>
              </template>

              <template v-else>
                <div class="mb-3">
                  <label class="form-label small fw-bold text-white-50" for="pickupDate">取車日期</label>
                  <input
                    id="pickupDate"
                    v-model="store.searchParams.pickupDate"
                    class="form-control bg-primary border border-light border-opacity-25 text-white"
                    type="date"
                    :min="todayDate"
                  >
                </div>

                <div class="mb-4">
                  <label class="form-label small fw-bold text-white-50" for="rentalMonths">租用期數</label>
                  <select
                    id="rentalMonths"
                    v-model.number="store.searchParams.rentalMonths"
                    class="form-select bg-primary border border-light border-opacity-25 text-white"
                  >
                    <option
                      v-for="period in store.rentalPeriods"
                      :key="period.value"
                      class="text-dark"
                      :value="period.value"
                    >
                      {{ period.label }}
                    </option>
                  </select>
                </div>
              </template>

              <button
                class="btn btn-warning w-100 fw-bold py-3 shadow-sm"
                type="button"
                :disabled="store.isInitialLoading || store.isLoading"
                @click="store.fetchVehicles"
              >
                {{ store.isLoading ? '搜尋中...' : '更新搜尋' }}
              </button>
            </div>
          </section>

          <section class="card rounded-4 shadow-sm border border-light-subtle">
            <div class="card-body p-4">
              <h2 class="h3 mb-4">篩選條件</h2>

              <div class="mb-4">
                <p class="small text-secondary fw-bold mb-2">車型</p>
                <div v-if="store.vehicleTypeOptions.length === 0" class="text-secondary small">
                  正在載入車型資料...
                </div>
                <div v-for="type in store.vehicleTypeOptions" :key="type" class="form-check mb-2">
                  <input
                    :id="`vehicleType-${type}`"
                    v-model="store.filters.vehicleTypes"
                    class="form-check-input"
                    type="checkbox"
                    :value="type"
                  >
                  <label class="form-check-label" :for="`vehicleType-${type}`">{{ type }}</label>
                </div>
              </div>

              <div class="mb-4">
                <p class="small text-secondary fw-bold mb-2">動力來源</p>
                <div v-if="store.powerSourceOptions.length === 0" class="text-secondary small">
                  搜尋後依車輛資料顯示
                </div>
                <div v-for="source in store.powerSourceOptions" :key="source" class="form-check mb-2">
                  <input
                    :id="`powerSource-${source}`"
                    v-model="store.filters.powerSources"
                    class="form-check-input"
                    type="checkbox"
                    :value="source"
                  >
                  <label class="form-check-label" :for="`powerSource-${source}`">
                    {{ source === '電動' ? '電動車（EV）' : source }}
                  </label>
                </div>
              </div>

              <div>
                <p class="small text-secondary fw-bold mb-2">價格區間</p>
                <div v-if="store.currentPriceRanges.length === 0" class="text-secondary small">
                  正在載入方案價格...
                </div>
                <div v-for="range in store.currentPriceRanges" :key="range.value" class="form-check mb-2">
                  <input
                    :id="`priceRange-${range.value}`"
                    v-model="store.filters.priceRanges"
                    class="form-check-input"
                    type="checkbox"
                    :value="range.value"
                  >
                  <label class="form-check-label" :for="`priceRange-${range.value}`">
                    {{ range.label }}
                  </label>
                </div>
              </div>
            </div>
          </section>
        </aside>

        <section class="col-lg-9">
          <div class="d-flex flex-wrap gap-3 mb-4">
            <button
              v-for="category in store.categories"
              :key="category"
              class="btn rounded-pill px-4 fw-bold"
              :class="store.isCategoryActive(category) ? 'btn-primary shadow-sm' : 'btn-light border text-dark'"
              type="button"
              @click="store.setCategory(category)"
            >
              {{ category }}
            </button>
          </div>

          <div v-if="store.errorMessage" class="alert alert-warning d-flex align-items-center gap-2" role="alert">
            <i class="fa-solid fa-circle-exclamation"></i>
            <span>{{ store.errorMessage }}</span>
          </div>

          <div v-if="store.isInitialLoading" class="text-center py-5 mt-5 bg-white rounded-4 shadow-sm border border-light-subtle">
            <div class="spinner-border text-primary" role="status">
              <span class="visually-hidden">載入中...</span>
            </div>
            <p class="text-secondary mt-3 fw-bold mb-0">正在載入租車地點與方案...</p>
          </div>

          <div v-else-if="store.isLoading" class="text-center py-5 mt-5">
            <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem;">
              <span class="visually-hidden">載入中...</span>
            </div>
            <p class="text-secondary mt-3 fw-bold">正在載入可租車輛...</p>
          </div>

          <div v-else-if="store.vehicles.length === 0" class="text-center py-5 mt-5 bg-white rounded-4 shadow-sm border border-light-subtle">
            <i class="fa-solid fa-magnifying-glass fs-1 text-secondary opacity-50 mb-3"></i>
            <h3 class="h4 fw-bold">請先設定租車條件</h3>
            <p class="text-secondary mb-0">選擇租車模式、地點與時間後，點選更新搜尋查詢可租車輛。</p>
          </div>

          <div v-else-if="store.filteredVehicles.length === 0" class="text-center py-5 mt-5 bg-white rounded-4 shadow-sm border border-light-subtle">
            <i class="fa-solid fa-car-tunnel fs-1 text-secondary opacity-50 mb-3"></i>
            <h3 class="h4 fw-bold">找不到符合條件的車輛</h3>
            <p class="text-secondary mb-0">請調整搜尋或篩選條件後，再點選更新搜尋。</p>
          </div>

          <template v-else>
            <OrderVehicleCard
              v-for="car in store.pagedVehicles"
              :key="car.id"
              :car="car"
              @select="handleSelectVehicle"
            />

            <nav v-if="store.totalPages > 1" class="mt-5" aria-label="車輛列表分頁">
              <ul class="pagination justify-content-center gap-2">
                <li class="page-item" :class="{ disabled: store.currentPage === 1 }">
                  <button class="page-link rounded-3" type="button" aria-label="上一頁" @click="store.setPage(store.currentPage - 1)">
                    <i class="fa-solid fa-chevron-left"></i>
                  </button>
                </li>
                <li
                  v-for="page in store.totalPages"
                  :key="page"
                  class="page-item"
                  :class="{ active: store.currentPage === page }"
                >
                  <button class="page-link rounded-3 px-3" type="button" @click="store.setPage(page)">
                    {{ page }}
                  </button>
                </li>
                <li class="page-item" :class="{ disabled: store.currentPage === store.totalPages }">
                  <button class="page-link rounded-3" type="button" aria-label="下一頁" @click="store.setPage(store.currentPage + 1)">
                    <i class="fa-solid fa-chevron-right"></i>
                  </button>
                </li>
              </ul>
            </nav>
          </template>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.rental-front-page {
  padding-top: 92px;
}

@media (max-width: 991.98px) {
  .rental-front-page {
    padding-top: 76px;
  }
}
</style>
