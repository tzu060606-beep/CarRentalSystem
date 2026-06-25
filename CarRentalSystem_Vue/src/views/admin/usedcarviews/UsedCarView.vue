<script setup>
import { ref, onMounted, computed } from 'vue';
import { vehicleAPI } from '@/api/vehicle/vehicleApi';
import usedCarApi from '@/api/usedcar/usedCarApi';

// 引入共用元件
import AddModal from './UsedCarAddView.vue';
import EditModal from './UsedCarEditView.vue';
import DetailModal from './UsedCarDetailView.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import AlertToast from '@/components/common/AlertToast.vue';
import SearchBar from '@/components/common/SearchBar.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';

// 狀態管理
const carList = ref([]);
const loading = ref(false);
const searchQuery = ref('');
const searchType = ref('all'); // 🌟 優化：預設改為 'all' 全局搜尋，體驗更好
const availableVehicles = ref([]);
const toast = ref(null);

// 彈窗控制
const addModal = ref(false);
const editModal = ref(false);
const detailModal = ref(false);

const selectedCar = ref(null);
const selectedVehicle = ref(null);
const currentUsedCarStatus = ref(''); // 🌟 配合上一題，增加存放二手車狀態的變數

// 讀取資料
const loadData = async () => {
    loading.value = true;
    try {
        const [resList, resVehicles] = await Promise.all([
            usedCarApi.getAll(),
            vehicleAPI.getAll()
        ]);
        carList.value = resList.data.data || resList.data || [];
        availableVehicles.value = resVehicles.data.data || resVehicles.data || [];
    } catch (error) {
        toast.value?.show('資料讀取失敗', 'danger');
    } finally {
        loading.value = false;
    }
};

// 🌟 優化：點擊整行開啟明細，順便分離二手車狀態與車輛詳情
const openModal = async (carItem) => {
    loading.value = true;
    try {
        const response = await usedCarApi.getOneDetail(carItem.usedCarId);
        const data = response.data.data || response.data;

        // 核心修改點：將狀態合併到 selectedVehicle 物件中，而不是單獨存成一個變數
        // 這樣傳遞給 DetailModal 時，它會被視為 selectedVehicle 的一部分
        const vehicleData = data.vehicle || data;

        selectedVehicle.value = {
            ...vehicleData,
            // 只要選定的物件裡有這個屬性，就不會產生「多餘的非 Prop 屬性」警告
            usedCarStatus: carItem.status?.dbCode || ''
        };

        detailModal.value = true;
    } catch (error) {
        toast.value?.show('無法取得明細', 'warning');
    } finally {
        loading.value = false;
    }
};

// 🌟 優化：效能與防禦力更高、且支援全局搜尋的計算屬性
const filteredCars = computed(() => {
    const query = searchQuery.value.trim().toLowerCase();
    if (!query) return carList.value;

    return carList.value.filter(car => {
        const vId = String(car.vehicleId || '').toLowerCase();
        const statusDesc = String(car.status?.description || '').toLowerCase();
        const statusCode = String(car.status?.dbCode || '').toLowerCase();
        const price = String(car.askingPrice || '');

        // 根據選擇的搜尋類型進行過濾
        if (searchType.value === 'vehicleId') {
            return vId.includes(query);
        } else if (searchType.value === 'status') {
            return statusDesc.includes(query) || statusCode.includes(query);
        } else {
            // 'all': 全局聯動搜尋（車牌/狀態/價格通通都能打）
            return vId.includes(query) || statusDesc.includes(query) || price.includes(query);
        }
    });
});

const handleEdit = async (id) => {
    try {
        const response = await usedCarApi.getById(id);
        selectedCar.value = response.data.data || response.data;
        editModal.value = true;
    } catch (error) {
        toast.value?.show('取得修改資料失敗', 'danger');
    }
};

const todayStr = new Date().toISOString().split('T')[0];
onMounted(loadData);
</script>

