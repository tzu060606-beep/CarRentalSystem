<script setup>
import { ref, computed, onMounted } from 'vue'
import { voucherAPI } from '@/api/point/point_voucher'
import EmptyState from '@/components/common/EmptyState.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import AlertToast from '@/components/common/AlertToast.vue'


const vouchers = ref([]) //複數，存所有票券
const statusFilter = ref('') //篩選票券狀態
const showUseModal = ref(false) // Modal 預設關閉
const selectedVoucher = ref(null) // 預設沒有選中的票券
const isSubmitting = ref(false) // 預設不在送出中
const showSuccessModal = ref(false)//兌換成功後的提示彈窗
const showErrorModal = ref(false)//兌換失敗後的提示彈窗
const errorMessage = ref('')//錯誤訊息
const toast = ref(null)

const fetchVouchers = async () => {
    const stored = JSON.parse(localStorage.getItem('customer'))
    const custId = stored?.custId
    if (!custId) return
    // TODO: custId 等 F1 完成後換成真實的
    const response = await voucherAPI.getByCustId(custId);
    vouchers.value = response.data.data;
    checkExpireingVouchers()
}


//================ 檢查是否有即將到期的票券（15天內）=======================

const checkExpireingVouchers = () => {
    const today = new Date()
    const sevenDaysLater = new Date()
    sevenDaysLater.setDate(today.getDate() + 15)

    //找出UNUSED且7天內到期的票券
    const expiring = vouchers.value.filter(v => {
        if (v.status != 'UNUSED') return false
        // 正確（只要不是 UNUSED 就排除）
        const expiryDate = new Date(v.expiryDate)
        return expiryDate >= today && expiryDate <= sevenDaysLater
    })

    if (expiring.length > 0) {
        toast.value.show(
            `您有 ${expiring.length} 張票券將在 15 天內到期，請盡快使用！`,
            'warning',
            5000  // 顯示久一點
        )
    }
}

onMounted(() => {
    fetchVouchers()
})


const statusOptions = [
    { value: '', label: '全部' },    // 空字串代表不篩選
    { value: 'UNUSED', label: '未使用' },
    { value: 'USED', label: '已使用' },
    { value: 'EXPIRED', label: '已過期' },
]

// 票券沒有類別、沒有點數排序，只需要用 statusFilter 篩選狀態
const filteredVouchers = computed(() => {
    if (statusFilter.value === '') return vouchers.value  // 全部
    return vouchers.value.filter(v => v.status === statusFilter.value)
})

//傳入票券資訊並打開彈窗
function openUseModal(voucher) {
    showUseModal.value = true //設定打開Modal的變數為true
    selectedVoucher.value = voucher //設定selectedvoucher的值為傳入的voucher物件
}

//在Modal裡點擊確認使用
const confirmUse = async () => {
    //將isSubmitting轉為true表示這張票券送出中
    isSubmitting.value = true
    //把前面選定的票券編號傳入api
    try {
        //前面已經把整個voucher物件傳入selectVoucher裡面所以這裡直接拿voucherCode出來
        const voucherCode = selectedVoucher.value.voucherCode
        //console.log('voucherCode:', voucherCode);//確認用，在console印出後端回傳的data
        //useVoucher api傳入要使用的voucherCode
        await voucherAPI.use(selectedVoucher.value.voucherCode);
        //確認票券狀態不是已使用或已過期
        //因為顯示時已經有針對已使用未使用狀態用filteredVoucher篩過了，這裡不用在篩
        // if (!statusOptions.value=='USED'&& !statusOptions.value== 'EXPIRED') {
        // }
        showUseModal.value = false //關閉Modal
        showSuccessModal.value = true //開啟成功兌換的modal
        await fetchVouchers();//重新載入，更新票券狀態
    } catch (error) {
        errorMessage.value = error.response?.data?.message || '使用失敗，請稍後再試'
        showErrorModal.value = true
    } finally {
        isSubmitting.value = false
    }
}

//樣式轉換用
function getStatusLabel(status) {
    if (status === 'UNUSED') return '未使用'
    if (status === 'USED') return '已使用'
    if (status === 'EXPIRED') return '已過期'
    return status
}

function getStatusBadgeClass(status) {
    if (status === 'UNUSED') return 'bg-primary'
    if (status === 'USED') return 'bg-success'
    if (status === 'EXPIRED') return 'bg-secondary'
}

</script>

