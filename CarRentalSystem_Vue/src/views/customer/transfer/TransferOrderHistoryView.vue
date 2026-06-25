<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/index'

const orders = ref([])
const isLoading = ref(false)

// 載入該會員的訂單
const fetchMyOrders = async () => {
  const customerStr = localStorage.getItem('customer')
  if (!customerStr) {
    alert('找不到會員登入資訊，請重新登入')
    return
  }

  try {
    isLoading.value = true
    const localCust = JSON.parse(customerStr)
    const custId = localCust.custId

    // 呼叫 API 取得會員歷史訂單
    const response = await request.get(`/transferOrder/member/${custId}?_t=${new Date().getTime()}`)
    // 讓新的訂單排在最前面 (依預約時間排序)
    orders.value = response.data.sort((a, b) => {
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
  } catch (error) {
    console.error('取得訂單失敗:', error)
    alert('無法取得訂單資訊，請稍後再試。')
  } finally {
    isLoading.value = false
  }
}

// 格式化日期與時間 (例如: 2026-05-18 14:30)
const formatDateTime = (dateString) => {
  if (!dateString) return ''
  const d = new Date(dateString)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 取消訂單
const cancelOrder = async (orderId) => {
  if (!confirm('確定要取消這筆專車接送訂單嗎？取消後將無法還原。')) return

  try {
    await request.post(`/transferOrder/${orderId}/cancel`)
    alert('訂單已成功取消')
    fetchMyOrders() // 重新整理列表
  } catch (error) {
    console.error('取消訂單失敗:', error)
    alert('取消訂單失敗，請稍後再試或聯絡客服。')
  }
}

onMounted(() => {
  fetchMyOrders()
})
</script>

<template>
  <div class="container py-4">
    <!-- 標題區塊 -->
    <div class="card shadow-sm border-0 rounded-3 mb-4">
      <div class="card-body d-flex align-items-center p-4">
        <i class="fa-solid fa-clock-rotate-left text-primary fs-2 me-3"></i>
        <div>
          <h4 class="fw-bold mb-1">我的專車接送訂單</h4>
          <p class="text-muted mb-0">查看您的預約紀錄及訂單狀態</p>
        </div>
      </div>
    </div>

    <!-- 載入中狀態 -->
    <div v-if="isLoading" class="text-center py-5 text-muted">
      <i class="fa-solid fa-spinner fa-spin fa-2x mb-3"></i>
      <p>正在載入訂單資料...</p>
    </div>

    <!-- 無訂單狀態 -->
    <div v-else-if="orders.length === 0" class="card shadow-sm border-0 rounded-3">
      <div class="card-body p-5 text-center text-muted">
        <i class="fa-solid fa-folder-open fa-3x mb-3 opacity-50"></i>
        <h5>目前沒有任何專車接送紀錄</h5>
        <p class="mb-4">您還沒有預約過專車接送服務，現在就去預約吧！</p>
        <button class="btn btn-primary px-4" @click="$router.push('/transfer')">
          前往預約專車
        </button>
      </div>
    </div>

    <!-- 訂單列表 -->
    <div v-else class="row g-4">
      <div v-for="order in orders" :key="order.transferId" class="col-12">
        <div class="card shadow-sm border-0 rounded-3 h-100 position-relative overflow-hidden">
          
          <!-- 左側狀態色條裝飾 -->
          <div class="position-absolute top-0 start-0 bottom-0" style="width: 5px;" 
               :class="{
                 'bg-warning': ['待處理', '已預訂', '處理中'].includes(order.status),
                 'bg-success': order.status === '已完成',
                 'bg-danger': order.status === '已取消',
                 'bg-info': order.status === '接送中'
               }">
          </div>

          <div class="card-body p-4 ms-2">
            
            <div class="d-flex justify-content-between align-items-start mb-3 flex-wrap gap-2">
              <div>
                <h5 class="fw-bold mb-1">訂單編號 #{{ order.transferId }}</h5>
                <small class="text-muted">預約時間: {{ formatDateTime(order.scheduledPickupTime) }}</small>
              </div>
              <div>
                <span class="badge fs-6 rounded-pill px-3 py-2"
                      :class="{
                        'text-bg-warning': ['待處理', '已預訂', '處理中'].includes(order.status),
                        'text-bg-success': order.status === '已完成',
                        'text-bg-danger': order.status === '已取消',
                        'text-bg-info': order.status === '接送中'
                      }">
                  {{ order.status }}
                </span>
                <span class="badge text-bg-primary rounded-pill px-3 py-2 ms-2">{{ order.transferType }}</span>
              </div>
            </div>

            <hr class="text-muted opacity-25">

            <div class="row g-3">
              <!-- 行程資訊 -->
              <div class="col-md-8">
                <div class="d-flex align-items-start mb-3">
                  <i class="fa-solid fa-location-dot text-success mt-1 me-3 fs-5"></i>
                  <div>
                    <span class="d-block text-muted small fw-bold mb-1">上車地點</span>
                    <span class="fw-medium">{{ order.pickupLocation }}</span>
                  </div>
                </div>
                
                <div class="d-flex align-items-start">
                  <i class="fa-solid fa-flag-checkered text-danger mt-1 me-3 fs-5"></i>
                  <div>
                    <span class="d-block text-muted small fw-bold mb-1">下車地點</span>
                    <span class="fw-medium">{{ order.dropoffLocation }}</span>
                  </div>
                </div>
              </div>
              
              <!-- 費用與車輛資訊 -->
              <div class="col-md-4 border-start-md">
                <div class="bg-light p-3 rounded-3 h-100">
                  <div class="mb-2">
                    <span class="text-muted small d-block">乘客人數</span>
                    <span class="fw-bold"><i class="fa-solid fa-users me-1 text-secondary"></i>{{ order.passengerCount }} 人</span>
                  </div>
                  <div class="mb-2" v-if="order.vehicleId">
                    <span class="text-muted small d-block">指派車號</span>
                    <span class="fw-bold text-primary"><i class="fa-solid fa-car me-1"></i>{{ order.vehicleId }}</span>
                  </div>
                  <div class="mt-3 pt-2 border-top">
                    <span class="text-muted small d-block">總費用</span>
                    <span class="fs-4 fw-bold text-danger">${{ order.totalAmount || 0 }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 取消按鈕 (只有待處理跟已預訂可以取消) -->
            <div class="text-end mt-4 pt-3 border-top" v-if="['待處理', '已預訂'].includes(order.status)">
              <button class="btn btn-outline-danger" @click="cancelOrder(order.transferId)">
                <i class="fa-solid fa-ban me-1"></i>取消訂單
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
@media (min-width: 768px) {
  .border-start-md {
    border-left: 1px solid #dee2e6;
  }
}
</style>
