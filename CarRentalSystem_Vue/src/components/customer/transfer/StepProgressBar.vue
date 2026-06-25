<script setup>
defineProps({
  currentStep: {
    type: Number,
    default: 1
  }
})

const steps = [
  { num: 1, label: '基本資料' },
  { num: 2, label: '預約資訊' },
  { num: 3, label: '費用明細' },
  { num: 4, label: '確認訂單' },
  { num: 5, label: '線上刷卡' }
]
</script>

<template>
  <div class="step-progress-bar d-flex justify-content-center align-items-start py-4">
    <template v-for="(step, idx) in steps" :key="step.num">
      <!-- 連接線 -->
      <div v-if="idx > 0" class="step-line" :class="{ active: currentStep >= step.num }"></div>
      <!-- 節點 -->
      <div class="step-node text-center" :class="{ active: currentStep >= step.num, current: currentStep === step.num }">
        <div class="step-circle mx-auto">{{ step.num }}</div>
        <div class="step-label mt-1">{{ step.label }}</div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.step-progress-bar {
  gap: 0;
}
.step-node {
  min-width: 80px;
}
.step-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.95rem;
  background: #dee2e6;
  color: #6c757d;
  transition: all 0.3s ease;
}
.step-node.active .step-circle {
  background: #0d6efd;
  color: #fff;
  box-shadow: 0 2px 8px rgba(13, 110, 253, 0.35);
}
.step-label {
  font-size: 0.8rem;
  color: #6c757d;
  font-weight: 500;
}
.step-node.active .step-label {
  color: #0d6efd;
  font-weight: 600;
}
.step-line {
  height: 3px;
  flex: 1;
  max-width: 60px;
  background: #dee2e6;
  margin-top: 17px;
  transition: background 0.3s ease;
}
.step-line.active {
  background: #0d6efd;
}

@media (max-width: 576px) {
  .step-node {
    min-width: 56px;
  }
  .step-circle {
    width: 30px;
    height: 30px;
    font-size: 0.8rem;
  }
  .step-label {
    font-size: 0.7rem;
  }
  .step-line {
    max-width: 30px;
    margin-top: 14px;
  }
}
</style>