<template>
    <div class="my-voucher-page">

        <!-- ── 頁面標題 ── -->
        <div class="d-flex align-items-center justify-content-between mb-4">
            <div>
                <h1 class="my-voucher-page__title mb-1">我的票券</h1>
                <p class="text-secondary mb-0" style="font-size: var(--text-sm);">
                    所有透過點數兌換獲得的優惠券都在這裡
                </p>
            </div>
        </div>

        <!-- ── Tab 底線樣式篩選 ── -->
        <div class="voucher-tabs mb-4">
            <button v-for="option in statusOptions" :key="option.value" class="voucher-tab"
                :class="{ active: statusFilter === option.value }" @click="statusFilter = option.value">
                {{ option.label }}
                <span class="voucher-tab-count">
                    ({{option.value === ''
                        ? vouchers.length
                        : vouchers.filter(v => v.status === option.value).length}})
                </span>
            </button>
        </div>

        <!-- ── 票券列表（列表式） ── -->
        <div class="voucher-list">
            <div v-for="voucher in filteredVouchers" :key="voucher.voucherId" class="voucher-item" :class="{
                'voucher-item--unused': voucher.status === 'UNUSED',
                'voucher-item--used': voucher.status === 'USED',
                'voucher-item--expired': voucher.status === 'EXPIRED'
            }">

                <!-- 左側彩色豎線 -->
                <div class="voucher-item__bar"></div>

                <!-- 主要內容 -->
                <div class="voucher-item__body">
                    <div class="voucher-item__meta">
                        <!-- 商品類別（從 redemptionOrder 進 product 取） -->
                        <span class="voucher-item__category">
                            {{ voucher.redemptionOrder?.product?.category }}
                        </span>
                        <span class="badge ms-2" :class="getStatusBadgeClass(voucher.status)">
                            {{ getStatusLabel(voucher.status) }}
                        </span>
                    </div>

                    <!-- 商品名稱（從 redemptionOrder 取） -->
                    <h5 class="voucher-item__name">
                        {{ voucher.redemptionOrder?.product?.productName }}
                    </h5>

                    <div class="voucher-item__info">
                        <!-- 票券號碼 -->
                        <span class="voucher-item__code"># {{ voucher.voucherCode }}</span>
                        <!-- 有效期 -->
                        <span class="voucher-item__expiry">
                            <i class="fa-regular fa-calendar me-1"></i>
                            有效至 {{ voucher.expiryDate?.slice(0, 10) }}
                        </span>
                        <!-- 使用時間（已使用才顯示） -->
                        <span v-if="voucher.useTime" class="voucher-item__expiry">
                            已使用 {{ voucher.useTime?.slice(0, 10) }}
                        </span>
                    </div>
                </div>

                <!-- 右側：按鈕 + 兌換點數 -->
                <div class="voucher-item__actions">
                    <!-- 立即使用按鈕（只有 UNUSED 才顯示） -->
                    <button v-if="voucher.status === 'UNUSED'" class="btn btn-primary btn-sm"
                        @click="openUseModal(voucher)">
                        <i class="fa-solid fa-qrcode me-1"></i>立即使用
                    </button>
                    <button v-else class="btn btn-outline-secondary btn-sm" disabled>
                        {{ getStatusLabel(voucher.status) }}
                    </button>
                    <!-- 兌換點數（從 redemptionOrder 進 product 取） -->
                    <span class="voucher-item__points">
                        {{ voucher.redemptionOrder?.product?.pointsRequired?.toLocaleString() }} 點兌換
                    </span>
                </div>
            </div>
        </div>

        <!-- ── 空狀態 ── -->
        <div class="text-center py-5" v-if="filteredVouchers.length === 0">
            <EmptyState icon="ticket" message="目前沒有票券" subMessage="去兌換商品取得票券吧！" />
        </div>

        <!-- ── 使用確認 Modal ── -->
        <BaseModal :isVisible="showUseModal" title="確認使用票券" size="sm" @close="showUseModal = false">
            <template #body>
                <div v-if="selectedVoucher" class="text-center py-2">
                    <p class="text-secondary mb-2">確定要使用以下票券？</p>
                    <div class="voucher-card__code mb-2">
                        {{ selectedVoucher.voucherCode }}
                    </div>

                  <!-- QR Code圖片 -->
                    <div class="text-center my-3">
                        <img :src="`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${selectedVoucher.voucherCode}`"
                            :alt="selectedVoucher.voucherCode" width="180" height="180"
                            style="border-radius: var(--radius-md);" />
                        <p class="text-secondary mt-2 mb-0" style="font-size: var(--text-xs);">
                            出示此 QR Code 給門市人員掃描
                        </p>
                    </div>



                    <p class="text-secondary mb-0" style="font-size: var(--text-sm);">
                        {{ selectedVoucher.redemptionOrder?.product?.productName }}
                    </p>
                    <div class="alert alert-warning mt-3 mb-0" style="font-size: var(--text-sm);">
                        ⚠️ 請由門市人員協助操作，使用後無法復原
                    </div>
                </div>
            </template>
            <template #footer>
                <div class="d-flex gap-2">
                    <button class="btn btn-outline-secondary" @click="showUseModal = false">取消</button>
                    <button class="btn btn-primary" :disabled="isSubmitting" @click="confirmUse">
                        <span v-if="isSubmitting">處理中...</span>
                        <span v-else>確認使用</span>
                    </button>
                </div>
            </template>
        </BaseModal>

        <!-- ──確認使用後的提示用BaseModal ── -->

        <!-- 使用成功提示 -->
        <BaseModal :isVisible="showSuccessModal" title="使用成功" size="sm" @close="showSuccessModal = false">
            <template #body>
                <div class="text-center py-3">
                    <div style="font-size: 48px; margin-bottom: 16px;">✅</div>
                    <p class="mb-0 text-secondary">票券已成功使用！</p>
                </div>
            </template>
        </BaseModal>

        <!-- 使用失敗提示 -->
        <BaseModal :isVisible="showErrorModal" title="使用失敗" size="sm" @close="showErrorModal = false">
            <template #body>
                <div class="text-center py-3">
                    <div style="font-size: 48px; margin-bottom: 16px;">❌</div>
                    <p class="mb-0 text-secondary">{{ errorMessage }}</p>
                </div>
            </template>
        </BaseModal>

        <AlertToast ref="toast" />

    </div>
