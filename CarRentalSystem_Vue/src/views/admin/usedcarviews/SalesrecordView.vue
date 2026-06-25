<script setup>
import { ref, onMounted, computed } from 'vue';
import salesrecordApi from '@/api/usedcar/salesrecordApi'; // 假設你的銷售紀錄 API 路徑
import usedCarApi from '@/api/usedcar/usedCarApi'; // 用於取得車輛選項
import { customerAPI } from '@/api/login/customerAPI'; // 用於取得客戶選項
import { employeeAPI } from '@/api/login/employeeAPI'; // 用於取得員工

// 引入成交紀錄專用的共用元件
import AddModal from './SalesrecordAddView.vue';
import EditModal from './SalesrecordEditView.vue';
import DetailModal from './SalesrecordDetailView.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import AlertToast from '@/components/common/AlertToast.vue';
import SearchBar from '@/components/common/SearchBar.vue';
import ActionButtons from '@/components/common/ActionButtons.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';

// 狀態管理
const salesList = ref([]);
const loading = ref(false);
const searchQuery = ref('');
const searchType = ref('saleId');
const toast = ref(null);

// 彈窗控制
const addModal = ref(false);
const editModal = ref(false);
const detailModal = ref(false);
const confirmDeleteModal = ref(false);

const selectedSales = ref(null);
const salesIdToDelete = ref(null);

// 儲存下拉式選項
const carOptions = ref([]);
const customerOptions = ref([]);
const empOptions = ref([]);

// 讀取成交紀錄資料
const loadData = async () => {
  loading.value = true;
  try {
    // 使用 allSettled，確保即使某個介面掛掉，其他資料也能顯示
    const results = await Promise.allSettled([
      salesrecordApi.getAll(),
      usedCarApi.getAllDetails(), // 請確認此 API 是否正確匯入
      customerAPI.getAll(),       // 請確認此 API 是否正確匯入
      employeeAPI.getAll()        // 請確認此 API 是否正確匯入
    ]);

    // 檢查每一個請求的結果
    const [sales, cars, custs, emps] = results;

    if (sales.status === 'fulfilled') salesList.value = sales.value.data?.data || sales.value.data || [];
    else toast.value.show('成交紀錄載入失敗', 'danger');

    if (cars.status === 'fulfilled') carOptions.value = cars.value.data || [];
    if (custs.status === 'fulfilled') customerOptions.value = custs.value.data || [];
    if (emps.status === 'fulfilled') empOptions.value = emps.value.data || [];

  } catch (error) {
    console.error("Data load error:", error);
    toast.value.show('系統資料載入發生異常', 'danger');
  } finally {
    loading.value = false;
  }
};

// 開啟明細彈窗
const openModal = async (id) => {
  try {
    const response = await salesrecordApi.getById(id);
    selectedSales.value = response.data.data || response.data;
    detailModal.value = true;
  } catch (error) {
    toast.value.show('無法取得成交明細', 'warning');
  }
};

// 篩選邏輯
const filteredSales = computed(() => {
  if (!searchQuery.value) return salesList.value;
  return salesList.value.filter(item => {
    let content = '';
    if (searchType.value === 'paymentMethod') {
      content = `${item.paymentMethod?.description || ''} ${item.paymentMethod?.dbCode || ''}`;
    } else {
      content = String(item[searchType.value] || '');
    }
    return content.toLowerCase().includes(searchQuery.value.toLowerCase());
  });
});

// 處理修改
const handleEdit = async (id) => {
  try {
    const response = await salesrecordApi.getById(id);
    selectedSales.value = response.data.data || response.data;
    editModal.value = true;
  } catch (error) {
    toast.value.show('取得修改資料失敗', 'danger');
  }
};


const todayStr = new Date().toISOString().split('T')[0];
onMounted(loadData);
</script>

