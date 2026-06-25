<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const emit = defineEmits(['navigate'])
</script>

<template>
  <div class="container-fluid py-4">
    <!-- Hero Banner -->
    <div class="card border-0 shadow-sm mb-4 bg-primary bg-gradient text-white rounded-4">
      <div class="card-body p-5 text-center">
        <i class="fa-solid fa-van-shuttle fa-3x mb-3 opacity-75"></i>
        <h1 class="fw-bold mb-2">專車接送</h1>
        <p class="mb-0 text-white-50 fs-5">Airport & Charter Transfer Service</p>
      </div>
    </div>

    <!-- 麵包屑 -->
    <nav aria-label="breadcrumb" class="mb-4">
      <ol class="breadcrumb">
        <li class="breadcrumb-item"><a href="/home" class="text-decoration-none">首頁</a></li>
        <li class="breadcrumb-item active" aria-current="page">專車接送</li>
      </ol>
    </nav>

    <!-- 主標題 -->
    <div class="text-center mb-5">
      <h2 class="fw-bold mb-3">接送服務，一站搞定</h2>
      <p class="text-muted mx-auto" style="max-width: 600px;">
        無論個人或企業，從機場接送到專屬駕駛，皆可彈性安排，讓每一段旅程輕鬆啟程。
      </p>
    </div>

    <!-- 服務卡片 A：機場接送 -->
    <div class="card border-0 shadow-sm rounded-4 mb-4 overflow-hidden">
      <div class="row g-0">
        <div class="col-md-5 bg-light d-flex align-items-center justify-content-center p-4">
          <div class="text-center">
            <i class="fa-solid fa-plane-departure fa-4x text-primary mb-3"></i>
            <p class="text-muted small mb-0">商務機場接送</p>
          </div>
        </div>
        <div class="col-md-7">
          <div class="card-body p-4">
            <h4 class="fw-bold mb-3"><i class="fa-solid fa-plane me-2 text-primary"></i>機場接送</h4>
            <p class="text-muted mb-3">
              提供桃園機場、松山機場的專車接送服務。專業司機準時接駁，讓您的出入境體驗更加舒適便利。
              支援多種車型選擇，滿足個人、家庭及商務需求。
            </p>
            <ul class="list-unstyled text-muted small mb-4">
              <li class="mb-1"><i class="fa-solid fa-check text-success me-2"></i>24小時全天候服務</li>
              <li class="mb-1"><i class="fa-solid fa-check text-success me-2"></i>航班延誤免費等候</li>
              <li class="mb-1"><i class="fa-solid fa-check text-success me-2"></i>專業禮遇舉牌服務</li>
            </ul>
            <div class="d-flex gap-2 flex-wrap">
              <button class="btn btn-primary px-4" @click="router.push('/transfer/booking/airport')">
                <i class="fa-solid fa-calendar-check me-1"></i>立即預約
              </button>
              <button class="btn btn-outline-primary px-4">
                <i class="fa-solid fa-credit-card me-1"></i>合作銀行卡友專區
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 服務卡片 B：其他接送 -->
    <div class="card border-0 shadow-sm rounded-4 mb-4 overflow-hidden">
      <div class="row g-0">
        <div class="col-md-7 order-md-1 order-2">
          <div class="card-body p-4">
            <h4 class="fw-bold mb-3"><i class="fa-solid fa-car me-2 text-primary"></i>其他接送</h4>
            <p class="text-muted mb-3">
              除了機場接送，我們也提供市區點對點專車、高鐵站接送、婚禮禮車、企業包車等多元接送服務。
              依據您的需求量身定制行程，打造專屬的移動體驗。
            </p>
            <ul class="list-unstyled text-muted small mb-4">
              <li class="mb-1"><i class="fa-solid fa-check text-success me-2"></i>市區點對點接送</li>
              <li class="mb-1"><i class="fa-solid fa-check text-success me-2"></i>高鐵站 | 火車站接送</li>
              <li class="mb-1"><i class="fa-solid fa-check text-success me-2"></i>細緻禮遇 靈活安排</li>
            </ul>
            <button class="btn btn-primary px-4" @click="router.push('/transfer/booking/other')">
              <i class="fa-solid fa-file-pen me-1"></i>填寫需求單
            </button>
          </div>
        </div>
        <div class="col-md-5 order-md-2 order-1 bg-light d-flex align-items-center justify-content-center p-4">
          <div class="text-center">
            <i class="fa-solid fa-car-side fa-4x text-primary mb-3"></i>
            <p class="text-muted small mb-0">多元接送</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
</style>
