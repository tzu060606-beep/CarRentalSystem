<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import { voucherAPI } from '@/api/point/point_voucher';
import { customerAPI } from '@/api/login/customerAPI'
import AlertToast from '@/components/common/AlertToast.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useRoute } from 'vue-router'


const vouchers = ref([])
const keyword = ref('')
const statusFilter = ref('') // UNUSED / USED / EXPIRED
const redemptionIdFilter = ref('')  // 從路由 query 帶入的訂單編號篩選
const route = useRoute()

// 查詢全部
const fetchVouchers = async () => {
    const response = await voucherAPI.getAll()
    vouchers.value = response.data.data
}

// 庫存+上架狀態+類別+關鍵字篩選、點數排序
const filteredVouchers = computed(() => {
    return vouchers.value

        // 【新增】客戶篩選：有選定客戶時，只顯示該客戶的票券
        // 沒有選定客戶時顯示全部（selectedCust 為 null）
        .filter(v => {
            if (selectedCust.value) {
                return v.redemptionOrder?.customerBean?.custId === selectedCust.value.custId
            }
            return true
        })
        // 狀態篩選
        .filter(v => {
            if (statusFilter.value === '') return true
            return v.status === statusFilter.value
        })
        // 票券號碼關鍵字搜尋
        .filter(v => {
            if (keyword.value === '') return true
            return v.voucherCode.includes(keyword.value)
        })
        //抓根據篩選到的訂單的票券
        .filter(v => {
            if (redemptionIdFilter.value === '') return true
            return v.redemptionOrder?.redemptionId == redemptionIdFilter.value
        })
})


// 重設所有篩選條件
const resetFilters = () => {
    statusFilter.value = ''
    keyword.value = ''
    // 【新增】清空客戶搜尋相關狀態
    custKeyword.value = ''
    selectedCust.value = null      // 清空選定客戶，列表回到顯示全部
    searchResults.value = []
    showDropdown.value = false
    redemptionIdFilter.value = ''//清空篩選訂單
}

const getStatusLabel = (status) => {
    if (status === 'UNUSED') return '未使用'
    if (status === 'USED') return '已使用'
    if (status === 'EXPIRED') return '已過期'
    return status
}

const getStatusBadgeClass = (status) => {
    if (status === 'UNUSED') return 'badge bg-primary'
    if (status === 'USED') return 'badge bg-success'
    if (status === 'EXPIRED') return 'badge bg-secondary'
}


const unusedCount = computed(() => vouchers.value.filter(v => v.status === 'UNUSED').length)
const usedCount = computed(() => vouchers.value.filter(v => v.status === 'USED').length)
const expiredCount = computed(() => vouchers.value.filter(v => v.status === 'EXPIRED').length)

//================客戶搜尋用========================

// 【客戶搜尋相關】
const custKeyword = ref('')        // 客戶搜尋輸入框的值
const searchResults = ref([])      // 搜尋到的客戶列表（下拉選單用）
const showDropdown = ref(false)    // 控制下拉選單顯示
const selectedCust = ref(null)    // 目前選定的客戶（用來篩選票券）
const isSelecting = ref(false)    // 防止選取時觸發 watch 重複查詢

// 【核銷相關】
const toast = ref(null)           // AlertToast 元件實例
const showConfirmDialog = ref(false)  // 控制確認核銷彈窗
const pendingVoucher = ref(null)  // 待核銷的票券（點核銷按鈕時存入）



// 【為什麼用 watch】監聽輸入框變化，即時打 API 搜尋客戶
// 跟 CustomerPointsView 的搜尋邏輯相同
watch(custKeyword, async (newVal) => {
    // 【容易忽略】選取客戶時 custKeyword 也會變，
    // 用 isSelecting flag 避免選取時再次觸發搜尋
    if (isSelecting.value) return

    if (newVal.trim() === '') {
        // 清空輸入時，重設所有搜尋狀態
        searchResults.value = []
        showDropdown.value = false
        selectedCust.value = null  // 清空選定客戶，列表回到顯示全部
        return
    }

    const response = await customerAPI.search(newVal)
    searchResults.value = response.data
    showDropdown.value = searchResults.value.length > 0
})

