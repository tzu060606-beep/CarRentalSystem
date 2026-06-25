<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/api/index';

// 儲存從後端取得的所有費率資料
const rates = ref([])

// ===== 搜尋條件相關變數 =====
// 關鍵字搜尋：用於比對費率名稱或車輛類型
const searchQuery = ref('')
// 狀態篩選：用於過濾是否啟用的狀態 (空字串代表全部、'true' 代表已啟用、'false' 代表未啟用)
const filterIsActive = ref('')

// ===== API 請求與資料處理 =====
// 向後端請求接送費率列表
const fetchRates = async () => {
  try {
    // 加上 ?_t=時間戳記 防止瀏覽器快取 GET 請求，確保每次都取得最新鮮的資料
    const response = await request.get('/transferRate?_t=' + new Date().getTime())
    rates.value = response.data
  } catch (error) {
    console.error('無法取得費率資料:', error)
  }
}

// 刪除指定的費率方案
const deleteRate = async (id) => {
  if (confirm('確定要刪除這筆費率方案嗎？')) {
    try {
      await request.delete(`/transferRate/${id}`)
      // 刪除成功後，重新抓取列表以更新畫面
      fetchRates()
    } catch (error) {
      console.error('刪除費率失敗:', error)
      alert('刪除失敗')
    }
  }
}

// ===== 核心過濾邏輯 =====
// computed 屬性：根據輸入的「關鍵字」與選擇的「啟用狀態」，即時過濾 rates 陣列
const filteredRates = computed(() => {
  return rates.value.filter(rate => {
    const keyword = searchQuery.value.toLowerCase()
    // 條件一：若關鍵字為空，或費率名稱/車型包含關鍵字，則視為符合
    const matchSearch = !keyword || rate.rateName.toLowerCase().includes(keyword) || rate.vehicleType.toLowerCase().includes(keyword)
    
    // 條件二：狀態篩選，若下拉選單有選擇，則比對 isActive 布林值是否相符
    let matchActive = true
    if (filterIsActive.value !== '') {
      matchActive = rate.isActive === (filterIsActive.value === 'true')
    }

    // 必須兩個條件同時符合才會顯示在畫面上
    return matchSearch && matchActive
  })
})

onMounted(() => {
  fetchRates()
})
</script>
<!-- <script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const rates = ref([])

const fetchRates = async () => {
  try {
    const response = await axios.get('http://localhost:8081/api/transferRate')
    rates.value = response.data
  } catch (error) {
    console.error(error)
  }
}

const deleteRate = async (id) => {
  if (confirm('確定要刪除嗎？')) {
    try {
      await axios.delete(`http://localhost:8081/api/transferRate/${id}`)
      fetchRates()
    } catch (error) {
      console.error(error)
      alert('刪除失敗')
    }
  }
}

onMounted(() => {
  fetchRates()
})
</script> -->

<template>
  <div class="container-fluid py-4">
    <!-- Page Header -->
    <div class="card border-0 shadow-sm mb-4 bg-primary bg-gradient text-white rounded-4">
      <div class="card-body p-4">
        <h2 class="fw-bold mb-1"><i class="fa-solid fa-money-check-dollar me-2"></i>接送費率管理</h2>
        <p class="mb-0 text-white-50">Transfer rates and pricing.</p>
      </div>
    </div>

    <!-- 搜尋與篩選工具列 -->
    <div class="card border-0 shadow-sm mb-4 rounded-4">
      <div class="card-body p-4">
        <div class="row g-3 align-items-end">
          
          <div class="col-md-5">
            <label class="form-label text-muted small fw-bold mb-1">關鍵字查詢</label>
            <div class="input-group">
              <span class="input-group-text bg-white border-end-0">
                <i class="fa-solid fa-magnifying-glass text-muted"></i>
              </span>
              <input v-model="searchQuery" type="text" class="form-control border-start-0 ps-0" placeholder="輸入費率名稱或車型...">
            </div>
          </div>

          <div class="col-md-3">
            <label class="form-label text-muted small fw-bold mb-1">啟用狀態</label>
            <select v-model="filterIsActive" class="form-select">
              <option value="">所有狀態</option>
              <option value="true">已啟用</option>
              <option value="false">未啟用</option>
            </select>
          </div>

          <div class="col text-end">
            <button class="btn btn-outline-info me-2" @click="$router.push('/home')">
              <i class="fa-solid fa-house me-1"></i>回首頁
            </button>
            <button class="btn btn-outline-secondary me-2" @click="searchQuery = ''; filterIsActive = ''">
              <i class="fa-solid fa-eraser me-1"></i>清除
            </button>
            <button class="btn btn-primary d-inline-flex align-items-center" @click="$router.push('/transferRate/add')">
                <i class="fa-solid fa-plus me-2"></i>新增費率
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
              <th class="py-3 px-4 text-muted small">費率編號 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i></th>
              <th class="py-3 px-4 text-muted small">名稱 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i></th>
              <th class="py-3 px-4 text-muted small">基本費 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i></th>
              <th class="py-3 px-4 text-muted small">每公里費 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i></th>
              <th class="py-3 px-4 text-muted small">車型 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i></th>
              <th class="py-3 px-4 text-muted small">啟用 <i class="fa-solid fa-sort ms-1" style="cursor: pointer;"></i></th>
              <th class="py-3 px-4 text-end text-muted small">操作</th>
            </tr>
          </thead>
          <tbody class="border-top-0">
            <tr v-for="rate in filteredRates" :key="rate.rateId">
              <td class="py-3 px-4 fw-bold">#{{ rate.rateId }}</td>
              <td class="py-3 px-4">{{ rate.rateName }}</td>
              <td class="py-3 px-4 text-secondary">${{ rate.baseFee }}</td>
              <td class="py-3 px-4 text-secondary">${{ rate.perKmFee }}</td>
              <td class="py-3 px-4">
                <span class="badge rounded-pill px-3 py-2 text-bg-info">{{ rate.vehicleType }}</span>
              </td>
              <td class="py-3 px-4">
                <span class="badge rounded-pill px-3 py-2" :class="rate.isActive ? 'text-bg-success' : 'text-bg-secondary'">
                  {{ rate.isActive ? '是' : '否' }}
                </span>
              </td>
              <td class="py-3 px-4 text-end">
                <div class="dropdown">
                  <button class="btn btn-sm btn-light me-2" type="button" @click.prevent="$router.push(`/transferRate/edit/${rate.rateId}`)">
                    <i class="fa-solid fa-pen" style="color: rgb(250, 196, 123);"></i> 編輯
                  </button>
                  <button class="btn btn-sm btn-light" type="button" @click.prevent="deleteRate(rate.rateId)">
                    <i class="fa-solid fa-trash-can text-danger"></i> 刪除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredRates.length === 0">
              <td colspan="7" class="text-center py-5 text-muted">
                <i class="fa-solid fa-money-check-dollar fs-1 mb-3 text-light"></i>
                <p class="mb-0">沒有找到符合條件的費率資料</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div class="card-footer bg-white border-top py-3 px-4 d-flex justify-content-between align-items-center">
        <span class="text-muted small">Showing {{ filteredRates.length }} entries</span>
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
