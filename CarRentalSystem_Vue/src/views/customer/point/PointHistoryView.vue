<script setup>
import { ref, computed, onMounted } from 'vue'
import { pointsHistoryAPI } from '@/api/point/point_history';

const pointsHistories = ref([]);

// ── 篩選狀態 ──
const activeType = ref('ALL')
const activePeriod = ref('6')

const typeFilters = [
    { label: '全部', value: 'ALL' },
    { label: '租車獲得', value: 'RENTAL' },
    { label: '專車獲得', value: 'TRANSFER' },
    { label: '新會員贈點', value: 'WELCOME_BONUS' },
    { label: '生日贈點', value: 'BIRTHDAY' },
    { label: '兌換消耗', value: 'REDEMPTION' },
    { label: '點數過期', value: 'EXPIRED' },
]

const periodOptions = [
    { label: '近 3 個月', value: '3' },
    { label: '近 6 個月', value: '6' },
    { label: '近 12 個月', value: '12' },
    { label: '全部時間', value: 'ALL' },
]

// ── 根據時間範圍 + 類型過濾後的資料 ──
const filteredHistories = computed(() => {
    let list = pointsHistories.value

    // 時間篩選
    if (activePeriod.value !== 'ALL') {
        const months = parseInt(activePeriod.value)
        const cutoff = new Date()
        cutoff.setMonth(cutoff.getMonth() - months)
        list = list.filter(h => new Date(h.createTime) >= cutoff)
    }

    // 類型篩選
    if (activeType.value !== 'ALL') {
        list = list.filter(h => h.changeType === activeType.value)
    }

    return list
})

// ── 統計卡片：從過濾後資料計算 ──
const statEarn = computed(() =>
    filteredHistories.value
        .filter(h => h.pointsChange > 0)
        .reduce((sum, h) => sum + h.pointsChange, 0)
)
const statSpend = computed(() =>
    filteredHistories.value
        .filter(h => h.pointsChange < 0)
        .reduce((sum, h) => sum + h.pointsChange, 0)
)
const statCount = computed(() => filteredHistories.value.length)

// ── 原有 helper function（不動）──
const getChangeTypeLabel = (changeType) => {
    const map = {
        'RENTAL': '租車獲得',
        'TRANSFER': '專車獲得',
        'BIRTHDAY': '生日贈點',
        'EXPIRED': '點數過期',
        'REDEMPTION': '兌換消耗',
        'FIRST_RENTAL': '首租獎勵',
        'WELCOME_BONUS': '新會員註冊贈點'
    }
    return map[changeType] || changeType
}

const getChangeTypeBadgeClass = (changeType) => {
    if (['RENTAL', 'TRANSFER', 'BIRTHDAY', 'FIRST_RENTAL', 'WELCOME_BONUS'].includes(changeType)) {
        return 'badge-earn'
    }
    if (changeType === 'REDEMPTION') return 'badge-redeem'
    if (changeType === 'EXPIRED') return 'badge-expired'
    return 'badge-expired'
}

const getPointsChangeDisplay = (pointsChange) => {
    if (pointsChange > 0) return `+${pointsChange.toLocaleString()}`
    return pointsChange.toLocaleString()
}

const getPointsChangeClass = (pointsChange) => {
    if (pointsChange > 0) return 'text-earn fw-bold'
    if (pointsChange < 0) return 'text-spend fw-bold'
    return 'text-secondary'
}

const formatDate = (timeStr) => {
    if (!timeStr) return '-'
    return timeStr.slice(0, 10).replace(/-/g, '/')
}

onMounted(async () => {
    const stored = JSON.parse(localStorage.getItem('customer'))
    const custId = stored?.custId
    if (!custId) return
    const response = await pointsHistoryAPI.getByCustId(custId)
    pointsHistories.value = response.data.data
})
</script>