</template>

<style scoped>
.my-voucher-page {
    max-width: 1100px;
    margin: 0 auto;
    padding: var(--space-8);
    position: relative;
}

/* 頁面標題 */
.my-voucher-page__title {
    font-size: var(--text-2xl);
    font-weight: var(--font-bold);
    color: var(--color-text-primary);
    display: inline-block;
    border-bottom: 3px solid var(--color-primary);
    padding-bottom: var(--space-1);
}

/* Tab 底線樣式篩選 */
.voucher-tabs {
    display: flex;
    gap: var(--space-6);
    border-bottom: 1px solid var(--color-border);
}

.voucher-tab {
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

.voucher-tab:hover {
    color: var(--color-primary);
}

.voucher-tab.active {
    color: var(--color-primary);
    font-weight: var(--font-semibold);
    border-bottom-color: var(--color-primary);
}

.voucher-tab-count {
    color: var(--color-text-muted);
    font-size: var(--text-xs);
    margin-left: var(--space-1);
}

/* 列表式票券 */
.voucher-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
}

.voucher-item {
    display: flex;
    align-items: center;
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    overflow: hidden;
    transition: box-shadow var(--transition-fast);
}

.voucher-item:hover {
    box-shadow: var(--shadow-md);
}

/* 左側彩色豎線 */
.voucher-item__bar {
    width: 4px;
    align-self: stretch;
    flex-shrink: 0;
}

.voucher-item--unused .voucher-item__bar {
    background: var(--color-primary);
}

.voucher-item--used .voucher-item__bar {
    background: var(--color-success);
}

.voucher-item--expired .voucher-item__bar {
    background: var(--color-gray-300);
}

/* 已使用、已過期變暗 */
.voucher-item--used {
    opacity: 0.7;
}

.voucher-item--expired {
    opacity: 0.5;
}

/* 主要內容 */
.voucher-item__body {
    flex: 1;
    padding: var(--space-4) var(--space-5);
}

.voucher-item__meta {
    display: flex;
    align-items: center;
    margin-bottom: var(--space-1);
}

.voucher-item__category {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
}

.voucher-item__name {
    font-size: var(--text-base);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-2) 0;
}

.voucher-item__info {
    display: flex;
    gap: var(--space-4);
    align-items: center;
    flex-wrap: wrap;
}

.voucher-item__code {
    font-size: var(--text-sm);
    font-family: var(--font-mono);
    color: var(--color-primary);
    font-weight: var(--font-medium);
}

.voucher-item__expiry {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
}

/* 右側：按鈕 + 點數 */
.voucher-item__actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: var(--space-2);
    padding: var(--space-4) var(--space-5);
    flex-shrink: 0;
}

.voucher-item__points {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
}

/* Modal 內票券號碼樣式（沿用原本的） */
.voucher-card__code {
    font-size: var(--text-base);
    font-weight: var(--font-semibold);
    color: var(--color-primary);
    background: var(--color-primary-light);
    border: 1px dashed var(--color-primary-border-light);
    border-radius: var(--radius-md);
    padding: var(--space-3) var(--space-4);
    text-align: center;
    letter-spacing: 0.05em;
}
</style>