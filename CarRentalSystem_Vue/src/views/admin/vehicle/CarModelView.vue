<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';

import { useCarmodelStore } from '@/store/vehicle/carModelStore';
import { carModelAPI } from '@/api/vehicle/carModelApi';
import CarModelFormModal from '@/components/admin/vehicle/CarModelFormModal.vue';
import AlertToast from '@/components/common/AlertToast.vue';

const router = useRouter();

// 宣告車款狀態
const carModel = ref(null);
const carModelStore = useCarmodelStore();
const formModalRef = ref(null);
const toastRef = ref(null);

const isLoading = ref(false);



// -----------------------------------------
// 1. 資料區 (完美對應你的資料庫欄位)
// -----------------------------------------
//存車款
const carModels = ref([
  {
    modelId: null,
    brand: '',
    modelName: '',
    displacement: '',
    turningRadius: '',
    vehicleType: '',
    seats: '',
    luggageCapacity: '',
    fuelType: '',
    transmission: '',
    wheelchairAvailable: false,
    vehicleImgUrl: '',
    isDeleted: false,
  },
]);

// Modal新增表單區：當子元件 Modal 存檔成功時，觸發此方法重新抓取清單
const refreshList = () => {
    fetchData();
}

// -----------------------------------------
// 2. 篩選與排序邏輯
// -----------------------------------------
const currentFilter = ref('All');
// 動態產生車型類別 (避免寫死)
const categories = computed(() => {
  const types = carModels.value.map(car => car.vehicleType);
  return ['All', ...new Set(types)]; // 利用 Set 過濾重複項目
});

const sortBy = ref('name');
const searchQuery = ref('');

// 處理畫面上要顯示的資料
const filteredCarModels = computed(() => {
  let result = carModels.value;
  
  // 篩選未被軟刪除的資料
  result = result.filter(car => !car.isDeleted);

  // 0. 關鍵字搜尋 (新增)
  const keyword = searchQuery.value.toLowerCase().trim();
  if (keyword) {
    result = result.filter(car => 
      (car.brand && car.brand.toLowerCase().includes(keyword)) ||
      (car.modelName && car.modelName.toLowerCase().includes(keyword))
    );
  }

  // 1. 篩選類別
  if (currentFilter.value !== 'All') {
    result = result.filter(car => car.vehicleType === currentFilter.value);
  }

  // 2. 排序邏輯
  if (sortBy.value === 'name') {
    result.sort((a, b) => a.modelName.localeCompare(b.modelName));
  } else if (sortBy.value === 'capacity') {
    result.sort((a, b) => b.seats - a.seats); // 座位數多的排前面
  }
  return result;
});

// -----------------------------------------
// 3. 動作區
// -----------------------------------------
const handleViewFleet = (id) => {
  // 跳到車輛列表，並透過 query parameter 自動過濾該車款
  router.push({ path: '/vehicle', query: { modelId: id } }); 
};

// 處理上架切換
const togglePublish = (car) => {
  car._isPublished = !car._isPublished;
  
  // 將已上架的車款 ID 儲存到 localStorage
  const publishedIds = carModels.value.filter(c => c._isPublished).map(c => c.modelId);
  localStorage.setItem('publishedCarModels', JSON.stringify(publishedIds));
};

