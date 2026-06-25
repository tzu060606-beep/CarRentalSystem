<script setup>
import { ref, computed, onMounted } from 'vue'
import { productAPI } from '@/api/point/point_product'
import EmptyState from '@/components/common/EmptyState.vue'
import { customerAPI } from '@/api/login/customerAPI'

//myPoints要接這位客戶的點數資料
const myPoints = ref('')
const customerData = ref(null)
const activeCategory = ref('all')
const sortBy = ref('default')
const toastMessage = ref('')
const products = ref([])

const fetchProducts = async () => {
  const response = await productAPI.getAll();
  products.value = response.data.data
}

//掛載
onMounted(async () => {
  fetchProducts()

  // 從 localStorage 取 custId
  const stored = JSON.parse(localStorage.getItem('customer'))
  if (stored?.custId) {
    customerData.value = stored
    // 打 API 取最新客戶資料（包含最新點數）
    const response = await customerAPI.getById(stored.custId)
    myPoints.value = response.data.currentPoints
  }
})

const categories = [
  { value: 'all', label: '全部' },
  { value: 'carparts', label: '汽車配件' },
  { value: 'addon', label: '加值服務' },
  { value: 'gift', label: '禮品兌換' },
  { value: 'partner', label: '合作品牌' },
]


const filteredproducts = computed(() => {
  //先將已下架商品過濾掉(isActive為false)
  //先取所有商品列表在用filter過濾已下架的商品
  let list = products.value.filter(p => p.isActive === true)

  //類別篩選
  if (activeCategory.value !== 'all') {
    const map = { carparts: '汽車配件', addon: '加值服務', gift: '禮品兌換', partner: '合作品牌' }
    list = list.filter(i => i.category === map[activeCategory.value])
  }
  //根據所需點數排序
  if (sortBy.value === 'points-asc') return [...list].sort((a, b) => a.pointsRequired - b.pointsRequired)
  if (sortBy.value === 'points-desc') return [...list].sort((a, b) => b.pointsRequired - a.pointsRequired)
  return list
})


//根據不同類別變換標籤顏色
function getCategoryBadgeClass(category) {
  if (category === '汽車配件') return 'bg-primary'
  if (category === '加值服務') return 'bg-info'
  if (category === '禮品兌換') return 'bg-warning'
  if (category === '合作品牌') return 'bg-success'
  return 'bg-secondary'
}

const redeemableCount = computed(() =>
  products.value.filter(i => canRedeem(i)).length
)

function canRedeem(product) {
  return myPoints.value >= product.pointsRequired && product.stockQuantity > 0
}

//根據庫存進度條的顏色產生變化
function stockColor(product) {
  const ratio = product.stockQuantity / 100
  if (ratio === 0) return 'var(--color-gray-300)'
  if (ratio <= 0.2) return 'var(--color-danger)'
  if (ratio <= 0.5) return 'var(--color-warning)'
  return 'var(--color-success)'
}


//================================================================================
//--------------------------兌換後的確認用Modal script-----------------------------
//================================================================================
import BaseModal from '@/components/common/BaseModal.vue'
import { redemptionOrderAPI } from '@/api/point/point_order'
import { voucherAPI } from '@/api/point/point_voucher'
import AlertToast from '@/components/common/AlertToast.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

// Modal 相關狀態
const showRedeemModal = ref(false)
const selectedProduct = ref(null)
const quantity = ref(1)
const isSubmitting = ref(false)

// 兌換結果（成功後顯示票券）
const redeemResult = ref(null) //→ 存兌換訂單（用來判斷是否兌換成功、顯示成功畫面）
const redeemVouchers = ref([]);//→ 存票券陣列（用來顯示票券號碼）

const showErrorModal = ref(false)
const errorMessage = ref('')

//剩餘點數alert
const toast = ref(null)

// 確認兌換前的二次確認 ConfirmDialog 開關
// 【為什麼要二次確認】兌換後無法取消，多一層確認防止誤操作
const showFinalConfirm = ref(false)


// 點擊立即兌換 → 開啟 Modal
function redeem(product) {
  if (!canRedeem(product)) return
  selectedProduct.value = product
  quantity.value = 1
  redeemResult.value = null
  showRedeemModal.value = true
}

