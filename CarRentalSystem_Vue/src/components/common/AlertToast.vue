<script setup>
// AlertToast.vue - 成功/失敗提示
// 使用方式：
// <AlertToast ref="toast" />
// 在 script 裡：const toast = ref(null)
// toast.show('新增成功！', 'success')
// toast.show('發生錯誤', 'danger')
// toast.show('警告訊息', 'warning')
// toast.show('資訊訊息', 'info')

import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
const variant = ref('success') // success / danger / warning / info
let timer = null

// Font Awesome icon 對應
const variantIcon = {
  success: 'circle-check',
  danger: 'circle-xmark',
  warning: 'triangle-exclamation',
  info: 'circle-info'
}

const show = (msg, type = 'success', duration = 3000) => {
  message.value = msg
  variant.value = type
  visible.value = true
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { visible.value = false }, duration)
}

defineExpose({ show })
</script>

<template>
  <Teleport to="body">
    <Transition name="toast-fade">
    <div v-if="visible" :class="`alert alert-${variant} alert-dismissible d-flex align-item-start gap-2 toast-custom toast-glow-${variant}`"
      style="position: fixed; top: 20px; right: 20px; z-index: 9999; min-width: 280px; max-width: 400px; padding: 1rem 1.25rem;">
      <!-- 2. 給 icon 加上 mt-1 (或 style="margin-top: 3px;") 讓它跟第一行文字對齊得更完美 -->
      <font-awesome-icon :icon="variantIcon[variant]" class="mt-1 flex-shrink-0"  />
      <span class="toast-message me-4">{{ message }}</span>
      <!-- 3. 關閉按鈕同樣加上 mt-1，確保它也是靠右上對齊 -->
      <button type="button" class="btn-close ms-auto mt-1" @click="visible = false"></button>
    </div>
  </Transition>
  </Teleport>
</template>

<style scoped>
/* ==========================================================================
   1. 基礎與毛玻璃核心
   ========================================================================== */
.toast-custom {
  border: 1px solid rgba(255, 255, 255, 0.25);
  /* 玻璃邊緣高亮線 */
  border-radius: 14px;
  color: #ffffff !important;
  /* 🌟 重點：全面強制使用白色文字，確保對比度 */

  /* 毛玻璃效果 */
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* 確保按鈕在深色背景下也是白的 */
:deep(.btn-close) {
  filter: invert(1) grayscale(1) brightness(2);
  /* 將 Bootstrap 關閉按鈕轉為白色 */
}

.toast-message {
  white-space: pre-line;
  word-break: break-all;
  font-weight: 600;
  /* 字體加粗，在鮮豔背景上更清晰 */
  line-height: 1.5;
}

.flex-shrink-0 {
  flex-shrink: 0;
  font-size: 1.2rem;
}

/* ==========================================================================
   2. 進場與出場動畫
   ========================================================================== */
.toast-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.1);
}

.toast-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  transform: translateX(30px) scale(0.9);
  opacity: 0;
}

/* ==========================================================================
   3. 高彩度透明背景與強烈光暈 (Vibrant Glassmorphism)
   ========================================================================== */
/* 失敗/危險 - 🌟 你要的透明紅 */
.toast-glow-danger {
  background-color: rgba(220, 53, 69, 0.8);
  /* 鮮豔的 Bootstrap 紅，80% 透明度 */
  box-shadow:
    0 12px 30px -5px rgba(220, 53, 69, 0.4),
    /* 增強紅光暈 */
    0 4px 12px -2px rgba(220, 53, 69, 0.2);
}

/* 成功 - 透明綠 */
.toast-glow-success {
  background-color: rgba(25, 135, 84, 0.8);
  /* 鮮豔綠 */
  box-shadow:
    0 12px 30px -5px rgba(25, 135, 84, 0.4),
    0 4px 12px -2px rgba(25, 135, 84, 0.2);
}

/* 警告 - 透明橘黃 */
.toast-glow-warning {
  background-color: rgba(255, 159, 26, 0.85);
  /* 用飽和度較高的橘黃，不然白字會看不清 */
  box-shadow:
    0 12px 30px -5px rgba(255, 159, 26, 0.45),
    0 4px 12px -2px rgba(255, 159, 26, 0.25);
}

/* 資訊 - 透明藍 */
.toast-glow-info {
  background-color: rgba(13, 110, 253, 0.8);
  /* 鮮豔藍 */
  box-shadow:
    0 12px 30px -5px rgba(13, 110, 253, 0.4),
    0 4px 12px -2px rgba(13, 110, 253, 0.2);
}
</style>