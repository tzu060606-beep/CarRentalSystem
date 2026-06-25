<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import * as bootstrap from 'bootstrap';
import { locationAPI } from '@/api/vehicle/locationApi';
import { crossLocationFeeAPI } from '@/api/vehicle/crossLocationFeeApi';
import AlertToast from '@/components/common/AlertToast.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';

const router = useRouter();
const toastRef = ref(null);
const confirmDialog = ref({ isVisible: false, message: '', action: null });

const showConfirm = (message, action) => {
  confirmDialog.value = { isVisible: true, message, action };
};
const handleConfirm = async () => {
  confirmDialog.value.isVisible = false;
  if (confirmDialog.value.action) await confirmDialog.value.action();
};

// ==========================================
// 1. 共用狀態與 UI 控制
// ==========================================
const activeTab = ref('directory');
const searchQuery = ref('');
const isLoading = ref(false);
let locationModalInstance = null;

// ==========================================
// 2. 據點目錄資料 (串接後端)
// ==========================================
const locationStatusMap = {
  'OPERATING': { text: '營運中', badge: 'bg-success bg-opacity-10 text-success border-success' },
  'REST': { text: '暫停營業', badge: 'bg-warning bg-opacity-10 text-warning border-warning' },
  'CANCEL': { text: '撤銷據點', badge: 'bg-light text-muted border-secondary' }
};

const locations = ref([]);
const allFees = ref([]); // 從後端取得的 CrossLocationFee 列表

// 計算容量飽和度顏色
const getCapacityColor = (current, max) => {
  if (!max || max === 0) return 'bg-secondary';
  const ratio = current / max;
  if (ratio >= 0.9) return 'bg-danger';
  if (ratio >= 0.7) return 'bg-warning';
  return 'bg-primary';
};

// 取得據點狀態的 dbCode
const getStatusCode = (loc) => {
  if (!loc.locationStatus) return 'OPERATING';
  return loc.locationStatus.dbCode || loc.locationStatus;
};

// 據點列表過濾器
const filteredLocations = computed(() => {
  if (!searchQuery.value) return locations.value;
  const keyword = searchQuery.value.toLowerCase();
  return locations.value.filter(loc =>
    loc.locationName.toLowerCase().includes(keyword) ||
    String(loc.locationId).includes(keyword)
  );
});

// 快速切換狀態
const changeLocationStatus = async (loc, newStatus) => {
  const statusText = locationStatusMap[newStatus]?.text || newStatus;
  // if (confirm(`確定要將「${loc.locationName}」狀態更改為「${statusText}」嗎？`)) {
  showConfirm(`確定要將「${loc.locationName}」狀態更改為「${statusText}」嗎？`, async () => {
    try {
      const payload = { ...loc, locationStatus: newStatus };
      await locationAPI.edit(payload, loc.locationId);
      await loadLocations();
    } catch (e) {
      console.error('狀態更新失敗', e);
      // alert('狀態更新失敗：' + (e.response?.data || e.message));
      toastRef.value?.show('狀態更新失敗：' + (e.response?.data || e.message), 'danger');
    }
  });
};

// ==========================================
// 3. 據點編輯/新增 Modal 控制
// ==========================================
const isEditMode = ref(false);
const editFormData = ref({ locationId: '', locationName: '', address: '', phone: '', parkingCapacity: 20, locationStatus: 'OPERATING' });

const openLocationModal = (loc = null) => {
  if (loc) {
    isEditMode.value = true;
    editFormData.value = {
      locationId: loc.locationId,
      locationName: loc.locationName,
      address: loc.address,
      phone: loc.phone,
      parkingCapacity: loc.parkingCapacity,
      locationStatus: getStatusCode(loc)
    };
  } else {
    isEditMode.value = false;
    editFormData.value = { locationId: '', locationName: '', address: '', phone: '', parkingCapacity: 20, locationStatus: 'OPERATING' };
  }
  locationModalInstance.show();
};

