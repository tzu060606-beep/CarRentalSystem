<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { authAPI } from '../api'

const router = useRouter()
const route = useRoute()

const employeeName = ref('')
const employeeRole = ref('')

const isCustomerMenuOpen = ref(false)
const isRentalMenuOpen = ref(false)
const isEmployeeMenuOpen = ref(false)
const isVehicleMenuOpen = ref(false)  // 車輛管理系統的開關 (預設 false 為關閉)
const isTransferMenuOpen = ref(false)
const isPointsMenuOpen = ref(false)

const isAdmin = () => {
  return employeeRole.value === 'ROLE_ADMIN' || employeeRole.value === 'ADMIN'
}

const handleLogout = async () => {
  try {
    await authAPI.employeeLogout()
  } finally {
    localStorage.removeItem('employee_token')
    localStorage.removeItem('role')
    router.push('/employee/login')
  }
}

onMounted(async () => {
  try {
    const res = await authAPI.checkLogin()
    if (res.data.loggedIn) {
      employeeName.value = res.data.employee?.empName || ''
      employeeRole.value = res.data.role || ''
    }
  } catch {
    // ignore
  }
})

const getNavClass = (isActive) => {
  return isActive
    ? 'd-flex align-items-center gap-2 p-2 rounded text-decoration-none text-small fw-bold border-start border-3'
    : 'd-flex align-items-center gap-2 p-2 rounded text-decoration-none text-small border-start border-3'
}

const getNavStyle = (isActive) => {
  return isActive
    ? 'color: var(--color-secondary-light, #d4e3ff); background-color: rgba(255, 255, 255, 0.12); border-left-color: var(--color-secondary-light, #d4e3ff); cursor: pointer; transition: all 0.2s;'
    : 'color: rgba(255, 255, 255, 0.65); border-left-color: transparent; cursor: pointer; transition: all 0.2s;'
}

const handleNavOver = (e, isActive) => {
  if (!isActive) {
    e.currentTarget.style.color = '#ffffff'
    e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.1)'
  }
}

const handleNavOut = (e, isActive) => {
  if (!isActive) {
    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.65)'
    e.currentTarget.style.backgroundColor = 'transparent'
  }
}
</script>

