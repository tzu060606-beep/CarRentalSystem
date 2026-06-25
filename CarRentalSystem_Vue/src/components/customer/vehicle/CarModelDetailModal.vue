<script setup>
import { ref, onMounted } from 'vue';
import * as bootstrap from 'bootstrap'; // 確保有引入 bootstrap

// 綁定 DOM 元素與 Bootstrap Modal 實體
const modalElement = ref(null);
let bsModal = null;

// 用來存放父元件傳進來的「車款」資料
const selectedCarModel = ref(null);

const openModal = (data) => {
  // 將傳進來的車款資料存入響應式變數
  selectedCarModel.value = { ...data };
  
  // 打開 Modal
  if (bsModal) {
    bsModal.show();
  }
};

onMounted(() => {
  // 初始化 Bootstrap Modal
  if (modalElement.value) {
    bsModal = new bootstrap.Modal(modalElement.value);
  }
});

// ⚠️ 非常重要：必須暴露這個方法，LandingView 的 ref 才能呼叫它
defineExpose({ openModal });
</script>

<template>
  <!-- ⚠️ 加上 ref="modalElement" 讓腳本抓得到 DOM -->
  <div class="modal fade" id="carModelDetailModal" tabindex="-1" ref="modalElement">
    <div class="modal-dialog">
      <!-- 使用 v-if 確保有資料才渲染內容，避免 undefined 錯誤 -->
      <div class="modal-content" v-if="selectedCarModel">
        <div class="modal-header">
          <h5 class="modal-title">車款詳情：{{ selectedCarModel.modelName }}</h5>
          <button class="btn btn-close" type="button" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <img v-if="selectedCarModel.vehicleImgUrl" :src="selectedCarModel.vehicleImgUrl" alt="車輛圖片" class="img-fluid mb-3"> 
          <img v-else src="https://via.placeholder.com/400x200?text=No+Image" alt="尚無圖片" class="img-fluid mb-3"> 
          
          <hr>
          <!-- 這裡改為顯示「車款(CarModel)」的專屬欄位 -->
          <p><strong>品牌：</strong>{{ selectedCarModel.brand || '無資料' }}</p>
          <p><strong>車款：</strong>{{ selectedCarModel.modelName }}</p>
          <p><strong>排氣量：</strong>{{ selectedCarModel.displacement || '無資料' }}</p>
          <p><strong>迴轉半徑：</strong>{{ selectedCarModel.turningRadius || '無資料' }}</p>
          <p><strong>車型：</strong>{{ selectedCarModel.vehicleType }}</p>
          <p><strong>座位數：</strong>{{ selectedCarModel.seats }} 人</p>
          <p><strong>行李數：</strong>{{ selectedCarModel.luggageCapacity || '無資料' }} 件</p>
          <p><strong>動力/燃料：</strong>{{ selectedCarModel.fuelType || '無資料' }}</p>
          <p><strong>變速系統：</strong>{{ selectedCarModel.transmission }}</p>
          <hr>
          <p class="text-end text-primary fw-bold fs-5">
            日租金：TWD {{ selectedCarModel.price?.toLocaleString() }} / 日起
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 你的樣式 */
</style>