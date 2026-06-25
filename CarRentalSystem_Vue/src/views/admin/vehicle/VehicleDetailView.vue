<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { orderVehicleAPI } from '@/api/rentalorder/ordervehicleAPI';
import { rentalOrderAPI } from '@/api/rentalorder/rentalorderAPI';
import { dispatchAPI } from '@/api/vehicle/dispatchLogApi';
import VehicleFormModal from '@/components/admin/vehicle/VehicleFormModal.vue';

import BackButton from '@/components/common/BackButton.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { vehicleAPI } from '@/api/vehicle/vehicleApi';

const route = useRoute();
const router = useRouter();
const vehicleId = route.params.id; // 從網址取得車輛ID

// --- 響應式狀態 ---
const isLoading = ref(true);
const vehicle = ref({});
const scheduleList = ref([]); // 合併後的排程時間軸資料
const formModalRef = ref(null);

// --- 車輛＆訂單狀態對應表 ---
// 車
const vehicleStatusMap = {
    'AVAILABLE': { label: '場內(可接單)', variant:'success' },
    'RENTING': { label: '出租中', variant: 'primary' },
    'DISPATCHING': { label: '調度中', variant: 'warning' },
    'MAINTAINING': { label: '維修中', variant: 'danger' },
    'CLEANING': { label: '場內(整理中)', variant: 'secondary' },
    'RETIRED': { label: '退役待處置', variant: 'dark' },
    'SHUTTLING': { label: '可用', variant:'success' },
};
// 各業務排程狀態對應表
const rentalStatusMap = {
    '已預約': { text: '已預約', badge: 'bg-info text-white' },
    '已取車': { text: '已取車', badge: 'bg-primary text-white' },
    '已歸還(待檢查)': { text: '已歸還', badge: 'bg-warning text-dark' },
    '逾期未還': { text: '逾期未還', badge: 'bg-danger text-white' },
    '已結案': { text: '已結案', badge: 'bg-success text-white' },
    '已取消': { text: '已取消', badge: 'bg-secondary text-white' },
};

const transferStatusMap = {
    '已預訂': { text: '已預約', badge: 'bg-info text-white' },
    '待處理': { text: '待處理', badge: 'bg-warning text-white' },
    '處理中': { text: '處理中', badge: 'bg-primary text-white' },
    '已完成': { text: '已完成', badge: 'bg-success text-white' },
    '已取消': { text: '已取消', badge: 'bg-secondary text-white' },
};

const dispatchStatusMap = {
    'PENDING': { text: '待調度', badge: 'vehicle-badge-almond' },       // 🌟 換成我們剛寫好的 CSS Class
    '待調度': { text: '待調度', badge: 'vehicle-badge-almond' },
    'IN_PROCESS': { text: '調度中', badge: 'bg-warning text-dark' },  // 🌟 換成我們剛寫好的 CSS Class
    '調度中': { text: '調度中', badge: 'bg-warning text-dark' },
    'FINISHED': { text: '已完成', badge: 'bg-success bg-opacity-10 text-success border border-success' },
    '已完成': { text: '已完成', badge: 'bg-success bg-opacity-10 text-success border border-success' },
    'CANCEL': { text: '已取消', badge: 'bg-light text-muted border' },
    '已取消': { text: '已取消', badge: 'bg-light text-muted border' }
};

