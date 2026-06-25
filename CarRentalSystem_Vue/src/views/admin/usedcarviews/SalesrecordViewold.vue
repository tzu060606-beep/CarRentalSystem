<script setup>
import { ref, onMounted, computed } from 'vue';
import salesrecordApi from '../../../api/usedcar/salesrecordApi';


const salesList = ref([]);
const loading = ref(false);

// 搜尋用的變數
const searchQuery = ref('');    // 文字框內容
const searchType = ref('paymentMethod'); // 預設搜尋類型

const showModal = ref(false); // 詳情彈窗
const editModal = ref(false); // 修改彈窗 (新增)
const selectedsales = ref(null);

// 1. 定義「工廠函式」回傳初始資料
// 這樣未來要增加欄位，只需要改這裡
const getInitialAddForm = () => ({
  usedCarId: 0,
  custId: 0,
  buyerName: '',
  buyerPhone: '',
  buyerIdno: 0,
  finalPrice: 0,
  paymentMethod: '',
  saleDate: '',
  empId: 0,
  notes: ''
});

// 2. 初始化響應式變數
const addModal = ref(false);
const addForm = ref(getInitialAddForm());

// 3. 開啟新增視窗的方法
const openAddModal = () => {
  addForm.value = getInitialAddForm();
  addModal.value = true;
};

// 4. 提交新增
const submitAdd = async () => {
  try {
    loading.value = true;

    const payload = {
      ...addForm.value,
      usedCarId: Number(addForm.value.usedCarId) || null,
      custId: Number(addForm.value.custId) || null,
      finalPrice: Number(addForm.value.finalPrice) || null,
      empId: Number(addForm.value.empId) || null,
      // 確保送出的是字串代碼
      paymentMethod: typeof addForm.value.paymentMethod === 'object'
        ? addForm.value.paymentMethod.dbCode
        : addForm.value.paymentMethod
    };

    const response = await salesrecordApi.insert(payload);
    salesList.value = response.data;

    alert('資料新增成功');
    addModal.value = false;
  } catch (error) {
    alert('新增失敗:' + (error.response?.data?.message || '格式錯誤'));
  } finally {
    loading.value = false;
  }
};

//查---------------------------------------------------------------

// 依照特定選項查單筆資料
const filteredsales = computed(() => {
  if (!searchQuery.value) return salesList.value; // 沒輸入文字就顯示全部

  return salesList.value.filter(sales => {
    let content = '';
    if (searchType.value === 'paymentMethod') {
      // 如果使用者選了「搜尋狀態」，我們把中文描述和英文代碼拼在一起
      content = `${sales.paymentMethod.description} ${sales.paymentMethod.dbCode}`;
    } else {
      content = String(sales[searchType.value] || '');
    }
    return content.toLowerCase().includes(searchQuery.value.toLowerCase());
  });
});

// 點擊「詳情」：抓取單筆資料並顯示視窗
const showDetail = async (id) => {
  try {
    const response = await salesrecordApi.getById(id);
    selectedsales.value = response.data;
    showModal.value = true;
  } catch (error) {
    alert('無法取得詳細資料');
  }
};

// 修改------------------------------------------
const editForm = ref({
  usedCarId: 0,
  custId: 0,
  buyerName: '',
  buyerPhone: '',
  buyerIdno: 0,
  finalPrice: 0,
  paymentMethod: '',
  saleDate: '',
  empId: 0,
  notes: ''
});

const loadData = async () => {
  loading.value = true;
  try {
    const response = await salesrecordApi.getAll();
    salesList.value = response.data;
  } catch (error) {
    alert('無法取得資料');
  } finally {
    loading.value = false;
  }
};

const handleUpdate = async (id) => {
  try {
    const response = await salesrecordApi.getById(id);
    const data = response.data;

    Object.assign(editForm.value, {
      ...data,
      paymentMethod: data.paymentMethod?.dbCode || data.paymentMethod
    });
    editModal.value = true;
  } catch (error) {
    alert('無法取的預修改的資料');
  }
};

//提交修改
const submitUpdate = async () => {
  try {
    loading.value = true;
    const id = editForm.value.saleId;

    const payload = {
      ...editForm.value,
      paymentMethod: typeof editForm.value.paymentMethod === 'object'
        ? editForm.value.paymentMethod.dbCode
        : editForm.value.paymentMethod
    };

    const response = await salesrecordApi.update(id, payload);
    salesList.value = response.data;

    alert('資料更新成功!');
    editModal.value = false;
  } catch (error) {
    // console.log(error.response); // 👈 一定要看
    alert('修改失敗');
  } finally {
    loading.value = false;
  }
};

