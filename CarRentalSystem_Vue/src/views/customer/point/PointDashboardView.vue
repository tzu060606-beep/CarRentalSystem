<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { customerAPI } from '@/api/login/customerAPI'
import { pointsHistoryAPI } from '@/api/point/point_history'
import { voucherAPI } from '@/api/point/point_voucher'

const router = useRouter()

// 從 localStorage 取會員資料
const customer = computed(() => {
    const stored = localStorage.getItem('customer')
    return stored ? JSON.parse(stored) : null
})

// 目前點數（從 API 取最新值）
const currentPoints = ref(0)
// 點數異動紀錄（用來計算本月累積、即將到期）
const histories = ref([])
// 可用兌換券張數
const unusedVoucherCount = ref(0)
// 近期點數異動（只取最新 4 筆）
const recentHistories = computed(() => histories.value.slice(0, 4))

// 本月累積點數（前端自己算）
const monthlyAccumulated = computed(() => {
    const now = new Date()
    const thisYear = now.getFullYear()
    const thisMonth = now.getMonth()
    return histories.value
        .filter(h => {
            const d = new Date(h.createTime)
            // 只取本月、獲得點數的紀錄（pointsChange > 0）
            return d.getFullYear() === thisYear &&
                d.getMonth() === thisMonth &&
                h.pointsChange > 0
        })
        .reduce((sum, h) => sum + h.pointsChange, 0)
})

// 即將到期點數（30 天內，前端自己算）
const expiringPoints = computed(() => {
    const now = new Date()
    const thirtyDaysLater = new Date()
    thirtyDaysLater.setDate(now.getDate() + 30)
    return histories.value
        .filter(h => {
            if (!h.expireTime) return false
            const expiry = new Date(h.expireTime)
            // 取 expireTime 在 30 天內、pointsChange > 0 的紀錄
            return expiry >= now && expiry <= thirtyDaysLater && h.pointsChange > 0
        })
        .reduce((sum, h) => sum + h.pointsChange, 0)
})

// 即將到期的最近日期（顯示在卡片上）
const nearestExpiry = computed(() => {
    const now = new Date()
    const thirtyDaysLater = new Date()
    thirtyDaysLater.setDate(now.getDate() + 30)
    const expiring = histories.value
        .filter(h => {
            if (!h.expireTime) return false
            const expiry = new Date(h.expireTime)
            return expiry >= now && expiry <= thirtyDaysLater && h.pointsChange > 0
        })
        .map(h => h.expireTime)
        .sort()
    return expiring.length > 0 ? expiring[0].slice(0, 10) : null
})

// changeType 中文對照
const getChangeTypeLabel = (changeType) => {
    const map = {
        'RENTAL': '租車累點',
        'TRANSFER': '專車累點',
        'BIRTHDAY': '生日贈點',
        'EXPIRED': '點數過期',
        'REDEMPTION': '兌換消耗',
        'FIRST_RENTAL': '首租獎勵',
        'WELCOME_BONUS': '新會員註冊贈點'
    }
    return map[changeType] || changeType
}

// 點數變動顏色
const getPointsChangeClass = (pointsChange) => {
    if (pointsChange > 0) return 'text-success fw-bold'
    if (pointsChange < 0) return 'text-danger fw-bold'
    return 'text-secondary'
}

// 點數變動正負號
const getPointsChangeDisplay = (pointsChange) => {
    if (pointsChange > 0) return `+${pointsChange.toLocaleString()}`
    return pointsChange.toLocaleString()
}

// 時間格式化
const formatTime = (timeStr) => {
    if (!timeStr) return '-'
    return timeStr.slice(0, 10)
}

onMounted(async () => {
    const stored = JSON.parse(localStorage.getItem('customer'))
    const custId = stored?.custId
    if (!custId) return

    // 同時打三支 API
    const [customerRes, historyRes, voucherRes] = await Promise.all([
        customerAPI.getById(custId),
        pointsHistoryAPI.getByCustId(custId),
        voucherAPI.getByCustId(custId)
    ])

    // 目前點數
    currentPoints.value = customerRes.data.currentPoints
    // 點數異動紀錄
    histories.value = historyRes.data.data
    // 可用兌換券張數（只算 UNUSED）
    unusedVoucherCount.value = voucherRes.data.data.filter(v => v.status === 'UNUSED').length
})