// 計算這次要花的點數
const totalPointsNeeded = computed(() => {
  if (!selectedProduct.value) return 0
  return selectedProduct.value.pointsRequired * quantity.value
})

// Modal 裡點「確認兌換」→ 先開 ConfirmDialog 做二次確認
// 【為什麼不直接打 API】讓使用者有機會反悔，避免誤觸
const confirmRedeem = () => {
  showFinalConfirm.value = true
}

// ConfirmDialog 確認後，才真正打 API 執行兌換
const doRedeem = async () => {
  isSubmitting.value = true
  try {
    //custId換成現在登入者的id
    const response = await redemptionOrderAPI.insert({
      productId: selectedProduct.value.productId,
      productQuantity: quantity.value
    })
    // console.log('回傳結果：', response.data)
    redeemResult.value = response.data.data
    // 拿到 redemptionId 後查對應的 voucher
    const vouchers = await voucherAPI.getByRedemptionId(redeemResult.value.redemptionId)
    redeemVouchers.value = vouchers.data.data

    // 兌換成功後更新點數
    myPoints.value -= totalPointsNeeded.value

    // 點數剩餘不多時跳 warning（門檻設 500）
    toast.value.show(
      `兌換成功！您的點數剩餘 ${myPoints.value} 點`,
      'info',
      5000)

    // 兌換成功後，重新打 API 取最新點數
    const response2 = await customerAPI.getById(customerData.value.custId)
    myPoints.value = response2.data.currentPoints

  } catch (err) {
    errorMessage.value = err.response?.data?.message || '兌換失敗，請稍後再試'
    showErrorModal.value = true
  } finally {
    isSubmitting.value = false
    // 【容易忽略】不管成功失敗都要關閉 ConfirmDialog
    showFinalConfirm.value = false
  }
}

// 關閉 Modal
const closeModal = () => {
  showRedeemModal.value = false
  selectedProduct.value = null
  redeemResult.value = null
  quantity.value = 1
  // 兌換成功後重新載入商品列表（更新庫存）
  if (redeemResult.value) fetchProducts()
}

//定義只在FrontLayout跳轉
const props = defineProps({
  // 【為什麼用 props】讓父層決定這個頁面是否允許點卡片跳轉詳細頁
  // 從 Nav 進入的商品頁：allowNavigate = true（可跳轉）
  // 從 MemberLayout 嵌入的商品頁：allowNavigate = false（不跳轉）
  allowNavigate: { type: Boolean, default: true }
})

import { useRoute } from 'vue-router'
const route = useRoute()

// 【為什麼用路徑判斷】MemberLayout 用 RouterView 無法直接傳 props
// /point/productlist 進來 → 允許點卡片跳轉詳細頁
// /customer/member/products 進來 → 不允許跳轉，只保留立即兌換按鈕
const allowNavigate = computed(() => {
  return route.path === '/point/productlist'
})

</script>


