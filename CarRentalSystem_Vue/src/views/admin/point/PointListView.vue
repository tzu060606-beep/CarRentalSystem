<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAllProducts } from '@/api/point/point_product'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'

//myPoints要接這位客戶的點數資料
const myPoints = ref(3500)
const activeCategory = ref('all')
const sortBy = ref('default')
const toastMessage = ref('')
const products = ref([])
const router = useRouter();

const fetchProducts = async () => {
  const response = await getAllProducts();
  products.value = response.data
}

//掛載
onMounted(() => {
  fetchProducts()
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

//需更改為兌換後進入兌換確認頁，並帶著商品id
function redeem(product) {
  if (!canRedeem(product)) return
  // 跳轉到兌換確認頁，帶商品 id
  router.push({
    path: `/points/redeem/${product.productId}`
  })
}
</script>


<template>
  <div class="point-list-page">

    <!-- ── 頁面標題 ── -->
    <div class="d-flex align-products-center justify-content-between mb-4">
      <div>
        <h1 class="point-list-page__title mb-1">點數商品列表</h1>
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
    <div class="d-flex align-products-center gap-2 mb-4 flex-wrap">
      <button v-for="cat in categories" :key="cat.value" class="btn btn-sm"
        :class="activeCategory === cat.value ? 'btn-primary' : 'btn-outline-secondary'"
        @click="activeCategory = cat.value">
        {{ cat.label }}
      </button>
      <div class="ms-auto d-flex align-products-center gap-2">
        <select class="form-select form-select-sm" style="width: auto;" v-model="sortBy">
          <option value="default">預設排序</option>
          <option value="points-asc">點數：低到高</option>
          <option value="points-desc">點數：高到低</option>
        </select>
      </div>
    </div>

    <!-- ── 可兌換提示 ── -->
    <div class="alert alert-primary d-flex align-products-center gap-2 mb-4" v-if="redeemableCount > 0">
      <strong>✓ 共 {{ redeemableCount }} 件商品</strong>可用您目前的點數兌換
    </div>

    <!-- ── 商品列表 ── -->
    <div class="row g-3">
      <div class="col-md-6 col-lg-4" v-for="product in filteredproducts" :key="product.productId">
        <div class="card h-100 point-card" :class="{ 'point-card--locked': !canRedeem(product) }">

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
            <div class="d-flex align-products-center justify-content-between mb-3">
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
            <button class="btn btn-sm btn-full" :class="canRedeem(product) ? 'btn-primary' : 'btn-outline-secondary'"
              :disabled="!canRedeem(product)" @click="redeem(product)">
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
      <!-- <div style="font-size: 48px; margin-bottom: var(--space-4);">🎁</div>
      <p class="text-secondary">此分類目前沒有商品</p> -->
      <EmptyState icon="inbox" message="此分類目前沒有商品" subMessage="請點擊其他分類查看" />
    </div>

    <!-- ── 兌換成功 Toast ── -->
    <div class="point-list-page__toast alert alert-success" v-if="toastMessage">
      ✓ {{ toastMessage }}
    </div>

  </div>
</template>


<style scoped>
.point-list-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: var(--space-8);
  position: relative;
}

/* 頁面標題 */
.point-list-page__title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  border-left: none;
  padding-left: 0;
}

/* 點數餘額區塊 */
.point-list-page__balance {
  display: flex;
  align-products: baseline;
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
  border-width: 0.5px;
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg);
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
  align-products: center;
  justify-content: center;
  height: 100px;
  border-bottom: 0.5px solid var(--color-border);
}

.point-card__icon {
  font-size: 40px;
  line-height: 1;
}

/* 點數顯示 */
.point-card__cost {
  display: flex;
  align-products: baseline;
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
