<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productAPI } from '@/api/point/point_product'
import { customerAPI } from '@/api/login/customerAPI'
import { redemptionOrderAPI } from '@/api/point/point_order'
import { voucherAPI } from '@/api/point/point_voucher'
import BaseModal from '@/components/common/BaseModal.vue'
import AlertToast from '@/components/common/AlertToast.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const route = useRoute()
const router = useRouter()

// 商品資料（假資料，之後接 API）
const product = ref(null)
const isLoading = ref(false)
const errorMsg = ref('')

// 兌換數量
const quantity = ref(1)

// 會員目前點數（假資料，之後從會員 store 拿）
const currentPoints = ref(0)



// 總共需要扣的點數
const totalPoints = computed(() => {
    if (!product.value) return 0
    return product.value.pointsRequired * quantity.value
})

// 點數是否足夠
const isPointsEnough = computed(() => {
    return currentPoints.value >= totalPoints.value
})

// 兌換按鈕是否可按
const canRedeem = computed(() => {
    if (!product.value) return false
    if (product.value.stockQuantity <= 0) return false
    if (!isPointsEnough.value) return false
    return true
})

// ============================================
// API 處理函式雛形（之後接後端時填內容）
// ============================================
const fetchProductDetail = async (productId) => {
    // 詳細頁載入時 isLoading 是 false，product 也是 null，所以畫面會先閃一下空白再顯示載入中。
    isLoading.value = true  // ← 確認這行在最前面
    errorMsg.value = ''
    try {
        const response = await productAPI.getById(productId)
        product.value = response.data.data

    } catch (err) {
        errorMsg.value = '商品資料載入失敗'
    } finally {
        isLoading.value = false
    }
}


// ============================================
// 觸發兌換
// ============================================
const isSubmitting = ref(false)  // 【新增】防止重複送出，按鈕 disabled 用

const submitRedeem = async () => {
    // 1. 把 isSubmitting 設為 true（這裡用 showConfirmModal 控制，不需要另外加）
    isSubmitting.value = true
    try {
        // 2. 打 redemptionOrderAPI.insert()，傳入哪些參數？
        // custId 換成現在登入者的 id
        // 【為什麼不傳 custId】後端從 session/token 取得登入者身份，更安全
        const response = await redemptionOrderAPI.insert({
            productId: product.value.productId,
            productQuantity: quantity.value
        })

        // 3. 成功後拿到 redemptionId，查對應的票券
        const newOrder = response.data.data

        // 拿到 redemptionId 後查對應的 voucher
        // 【為什麼要查票券】兌換成功後才產生票券，需要拿到票券號碼顯示給使用者
        const vouchersResponse = await voucherAPI.getByRedemptionId(newOrder.redemptionId)
        const newVouchers = vouchersResponse.data.data

        // 4. 關閉 Modal，成功後顯示提示
        showConfirmDialog.value = false
        redeemVouchers.value = newVouchers
        showResultModal.value = true

        // AlertToast 顯示點數剩餘
        // 【為什麼重新打 API】確保點數和庫存是資料庫最新的值，不自己算
        const stored = JSON.parse(localStorage.getItem('customer'))
        if (stored?.custId) {
            const customerResponse = await customerAPI.getById(stored.custId)
            currentPoints.value = customerResponse.data.currentPoints
        }
        toast.value.show(`兌換成功！您的點數剩餘 ${currentPoints.value} 點`, 'success', 5000)
        await fetchProductDetail(product.value.productId)

    } catch (err) {
        console.error(err)
        toast.value.show(err.response?.data?.message || '兌換失敗，請稍後再試', 'danger', 5000)
    } finally {
        // 【容易忽略】不管成功或失敗都要設回 false，按鈕才能再次點擊
        isSubmitting.value = false
    }
}



// ============================================
// 事件處理
// ============================================
const handleRedeemClick = () => {
    if (!canRedeem.value) return
    showConfirmDialog.value = true
}

const handleQuantityChange = (delta) => {
    const newQty = quantity.value + delta
    if (newQty < 1) return
    if (product.value && newQty > product.value.stockQuantity) return
    quantity.value = newQty
}

const goBack = () => {
    router.push('/point/productlist')
}



// ============================================
// 兌換前警示
// ============================================
const showConfirmDialog = ref(false)   // ConfirmDialog 開關
const showResultModal = ref(false)     // BaseModal 顯示票券號碼
const redeemVouchers = ref([])         // 存兌換成功的票券列表
const toast = ref(null)                // AlertToast 實例


