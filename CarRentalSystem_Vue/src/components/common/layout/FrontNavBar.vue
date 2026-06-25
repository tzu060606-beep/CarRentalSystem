<script setup>
import { ref, onMounted, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { authAPI } from '../../../api/index.js'
import { compile } from 'sass'

const route = useRoute()
const router = useRouter()

const isLoggedIn = ref(false)
const customerName = ref('')

const checkLoginStatus = () => {
  const token = localStorage.getItem('customer_token')
  const customerStr = localStorage.getItem('customer')

  if (token && customerStr) {
    try {
      const customer = JSON.parse(customerStr)
      customerName.value = customer.custName || '顧客'
      isLoggedIn.value = true
    } catch (e) {
      console.error('讀取客戶資料失敗', e)
    }
  } else {
    isLoggedIn.value = false
    customerName.value = ''
  }
}

onMounted(() => {
  checkLoginStatus()
})

// 當網址改變時（例如從登入頁跳到首頁），重新檢查一次 localStorage
watch(() => route.path, () => {
  checkLoginStatus()
})

const handleLogout = async () => {
  try {
    await authAPI.customerlogout()
  } catch (e) {
    console.error(e)
  } finally {
    // 清除本地紀錄並登出
    localStorage.removeItem('customer_token')
    localStorage.removeItem('customer')
    isLoggedIn.value = false
    customerName.value = ''
    router.push('/')
  }
}

// 首頁導覽列使用系統中已存在的模組與路由。
// 每個主選單都帶一組 children，hover 時可展開查看子頁與實際路徑。
const navItems = [
  {
    label: '首頁',
    to: '/',
    children: [
      // { label: '首頁 Landing', to: '/' },
    ],
  },
  {
    label: '會員管理',
    to: '/customer/member/profile',
    children: [], // 先不放子選單
    // children: [
    //   { label: '個人資料', to: '/customer/profile' },
    //   { label: '日/長租訂單', to: '/customer/order' },
    //   { label: '點數紀錄', to: '/customer/point' },
    //   { label: '專車接送訂單', to: '/customer/transfer' },
    //   { label: '二手車買賣紀錄', to: '/customer/usedCar' },
    // ],
    
  },
  {
    label: '租車管理',
    to: '/rental-intro',
    children: [
      { label: '租車介紹', to: '/rental-intro' },
      { label: '車款介紹', to: '/carmodels' },
    ],
  },
  {
    label: '專車接送',
    to: '/transfer',
    children: [
      { label: '接送服務介紹', to: '/transfer' },
      { label: '機場接送', to: '/transfer/booking/airport' },
      { label: '其他接送', to: '/transfer/booking/other' },
    ],
  },
  {
    label: '聯絡我們',
    to: '/locations',
    children: [
      { label: '據點介紹', to: '/locations' },
    ],
  },
  {
    label: '點數兌換',
    to: '/point/productlist',
    // label: '點數系統',
    // to: '/customer/point',
    children: [
      // { label: '兌換商品', to: '/point/productlist' },
      // { label: '我的兌換券', to: '/point/myvoucher' },
      // { label: '兌換商品管理', to: '/point/products' },
    ],
  },
  {
    label: '中古車',
    to: '/customer/usedcar',
    children: [
      { label: '中古車首頁', to: '/usedcar' },
      { label: '中古車列表', to: '/usedcargetall' },
      { label: '銷售紀錄', to: '/salesrecordgetall' },
      { label: '賞車預約', to: '/viewingappointmentgetall' },
      { label: '中古車商城', to: '/usedcarshop' },
      { label: '車輛比較', to: '/usedcarshop/compare' },
    ],
  },
]

// 主選單與子選單都用目前 path 來判斷 active。
// 動態頁如 /vehicle/detail/:id 或 /orders/edit/:id 也會被對應到所屬模組。
const isLinkActive = (targetPath) => {
  if (targetPath === '/') return route.path === '/'
  if (targetPath === '/transfer') return route.path === '/transfer'
  return route.path === targetPath || route.path.startsWith(`${targetPath}/`)
}

const isGroupActive = (item) => item.children.some((child) => isLinkActive(child.to)) || isLinkActive(item.to)
</script>

<template>
  <header class="front-header">
    <nav class="navbar navbar-expand-lg front-navbar">
      <div class="container-fluid front-nav-inner">
        <RouterLink class="navbar-brand brand-lockup" to="/" aria-label="OneRent 首頁">
          <span class="brand-mark"></span>
          <span class="brand-text-wrap">
            <span class="brand-name"><span>One</span>Rent</span>
            <span class="brand-tagline">輕鬆租車・自在出行</span>
          </span>
        </RouterLink>

        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#frontNav"
          aria-controls="frontNav" aria-expanded="false" aria-label="切換導覽">
          <span class="navbar-toggler-icon"></span>
        </button>

        <div id="frontNav" class="collapse navbar-collapse">
          <ul class="navbar-nav mx-auto front-menu">
            <li v-for="item in navItems" :key="item.label" class="nav-item front-menu-item"
              :class="{ active: isGroupActive(item) }">
              <RouterLink class="nav-link" :class="{ active: isGroupActive(item) }" :to="item.to">
                {{ item.label }}
              </RouterLink>

              <div class="front-submenu" v-if="item.children && item.children.length > 0">
                <RouterLink
                  v-for="child in item.children"
                  :key="child.to"
                  class="front-submenu-link"
                  :class="{ active: isLinkActive(child.to) }"
                  :to="child.to"
                  
                >
                  <span>{{ child.label }}</span>
                  <small>{{ child.to }}</small>
                </RouterLink>
              </div>
            </li>
          </ul>

          <div class="front-actions">
            <!-- 未登入狀態 -->
            <template v-if="!isLoggedIn">
              <RouterLink to="/customer/login" class="login-link">
                <i class="fa-regular fa-user" aria-hidden="true"></i>
                <span>登入</span>
              </RouterLink>
              <RouterLink to="/customer/register" class="btn btn-primary register-btn">會員註冊</RouterLink>
            </template>

            <!-- 已登入狀態 -->
            <template v-else>
              <div class="d-flex align-items-center gap-3">
                <span class="fw-bold text-primary">
                  <i class="fa-regular fa-user me-1"></i>
                  {{ customerName }}，您好
                </span>
                <button @click="handleLogout" class="btn btn-outline-primary btn-sm rounded-3">登出</button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </nav>
  </header>
</template>

<style scoped>
.front-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--z-index-navbar);
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
}

