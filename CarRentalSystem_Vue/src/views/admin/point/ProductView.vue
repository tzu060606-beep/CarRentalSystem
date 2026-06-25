<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import { productAPI } from '@/api/point/point_product'
import ProductFormModal from '@/components/admin/point/ProductFormModal.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import AlertToast from '@/components/common/AlertToast.vue'

const products = ref([])
const keyword = ref('')
const isActiveFilter = ref('')
const sortFilter = ref('')
const categoryFilter = ref('')
const showModal = ref(false);
const mode = ref('insert');
const selectedProduct = ref(null);
const toast = ref(null);

// 查詢全部
const fetchProducts = async () => {
  const response = await productAPI.getAll()
  products.value = response.data.data
}


// // 關鍵字搜尋
// const handleSearch = async () => {
//   if (keyword.value === '') {
//     await fetchProducts()
//   } else {
//     const response = await getProductByKeyword(keyword.value)
//     products.value = response.data
//   }
// }

// // 庫存+上架狀態篩選、點數排序
// const handleFilter = async () => {
//   // 不需要判斷是否為空再決定要不要呼叫，直接呼叫 getProductByFilters 就好，因為 Service 已經處理了空值的情況
//   const response = await getProductByFilters(isActiveFilter.value, categoryFilter.value, sortFilter.value)
//   products.value = response.data

// }

// 庫存+上架狀態+類別+關鍵字篩選、點數排序
const filteredProducts = computed(() => {
  return products.value
    // status 篩選
    .filter(p => {
      // filter 的規則是：回傳 true 代表這個元素通過篩選，回傳 false 代表濾掉。
      // 如果篩選條件是空字串，代表「不篩選」，所有商品都應該通過，所以 return true。
      if (isActiveFilter.value === '') return true
      // 要回傳條件判斷式，結果是 true 或 false
      if (isActiveFilter.value === 'active') return p.isActive //上架中包含缺貨補貨中
      if (isActiveFilter.value === 'outofstock') return p.isActive && p.stockQuantity === 0
      if (isActiveFilter.value === 'inactive') return !p.isActive
      return true
    })
    // category 篩選
    .filter(p => {
      if (categoryFilter.value === '') return true
      return p.category === categoryFilter.value
    })
    // 關鍵字搜尋
    .filter(p => {
      if (keyword.value === '') return true
      return p.productName.includes(keyword.value) || p.description?.includes(keyword.value)
    })
    // 排序
    //sort 的邏輯是：每次拿兩個元素比較，回傳負數就 a 排前面，回傳正數就 b 排前面，回傳 0 就不換位置。
    .sort((a, b) => {
      if (sortFilter.value === 'asc') return a.pointsRequired - b.pointsRequired // 低到高
      if (sortFilter.value === 'desc') return b.pointsRequired - a.pointsRequired // 高到低
      return 0 //不排序
    })
})


// 重設所有篩選條件
const resetFilters = () => {
  isActiveFilter.value = ''
  categoryFilter.value = ''
  sortFilter.value = ''
  keyword.value = ''
}


//根據不同狀態改變標籤樣式
//在 <script setup > 加上 getStatusLabel 函式
const getStatusLabel = (product) => {
  if (product.isActive && product.stockQuantity > 0) { return '上架中' }
  if (product.isActive && product.stockQuantity === 0) { return '缺貨補貨中' }
  return '已下架'
}
//再寫一個 getStatusBadgeClass 函式回傳對應的 badge class
const getStatusBadgeClass = (product) => {
  if (getStatusLabel(product) === '上架中') { return 'badge bg-success' }
  if (getStatusLabel(product) === '缺貨補貨中') { return 'badge bg-warning' }
  if (getStatusLabel(product) === '已下架') { return 'badge bg-gray' }
}