<template>
  <div class="container py-4">
    <!-- 全域元件 -->
    <LoadingSpinner :isLoading="loading" overlay />
    <AlertToast ref="toast" />

    <!-- 頁首：標題與新增按鈕 -->
    <div class="d-flex flex-column flex-md-row align-items-md-center justify-content-between mb-4 gap-3">
      <div>
        <h2 class="mb-1 fw-bold text-dark">成交紀錄管理</h2>
        <p class="text-secondary mb-0 small">追蹤二手車成交歷史、買家資訊與收款金額</p>
      </div>
      <button @click="addModal = true" class="btn btn-success px-4 py-2 shadow-sm">
        <i class="bi bi-cart-check me-2"></i>登記新成交
      </button>
    </div>

    <!-- 篩選區 -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-body p-3">
        <div class="row g-3 align-items-center">
          <div class="col-12 col-md-auto">
            <div class="input-group">
              <label class="input-group-text bg-light border-end-0" for="searchTypeSelect">
                <i class="bi bi-filter"></i>
              </label>
              <select id="searchTypeSelect" v-model="searchType" class="form-select border-start-0"
                style="min-width: 130px;">
                <option value="saleId">成交編號</option>
                <option value="buyerName">買受人姓名</option>
                <option value="usedCarId">待售車編號</option>
                <option value="paymentMethod">付款方式</option>
              </select>
            </div>
          </div>
          <div class="col-12 col-md">
            <SearchBar v-model="searchQuery" :showButton="false" placeholder="請輸入關鍵字檢索..." />
          </div>
        </div>
      </div>
    </div>

    <!-- 資料列表區 -->
    <div class="card border-0 shadow-sm overflow-hidden">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th class="ps-4 text-secondary text-uppercase small fw-bold">成交編號</th>
              <th class="text-secondary text-uppercase small fw-bold">買受人</th>
              <th class="text-secondary text-uppercase small fw-bold">車輛 ID</th>
              <th class="text-secondary text-uppercase small fw-bold">成交總價</th>
              <th class="text-secondary text-uppercase small fw-bold">成交日期</th>
              <th class="text-secondary text-uppercase small fw-bold">付款方式</th>
              <th class="text-secondary text-uppercase small fw-bold">付款狀態</th>
              <th class="text-center pe-4 text-secondary text-uppercase small fw-bold">功能操作</th>
            </tr>
          </thead>
          <tbody class="border-top-0">
            <tr v-for="sale in filteredSales" :key="sale.saleId" @click="openModal(sale.saleId)" role="button"
              class="user-select-none">
              <td class="ps-4">
                <span class="fw-bold">#{{ sale.saleId }}</span>
              </td>
              <td>
                <div class="d-flex align-items-center">
                  <i class="bi bi-person-circle me-2 text-secondary"></i>
                  <span>{{ sale.buyerName }}</span>
                </div>
              </td>
              <td>
                <span class="badge rounded-pill bg-light text-dark border px-3">
                  {{ sale.usedCarId }}
                </span>
              </td>
              <td class="fw-bold text-success">
                ${{ sale.finalPrice?.toLocaleString() }}
              </td>
              <td class="text-muted small">{{ sale.saleDate }}</td>
              <td>
                <StatusBadge :status="sale.paymentMethod?.dbCode || sale.paymentMethod" :map="{
                  CASH: { label: '現金', variant: 'success' },
                  CREDIT_CARD: { label: '信用卡', variant: 'primary' },
                  TRANSFER: { label: '銀行轉帳', variant: 'info' }
                }" />
              </td>
              <td>
                <StatusBadge :status="sale.payStatus?.dbCode || sale.payStatus" :map="{
                  PENDING: { label: '待付款', variant: 'success' },
                  PAID: { label: '已付款', variant: 'primary' },
                  CANCELLED: { label: '已取消', variant: 'danger' }
                }" />
              </td>
              <td @click.stop class="text-center pe-4">
                <button @click="handleEdit(sale.saleId)"
                  class="btn btn-sm btn-outline-primary px-3 rounded-2 fw-medium">
                  <i class="bi bi-pencil-square me-1"></i>編輯
                </button>
              </td>
            </tr>
            <!-- 無資料顯示 -->
            <tr v-if="filteredSales.length === 0">
              <td colspan="7" class="text-center py-5">
                <div class="text-muted">
                  <i class="bi bi-clipboard-x display-6 d-block mb-2"></i>
                  尚無成交紀錄資料
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- 頁腳資訊 -->
      <div class="card-footer bg-white border-top-0 py-3 ps-4">
        <small class="text-muted">本頁紀錄共：<strong>{{ filteredSales.length }}</strong> 筆</small>
      </div>
    </div>

    <!-- 彈窗區 -->
    <AddModal :isVisible="addModal" :todayStr="todayStr" @close="addModal = false" @refresh="loadData" />

    <EditModal :isVisible="editModal" :selectedSales="selectedSales" :carOptions="carOptions"
      :customerOptions="customerOptions" :empOptions="empOptions" @close="editModal = false" />

    <DetailModal :isVisible="detailModal" :selectedSales="selectedSales" @close="detailModal = false" />


  </div>
</template>

<style scoped>
/* .table-hover tbody tr:hover {
  background-color: rgba(0, 0, 0, 0.02);
} */
</style>