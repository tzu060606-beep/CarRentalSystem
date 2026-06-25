<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useDispatchStore } from '@/store/vehicle/dispatchLogStore';
import { locationAPI } from '@/api/vehicle/locationApi';
import { vehicleAPI } from '@/api/vehicle/vehicleApi';

const router = useRouter();
const dispatchStore = useDispatchStore();
const { dispatchLogs } = storeToRefs(dispatchStore);
const isLoading = ref(false);

// -----------------------------------------
// 1. 各狀態筆數即時統計 (Computed)
// -----------------------------------------
const pendingCount = computed(() =>
  dispatchLogs.value.filter(log => log.status?.dbCode === 'PENDING').length
);
const inProcessCount = computed(() =>
  dispatchLogs.value.filter(log => log.status?.dbCode === 'IN_PROCESS').length
);
const finishedCount = computed(() =>
  dispatchLogs.value.filter(log => log.status?.dbCode === 'FINISHED').length
);

// -----------------------------------------
// 2. 進行中調度任務（僅七天內 + 前 5 筆）
// -----------------------------------------
const activeTasks = computed(() => {
  const now = new Date();
  const sevenDaysLater = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
  return dispatchLogs.value
    .filter(log => {
      if (!['PENDING', 'IN_PROCESS'].includes(log.status?.dbCode)) return false;
      if (!log.scheduledStartTime) return true; // 無時間的也顯示
      const t = new Date(log.scheduledStartTime);
      return t >= now;
    })
    .slice(0, 5);
});

// -----------------------------------------
// 3. 狀態對應表
// -----------------------------------------
const statusMap = {
  PENDING:    { text: '待執行', badgeClass: 'bg-warning text-dark' },
  IN_PROCESS: { text: '調度中', badgeClass: 'bg-primary text-white' },
  FINISHED:   { text: '已完成', badgeClass: 'bg-success bg-opacity-10 text-success' },
  CANCEL:     { text: '已取消', badgeClass: 'bg-light text-muted border' },
};

// -----------------------------------------
// 4. 各據點飽和度 (從 API 即時計算)
// -----------------------------------------
const locations = ref([]);
const vehicles = ref([]);

const locationSaturation = computed(() => {
  return locations.value
    .filter(loc => loc.parkingCapacity && loc.parkingCapacity > 0)
    .map(loc => {
      const currentCount = vehicles.value.filter(
        v => v.location?.locationId === loc.locationId && !v.isDeleted
      ).length;
      const capacity = loc.parkingCapacity;
      const percentage = Math.min(Math.round((currentCount / capacity) * 100), 100);
      return {
        locationId: loc.locationId,
        locationName: loc.locationName,
        currentCount,
        capacity,
        percentage,
      };
    });
});

// -----------------------------------------
// 5. 工具方法
// -----------------------------------------
const formatDateTime = (val) => {
  if (!val) return '--';
  const d = new Date(val);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}, ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const handleViewAllTasks = () => {
  router.push('/dispatch/task');
};

// -----------------------------------------
// 6. 生命週期：載入真實資料
// -----------------------------------------
onMounted(async () => {
  isLoading.value = true;
  try {
    await dispatchStore.fetchDispatches();
    const [locRes, vehRes] = await Promise.all([
      locationAPI.getAll(),
      vehicleAPI.getAll(),
    ]);
    locations.value = Array.isArray(locRes.data) ? locRes.data : [];
    vehicles.value = Array.isArray(vehRes.data) ? vehRes.data : [];
  } catch (err) {
    console.error('載入調度資料失敗', err);
  } finally {
    isLoading.value = false;
  }
});
</script>