// 一鍵填入測試資料
const fillTestData = () => {
  editFormData.value.locationName = '新竹高鐵站';
  editFormData.value.address = '新竹縣竹北市高鐵七路6號';
  editFormData.value.phone = '03-550-1234';
  editFormData.value.parkingCapacity = 50;
  editFormData.value.locationStatus = 'OPERATING';
};

const saveLocation = async () => {
  try {
    if (isEditMode.value) {
      await locationAPI.edit(editFormData.value, editFormData.value.locationId);
    } else {
      await locationAPI.insert(editFormData.value);
    }
    // alert('儲存成功！');
    toastRef.value?.show('儲存成功！', 'success');
    locationModalInstance.hide();
    await loadLocations();
  } catch (e) {
    console.error('儲存失敗', e);
    // alert('儲存失敗：' + (e.response?.data || e.message));
    toastRef.value?.show('儲存失敗：' + (e.response?.data || e.message), 'danger');
  }
};

// ==========================================
// 4. 費率矩陣資料與邏輯 (串接後端)
// ==========================================

// 從 allFees 建立費率查找 Map: fees[fromId][toId] = { feeId, extraFee }
const feesMap = computed(() => {
  const map = {};
  for (const fee of allFees.value) {
    // 後端回傳的 fromLocation / toLocation 可能是完整物件
    const fromId = Number(fee.fromLocation?.locationId ?? fee.fromLocationId);
    const toId = Number(fee.toLocation?.locationId ?? fee.toLocationId);
    if (!isNaN(fromId) && !isNaN(toId)) {
      if (!map[fromId]) map[fromId] = {};
      map[fromId][toId] = { feeId: fee.feeId, extraFee: Number(fee.extraFee) || 0 };
    }
  }
  return map;
});

// 統一用 Number() 查找，避免字串/數字型別不一致
const getFee = (fromId, toId) => feesMap.value[Number(fromId)]?.[Number(toId)]?.extraFee ?? 0;
const getFeeId = (fromId, toId) => feesMap.value[Number(fromId)]?.[Number(toId)]?.feeId ?? null;

const editingCell = ref({ origin: null, dest: null });
const editValue = ref(0);
const isSaving = ref(false);

const startEdit = (originId, destId, currentFee) => {
  if (originId === destId) return;
  // 如果已經在編輯中，先不切換
  if (isSaving.value) return;
  editingCell.value = { origin: originId, dest: destId };
  editValue.value = currentFee;
  // 等 DOM 更新後再自動聚焦
  nextTick(() => {
    const input = document.querySelector('.edit-input');
    if (input) input.focus();
  });
};

const saveEdit = async () => {
  if (isSaving.value) return;
  const { origin, dest } = editingCell.value;
  if (origin == null || dest == null) return;

  // 找到起點和迄點名稱
  const fromName = locations.value.find(l => l.locationId === origin)?.locationName || origin;
  const toName = locations.value.find(l => l.locationId === dest)?.locationName || dest;

  // 確認是否修改
  showConfirm(`確定要將「${fromName} → ${toName}」的費率修改為 $${editValue.value} 嗎？`, async () => {
    isSaving.value = true;
    const existingFeeId = getFeeId(origin, dest);
    console.log(`[DEBUG] origin=${origin}, dest=${dest}, existingFeeId=${existingFeeId}, feesMap=`, feesMap.value);

    const payload = {
      fromLocation: { locationId: origin },
      toLocation: { locationId: dest },
      extraFee: editValue.value,
      isDeleted: false,
    };

    try {
      if (existingFeeId) {
        // 帶上 feeId 確保後端 Hibernate 做 UPDATE
        payload.feeId = existingFeeId;
        await crossLocationFeeAPI.edit(payload, existingFeeId);
      } else {
        await crossLocationFeeAPI.insert(payload);
      }
      toastRef.value?.show('費率修改成功！', 'success');
      
      await loadFees();
    } catch (e) {
      console.error('費率儲存失敗', e);
      toastRef.value?.show('費率儲存失敗：' + (e.response?.data || e.message), 'danger');
    } finally {
      isSaving.value = false;
      editingCell.value = { origin: null, dest: null };
    }
  });
};

