// ============================================================
// Dashboard Pinia Store — 假資料集中管理
// 未來接 API 時，只需修改 actions 裡的 fetch 方法即可
// ============================================================
import { defineStore } from 'pinia'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    // --- 第一排：6 張統計卡片 ---
    stats: [
      {
        label: '會員總數',
        value: '12,450',
        icon: 'fa-users',
        trend: '+5.2%',
        trendDir: 'up',       // up / down / flat
        colorClass: 'primary'
      },
      {
        label: '本月訂單',
        value: '3,892',
        icon: 'fa-file-invoice',
        trend: '+12.4%',
        trendDir: 'up',
        colorClass: 'info'
      },
      {
        label: '進行中訂單',
        value: '428',
        icon: 'fa-taxi',
        trend: '持平',
        trendDir: 'flat',
        colorClass: 'teal'
      },
      {
        label: '本月營收',
        value: '$842K',
        icon: 'fa-money-bill-wave',
        trend: '+8.1%',
        trendDir: 'up',
        colorClass: 'warning'
      },
      {
        label: '可用車輛',
        value: '156',
        icon: 'fa-car-side',
        trend: '-2.5%',
        trendDir: 'down',
        colorClass: 'success'
      },
      {
        label: '待處理兌換',
        value: '42',
        icon: 'fa-gift',
        trend: '需關注',
        trendDir: 'up',
        colorClass: 'orange'
      }
    ],

    // --- 第二排左側：最近訂單 ---
    recentOrders: [
      { id: '#ORD-9021', name: '陳建明', plate: 'ABC-1234', pickupTime: '2025-10-25 14:30', status: '已取車',  statusClass: 'bg-success' },
      { id: '#ORD-9020', name: '林雅婷', plate: 'XYZ-9876', pickupTime: '2025-10-25 15:00', status: '已預約',  statusClass: 'bg-warning' },
      { id: '#ORD-9019', name: '張威廉', plate: 'QQQ-5566', pickupTime: '2025-10-25 16:15', status: '已結案',  statusClass: 'bg-gray' },
      { id: '#ORD-9018', name: '李小龍', plate: 'FAS-3321', pickupTime: '2025-10-25 17:00', status: '已取消',  statusClass: 'bg-danger' }
    ],

    // --- 第二排右側：待辦事項 ---
    todos: [
      { text: '3 筆預約待取車',     priority: 'warning', label: '今日到期',  done: false },
      { text: '2 筆待確認看車預約',  priority: 'muted',   label: '3 筆待處理', done: false },
      { text: '1 筆兌換待出貨',      priority: 'danger',  label: '已逾期',    done: false },
      { text: '員工會議準備',         priority: 'muted',   label: '明天 10:00', done: false }
    ]
  }),

  actions: {
    // 未來接 API 時替換此方法
    async fetchDashboardData() {
      // const res = await axios.get('/api/dashboard/stats')
      // this.stats = res.data.stats
      console.log('Dashboard data loaded (mock)')
    }
  }
})