<template>
  <div class="container-fluid py-4">
    
    <div class="mb-4">
      <h2 class="fw-bold mb-1" style="color: var(--color-text-primary);">車輛調度中心</h2>
      <p style="color: var(--color-text-secondary);">即時掌握車隊調度狀態與維修保養健康度。</p>
    </div>

    <!-- 載入中 -->
    <div v-if="isLoading" class="d-flex justify-content-center py-5">
      <div class="spinner-border" style="color: var(--color-primary);" role="status"></div>
    </div>

    <template v-else>

      <!-- ========== 統計卡片 ========== -->
      <div class="row g-4 mb-4">

        <!-- 待處理調度 -->
        <div class="col-md-4">
          <div class="vehicle-stat-card vehicle-stat-card--pending" @click="router.push('/dispatch/task')">
            <div class="d-flex justify-content-between align-items-start">
              <span class="vehicle-stat-label">待處理調度</span>
              <span class="vehicle-stat-icon-wrap">
                <i class="fa-solid fa-arrow-up-right-from-square"></i>
              </span>
            </div>
            <h2 class="vehicle-stat-number">{{ pendingCount }}</h2>
            <div class="vehicle-stat-footer">
              <i class="fa-solid fa-chart-line me-1"></i>
              {{ pendingCount }} 件待執行
            </div>
          </div>
        </div>

        <!-- 進行中調度 -->
        <div class="col-md-4">
          <div class="vehicle-stat-card vehicle-stat-card--active" @click="router.push('/dispatch/task')">
            <div class="d-flex justify-content-between align-items-start">
              <span class="vehicle-stat-label">進行中調度</span>
              <span class="vehicle-stat-icon-wrap">
                <i class="fa-solid fa-arrow-up-right-from-square"></i>
              </span>
            </div>
            <h2 class="vehicle-stat-number">{{ inProcessCount }}</h2>
            <div class="vehicle-stat-footer">
              <i class="fa-solid fa-chart-line me-1"></i>
              共 {{ finishedCount }} 件已完成
            </div>
          </div>
        </div>

        <!-- 已完成調度 -->
        <div class="col-md-4">
          <div class="vehicle-stat-card vehicle-stat-card--finished" @click="router.push('/dispatch/task')">
            <div class="d-flex justify-content-between align-items-start">
              <span class="vehicle-stat-label">已完成調度</span>
              <span class="vehicle-stat-icon-wrap">
                <i class="fa-solid fa-arrow-up-right-from-square"></i>
              </span>
            </div>
            <h2 class="vehicle-stat-number">{{ finishedCount }}</h2>
            <div class="vehicle-stat-footer">
              <i class="fa-solid fa-chart-line me-1"></i>
              全部 {{ dispatchLogs.length }} 筆調度單
            </div>
          </div>
        </div>

      </div>

      <!-- ========== 任務表格 + 據點飽和度 ========== -->
      <div class="row g-4 mb-4">
        
        <!-- 進行中任務 -->
        <div class="col-lg-8">
          <div class="card border-0 shadow-sm rounded-4 h-100 overflow-hidden">
            <div class="card-header bg-white border-bottom-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center">
              <h5 class="fw-bold mb-0">進行中調度任務</h5>
              <a href="#" @click.prevent="handleViewAllTasks" class="text-primary text-decoration-none small fw-bold d-flex align-items-center">
                查看全部任務 <i class="fa-solid fa-chevron-right ms-1 small"></i>
              </a>
            </div>
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0 border-top">
                <thead class="table-light text-muted small">
                  <tr>
                    <th class="px-4 py-3 fw-normal">車輛 / 車牌</th>
                    <th class="px-4 py-3 fw-normal">調度路線</th>
                    <th class="px-4 py-3 fw-normal">狀態</th>
                    <th class="px-4 py-3 fw-normal text-end">預計時間</th>
                  </tr>
                </thead>
                <tbody class="border-top-0">
                  <tr v-for="task in activeTasks" :key="task.dispatchId">
                    <td class="px-4 py-3">
                      <div class="fw-bold text-primary">{{ task.vehicle?.plateNo || '-' }}</div>
                      <div class="small text-muted">{{ task.vehicle?.carModel?.brand }} {{ task.vehicle?.carModel?.modelName }}</div>
                    </td>
                    <td class="px-4 py-3 text-muted">
                      {{ task.fromLocation?.locationName || '-' }} → {{ task.toLocation?.locationName || '-' }}
                    </td>
                    <td class="px-4 py-3">
                      <span class="badge rounded-pill px-2 py-1" :class="statusMap[task.status?.dbCode]?.badgeClass || 'bg-secondary'" style="font-size: 0.65rem;">
                        {{ statusMap[task.status?.dbCode]?.text || '未知' }}
                      </span>
                    </td>
                    <td class="px-4 py-3 fw-bold text-end">{{ formatDateTime(task.scheduledStartTime) }}</td>
                  </tr>
                  <tr v-if="activeTasks.length === 0">
                    <td colspan="4" class="text-center py-4 text-muted">
                      <i class="fa-solid fa-clipboard-check fs-3 mb-2 opacity-25 d-block"></i>
                      目前沒有七天內的進行中調度任務
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 各據點飽和度 -->
        <div class="col-lg-4">
          <div class="card border border-light shadow-sm rounded-4 h-100 d-flex flex-column">
            <div class="card-header bg-white border-bottom-0 pt-3 pb-3 px-4 rounded-top-4 d-flex justify-content-between align-items-center">
              <h6 class="fw-bold mb-0 d-flex align-items-center" style="color: var(--color-text-primary);">
                <i class="fa-solid fa-chart-pie me-2" style="color: #dba772;"></i>各據點飽和度
              </h6>
              <a href="#" @click.prevent="router.push('/vehicle')" class="text-primary text-decoration-none small fw-bold d-flex align-items-center">
                查看全部車輛 <i class="fa-solid fa-chevron-right ms-1 small"></i>
              </a>
            </div>
            
            <div class="card-body p-4 flex-grow-1 d-flex flex-column gap-4">
              <div v-if="locationSaturation.length === 0" class="text-center py-4 text-muted">
                <i class="fa-solid fa-chart-pie fs-3 mb-2 opacity-25 d-block"></i>
                暫無據點資料
              </div>
              <div v-for="loc in locationSaturation" :key="loc.locationId" class="d-flex align-items-center gap-3">
                <!-- 圓餅圖 (SVG donut) -->
                <div class="vehicle-donut-wrap">
                  <svg viewBox="0 0 36 36" class="vehicle-donut-chart">
                    <!-- 背景圓 -->
                    <circle cx="18" cy="18" r="14" fill="none" stroke="#fce7d5" stroke-width="4" />
                    <!-- 進度圓 -->
                    <circle cx="18" cy="18" r="14"
                      fill="none"
                      stroke="#dba772"
                      stroke-width="4"
                      stroke-dasharray="88"
                      :stroke-dashoffset="88 - (88 * loc.percentage / 100)"
                      stroke-linecap="round"
                      transform="rotate(-90 18 18)"
                    />
                  </svg>
                  <div class="vehicle-donut-text">
                    <strong>{{ loc.currentCount }}</strong>
                    <small>/{{ loc.capacity }}</small>
                  </div>
                </div>
                <!-- 據點名稱 + 百分比 -->
                <div class="flex-grow-1">
                  <div class="fw-bold small" style="color: var(--color-text-primary);">{{ loc.locationName }}</div>
                  <div class="small text-muted">飽和度 {{ loc.percentage }}%</div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>

      <!-- ========== 地圖區塊 ========== -->
      <div class="card border-0 shadow-sm rounded-4 overflow-hidden position-relative vehicle-map-placeholder" style="height: 300px;">
        <div class="position-absolute w-100 h-100 opacity-75 vehicle-map-bg"></div>
        
        <div class="position-relative z-1 d-flex h-100 align-items-center justify-content-center">
          <div class="bg-white bg-opacity-75 vehicle-backdrop-blur p-4 rounded-4 shadow-sm text-center border border-white" style="max-width: 400px;">
            <i class="fa-solid fa-map-location-dot fs-1 text-primary mb-3"></i>
            <h5 class="fw-bold" style="color: var(--color-text-primary);">即時車隊定位</h5>
            <p class="text-muted small mb-3">即時追蹤 {{ dispatchLogs.length }} 輛車隊車輛的位置與路線狀態。</p>
            <button class="btn btn-primary fw-bold w-100">開啟調度地圖</button>
          </div>
        </div>
      </div>

    </template>

  </div>

  <!-- FAB 浮動按鈕 -->
  <button class="btn btn-primary rounded-circle shadow-lg position-fixed d-flex align-items-center justify-content-center vehicle-fab-btn" 
          style="bottom: 30px; right: 30px; width: 60px; height: 60px; z-index: 1050;"
          @click="router.push('/dispatch/task')">
    <i class="fa-solid fa-clipboard-check fs-4"></i>
    <div class="vehicle-fab-tooltip bg-dark text-white small px-3 py-1 rounded-3 position-absolute">
      建立新調度單
    </div>
  </button>