<template>
  <div class="point-list-page">

    <!-- ========================== -->
    <!-- 商品頁 -->
    <!-- ========================== -->

    <!-- ── 頁面標題 ── -->
    <div class="d-flex align-items-center justify-content-between mb-4">
      <div>
        <h1 class="point-list-page__title mb-2">點數商品列表</h1>
        <p class="text-secondary mb-0" style="font-size: var(--text-sm);">
          使用累積點數兌換專屬優惠與禮遇
        </p>
      </div>
      <div class="point-list-page__balance">
        <span class="point-list-page__balance-label">我的點數</span>
        <span class="point-list-page__balance-value">{{ myPoints.toLocaleString() }}</span>
        <span class="point-list-page__balance-unit">pt</span>
      </div>
    </div>

    <!-- ── 篩選列 ── -->
    <div class="d-flex align-items-center gap-2 mb-4 flex-wrap">
      <button v-for="cat in categories" :key="cat.value" class="btn btn-sm px-3" :class="activeCategory === cat.value
        ? 'btn-primary-pill-active'
        : 'btn-primary-pill'" @click="activeCategory = cat.value">
        {{ cat.label }}
      </button>
      <div class="ms-auto d-flex align-items-center gap-2">
        <select class="form-select form-select-sm" style="width: auto;" v-model="sortBy">
          <option value="default">預設排序</option>
          <option value="points-asc">點數：低到高</option>
          <option value="points-desc">點數：高到低</option>
        </select>
      </div>
    </div>

    <!-- ── 可兌換提示 ── -->
    <div class="alert alert-primary d-flex align-items-center gap-2 mb-4" v-if="redeemableCount > 0">
      <strong>✓ 共 {{ redeemableCount }} 件商品</strong>可用您目前的點數兌換
    </div>

    <!-- ── 商品列表 ── -->
    <div class="row g-4">
      <div class="col-md-6 col-lg-4" v-for="product in filteredproducts" :key="product.productId">
       <div class="card h-100 point-card" :class="{ 'point-card--locked': !canRedeem(product) }"
          @click="allowNavigate ? $router.push(`/point/products/${product.productId}`) : null"
          :style="allowNavigate ? 'cursor: pointer;' : ''">

          <!-- 分類標籤 -->
          <div class="point-card__category-tag">
            <span class="badge" :class="getCategoryBadgeClass(product.category)">{{ product.category }}</span>
          </div>

          <!-- 圖示區 -->
          <div class="point-card__icon-area">
            <!-- 綁定圖片網址 -->
            <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.productName"
              style="width: 100%; height: 100%; object-fit: cover;">
            <!-- 有圖片就顯示圖片，沒有圖片就顯示預設的icon -->
            <div v-else style="font-size: 40px; display:flex; align-items:center; justify-content:center; height:100%;">
              <font-awesome-icon icon="fa-solid fa-image" style="color: rgb(2, 80, 172);" />
            </div>
            <!-- <span class="point-card__icon">{{ product.imageUrl }}</span> -->
          </div>

          <div class="card-body d-flex flex-column">
            <h5 class="card-title mb-1">{{ product.productName }}</h5>
            <p class="card-text text-secondary mb-3" style="font-size: var(--text-sm); flex: 1;">
              {{ product.description }}
            </p>

            <!-- 點數與有效期 -->
            <div class="d-flex align-items-center justify-content-between mb-3">
              <div class="point-card__cost">
                <span class="point-card__cost-num">{{ product.pointsRequired.toLocaleString() }}</span>
                <span class="point-card__cost-unit">pt</span>
              </div>
            </div>

            <!-- 庫存進度條 -->
            <div class="mb-3">
              <div class="d-flex justify-content-between mb-1">
                <span style="font-size: var(--text-xs); color: var(--color-text-secondary);">剩餘數量</span>
                <span style="font-size: var(--text-xs); color: var(--color-text-secondary);">{{ product.stockQuantity
                }}</span>
              </div>

              <!-- 進度條 -->
              <div class="progress" style="height: 4px; border-radius: var(--radius-full);">
                <div class="progress-bar" :style="{
                  width: Math.min(product.stockQuantity, 100) + '%',
                  background: stockColor(product)
                }"></div>
              </div>
            </div>

            <!-- 兌換按鈕 -->
            <button class="btn btn-sm btn-full" :class="canRedeem(product) ? 'btn-redeem' : 'btn-outline-secondary'"
              @click.stop="canRedeem(product)
                ? redeem(product)
                : myPoints.value < product.pointsRequired
                  ? toast.value.show('點數不足，無法兌換此商品', 'danger', 3000)
                  : null">
              <span v-if="canRedeem(product)">立即兌換</span>
              <span v-else-if="myPoints < product.pointsRequired">點數不足</span>
              <span v-else>已售完</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 空狀態 ── -->
    <div class="text-center py-5" v-if="filteredproducts.length === 0">
      <EmptyState icon="inbox" message="此分類目前沒有商品" subMessage="請點擊其他分類查看" />
    </div>

    <!-- ── 兌換成功 Toast ── -->
    <div class="point-list-page__toast alert alert-success" v-if="toastMessage">
      ✓ {{ toastMessage }}
    </div>

  </div>



  <!-- ========================== -->
  <!-- 兌換確認 Modal -->
  <!-- ========================== -->
  <BaseModal :isVisible="showRedeemModal" :title="redeemResult ? '兌換成功！' : '確認兌換'" size="md" @close="closeModal">
    <template #body>

      <!-- 兌換成功：顯示票券號碼 -->
      <!-- v-if="redeemResult" 判斷是否顯示成功畫面 -->
      <div v-if="redeemResult" class="text-center py-3">
        <div style="font-size: 48px; margin-bottom: 16px;">🎉</div>
        <h5 class="fw-bold mb-3">兌換成功！</h5>
        <p class="text-secondary mb-3">您的票券號碼：</p>
        <!-- v-for="voucher in redeemVouchers" 顯示票券號碼 -->
        <div v-for="voucher in redeemVouchers" :key="voucher.voucherCode"
          class="alert alert-primary fw-bold font-monospace mb-2">
          {{ voucher.voucherCode }}
        </div>
        <p class="text-muted" style="font-size: 0.85rem;">
          請前往「我的票券」查看所有票券
        </p>
      </div>

      <!-- 確認兌換：顯示商品資訊 -->
      <div v-else-if="selectedProduct">

        <!-- 商品資訊 -->
        <div class="d-flex gap-3 mb-4">
          <img v-if="selectedProduct.imageUrl" :src="selectedProduct.imageUrl" :alt="selectedProduct.productName"
            style="width: 80px; height: 80px; object-fit: cover; border-radius: 8px;">
          <div v-else class="d-flex align-items-center justify-content-center bg-light"
            style="width: 80px; height: 80px; border-radius: 8px; font-size: 32px;">
            🎁
          </div>
          <div>
            <h6 class="fw-bold mb-1">{{ selectedProduct.productName }}</h6>
            <span :class="getCategoryBadgeClass(selectedProduct.category)" class="badge mb-2">
              {{ selectedProduct.category }}
            </span>
            <p class="text-secondary mb-0" style="font-size: 0.85rem;">
              {{ selectedProduct.description }}
            </p>
          </div>
        </div>

        <!-- 數量選擇 -->
        <div class="mb-3">
          <label class="form-label fw-semibold">兌換數量</label>
          <div class="d-flex align-items-center gap-3">
            <button class="btn btn-outline-secondary btn-sm" @click="quantity > 1 ? quantity-- : null">－</button>
            <span class="fw-bold fs-5">{{ quantity }}</span>
            <button class="btn btn-outline-secondary btn-sm"
              @click="quantity < selectedProduct.stockQuantity ? quantity++ : null">＋</button>
            <span class="text-muted" style="font-size: 0.85rem;">
              （庫存剩 {{ selectedProduct.stockQuantity }} 件）
            </span>
          </div>
        </div>

        <!-- 點數計算 -->
        <div class="bg-light rounded p-3">
          <div class="d-flex justify-content-between mb-2">
            <span class="text-secondary">單件點數</span>
            <span>{{ selectedProduct.pointsRequired.toLocaleString() }} pt</span>
          </div>
          <div class="d-flex justify-content-between mb-2">
            <span class="text-secondary">兌換數量</span>
            <span>× {{ quantity }}</span>
          </div>
          <hr class="my-2">
          <div class="d-flex justify-content-between fw-bold">
            <span>合計點數</span>
            <span class="text-primary">{{ totalPointsNeeded.toLocaleString() }} pt</span>
          </div>
          <div class="d-flex justify-content-between mt-2" style="font-size: 0.85rem;">
            <span class="text-secondary">兌換後剩餘</span>
            <span :class="myPoints - totalPointsNeeded >= 0 ? 'text-success' : 'text-danger'">
              {{ (myPoints - totalPointsNeeded).toLocaleString() }} pt
            </span>
          </div>
        </div>

      </div>
    </template>

    <template #footer>
      <!-- 兌換成功：只有關閉按鈕 -->
      <div v-if="redeemResult">
        <button class="btn btn-primary" @click="closeModal">確認</button>
      </div>
      <!-- 確認兌換：取消 + 確認按鈕 -->
      <div v-else class="d-flex gap-2">
        <button class="btn btn-outline-secondary" @click="closeModal">取消</button>
        <button class="btn btn-primary" :disabled="isSubmitting || totalPointsNeeded > myPoints" @click="confirmRedeem">
          <span v-if="isSubmitting">處理中...</span>
          <span v-else>確認兌換</span>
        </button>
      </div>
    </template>

  </BaseModal>

  <BaseModal :isVisible="showErrorModal" title="兌換失敗" size="sm" @close="showErrorModal = false">
    <template #body>
      <div class="text-center py-3">
        <div style="font-size: 48px; margin-bottom: 16px;">❌</div>
        <p class="mb-0 text-secondary">{{ errorMessage }}</p>
      </div>
    </template>
  </BaseModal>

  <AlertToast ref="toast" />

  <ConfirmDialog :isVisible="showFinalConfirm" title="再次確認兌換"
    :message="`確定要兌換 ${selectedProduct?.productName} x ${quantity} 件嗎？\n此操作無法取消。`" confirmText="確認兌換" @confirm="doRedeem"
    @cancel="showFinalConfirm = false" />

