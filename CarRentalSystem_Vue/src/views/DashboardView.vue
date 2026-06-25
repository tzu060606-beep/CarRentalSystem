<script setup>
// ============================================================
// DashboardView.vue — 後台首頁 Dashboard
// ============================================================
import { onMounted } from 'vue'
import { useDashboardStore } from '../store/dashboard'

const store = useDashboardStore()

onMounted(() => {
  store.fetchDashboardData()
})

const getTrendIcon = (dir) => {
  if (dir === 'up') return 'fa-arrow-trend-up'
  if (dir === 'down') return 'fa-arrow-trend-down'
  return 'fa-minus'
}

const getTrendClass = (dir) => {
  if (dir === 'up') return 'text-success'
  if (dir === 'down') return 'text-danger'
  return 'text-secondary'
}
</script>

<template>
  <div style="font-family: var(--font-sans);">
    <!-- ===== 頁面標題 ===== -->
    <div class="mb-4">
      <h1 class="h1 fw-bold text-primary mb-1">Dashboard Overview</h1>
      <p class="text-small text-secondary">Welcome back. Here is the latest data for OneRent.</p>
    </div>

    <!-- ===== 第一排：6 張統計卡片 ===== -->
    <div class="row g-3 mb-4">
      <div v-for="(stat, index) in store.stats" :key="index" class="col-12 col-sm-6 col-md-4 col-xl-2">
        <div class="card h-100 shadow-sm border" style="cursor: default; transition: transform 0.25s, box-shadow 0.25s;" onmouseover="this.style.transform='translateY(-4px)'; this.style.boxShadow='var(--shadow-md)'" onmouseout="this.style.transform='none'; this.style.boxShadow='var(--shadow-sm)'">
          <div class="card-body p-3">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <span class="text-caption fw-semibold text-secondary text-uppercase" style="letter-spacing: 0.05em;">{{ stat.label }}</span>
              <div class="rounded-circle d-flex align-items-center justify-content-center" :class="`bg-${stat.colorClass}-subtle text-${stat.colorClass}`" style="width: 2rem; height: 2rem; font-size: 0.85rem;">
                <i class="fa-solid" :class="stat.icon"></i>
              </div>
            </div>
            <div class="h3 fw-bold text-primary mb-1">{{ stat.value }}</div>
            <div class="d-flex align-items-center gap-1 text-small" :class="getTrendClass(stat.trendDir)">
              <i class="fa-solid" :class="getTrendIcon(stat.trendDir)" style="font-size: 0.75rem;"></i>
              <span>{{ stat.trend }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 第二排：最近訂單 + 待辦事項 ===== -->
    <div class="row g-3 mb-4">
      <!-- 左側：最近訂單 -->
      <div class="col-12 col-lg-8">
        <div class="card shadow-sm h-100 border">
          <div class="card-header bg-white border-bottom d-flex justify-content-between align-items-center p-3">
            <h5 class="h5 fw-semibold text-primary mb-0">最近訂單</h5>
            <button class="btn btn-link text-decoration-none fw-semibold text-secondary text-small p-0" onmouseover="this.classList.add('text-primary'); this.classList.remove('text-secondary')" onmouseout="this.classList.add('text-secondary'); this.classList.remove('text-primary')">View All</button>
          </div>
          <div class="table-responsive">
            <table class="table table-hover mb-0">
              <thead class="bg-light">
                <tr>
                  <th class="text-caption text-secondary fw-medium text-uppercase border-bottom">Order ID</th>
                  <th class="text-caption text-secondary fw-medium text-uppercase border-bottom">Name</th>
                  <th class="text-caption text-secondary fw-medium text-uppercase border-bottom">Plate</th>
                  <th class="text-caption text-secondary fw-medium text-uppercase border-bottom">Pickup Time</th>
                  <th class="text-caption text-secondary fw-medium text-uppercase border-bottom">Status</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in store.recentOrders" :key="order.id">
                  <td class="text-primary fw-medium font-monospace">{{ order.id }}</td>
                  <td class="text-dark">{{ order.name }}</td>
                  <td class="text-dark">{{ order.plate }}</td>
                  <td class="text-muted text-small">{{ order.pickupTime }}</td>
                  <td>
                    <span class="badge" :class="order.statusClass">{{ order.status }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 右側：待辦事項 -->
      <div class="col-12 col-lg-4">
        <div class="card shadow-sm h-100 border">
          <div class="card-body p-3">
            <h5 class="h5 fw-semibold text-primary mb-3 pb-2 border-bottom">待辦事項</h5>
            <ul class="d-flex flex-column gap-1 list-unstyled mb-0">
              <li v-for="(todo, index) in store.todos" :key="index" class="d-flex align-items-start gap-2 p-2 rounded" style="transition: background-color 0.2s;" onmouseover="this.style.backgroundColor='var(--color-bg-sunken)'" onmouseout="this.style.backgroundColor='transparent'">
                <input type="checkbox" class="form-check-input mt-1" :checked="todo.done" @change="todo.done = !todo.done" />
                <div>
                  <p class="text-small mb-0 text-dark" :class="{ 'text-decoration-line-through opacity-50': todo.done }">{{ todo.text }}</p>
                  <small class="text-caption fw-semibold text-uppercase" :class="'text-' + todo.priority">{{ todo.label }}</small>
                </div>
              </li>
            </ul>
            <button class="btn btn-outline-primary btn-sm w-100 mt-3">Add New Task</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 第三排：圖表 Placeholder ===== -->
    <div class="row g-3 mb-4">
      <div class="col-12 col-lg-6">
        <div class="card shadow-sm border">
          <div class="card-body p-3">
            <h5 class="h5 fw-semibold text-primary mb-3">Monthly Revenue Trend</h5>
            <div class="rounded d-flex flex-column align-items-center justify-content-center" style="height: 16rem; background-color: var(--color-bg-sunken); border: 1px dashed var(--color-border);">
              <i class="fa-solid fa-chart-line text-muted mb-2" style="font-size: 3rem;"></i>
              <span class="text-small text-secondary">Chart Placeholder (Line)</span>
            </div>
          </div>
        </div>
      </div>
      <div class="col-12 col-lg-6">
        <div class="card shadow-sm border">
          <div class="card-body p-3">
            <h5 class="h5 fw-semibold text-primary mb-3">Order Status Distribution</h5>
            <div class="rounded d-flex flex-column align-items-center justify-content-center" style="height: 16rem; background-color: var(--color-bg-sunken); border: 1px dashed var(--color-border);">
              <i class="fa-solid fa-chart-pie text-muted mb-2" style="font-size: 3rem;"></i>
              <span class="text-small text-secondary">Chart Placeholder (Pie)</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