<template>
    <div class="ph-page">

        <!-- 頁面標題 -->
        <div class="mb-4">
            <h2 class="ph-title">點數異動紀錄</h2>
            <p class="text-secondary mb-0" style="font-size: var(--text-sm);">查看每一筆點數的來源與去向</p>
        </div>

        <!-- 統計卡片 -->
        <div class="ph-stats-row mb-4">
            <div class="ph-stat-card ph-stat-earn">
                <div class="ph-stat-icon">
                    <font-awesome-icon icon="arrow-trend-up" />
                </div>
                <div>
                    <p class="ph-stat-label">期間累積</p>
                    <p class="ph-stat-value text-earn">+{{ statEarn.toLocaleString() }}</p>
                </div>
            </div>
            <div class="ph-stat-card ph-stat-spend">
                <div class="ph-stat-icon">
                    <font-awesome-icon icon="arrow-trend-down" />
                </div>
                <div>
                    <p class="ph-stat-label">期間消耗</p>
                    <p class="ph-stat-value text-spend">{{ statSpend.toLocaleString() }}</p>
                </div>
            </div>
            <div class="ph-stat-card ph-stat-count">
                <div class="ph-stat-icon">
                    <font-awesome-icon icon="list" />
                </div>
                <div>
                    <p class="ph-stat-label">異動筆數</p>
                    <p class="ph-stat-value">{{ statCount }} 筆</p>
                </div>
            </div>
        </div>

        <!-- 表格區塊 -->
        <div class="ph-table-card">

            <!-- 篩選列 -->
            <div class="ph-filter-row">
                <div class="ph-type-filters">
                    <button v-for="f in typeFilters" :key="f.value" class="ph-filter-btn"
                        :class="{ active: activeType === f.value }" @click="activeType = f.value">
                        {{ f.label }}
                    </button>
                </div>
                <select class="ph-period-select" v-model="activePeriod">
                    <option v-for="p in periodOptions" :key="p.value" :value="p.value">{{ p.label }}</option>
                </select>
            </div>

            <!-- 表格 -->
            <div class="table-responsive">
                <table class="ph-table">
                    <thead>
                        <tr>
                            <th style="width: 110px;">日期</th>
                            <th style="width: 120px;">類型</th>
                            <th>說明</th>
                            <th class="text-end" style="width: 110px;">點數異動</th>
                            <th class="text-end" style="width: 90px;">餘額</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="history in filteredHistories" :key="history.recordId">
                            <td class="text-secondary" style="font-size: var(--text-sm);">
                                {{ formatDate(history.createTime) }}
                            </td>
                            <td>
                                <span class="ph-badge" :class="getChangeTypeBadgeClass(history.changeType)">
                                    {{ getChangeTypeLabel(history.changeType) }}
                                </span>
                            </td>
                            <td style="font-size: var(--text-sm);">
                                {{ history.notes || history.referenceId || '系統自動寫入' }}
                            </td>
                            <td class="text-end" :class="getPointsChangeClass(history.pointsChange)">
                                {{ getPointsChangeDisplay(history.pointsChange) }}
                            </td>
                            <td class="text-end text-secondary" style="font-size: var(--text-sm);">
                                {{ history.remainPoints?.toLocaleString() }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 空狀態 -->
            <div v-if="filteredHistories.length === 0" class="ph-empty">
                <font-awesome-icon icon="clock-rotate-left" style="font-size: 2.5rem; color: var(--color-gray-300);" />
                <p class="mt-3 mb-0 text-secondary">目前沒有點數異動紀錄</p>
            </div>

            <!-- 底部備注 -->
            <p v-else class="ph-footnote">
                系統僅保留近 24 個月內的異動紀錄，如需更早資料，請洽客服 0800-123-456。
            </p>

        </div>
    </div>
</template>

<style scoped>
.ph-page {
    padding: var(--space-8);
}

.ph-title {
    font-size: var(--text-2xl);
    font-weight: var(--font-bold);
    color: var(--color-text-primary);
}

/* ── 統計卡片 ── */
.ph-stats-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-4);
}

.ph-stat-card {
    display: flex;
    align-items: center;
    gap: var(--space-4);
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-5) var(--space-6);
}

.ph-stat-icon {
    width: 40px;
    height: 40px;
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.1rem;
    flex-shrink: 0;
}

.ph-stat-earn .ph-stat-icon {
    background: var(--color-success-bg);
    color: var(--color-success);
}

.ph-stat-spend .ph-stat-icon {
    background: var(--color-danger-bg);
    color: var(--color-danger);
}

.ph-stat-count .ph-stat-icon {
    background: var(--color-primary-light);
    color: var(--color-primary);
}

.ph-stat-label {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    margin: 0 0 2px;
}

.ph-stat-value {
    font-size: var(--text-2xl);
    font-weight: var(--font-bold);
    color: var(--color-text-primary);
    margin: 0;
    line-height: 1.2;
}

/* ── 顏色 ── */
.text-earn {
    color: var(--color-success) !important;
}

.text-spend {
    color: var(--color-danger) !important;
}

/* ── 表格卡片 ── */
.ph-table-card {
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    overflow: hidden;
}

/* ── 篩選列 ── */
.ph-filter-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-4) var(--space-5);
    border-bottom: 1px solid var(--color-border);
    flex-wrap: wrap;
}

.ph-type-filters {
    display: flex;
    gap: var(--space-2);
    flex-wrap: wrap;
}

.ph-filter-btn {
    border: 1px solid var(--color-border);
    background: transparent;
    border-radius: var(--radius-full);
    padding: 4px 14px;
    font-size: var(--text-sm);
    cursor: pointer;
    color: var(--color-text-secondary);
    transition: all var(--transition-fast);
}

.ph-filter-btn:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
}

.ph-filter-btn.active {
    background: var(--color-primary);
    border-color: var(--color-primary);
    color: #fff;
}

.ph-period-select {
    border: 1px solid var(--color-border);
    background: var(--color-bg-surface);
    color: var(--color-text-primary);
    border-radius: var(--radius-md);
    padding: 4px 10px;
    font-size: var(--text-sm);
    cursor: pointer;
}

/* ── 表格 ── */
.ph-table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--text-sm);
}

.ph-table thead th {
    background: var(--color-bg-sunken);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    font-weight: var(--font-medium);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    padding: var(--space-3) var(--space-4);
    border-bottom: 1px solid var(--color-border);
}

.ph-table tbody td {
    padding: var(--space-3) var(--space-4);
    border-bottom: 1px solid var(--color-border);
    color: var(--color-text-primary);
    vertical-align: middle;
}

.ph-table tbody tr:last-child td {
    border-bottom: none;
}

.ph-table tbody tr:hover td {
    background: var(--color-primary-light);
}

/* ── Badge ── */
.ph-badge {
    display: inline-block;
    border-radius: var(--radius-full);
    padding: 3px 10px;
    font-size: var(--text-xs);
    font-weight: var(--font-medium);
}

.badge-earn {
    background: var(--color-success-bg);
    color: var(--color-success-hover);
}

.badge-redeem {
    background: var(--color-warning-bg);
    color: var(--color-warning-hover);
}

.badge-expired {
    background: var(--color-bg-sunken);
    color: var(--color-text-secondary);
}

/* ── 空狀態 ── */
.ph-empty {
    text-align: center;
    padding: var(--space-16) var(--space-8);
}

/* ── 備注 ── */
.ph-footnote {
    text-align: center;
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    padding: var(--space-4);
    margin: 0;
    border-top: 1px solid var(--color-border);
}
</style>