const cancelEdit = () => {
  if (!isSaving.value) {
    editingCell.value = { origin: null, dest: null };
  }
};

// 費率計算機
const calcOrigin = ref('');
const calcDest = ref('');
const calculatedFee = computed(() => {
  if (!calcOrigin.value || !calcDest.value) return null;
  if (calcOrigin.value === calcDest.value) return 0;
  return getFee(calcOrigin.value, calcDest.value);
});

// ==========================================
// 5. 資料載入
// ==========================================
const loadLocations = async () => {
  try {
    const res = await locationAPI.getAll();
    locations.value = res.data;
  } catch (e) {
    console.error('載入據點失敗', e);
  }
};

const loadFees = async () => {
  try {
    const res = await crossLocationFeeAPI.getAll();
    console.log('後端回傳的費率資料:', res.data);
    allFees.value = res.data;
  } catch (e) {
    console.error('載入費率失敗', e);
  }
};

onMounted(async () => {
  locationModalInstance = new bootstrap.Modal(document.getElementById('locationFormModal'));
  isLoading.value = true;
  await Promise.all([loadLocations(), loadFees()]);
  isLoading.value = false;
});
</script>

<template>
  <div class="container-fluid vehicle-fee-container min-vh-100 py-4 px-md-4 ">

    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&family=Manrope:wght@600;700;800&display=swap" rel="stylesheet">

    <div class="d-flex align-items-center mb-4 text-muted">
      <span class="cursor-pointer text-primary v-hover-link fw-bold" role="button" @click="router.push('/dispatch/dashboard')">
        <font-awesome-icon icon="fa-solid fa-headset" style="width: 1.25rem; font-size: 0.9rem;"/> 車輛調度中心
      </span>
      <i class="fa-solid fa-chevron-right mx-2 small"></i>
      <span class="text-dark fw-bold">據點與調度費率</span>
    </div>

    <div class="mb-4 d-flex justify-content-between align-items-end">
      <div>
        <h2 class=" fw-bold mb-1 text-primary vehicle-tracking-tight">據點與調度費率</h2>
        <p class="text-muted mb-0 small">詳情</p>
      </div>
      <button class="btn btn-primary fw-bold shadow-sm d-flex align-items-center rounded-3" @click="openLocationModal()">
        <i class="fa-solid fa-file-circle-plus me-2"></i>新增據點
      </button>
    </div>

    <ul class="nav nav-tabs ds-tabs mb-4 gap-3 fs-5">
      <li class="nav-item cursor-pointer" role="button" @click="activeTab = 'directory'">
        <a class="nav-link border-0  fw-bold pb-3" :class="activeTab === 'directory' ? 'active-tab' : 'text-muted'">
          <i class="fa-solid fa-building-flag me-2"></i>據點目錄
        </a>
      </li>
      <li class="nav-item cursor-pointer" role="button" @click="activeTab = 'matrix'">
        <a class="nav-link border-0  fw-bold pb-3" :class="activeTab === 'matrix' ? 'active-tab' : 'text-muted'">
          <i class="fa-solid fa-table-cells me-2"></i>異地還車費率矩陣
        </a>
      </li>
    </ul>

    <!-- 載入中 -->
    <div v-if="isLoading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <!-- 據點目錄 Tab -->
    <div v-if="!isLoading && activeTab === 'directory'">

      <div class="row mb-4">
        <div class="col-md-6 col-lg-4">
          <div class="input-group shadow-sm rounded-3 overflow-hidden">
            <span class="input-group-text bg-white border-0 text-muted"><i class="fa-solid fa-magnifying-glass"></i></span>
            <input v-model="searchQuery" type="text" class="vehicle-form-control border-0 ps-0" placeholder="搜尋據點名稱...">
          </div>
        </div>
      </div>

      <div class="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-4">
        <div class="col" v-for="loc in filteredLocations" :key="loc.locationId">
          <div class="card h-100 border-0 shadow-sm rounded-4 vehicle-hover-lift">
            <div class="card-body p-4 d-flex flex-column">

              <div class="d-flex justify-content-between align-items-start mb-3">
                <div>
                  <h4 class=" fw-bold text-dark mb-0">{{ loc.locationName }}</h4>
                  <span class="small text-muted vehicle-font-mono">ID: {{ loc.locationId }}</span>
                </div>

                <div class="dropdown">
                  <button class="badge rounded-pill px-3 py-2 border cursor-pointer bg-white d-flex align-items-center gap-2"
                          role="button"
                          :class="locationStatusMap[getStatusCode(loc)]?.badge"
                          data-bs-toggle="dropdown" aria-expanded="false">
                    <span class="vehicle-status-pill">{{ locationStatusMap[getStatusCode(loc)]?.text || '未知' }}</span>
                    <i class="fa-solid fa-caret-down opacity-50"></i>
                  </button>
                  <ul class="dropdown-menu shadow-sm border-0 rounded-3">
                    <li v-for="(info, key) in locationStatusMap" :key="key">
                      <button class="dropdown-item small fw-bold" @click="changeLocationStatus(loc, key)">
                        標記為 {{ info.text }}
                      </button>
                    </li>
                  </ul>
                </div>
              </div>

              <div class="mb-4">
                <div class="text-muted small mb-2 d-flex align-items-start gap-2">
                  <i class="fa-solid fa-location-dot mt-1 opacity-50"></i>
                  <span>{{ loc.address || '尚未設定地址' }}</span>
                </div>
                <div class="text-muted small d-flex align-items-center gap-2">
                  <i class="fa-solid fa-phone opacity-50"></i>
                  <span class="vehicle-font-mono">{{ loc.phone || '尚未設定電話' }}</span>
                </div>
              </div>

              <div class="mt-auto pt-3 border-top border-primary-subtle">
                <div class="d-flex justify-content-between align-items-center mb-1">
                  <span class="small fw-bold text-on-surface">停車容量</span>
                  <span class="small vehicle-font-mono fw-bold">{{ loc.parkingCapacity || 0 }} 格</span>
                </div>
              </div>

            </div>

            <div class="card-footer bg-transparent border-0 p-3 pt-0">
              <button @click="openLocationModal(loc)" class="btn btn-light w-100 fw-bold text-primary rounded-3 border vehicle-hover-primary">
                <i class="fa-solid fa-pen-to-square me-2"></i>編輯據點設定
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 費率矩陣 Tab -->
    <div class="row g-4" v-if="!isLoading && activeTab === 'matrix'">
      <div class="col-xl-8">
        <div class="card border-0 shadow-sm rounded-4 h-100 d-flex flex-column overflow-hidden">
          <div class="card-header bg-white border-bottom p-4 d-flex justify-content-between align-items-center">
            <div>
              <h5 class=" fw-bold mb-1 text-dark">異地還車費率表</h5>
              <p class="text-muted small mb-0 ">起點 (Y軸) 至 迄點 (X軸) 的附加費用。點擊儲存格可修改金額。</p>
            </div>
          </div>

          <div class="table-responsive flex-grow-1 bg-white">
            <table class="table table-bordered mb-0 table vehicle-fee-table">
              <thead class="bg-white sticky-top z-2">
                <tr>
                  <th class="p-3 text-muted small text-uppercase tracking-wider align-middle bg-white sticky-col z-3" style="min-width: 150px;">
                    起點 \ 迄點
                  </th>
                  <th v-for="dest in locations" :key="dest.locationId" class="p-3 fw-bold text-dark text-center align-middle bg-white" style="min-width: 120px;">
                    {{ dest.locationName }}
                  </th>
                </tr>
              </thead>
              <tbody class="">
                <tr v-for="origin in locations" :key="origin.locationId" class="bg-white">
                  <th class="p-3 fw-bold text-dark align-middle bg-white sticky-col">
                    {{ origin.locationName }}
                  </th>

                  <td v-for="dest in locations" :key="dest.locationId"
                      class="p-0 align-middle text-center position-relative fee-cell"
                      :class="{ 'bg-light text-muted opacity-50': origin.locationId === dest.locationId }"
                      @click="startEdit(origin.locationId, dest.locationId, getFee(origin.locationId, dest.locationId))">

                    <span v-if="origin.locationId === dest.locationId">--</span>

                    <div v-else-if="editingCell.origin === origin.locationId && editingCell.dest === dest.locationId" class="p-2 bg-white h-100">
                      <div class="input-group input-group-sm shadow-sm rounded-3 overflow-hidden">
                        <span class="input-group-text bg-white border-primary border-end-0 text-muted">$</span>
                        <input type="number"
                               v-model="editValue"
                               class="vehicle-form-control border-primary border-start-0 text-center fw-bold text-primary edit-input"
                               @keyup.enter="saveEdit"
                               @keyup.escape="cancelEdit">
                      </div>
                    </div>

                    <div v-else class="p-3 h-100 w-100 cursor-pointer cell-content" role="button">
                      <span class="fw-bold vehicle-font-mono text-dark">${{ getFee(origin.locationId, dest.locationId) }}</span>
                      <i class="fa-solid fa-pen text-primary edit-icon position-absolute top-0 end-0 m-2 opacity-0 vehicle-transition-all"></i>
                    </div>

                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="col-xl-4 d-flex flex-column gap-4">

        <div class="card border-0 shadow-sm rounded-4 bg-primary text-white">
          <div class="card-body p-4">
            <h5 class=" fw-bold mb-3 d-flex align-items-center">
              <i class="fa-solid fa-calculator me-2"></i>跨區費率速查
            </h5>
            <div class="row g-2 ">
              <div class="col-12">
                <label class="small text-white-50 fw-bold mb-1">取車據點</label>
                <select v-model="calcOrigin" class="form-select form-select-sm rounded-3 border-0 shadow-none">
                  <option value="" disabled>請選擇起點...</option>
                  <option v-for="loc in locations" :key="loc.locationId" :value="loc.locationId">{{ loc.locationName }}</option>
                </select>
              </div>
              <div class="col-12 text-center py-1">
                <i class="fa-solid fa-arrow-down text-white-50"></i>
              </div>
              <div class="col-12">
                <label class="small text-white-50 fw-bold mb-1">還車據點</label>
                <select v-model="calcDest" class="form-select form-select-sm rounded-3 border-0 shadow-none">
                  <option value="" disabled>請選擇迄點...</option>
                  <option v-for="loc in locations" :key="loc.locationId" :value="loc.locationId">{{ loc.locationName }}</option>
                </select>
              </div>
            </div>

            <div class="mt-4 p-3 bg-white bg-opacity-10 rounded-3 text-center border border-white border-opacity-25 ">
              <div class="small text-white-50 fw-bold mb-1">異地還車附加費</div>
              <div v-if="calculatedFee !== null">
                <span class="fs-3 fw-bold vehicle-font-mono">NT$ {{ calculatedFee }}</span>
              </div>
              <div v-else class="fs-6 mt-2 text-white-50">請選擇起迄點</div>
            </div>
          </div>
        </div>

        <div class="card border border-warning shadow-sm rounded-4 position-relative overflow-hidden">
          <div class="position-absolute top-0 end-0 w-25 h-100 bg-warning opacity-10" style="border-bottom-left-radius: 100%;"></div>
          <div class="card-body p-4 ">
            <h6 class="fw-bold text-dark flex items-center gap-2 mb-3 ">
              <i class="fa-solid fa-lightbulb text-warning me-2"></i>營運優化建議
            </h6>
            <p class="small text-muted mb-4">
              根據近期需求預測，建議調高「台北」至「台中」的異地還車費，以緩解週末前北部車輛庫存流失的問題。
            </p>
          </div>
        </div>

      </div>
    </div>


    <!-- 據點 Modal -->
    <div class="modal fade " id="locationFormModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-4">

          <div class="modal-header border-bottom-0 p-4 pb-0">
            <div>
              <h5 class="modal-title  fw-bold text-primary">{{ isEditMode ? '編輯據點設定' : '新增營運據點' }}</h5>
              <p class="text-muted small mb-0">{{ isEditMode ? '修改據點相關設定' : '建立新的營運據點' }}</p>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>

          <div class="modal-body p-4">
            <form @submit.prevent="saveLocation">
              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label small fw-bold text-muted">據點名稱 <span class="text-danger">*</span></label>
                  <input v-model="editFormData.locationName" type="text" class="vehicle-form-control rounded-3 bg-light" placeholder="如: 台北總站" required>
                </div>
                <div class="col-md-6">
                  <label class="form-label small fw-bold text-muted">聯絡電話</label>
                  <input v-model="editFormData.phone" type="text" class="vehicle-form-control rounded-3 bg-light vehicle-font-mono" placeholder="如: 02-1234-5678">
                </div>

                <div class="col-12">
                  <label class="form-label small fw-bold text-muted">詳細地址</label>
                  <input v-model="editFormData.address" type="text" class="vehicle-form-control rounded-3 bg-light">
                </div>

                <div class="col-md-6">
                  <label class="form-label small fw-bold text-muted">停車容量</label>
                  <input v-model="editFormData.parkingCapacity" type="number" min="1" class="vehicle-form-control rounded-3 bg-light">
                </div>
                <div class="col-md-6">
                  <label class="form-label small fw-bold text-muted">營運狀態 <span class="text-danger">*</span></label>
                  <select v-model="editFormData.locationStatus" class="form-select rounded-3 bg-light">
                    <option v-for="(info, key) in locationStatusMap" :key="key" :value="key">{{ info.text }}</option>
                  </select>
                </div>
              </div>
            </form>
          </div>

          <div class="modal-footer border-top-0 p-4 pt-0 d-flex justify-content-between align-items-center">
            <div>
              <button type="button" class="btn btn-outline-secondary btn-sm" @click="fillTestData" v-if="!isEditMode">
                <i class="fa-solid fa-wand-magic-sparkles me-1"></i>一鍵測試
              </button>
            </div>
            <div class="d-flex gap-2">
              <button type="button" class="btn btn-light fw-bold text-muted rounded-3" data-bs-dismiss="modal">取消</button>
              <button type="button" @click="saveLocation" class="btn btn-primary fw-bold rounded-3 shadow-sm">
                <i class="fa-solid fa-floppy-disk me-2"></i>儲存設定
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>

  </div>
  <AlertToast ref="toastRef" />
    <ConfirmDialog 
      :isVisible="confirmDialog.isVisible" 
      :message="confirmDialog.message" 
      @confirm="handleConfirm" 
      @cancel="confirmDialog.isVisible = false" 
    />