// 點選下拉選單中的客戶
const selectCust = (cust) => {
    isSelecting.value = true       // 先設 flag，防止 watch 被觸發
    custKeyword.value = cust.custName  // 把客戶名稱填入輸入框
    selectedCust.value = cust     // 存入選定客戶，filteredVouchers 會自動重算
    showDropdown.value = false    // 關閉下拉選單
    isSelecting.value = false     // 重設 flag
}

// 開啟核銷確認彈窗
// 【為什麼不直接核銷】核銷不可逆，需要多一層確認防止誤操作
const openVerifyDialog = (voucher) => {
    pendingVoucher.value = voucher  // 把要核銷的票券存起來，confirmVerify 才能用
    showConfirmDialog.value = true
}

// 執行核銷
const confirmVerify = async () => {
    try {
        // 【為什麼先打 API】資料庫是真相來源，成功後才更新前端
        await voucherAPI.use(pendingVoucher.value.voucherCode)
        toast.value.show('核銷成功！', 'success', 3000)
        // 重新抓全部票券，確保列表狀態跟資料庫一致
        await fetchVouchers()
    } catch (error) {
        toast.value.show(error.response?.data?.message || '核銷失敗', 'danger', 3000)
    } finally {
        // 【容易忽略】不管成功或失敗都要關閉彈窗並清空待核銷票券
        showConfirmDialog.value = false
        pendingVoucher.value = null
    }
}


onMounted(() => {
    fetchVouchers()
    // 如果網址帶了 redemptionId，自動篩選該訂單的票券
    if (route.query.redemptionId) {
        redemptionIdFilter.value = route.query.redemptionId
    }
})
</script>



