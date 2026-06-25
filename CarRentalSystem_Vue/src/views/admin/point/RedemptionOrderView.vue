<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { redemptionOrderAPI } from '@/api/point/point_order'
import { RouterLink } from 'vue-router'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import { useRoute } from 'vue-router'

// 存兌換訂單列表
const redemptionOrders = ref([])
// 存關鍵字
const keyword = ref('')
// 存狀態篩選
const statusFilter = ref('')

// 查詢全部兌換訂單
const fetchRedemptionOrders = async () => {
   const response = await redemptionOrderAPI.getAllWithVouchers()
   redemptionOrders.value = response.data.data
}

// // 關鍵字搜尋
// const handleSearch = async () => {
//    if (keyword.value === '') {
//       await fetchRedemptionOrders()
//    } else {
//       const response = await redemptionOrderAPI.getByKeyword(keyword.value)
//       redemptionOrders.value = response.data.data
//    }
// }

// // 依狀態篩選
// const handleFilterByStatus = async () => {
//    if (statusFilter.value === '') {
//       await fetchRedemptionOrders()
//    } else {
//       const response = await redemptionOrderAPI.getByStatus(statusFilter.value)
//       redemptionOrders.value = response.data.data
//    }
// }

// 更新訂單狀態
const handleUpdateStatus = async (order, newStatus) => {
   await redemptionOrderAPI.updateStatus(order.redemptionId, newStatus)
   await fetchRedemptionOrders()

}

// 訂單狀態對照：英文 → 中文
const getOrderStatusLabel = (status) => {
   const map = {
      'ACTIVE': '待使用',
      'USED': '已使用',
      'EXPIRED': '已過期'
   }
   return map[status] || status
}

// 訂單狀態對照：英文 → badge class
const getOrderStatusBadgeClass = (status) => {
   if (status === 'ACTIVE') return 'badge bg-primary'
   if (status === 'USED') return 'badge bg-success'
   if (status === 'EXPIRED') return 'badge bg-warning'
   return 'badge bg-secondary'
}

//===========從兌換券管理跳回該筆訂單篩選畫面====================
const route = useRoute()

const redemptionIdFilter = ref('')


const filteredOrders = computed(() => {
   return redemptionOrders.value
      .filter(o => {
         // 訂單 ID 篩選（從票券頁跳過來）
         if (redemptionIdFilter.value === '') return true
         return o.redemptionId == redemptionIdFilter.value
      })
      .filter(o => {
         // 狀態篩選
         if (statusFilter.value === '') return true
         return o.orderStatus === statusFilter.value
      })
      .filter(o => {
         // 關鍵字搜尋
         if (keyword.value === '') return true
         return o.custName?.includes(keyword.value) || o.productName?.includes(keyword.value)
      })
})

// 重設篩選
const resetFilters = () => {
   redemptionIdFilter.value = ''
   statusFilter.value = ''
   keyword.value = ''
}

onMounted(async () => {
   // 注意 fetchRedemptionOrders() 前面也要加 await，
   // 確保資料載入完才執行 highlight，否則 DOM 還沒渲染找不到元素。
   await fetchRedemptionOrders()

   if (route.query.redemptionId) {
      redemptionIdFilter.value = route.query.redemptionId
   }
})
</script>