// --- 初始化資料 ---
const fetchVehicleData = async () => {
    isLoading.value = true;
    try {
        // 先抓車輛基本資料
        const vehRes = await vehicleAPI.get(vehicleId);
        vehicle.value = vehRes.data;

        // 並發抓取該車的「租車單」、「專車單」、「調度單」
        // (使用Promise.allAllSettled避免其中一支API壞掉導致整個畫面死掉)
        const [rentalRes, transRes, dispatchRes] = await Promise.allSettled([
            rentalOrderAPI.getAll(), // 沒有searchByVehicle()只好全撈
            vehicleAPI.getAllTransferOrder(),
            dispatchAPI.searchByVehicle(vehicleId),
        ]);

        let rawEvents = [];

        // 1. 租車訂單
        if (rentalRes.status === 'fulfilled' && rentalRes.value.data) {
            // 先從全部清單中，過濾出該台車的租車訂單
            const excludedStatuses = ['已結案', '已取消'];
            const myRentalOrders = rentalRes.value.data.filter( r => r.vehicle?.vehicleId == vehicleId && !excludedStatuses.includes(r.orderStatus));
            const rentals = myRentalOrders.map(order => {
                const mapObj = rentalStatusMap[order.orderStatus] || { text: order.orderStatus || '未知', badge: 'bg-secondary text-white' };
                return {
                    id: `R-${order.orderId}`,
                    typeLabel: '租車訂單',
                    title: `(#${order.orderId})`,
                    startTime: new Date(order.pickupTime),
                    endTime: new Date(order.returnTime),
                    startLocationStr: order.pickupLocation.locationName,
                    endLocationStr: order.returnLocation.locationName,
                    badgeText: mapObj.text,
                    badgeClass: mapObj.badge,
                    dotColor: 'text-primary' // 藍色點點
                };
            });
            rawEvents.push(...rentals);
        }

        // 2. 專車訂單
        if (transRes.status === 'fulfilled' && transRes.value.data) {
            // 先從全部清單中，過濾出該台車的租車訂單
            const myTransOrders = transRes.value.data.filter( t => t.vehicleId == vehicleId && t.status != '已取消' && t.status != '已完成');
            const transOrders = myTransOrders.map(order => {
                const mapObj = transferStatusMap[order.status] || { text: order.status || '未知', badge: 'bg-secondary text-white' };
                return {
                    id: `R-${order.transferId}`,
                    typeLabel: '專車訂單',
                    title: `(#${order.transferId})`,
                    startTime: new Date(order.scheduledPickupTime),
                    endTime: new Date(order.scheduledDropoffTime),
                    startLocationStr: order.pickupLocation ,
                    endLocationStr: order.dropoffLocation,
                    statusStr: order.status, 
                    badgeText: mapObj.text,
                    badgeClass: mapObj.badge,
                    dotColor: 'text-success' // 綠色點點
                }
            });
            rawEvents.push(...transOrders);
        }

        // 3. 車輛調度單
        if (dispatchRes.status === 'fulfilled' && dispatchRes.value.data) {
            const myDispatchLogs = dispatchRes.value.data.filter( d => d.status.dbCode != 'FINISHED' && d.status.dbCode != 'CANCEL');
            const disaptches = myDispatchLogs.map(dispatch => {
                const statusKey = dispatch.status?.dbCode || dispatch.status;
                const mapObj = dispatchStatusMap[statusKey] || { text: statusKey || '未知', badge: 'bg-secondary text-white' };
                return {
                    id: `D-${dispatch.dispatchId}`,
                    typeLabel: '調度單',
                    title: `(#${dispatch.dispatchId})`,
                    startTime: new Date(dispatch.scheduledStartTime),
                    endTime: new Date(dispatch.scheduledStartTime).getTime() + 2 * 60 * 60 * 1000,
                    startLocationStr: dispatch.fromLocation.locationName,
                    endLocationStr: dispatch.toLocation.locationName,
                    statusStr: dispatch.status?.dbCode || dispatch.status, 
                    badgeText: mapObj.text,
                    badgeClass: mapObj.badge,
                    dotColor: 'text-warning' // 黃色點點
                }
            });
            rawEvents.push(...disaptches);
        }

        // --- 排序(依仗時間從近到遠排) ---
        rawEvents.sort((a, b) => a.startTime.getTime() - b.startTime.getTime());

        // 將結果存入響應式變數
        scheduleList.value = rawEvents;

    } catch (error) {
        console.error("載入車輛詳情失敗：", error);
    } finally {
        isLoading.value = false;
    }
};

// --- 工具方法 ---
const formatDate = (dateString) => {
    if (!dateString) return '無資料';
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};

const formatTimeRange = (start, end) => {
    const formatOptions = { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false };
    const sStr = start.toLocaleString('zh-TW', formatOptions);
    const eStr = end.toLocaleString('zh-TW', { hour: '2-digit', minute: '2-digit', hour12: false });
    return `${sStr} - ${eStr}`;
};

