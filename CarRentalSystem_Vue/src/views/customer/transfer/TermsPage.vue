<script setup>
import { ref } from 'vue'

const emit = defineEmits(['navigate'])
const props = defineProps({
  bookingData: { type: Object, required: true }
})

const agreed1 = ref(false)
const agreed2 = ref(false)

const transferDirection = ref('toAirport') // toAirport = 市區→機場(送機), fromAirport = 機場→市區(接機)

const proceed = () => {
  // 將接送類型寫入 bookingData
  props.bookingData.transferType = transferDirection.value === 'toAirport' ? '送機' : '接機'
  emit('navigate', 'step1')
}
</script>

<template>
  <div class="container-fluid py-4">
    <!-- Hero Banner -->
    <div class="card border-0 shadow-sm mb-4 bg-primary bg-gradient text-white rounded-4">
      <div class="card-body p-5 text-center">
        <i class="fa-solid fa-file-shield fa-3x mb-3 opacity-75"></i>
        <h1 class="fw-bold mb-2">使用方式及條件確認</h1>
        <p class="mb-0 text-white-50 fs-5">Terms & Conditions</p>
      </div>
    </div>

    <!-- 接送類別切換 -->
    <div class="card border-0 shadow-sm rounded-4 mb-4">
      <div class="card-body p-4">
        <h5 class="fw-bold mb-3"><i class="fa-solid fa-route me-2 text-primary"></i>選擇接送類別</h5>
        <div class="row g-3">
          <div class="col-sm-6">
            <label class="card border-2 rounded-4 p-3 text-center w-100 h-100 d-flex flex-column align-items-center justify-content-center transfer-type-card"
                   :class="transferDirection === 'toAirport' ? 'border-primary bg-primary bg-opacity-10' : 'border-light'"
                   style="cursor: pointer;">
              <input type="radio" v-model="transferDirection" value="toAirport" class="d-none" />
              <i class="fa-solid fa-city fa-2x mb-2" :class="transferDirection === 'toAirport' ? 'text-primary' : 'text-muted'"></i>
              <i class="fa-solid fa-arrow-right mb-2" :class="transferDirection === 'toAirport' ? 'text-primary' : 'text-muted'"></i>
              <i class="fa-solid fa-plane-departure fa-2x mb-2" :class="transferDirection === 'toAirport' ? 'text-primary' : 'text-muted'"></i>
              <span class="fw-bold" :class="transferDirection === 'toAirport' ? 'text-primary' : 'text-muted'">市區 → 機場</span>
            </label>
          </div>
          <div class="col-sm-6">
            <label class="card border-2 rounded-4 p-3 text-center w-100 h-100 d-flex flex-column align-items-center justify-content-center transfer-type-card"
                   :class="transferDirection === 'fromAirport' ? 'border-primary bg-primary bg-opacity-10' : 'border-light'"
                   style="cursor: pointer;">
              <input type="radio" v-model="transferDirection" value="fromAirport" class="d-none" />
              <i class="fa-solid fa-plane-arrival fa-2x mb-2" :class="transferDirection === 'fromAirport' ? 'text-primary' : 'text-muted'"></i>
              <i class="fa-solid fa-arrow-right mb-2" :class="transferDirection === 'fromAirport' ? 'text-primary' : 'text-muted'"></i>
              <i class="fa-solid fa-city fa-2x mb-2" :class="transferDirection === 'fromAirport' ? 'text-primary' : 'text-muted'"></i>
              <span class="fw-bold" :class="transferDirection === 'fromAirport' ? 'text-primary' : 'text-muted'">機場 → 市區</span>
            </label>
          </div>
        </div>
      </div>
    </div>

    <!-- 快速連結 -->
    <div class="card border-0 shadow-sm rounded-4 mb-4">
      <div class="card-body p-4">
        <h5 class="fw-bold mb-3"><i class="fa-solid fa-link me-2 text-primary"></i>相關資訊</h5>
        <div class="row g-2">
          <div class="col-sm-6"><a href="#" class="text-decoration-none"><i class="fa-solid fa-chevron-right me-1 small"></i>連續假期費用表</a></div>
          <div class="col-sm-6"><a href="#" class="text-decoration-none"><i class="fa-solid fa-chevron-right me-1 small"></i>車型參考表</a></div>
          <div class="col-sm-6"><a href="#" class="text-decoration-none"><i class="fa-solid fa-chevron-right me-1 small"></i>機場與各縣市費用表</a></div>
          <div class="col-sm-6"><a href="#" class="text-decoration-none"><i class="fa-solid fa-chevron-right me-1 small"></i>加價地區加價費用表</a></div>
        </div>
      </div>
    </div>

    <!-- 條款文字框 -->
    <div class="card border-0 shadow-sm rounded-4 mb-4">
      <div class="card-body p-4">
        <h5 class="fw-bold mb-3"><i class="fa-solid fa-scroll me-2 text-primary"></i>機場接送尊榮禮遇</h5>
        <div class="border rounded-3 p-3 bg-light" style="max-height: 240px; overflow-y: auto;">
          <h6 class="fw-bold">服務範圍</h6>
          <p class="small text-muted">本服務適用於桃園國際機場（含第一、第二、第三航廈）及台北松山機場之接機與送機服務。服務範圍涵蓋台灣本島各縣市，部分偏遠地區可能需加收費用。</p>

          <h6 class="fw-bold">預約規範</h6>
          <p class="small text-muted">請於搭車前至少 24 小時完成線上預約，臨時預約恕無法保證車輛供應。預約成功後，系統將發送確認簡訊至您的手機號碼。如需變更或取消預約，請於搭車前 12 小時前聯繫客服。</p>

          <h6 class="fw-bold">計費方式</h6>
          <p class="small text-muted">費用依據接送地點之距離、車型及時段而定。基本費用包含高速公路過路費及停車場費用。深夜時段（23:00 - 06:00）及連續假期期間可能需加收費用，詳細價格請參考費用表。</p>

          <h6 class="fw-bold">乘客須知</h6>
          <p class="small text-muted">每趟接送服務限搭載同一組乘客，不接受併車。乘客須自行確認航班時間，如因航班延誤造成的等候，本公司將免費等候 60 分鐘。超過免費等候時間，每 30 分鐘加收 $200 元。行李數量依車型而定，超出標準行李數可能需要加訂車輛。</p>

          <h6 class="fw-bold">取消政策</h6>
          <p class="small text-muted">搭車前 12 小時以上取消，全額退費。搭車前 6-12 小時取消，收取 50% 費用。搭車前 6 小時內取消或未到，收取全額費用。</p>
        </div>
      </div>
    </div>

    <!-- 同意核取方塊 -->
    <div class="card border-0 shadow-sm rounded-4 mb-4">
      <div class="card-body p-4">
        <div class="form-check mb-3">
          <input class="form-check-input" type="checkbox" id="agree1" v-model="agreed1" />
          <label class="form-check-label" for="agree1">
            我已經詳細閱讀以上資訊，並充分明白應注意事項，願配合遵守。
            <span class="text-danger">*</span>
          </label>
        </div>
        <div class="form-check">
          <input class="form-check-input" type="checkbox" id="agree2" v-model="agreed2" />
          <label class="form-check-label" for="agree2">
            我已參閱並同意 <a href="#" class="text-decoration-none">機場接送隱私權政策</a>。
            <span class="text-danger">*</span>
          </label>
        </div>
      </div>
    </div>

    <!-- 立即預約按鈕 -->
    <div class="text-center mb-4">
      <button class="btn btn-primary btn-lg px-5 rounded-pill"
              :disabled="!agreed1 || !agreed2"
              @click="proceed">
        <i class="fa-solid fa-calendar-check me-2"></i>立即預約
      </button>
      <div v-if="!agreed1 || !agreed2" class="text-muted small mt-2">
        <i class="fa-solid fa-circle-info"></i> 請先勾選上方兩個同意項目
      </div>
    </div>
  </div>
</template>

<style scoped>
.transfer-type-card {
  transition: all 0.2s ease;
}
.transfer-type-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>