// -----------------------------------------
// 生命週期
// -----------------------------------------
const fetchData = async () => {
  // 串接 API
  try {
    isLoading.value = true;
    const res = await carModelAPI.getAll();
    
    // 讀取 localStorage 內已上架的 ID
    const savedIds = JSON.parse(localStorage.getItem('publishedCarModels') || '[]');
    
    // 寫入自訂的 _isPublished 屬性
    carModels.value = res.data.map(car => ({
      ...car,
      _isPublished: savedIds.includes(car.modelId)
    }));
  } catch (error) {
    console.error(error);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div class="container-fluid py-4 vehicle-page-bg">
    
    <div class="d-flex align-items-center mb-4 text-muted">
      <span class="vehicle-breadcrumb-link fw-bold" role="button" @click="router.push('/dispatch/dashboard')">
        <font-awesome-icon icon="fa-solid fa-headset" style="width: 1.25rem; font-size: 0.9rem;"/> 車輛調度中心
      </span>
      <i class="fa-solid fa-chevron-right mx-2 small"></i>
      <span class="text-dark fw-bold">車款管理</span>
    </div>

    <div class="mb-4 d-flex justify-content-between align-items-end">
      <div>
        <h2 class="fw-bold mb-1 vehicle-page-title">車款管理</h2>
        <p class="mb-0 small vehicle-page-subtitle">建立新進品牌型號、管理現有車款</p>
      </div>
      <button class="btn fw-bold d-flex align-items-center vehicle-btn-primary" @click="formModalRef.openModal(null, 'add')">
        <i class="fa-solid fa-file-circle-plus me-2"></i>新增車款
      </button>
    </div>

    <div class="card border-0 mb-4 vehicle-search-card">
      <div class="card-body p-3 d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
        
        <div class="d-flex gap-2 vehicle-scroll-hidden w-100">
          <button 
            v-for="cat in categories" :key="cat"
            class="btn btn-sm rounded-pill px-3 fw-bold border-0 text-nowrap"
            :class="currentFilter === cat ? 'btn-primary' : 'btn-light text-muted'"
            @click="currentFilter = cat"
          >
            {{ cat === 'All' ? 'All Models' : cat }}
          </button>
        </div>

        <div class="d-flex gap-2 w-100" style="max-width: 450px;">
          <!-- 新增關鍵字查詢 -->
          <div class="input-group input-group-sm w-100">
            <span class="input-group-text bg-light border-0 text-muted"><i class="fa-solid fa-magnifying-glass"></i></span>
            <input v-model="searchQuery" type="text" class="form-control border-0 bg-light shadow-none fw-bold" placeholder="搜尋品牌或型號...">
          </div>
          <!-- 排序 -->
          <select v-model="sortBy" class="form-select form-select-sm bg-light border-0 fw-bold text-muted w-100" style="max-width: 180px;">
            <option value="name">依 型號 排序</option>
            <option value="capacity">依 座位數 排序</option>
          </select>
        </div>

      </div>
    </div>

    <div v-if="!isLoading" class="row row-cols-1 row-cols-md-2 row-cols-lg-3 row-cols-xl-4 g-4">
      
      <div class="col" v-for="car in filteredCarModels" :key="car.modelId">
        <div class="card h-100 border-0 overflow-hidden vehicle-card">
          
          <div class="position-relative bg-light border-bottom d-flex align-items-center justify-content-center" style="height: 180px;">
            <img v-if="car.vehicleImgUrl" :src="car.vehicleImgUrl" class="w-100 h-100 vehicle-img-cover" alt="Car Image">
            <i v-else class="fa-solid fa-car-side fs-1 text-muted opacity-25"></i>
            
            <span class="position-absolute top-0 end-0 m-2 badge bg-white text-dark border shadow-sm text-uppercase" style="font-size: 0.65rem;">
              {{ car.vehicleType }}
            </span>
          </div>

          <div class="card-body d-flex flex-column p-4">
            
            <div class="d-flex justify-content-between align-items-start mb-3">
              <div>
                <h5 class="fw-bold mb-0 text-dark">{{ car.brand }} {{ car.modelName }}</h5>
                <small class="text-muted d-block mt-1">
                  {{ car.displacement }} cc <span v-if="car.wheelchairAvailable" class="text-primary ms-1" title="無障礙車輛"><i class="fa-solid fa-wheelchair"></i></span>
                </small>
              </div>
              <span v-if="car.activeCount" class="badge text-bg-primary bg-opacity-10 text-primary px-2 py-1">
                {{ car.activeCount }} Active
              </span>
            </div>

            <div class="row g-2 mt-auto mb-3 pt-3 border-top">
              <div class="col-6 d-flex align-items-center text-muted small text-truncate">
                <i class="fa-solid fa-user-group me-2 vehicle-icon-w text-center"></i>{{ car.seats || '-' }} 人座
              </div>
              <div class="col-6 d-flex align-items-center text-muted small text-truncate">
                <i class="fa-solid fa-gears me-2 vehicle-icon-w text-center"></i>{{ car.transmission || 'Auto' }}
              </div>
              <div class="col-6 d-flex align-items-center text-muted small text-truncate">
                <i class="fa-solid fa-gas-pump me-2 vehicle-icon-w text-center"></i>{{ car.fuelType || '-' }}
              </div>
              <div class="col-6 d-flex align-items-center text-muted small text-truncate">
                <i class="fa-solid fa-suitcase me-2 vehicle-icon-w text-center"></i>{{ car.luggageCapacity || '-' }} 件
              </div>
            </div>

            <div class="d-flex gap-2 mb-2">

            </div>
            <div class="d-flex gap-2">
              <!-- <button @click="handleViewFleet(car.modelId)" class="btn btn-outline-primary btn-sm px-3 fw-bold flex-grow-1">
                查看車隊
              </button> -->
                <button @click="togglePublish(car)" class="btn btn-sm px-3 fw-bold flex-grow-1" :class="car._isPublished ? 'vehicle-btn-published' : 'btn-outline-secondary'">
                <i class="fa-solid" :class="car._isPublished ? 'fa-check ' : 'fa-arrow-up'"></i>
                {{ car._isPublished ? '已上架' : '上架前台' }}
              </button>
              <button @click="formModalRef.openModal(car)" class="btn btn-light text-primary border btn-sm px-3">
                <i class="fa-solid fa-pen"></i>
              </button>
            </div>

          </div>
        </div>
      </div>

    </div>
    
    <div v-if="isLoading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>
    <div v-if="!isLoading && filteredCarModels.length === 0" class="text-center py-5 text-muted">
      <i class="fa-solid fa-car-burst fs-1 mb-3 opacity-50"></i>
      <h5>找不到符合條件的車款</h5>
    </div>

  </div>
  <CarModelFormModal ref="formModalRef" @saved="refreshList" />
  <AlertToast ref="toastRef" />
</template>

<style scoped>
/* ============================================================
   CarModelView — OneRent Design System
   所有色碼、間距、圓角皆使用 variables-3.css 定義的 CSS 變數
   自訂 class 一律加上 vehicle- 前綴
   ============================================================ */

/* ── 頁面底色 ── */
.vehicle-page-bg {
  background-color: var(--color-bg-page);
  min-height: 100vh;
}

/* ── 麵包屑連結 ── */
.vehicle-breadcrumb-link {
  color: var(--color-primary);
  cursor: pointer;
  transition: color var(--transition-fast);
}
.vehicle-breadcrumb-link:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
}

/* ── 頁面標題 ── */
.vehicle-page-title {
  color: var(--color-primary);
  letter-spacing: -0.02em;
  font-family: var(--font-sans);
}
.vehicle-page-subtitle {
  color: var(--color-text-secondary);
}

/* ── 搜尋卡片 ── */
.vehicle-search-card {
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
}

/* ── 主要操作按鈕（新增車款）── */
.vehicle-btn-primary {
  background-color: var(--color-primary);
  color: var(--color-text-inverse);
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
}
.vehicle-btn-primary:hover {
  background-color: var(--color-primary-hover);
  color: var(--color-text-inverse);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* ── 車款卡片 ── */
.vehicle-card {
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
}
.vehicle-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

/* ── 卡片圖片填滿 ── */
.vehicle-img-cover {
  object-fit: cover;
}

/* ── icon 對齊寬度 ── */
.vehicle-icon-w {
  width: 15px;
}

/* ── pill 捲軸隱藏 ── */
.vehicle-scroll-hidden {
  overflow-x: auto;
  scrollbar-width: none;
}
.vehicle-scroll-hidden::-webkit-scrollbar {
  display: none;
}

/* ── 已上架按鈕（沙色底白字）── */
.vehicle-btn-published {
  background-color: var(--color-accent);
  color: var(--color-text-inverse) !important;
  border-color: var(--color-accent);
}
.vehicle-btn-published:hover {
  background-color: var(--color-accent-hover);
  border-color: var(--color-accent-hover);
  color: var(--color-text-inverse) !important;
}
</style>