<template>
   <div>

      <!-- 返回INDEX麵包屑 -->
      <div class="mb-3">
         <Breadcrumb :items="[
            { label: '首頁', to: '/' },
            { label: '點數管理', to: '/point' },
            { label: '兌換訂單管理' }
         ]" />
      </div>

      <div class="d-flex justify-content-between align-items-center mb-4">
         <!-- 標題 -->
         <h3 class="mb-0 fw-bold">兌換訂單管理</h3>

         <!-- 資料筆數 -->
         <span class="ms-auto me-3 text-primary">
            共 {{ redemptionOrders.length }} 筆資料
         </span>
      </div>

      <!-- 操作列 -->
      <div class="d-flex mb-2 mt-2">
         <div class="p-2 d-flex gap-2">

            <!-- 狀態篩選 -->
             <!-- 狀態篩選：移除 @change，v-model 直接觸發 computed -->
            <select class="form-select w-auto" v-model="statusFilter">
               <option value="">所有狀態</option>
               <option value="ACTIVE">待使用</option>
               <option value="USED">已使用</option>
               <option value="EXPIRED">已過期</option>
            </select>

            <!-- 關鍵字搜尋 -->
             <!-- 關鍵字搜尋：移除按鈕，改成即時篩選 -->
            <input v-model="keyword" class="form-control" type="text" placeholder="請輸入關鍵字..." />
         </div>
      </div>


      <!-- 從票券頁跳過來時，顯示目前篩選的訂單編號 -->
      <div v-if="redemptionIdFilter || statusFilter || keyword" class="mb-2 d-flex align-items-center gap-2">
         <span class="badge bg-primary-subtle text-primary">
            目前篩選：訂單 #{{ redemptionIdFilter }}
         </span>
         <button @click="resetFilters" class="btn btn-sm btn-outline-secondary">
            重設篩選
         </button>
      </div>

      <!-- 兌換訂單表格 -->
      <div class="bg-white border rounded-4 shadow-sm overflow-hidden">
         <table class="table table-hover align-middle text-nowrap mb-0">
            <thead>
               <tr>
                  <th>訂單編號</th>
                  <th>客戶名稱</th>
                  <th>兌換商品</th>
                  <th>數量</th>
                  <th>花費點數</th>
                  <th>訂單狀態</th>
                  <th>建立時間</th>
                  <th>更新時間</th>
                  <th>操作</th>
                  <th>票券狀態</th>
               </tr>
            </thead>
            <tbody>
               <tr v-for="order in filteredOrders" :key="order.redemptionId" 
               :id="`order-${order.redemptionId}`">
                  <td><code>{{ order.redemptionId }}</code></td>
                  <td class="fw-bold text-primary" style="cursor: pointer;"
                     @click="$router.push(`/point/customer-points?custId=${order.custId}`)">
                     {{ order.custName }}
                  </td>
                  <td class="fw-bold">{{ order.productName }}</td>
                  <td>{{ order.productQuantity }}</td>
                  <td class="fw-bold">{{ order.pointsSpent }} 點</td>
                  <td>
                     <span :class="getOrderStatusBadgeClass(order.orderStatus)">
                        {{ getOrderStatusLabel(order.orderStatus) }}
                     </span>
                  </td>
                  <td>{{ order.createTime }}</td>
                  <td>{{ order.updateTime ?? '尚未更新' }}</td>
                  <td>
                     <!-- 只有 ACTIVE 狀態才能標記為已使用(待修正)-->
                     <button v-if="order.orderStatus === 'ACTIVE'" @click="handleUpdateStatus(order, 'USED')"
                        class="btn btn-sm btn-outline-success">
                        標記已使用
                     </button>
                     <span v-else class="text-muted">—</span>
                  </td>
                  <td>
                     <RouterLink :to="`/point/vouchers?redemptionId=${order.redemptionId}`"
                        class="text-decoration-none voucher-status-link">
                        <span v-if="order.vouchers?.length === 0">—</span>
                        <span v-else>
                           {{order.vouchers?.filter(v => v.status === 'USED').length}}
                           已使用 /
                           共 {{ order.vouchers?.length }} 張
                        </span>
                     </RouterLink>
                  </td>
               </tr>
            </tbody>
         </table>
      </div>

      <div class="d-flex mt-3">
         <span class="ms-auto me-3 text-primary">
            共 {{ redemptionOrders.length }} 筆資料
         </span>
      </div>
   </div>
</template>

<style scoped>
.voucher-status-link:hover {
   font-weight: var(--font-bold);
}
</style>