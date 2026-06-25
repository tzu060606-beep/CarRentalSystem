<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, RouterLink, useRoute } from 'vue-router'
import { customerAPI } from '@/api/login/customerAPI'

const router = useRouter()
const route = useRoute()

// 從 localStorage 預載會員資料
const customer = ref(JSON.parse(localStorage.getItem('customer') || 'null'))

//登出移除資訊
const handleLogout = () => {
    localStorage.removeItem('customer_token')
    localStorage.removeItem('customer')
    router.push('/customer/login')
}

// 每個項目點了才展開子選單，子選單的內容由各模組自己定義
const sections = [
    {
        label: '會員管理',
        items: [
            { label: '個人資料', icon: 'fa-user', path: '/customer/member/profile' },
            { label: '日/長租訂單', icon: 'fa-car', path: '/customer/member/order' },
            { label: '專車接送訂單', icon: 'fa-taxi', path: '/customer/member/transfer' },
            { label: '二手車買賣紀錄', icon: 'fa-car-side', path: '/customer/usedCar' },
        ]
    },
    {
        label: '點數專區',
        items: [
            { label: '點數中心', icon: 'fa-star', path: '/customer/member/pointcenter' },
            { label: '我的兌換券', icon: 'fa-ticket', path: '/customer/member/voucher' },
            { label: '點數異動紀錄', icon: 'fa-clock-rotate-left', path: '/customer/member/history' },
            { label: '兌換商品列表', icon: 'fa-gift', path: '/customer/member/products' },
        ]
    }
]

// 記錄哪個選單是展開的
const openMenu = ref('點數紀錄')  // 預設點數紀錄展開

// 最新點數
const currentPoints = ref(0)

// 監聽 CustomerProfileView 存檔後發出的事件，即時更新頭像
const onCustomerUpdated = (event) => {
    customer.value = event.detail
    currentPoints.value = event.detail.currentPoints
}

// 載入時抓最新的客戶資訊（包含點數和最新大頭貼）
onMounted(async () => {
    if (customer.value?.custId) {
        try {
            const response = await customerAPI.getById(customer.value.custId)
            customer.value = response.data
            currentPoints.value = response.data.currentPoints
            // 同步更新 localStorage 讓其他組件也能吃到最新資料
            localStorage.setItem('customer', JSON.stringify(response.data))
        } catch (error) {
            console.error('Failed to fetch customer data', error)
        }
    }
    window.addEventListener('customer-updated', onCustomerUpdated)
})

onUnmounted(() => {
    window.removeEventListener('customer-updated', onCustomerUpdated)
})

</script>

<template>
    <!-- Sidebar -->
    <aside class="member-sidebar">
        <!-- 會員資訊、選單、登出  -->
        <div class="sidebar-profile">
            <div class="sidebar-avatar">
                <img v-if="customer?.custAvatar" :src="customer.custAvatar" alt="avatar" class="avatar-image" />
                <template v-else>
                    {{ customer?.custName?.charAt(0) ?? '?' }}
                </template>
            </div>
            <div class="sidebar-info">
                <p class="sidebar-name">{{ customer?.custName }}</p>
                <p class="sidebar-account">{{ customer?.custAccount }}</p>
            </div>
        </div>


        <!-- 扁平選單，用 sections 資料驅動 -->
        <nav class="sidebar-nav">
            <div v-for="section in sections" :key="section.label" class="sidebar-section">
                <p class="sidebar-section-label">{{ section.label }}</p>
                <RouterLink v-for="item in section.items" :key="item.path" :to="item.path" class="sidebar-nav-item"
                    :class="{ active: route.path === item.path }">
                    <i :class="`fa-solid ${item.icon} me-2`"></i>
                    {{ item.label }}
                </RouterLink>
            </div>
        </nav>

        <!-- 登出 -->
        <!-- <button class="sidebar-logout" @click="handleLogout">
            <i class="fa-solid fa-right-from-bracket me-2"></i>登出
        </button> -->

    </aside>
</template>

<style scoped>
/* 會員資訊區 */
.sidebar-profile {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    margin-bottom: var(--space-4);
    padding-bottom: var(--space-4);
    border-bottom: 1px solid var(--color-border);
}

.sidebar-avatar {
    width: 40px;
    height: 40px;
    border-radius: var(--radius-full);
    background: var(--color-primary);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: var(--font-bold);
    font-size: var(--text-lg);
    flex-shrink: 0;
    overflow: hidden;
}

.avatar-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.sidebar-name {
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0;
    font-size: var(--text-sm);
}

.sidebar-account {
    color: var(--color-text-muted);
    margin: 0;
    font-size: var(--text-xs);
}


/* 選單 */
.sidebar-nav {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin-bottom: var(--space-4);
}

.sidebar-nav-item {
    display: flex;
    align-items: center;
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-md);
    color: var(--color-text-secondary);
    text-decoration: none;
    font-size: var(--text-sm);
    transition: all var(--transition-fast);
}

.sidebar-nav-item:hover {
    background: var(--color-bg-sunken);
    color: var(--color-primary);
}

.sidebar-nav-item.active {
    background: var(--color-primary-light);
    color: var(--color-primary);
    font-weight: var(--font-semibold);
}

/* 登出按鈕 */
.sidebar-logout {
    display: flex;
    align-items: center;
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-md);
    color: var(--color-danger);
    background: none;
    border: none;
    font-size: var(--text-sm);
    cursor: pointer;
    width: 100%;
    transition: background var(--transition-fast);
}

.sidebar-logout:hover {
    background: var(--color-danger-bg);
}

.sidebar-section-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    background: none;
    border: none;
    padding: var(--space-2) var(--space-3);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    font-weight: var(--font-semibold);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    cursor: pointer;
    border-radius: var(--radius-md);
}

.sidebar-section-title:hover {
    background: var(--color-bg-sunken);
}

.sidebar-section-items {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
}

.sidebar-divider {
    height: 1px;
    background: var(--color-border);
    margin: var(--space-3) 0;
}

.sidebar-section-label {
    font-size: var(--text-xs);
    font-weight: var(--font-semibold);
    color: var(--color-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    padding: var(--space-2) var(--space-3);
    margin-bottom: var(--space-1);
}

.member-sidebar {
    width: 280px;
    height: calc(100vh - 92px);
    position: sticky;
    top: 92px;
    background: var(--color-bg-surface);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    display: flex;
    flex-direction: column;
    padding: var(--space-6);
    overflow-y: auto;
    flex-shrink: 0;
    align-self: flex-start;
}

.sidebar-section-title-label {
    font-size: var(--text-xs);
    font-weight: var(--font-semibold);
    color: var(--color-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    padding: var(--space-2) var(--space-3);
    margin: 0 0 var(--space-1) 0;
}

.sidebar-nav-btn {
    width: 100%;
    background: none;
    border: none;
    cursor: pointer;
    justify-content: flex-start;
}

.sidebar-sub-items {
    display: flex;
    flex-direction: column;
    padding-left: var(--space-4);
    margin-top: var(--space-1);
}

.sidebar-nav-sub {
    font-size: var(--text-xs);
    padding: var(--space-1) var(--space-3);
}
</style>