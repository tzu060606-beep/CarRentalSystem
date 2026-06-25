<script setup>
import FrontNavBar from '@/components/common/layout/FrontNavBar.vue';
import FrontFooter from '@/components/common/layout/FrontFooter.vue';
</script>

<template>
  <div class="front-layout">
    <FrontNavBar />
    <main class="front-main">
      <RouterView />
    </main>
    <FrontFooter />
  </div>
</template>

<style scoped>
.front-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

.front-main {
  flex: 1;
  padding-top: 92px;
  background:
    linear-gradient(
      120deg,
      rgba(250, 253, 255, 0.99) 0%,
      rgba(229, 245, 255, 0.58) 34%,
      rgba(255, 247, 232, 0.62) 68%,
      rgba(255, 250, 246, 0.7) 100%
    ),
    radial-gradient(ellipse at 50% 42%, rgba(255, 255, 255, 0.9), transparent 19rem);
}
</style>
