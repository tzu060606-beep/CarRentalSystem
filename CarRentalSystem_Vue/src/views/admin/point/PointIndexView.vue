<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { pointsHistoryAPI } from '@/api/point/point_history'
import { voucherAPI } from '@/api/point/point_voucher'
import { redemptionOrderAPI } from '@/api/point/point_order'
import EmptyState from '@/components/common/EmptyState.vue'

// ── 資料 ──
const histories = ref([])
const vouchers = ref([])
const orders = ref([])

// ── 統計計算 ──

// 本月發出點數（pointsChange > 0 的加總）
const monthlyIssued = computed(() => {
    const now = new Date()
    return histories.value
        .filter(h => {
            const d = new Date(h.createTime)
            return d.getFullYear() === now.getFullYear() &&
                d.getMonth() === now.getMonth() &&
                h.pointsChange > 0
        })
        .reduce((sum, h) => sum + h.pointsChange, 0)
})

// 本月消耗點數（REDEMPTION 類型的加總，取絕對值）
const monthlyRedeemed = computed(() => {
    const now = new Date()
    return histories.value
        .filter(h => {
            const d = new Date(h.createTime)
            return d.getFullYear() === now.getFullYear() &&
                d.getMonth() === now.getMonth() &&
                h.changeType === 'REDEMPTION'
        })
        .reduce((sum, h) => sum + Math.abs(h.pointsChange), 0)
})

// 待使用兌換券張數
const unusedVoucherCount = computed(() =>
    vouchers.value.filter(v => v.status === 'UNUSED').length
)

// 本月新增兌換訂單數
const monthlyOrderCount = computed(() => {
    const now = new Date()
    return orders.value.filter(o => {
        const d = new Date(o.createTime)
        return d.getFullYear() === now.getFullYear() &&
            d.getMonth() === now.getMonth()
    }).length
})

// 近期兌換訂單（最新 5 筆）
const recentOrders = computed(() => orders.value.slice(0, 5))

//==============即將到期點數紀錄=====================
// 即將到期點數警示（30 天內，依客戶分組）
const expiringCustomers = computed(() => {
    // 取得今天和 30 天後的日期，用來篩選即將到期的點數
    const now = new Date()
    const thirtyDaysLater = new Date()
    thirtyDaysLater.setDate(now.getDate() + 30)

    // grouped 是一個物件，key 是 custId，value 是該客戶的到期點數摘要
    const grouped = {}

    histories.value
        // 篩選條件：availablePoints > 0（還有可用點數）且 expireTime 在 30 天內
        .filter(h => {
            if (!h.availablePoints || h.availablePoints <= 0) return false
            const expiry = new Date(h.expireTime)
            return expiry >= now && expiry <= thirtyDaysLater
        })
        // 依 custId 分組，同一個客戶的即將到期點數加總
        .forEach(h => {
            if (!grouped[h.custId]) {
                // 第一次遇到這個客戶，建立新的群組
                grouped[h.custId] = {
                    custId: h.custId,
                    custName: h.custName,
                    totalExpiring: 0,           // 該客戶即將到期的點數加總
                    nearestExpiry: h.expireTime?.slice(0, 10)  // 最近的到期日
                }
            }
            // 把這筆的 availablePoints 加進該客戶的加總
            grouped[h.custId].totalExpiring += h.availablePoints
        })

    // Object.values() 把物件轉成陣列，再按最近到期日升冪排序（最快到期的排最上面）
    return Object.values(grouped).sort((a, b) =>
        new Date(a.nearestExpiry) - new Date(b.nearestExpiry)
    )
})
/*
1. grouped[h.custId]
用 custId 當 key，把同一個客戶的資料放在同一個格子裡，這就是「分組」。
2. Object.values(grouped)
grouped 是物件，不是陣列，v -for 不能直接用。
Object.values() 把物件的所有值取出來變成陣列。
3..sort((a, b) => new Date(a) - new Date(b))
日期字串相減會自動轉成毫秒數，結果是正數代表 a 在 b 後面，負數代表 a 在 b 前面，升冪排序。
*/