</template>

<style scoped>
/* ============================================================
   DispatchDashboardView — OneRent Design System
   ============================================================ */

/* ── 統計卡片（參考圖一風格） ── */
.vehicle-stat-card {
  border-radius: var(--radius-xl);
  padding: var(--space-5) var(--space-6);
  cursor: pointer;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal), background-color var(--transition-normal);
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: linear-gradient(135deg, #0150ad 0%, #daebfc 100%);
  color: #fff;
}

.vehicle-stat-card--pending {
  background: linear-gradient(135deg, #fce7d5 0%, #fff 100%);
  color: var(--color-text-primary);
}
.vehicle-stat-card--active {
  background: linear-gradient(135deg, #daebfc 0%, #fff 100%);
  color: var(--color-text-primary);
  /* border: 1px solid var(--color-border); */
}
.vehicle-stat-card--finished {
  background: linear-gradient(135deg, #d7f5c7 0%, #fff 100%);
  color: var(--color-text-primary);
  /* border: 1px solid var(--color-border); */
}

.vehicle-stat-card--pending:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
  background: #fce7d5 !important;
  color: var(--color-text-primary) !important;
}
.vehicle-stat-card--active:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
  background: #daebfc !important;
  color: var(--color-text-primary) !important;
}
.vehicle-stat-card--finished:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
  background: #d7f5c7 !important;
  color: var(--color-text-primary) !important;
}
.vehicle-stat-card:hover .vehicle-stat-label,
.vehicle-stat-card:hover .vehicle-stat-number,
.vehicle-stat-card:hover .vehicle-stat-footer,
.vehicle-stat-card:hover .vehicle-stat-icon-wrap i {
  color: var(--color-text-primary) !important;
}