//新增商品
//TRYCATCH如果從後端收到錯誤訊息就在前端印出訊息
const handleInsert = async (formData) => {
  try {
    // 呼叫 insertProduct API
    await productAPI.insert(formData);//等新增完成
    // 成功後關閉彈窗
    showModal.value = false;
    //提示新增成功
    toast.value.show('新增成功！', 'success')
    // 重新載入商品列表
    await fetchProducts();
  } catch (error) {
    const message = error.response?.data?.message || '新增失敗，請稍後再試'
    toast.value.show(message, 'danger')
  }
}

//刪除確認用
const showDeleteConfirm = ref(false)
const pendingDeleteId = ref(null)

// 刪除商品
const handleDelete = (id) => {
  // 刪除：先暫存 id，顯示 ConfirmDialog
  // if (confirm('確定刪除?')) {
  pendingDeleteId.value = id
  showDeleteConfirm.value = true
  // }
}


// 使用者確認後才真正刪除
//驗證上架中的商品無法刪除，接到後端ApiResponse把錯誤訊息印出
const confirmDelete = async () => {
  try {
    await productAPI.deleteById(pendingDeleteId.value)
    showDeleteConfirm.value = false
    toast.value.show('刪除成功！', 'warning')
    await fetchProducts()
  } catch (error) {
    showDeleteConfirm.value = false
    // 從後端回傳的 ApiResponse 取出 message
    const message = error.response?.data?.message || '刪除失敗，請稍後再試'
    toast.value.show(message, 'danger')
  }
}


//修改確認用
const showUpdateConfirm = ref(false)  // 控制修改確認 ConfirmDialog
const pendingFormData = ref(null)     // 暫存使用者送出的表單資料，等確認後才送 API


//修改商品：先暫存資料，等使用者在 ConfirmDialog 按確定才送 API
const handleUpdate = (formData) => {
  // if (confirm('確定修改?送出後將無法還原')) {
  pendingFormData.value = formData  // 暫存表單資料
  showUpdateConfirm.value = true    // 顯示確認對話框
  // }
}


// 使用者在 ConfirmDialog 按確定後才真正送 API
const confirmUpdate = async () => {
  try {
    await productAPI.updateById(selectedProduct.value.productId, pendingFormData.value)
    showUpdateConfirm.value = false
    showModal.value = false
    toast.value.show('修改成功！', 'success')
    await fetchProducts()
  } catch (error) {
    const message = error.response?.data?.message || '修改失敗，請稍後再試'
    toast.value.show(message, 'danger')
  }
}



// 跳轉新增頁
const goToInsert = () => {
  // 之後串接路由
  mode.value = 'insert'
  selectedProduct.value = null
  showModal.value = true
}

// 跳轉修改頁
const goToUpdate = (id) => {
  // { ...spread } 展開成新物件，讓 watch 偵測到「有變化」
  selectedProduct.value = { ...products.value.find(p => p.productId === id) }
  mode.value = 'update'
  showModal.value = true
}

// 跳轉查詢頁
const goToView = async (id) => {
  const response = await productAPI.getById(id)
  selectedProduct.value = response.data.data
  mode.value = 'view'
  showModal.value = true;
}

//決定收到資料後要做什麼，是呼叫新增 API 還是修改 API。
const handleSubmit = (formData) => {
  if (mode.value === 'insert') {
    handleInsert(formData)
  } else {
    handleUpdate(formData)
  }
}

//==================顯示模式控制====================

const viewMode = ref('table')  // 'table' | 'card'，控制目前顯示模式


// 記錄目前哪張卡片的選單是開啟的（null 代表全部關閉）
const openMenuId = ref(null)

//================三點按鈕控制=======================

// 滾動時關閉選單
const handleScroll = () => { openMenuId.value = null }


//================兌換次數===============
import { redemptionOrderAPI } from '@/api/point/point_order'

const orders = ref([])

const fetchOrders = async () => {
  const response = await redemptionOrderAPI.getAll()
  orders.value = response.data.data
}