//================點數明細與點數效期TAB分頁==================
// Tab 控制
const activeTab = ref('history') // 'history' | 'expiry'

// 點數效期：依 expireTime 分組，同一天到期的點數合併加總
const expiryRecords = computed(() => {
    const grouped = {}
    histories.value
        .filter(h => h.availablePoints > 0)
        .forEach(h => {
            const dateKey = h.expireTime?.slice(0, 10)
            if (!dateKey) return
            if (!grouped[dateKey]) {
                grouped[dateKey] = { expireTime: dateKey, totalPoints: 0 }
            }
            grouped[dateKey].totalPoints += h.availablePoints
        })
    // 轉成陣列並按到期日升冪排序
    return Object.values(grouped).sort((a, b) =>
        new Date(a.expireTime) - new Date(b.expireTime)
    )
})

//=============近期點數異動篩選按鈕==============================
const historyFilter = ref('all') // 'all' | 'earn' | 'spend'

const filteredRecentHistories = computed(() => {
    let list = histories.value
    if (historyFilter.value === 'earn') list = list.filter(h => h.pointsChange > 0)
    if (historyFilter.value === 'spend') list = list.filter(h => h.pointsChange < 0)
    return list.slice(0, 4)
})

</script>

<template>
    <div class="point-dashboard-page">

        <!-- ── 頁面標題 ── -->
        <div class="mb-4">
            <h1 class="point-dashboard-page__title mb-1">點數中心</h1>
            <p class="text-secondary mb-0" style="font-size: var(--text-sm);">
                管理您的 OneRent 點數、兌換券與每一筆異動
            </p>
        </div>

        <!-- ── 主要點數卡片 ── -->
        <div class="points-hero mb-4">
            <div class="points-hero__label">ONERENT POINTS</div>
            <div class="points-hero__value">
                {{ currentPoints.toLocaleString() }}
                <span class="points-hero__unit">點</span>
            </div>
            <div class="points-hero__stats">
                <div class="points-hero__stat">
                    <span class="points-hero__stat-label">本月累積</span>
                    <span class="points-hero__stat-value text-success">
                        +{{ monthlyAccumulated.toLocaleString() }}
                    </span>
                </div>
                <div class="points-hero__stat">
                    <span class="points-hero__stat-label">即將到期</span>
                    <span class="points-hero__stat-value text-warning">
                        {{ expiringPoints.toLocaleString() }}
                    </span>
                    <span v-if="nearestExpiry" class="points-hero__stat-sub">
                        {{ nearestExpiry }} 到期
                    </span>
                </div>
                <div class="points-hero__stat">
                    <span class="points-hero__stat-label">可用兌換券</span>
                    <span class="points-hero__stat-value">{{ unusedVoucherCount }}</span>
                    <span class="points-hero__stat-sub">張</span>
                </div>
            </div>
        </div>

        <!-- ── 快捷入口 ── -->
        <div class="row g-3 mb-4">
            <div class="col-6 col-md-3">
                <div class="shortcut-card" @click="$router.push('/customer/member/products')">
                    <i class="fa-solid fa-gift shortcut-card__icon"></i>
                    <span class="shortcut-card__label">兌換商品</span>
                    <i class="fa-solid fa-chevron-right shortcut-card__arrow"></i>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="shortcut-card" @click="$router.push('/customer/member/voucher')">
                    <i class="fa-solid fa-ticket shortcut-card__icon"></i>
                    <span class="shortcut-card__label">我的兌換券</span>
                    <span class="shortcut-card__badge" v-if="unusedVoucherCount > 0">
                        {{ unusedVoucherCount }}
                    </span>
                    <i class="fa-solid fa-chevron-right shortcut-card__arrow"></i>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="shortcut-card" @click="$router.push('/customer/member/history')">
                    <i class="fa-solid fa-clock-rotate-left shortcut-card__icon"></i>
                    <span class="shortcut-card__label">點數紀錄</span>
                    <i class="fa-solid fa-chevron-right shortcut-card__arrow"></i>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="shortcut-card" @click="$router.push('/customer/member/products')">
                    <i class="fa-solid fa-store shortcut-card__icon"></i>
                    <span class="shortcut-card__label">點數商城</span>
                    <i class="fa-solid fa-chevron-right shortcut-card__arrow"></i>
                </div>
            </div>
        </div>

      <!-- ── Tab 標題列 ── -->
        <div class="mb-3 d-flex justify-content-between align-items-center">
            <div class="dashboard-tabs">
                <button class="dashboard-tab" :class="{ active: activeTab === 'history' }"
                    @click="activeTab = 'history'">
                    近期點數異動
                </button>
                <button class="dashboard-tab" :class="{ active: activeTab === 'expiry' }" @click="activeTab = 'expiry'">
                    點數效期
                </button>
            </div>
            <span v-if="activeTab === 'history'" class="text-primary"
                style="font-size: var(--text-sm); cursor: pointer;" @click="$router.push('/customer/member/history')">
                查看全部 →
            </span>
        </div>

        
        <!-- ── 近期點數異動 Tab ── -->
        <div v-if="activeTab === 'history'" class="recent-history">

            <!-- 篩選按鈕 -->
              <div class="d-flex gap-2 mb-3">
                  <button class="filter-pill" :class="{ 'filter-pill--active': historyFilter === 'all' }"
                      @click="historyFilter = 'all'">全部</button>
                  <button class="filter-pill" :class="{ 'filter-pill--earn': historyFilter === 'earn' }"
                      @click="historyFilter = 'earn'">獲點</button>
                  <button class="filter-pill" :class="{ 'filter-pill--spend': historyFilter === 'spend' }"
                      @click="historyFilter = 'spend'">扣點</button>
              </div>

              <!-- 點數異動紀錄 -->
            <div v-for="history in filteredRecentHistories" :key="history.recordId" class="recent-history__item">
                <span class="badge me-3"
                    :class="history.pointsChange > 0 ? 'bg-success' : history.changeType === 'EXPIRED' ? 'bg-secondary' : 'bg-warning'">
                    {{ getChangeTypeLabel(history.changeType) }}
                </span>
                <div class="recent-history__body">
                    <p class="mb-0" style="font-size: var(--text-sm);">
                        {{ history.notes || history.referenceId || '-' }}
                    </p>
                    <span style="font-size: var(--text-xs); color: var(--color-text-muted);">
                        {{ formatTime(history.createTime) }}
                    </span>
                </div>
                <span :class="getPointsChangeClass(history.pointsChange)" style="font-size: var(--text-sm);">
                    {{ getPointsChangeDisplay(history.pointsChange) }}
                </span>
            </div>
            <div v-if="histories.length === 0" class="text-center py-4">
                <p class="text-secondary">目前沒有點數異動紀錄</p>
            </div>
        </div>

      <!-- ── 點數效期 Tab ── -->
        <div v-if="activeTab === 'expiry'">
            <div class="d-flex justify-content-between px-3 py-2 text-secondary" style="font-size: var(--text-xs);">
                <span>到期日</span>
                <span>剩餘點數</span>
            </div>
            <div class="recent-history">
                <div v-for="record in expiryRecords" :key="record.expireTime" class="recent-history__item">
                    <div class="recent-history__body">
                        <p class="mb-0 fw-medium" style="font-size: var(--text-sm);">
                            {{ record.expireTime }} 到期
                        </p>
                    </div>
                    <span class="fw-bold text-primary" style="font-size: var(--text-sm);">
                        {{ record.totalPoints.toLocaleString() }} 點
                    </span>
                </div>
                <div v-if="expiryRecords.length === 0" class="text-center py-4">
                    <p class="text-secondary">目前沒有有效點數</p>
                </div>
            </div>
        </div>

    </div>
