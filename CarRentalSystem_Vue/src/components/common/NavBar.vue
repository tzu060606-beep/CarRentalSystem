<script setup>
// NavBar.vue - 後台導覽列
// 使用方式：直接放在 AdminLayout.vue 或各頁面最上方
// <NavBar />

import { RouterLink } from 'vue-router'
</script>

<template>
  <nav class="navbar navbar-expand-lg" style="background: var(--color-primary); height: var(--navbar-height);">
    <div class="container-fluid px-4">
      <!-- Logo -->
      <RouterLink class="navbar-brand d-flex align-items-center gap-2" to="/">
        <span class="fw-bold text-white fs-5">OneRent</span>
        <span class="badge bg-secondary text-white" style="font-size: 0.65rem;">後台管理</span>
      </RouterLink>

      <!-- 漢堡選單（手機版） -->
      <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#adminNav">
        <span class="navbar-toggler-icon"></span>
      </button>

      <!-- 導覽連結 -->
      <div class="collapse navbar-collapse" id="adminNav">
        <ul class="navbar-nav me-auto gap-1">
         <li class="nav-item">
            <RouterLink class="nav-link text-white opacity-75" active-class="opacity-100 fw-semibold" to="/customers">
              會員管理
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link text-white opacity-75" active-class="opacity-100 fw-semibold" to="/orders">
              租車系統
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link text-white opacity-75" active-class="opacity-100 fw-semibold"
              to="/transferOrder">
              專車接送
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link text-white opacity-75" active-class="opacity-100 fw-semibold" to="/point">
              點數管理
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link text-white opacity-75" active-class="opacity-100 fw-semibold" to="/vehicle">
              車輛管理
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link text-white opacity-75" active-class="opacity-100 fw-semibold" to="/usedcar">
              二手車出售
            </RouterLink>
          </li>
        </ul>

        <!-- 右側 -->
        <div class="d-flex align-items-center gap-3">
          <span class="text-white opacity-75 small">管理員</span>
          <RouterLink to="/employee/login" class="btn btn-outline-light btn-sm">登出</RouterLink>
        </div>
      </div>
    </div>
  </nav>
</template>
