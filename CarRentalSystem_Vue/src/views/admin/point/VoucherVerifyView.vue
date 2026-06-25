<script setup>
import { ref, computed } from 'vue'
import { voucherAPI } from '@/api/point/point_voucher'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import AlertToast from '@/components/common/AlertToast.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const voucherCode = ref('')       // 存輸入的號碼
const voucher = ref(null)          // 存查詢到的票券
const isSearched = ref(false)      // 存是否已查詢
const isSubmitting = ref(false)    // 存是否送出中
// const errorMessage = ref('')       // 存錯誤訊息
// const successMessage = ref('')     //存成功訊息
const toast = ref(null)            //存提示訊息實例
const showConfirmDialog = ref(false)

// 【為什麼這樣寫】computed 從已有的 voucher 資料推算，不需要額外的 ref
const isUsed = computed(() => voucher.value?.status === 'USED')//是否已使用
const isExpired = computed(() => voucher.value?.status === 'EXPIRED')//是否已過期
const canVerify = computed(() => voucher.value?.status === 'UNUSED')

// 【為什麼這樣寫】查詢和核銷分開，符合單一職責原則
// 查詢只負責「找到票券」，不做核銷
// 觸發查詢這個動作
const handleSearch = async () => {
    // 先接到輸入的票券號碼，
    // 用api呼叫findByVoucherCode取得查詢到的票券
    // 把查詢到的票券的值放進voucher變數
    // 如果查不到，丟錯誤訊息

    voucher.value = null
    isSearched.value = false

    if (!voucherCode.value.trim()) {
        toast.value.show('請輸入票券號碼','warning',3000)
        return
    }

    try {
        const response = await voucherAPI.getByCode(voucherCode.value.trim())
        voucher.value = response.data.data
        isSearched.value = true
        toast.value.show('查詢成功', 'success', 2000)
    } catch (error) {
        toast.value.show(error.response?.data?.message || '查無此票券', 'warning', 3000)
    }
}

// 觸發【核銷】這個動作
// 核銷：確認後打 API，以資料庫為主，成功後再更新前端畫面
const confirmVerify = async () => {
    //    把 isSubmitting 設為 true(核銷送出中)
    //      打API把行為同步回資料庫
    //     然後呢？->把票券狀態設為已使用
    //     成功後要做什麼？把前端畫面中剛剛查詢到的票券的狀態設定為已使用
    //     失敗後要做什麼？丟錯誤訊息
    //     最後要做什麼？成功後更新前端畫面、把票券樣式改變
    //      finally 把isSubmmiting設回false，按鈕才不會一直轉圈

    isSubmitting.value = true
    try {
        // 【為什麼先打 API】資料庫是真相來源，成功後才更新前端
        // 不能先改前端再同步，萬一 API 失敗畫面會顯示錯誤狀態
        await voucherAPI.use(voucherCode.value.trim())
        // 成功後重新查詢，確保前端資料跟資料庫一致
        const response = await voucherAPI.getByCode(voucherCode.value.trim())
        // 【關鍵概念】ApiResponse 包裝：真正的票券資料在 response.data.data 裡
        voucher.value = response.data.data
        // 顯示提示訊息
        toast.value.show('核銷成功！票券已標記為已使用', 'success', 3000)
    } catch (error) {
        toast.value.show(error.response?.data?.message || '核銷失敗，請稍後再試', 'danger', 3000)
    } finally {
        // 【容易忽略】不管成功或失敗，最後都要把 isSubmitting 設回 false
        // 否則按鈕會一直顯示「處理中」
       showConfirmDialog.value = false
    }
}

// 觸發重設這個動作
const reset = () => {
    // 把所有變數還原回初始狀態。
    voucherCode.value = ''
    voucher.value = null
    isSearched.value = false
    isSubmitting.value = false
}

// 時間格式化
const formatDateTime = (timeStr) => {
    if (!timeStr) return '—'
    return timeStr.slice(0, 19).replace('T', ' ')
}

//狀態標籤轉換標籤顯示文字
const getStatusLabel = (status) => {
    if (status === 'UNUSED') return '未使用'
    if (status === 'USED') return '已使用'
    if (status === 'EXPIRED') return '已過期'
    return status
}