</template>

<style scoped>
.point-dashboard-page {
    padding: var(--space-8);
}

/* 頁面標題 */
.point-dashboard-page__title {
    font-size: var(--text-2xl);
    font-weight: var(--font-bold);
    color: var(--color-text-primary);
    display: inline-block;
    border-bottom: 3px solid var(--color-primary);
    padding-bottom: var(--space-1);
}

/* 主要點數卡片 */
.points-hero {
    background: linear-gradient(135deg, var(--color-blue-700) 0%, var(--color-blue-600) 60%, var(--color-blue-500) 100%);
    border-radius: var(--radius-xl);
    padding: var(--space-8);
    color: white;
    position: relative;
    overflow: hidden;
}

.points-hero::after {
    content: '';
    position: absolute;
    right: -40px;
    top: -40px;
    width: 200px;
    height: 200px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.05);
}

.points-hero__label {
    font-size: var(--text-xs);
    letter-spacing: 0.1em;
    opacity: 0.7;
    margin-bottom: var(--space-2);
}

.points-hero__value {
    font-size: 3rem;
    font-weight: var(--font-bold);
    line-height: 1;
    margin-bottom: var(--space-6);
    font-family: var(--font-mono);
}

.points-hero__unit {
    font-size: var(--text-xl);
    font-weight: var(--font-normal);
    margin-left: var(--space-2);
}