const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return '無資料';
  const date = new Date(dateTimeString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day},  ${hour}:${minute}`;
};

const refreshVehicleData = () => {
    fetchVehicleData();
};

// --- 維修驗車提醒（依真實資料即時計算）---
const maintenanceAlerts = computed(() => {
  const alerts = [];
  const v = vehicle.value;
  if (!v || !v.vehicleId) return alerts;

  // ── 1. 驗車日提醒 ──
  if (v.inspectionDate) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const inspDate = new Date(v.inspectionDate);
    inspDate.setHours(0, 0, 0, 0);
    const diffDays = Math.ceil((inspDate - today) / (1000 * 60 * 60 * 24));

    if (diffDays <= 0) {
      // 已逾期
      alerts.push({
        level: 'danger',
        icon: 'fa-solid fa-circle-exclamation',
        title: `驗車日已逾期 ${Math.abs(diffDays)} 天`,
        desc: `法定驗車日為 ${formatDate(v.inspectionDate)}，請立即安排驗車。`,
      });
    } else if (diffDays <= 15) {
      // 紅色：15天內
      alerts.push({
        level: 'danger',
        icon: 'fa-solid fa-circle-exclamation',
        title: `距法定驗車日僅剩 ${diffDays} 天`,
        desc: `驗車截止日 ${formatDate(v.inspectionDate)}，請盡速安排。`,
      });
    } else if (diffDays <= 30) {
      // 黃色：16~30天
      alerts.push({
        level: 'warning',
        icon: 'fa-solid fa-triangle-exclamation',
        title: `距法定驗車日尚有 ${diffDays} 天`,
        desc: `驗車截止日 ${formatDate(v.inspectionDate)}，建議提前安排。`,
      });
    }
  }

  // ── 2. 維修里程提醒 ──
  const currentMileage = Number(v.mileage) || 0;
  const nextMaintMileage = Number(v.nextMaintainenceMileage) || 0;
  if (nextMaintMileage > 0) {
    const gap = nextMaintMileage - currentMileage;

    if (gap <= 0) {
      // 已超標
      alerts.push({
        level: 'danger',
        icon: 'fa-solid fa-oil-can',
        title: `保養里程已超標 ${Math.abs(gap).toLocaleString()} 公里`,
        desc: `下次保養里程 ${nextMaintMileage.toLocaleString()} km，當前 ${currentMileage.toLocaleString()} km，請立即保養。`,
      });
    } else if (gap <= 2500) {
      // 紅色：5000公里內
      alerts.push({
        level: 'danger',
        icon: 'fa-solid fa-oil-can',
        title: `距下次保養僅剩 ${gap.toLocaleString()} 公里`,
        desc: `下次保養里程 ${nextMaintMileage.toLocaleString()} km，請盡速安排。`,
      });
    } else if (gap <= 5000) {
      // 黃色：5001~10000公里
      alerts.push({
        level: 'warning',
        icon: 'fa-solid fa-oil-can',
        title: `距下次保養尚有 ${gap.toLocaleString()} 公里`,
        desc: `下次保養里程 ${nextMaintMileage.toLocaleString()} km，建議提前規劃。`,
      });
    }
  }

  return alerts;
});

// --- 初始化 ---
onMounted(() => {
    if (vehicleId) {
        fetchVehicleData();
    }
})
</script>

<template>
  <div class="container-fluid py-4 bg-light min-vh-100">
    
    <LoadingSpinner :isLoading="isLoading" overlay />

    <div class="d-flex align-items-center mb-4 text-muted">
      <span class="cursor-pointer text-primary v-hover-link fw-bold" role="button" @click="router.push('/dispatch/dashboard')">
        <font-awesome-icon icon="fa-solid fa-headset" style="width: 1.25rem; font-size: 0.9rem;"/> 車輛調度中心
      </span>
      <i class="fa-solid fa-chevron-right mx-2 small"></i>
      <span class="cursor-pointer text-primary v-hover-link fw-bold" role="button" @click="router.push('/vehicle')">
        車輛管理
      </span>
      <i class="fa-solid fa-chevron-right mx-2 small"></i>
      <span class="text-dark fw-bold">車輛詳情</span>
    </div>

    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
      <div>
        <h2 class="fw-bold mb-1">車輛詳情</h2>
        <!-- <p class="text-muted mb-0">Manage vehicle specifications, availability, and fleet categories.</p> -->
      </div>
    </div>

    <div v-if="!isLoading && vehicle.vehicleId">
      
      <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
        <div class="row g-0">
          
            <div class="col-md-5 bg-white p-4 d-flex flex-column justify-content-center border-end">
                <div class="text-center mb-4 bg-light rounded-4 py-4">
                    <img v-if="vehicle.carModel?.vehicleImgUrl" :src="vehicle.carModel?.vehicleImgUrl" class="img-fluid" style="max-height: 220px; object-fit: contain;" alt="車輛照片">
                    <i v-else class="fa-solid fa-car-side fa-6x text-muted opacity-25"></i>
                </div>
                
                <h2 class="fw-bold text-dark mb-1">{{ vehicle.plateNo }}</h2>
                <h5 class="text-muted mb-4">{{ vehicle.carModel?.brand }} {{ vehicle.carModel?.modelName }} ({{ new Date(vehicle.manufactureDate).getFullYear() || '年分未知' }})</h5>
                
                <div class="d-flex align-items-center flex-wrap gap-2">
                    <StatusBadge :status="vehicle.status?.dbCode || vehicle.status" :map="vehicleStatusMap" />
                    <span class="badge bg-light text-dark border px-2 py-1"><i class="fa-solid fa-location-dot me-1 text-secondary"></i>{{ vehicle.location?.locationName || '未知據點' }}</span>
                    <span class="badge bg-light text-dark border px-2 py-1"><i class="fa-solid fa-tag me-1 text-secondary"></i>{{ vehicle.carModel?.vehicleType || '未知車型' }}</span>
                    <span class="badge bg-light text-dark border px-2 py-1"><i class="fa-solid fa-gas-pump me-1 text-secondary"></i>{{ vehicle.carModel?.fuelType || '未知動力' }}</span>
                    <span class="badge bg-light text-dark border px-2 py-1"><i class="fa-solid fa-gear me-1 text-secondary"></i>{{ vehicle.carModel?.transmission || '未知變速箱' }}</span>
                </div>
            </div>

            <div class="col-md-7 bg-white p-4 p-lg-5 position-relative d-flex flex-column">

                <button @click="formModalRef.openModal(vehicle)" class="btn btn-link text-decoration-none fw-bold position-absolute top-0 end-0 mt-4 me-4">
                    <i class="fa-solid fa-pen me-1"></i>編輯資料
                </button>

                <h5 class="fw-bold mb-4">車輛詳細資訊</h5>

                <div class="mb-4">
                    <h6 class="text-secondary fw-bold mb-2"><i class="fa-solid fa-cube me-2"></i>基本規格</h6>
                    <div class="border rounded-3 overflow-hidden">
                    <div class="row g-0">
                        <div class="col-6 col-sm-3 border-end p-3 bg-light bg-opacity-50 text-center">
                        <div class="text-muted small fw-bold mb-1">座位數</div>
                        <div class="fw-bold text-dark">{{ vehicle.carModel?.seats || '--' }} 人座</div>
                        </div>
                        <div class="col-6 col-sm-3 border-end p-3 bg-light bg-opacity-50 text-center">
                        <div class="text-muted small fw-bold mb-1">行李數</div>
                        <div class="fw-bold text-dark">{{ vehicle.carModel?.luggageCapacity || '--' }} 大</div>
                        </div>
                        <div class="col-6 col-sm-3 border-end p-3 bg-light bg-opacity-50 text-center">
                        <div class="text-muted small fw-bold mb-1">排氣量</div>
                        <div class="fw-bold text-dark">{{ vehicle.carModel?.displacement || '--' }} cc</div>
                        </div>
                        <div class="col-6 col-sm-3 p-3 bg-light bg-opacity-50 text-center">
                        <div class="text-muted small fw-bold mb-1">迴轉半徑</div>
                        <div class="fw-bold text-dark">{{ vehicle.carModel?.turningRadius || '--' }} m</div>
                        </div>
                    </div>
                    </div>
                </div>

                <div class="row g-4 mb-4">
                    <div class="col-md-6">
                        <h6 class="text-secondary fw-bold mb-2"><i class="fa-solid fa-id-card me-2"></i>車籍與外觀</h6>
                        <div class="px-3 py-2 bg-light rounded-3 h-100 d-flex flex-column justify-content-center">
                            
                            <div class="d-flex justify-content-between py-1 border-bottom border-secondary border-opacity-10">
                            <span class="text-muted small fw-bold">車色</span>
                            <span class="fw-bold text-dark">{{ vehicle.color || '--' }}</span>
                            </div>
                            
                            <div class="d-flex justify-content-between py-1 border-bottom border-secondary border-opacity-10">
                            <span class="text-muted small fw-bold">出廠日期</span>
                            <span class="fw-bold text-dark">{{ formatDate(vehicle.manufactureDate) }}</span>
                            </div>
                            
                            <div class="d-flex justify-content-between py-1">
                            <span class="text-muted small fw-bold">發照日期</span>
                            <span class="fw-bold text-dark">{{ formatDate(vehicle.issuedDate) }}</span>
                            </div>

                        </div>
                    </div>

                    <div class="col-md-6">
                    <h6 class="text-secondary fw-bold mb-2"><i class="fa-solid fa-wrench me-2"></i>里程與保養</h6>
                        <div class="px-3 py-2 bg-light rounded-3 h-100 d-flex flex-column justify-content-center">
                            
                            <div class="d-flex justify-content-between py-1 border-bottom border-secondary border-opacity-10">
                            <span class="text-muted small fw-bold">當前里程</span>
                            <span class="fw-bold text-dark">{{ Number(vehicle.mileage).toLocaleString() || 0 }} km</span>
                            </div>
                            
                            <div class="d-flex justify-content-between py-1 border-bottom border-secondary border-opacity-10">
                            <span class="text-muted small fw-bold">下次保養</span>
                            <span class="fw-bold text-dark">{{ Number(vehicle.nextMaintainenceMileage).toLocaleString() || '--' }} km</span>
                            </div>
                            
                            <div class="d-flex justify-content-between py-1">
                            <span class="text-muted small fw-bold">預計驗車</span>
                            <span class="fw-bold text-danger">{{ formatDate(vehicle.inspectionDate) }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="mb-auto pt-4">
                    <h6 class="text-secondary fw-bold mb-2"><i class="fa-solid fa-clipboard-list me-2"></i>車況記事</h6>
                    <div class="p-3 border rounded-3 bg-light bg-opacity-50">
                    <p class="text-dark mb-0 small" style="line-height: 1.6;">{{ vehicle.description || '無特別記事' }}</p>
                    </div>
                </div>

                <div class="text-end mt-4 pt-3 border-top">
                    <span class="text-muted small fw-bold">資料建立時間：</span>
                    <span class="text-muted fw-bold small">{{ formatDateTime(vehicle.createdTime) }}</span>
                </div>

            </div>

        </div>
      </div>

      <div class="row g-4">
        
        <div class="col-lg-7">
          <div class="card border-0 shadow-sm rounded-4 h-100 p-4">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h5 class="fw-bold mb-0">目前排程</h5>
              <!-- <button class="btn btn-sm btn-outline-primary fw-bold"><i class="fa-regular fa-calendar me-1"></i>查看完整日曆</button> -->
            </div>

            <EmptyState v-if="scheduleList.length === 0" icon="calendar-xmark" message="目前沒有任何排程紀錄" />

            <div class="vehicle-timeline-container px-3 mt-3" v-else>
              
              <div v-for="(event, index) in scheduleList" :key="event.id" 
                   class="position-relative pb-4"
                   :class="{'border-start border-2 border-light': index !== scheduleList.length - 1}">
                
                <div class="position-absolute top-0 start-0 translate-middle rounded-circle bg-white" style="width: 14px; height: 14px; border: 3px solid currentColor;" :class="event.dotColor"></div>
                
                <div class="card border ms-4 shadow-sm bg-light bg-opacity-50">
                  <div class="card-body p-3">
                    
                    <div class="d-flex justify-content-between align-items-start mb-2">
                      <div class="small text-muted fw-bold">
                        <i class="fa-regular fa-clock me-1"></i> {{ formatDateTime(event.startTime) }} - {{formatDateTime (event.endTime) }}
                      </div>
                      <span class="badge" :class="event.badgeClass">{{ event.badgeText }}</span>
                    </div>

                    <h6 class="fw-bold mb-2 text-dark">
                      {{ event.typeLabel }} <span class="text-secondary fw-normal">{{ event.title }}</span>
                    </h6>

                    <div class="small text-muted">
                      <i class="fa-solid fa-location-dot me-1" style="color: #dba772;"></i>               
                      {{ event.startLocationStr }}
                      <i class="fa-solid fa-right-long" style="color: #dba772;"></i>
                      {{ event.endLocationStr }}
                    </div>

                  </div>
                </div>
              </div>

            </div>
          </div>
        </div>

        <div class="col-lg-5">
          <div class="card border-0 shadow-sm rounded-4 h-100 p-4">
            <h5 class="fw-bold mb-4">維修驗車提醒</h5>
            <!-- 無提醒 -->
            <div v-if="maintenanceAlerts.length === 0" class="text-center py-5 text-muted">
              <i class="fa-solid fa-clipboard-check fs-1 mb-3 opacity-25 d-block"></i>
              <div class="fw-bold">暫無需安排事項</div>
              <div class="small">驗車與保養里程皆在安全範圍內。</div>
            </div>

            <!-- 動態提醒列表 -->
            <div v-for="(alert, idx) in maintenanceAlerts" :key="idx"
                 class="alert d-flex align-items-start mb-3"
                 :class="alert.level === 'danger'
                   ? 'alert-danger border-danger bg-danger bg-opacity-10'
                   : 'alert-warning border-warning bg-warning bg-opacity-10'">
              <i :class="[alert.icon, alert.level === 'danger' ? 'text-danger' : 'text-warning']" class="me-3 fs-4 mt-1"></i>
              <div>
                <div class="fw-bold" :class="alert.level === 'danger' ? 'text-danger' : ''">
                  {{ alert.title }}
                </div>
                <div class="small text-muted">{{ alert.desc }}</div>
              </div>
            </div>
          </div>
        </div>

      </div>

    </div>
  </div>
  <VehicleFormModal ref="formModalRef" @saved="refreshVehicleData" /> 
</template>

<style scoped>
/* 補足 Bootstrap 無法簡單達成的微調樣式 */
.vehicle-timeline-container {
  /* 讓點點對齊得更好看 */
  margin-left: 0.5rem;
}
/* ======== 專案專屬主題色 Badge ======== */
.vehicle-badge-almond {
    background-color: #FCE7D5 !important;
    color: #DBA772 !important; 
    border: 1px solid #DBA772; /* 加上同色系邊框會更有質感 */
}

.vehicle-badge-sapphire {
    background-color: #0150AD !important;
    color: #FFFFFF !important;
}
/* ── 車輛狀態 Badge（淺底配深字）── */
/* 清理中：沙色 */
.vehicle-badge-cleaning {
  background-color: var(--color-accent-light);
  color: var(--color-sandy-800);
}
/* 可接單：綠 */
.vehicle-badge-available {
  background-color: var(--color-success-bg);
  color: var(--color-success);
}
/* 出租中：藍 */
.vehicle-badge-renting {
  background-color: var(--color-primary-light);
  color: var(--color-primary);
}
/* 維修中：紅 */
.vehicle-badge-maintaining {
  background-color: var(--color-danger-bg);
  color: var(--color-danger);
}
/* 調度中：黃 */
.vehicle-badge-dispatching {
  background-color: var(--color-warning-bg);
  color: var(--color-amber-800);
}
/* 專車接送中：青綠 */
.vehicle-badge-shuttling {
  background-color: var(--color-teal-bg);
  color: var(--color-teal);
}
/* 退役待處置：灰底深灰字 */
.vehicle-badge-retired {
  background-color: var(--color-gray-200);
  color: var(--color-gray-700);
}
</style>