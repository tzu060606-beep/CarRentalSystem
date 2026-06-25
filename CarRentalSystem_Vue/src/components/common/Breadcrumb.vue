<script setup>
// Breadcrumb.vue - 麵包屑導覽
// 使用方式：
// <Breadcrumb :items="[
//   { label: '首頁', to: '/' },
//   { label: '點數管理', to: '/point' },
//   { label: '商品管理' }
// ]" />
// 最後一個 item 不需要 to，會自動顯示為目前頁面（不可點擊）

import { RouterLink } from 'vue-router'

const props = defineProps({
  items: {
    type: Array,
    required: true
    // [{ label: String, to?: String }]
  }
})
</script>

<template>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb mb-0">
      <li
        v-for="(item, index) in items"
        :key="index"
        :class="['breadcrumb-item', { active: index === items.length - 1 }]"
      >
        <!-- 最後一個項目：不可點擊，顯示為目前頁面 -->
        <span v-if="index === items.length - 1" style="color: var(--color-text-secondary);">
          {{ item.label }}
        </span>
        <!-- 其他項目：RouterLink -->
        <RouterLink v-else :to="item.to" style="color: var(--color-primary);">
          {{ item.label }}
        </RouterLink>
      </li>
    </ol>
  </nav>
</template>