.vehicle-stat-label {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  opacity: 0.85;
}

.vehicle-stat-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  border: 1px solid currentColor;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.5;
  font-size: var(--text-xs);
}

.vehicle-stat-number {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  margin: var(--space-3) 0 var(--space-2);
  line-height: 1;
}

.vehicle-stat-footer {
  font-size: var(--text-xs);
  opacity: 0.7;
  font-weight: var(--font-medium);
}

/* 深色卡片文字 */
.vehicle-stat-card--active .vehicle-stat-label,
.vehicle-stat-card--finished .vehicle-stat-label {
  color: var(--color-text-secondary);
}
.vehicle-stat-card--active .vehicle-stat-footer,
.vehicle-stat-card--finished .vehicle-stat-footer {
  color: var(--color-text-muted);
}

/* ── 圓餅圖 ── */
.vehicle-donut-wrap {
  position: relative;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
}

.vehicle-donut-chart {
  width: 100%;
  height: 100%;
}

.vehicle-donut-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  line-height: 1.1;
}

.vehicle-donut-text strong {
  display: block;
  font-size: var(--text-base);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.vehicle-donut-text small {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

/* ── 地圖 ── */
.vehicle-map-placeholder {
  background-color: var(--color-primary-light);
}
.vehicle-map-bg {
  background-image: radial-gradient(var(--color-primary) 1px, transparent 1px);
  background-size: 20px 20px;
  transition: transform 0.5s ease;
}
.vehicle-map-placeholder:hover .vehicle-map-bg {
  transform: scale(1.05);
}

.vehicle-backdrop-blur {
  backdrop-filter: blur(10px);
}

/* ── FAB ── */
.vehicle-fab-btn {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.vehicle-fab-btn:hover {
  transform: scale(1.1);
  box-shadow: var(--shadow-lg);
}
.vehicle-fab-tooltip {
  right: 120%;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease, transform 0.3s ease;
  transform: translateX(10px);
}
.vehicle-fab-btn:hover .vehicle-fab-tooltip {
  opacity: 1;
  transform: translateX(0);
}
</style>