<template>
  <div class="d-flex min-vh-100">
    <!-- ===== 側邊欄 ===== -->
    <aside class="d-flex flex-column bg-primary position-fixed top-0 start-0 overflow-y-auto"
      style="width: 250px; min-height: 100vh; z-index: 1020; padding: 1.5rem 0.75rem;">
      <!-- 品牌區 -->
      <div class="d-flex align-items-center gap-2 px-3 mb-3">
        <i class="fa-solid fa-car-side text-white" style="font-size: 1.75rem;"></i>
        <span class="h3 fw-bold text-white mb-0" style="letter-spacing: -0.02em;">OneRent</span>
      </div>

      <!-- 使用者資訊 -->
      <div class="d-flex align-items-center gap-2 px-3 pb-3 mb-3"
        style="border-bottom: 1px solid rgba(255, 255, 255, 0.1);">
        <div class="rounded-circle d-flex align-items-center justify-content-center text-white"
          style="width: 2.5rem; height: 2.5rem; background-color: rgba(255, 255, 255, 0.15); border: 1px solid rgba(255, 255, 255, 0.2); flex-shrink: 0;">
          <i class="fa-solid fa-user"></i>
        </div>
        <div class="overflow-hidden">
          <span class="text-small fw-semibold text-white text-truncate d-block">
            {{ employeeName || 'Admin Portal' }}
          </span>
          <span class="text-caption text-truncate d-block" style="color: rgba(255, 255, 255, 0.6);">
            {{ employeeRole || 'employee' }}
          </span>
        </div>
      </div>

      <!-- 導航連結 -->
      <nav class="d-flex flex-column flex-grow-1 overflow-auto" style="gap: 0.25rem;">

        <!-- 首頁 -->
        <RouterLink to="/employee" :class="getNavClass(route.path === '/')" :style="getNavStyle(route.path === '/')"
          @mouseover="handleNavOver($event, route.path === '/')" @mouseout="handleNavOut($event, route.path === '/')">
          <i class="fa-solid fa-house text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
          <span>首頁</span>
        </RouterLink>

        <!-- ===== 客戶管理（摺疊選單）===== -->
        <div>
          <div :class="getNavClass(route.path.startsWith('/customers'))"
            :style="getNavStyle(route.path.startsWith('/customers'))"
            @mouseover="handleNavOver($event, route.path.startsWith('/customers'))"
            @mouseout="handleNavOut($event, route.path.startsWith('/customers'))"
            @click="isCustomerMenuOpen = !isCustomerMenuOpen" style="user-select: none;">
            <div class="d-flex align-items-center gap-2 flex-grow-1">
              <i class="fa-solid fa-users text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>客戶管理</span>
            </div>
            <i class="fa-solid fa-chevron-down"
              style="font-size: 0.6rem; transition: transform 0.25s; color: rgba(255, 255, 255, 0.4);"
              :style="isCustomerMenuOpen ? 'transform: rotate(180deg);' : ''"></i>
          </div>

          <div class="overflow-hidden" style="transition: max-height 0.3s ease; padding-left: 0.5rem;"
            :style="isCustomerMenuOpen ? 'max-height: 300px;' : 'max-height: 0;'">
            <RouterLink to="/customers/add"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-user-plus text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>新增客戶</span>
            </RouterLink>
            <RouterLink to="/customers/search"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-magnifying-glass text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>查詢客戶</span>
            </RouterLink>
            <RouterLink to="/customers"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-list text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>全部清單</span>
            </RouterLink>
          </div>
        </div>

        <!-- ===== 員工管理（僅 Admin 可見）===== -->
        <div v-if="isAdmin()">
          <RouterLink to="/employeesEdit" class="text-decoration-none text-white">
            <div :class="getNavClass(route.path.startsWith('/employeesEdit'))"
              :style="getNavStyle(route.path.startsWith('/employeesEdit'))"
              @mouseover="handleNavOver($event, route.path.startsWith('/employeesEdit'))"
              @mouseout="handleNavOut($event, route.path.startsWith('/employeesEdit'))">
              <div class="d-flex align-items-center gap-2 flex-grow-1">
                <i class="fa-solid fa-user-tie text-center" style="width: 1.25rem; font-size: 0.9rem;">
                </i>

                <span>員工管理</span>
              </div>
            </div>
          </RouterLink>
        </div>

        <!-- ===== 租訂系統（摺疊選單）===== -->
        <div>
          <RouterLink to="/orders"
            :class="getNavClass(route.path.startsWith('/orders') || route.path.startsWith('/plans'))"
            :style="getNavStyle(route.path.startsWith('/orders') || route.path.startsWith('/plans'))"
            @mouseover="handleNavOver($event, route.path.startsWith('/orders') || route.path.startsWith('/plans'))"
            @mouseout="handleNavOut($event, route.path.startsWith('/orders') || route.path.startsWith('/plans'))"
            @click="isRentalMenuOpen = !isRentalMenuOpen" style="user-select: none;">
            <div class="d-flex align-items-center gap-2 flex-grow-1">

              <i class="fa-solid fa-calendar text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>租訂系統</span>

            </div>
            <i class="fa-solid fa-chevron-down"
              style="font-size: 0.6rem; transition: transform 0.25s; color: rgba(255, 255, 255, 0.4);"
              :style="isRentalMenuOpen ? 'transform: rotate(180deg);' : ''"></i>
          </RouterLink>

          <div class="overflow-hidden" style="transition: max-height 0.3s ease; padding-left: 0.5rem;"
            :style="isRentalMenuOpen ? 'max-height: 300px;' : 'max-height: 0;'">
            <RouterLink to="/orders/list"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-file-invoice text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>訂單管理</span>
            </RouterLink>
            <RouterLink to="/plans" class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-file-pen text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>方案管理</span>
            </RouterLink>

          </div>
        </div>

        <!-- 點數兌換 -->
        <!-- <RouterLink
          to="/point"
          :class="getNavClass(route.path.startsWith('/point'))"
          :style="getNavStyle(route.path.startsWith('/point'))"
          @mouseover="handleNavOver($event, route.path.startsWith('/point'))"
          @mouseout="handleNavOut($event, route.path.startsWith('/point'))"
        >
          <i class="fa-solid fa-gem text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
          <span>點數兌換</span>
        </RouterLink> -->

        <!-- ===== 點數系統（改為摺疊選單）===== -->
        <div>
          <!-- 1. 點擊觸發層 (原本的 RouterLink 改為 div) -->
          <div :class="getNavClass(route.path.startsWith('/point'))"
            :style="getNavStyle(route.path.startsWith('/point'))"
            @mouseover="handleNavOver($event, route.path.startsWith('/point'))"
            @mouseout="handleNavOut($event, route.path.startsWith('/point'))"
            @click="isPointsMenuOpen = !isPointsMenuOpen; router.push('/point')" ;
            style="user-select: none; cursor: pointer;">
            <div class="d-flex align-items-center gap-2 flex-grow-1">
              <i class="fa-solid fa-gem text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>點數系統</span>
            </div>
            <!-- 旋轉箭頭 -->
            <i class="fa-solid fa-chevron-down"
              style="font-size: 0.6rem; transition: transform 0.25s; color: rgba(255, 255, 255, 0.4);"
              :style="isPointsMenuOpen ? 'transform: rotate(180deg);' : ''"></i>
          </div>

          <!-- 2. 下拉內容層 -->
          <div class="overflow-hidden" style="transition: max-height 0.3s ease; padding-left: 0.5rem;"
            :style="isPointsMenuOpen ? 'max-height: 300px;' : 'max-height: 0;'">


            <!-- 子項目:會員點數查詢 -->
            <RouterLink to="/point/customer-points"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-magnifying-glass text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>會員點數查詢</span>
            </RouterLink>


            <!-- 子項目:兌換券核銷 （操作類，高頻，移到前面）-->
            <RouterLink to="/point/verify"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-stamp text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>兌換券核銷</span>
            </RouterLink>


            <!-- 子項目:兌換訂單 -->
            <RouterLink to="/point/orders"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-clock-rotate-left text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>兌換訂單</span>
            </RouterLink>

            <!-- 子項目:兌換券 -->
            <RouterLink to="/point/vouchers"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-ticket-simple text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>兌換券</span>
            </RouterLink>

            <!-- 子項目:商品管理 -->
            <RouterLink to="/point/products"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-gifts text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>商品管理</span>
            </RouterLink>

            <!-- 子項目:點數異動稽核 -->
            <RouterLink to="/point/histories"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-circle-dollar-to-slot text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>點數異動稽核</span>
            </RouterLink>


            <!-- 子項目:點數規則（低頻，移到最後） -->
            <RouterLink to="/point/rules"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-list text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>點數規則</span>
            </RouterLink>





          </div>
        </div>

        <!-- ===== 專車接送（摺疊選單）===== -->
        <div>
          <div :class="getNavClass(route.path.startsWith('/transfer'))"
            :style="getNavStyle(route.path.startsWith('/transfer'))"
            @mouseover="handleNavOver($event, route.path.startsWith('/transfer'))"
            @mouseout="handleNavOut($event, route.path.startsWith('/transfer'))"
            @click="isTransferMenuOpen = !isTransferMenuOpen" style="user-select: none;">
            <div class="d-flex align-items-center gap-2 flex-grow-1">
              <i class="fa-solid fa-van-shuttle text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>專車接送</span>
            </div>
            <i class="fa-solid fa-chevron-down"
              style="font-size: 0.6rem; transition: transform 0.25s; color: rgba(255, 255, 255, 0.4);"
              :style="isTransferMenuOpen ? 'transform: rotate(180deg);' : ''"></i>
          </div>

          <div class="overflow-hidden" style="transition: max-height 0.3s ease; padding-left: 0.5rem;"
            :style="isTransferMenuOpen ? 'max-height: 300px;' : 'max-height: 0;'">
            <RouterLink to="/transferOrder"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-file-invoice text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>訂單管理</span>
            </RouterLink>
            <RouterLink to="/transferRate"
              class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
              style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
              onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
              onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-money-check-dollar text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
              <span>費率管理</span>
            </RouterLink>
          </div>
        </div>

        <!-- 二手車出售 -->
        <RouterLink to="/usedcar" :class="getNavClass(route.path.startsWith('/usedcar'))"
          :style="getNavStyle(route.path.startsWith('/usedcar'))"
          @mouseover="handleNavOver($event, route.path.startsWith('/usedcar'))"
          @mouseout="handleNavOut($event, route.path.startsWith('/usedcar'))">
          <i class="fa-solid fa-tags text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
          <span>二手車出售</span>
        </RouterLink>

        <!-- 車輛管理 (折疊選單) -->
        <RouterLink to="/dispatch/dashboard" :class="getNavClass(route.path.startsWith('/vehicle'))"
          :style="getNavStyle(route.path.startsWith('/vehicle'))"
          @mouseover="handleNavOver($event, route.path.startsWith('/vehicle'))"
          @mouseout="handleNavOut($event, route.path.startsWith('/vehicle'))"
          @click="isVehicleMenuOpen = !isVehicleMenuOpen" style="user-select: none;">
          <div class="d-flex align-items-center gap-2 flex-grow-1">
            <!-- <i class="fa-solid fa-car text-center" style="width: 1.25rem; font-size: 0.9rem;"></i> -->
            <font-awesome-icon icon="fa-solid fa-headset" style="width: 1.25rem; font-size: 0.9rem;" />
            <span>車輛調度中心</span>
          </div>
          <i class="fa-solid fa-chevron-down"
            style="font-size: 0.6rem; transition: transform 0.25s; color: rgba(255, 255, 255, 0.4);"
            :style="isVehicleMenuOpen ? 'transform: rotate(180deg);' : ''"></i>
        </RouterLink>

        <div class="overflow-hidden" style="transition: max-height 0.3s ease; padding-left: 0.5rem;"
          :style="isVehicleMenuOpen ? 'max-height: 300px;' : 'max-height: 0;'">
          <RouterLink to="/dispatch/task"
            class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
            style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
            onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
            onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
            <!-- <i class="fa-solid fa-repeat text-center" style="width: 1.25rem; font-size: 0.9rem;"></i> -->
            <!-- <FontAwesomeIcon :icon="byPrefixAndName.fas['repeat']" /> -->
            <font-awesome-icon icon="fa-solid fa-repeat" style="width: 1.25rem; font-size: 0.9rem;" />
            <span>調度任務管理</span>
          </RouterLink>
          <RouterLink to="/vehicle" class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
            style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
            onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
            onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
            <i class="fa-solid fa-car text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
            <span>車輛總覽</span>
          </RouterLink>
          <RouterLink to="/carmodel" class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
            style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
            onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
            onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
            <i class="fa-solid fa-tags text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
            <span>車款管理</span>
          </RouterLink>
          <RouterLink to="/crosslocationfee"
            class="d-flex align-items-center gap-2 text-decoration-none text-white opacity-75"
            style="font-size: var(--text-xs); padding: 0.35rem 0.75rem; margin-top: 0.125rem; transition: all 0.2s;"
            onmouseover="this.style.opacity='1'; this.style.backgroundColor='rgba(255,255,255,0.1)'; this.style.borderRadius='var(--radius-md)'"
            onmouseout="this.style.opacity='0.75'; this.style.backgroundColor='transparent'">
            <i class="fa-solid fa-thumbtack text-center" style="width: 1.25rem; font-size: 0.9rem;"></i>
            <span>據點與調度費率</span>
          </RouterLink>
        </div>
      </nav>

      <!-- 側邊欄底部：登出按鈕 -->
      <div class="mt-auto pt-3" style="border-top: 1px solid rgba(255, 255, 255, 0.1); padding: 0 0.25rem;">
        <button class="btn btn-outline-danger w-100 d-flex align-items-center justify-content-center gap-2 border-0"
          @click="handleLogout" id="logout-btn"
          style="padding: 0.5rem; font-size: var(--text-sm); font-weight: var(--font-medium); transition: all 0.25s;"
          onmouseover="this.classList.add('bg-danger', 'text-white')"
          onmouseout="this.classList.remove('bg-danger', 'text-white')">
          <i class="fa-solid fa-right-from-bracket"></i>
          <span>登出</span>
        </button>
      </div>
    </aside>

    <!-- ===== 右側區域（頂部列 + 主內容）===== -->
    <div class="d-flex flex-column flex-grow-1" style="margin-left: 250px; min-height: 100vh;">
      <!-- 頂部列 -->
      <header
        class="bg-white border-bottom shadow-sm position-sticky top-0 d-flex justify-content-between align-items-center"
        style="height: 64px; padding: 0 1.5rem; z-index: 1010;">
        <!-- 品牌文字（桌面端） -->
        <div class="d-none d-md-block">
          <span class="h4 fw-bold text-primary mb-0" style="letter-spacing: -0.02em;">OneRent</span>
        </div>

        <!-- 右側工具 -->
        <div class="d-flex align-items-center gap-3 ms-auto">
          <!-- 搜尋框 -->
          <div class="position-relative d-none d-md-block" style="width: 16rem;">
            <i class="fa-solid fa-magnifying-glass position-absolute text-muted"
              style="left: 0.75rem; top: 50%; transform: translateY(-50%); font-size: 0.85rem; z-index: 2;"></i>
            <input type="text" class="form-control rounded-pill text-small bg-white" placeholder="Search..."
              style="padding-left: 2.25rem; padding-top: 0.4rem; padding-bottom: 0.4rem;"
              onfocus="this.style.boxShadow='0 0 0 2px rgba(1, 80, 173, 0.12)'; this.style.borderColor='var(--color-primary)'"
              onblur="this.style.boxShadow='none'; this.style.borderColor='var(--color-border)'" />
          </div>

          <!-- 通知 + 使用者 -->
          <div class="d-flex align-items-center gap-2">
            <button class="btn btn-link text-primary rounded-circle p-2 text-decoration-none"
              style="font-size: 1.1rem; transition: background-color 0.2s;"
              onmouseover="this.style.backgroundColor='var(--color-bg-sunken)'"
              onmouseout="this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-bell"></i>
            </button>
            <button class="btn btn-link text-primary rounded-circle p-2 text-decoration-none"
              style="font-size: 1.1rem; transition: background-color 0.2s;"
              onmouseover="this.style.backgroundColor='var(--color-bg-sunken)'"
              onmouseout="this.style.backgroundColor='transparent'">
              <i class="fa-solid fa-circle-user"></i>
            </button>
          </div>
        </div>
      </header>

      <!-- 主內容區 -->
      <main class="flex-grow-1 p-3 p-lg-4" style="background-color: var(--color-bg-page); overflow-y: auto;">
        <!-- <router-view /> -->
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped></style>