// 導覽卡片
const navCards = [
    { label: '會員點數查詢', icon: 'fa-users', desc: '查詢所有會員的點數餘額與異動', to: '/point/customer-points' },
    { label: '商品管理', icon: 'fa-gift', desc: '管理可兌換的點數商品與庫存', to: '/point/products' },
    { label: '點數規則管理', icon: 'fa-sliders', desc: '設定各種點數獲得與扣除規則', to: '/point/rules' },
    { label: '點數變動紀錄', icon: 'fa-clock-rotate-left', desc: '查看所有點數異動的流水帳紀錄', to: '/point/histories' },
    { label: '兌換訂單管理', icon: 'fa-file-invoice', desc: '查看所有會員的兌換訂單', to: '/point/orders' },
    { label: '兌換券管理', icon: 'fa-ticket', desc: '查看與管理所有兌換券狀態', to: '/point/vouchers' },
]

const loadChartJs = () => {
    return new Promise((resolve) => {
        if (typeof Chart !== 'undefined') return resolve()
        const script = document.createElement('script')
        script.src = 'https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js'
        script.onload = resolve
        document.head.appendChild(script)
    })
}

onMounted(async () => {
    // 同時打三支 API
    const [historyRes, voucherRes, orderRes] = await Promise.all([
        pointsHistoryAPI.getAll(),
        voucherAPI.getAll(),
        redemptionOrderAPI.getAll()
    ])
    histories.value = historyRes.data.data
    vouchers.value = voucherRes.data.data
    orders.value = orderRes.data.data

    // 資料載入完才畫圖
    await loadChartJs()
    await initCharts()
})

// 時間格式調整
const formatDateTime = (timeStr) => {
    if (!timeStr) return '-'
    return timeStr.slice(0, 19).replace('T', ' ')
}


//圖表相關script

import {nextTick } from 'vue'

// 圖表用
const orderChartRef = ref(null)
const pointsChartRef = ref(null)

// 近 6 個月訂單趨勢資料
const orderChartData = computed(() => {
    const months = []
    const now = new Date()
    for (let i = 5; i >= 0; i--) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
        months.push({
            label: `${d.getMonth() + 1}月`,
            year: d.getFullYear(),
            month: d.getMonth()
        })
    }
    return months.map(m => ({
        label: m.label,
        count: orders.value.filter(o => {
            const d = new Date(o.createTime)
            return d.getFullYear() === m.year && d.getMonth() === m.month
        }).length
    }))
})

// 點數類型分布資料
const pointsTypeData = computed(() => {
    const typeMap = {
        'RENTAL': '租車累點',
        'TRANSFER': '專車累點',
        'BIRTHDAY': '生日贈點',
        'WELCOME_BONUS': '新會員贈點',
        'FIRST_RENTAL': '首租獎勵',
        'REDEMPTION': '兌換消耗',
        'EXPIRED': '點數過期'
    }
    const counts = {}
    histories.value.forEach(h => {
        const label = typeMap[h.changeType] || h.changeType
        counts[label] = (counts[label] || 0) + 1
    })
    return {
        labels: Object.keys(counts),
        data: Object.values(counts)
    }
})

