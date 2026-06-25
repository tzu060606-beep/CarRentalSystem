<script setup>
// EmptyState.vue - 空狀態提示
// 使用方式：
// <EmptyState message="目前沒有資料" />
// <EmptyState icon="inbox" message="目前沒有訂單" subMessage="兌換後將在此顯示" />
// icon 使用 Font Awesome solid 圖示名稱，例如：inbox / box-open / magnifying-glass / folder-open

const props = defineProps({
  icon: { type: String, default: 'inbox' },
  message: { type: String, default: '目前沒有資料' },
  subMessage: { type: String, default: '' }
})
</script>

<template>
  <div class="d-flex flex-column align-items-center justify-content-center py-5 gap-3 text-center">
    <font-awesome-icon :icon="icon" style="font-size: 3rem; color: var(--color-gray-300);" />
    <div>
      <p class="fw-semibold mb-1" style="color: var(--color-text-secondary);">{{ message }}</p>
      <p v-if="subMessage" class="small mb-0" style="color: var(--color-text-muted);">{{ subMessage }}</p>
    </div>
    <slot />
  </div>
</template>