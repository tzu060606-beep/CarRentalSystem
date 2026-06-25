<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { locationAPI } from '@/api/vehicle/locationApi';

const router = useRouter();
const locations = ref([]);
const isLoading = ref(false);
const mapElements = ref({});
const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

// 據點狀態對照
const statusInfo = {
  OPERATING: { text: '營業中', color: 'var(--color-success)' },
  REST:      { text: '暫停營業', color: 'var(--color-warning)' },
  CANCEL:    { text: '已撤銷', color: 'var(--color-text-muted)' },
};

const getStatusCode = (loc) => {
  if (!loc.locationStatus) return 'OPERATING';
  return loc.locationStatus.dbCode || loc.locationStatus;
};

const getStatusText = (loc) => statusInfo[getStatusCode(loc)]?.text || '未知';
const getStatusColor = (loc) => statusInfo[getStatusCode(loc)]?.color || 'var(--color-text-muted)';

// 載入資料
onMounted(async () => {
  try {
    isLoading.value = true;
    const res = await locationAPI.getAll();
    // 只顯示營運中的據點
    locations.value = res.data.filter(loc => getStatusCode(loc) === 'OPERATING');
  } catch (error) {
    console.error('載入據點失敗', error);
  } finally {
    isLoading.value = false;
    // 確保 DOM 已更新 (mapElements refs 已經綁定) 後再載入並初始化地圖
    await nextTick();
    loadGoogleMaps();
  }
});

