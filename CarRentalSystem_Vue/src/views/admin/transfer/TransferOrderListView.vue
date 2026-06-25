<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/api/index';

// 儲存從後端取得的所有接送訂單資料
const orders = ref([])

// ===== 搜尋條件相關變數 =====
// 預設查詢欄位為 'all' (全部訂單)
const searchField = ref('all')
// 用於儲存數字類型的搜尋條件 (例如：訂單編號、客戶編號)
const intValue = ref('')
// 用於儲存文字類型的搜尋條件 (例如：電話、地點、備註)
const textValue = ref('')
// 預設下拉選單選項
const transferTypeValue = ref('接機')
const statusValue = ref('已預訂')
// 用於儲存日期時間區間的搜尋條件
const startDateTime = ref('')
const endDateTime = ref('')

// 清除所有搜尋條件的函式，將各項輸入值還原為預設狀態
const clearSearch = () => {
  searchField.value = 'all'
  intValue.value = ''
  textValue.value = ''
  transferTypeValue.value = '接機'
  statusValue.value = '已預訂'
  startDateTime.value = ''
  endDateTime.value = ''
}

// ===== 核心過濾邏輯 =====
// computed 屬性：根據目前選擇的 `searchField` 及對應的輸入值，即時過濾 orders 陣列
const filteredOrders = computed(() => {
  if (searchField.value === 'all') return orders.value;

  return orders.value.filter(order => {
    const field = searchField.value;
    
    // 數字類型欄位 (精確或包含比對)
    if (['transferId', 'custId', 'driverId', 'vehicleId', 'rateId'].includes(field)) {
      if (!intValue.value) return true;
      const val = order[field];
      return val && val.toString().includes(intValue.value.toString());
    } 
    // 文字類型欄位 (模糊比對)
    else if (['custPhone', 'pickupLocation', 'dropoffLocation', 'note'].includes(field)) {
      if (!textValue.value) return true;
      const val = order[field];
      return val && val.includes(textValue.value);
    } 
    // 接送類型下拉選單
    else if (field === 'transferType') {
      return order.transferType === transferTypeValue.value;
    } 
    // 訂單狀態下拉選單
    else if (field === 'status') {
      return order.status === statusValue.value;
    } 
    // 日期時間區間
    else if (['scheduledPickupTime', 'scheduledDropoffTime', 'realDropoffTime', 'createdAt'].includes(field)) {
      if (!startDateTime.value && !endDateTime.value) return true;
      const val = order[field];
      if (!val) return false;
      
      const dateVal = new Date(val).getTime();
      const start = startDateTime.value ? new Date(startDateTime.value).getTime() : 0;
      const end = endDateTime.value ? new Date(endDateTime.value).getTime() : Infinity;
      
      return dateVal >= start && dateVal <= end;
    }
    return true;
  });
})

// ===== API 請求與資料處理 =====
// 向後端請求接送訂單列表
const fetchOrders = async () => {
  try {
    // 加上 ?_t=時間戳記 防止瀏覽器快取 GET 請求，確保每次都取得最新資料
    const response = await request.get('/transferOrder?_t=' + new Date().getTime())
    orders.value = response.data
  } catch (error) {
    console.error('無法取得接送訂單資料:', error)
  }
}

// 刪除指定的訂單
const deleteOrder = async (id) => {
  if (confirm('確定要刪除這筆訂單嗎？')) {
    try {
      await request.delete(`/transferOrder/${id}`)
      // 刪除成功後，重新抓取列表以更新畫面
      fetchOrders()
    } catch (error) {
      console.error('刪除訂單失敗:', error)
      alert('刪除失敗')
    }
  }
}

// 變更狀態流程 API（開始、取消等通用狀態切換）
const updateStatus = async (id, action, confirmMessage) => {
  if (confirm(confirmMessage)) {
    try {
      await request.post(`/transferOrder/${id}/${action}`)
      // 狀態變更成功後，重新抓取列表以更新畫面
      fetchOrders()
    } catch (error) {
      console.error(`狀態變更失敗 (${action}):`, error)
      alert('狀態變更失敗')
    }
  }
}

// 完成接送（需輸入結束里程，後端會重新計算費用並更新車輛里程）
const finishTransfer = async (id) => {
  const input = prompt('請輸入結束里程數（公里）：')
  // 使用者按取消則中止
  if (input === null) return
  const endMileage = Number(input)
  if (isNaN(endMileage) || endMileage < 0) {
    alert('請輸入有效的里程數（正整數）')
    return
  }
  if (!confirm('確定已完成接送嗎？')) return
  try {
    await request.post(
      `/transferOrder/${id}/finish`,
      null,
      { params: { endMileage } }
    )
    fetchOrders()
  } catch (error) {
    console.error('完成接送失敗:', error)
    alert('完成接送失敗')
  }
}

onMounted(() => {
  fetchOrders()
})
</script>
<!-- <script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const orders = ref([])