.front-navbar {
  min-height: 92px;
  padding: 0;
}

.front-nav-inner {
  padding: 0 3rem;
}

.brand-lockup {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--color-text-primary);
  text-decoration: none;
}

.brand-mark {
  position: relative;
  width: 54px;
  height: 54px;
  border: 0;
  border-radius: 50%;
  background: conic-gradient(from 196deg,
      var(--color-accent) 0deg,
      var(--color-sandy-400) 34deg,
      rgba(255, 255, 255, 0.96) 66deg,
      var(--color-blue-400) 88deg,
      var(--color-primary) 108deg,
      var(--color-primary) 360deg);
  display: inline-block;
  flex: 0 0 auto;
}

.brand-mark::after {
  content: '';
  position: absolute;
  inset: 12px;
  border-radius: 50%;
  background: rgba(255, 252, 248, 0.95);
}

.brand-text-wrap {
  display: flex;
  flex-direction: column;
  line-height: 1.05;
}

.brand-name {
  color: var(--color-text-primary);
  font-size: 2rem;
  font-weight: var(--font-bold);
}

.brand-name span {
  color: var(--color-primary);
}

.brand-tagline {
  margin-top: 0.35rem;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  letter-spacing: 0.08em;
}

.front-menu {
  gap: 2.1rem;
}

.front-menu-item {
  position: relative;
}

.front-menu .nav-link {
  position: relative;
  padding: 2rem 0 1.6rem;
  color: var(--color-text-primary);
  font-size: 0.98rem;
  font-weight: var(--font-semibold);
  border-bottom: 0;
}