// 串chart.js api畫圖
const initCharts = async () => {
    await nextTick()
    if (typeof Chart === 'undefined') return

    // 折線圖
    if (orderChartRef.value) {
        new Chart(orderChartRef.value, {
            type: 'line',
            data: {
                labels: orderChartData.value.map(d => d.label),
                datasets: [{
                    label: '兌換訂單數',
                    data: orderChartData.value.map(d => d.count),
                    borderColor: '#0150AD',
                    backgroundColor: 'rgba(1, 80, 173, 0.08)',
                    borderWidth: 2,
                    pointRadius: 4,
                    pointBackgroundColor: '#0150AD',
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 1 }
                    }
                }
            }
        })
    }

    // 圓餅圖
    if (pointsChartRef.value) {
        new Chart(pointsChartRef.value, {
            type: 'doughnut',
            data: {
                labels: pointsTypeData.value.labels,
                datasets: [{
                    data: pointsTypeData.value.data,
                    backgroundColor: [
                        '#DAEBFC',  // 藍 100（系統藍延伸）
                        '#93C5FD',  // 藍 300
                        '#DCFCE7',  // 綠 100
                        '#BBF7D0',  // 綠 200
                        '#FEF9C3',  // 黃 100
                        '#FCE7D5',  // 沙 100（系統 Sandy Clay 延伸）
                        '#F1EFE8',  // 灰 100
                    ],
                    borderColor: [
                        '#002a5c',  // 藍 800
                        '#003d80',  // 藍 700
                        '#107b37',  // 綠 700
                        '#15803D',  // 綠 700
                        '#A16207',  // 黃 700
                        '#A06B38',  // 沙 800
                        '#444441',  // 灰 800
                    ],
                    borderWidth: 1,  // 線條也加粗一點，對比更明顯
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'right',
                        labels: { font: { size: 12 }, padding: 16 }
                    }
                }
            }
        })
    }
}
</script>