const fetchOrders = async () => {
  try {
    const response = await axios.get('http://localhost:8081/api/transferOrder')
    orders.value = response.data
  } catch (error) {
    console.error(error)
  }
}

const deleteOrder = async (id) => {
  if (confirm('確定要刪除嗎？')) {
    try {
      await axios.delete(`http://localhost:8081/api/transferOrder/${id}`)
      fetchOrders()
    } catch (error) {
      console.error(error)
      alert('刪除失敗')
    }
  }
}

onMounted(() => {
  fetchOrders()
})
</script> -->

<template>
  <div class="container-fluid py-4">
    
    <!-- Page Header (漸層背景標題) -->
    <div class="card border-0 shadow-sm mb-4 bg-primary bg-gradient text-white rounded-4">
      <div class="card-body p-4">
        <h2 class="fw-bold mb-1"><i class="fa-solid fa-van-shuttle me-2"></i>接送訂單管理</h2>
        <p class="mb-0 text-white-50">Transfer orders status.</p>
      </div>
    </div>

    <!-- 搜尋與篩選工具列：根據下拉選單動態顯示對應的輸入元件 -->
    <div class="card border-0 shadow-sm mb-4 rounded-4">
      <div class="card-body p-4">
        <div class="row g-3 align-items-end">
          
          <div class="col-md-3">
            <label class="form-label text-muted small fw-bold mb-1">查詢欄位</label>
            <select v-model="searchField" class="form-select">
              <option value="all">全部訂單</option>
              <option value="transferId">接送訂單編號</option>
              <option value="custId">客戶編號</option>
              <option value="custPhone">客戶連絡電話</option>
              <option value="driverId">司機編號</option>
              <option value="vehicleId">車輛編號</option>
              <option value="rateId">費率編號</option>
              <option value="transferType">接送類型</option>
              <option value="pickupLocation">上車地點</option>
              <option value="dropoffLocation">下車地點</option>
              <option value="scheduledPickupTime">預計上車時間</option>
              <option value="scheduledDropoffTime">預計下車時間</option>
              <option value="realDropoffTime">實際下車時間</option>
              <option value="status">訂單狀態</option>
              <option value="note">備註</option>
              <option value="createdAt">資料建立時間</option>
            </select>
          </div>

          <!-- 動態顯示對應的輸入框 -->
          <div v-if="['transferId', 'custId', 'driverId', 'vehicleId', 'rateId'].includes(searchField)" class="col-md-4">
            <label class="form-label text-muted small fw-bold mb-1">查詢值 (數字)</label>
            <div class="input-group">
              <span class="input-group-text bg-white border-end-0">
                <i class="fa-solid fa-magnifying-glass text-muted"></i>
              </span>
              <input v-model="intValue" type="number" class="form-control border-start-0 ps-0" placeholder="請輸入數值" />
            </div>
          </div>

          <div v-if="['custPhone', 'pickupLocation', 'dropoffLocation', 'note'].includes(searchField)" class="col-md-4">
            <label class="form-label text-muted small fw-bold mb-1">查詢文字</label>
            <div class="input-group">
              <span class="input-group-text bg-white border-end-0">
                <i class="fa-solid fa-magnifying-glass text-muted"></i>
              </span>
              <input v-model="textValue" type="text" class="form-control border-start-0 ps-0" placeholder="請輸入關鍵字" />
            </div>
          </div>

          <div v-if="searchField === 'transferType'" class="col-md-4">
            <label class="form-label text-muted small fw-bold mb-1">接送類型</label>
            <select v-model="transferTypeValue" class="form-select">
              <option value="接機">接機</option>
              <option value="送機">送機</option>
              <option value="一般">一般</option>
            </select>
          </div>

          <div v-if="searchField === 'status'" class="col-md-4">
            <label class="form-label text-muted small fw-bold mb-1">訂單狀態</label>
            <select v-model="statusValue" class="form-select">
              <option value="已預訂">已預訂</option>
              <option value="接送中">接送中</option>
              <option value="已完成">已完成</option>
              <option value="已取消">已取消</option>
              <option value="待處理">待處理</option>
              <option value="處理中">處理中</option>
            </select>
          </div>

          <div v-if="['scheduledPickupTime', 'scheduledDropoffTime', 'realDropoffTime', 'createdAt'].includes(searchField)" class="col-md-5">
            <div class="row g-2">
              <div class="col-6">
                <label class="form-label text-muted small fw-bold mb-1">開始日期時間</label>
                <input v-model="startDateTime" type="datetime-local" class="form-control" />
              </div>
              <div class="col-6">
                <label class="form-label text-muted small fw-bold mb-1">結束日期時間</label>
                <input v-model="endDateTime" type="datetime-local" class="form-control" />
              </div>
            </div>
          </div>

          <!-- 新增與清除按鈕 -->
          <div class="col text-end">
            <button class="btn btn-outline-info me-2" @click="$router.push('/home')">
              <i class="fa-solid fa-house me-1"></i>回首頁
            </button>
            <button class="btn btn-outline-secondary me-2" @click="clearSearch">
              <i class="fa-solid fa-eraser me-1"></i>清除
            </button>
            <button class="btn btn-primary d-inline-flex align-items-center" @click="$router.push('/transferOrder/add')">
                <i class="fa-solid fa-plus me-2"></i>新增訂單
            </button>
          </div>

        </div>
      </div>
    </div>

    <!-- 資料表格區塊 -->
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0 text-nowrap">
          <thead class="table-light">
            <tr>
              <th class="py-3 px-4 text-muted small">
                訂單編號 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i>
              </th>
              <th class="py-3 px-4 text-muted small">
                客戶 (編號/名稱) <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i>
              </th>
              <th class="py-3 px-4 text-muted small">
                客戶電話 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i>
              </th>
              <th class="py-3 px-4 text-muted small">
                類型 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i>
              </th>
              <th class="py-3 px-4 text-muted small">
                上車地點 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i>
              </th>
              <th class="py-3 px-4 text-muted small">
                下車地點 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i>
              </th>
              <th class="py-3 px-4 text-muted small">
                指派車輛 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i>
              </th>
              <th class="py-3 px-4 text-muted small">
                總費用 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i>
              </th>
              <th class="py-3 px-4 text-muted small">
                狀態 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i>
              </th>
              <th class="py-3 px-4 text-end text-muted small">操作</th>
            </tr>
          </thead>
          <tbody class="border-top-0">
            <tr v-for="order in filteredOrders" :key="order.transferId">
              <td class="py-3 px-4 fw-bold">#{{ order.transferId }}</td>
              <td class="py-3 px-4">
                {{ order.custId }}
                <span v-if="order.custName" style="color: #64748b; font-size: 0.9em; margin-left: 4px;">({{ order.custName }})</span>
              </td>
              <td class="py-3 px-4 text-secondary">{{ order.custPhone || '未提供' }}</td>
              <td class="py-3 px-4">
                <span class="badge rounded-pill px-3 py-2 text-bg-primary">{{ order.transferType }}</span>
              </td>
              <td class="py-3 px-4 text-secondary">{{ order.pickupLocation }}</td>
              <td class="py-3 px-4 text-secondary">{{ order.dropoffLocation }}</td>
              <td class="py-3 px-4">
                <span v-if="order.vehicleId" class="badge bg-secondary">車號: {{ order.vehicleId }}</span>
                <span v-else class="text-muted small">尚未指派</span>
              </td>
              <td class="py-3 px-4 text-secondary">${{ order.totalAmount }}</td>
              <td class="py-3 px-4">
                <span class="badge rounded-pill px-3 py-2" :class="['已完成'].includes(order.status) ? 'text-bg-success' : 'text-bg-warning'">
                  {{ order.status }}
                </span>
              </td>
              <td class="py-3 px-4 text-end">
                <div class="d-flex justify-content-end gap-1">
                  <!-- 狀態按鈕 -->
                  <button v-if="['待處理', '已預訂'].includes(order.status)" class="btn btn-sm btn-outline-primary" type="button" @click.prevent="updateStatus(order.transferId, 'start', '確定要開始接送嗎？')">
                    <i class="fa-solid fa-play"></i> 開始
                  </button>
                  <button v-if="['處理中', '接送中'].includes(order.status)" class="btn btn-sm btn-outline-success" type="button" @click.prevent="finishTransfer(order.transferId)">
                    <i class="fa-solid fa-check"></i> 完成
                  </button>
                  <button v-if="['待處理', '已預訂', '處理中', '接送中'].includes(order.status)" class="btn btn-sm btn-outline-danger" type="button" @click.prevent="updateStatus(order.transferId, 'cancel', '確定要取消此接送訂單嗎？')">
                    <i class="fa-solid fa-xmark"></i> 取消
                  </button>

                  <!-- 原本的編輯與刪除 -->
                  <button class="btn btn-sm btn-light ms-1" type="button" @click.prevent="$router.push(`/transferOrder/edit/${order.transferId}`)">
                    <i class="fa-solid fa-pen" style="color: rgb(250, 196, 123);"></i> 編輯
                  </button>
                  <button class="btn btn-sm btn-light" type="button" @click.prevent="deleteOrder(order.transferId)">
                    <i class="fa-solid fa-trash-can text-danger"></i> 刪除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredOrders.length === 0">
              <td colspan="9" class="text-center py-5 text-muted">
                <i class="fa-solid fa-van-shuttle fs-1 mb-3 text-light"></i>
                <p class="mb-0">沒有找到符合條件的訂單資料</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 分頁 Footer (純切版) -->
      <div class="card-footer bg-white border-top py-3 px-4 d-flex justify-content-between align-items-center">
        <span class="text-muted small">Showing {{ filteredOrders.length }} entries</span>
        <div class="btn-group">
          <button class="btn btn-sm btn-outline-secondary border-0"><i class="fa-solid fa-chevron-left"></i></button>
          <button class="btn btn-sm btn-outline-secondary border-0"><i class="fa-solid fa-chevron-right"></i></button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
</style>