.front-menu .nav-link::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 1.25rem;
  height: 3px;
  background: transparent;
  border-radius: var(--radius-full);
}

.front-menu .nav-link.active,
.front-menu .nav-link:hover,
.front-menu .nav-link:focus-visible {
  color: var(--color-primary);
  border-bottom: 0;
}

.front-menu .nav-link.active::after,
.front-menu .nav-link:hover::after,
.front-menu .nav-link:focus-visible::after {
  background: var(--color-primary);
}

/* 桌機版使用 hover 展開子頁選單，讓首頁可直達系統中既有模組頁面。 */
.front-submenu {
  position: absolute;
  top: calc(100% - 0.5rem);
  left: 50%;
  min-width: 280px;
  display: grid;
  gap: 0.4rem;
  padding: 0.85rem;
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid rgba(1, 80, 173, 0.12);
  border-radius: 1rem;
  box-shadow: 0 20px 42px rgba(11, 26, 48, 0.16);
  opacity: 0;
  visibility: hidden;
  transform: translate(-50%, 0.75rem);
  transition: opacity 0.18s ease, transform 0.18s ease, visibility 0.18s ease;
  backdrop-filter: blur(10px);
}

.front-menu-item:hover .front-submenu {
/* .front-menu-item:focus-within .front-submenu { */
  opacity: 1;
  visibility: visible;
  transform: translate(-50%, 0);
}

.front-submenu-link {
  display: grid;
  gap: 0.15rem;
  padding: 0.7rem 0.85rem;
  color: var(--color-text-primary);
  text-decoration: none;
  border-radius: 0.8rem;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.front-submenu-link span {
  font-size: 0.95rem;
  font-weight: 600;
}

.front-submenu-link small {
  color: var(--color-text-muted);
  font-size: 0.74rem;
  line-height: 1.3;
}

.front-submenu-link:hover {
  color: var(--color-primary);
  background: rgba(1, 80, 173, 0.08);
}

.front-submenu-link:hover small {
  color: var(--color-primary);
}

.front-actions {
  display: flex;
  align-items: center;
  gap: 1.2rem;
}

.login-link {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  color: var(--color-primary);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  text-decoration: none;
}

.register-btn {
  min-width: 118px;
  padding: 0.8rem 1.25rem;
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  font-weight: 500 !important;
}

.front-actions .register-btn.btn.btn-primary {
  font-weight: 500 !important;
}

@media (max-width: 1199.98px) {
  .front-nav-inner {
    padding: 0 1.5rem;
  }

  .front-menu {
    gap: 1rem;
  }

  .front-menu .nav-link {
    font-size: 0.92rem;
  }
}

@media (max-width: 991.98px) {
  .front-header {
    background: rgba(255, 255, 255, 0.96);
  }

  .front-navbar {
    min-height: 76px;
  }

  .brand-mark {
    width: 42px;
    height: 42px;
  }

  .brand-mark::after {
    inset: 9px;
  }

  .brand-name {
    font-size: 1.5rem;
  }

  .brand-tagline {
    font-size: 0.72rem;
  }

  .front-menu {
    gap: 0;
    padding-top: 1rem;
  }

  .front-menu-item {
    display: block;
    padding: 0.25rem 0;
  }

  .front-menu .nav-link {
    padding: 0.75rem 0;
    font-size: 1rem;
  }

  .front-menu .nav-link::after {
    bottom: 0.35rem;
    right: auto;
    width: 2.5rem;
  }

  /* 手機版改成直接展開子頁，避免 hover 行為在觸控裝置失效。 */
  .front-submenu {
    position: static;
    left: auto;
    min-width: 0;
    margin-top: 0.35rem;
    opacity: 1;
    visibility: visible;
    transform: none;
    box-shadow: none;
    border-radius: 0.85rem;
    border-color: rgba(1, 80, 173, 0.08);
  }

  .front-actions {
    padding: 1rem 0 1.25rem;
  }
}

@media (max-width: 767.98px) {
  .front-nav-inner {
    padding: 0 1rem;
  }
}
</style>