.points-hero__stats {
    display: flex;
    gap: var(--space-8);
}

.points-hero__stat {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.points-hero__stat-label {
    font-size: var(--text-xs);
    opacity: 0.7;
}

.points-hero__stat-value {
    font-size: var(--text-xl);
    font-weight: var(--font-bold);
}

.points-hero__stat-sub {
    font-size: var(--text-xs);
    opacity: 0.7;
}

/* 快捷入口卡片 */
.shortcut-card {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-4);
    cursor: pointer;
    transition: all var(--transition-fast);
    position: relative;
}

.shortcut-card:hover {
    box-shadow: var(--shadow-md);
    border-color: var(--color-primary);
    transform: translateY(-1px);
}

.shortcut-card__icon {
    font-size: 1.25rem;
    color: var(--color-primary);
    flex-shrink: 0;
}

.shortcut-card__label {
    font-size: var(--text-sm);
    font-weight: var(--font-medium);
    color: var(--color-text-primary);
    flex: 1;
}

.shortcut-card__arrow {
    font-size: 0.75rem;
    color: var(--color-text-muted);
}

.shortcut-card__badge {
    position: absolute;
    top: -6px;
    right: -6px;
    background: var(--color-danger);
    color: white;
    font-size: 0.7rem;
    font-weight: var(--font-bold);
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* 近期異動 */
.recent-history {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
}

.recent-history__item {
    display: flex;
    align-items: center;
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-3) var(--space-4);
}

.recent-history__body {
    flex: 1;
}

/* tab樣式 */
/* Dashboard Tab */
.dashboard-tabs {
    display: flex;
    gap: var(--space-4);
    border-bottom: 1px solid var(--color-border);
}

.dashboard-tab {
    background: none;
    border: none;
    padding: var(--space-2) 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    cursor: pointer;
    border-bottom: 2px solid transparent;
    margin-bottom: -1px;
    transition: all var(--transition-fast);
}

.dashboard-tab:hover {
    color: var(--color-primary);
}

.dashboard-tab.active {
    color: var(--color-primary);
    font-weight: var(--font-semibold);
    border-bottom-color: var(--color-primary);
}

/* 膠囊篩選按鈕 */
.filter-pill {
    border-radius: var(--radius-full);
    border: 1px solid var(--color-border-strong);
    background: white;
    color: var(--color-text-secondary);
    padding: var(--space-1) var(--space-4);
    font-size: var(--text-sm);
    cursor: pointer;
    transition: all var(--transition-fast);
}

.filter-pill--active {
    border-color: var(--color-primary);
    background: var(--color-primary-light);
    color: var(--color-primary);
    font-weight: var(--font-medium);
}

.filter-pill--earn {
    border-color: var(--color-success);
    background: var(--color-green-100);
    color: var(--color-success);
    font-weight: var(--font-medium);
}

.filter-pill--spend {
    border-color: var(--color-warning-hover);
    background: var(--color-warning-bg);
    color: var(--color-warning-hover);
    font-weight: var(--font-medium);
}

.filter-pill:hover:not(.filter-pill--active):not(.filter-pill--earn):not(.filter-pill--spend) {
    border-color: var(--color-primary);
    color: var(--color-primary);
}
</style>