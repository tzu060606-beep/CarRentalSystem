// src/router/point.js
// 引入你的畫面元件

import PointIndexView from "@/views/admin/point/PointIndexView.vue";
import PointsHistoryView from "@/views/admin/point/PointsHistoryView.vue";
import PointsRuleView from "@/views/admin/point/PointsRuleView.vue";
import ProductView from "@/views/admin/point/ProductView.vue";
import RedemptionOrderView from "@/views/admin/point/RedemptionOrderView.vue";
import VoucherView from "@/views/admin/point/VoucherView.vue";
//測試元件用畫面，測試後刪除
import ComponentTestView from "@/views/admin/point/ComponentTestView.vue";
import CustomerPointsView from "@/views/admin/point/CustomerPointsView.vue";
import VoucherVerifyView from "@/views/admin/point/VoucherVerifyView.vue";

// 單純匯出一個「陣列」，裡面裝你負責的所有路由
export default [

    {
        // 後台導覽頁
        path: '/point',
        name: 'AdminPoint',
        component: PointIndexView  
    },
    {
        //後台:商品列表頁
        path: '/point/products',
        name: 'AdminPointProducts',
        component: ProductView
    },
     {
        //後台:點數規則列表頁
        path: '/point/rules',
        name: 'AdminPointRules',
        component: PointsRuleView
    },
     {
        //後台:點數異動紀錄列表頁
        path: '/point/histories',
        name: 'AdminPointHistories',
        component: PointsHistoryView
    },
     {
        //後台:兌換訂單列表頁
        path: '/point/orders',
        name: 'AdminPointOrders',
        component: RedemptionOrderView
    },
    {
        //後台:兌換券列表頁
        path: '/point/vouchers',
        name: 'AdminPointVouchers',
        component: VoucherView
    },
    {
        //後台:兌換券核銷頁
        path: '/point/verify',
        name: 'AdminPointVerify',
        component: VoucherVerifyView
    },
      {
        //後台:共用元件測試頁,測試後刪除
        path: '/point/components',
        name: 'AdminCommonComponents',
        component: ComponentTestView
    },
    {
       //後台:會員點數查詢頁
    path: '/point/customer-points',
    name: 'AdminCustomerPoints',
    component: CustomerPointsView
    },
    
]