</template>


<style scoped>
.point-list-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: var(--space-8);
  position: relative;
  background: var(--color-bg-page);
  min-height: 100vh;
}

/* 頁面標題 */
.point-list-page__title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  border-bottom: 3px solid var(--color-primary);
  /* 加左邊藍線 */
  padding-bottom: var(--space-2);
  /* 文字跟線的間距 */
  display: inline-block;
}

/* 點數餘額區塊 */
.point-list-page__balance {
  display: flex;
  align-items: baseline;
  gap: var(--space-1);
  background: var(--color-primary-light);
  border: 1px solid var(--color-primary-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-3) var(--space-5);
}

.point-list-page__balance-label {
  font-size: var(--text-sm);
  color: var(--color-primary-hover);
  font-weight: var(--font-medium);
  margin-right: var(--space-2);
}

.point-list-page__balance-value {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-primary);
  font-family: var(--font-mono);
}

.point-list-page__balance-unit {
  font-size: var(--text-sm);
  color: var(--color-primary);
  font-weight: var(--font-medium);
}

/* 卡片 */
.point-card {
  border-width: 1px;
  /* 原本 0.5px，加粗一點 */
  border-color: var(--color-border-strong);
  /* 用較深的邊框色 */
  box-shadow: var(--shadow-sm);
  /* 預設就有輕微陰影 */
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--color-bg-surface);
  /* 確保卡片是白色 */
}