<template>
    <div>

        <!-- 麵包屑 -->
        <div class="mb-3">
            <Breadcrumb :items="[
                { label: '首頁', to: '/' },
                { label: '點數管理', to: '/point' },
                { label: '兌換券管理' }
            ]" />
        </div>

        <!-- 標題 -->
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h3 class="mb-0 fw-bold">兌換券管理</h3>
        </div>

      <!-- 統計卡片 -->
        <div class="row g-3 mb-3">
            <div class="col-4">
                <div class="bg-white border rounded-4 p-3 text-center">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">未使用</p>
                    <p class="fw-bold mb-0" style="font-size: var(--text-2xl); color: var(--color-primary);">
                        {{ unusedCount }}
                    </p>
                </div>
            </div>
            <div class="col-4">
                <div class="bg-white border rounded-4 p-3 text-center">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">已使用</p>
                    <p class="fw-bold mb-0" style="font-size: var(--text-2xl); color: var(--color-success);">
                        {{ usedCount }}
                    </p>
                </div>
            </div>
            <div class="col-4">
                <div class="bg-white border rounded-4 p-3 text-center">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">已過期</p>
                    <p class="fw-bold mb-0" style="font-size: var(--text-2xl); color: var(--color-gray-400);">
                        {{ expiredCount }}
                    </p>
                </div>
            </div>
        </div>


        <!-- 操作列 -->
        <div class="d-flex justify-content-between align-items-center mb-2 mt-2">

            <!-- 左邊：狀態篩選按鈕 -->
            <div class="d-flex gap-2">
                <button @click="statusFilter = ''"
                    :class="statusFilter === '' ? 'btn btn-primary' : 'btn btn-outline-primary'">
                    全部
                </button>
                <button @click="statusFilter = 'UNUSED'"
                    :class="statusFilter === 'UNUSED' ? 'btn btn-primary' : 'btn btn-outline-primary'">
                    未使用
                </button>
                <button @click="statusFilter = 'USED'"
                    :class="statusFilter === 'USED' ? 'btn btn-success' : 'btn btn-outline-success'">
                    已使用
                </button>
                <button @click="statusFilter = 'EXPIRED'"
                    :class="statusFilter === 'EXPIRED' ? 'btn btn-secondary' : 'btn btn-outline-secondary'">
                    已過期
                </button>
            </div>


            <div class="d-flex gap-2">
                
                
                <!-- 【新增】客戶搜尋框，搜尋後列表只顯示該客戶的票券 -->
                <div class="position-relative">
                    <input v-model="custKeyword" class="form-control" type="text" placeholder="搜尋客戶姓名..."
                        style="min-width: 160px;" />

                    <!-- 下拉選單：有搜尋結果才顯示 -->
                    <div v-if="showDropdown" class="position-absolute w-100 bg-white border rounded-3 shadow-sm"
                        style="top: 100%; left: 0; z-index: 1000; margin-top: 4px;">
                        <div v-for="cust in searchResults" :key="cust.custId"
                            class="px-3 py-2 d-flex justify-content-between align-items-center"
                            style="cursor: pointer; font-size: var(--text-sm);" @click="selectCust(cust)"
                            @mouseover="$event.currentTarget.style.backgroundColor = 'var(--color-primary-light)'"
                            @mouseleave="$event.currentTarget.style.backgroundColor = ''">
                            <span class="fw-bold">{{ cust.custName }}</span>
                            <code>ID: {{ cust.custId }}</code>
                        </div>
                    </div>
                </div>
                
                
                
                <!-- 右邊：關鍵字搜尋 + 重設 -->
                <input v-model="keyword" class="form-control" type="text" placeholder="請輸入票券號碼關鍵字..." />
                <button v-if="statusFilter || keyword || custKeyword || redemptionIdFilter"  @click="resetFilters"
                    class="btn btn-outline-secondary text-nowrap">
                    重設篩選
                </button>
            </div>
        </div>


     <!-- 從訂單頁跳過來時，顯示目前篩選的訂單編號 -->
        <div v-if="redemptionIdFilter" class="mb-2 d-flex align-items-center gap-2">
            <span class="badge bg-primary-subtle text-primary">
                目前篩選：訂單 #{{ redemptionIdFilter }}
            </span>
            <span class="text-secondary" style="font-size: var(--text-xs);">
                （點「重設篩選」可回到全部票券）
            </span>
            <RouterLink to="/point/orders" class="text-secondary text-decoration-none"
                style="font-size: var(--text-xs);">
                ← 返回兌換訂單管理
            </RouterLink>
        </div>





        <!-- 筆數顯示 -->
        <div class="d-flex justify-content-end gap-2 m-2">
            <span class="text-primary">
                共 {{ vouchers.length }} 筆，搜尋結果：{{ filteredVouchers.length }} 筆
            </span>
        </div>

        <!-- 票券表格 -->
        <div class="bg-white border rounded-4 shadow-sm overflow-hidden">
            <table class="table table-hover align-middle text-nowrap mb-0">
                <thead>
                    <tr>
                        <th>票券編號</th>
                        <th>持有客戶姓名</th>
                        <th>票券號碼</th>
                        <th>兌換訂單編號</th>
                        <th>核銷狀態</th>
                        <th>使用時間</th>
                        <th>到期日</th>
                        <!-- 表頭加操作欄 -->
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="voucher in filteredVouchers" :key="voucher.voucherId">
                        <td><code>{{ voucher.voucherId }}</code></td>
                        <td>{{ voucher.redemptionOrder?.customerBean?.custName }}</td>
                        <td class="fw-bold">{{ voucher.voucherCode }}</td>
                        <td>
                            <!-- 讓連結帶query參數，反查回去該筆訂單 -->
                           <RouterLink :to="`/point/orders?redemptionId=${voucher.redemptionOrder?.redemptionId}`"
                                class="text-decoration-none font-monospace text-primary">
                                #{{ voucher.redemptionOrder?.redemptionId }}
                            </RouterLink>
                        </td>
                        <td>
                            <span :class="getStatusBadgeClass(voucher.status)">
                                {{ getStatusLabel(voucher.status) }}
                            </span>
                        </td>
                        <td>{{ voucher.useTime ?? '—' }}</td>
                        <td>{{ voucher.expiryDate }}</td>
                        <!-- 表格內容：UNUSED 才顯示核銷按鈕，其他顯示破折號 -->
                        <td>
                            <button v-if="voucher.status === 'UNUSED'" class="btn btn-sm btn-outline-primary"
                                @click="openVerifyDialog(voucher)">
                                核銷
                            </button>
                            <span v-else class="text-secondary">—</span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

    </div>

    <AlertToast ref="toast" />

    <!-- 核銷確認彈窗 -->
    <!-- 【為什麼動態綁定 message】每張票券號碼不同，要顯示正確的號碼讓使用者確認 -->
    <ConfirmDialog :isVisible="showConfirmDialog" title="確認核銷"
        :message="`確認核銷票券 ${pendingVoucher?.voucherCode}？此操作無法復原。`" confirmText="確認核銷" @confirm="confirmVerify"
        @cancel="showConfirmDialog = false" />


</template>


<style scoped></style>