//刪--------------------------------------------------

// 刪除資料
const handleDelete = async (id) => {
  if (confirm(`確定要刪除編號 ${id} 嗎？`)) {
    try {
      const response = await salesrecordApi.delete(id);
      salesList.value = response.data; // 刪除後立即刷新
      alert('刪除成功');
    } catch (error) {
      alert('刪除失敗');
    }
  }
};

const fillTestData = () => {
  addForm.value = {
    usedCarId: 1,     // ⚠️ 要確定資料庫存在
    custId: 1,        // ⚠️ 要存在
    buyerName: '測試用戶',
    buyerPhone: '0912345678',
    buyerIdno: 'A123456789',
    finalPrice: 500000,
    paymentMethod: 'CASH',
    saleDate: '2024-01-01',
    empId: 1,         // ⚠️ 要存在
    notes: '測試資料'
  };
};

//日期判斷邏輯

const todayStr = new Date().toISOString().split('T')[0];

onMounted(() => {
  loadData();
});


</script>

<template>
  <div class="container">
    <h2 class="mb-0 fw-bold">銷售紀錄管理</h2>

    <div class="action-bar d-flex align-items-center justify-content-between mb-3">
      <div class="search-group d-flex gap-2 flex-grow-1 me-3" style="max-width: 500px;">
        <select v-model="searchType" class="form-select w-auto" placeholder="請選擇搜尋方式">
          <option value="paymentMethod">按付款方式</option>
          <option value="empId">經手員工</option>
          <option value="buyerName">按買受人姓名</option>
        </select>

        <div class="input-group">
          <input type="text" v-model="searchQuery" placeholder="輸入關鍵字查詢..." class="form-control" />
          <button class="btn btn-outline-secondary">查詢</button>
        </div>
      </div>

      <button @click="openAddModal" class="btn btn-primary text-nowrap">
        ➕ 新增成交表
      </button>
    </div>

    <div class="bg-white border rounded shadow-sm overflow-hidden">
      <div class="table-responsive">
        <table class="table table-hover align-middle text-nowrap mb-0">
          <thead class="table-dark">
            <tr>
              <th>成交編號</th>
              <th>待售編號</th>
              <th>客戶編號</th>
              <th>買受人姓名</th>
              <th>成交價</th>
              <th>付款方式</th>
              <th>成交日</th>
              <th>經手員工</th>
              <th>成交備註</th>
              <th class="text-center sticky-end-header">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="sales in filteredsales" :key="sales.saleId" @click="showDetail(sales.saleId)"
              class="clickable-row">
              <td>{{ sales.saleId }}</td>
              <td>{{ sales.usedCarId }}</td>
              <td>{{ sales.custId }}</td>
              <td>{{ sales.buyerName }}</td>
              <td>{{ sales.finalPrice }}</td>
              <td>{{ sales.paymentMethod.description }}</td>
              <td>{{ sales.saleDate }}</td>
              <td>{{ sales.empId }}</td>
              <td>{{ sales.notes }}</td>
              <td class="d-flex justify-content-center">
                <button @click.stop="handleUpdate(sales.saleId)"
                  class="btn btn-sm btn-outline-secondary me-2">編輯</button>
                <button @click.stop="handleDelete(sales.saleId)" class="btn btn-sm btn-outline-danger">刪除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="footer">
      <h3>📊 統計資訊：共 {{ filteredsales.length }}/{{ salesList.length }} 筆資料</h3>
    </div>

    <div class="modal fade" :class="{ 'show d-block': showModal }" tabindex="-1" v-if="showModal"
      style="background: rgba(0,0,0,0.5)">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow border-0">

          <div class="modal-header bg-success text-white">
            <h5 class="modal-title fw-bold">💰 成交詳細資料</h5>
            <button type="button" class="btn-close btn-close-white" @click="showModal = false"></button>
          </div>

          <div class="modal-body p-0">
            <div v-if="selectedsales" class="detail-container">
              <ul class="list-group list-group-flush">

                <li class="list-group-item d-flex justify-content-between align-items-center">
                  <span class="text-muted">成交單號 / 員工</span>
                  <span>
                    <span class="fw-bold text-primary me-2">#{{ selectedsales.saleId }}</span>
                    <span class="badge bg-outline-secondary text-dark border">員工編號: {{ selectedsales.empId }}</span>
                  </span>
                </li>

                <li class="list-group-item d-flex justify-content-between align-items-center">
                  <span class="text-muted">待售車輛編號</span>
                  <span class="badge bg-secondary">ID: {{ selectedsales.usedCarId }}</span>
                </li>

                <li class="list-group-item d-flex justify-content-between align-items-center">
                  <span class="text-muted">買受人資訊</span>
                  <span class="text-end">
                    <div class="fw-bold">{{ selectedsales.buyerName }} (ID:{{ selectedsales.custId }})</div>
                    <div class="small text-muted">{{ selectedsales.buyerPhone }}</div>
                    <div class="small text-muted">{{ selectedsales.buyerIdno }}</div>
                  </span>
                </li>

                <li class="list-group-item d-flex justify-content-between align-items-center bg-light">
                  <span class="text-muted fw-bold">成交總價</span>
                  <span class="text-danger fw-bold fs-5">${{ selectedsales.finalPrice }}</span>
                </li>

                <li class="list-group-item d-flex justify-content-between align-items-center">
                  <span class="text-muted">付款方式</span>
                  <span class="badge bg-info text-dark">{{ selectedsales.paymentMethod?.description }}</span>
                </li>

                <li class="list-group-item d-flex justify-content-between align-items-center">
                  <span class="text-muted">成交日期</span>
                  <span>{{ selectedsales.saleDate }}</span>
                </li>

                <li class="list-group-item border-bottom-0">
                  <div class="text-muted small mb-1">成交備註：</div>
                  <div class="p-2 bg-light rounded text-break">
                    {{ selectedsales.notes || '無相關備註' }}
                  </div>
                </li>

              </ul>
            </div>

            <div v-else class="text-center p-5">
              <div class="spinner-border text-success" role="status"></div>
              <p class="mt-2 text-muted">正在讀取成交資訊...</p>
            </div>
          </div>

          <div class="modal-footer bg-light">
            <button type="button" class="btn btn-secondary px-4" @click="showModal = false">關閉</button>
          </div>

        </div>
      </div>
    </div>
  </div>

  <!-- 修改------------------------------------------ -->
  <div class="modal fade" :class="{ 'show d-block': editModal }" tabindex="-1" v-if="editModal"
    style="background: rgba(0,0,0,0.5)">
    <div class="modal-dialog modal-lg modal-dialog-centered">
      <div class="modal-content shadow border-0">

        <div class="modal-header bg-light">
          <h5 class="modal-title fw-bold">📝 修改成交資料 (編號: {{ editForm.saleId }})</h5>
          <button type="button" class="btn-close" @click="editModal = false"></button>
        </div>

        <div class="modal-body px-4">
          <form>
            <div class="row g-3">
              <div class="col-md-4">
                <label class="form-label fw-bold">待售編號 <span class="text-danger">*</span></label>
                <input type="number" v-model="editForm.usedCarId" class="form-control" />
              </div>
              <div class="col-md-4">
                <label class="form-label fw-bold">客戶編號 <span class="text-danger">*</span></label>
                <input type="number" v-model="editForm.custId" class="form-control" />
              </div>
              <div class="col-md-4">
                <label class="form-label fw-bold">經手員工 <span class="text-danger">*</span></label>
                <input type="number" v-model="editForm.empId" class="form-control" />
              </div>

              <div class="col-md-4">
                <label class="form-label fw-bold">買受人姓名 <span class="text-danger">*</span></label>
                <input type="text" v-model="editForm.buyerName" class="form-control" />
              </div>
              <div class="col-md-4">
                <label class="form-label fw-bold">買受人電話</label>
                <input type="text" v-model="editForm.buyerPhone" class="form-control" />
              </div>
              <div class="col-md-4">
                <label class="form-label fw-bold">身分證字號</label>
                <input type="text" v-model="editForm.buyerIdno" class="form-control" />
              </div>

              <div class="col-md-6">
                <label class="form-label fw-bold">成交價 <span class="text-danger">*</span></label>
                <div class="input-group">
                  <span class="input-group-text">$</span>
                  <input type="number" v-model="editForm.finalPrice" class="form-control" />
                </div>
              </div>
              <div class="col-md-6">
                <label class="form-label fw-bold">付款方式 <span class="text-danger">*</span></label>
                <select v-model="editForm.paymentMethod" class="form-select">
                  <option value="CASH">💵 現金 (CASH)</option>
                  <option value="CREDIT_CARD">💳 信用卡 (CREDIT_CARD)</option>
                  <option value="TRANSFER">🏦 轉帳 (TRANSFER)</option>
                </select>
              </div>

              <div class="col-12">
                <label class="form-label fw-bold">成交日</label>
                <input type="date" v-model="editForm.saleDate" :max="todayStr" class="form-control" />
              </div>

              <div class="col-12">
                <label class="form-label fw-bold">成交備註</label>
                <textarea v-model="editForm.notes" rows="2" class="form-control" placeholder="請輸入成交相關備註..."></textarea>
              </div>
            </div>
          </form>
        </div>

        <div class="modal-footer bg-light">
          <button type="button" class="btn btn-secondary px-4" @click="editModal = false">取消</button>
          <button type="button" class="btn btn-primary px-4 shadow-sm" @click="submitUpdate" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
            {{ loading ? '儲存中...' : '確認修改' }}
          </button>
        </div>

      </div>
    </div>
  </div>

  <!-- 新增---------------------------------------------------- -->

  <div class="modal fade" :class="{ 'show d-block': addModal }" tabindex="-1" v-if="addModal"
    style="background: rgba(0,0,0,0.5)">
    <div class="modal-dialog modal-lg modal-dialog-centered">
      <div class="modal-content shadow border-0">

        <div class="modal-header bg-light d-flex justify-content-between align-items-center">
          <h5 class="modal-title fw-bold">➕ 新增成交紀錄</h5>
          <button @click.prevent="fillTestData" class="btn btn-sm btn-outline-info ms-auto me-2">
            ⚡ 一鍵填入測試資料
          </button>
          <button type="button" class="btn-close" @click="addModal = false"></button>
        </div>

        <div class="modal-body px-4">
          <form>
            <div class="row g-3">
              <div class="col-md-4">
                <label class="form-label fw-bold">待售車輛編號 <span class="text-danger">*</span></label>
                <input type="number" v-model="addForm.usedCarId" class="form-control" placeholder="輸入車輛 ID" />
              </div>
              <div class="col-md-4">
                <label class="form-label fw-bold">客戶編號 <span class="text-danger">*</span></label>
                <input type="number" v-model="addForm.custId" class="form-control" placeholder="輸入客戶 ID" />
              </div>
              <div class="col-md-4">
                <label class="form-label fw-bold">經手員工編號 <span class="text-danger">*</span></label>
                <input type="number" v-model="addForm.empId" class="form-control" placeholder="輸入員工 ID" />
              </div>

              <div class="col-md-4">
                <label class="form-label fw-bold">買受人姓名 <span class="text-danger">*</span></label>
                <input type="text" v-model="addForm.buyerName" class="form-control" placeholder="真實姓名" />
              </div>
              <div class="col-md-4">
                <label class="form-label fw-bold">聯繫電話</label>
                <input type="text" v-model="addForm.buyerPhone" class="form-control" placeholder="09xx-xxx-xxx" />
              </div>
              <div class="col-md-4">
                <label class="form-label fw-bold">身分證字號</label>
                <input type="text" v-model="addForm.buyerIdno" class="form-control" placeholder="身分證字號" />
              </div>

              <div class="col-md-6">
                <label class="form-label fw-bold">成交價格 <span class="text-danger">*</span></label>
                <div class="input-group">
                  <span class="input-group-text">$</span>
                  <input type="number" v-model="addForm.finalPrice" class="form-control" placeholder="請輸入成交金額" />
                </div>
              </div>
              <div class="col-md-6">
                <label class="form-label fw-bold">付款方式 <span class="text-danger">*</span></label>
                <select v-model="addForm.paymentMethod" class="form-select">
                  <option value="" disabled>請選擇付款方式</option>
                  <option value="CASH">💵 現金 (CASH)</option>
                  <option value="CREDIT_CARD">💳 信用卡 (CREDIT_CARD)</option>
                  <option value="TRANSFER">🏦 轉帳 (TRANSFER)</option>
                </select>
              </div>

              <div class="col-12">
                <label class="form-label fw-bold">成交日期 <span class="text-danger">*</span></label>
                <input type="date" v-model="addForm.saleDate" :max="todayStr" class="form-control" />
              </div>

              <div class="col-12">
                <label class="form-label fw-bold">成交備註</label>
                <textarea v-model="addForm.notes" rows="2" class="form-control" placeholder="備註相關交車細節..."></textarea>
              </div>
            </div>
          </form>
        </div>

        <div class="modal-footer bg-light">
          <button type="button" class="btn btn-secondary px-4" @click="addModal = false">取消</button>
          <button type="button" class="btn btn-primary px-4 shadow-sm" @click="submitAdd" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
            {{ loading ? '儲存中...' : '確認新增' }}
          </button>
        </div>

      </div>
    </div>
  </div>


</template>


<!-- <style scoped>
</style> -->