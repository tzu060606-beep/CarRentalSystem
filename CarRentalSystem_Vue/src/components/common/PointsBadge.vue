<script setup>
// PointsBadge.vue - 點數顯示徽章
// 使用方式：
// <PointsBadge :points="customer.currentPoints" />
// <PointsBadge :points="500" size="lg" />

const props = defineProps({
  points: { type: Number, default: 0 },
  size: { type: String, default: 'md' } // sm / md / lg
})

const sizeClass = {
  sm: 'fs-6',
  md: 'fs-5',
  lg: 'fs-4'
}
</script>

<template>
  <span class="d-inline-flex align-items-center gap-1 fw-bold" :class="sizeClass[size]"
    style="color: var(--color-accent);">
    <font-awesome-icon icon="star" style="font-size: 0.85em;" />
    {{ points.toLocaleString() }} 點
  </span>
</template>