<script setup>
// LoadingSpinner.vue - 載入中 Spinner
// 使用方式：
// <LoadingSpinner :isLoading="isLoading" />
// <LoadingSpinner :isLoading="isLoading" text="資料載入中..." />
// overlay 模式（遮住整個頁面）：
// <LoadingSpinner :isLoading="isLoading" overlay />

const props = defineProps({
  isLoading: { type: Boolean, default: false },
  text: { type: String, default: '載入中...' },
  overlay: { type: Boolean, default: false }
})
</script>

<template>
  <!-- Overlay 模式 -->
  <Teleport to="body" v-if="overlay">
    <div
      v-if="isLoading"
      class="d-flex justify-content-center align-items-center flex-column gap-3"
      style="position: fixed; inset: 0; background: rgba(11,26,48,0.4); z-index: 9998;"
    >
      <div class="spinner-border text-light" style="width: 3rem; height: 3rem;" role="status"></div>
      <span class="text-light fw-semibold">{{ text }}</span>
    </div>
  </Teleport>

  <!-- Inline 模式 -->
  <div v-if="!overlay && isLoading" class="d-flex justify-content-center align-items-center gap-2 py-4">
    <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
    <span class="text-secondary">{{ text }}</span>
  </div>
</template>