// 標籤樣式控制
const getStatusBadgeClass = (status) => {
    if (status === 'UNUSED') return 'badge bg-primary'
    if (status === 'USED') return 'badge bg-success'
    if (status === 'EXPIRED') return 'badge bg-secondary'
}

</script>
<template>
    <div>
        <!-- 麵包屑 -->
        <div class="mb-3">
            <Breadcrumb :items="[
                { label: '首頁', to: '/' },
                { label: '點數管理', to: '/point' },
                { label: '票券核銷' }
            ]" />
        </div>

        <!-- 標題 -->
        <div class="mb-4">
            <h3 class="fw-bold mb-1">票券核銷</h3>
            <p class="text-secondary mb-0" style="font-size: var(--text-sm);">
                輸入票券號碼查詢並執行核銷
            </p>
        </div>

        <!-- 搜尋列 -->
        <div class="bg-white border rounded-4 shadow-sm p-4 mb-4">
            <label class="fw-semibold mb-2 d-block">輸入票券號碼</label>
            <div class="d-flex gap-2">
                <input v-model="voucherCode" type="text" class="form-control" placeholder="例如：VC-4D9F2BF2"
                    @keyup.enter="handleSearch" />
                <button class="btn btn-primary text-nowrap" @click="handleSearch">
                    查詢
                </button>
                <button class="btn btn-outline-secondary text-nowrap" @click="reset">
                    重設
                </button>
            </div>

            <!-- 錯誤訊息 -->
            <!-- <div v-if="errorMessage" class="alert alert-danger mt-3 mb-0">
                {{ errorMessage }}
            </div> -->

            <!-- 成功訊息 -->
            <!-- <div v-if="successMessage" class="alert alert-success mt-3 mb-0">
                ✅ {{ successMessage }}
            </div> -->
        </div>

        <!-- 查詢結果 -->
        <div v-if="isSearched && voucher" class="bg-white border rounded-4 shadow-sm p-4">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h6 class="fw-semibold mb-0">票券資訊</h6>
                <span :class="getStatusBadgeClass(voucher.status)">
                    {{ getStatusLabel(voucher.status) }}
                </span>
            </div>

            <div class="row g-3 mb-4">
                <div class="col-md-6">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">票券號碼</p>
                    <p class="fw-bold font-monospace mb-0">{{ voucher.voucherCode }}</p>
                </div>
                <div class="col-md-6">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">商品名稱</p>
                    <p class="fw-bold mb-0">{{ voucher.redemptionOrder?.product?.productName }}</p>
                </div>
                <div class="col-md-6">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">持有客戶</p>
                    <p class="mb-0">{{ voucher.redemptionOrder?.customerBean?.custName }}</p>
                </div>
                <div class="col-md-6">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">到期日</p>
                    <p class="mb-0">{{ voucher.expiryDate?.slice(0, 10) }}</p>
                </div>
                <div class="col-md-6" v-if="voucher.useTime">
                    <p class="text-secondary mb-1" style="font-size: var(--text-xs);">使用時間</p>
                    <p class="mb-0">{{ formatDateTime(voucher.useTime) }}</p>
                </div>
            </div>

            <!-- 已使用提示 -->
            <div v-if="isUsed" class="alert alert-success mb-0">
                此票券已於 {{ formatDateTime(voucher.useTime) }} 完成核銷
            </div>

            <!-- 已過期提示 -->
            <div v-else-if="isExpired" class="alert alert-secondary mb-0">
                此票券已過期，無法核銷
            </div>

            <!-- 可核銷：顯示確認按鈕 -->
            <div v-else-if="canVerify"
                class="alert alert-warning d-flex justify-content-between align-items-center mb-0">
                <span>⚠️ 確認後無法復原，請確認已於門市完成服務</span>
                <button class="btn btn-primary ms-3 text-nowrap" :disabled="isSubmitting"
                    @click="showConfirmDialog = true">
                    <span v-if="isSubmitting">處理中...</span>
                    <span v-else>確認手動核銷</span>
                </button>
            </div>
        </div>
    </div>

    <AlertToast ref="toast" />

    <ConfirmDialog :isVisible="showConfirmDialog" title="確認核銷" message="核銷後無法復原，請確認已於門市完成服務。" confirmText="確認核銷"
        @confirm="confirmVerify" @cancel="showConfirmDialog = false" />


</template>

<style scoped></style>