// Google Maps
const loadGoogleMaps = () => {
  if (window.google && window.google.maps) {
    initAllMaps();
    return;
  }
  window.initLocationMaps = initAllMaps;
  const script = document.createElement('script');
  script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&callback=initLocationMaps&loading=async`;
  script.async = true;
  script.defer = true;
  document.head.appendChild(script);
};

const initAllMaps = () => {
  locations.value.forEach((loc) => {
    const el = mapElements.value[loc.locationId];
    if (!el) return;

    // 用地址做 Geocoding 取得座標
    const geocoder = new google.maps.Geocoder();
    const address = loc.address || loc.locationName;

    geocoder.geocode({ address }, (results, status) => {
      if (status === 'OK' && results[0]) {
        const position = results[0].geometry.location;
        const map = new google.maps.Map(el, {
          zoom: 15,
          center: position,
          mapTypeControl: false,
          streetViewControl: false,
          fullscreenControl: false,
          zoomControl: false,
        });
        new google.maps.Marker({
          position,
          map,
          title: loc.locationName,
          icon: {
            url: 'https://maps.google.com/mapfiles/ms/icons/blue-dot.png',
          },
        });
      } else {
        // Geocode 失敗時顯示台灣中心
        const fallback = { lat: 24.15, lng: 120.67 };
        new google.maps.Map(el, {
          zoom: 7,
          center: fallback,
          mapTypeControl: false,
          streetViewControl: false,
          fullscreenControl: false,
          zoomControl: false,
        });
      }
    });
  });
};

const setMapRef = (el, id) => {
  if (el) mapElements.value[id] = el;
};

const openGoogleMap = (loc) => {
  const q = encodeURIComponent(loc.address || loc.locationName);
  window.open(`https://www.google.com/maps/search/?api=1&query=${q}`, '_blank');
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
</script>

<template>
  <div class="vehicle-location-page">

    <!-- Hero Banner -->
    <section class="vehicle-location-hero">
      <div class="container vehicle-location-hero-inner">
        <h1>服務據點</h1>
        <p class="vehicle-location-hero-desc">
          OneRent 遍布全台，確保您隨時隨地都能輕鬆啟程。請在下方尋找您方便的租車據點，安心上路。
        </p>
      </div>
    </section>

    <!-- 據點列表 -->
    <section class="vehicle-location-list">
      <div class="container vehicle-location-container">

        <!-- 載入中 -->
        <div v-if="isLoading" class="d-flex justify-content-center py-5">
          <div class="spinner-border" style="color: var(--color-primary);" role="status"></div>
        </div>

        <!-- 無資料 -->
        <div v-else-if="locations.length === 0" class="text-center py-5">
          <i class="fa-solid fa-map-location-dot" style="font-size: 3rem; color: var(--color-text-muted); opacity: 0.25;"></i>
          <h4 class="mt-3" style="color: var(--color-text-primary);">目前沒有營運中的據點</h4>
        </div>

        <!-- 據點卡片 -->
        <div v-else class="d-flex flex-column gap-4">
          <div
            v-for="(loc, index) in locations"
            :key="loc.locationId"
            class="vehicle-location-card"
            :class="{ 'vehicle-location-card--reverse': index % 2 !== 0 }"
          >
            <!-- 地圖區 -->
            <div class="vehicle-location-map">
              <div :ref="(el) => setMapRef(el, loc.locationId)" class="vehicle-location-map-canvas">
                <div class="d-flex justify-content-center align-items-center h-100">
                  <span style="color: var(--color-text-muted);">地圖載入中...</span>
                </div>
              </div>
            </div>

            <!-- 資訊區 -->
            <div class="vehicle-location-info">
              <!-- 據點名稱 -->
              <h2 class="vehicle-location-name">{{ loc.locationName }}</h2>

              <!-- 地址 -->
              <div class="vehicle-location-detail">
                <i class="fa-solid fa-location-dot"></i>
                <span>{{ loc.address || '地址尚未設定' }}</span>
              </div>

              <!-- 電話 -->
              <div class="vehicle-location-detail">
                <i class="fa-solid fa-phone"></i>
                <span>{{ loc.phone || '尚未設定' }}</span>
              </div>

              <!-- 營業狀態 -->
              <div class="vehicle-location-detail">
                <i class="fa-regular fa-clock"></i>
                <span class="text-secondary" :style="{fontWeight: 'var(--font-semibold)' }">
                    08:00 - 20:30
                </span>
              </div>

              <!-- 按鈕 -->
              <button
                class="btn btn-primary vehicle-location-cta"
                 @click="goToOrder(loc)"
              >
                查看可用車輛
                <i class="fa-solid fa-arrow-right ms-2"></i>
              </button>
            </div>
          </div>
        </div>

      </div>
    </section>
  </div>
</template>

<style scoped>
/* ============================================================
   CustomLocationView — OneRent Design System
   ============================================================ */

.vehicle-location-page {
  min-height: 100vh;
  background-color: var(--color-bg-page);
  font-family: var(--font-sans);
}

/* ── Hero Banner ── */
.vehicle-location-hero {
  /* background: linear-gradient(to right, #dba772, #fce7d5, #daebfc, #0150ad); */
  background: linear-gradient(to right,  #fce7d5, #daebfc);
  padding: var(--space-12) 0;
}

.vehicle-location-hero-inner {
  max-width: var(--container-max);
}

.vehicle-location-hero h1 {
  color: var(--color-blue-900);
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  margin: 0 0 var(--space-3);
}

.vehicle-location-hero-desc {
  color: var(--color-text-secondary);
  font-size: var(--text-base);
  font-weight: var(--font-normal);
  line-height: var(--leading-normal);
  max-width: 640px;
  margin: 0;
}

/* ── 列表區 ── */
.vehicle-location-list {
  padding: var(--space-10) 0 var(--space-16);
}

.vehicle-location-container {
  max-width: var(--container-max);
}

/* ── 據點卡片（左右交錯佈局） ── */
.vehicle-location-card {
  display: grid;
  grid-template-columns: 3fr 4fr;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-normal);
  min-height: 280px;
}

.vehicle-location-card:hover {
  box-shadow: var(--shadow-lg);
}

/* 偶數卡片：地圖在左、資訊在右（反轉） */
.vehicle-location-card--reverse {
  direction: rtl;
}

.vehicle-location-card--reverse > * {
  direction: ltr;
}

/* ── 地圖區 ── */
.vehicle-location-map {
  position: relative;
  min-height: 280px;
}

.vehicle-location-map-canvas {
  width: 100%;
  height: 100%;
  min-height: 280px;
  background-color: var(--color-bg-sunken);
}

/* ── 資訊區 ── */
.vehicle-location-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: var(--space-8) var(--space-10);
}

.vehicle-location-name {
  margin: 0 0 var(--space-5);
  color: var(--color-text-primary);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  line-height: var(--leading-tight);
}

.vehicle-location-detail {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--text-base);
}

.vehicle-location-detail i {
  width: 20px;
  text-align: center;
  color: var(--color-primary);
  font-size: var(--text-base);
  flex-shrink: 0;
}

.vehicle-location-cta {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-5);
  padding: var(--space-3) var(--space-6);
  border-radius: var(--radius-full);
  font-weight: var(--font-semibold);
  font-size: var(--text-sm);
  align-self: flex-start;
}

/* ── RWD ── */
@media (max-width: 767.98px) {
  .vehicle-location-card {
    grid-template-columns: 1fr;
  }

  .vehicle-location-card--reverse {
    direction: ltr;
  }

  .vehicle-location-map {
    min-height: 200px;
  }

  .vehicle-location-info {
    padding: var(--space-5);
  }

  .vehicle-location-hero {
    padding: var(--space-8) 0;
  }

  .vehicle-location-hero h1 {
    font-size: var(--text-3xl);
  }
}
</style>