onMounted(async () => {
   
    const productId = route.params.id || 1
    await fetchProductDetail(productId)

    // 從 localStorage 取 custId，打 API 取最新點數
    const stored = JSON.parse(localStorage.getItem('customer'))
    if (stored?.custId) {
        const response = await customerAPI.getById(stored.custId)
        currentPoints.value = response.data.currentPoints
    }
})
</script>

<template>
    <div class="container py-4">

        <!-- 返回按鈕 -->
        <div class="mb-3">
            <button @click="goBack" class="btn" style="background-color: #f0f0f0; color: #333;">
                ← 返回商品列表
            </button>
        </div>


        <!-- 載入中 -->
        <div v-if="isLoading" style="text-align: center; padding: 50px;">
            <div class="spinner-border" role="status"></div>
            <p style="margin-top: 10px; color: #888;">載入中...</p>
        </div>

        <!-- 錯誤 -->
        <div v-else-if="errorMsg" style="text-align: center; padding: 50px; color: red;">
            {{ errorMsg }}
        </div>

        <!-- 商品內容 -->
        <div v-else-if="product" class="wrapper">

            <!-- 麵包屑 -->
            <div style="font-size: 13px; color: #666; margin-bottom: 20px;">
                <span style="cursor: pointer; color: #0150AD;" @click="router.push('/')">首頁</span>
                <span style="margin: 0 8px;">/</span>
                <span style="cursor: pointer; color: #0150AD;" @click="router.push('/point/productlist')">點數兌換</span>
                <span style="margin: 0 8px;">/</span>
                <span>{{ product.productName }}</span>
            </div>

            <div class="row">
                <!-- 商品圖片 -->
                <div class="col-md-6">
                    <div style="background-color: #f5f5f5; border-radius: 8px; padding: 20px; text-align: center;">
                        <img :src="product.imageUrl" :alt="product.name" style="max-width: 100%; height: auto;" />
                    </div>
                </div>

                <!-- 商品資訊 -->
                <div class="col-md-6">
                    <div style="padding-left: 20px;">

                        <!-- 類別標籤 -->
                        <span style="
                display: inline-block;
                background-color: #DBA772;
                color: white;
                padding: 4px 12px;
                border-radius: 12px;
                font-size: 12px;
                margin-bottom: 12px;
              ">
                            {{ product.category }}
                        </span>

                        <!-- 商品名稱 -->
                        <h2 style="font-size: 24px; font-weight: bold; color: #222; margin-bottom: 15px;">
                            {{ product.productName }}
                        </h2>

                        <!-- 點數價格 -->
                        <div style="margin-bottom: 20px;">
                            <span style="color: #888; font-size: 14px;">所需點數</span>
                            <div style="display: flex; align-items: center; gap: 6px; margin-top: 4px;">
                                <span style="color: #DBA772; font-size: 28px;">★</span>
                                <span style="font-size: 32px; font-weight: bold; color: #DBA772;">
                                    {{ product.pointsRequired.toLocaleString() }}
                                </span>
                                <span style="color: #555; font-size: 16px;">點</span>
                            </div>
                        </div>

                        <!-- 庫存 -->
                        <div style="margin-bottom: 20px; color: #555;">
                            庫存：
                            <span
                                :style="{ color: product.stockQuantity > 0 ? '#28a745' : '#dc3545', fontWeight: 'bold' }">
                                {{ product.stockQuantity > 0 ? `剩餘 ${product.stockQuantity} 個` : '已售完' }}
                            </span>
                        </div>

                        <!-- 商品說明 -->
                        <div style="margin-bottom: 20px;">
                            <p style="font-size: 14px; color: #555; line-height: 1.7; white-space: pre-line;">
                                {{ product.description }}
                            </p>
                        </div>

                        <!-- 注意事項 -->
                        <div style="
                background-color: #fff8e1;
                border-left: 3px solid #ffc107;
                padding: 12px 15px;
                margin-bottom: 25px;
                border-radius: 4px;
              ">
                            <div style="font-size: 13px; font-weight: bold; color: #856404; margin-bottom: 5px;">
                                ⚠ 兌換注意事項
                            </div>
                            <div style="font-size: 13px; color: #856404;">
                                兌換後無法取消或退回點數，請確認後再行兌換
                            </div>
                        </div>

                        <!-- 數量選擇 -->
                        <div style="margin-bottom: 20px;">
                            <div style="font-size: 14px; color: #555; margin-bottom: 8px;">兌換數量</div>
                            <div style="display: flex; align-items: center; gap: 0;">
                                <button @click="handleQuantityChange(-1)" :disabled="quantity <= 1" style="
                    width: 36px;
                    height: 36px;
                    border: 1px solid #ccc;
                    background: white;
                    border-radius: 4px 0 0 4px;
                    cursor: pointer;
                  ">
                                    −
                                </button>
                                <input v-model.number="quantity" type="number" readonly style="
                    width: 60px;
                    height: 36px;
                    text-align: center;
                    border: 1px solid #ccc;
                    border-left: none;
                    border-right: none;
                  " />
                                <button @click="handleQuantityChange(1)" :disabled="quantity >= product.stockQuantity"
                                    style="
                    width: 36px;
                    height: 36px;
                    border: 1px solid #ccc;
                    background: white;
                    border-radius: 0 4px 4px 0;
                    cursor: pointer;
                  ">
                                    ＋
                                </button>
                            </div>
                        </div>

                        <!-- 總計 -->
                        <div style="
                background-color: #f8f9fa;
                padding: 15px;
                border-radius: 6px;
                margin-bottom: 20px;
              ">
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                                <span style="color: #555;">本次扣抵點數</span>
                                <span style="font-weight: bold; color: #DBA772;">
                                    ★ {{ totalPoints.toLocaleString() }} 點
                                </span>
                            </div>
                            <div style="display: flex; justify-content: space-between;">
                                <span style="color: #555;">兌換後剩餘點數</span>
                                <span :style="{
                                    fontWeight: 'bold',
                                    color: isPointsEnough ? '#0150AD' : '#dc3545'
                                }">
                                    {{ (currentPoints - totalPoints).toLocaleString() }} 點
                                </span>
                            </div>
                            <div v-if="!isPointsEnough" style="color: #dc3545; font-size: 13px; margin-top: 8px;">
                                點數不足，無法兌換
                            </div>
                        </div>

                        <!-- 兌換按鈕 -->
                        <button @click="handleRedeemClick" :disabled="!canRedeem" style="
                width: 100%;
                padding: 12px;
                background-color: #0150AD;
                color: white;
                border: none;
                border-radius: 6px;
                font-size: 16px;
                font-weight: bold;
                cursor: pointer;
              " :style="{
                backgroundColor: canRedeem ? '#0150AD' : '#999',
                cursor: canRedeem ? 'pointer' : 'not-allowed',
                width: '100%',
                padding: '12px',
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                fontSize: '16px',
                fontWeight: 'bold'
            }">
                            立即兌換
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 確認兌換 ConfirmDialog -->
        <ConfirmDialog :isVisible="showConfirmDialog" title="確認兌換"
            :message="`確定要兌換 ${product?.productName} x ${quantity} 嗎？\n將扣除 ${totalPoints} 點，兌換後無法取消。`" confirmText="確認兌換"
            @confirm="submitRedeem" @cancel="showConfirmDialog = false" />

        <!-- 兌換成功 BaseModal（顯示票券號碼） -->
        <BaseModal :isVisible="showResultModal" title="兌換成功！" size="sm" @close="showResultModal = false">
            <template #body>
                <div class="text-center py-3">
                    <div style="font-size: 48px; margin-bottom: 16px;">🎉</div>
                    <p class="text-secondary mb-3">您的票券號碼：</p>
                    <div v-for="voucher in redeemVouchers" :key="voucher.voucherCode"
                        class="alert alert-primary fw-bold font-monospace mb-2">
                        {{ voucher.voucherCode }}
                    </div>
                    <p class="text-muted" style="font-size: 0.85rem;">
                        請前往「我的票券」查看所有票券
                    </p>
                </div>
            </template>
            <template #footer>
                <button class="btn btn-primary" @click="showResultModal = false">確認</button>
            </template>
        </BaseModal>

        <!-- AlertToast -->
        <AlertToast ref="toast" />

    </div>
</template>

<style scoped>
.container {
    max-width: 1200px;
    margin: 0 auto;
}

.wrapper {
    background: white;
    padding: 25px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.row {
    display: flex;
    flex-wrap: wrap;
    margin: -10px;
}

.col-md-6 {
    flex: 0 0 50%;
    max-width: 50%;
    padding: 10px;
}

@media (max-width: 768px) {
    .col-md-6 {
        flex: 0 0 100%;
        max-width: 100%;
    }
}
</style>