<template>
    <div class="container py-4">
        <LoadingSpinner :isLoading="loading" overlay />
        <AlertToast ref="toast" />

        <div class="d-flex flex-column flex-md-row align-items-md-center justify-content-between mb-4 gap-3">
            <div>
                <h2 class="mb-1 fw-bold text-dark">二手車庫存管理</h2>
                <p class="text-secondary small mb-0">管理租賃與公務車退役後之轉售車況、上架狀態與市場定價</p>
            </div>
            <button @click="addModal = true" class="btn btn-primary px-4 py-2 shadow-sm fw-medium">
                <i class="bi bi-plus-lg me-2"></i>新增庫存車輛
            </button>
        </div>

        <div class="card border-0 shadow-sm mb-4">
            <div class="card-body p-3">
                <div class="row g-3 align-items-center">
                    <div class="col-12 col-md-auto">
                        <div class="input-group">
                            <label class="input-group-text bg-light border-end-0" for="searchTypeSelect">
                                <i class="bi bi-filter text-secondary"></i>
                            </label>
                            <select id="searchTypeSelect" v-model="searchType" class="form-select border-start-0"
                                style="min-width: 140px;">
                                <option value="all">🔍 全部欄位</option>
                                <option value="vehicleId">車輛編號</option>
                                <option value="status">出售狀態</option>
                            </select>
                        </div>
                    </div>
                    <div class="col-12 col-md">
                        <SearchBar v-model="searchQuery" :showButton="false" placeholder="請輸入車輛編號、狀態關鍵字檢索..." />
                    </div>
                </div>
            </div>
        </div>

        <div class="card border-0 shadow-sm overflow-hidden">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light border-bottom">
                        <tr>
                            <th class="ps-4 text-secondary text-uppercase small fw-bold" style="width: 80px;">ID</th>
                            <th class="text-secondary text-uppercase small fw-bold">車輛編號</th>
                            <th class="text-secondary text-uppercase small fw-bold">預售價格</th>
                            <th class="text-secondary text-uppercase small fw-bold">上架日期</th>
                            <th class="text-secondary text-uppercase small fw-bold">刊登截止</th>
                            <th class="text-secondary text-uppercase small fw-bold">目前狀態</th>
                            <th class="text-center pe-4 text-secondary text-uppercase small fw-bold"
                                style="width: 120px;">功能操作</th>
                        </tr>
                    </thead>
                    <tbody class="border-top-0">
                        <tr v-for="car in filteredCars" :key="car.usedCarId" @click="openModal(car)" role="button"
                            class="user-select-none table-row-hover">
                            <td class="ps-4 text-secondary font-monospace">{{ car.usedCarId }}</td>
                            <td>
                                <span
                                    class="badge rounded-pill bg-primary bg-opacity-10 text-primary px-3 py-1.5 fw-medium">
                                    {{ car.vehicleId }}
                                </span>
                            </td>
                            <td class="fw-bold text-dark font-monospace">${{ car.askingPrice?.toLocaleString() }}</td>
                            <td class="text-muted small">{{ car.listDate }}</td>
                            <td class="text-muted small">{{ car.expireDate || '無期限' }}</td>
                            <td>
                                <StatusBadge :status="car.status?.dbCode" :map="{
                                    ACTIVE: { label: '上架中', variant: 'success' },
                                    SOLD: { label: '已售出', variant: 'primary' },
                                    REMOVED: { label: '下架', variant: 'warning' }
                                }" />
                            </td>
                            <td @click.stop class="text-center pe-4">
                                <button @click="handleEdit(car.usedCarId)"
                                    class="btn btn-sm btn-outline-primary px-3 rounded-2 fw-medium">
                                    <i class="bi bi-pencil-square me-1"></i>編輯
                                </button>
                            </td>
                        </tr>

                        <tr v-if="filteredCars.length === 0">
                            <td colspan="7" class="text-center py-5">
                                <div class="text-muted">
                                    <i class="bi bi-search display-6 d-block mb-3 text-secondary bg-opacity-10"></i>
                                    <span class="fw-medium">沒有找到符合條件的二手車輛</span>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="card-footer bg-white border-top py-3 ps-4 d-flex justify-content-between align-items-center">
                <small class="text-muted">目前顯示數量：<strong class="text-dark">{{ filteredCars.length }}</strong> 筆</small>
                <small class="text-muted pe-3">系統時間：{{ todayStr }}</small>
            </div>
        </div>

        <AddModal :isVisible="addModal" :availableVehicles="availableVehicles" :usedCars="carList" :todayStr="todayStr"
            @close="addModal = false" @refresh="loadData" />

        <EditModal :isVisible="editModal" :editData="selectedCar" :todayStr="todayStr" @close="editModal = false"
            @refresh="loadData" />

        <DetailModal :isVisible="detailModal" :selectedVehicle="selectedVehicle" @close="detailModal = false" />
    </div>
</template>

<style scoped>
/* 🌟 自訂精細的 CSS 讓整體呈現更具 OneRent 的商用質感 */
.table-row-hover {
    transition: background-color 0.15s ease;
}

.table-row-hover:hover {
    background-color: var(--bs-gray-100) !important;
}

.font-monospace {
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}
</style>