// 計算某商品被兌換幾次
const getRedeemCount = (productId) => {
  return orders.value
    // filter 找出這個商品的所有訂單，reduce 把每筆的 productQuantity 加總。
    .filter(o => o.product?.productId === productId)
    .reduce((total, o) => total + o.productQuantity, 0)
}

//===============庫存警示==================

// 庫存低於此門檻顯示警示
const STOCK_WARNING = 10

const isLowStock = (product) => {
  return product.isActive && product.stockQuantity > 0 && product.stockQuantity <= STOCK_WARNING
}


onMounted(() => {
  fetchProducts()
  // 監聽滾動事件，滾動時關閉三點選單
  window.addEventListener('scroll', handleScroll, true)

  //取得兌換次數
  fetchOrders()
})

// 元件卸載時移除監聽，避免記憶體洩漏
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll, true)
})
</script>



<template>

  <div>

    <!-- 返回INDEX麵包屑 -->
    <div class="mb-3">
      <Breadcrumb :items="[
        { label: '首頁', to: '/' },
        { label: '點數管理', to: '/point' },
        { label: '商品管理' }
      ]" />
    </div>


    <!-- 標題+新增按鈕 -->
    <!-- 標題 -->
    <div class="d-flex justify-content-between align-items-center mb-1">
      <h3 class="mb-0 fw-bold">商品管理</h3>
      <div class="ms-auto ">

        <!-- 新增按鈕 -->
        <button @click="goToInsert" class="btn btn-primary">＋ 新增商品</button>
      </div>
    </div>


    <!-- 操作列 -->
    <div class="d-flex justify-content-between align-items-center mb-2 mt-2 ">

      <!-- 左邊類別按鈕 -->
      <div class="d-flex gap-2 p-2">
        <button class="" @click="categoryFilter = ''"
          :class="categoryFilter === '' ? 'btn btn-primary' : 'btn btn-outline-primary'">
          全部
        </button>
        <button @click="categoryFilter = '汽車配件'"
          :class="categoryFilter === '汽車配件' ? 'btn btn-primary' : 'btn btn-outline-primary'">
          汽車配件
        </button>
        <button @click="categoryFilter = '加值服務'"
          :class="categoryFilter === '加值服務' ? 'btn btn-primary' : 'btn btn-outline-primary'">
          加值服務
        </button>
        <button @click="categoryFilter = '禮品兌換'"
          :class="categoryFilter === '禮品兌換' ? 'btn btn-primary' : 'btn btn-outline-primary'">
          禮品兌換
        </button>
        <button @click="categoryFilter = '合作品牌'"
          :class="categoryFilter === '合作品牌' ? 'btn btn-primary' : 'btn btn-outline-primary'">
          合作品牌
        </button>
      </div>

      <!-- 右邊篩選排序 -->
      <div class="p-2 d-flex gap-2">
        <select class="form-select w-auto" aria-label="Default select example" v-model="isActiveFilter">
          <option value="">所有上架狀態</option>
          <option value="active">上架中</option>
          <option value="inactive">已下架</option>
          <option value="outofstock">缺貨補貨中</option>
        </select>
        <input v-model="keyword" class="form-control" type="text" placeholder="請輸入商品名稱關鍵字..." />
        <!-- 右邊：排序下拉選單 -->
        <select class="form-select w-auto" v-model="sortFilter">
          <option value="">預設排序</option>
          <option value="asc">點數：低到高</option>
          <option value="desc">點數：高到低</option>
        </select>

        <!-- 重設篩選按鈕:只在有設篩選條件時出現-->
        <button v-if="isActiveFilter || categoryFilter || sortFilter || keyword" @click="resetFilters"
          class="btn btn-outline-secondary text-nowrap">
          重設篩選
        </button>


        <!-- 搜尋按鈕刪除，關鍵字改成即時篩選，輸入時 computed 自動更新 -->
        <!-- <button @click="handleSearch" class="btn btn-primary text-nowrap">搜尋</button> -->
        <!-- text-nowrap：應用在 button 上，防止在視窗縮小時文字被擠壓換行。 -->
      </div>


      <!-- 顯示模式切換 -->
      <div class="btn-group ms-2">
        <button @click="viewMode = 'table'"
          :class="viewMode === 'table' ? 'btn btn-primary btn-sm' : 'btn btn-outline-primary btn-sm'">
          <i class="fa-solid fa-table-list"></i>
        </button>
        <button @click="viewMode = 'card'"
          :class="viewMode === 'card' ? 'btn btn-primary btn-sm' : 'btn btn-outline-primary btn-sm'">
          <i class="fa-solid fa-grip"></i>
        </button>
      </div>

    </div>

    <div class="d-flex justify-content-between align-items-center gap-2 m-2">

      <!-- 所有資料、篩選資料顯示筆數 -->
      <span class="ms-auto me-3 text-primary">
        共 {{ products.length }} 筆 , 搜尋結果: {{ filteredProducts.length }} 筆
      </span>

    </div>


    <!-- 商品表格 -->
    <!-- 表格模式 -->
    <div v-if="viewMode === 'table'" class="bg-white border rounded-4 shadow-sm overflow-hidden">
      <table class="table table-hover align-middle text-nowrap mb-0">
        <thead>
          <tr>
            <th>商品編號</th>
            <th>圖片</th>
            <th>商品名稱</th>
            <th>所需點數</th>
            <th>庫存</th>
            <th>商品描述</th>
            <th>上架狀態</th>
            <th>商品類別</th>
            <th>已兌換</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>

          <!-- 下拉選單和搜尋框改成即時觸發（不需要按按鈕） -->
          <tr v-for="product in filteredProducts" :key="product.productId">
            <td><code>{{ product.productId }}</code></td>
            <td>
              <img v-if="product.imageUrl" :src="product.imageUrl" style="width: 80px;height: 80px; object-fit: cover;">
              <span v-else>無圖片</span>
            </td>
            <td @click="goToView(product.productId)" class="fw-bold fs-6 text-primary">
              {{ product.productName }}
            </td>
            <td class="fw-bold">{{ product.pointsRequired }} 點</td>
            <td :style="isLowStock(product) ? 'color: var(--color-danger); font-weight: bold;' : ''">
              {{ product.stockQuantity }} 件
            </td>
            <td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
              {{ product.description }}
            </td>
            <td>
              <span :class="getStatusBadgeClass(product)">
                {{ getStatusLabel(product) }}
              </span>
            </td>
            <td>{{ product.category }}</td>
            <td>{{ getRedeemCount(product.productId) }} 次</td>
            <td>
              <!-- 表格版三點選單 - openMenuId 跟卡片共用同一個 ref，確保兩種模式的選單不會同時開啟-->
              <div class="position-relative d-inline-block" @click.stop>
                <button class="btn btn-sm btn-light rounded-circle" style="width: 28px; height: 28px; padding: 0;"
                  @click="openMenuId = openMenuId === product.productId ? null : product.productId">
                  <i class="fa-solid fa-ellipsis-vertical"></i>
                </button>
                <ul v-if="openMenuId === product.productId" class="dropdown-menu show shadow-sm"
                  style="position: absolute; right: 0; top: 100%; min-width: 100px; z-index: 10;">
                  <li>
                    <button class="dropdown-item" @click="goToUpdate(product.productId); openMenuId = null">
                      <i class="fa-solid fa-pen me-2"></i>修改
                    </button>
                  </li>
                  <li>
                    <button class="dropdown-item text-danger"
                      @click="handleDelete(product.productId); openMenuId = null">
                      <i class="fa-solid fa-trash me-2"></i>刪除
                    </button>
                  </li>
                </ul>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>


    <!-- 卡片模式 -->
    <div v-else class="row g-3 mt-1">
      <div v-for="product in filteredProducts" :key="product.productId" class="col-md-3">
        <!-- 
            整張卡片可點擊查看 → @click="goToView"
            position-relative → 讓內部的絕對定位元素（badge、三點按鈕）相對卡片定位
            product-card → 自訂 class，用於 hover 光暈效果
        -->
        <div class="card h-100 shadow-sm border point-product-card position-relative"
          @click="goToView(product.productId); openMenuId = null" style="cursor: pointer;">

          <!-- 左上角：狀態 badge -->
          <div class="position-absolute top-0 start-0 m-2" style="z-index: 1;">
            <span :class="getStatusBadgeClass(product)">
              {{ getStatusLabel(product) }}
            </span>
          </div>

          <!-- 右上角：三點選單 -->
          <div class="position-absolute top-0 end-0 m-2" style="z-index: 1;" @click.stop>
            <!-- 用 Vue ref 控制開關，不依賴 Bootstrap JS -->
            <button class="btn btn-sm  rounded-circle px-2" style="background-color: rgba(255, 255, 255, 0.7);"
              @click="openMenuId = openMenuId === product.productId ? null : product.productId">
              <i class="fa-solid fa-ellipsis-vertical"></i>
            </button>
            <!-- v-if 控制顯示，點擊外部關閉 -->
            <ul v-if="openMenuId === product.productId" class="dropdown-menu show shadow-sm"
              style="position: absolute; right: 0; top: 100%; min-width: 100px;">
              <li>
                <button class="dropdown-item" @click="goToUpdate(product.productId); openMenuId = null">
                  <i class="fa-solid fa-pen me-2"></i>修改
                </button>
              </li>
              <li>
                <button class="dropdown-item text-danger" @click="handleDelete(product.productId); openMenuId = null">
                  <i class="fa-solid fa-trash me-2"></i>刪除
                </button>
              </li>
            </ul>
          </div>


          <!-- 圖片：上方圓角修正用 overflow-hidden 包住 -->
          <div class="overflow-hidden rounded-top" style="height: 160px;">
            <img v-if="product.imageUrl" :src="product.imageUrl" class="w-100 h-100 object-fit-cover">
            <div v-else class="w-100 h-100 bg-light d-flex align-items-center justify-content-center">
              <span class="text-muted small">無圖片</span>
            </div>
          </div>

          <!-- 卡片內容 -->
          <div class="card-body p-3">
            <!-- 商品名稱 + 點數：同一行 -->
            <div class="d-flex justify-content-between align-items-start">
              <p class="fw-bold mb-0 text-primary" style="font-size: var(--text-base);">
                {{ product.productName }}
              </p>
              <span class="text-nowrap ms-2 fw-semibold" style="color: var(--color-accent); font-size: var(--text-sm);">
                <i class="fa-solid fa-star me-1" style="font-size: 0.75em;"></i>
                {{ product.pointsRequired }} 點
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>



    <!-- 彈窗，點擊新增修改查詢時出現 -->
    <ProductFormModal :isVisible="showModal" :mode="mode" @close="showModal = false" @submit="handleSubmit"
      :productData="selectedProduct">
    </ProductFormModal>
  </div>
  <!-- 確定修改confirm -->
  <ConfirmDialog :isVisible="showUpdateConfirm" title="確認修改" message="確定要送出修改嗎？送出後將無法還原。" confirmText="確定修改"
    confirmVariant="primary" @confirm="confirmUpdate" @cancel="showUpdateConfirm = false">
  </ConfirmDialog>
  <!-- 確定刪除confirm -->
  <ConfirmDialog :isVisible="showDeleteConfirm" title="確認刪除" message="確定要刪除嗎？刪除後將無法還原。" confirmText="確定刪除"
    confirmVariant="primary" @confirm="confirmDelete" @cancel="showDeleteConfirm = false">
  </ConfirmDialog>

  <AlertToast ref="toast">
  </AlertToast>

</template>


<style scoped>
/* 卡片 hover 光暈效果 */
.point-product-card {
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
}

.point-product-card:hover {
  box-shadow: var(--shadow-md) !important;
  transform: translateY(-2px);
}
</style>
