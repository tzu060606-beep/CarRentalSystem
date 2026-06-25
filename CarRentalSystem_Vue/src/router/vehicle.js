// 引入你的畫面元件
// import CarModelView from '@/views/admin/vehicle/CarModelView.vue'
// import VehicleListView from '@/views/admin/vehicle/VehicleListView.vue'
// import VehicleFormView from '@/views/admin/vehicle/VehicleFormView.vue'

// 單純匯出一個「陣列」，裡面裝你負責的所有路由
export default [
    {
        path: '/carmodel',
        name: 'carmodel',
        component: () => import('@/views/admin/vehicle/CarModelView.vue')
    },
    {
        path: '/vehicle',
        name: 'vehicle',
        component: () => import('@/views/admin/vehicle/VehicleListView.vue')
    },
    {
        path: '/vehicle/detail/:id',
        name: 'vehicleDetail',
        component: () => import('@/views/admin/vehicle/VehicleDetailView.vue')
    },
    {
        path: '/dispatch/dashboard',
        name: 'dispatchDashboard',
        component: () => import('@/views/admin/vehicle/DispatchDashboardView.vue')
    },
    {
        path: '/dispatch/task',
        name: 'dispatchTask',
        component: () => import('@/views/admin/vehicle/DispatchTaskView.vue')
    },
    {
        path: '/crosslocationfee',
        name: 'crosslocationfee',
        component: () => import('@/views/admin/vehicle/CrossLocationFeeView.vue')
    },
    // 未來如果你有 /carmodel/add 也可以繼續寫在這裡
]