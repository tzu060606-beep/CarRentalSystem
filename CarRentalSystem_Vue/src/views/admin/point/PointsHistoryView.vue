<script setup>

import { ref, onMounted } from 'vue'
import { pointsHistoryAPI } from '@/api/point/point_history';
import PointIndexView from './PointIndexView.vue';
import { RouterLink } from 'vue-router'
import Breadcrumb from '@/components/common/Breadcrumb.vue'

//存點數異動紀錄表
const pointsHistories = ref([]);
//存關鍵字
const keyword = ref();

// 查詢全部點數異動
const fetchPointsHistories = async () => {
   const response = await pointsHistoryAPI.getAll()
   pointsHistories.value = response.data.data
}


// 執行查詢方法
// 非同步函式，不需要傳入參數，因為要搜尋的關鍵字已經存在 keyword 這個 ref 變數裡
const handleSearch = async () => {

   // 【A】如果搜尋框是空的，就重新載入全部資料，等於清空搜尋。
   if (keyword.value === '') {
      await fetchPointsHistories();
   } else {
      // 【B】如果有輸入關鍵字，就呼叫關鍵字搜尋 API，把結果存進 pointsHistories，畫面自動更新。
      const response = await pointsHistoryAPI.getByKeyword(keyword.value);
      pointsHistories.value = response.data.data;

   }
}

//點數變動紀錄頁面是唯讀的列表頁，不需要點進去看單筆詳細，這裡不需要getPointsHistoryById

// changeType 現在存的是英文（RENTAL、REDEMPTION 等），前端顯示時要轉成中文。//對照函式
const getChangeTypeLabel = (changeType) => {
   const map = {
      'RENTAL': '租車累點',
      'TRANSFER': '專車累點',
      'BIRTHDAY': '生日贈點',
      'EXPIRED': '點數過期',
      'REDEMPTION': '兌換消耗',
      'FIRST_RENTAL': '首租獎勵',
      'WELCOME_BONUS': '新會員註冊贈點'
   }
   return map[changeType] || changeType
}

//changeType增加Badge樣式
const getChangeTypeBadgeClass = (changeType) => {
   if (changeType === 'RENTAL' || changeType === 'TRANSFER' || changeType === 'BIRTHDAY') {
      return 'badge bg-success'   // 獲得點數 → 綠色
   }
   if (changeType === 'REDEMPTION') {
      return 'badge bg-warning'   // 兌換消耗 → 黃色
   }
   if (changeType === 'EXPIRED') {
      return 'badge bg-secondary' // 點數過期 → 灰色
   }
   return 'badge bg-secondary'
}


// 時間格式化：2026-01-15T10:00:00 → 2026/01/15 10:00:00
const formatDateTime = (timeStr) => {
   if (!timeStr) return '-'
   return timeStr.slice(0, 19).replace('T', ' ')
}

onMounted(() => {
   fetchPointsHistories()
})

</script>
<template>


   <div>
      <!-- 返回INDEX麵包屑 -->
      <div class="mb-3">
         <Breadcrumb :items="[
            { label: '首頁', to: '/' },
            { label: '點數管理', to: '/point' },
            { label: '點數異動稽核' }
         ]" />
      </div>
      <!-- 標題 -->
      <div class="d-flex justify-content-between align-items-center mb-4">
         <h3 class="mb-0 fw-bold">點數異動稽核</h3>
         <!-- 資料筆數 -->
         <span class="ms-auto me-3 text-primary">
            共 {{ pointsHistories.length }} 筆資料
         </span>
      </div>


      <!-- 操作列 -->
      <div class="d-flex mb-2 mt-2 ">

         <div class="p-2 d-flex gap-2">

            <!-- 上架狀態下拉選單 -->
            <!-- <select class="form-select w-auto" aria-label="Default select example" v-model="isActiveFilter"
                    @change="handleFilterByIsActive">
                    <option value="">所有上架狀態</option>
                    <option value="true">上架中</option>
                    <option value="false">已下架</option>
                </select> -->

            <!-- 搜尋按鈕與搜尋列 -->
            <input v-model="keyword" class="form-control" type="text" placeholder="請輸入點數異動紀錄關鍵字..." />
            <button @click="handleSearch" class="btn btn-primary text-nowrap">
               搜尋</button>
            <!-- text-nowrap：應用在 button 上，防止在視窗縮小時文字被擠壓換行。-->

         </div>
      </div>


      <!-- 點數規則表格 -->
      <div class="bg-white border rounded-4 shadow-sm overflow-hidden">
         <table class="table table-hover align-middle text-nowrap mb-0">
            <thead>
               <tr>
                  <th>紀錄編號</th>
                  <th>客戶編號</th>
                  <th>客戶姓名</th>
                  <th>異動類型</th>
                  <th>異動點數</th>
                  <th>剩餘點數</th>
                  <th>來源單號</th>
                  <th>備註</th>
                  <th>建立時間</th>
                  <th>到期時間</th>
                  <!-- <th>操作</th> -->
               </tr>
            </thead>
            <tbody>
               <tr v-for="pointsHistory in pointsHistories" :key="pointsHistory.recordId">
                  <td>
                     <code>{{ pointsHistory.recordId }}</code>
                  </td>
                  <td class="fw-bold fs-6 text-primary">
                     {{ pointsHistory.custId }}
                  </td>
                  <td>{{ pointsHistory.custName }}</td>
                  <td class="fw-bold fs-6 text-primary">
                     <span :class="getChangeTypeBadgeClass(pointsHistory.changeType)">
                        {{ getChangeTypeLabel(pointsHistory.changeType) }}
                     </span>
                  </td>
                  <td class="fw-bold fs-6 text-primary">
                     {{ pointsHistory.pointsChange }}
                  </td>
                  <td class="fw-bold">{{ pointsHistory.remainPoints }} </td>
                  <td>{{ pointsHistory.referenceId }} </td>
                  <td>{{ pointsHistory.notes }}</td>
                  <td>{{ formatDateTime(pointsHistory.createTime) }}</td>
                  <td>{{ formatDateTime(pointsHistory.expireTime) }}</td>


               </tr>
            </tbody>
         </table>
      </div>

      <div class="d-flex mt-3">
         <span class="ms-auto me-3 text-primary">
            共 {{ pointsHistories.length }} 筆資料
         </span>
      </div>


   </div>


</template>
<style scoped></style>