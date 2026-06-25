<script setup>
// NotFoundView.vue - 404 頁面
// 路由設定：
// { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFoundView }
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="d-flex flex-column align-items-center justify-content-center text-center"
    style="min-height: 100vh; background: var(--color-bg-page);">
    <div style="max-width: 480px; padding: 2rem;">
      <div class="fw-bold mb-3" style="font-size: 8rem; color: var(--color-primary); line-height: 1;">
        404
      </div>
      <font-awesome-icon icon="circle-exclamation"
        style="font-size: 3rem; color: var(--color-primary); margin-bottom: 1rem;" />
      <h2 class="fw-bold mb-2" style="color: var(--color-text-primary);">找不到這個頁面</h2>
      <p class="mb-4" style="color: var(--color-text-secondary);">
        您要找的頁面不存在，可能已被移除或網址輸入錯誤。
      </p>
      <RouterLink to="/" class="btn btn-primary px-4">
        <font-awesome-icon icon="house" class="me-2" />
        回到首頁
      </RouterLink>
    </div>
  </div>
</template>