<template>
    <div>
        <!-- ── 標題 ── -->
        <div class="mb-4">
            <h3 class="fw-bold mb-1">點數兌換管理</h3>
            <p class="text-secondary mb-0" style="font-size: var(--text-sm);">
                管理會員點數、商品兌換與票券核銷
            </p>
        </div>

        <!-- ── KPI 統計卡片 ── -->
        <div class="row g-3 mb-4">
            <div class="col-6 col-md-3">
                <div class="bg-white border rounded-4 p-4 h-100">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">本月發出點數</p>
                    <p class="fw-bold mb-0" style="font-size: var(--text-2xl); color: var(--color-success);">
                        +{{ monthlyIssued.toLocaleString() }}
                    </p>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="bg-white border rounded-4 p-4 h-100">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">本月消耗點數</p>
                    <p class="fw-bold mb-0" style="font-size: var(--text-2xl); color: var(--color-warning);">
                        -{{ monthlyRedeemed.toLocaleString() }}
                    </p>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="bg-white border rounded-4 p-4 h-100">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">待使用兌換券</p>
                    <p class="fw-bold mb-0" style="font-size: var(--text-2xl); color: var(--color-primary);">
                        {{ unusedVoucherCount }}
                        <span style="font-size: var(--text-sm); font-weight: normal;">張</span>
                    </p>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="bg-white border rounded-4 p-4 h-100">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">本月兌換訂單</p>
                    <p class="fw-bold mb-0" style="font-size: var(--text-2xl); color: var(--color-primary);">
                        {{ monthlyOrderCount }}
                        <span style="font-size: var(--text-sm); font-weight: normal;">筆</span>
                    </p>
                </div>
            </div>
        </div>


        <!-- ── 圖表區 ── -->
        <div class="row g-3 mb-4">
            <!-- 折線圖 -->
            <div class="col-md-8">
                <div class="bg-white border rounded-4 p-4 h-100">
                    <h6 class="fw-semibold mb-3">近 6 個月兌換訂單趨勢</h6>
                    <div style="height: 220px;">
                        <canvas ref="orderChartRef"></canvas>
                    </div>
                </div>
            </div>

            <!-- 圓餅圖 -->
            <div class="col-md-4">
                <div class="bg-white border rounded-4 p-4 h-100">
                    <h6 class="fw-semibold mb-3">點數類型分布</h6>
                    <div style="height: 220px;">
                        <canvas ref="pointsChartRef"></canvas>
                    </div>
                </div>
            </div>
        </div>


        <!-- ── 導覽卡片 ── -->
        <div class="row g-3 mb-4">
            <div class="col-md-4" v-for="card in navCards" :key="card.to">
                <RouterLink :to="card.to"
                    class="nav-card d-flex align-items-center gap-3 text-decoration-none bg-white border rounded-4 p-3">
                    <div class="d-flex align-items-center justify-content-center flex-shrink-0 rounded-3"
                        style="width:44px; height:44px; background: var(--color-primary-light); color: var(--color-primary); font-size: 1.2rem;">
                        <font-awesome-icon :icon="card.icon" />
                    </div>
                    <div class="flex-grow-1">
                        <p class="fw-semibold mb-1"
                            style="font-size: var(--text-sm); color: var(--color-text-primary);">
                            {{ card.label }}
                        </p>
                        <p class="mb-0" style="font-size: var(--text-xs); color: var(--color-text-muted);">
                            {{ card.desc }}
                        </p>
                    </div>
                    <font-awesome-icon icon="chevron-right"
                        style="color: var(--color-text-muted); font-size: 0.75rem;" />
                </RouterLink>
            </div>
        </div>


        <!-- ── 即將到期點數警示 ── -->
        <div class="bg-white border rounded-4 p-4 mb-4">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h6 class="fw-semibold mb-0 text-warning">
                    <font-awesome-icon icon="triangle-exclamation" class="me-2" />
                    30 天內即將到期點數
                    <span v-if="expiringCustomers.length > 0">
                        （{{ expiringCustomers.length }} 位會員）
                    </span>
                </h6>
            </div>

            <!-- 有資料：顯示表格 -->
            <table v-if="expiringCustomers.length > 0" class="table table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th>客戶編號</th>
                        <th>客戶姓名</th>
                        <th>最近到期日</th>
                        <th class="text-end">即將到期點數</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="c in expiringCustomers" :key="c.custId">
                        <td><code>{{ c.custId }}</code></td>
                        <td class="fw-bold">{{ c.custName }}</td>
                        <td class="text-warning">{{ c.nearestExpiry }}</td>
                        <td class="text-end fw-bold text-warning">
                            {{ c.totalExpiring.toLocaleString() }} 點
                        </td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary"
                                @click="$router.push(`/point/customer-points?custId=${c.custId}`)">
                                查看詳情
                            </button>
                        </td>
                    </tr>
                </tbody>
            </table>

            <!-- 沒資料：顯示 EmptyState -->
            <EmptyState v-else icon="circle-check" message="目前沒有即將到期的點數" subMessage="所有會員的點數效期都還充裕" />
        </div>



        <!-- ── 近期兌換訂單 ── -->
        <div class="bg-white border rounded-4 p-4">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h6 class="fw-semibold mb-0">近期兌換訂單</h6>
                <RouterLink to="/point/orders"
                    style="font-size: var(--text-sm); color: var(--color-primary); text-decoration: none;">
                    查看全部 →
                </RouterLink>
            </div>
            <table class="table table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th>訂單編號</th>
                        <th>客戶編號</th>
                        <th>客戶姓名</th>
                        <th>商品</th>
                        <th>數量</th>
                        <th>建立時間</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="order in recentOrders" :key="order.redemptionId">
                        <td><code>{{ order.redemptionId }}</code></td>
                        <td>{{ order.customerBean?.custId }}</td>
                        <td>{{ order.customerBean?.custName }}</td>
                        <td>{{ order.product?.productName }}</td>
                        <td>{{ order.productQuantity }}</td>
                        <td>{{ formatDateTime(order.createTime) }}</td>
                    </tr>
                </tbody>
            </table>
            <div v-if="recentOrders.length === 0" class="text-center py-3 text-secondary">
                目前沒有兌換訂單
            </div>
        </div>

        <!-- 測試元件連結 -->
        <div class="mt-3">
            <RouterLink to="/point/components" class="text-danger" style="font-size: var(--text-sm);">
                <font-awesome-icon icon="flask" class="me-1" />測試元件展示
            </RouterLink>
        </div>
    </div>
</template>

<style scoped>
/* hover 效果無法用 Bootstrap 做，必須在這裡寫 */
.nav-card {
    transition: all var(--transition-fast);
}

.nav-card:hover {
    box-shadow: var(--shadow-md);
    border-color: var(--color-primary) !important;
    transform: translateY(-1px);
}
</style>