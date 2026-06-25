<script setup>
// Pagination.vue - 分頁元件
// 使用方式：
// <Pagination
//   :currentPage="currentPage"
//   :totalPages="totalPages"
//   @change="currentPage = $event"
// />

import { computed } from 'vue'

const props = defineProps({
  currentPage: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  maxVisible: { type: Number, default: 5 }
})
const emit = defineEmits(['change'])

const pages = computed(() => {
  const half = Math.floor(props.maxVisible / 2)
  let start = Math.max(1, props.currentPage - half)
  let end = Math.min(props.totalPages, start + props.maxVisible - 1)
  if (end - start < props.maxVisible - 1) {
    start = Math.max(1, end - props.maxVisible + 1)
  }
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})
</script>

<template>
  <nav v-if="totalPages > 1">
    <ul class="pagination mb-0">
      <li :class="['page-item', { disabled: currentPage === 1 }]">
        <button class="page-link" @click="emit('change', currentPage - 1)">‹</button>
      </li>
      <li v-for="page in pages" :key="page" :class="['page-item', { active: page === currentPage }]">
        <button class="page-link" @click="emit('change', page)">{{ page }}</button>
      </li>
      <li :class="['page-item', { disabled: currentPage === totalPages }]">
        <button class="page-link" @click="emit('change', currentPage + 1)">›</button>
      </li>
    </ul>
  </nav>
</template>
