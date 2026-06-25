export default [
    {
        path: '/carmodels',
        name: 'customercarmodel',
        component: () => import('@/views/customer/vehicle/CustomCarModelView.vue')
    },
    {
        path: '/locations',
        name: 'customerlocation',
        component: () => import('@/views/customer/vehicle/CustomLocationView.vue')
    },
]