</template>

<style scoped>
.vehicle-fee-container {
  background-color: var(--color-bg-page);
}
.vehicle-font-mono {
  font-family: var(--font-mono);
  font-feature-settings: "tnum";
  letter-spacing: 0.05em;
}
.vehicle-fee-header {
  background-color: var(--color-blue-900);
}
.vehicle-hover-primary:hover {
  color: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  background-color: var(--color-primary-light) !important;
}

.vehicle-status-pill {
  font-family: var(--font-sans);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 700;
  font-size: 0.65rem;
}

.vehicle-form-control:focus {
  box-shadow: none;
}

.vehicle-table-layout-fixed { table-layout: fixed; }
.vehicle-hover-lift { transition: transform 0.2s ease, box-shadow 0.2s ease; }
.vehicle-hover-lift:hover { transform: translateY(-4px); box-shadow: var(--shadow-md) !important; }

.vehicle-fee-table th { background-color: #f8f9fa; }
.vehicle-fee-table td { position: relative; transition: background-color 0.2s ease; }
.vehicle-fee-table td:hover { background-color: var(--color-primary-light) !important; cursor: pointer; }
.vehicle-fee-table td:hover .edit-icon { opacity: 1 !important; transform: scale(1.1); }
.vehicle-transition-all { transition: all 0.2s ease; }

/* .cursor-pointer { cursor: pointer; } */
.vehicle-tracking-tight { letter-spacing: -0.02em; }
</style>