.point-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.point-card--locked {
  opacity: 0.7;
}

.point-card--locked:hover {
  transform: none;
  box-shadow: var(--shadow-sm);
}

/* 分類標籤位置 */
.point-card__category-tag {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  z-index: 1;
}

/* 圖示區 */
.point-card__icon-area {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  border-bottom: 0.5px solid var(--color-border);
  overflow: hidden;
}

.point-card__icon {
  font-size: 40px;
  line-height: 1;
}

/* 點數顯示 */
.point-card__cost {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.point-card__cost-num {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-primary);
  font-family: var(--font-mono);
}

.point-card__cost-unit {
  font-size: var(--text-xs);
  color: var(--color-primary);
  font-weight: var(--font-medium);
}

/* 全寬按鈕 */
.btn-full {
  width: 100%;
  border-radius: var(--radius-md);
}

/* Toast 通知 */
.point-list-page__toast {
  position: fixed;
  bottom: var(--space-8);
  right: var(--space-8);
  z-index: 1000;
  min-width: 280px;
  box-shadow: var(--shadow-lg);
  animation: slideUp 0.3s ease;
}

/* 兌換按鈕：橘色系，hover 變藍 */
.btn-redeem {
  background-color: var(--color-sandy-100);
  border-color: var(--color-sandy-100);
  color: var(--color-sandy-700);
  transition: background-color var(--transition-fast), border-color var(--transition-fast);
}

.btn-redeem:hover {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

/* 膠囊篩選按鈕 */
.btn-primary-pill {
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border-strong);
  background: white;
  color: var(--color-text-secondary);
}

.btn-primary-pill:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-primary-pill-active {
  border-radius: var(--radius-full);
  border: 1px solid var(--color-primary);
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: var(--font-medium);
}


@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
