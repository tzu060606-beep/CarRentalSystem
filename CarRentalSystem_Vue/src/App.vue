<script setup>
import { RouterLink, RouterView } from 'vue-router'

</script>

<!-- <header class="d-flex align-items-center justify-content-between p-3 bg-light border-bottom"> -->
<!-- 左側標題 -->
<!-- <div class="titlecar fw-bold fs-4">
      <RouterLink to="/" class="text-decoration-none text-dark">租車系統</RouterLink>
    </div>
    <nav>
      <RouterLink to="/orders">租車系統</RouterLink>
      <RouterLink to="/transferOrder">專車接送</RouterLink>
      <RouterLink to="/point">兌換專區</RouterLink>
      <RouterLink to="/vehicle">車輛管理</RouterLink>
      <RouterLink to="/usedcar">二手車出售</RouterLink>
      <RouterLink to="/customers">客戶管理</RouterLink>
      <RouterLink to="/login">登入</RouterLink>
    </nav>
  </header>-->
<template>

  <div class="main-content">

    <RouterView />



  </div>
</template>

<style scoped></style>

