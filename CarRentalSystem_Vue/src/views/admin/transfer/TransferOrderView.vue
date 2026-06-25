<script setup>
import { ref, onMounted } from 'vue'
import {
  getAllTransferOrders,
  createTransfer,
  deleteTransfer
} from '@/api/transfer/TransferOrderApi'

// ========== 訂單列表 ==========
const orders = ref([])
const loading = ref(true)
const error = ref(null)

// ========== 新增表單 ==========
const showAddForm = ref(false)
const newOrder = ref({
  customerName: '',
  customerPhone: '',
  pickupLocation: '',
  dropoffLocation: '',
  passengerCount: 1,
  totalFee: 0
})

// ========== 取得所有訂單 ==========
const fetchOrders = async () => {
  try {
    loading.value = true
    error.value = null
    const response = await getAllTransferOrders()
    orders.value = response.data
  } catch (err) {
    error.value = '無法載入接送訂單資料，請確定後端伺服器(8081 port)已啟動。'
    console.error(err)
  } finally {
    loading.value = false
  }
}

// ========== 新增訂單 ==========
const handleCreate = async () => {
  try {
    await createTransfer({ ...newOrder.value })
    // 重置表單
    newOrder.value = {
      customerName: '',
      customerPhone: '',
      pickupLocation: '',
      dropoffLocation: '',
      passengerCount: 1,
      totalFee: 0
    }
    showAddForm.value = false
    // 重新載入列表
    await fetchOrders()
    alert('✅ 訂單新增成功！')
  } catch (err) {
    console.error(err)
    alert('❌ 新增訂單失敗，請稍後再試。')
  }
}

// ========== 刪除訂單 ==========
const handleDelete = async (id) => {
  if (!confirm(`確定要刪除訂單 #${id} 嗎？此操作無法復原。`)) return
  try {
    await deleteTransfer(id)
    await fetchOrders()
    alert('✅ 訂單已刪除！')
  } catch (err) {
    console.error(err)
    alert('❌ 刪除訂單失敗，請稍後再試。')
  }
}

// ========== 工具函式 ==========
const formatCurrency = (amount) => {
  if (amount === null || amount === undefined) return '未計算'
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    minimumFractionDigits: 0
  }).format(amount)
}

// ========== 初始載入 ==========
onMounted(() => {
  fetchOrders()
})
</script>

<template>
  <div class="transfer-container">
    <!-- ===== 標題區 ===== -->
    <div class="header-section">
      <h1 class="title">接送訂單系統</h1>
      <p class="subtitle">Transfer Orders</p>

      <div class="header-actions">
        <button class="btn-action btn-primary" @click="showAddForm = !showAddForm">
          {{ showAddForm ? '✖ 取消新增' : '➕ 新增訂單' }}
        </button>
        <button class="btn-action" @click="$router.push('/transferOrder/search')">🔍 查詢 | 修改列表</button>
        <button class="btn-action" @click="$router.push('/transferRate')">💰 費率管理</button>
      </div>
    </div>

    <!-- ===== 新增表單 ===== -->
    <Transition name="slide-fade">
      <div v-if="showAddForm" class="add-form-card">
        <h2 class="form-title">📝 新增接送訂單</h2>
        <form @submit.prevent="handleCreate" class="form-grid">
          <div class="form-group">
            <label for="customerName">顧客姓名</label>
            <input
              id="customerName"
              v-model="newOrder.customerName"
              type="text"
              placeholder="請輸入顧客姓名"
              required
            />
          </div>

          <div class="form-group">
            <label for="customerPhone">聯絡電話</label>
            <input
              id="customerPhone"
              v-model="newOrder.customerPhone"
              type="text"
              placeholder="請輸入聯絡電話"
              required
            />
          </div>

          <div class="form-group">
            <label for="pickupLocation">上車地點</label>
            <input
              id="pickupLocation"
              v-model="newOrder.pickupLocation"
              type="text"
              placeholder="請輸入上車地點"
              required
            />
          </div>

          <div class="form-group">
            <label for="dropoffLocation">下車地點</label>
            <input
              id="dropoffLocation"
              v-model="newOrder.dropoffLocation"
              type="text"
              placeholder="請輸入下車地點"
              required
            />
          </div>

          <div class="form-group">
            <label for="passengerCount">乘客人數</label>
            <input
              id="passengerCount"
              v-model.number="newOrder.passengerCount"
              type="number"
              min="1"
              max="20"
              required
            />
          </div>

          <div class="form-group">
            <label for="totalFee">總費用</label>
            <input
              id="totalFee"
              v-model.number="newOrder.totalFee"
              type="number"
              min="0"
              placeholder="請輸入總費用"
              required
            />
          </div>

          <div class="form-actions">
            <button type="submit" class="btn-submit">✅ 確認新增</button>
            <button type="button" class="btn-cancel" @click="showAddForm = false">取消</button>
          </div>
        </form>
      </div>
    </Transition>

    <!-- ===== Loading 狀態 ===== -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>資料載入中 Loading...</p>
    </div>

    <!-- ===== 錯誤狀態 ===== -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">⚠️</div>
      <p>{{ error }}</p>
      <button @click="fetchOrders" class="btn-retry">重新整理 Retry</button>
    </div>

    <!-- ===== 空資料狀態 ===== -->
    <div v-else-if="orders.length === 0" class="empty-state">
      <div class="empty-icon">📭</div>
      <p>目前沒有任何接送訂單 No orders found.</p>
    </div>

    <!-- ===== 訂單卡片列表 ===== -->
    <div v-else class="cards-grid">
      <div v-for="order in orders" :key="order.transferId" class="order-card">
        <div class="card-header">
          <span class="order-id">#{{ order.transferId }}</span>
          <span :class="['status-badge', order.status ? order.status.toLowerCase() : 'pending']">
            {{ order.status || '處理中' }}
          </span>
        </div>

        <div class="card-body">
          <!-- 顧客姓名 -->
          <div class="info-row">
            <span class="icon">👤</span>
            <div class="info-content">
              <span class="label">顧客姓名 (Customer)</span>
              <span class="value">{{ order.customerName || '未提供' }}</span>
            </div>
          </div>

          <!-- 聯絡電話 -->
          <div class="info-row">
            <span class="icon">📞</span>
            <div class="info-content">
              <span class="label">聯絡電話 (Phone)</span>
              <span class="value">{{ order.customerPhone || '未提供' }}</span>
            </div>
          </div>

          <!-- 接送地點 -->
          <div class="info-row">
            <span class="icon">📍</span>
            <div class="info-content">
              <span class="label">接送地點 (Route)</span>
              <span class="value">{{ order.pickupLocation || '未提供' }} ➔ {{ order.dropoffLocation || '未提供' }}</span>
            </div>
          </div>

          <!-- 乘客人數 -->
          <div class="info-row">
            <span class="icon">🧑‍🤝‍🧑</span>
            <div class="info-content">
              <span class="label">乘客人數 (Passengers)</span>
              <span class="value">{{ order.passengerCount ?? '未提供' }} 人</span>
            </div>
          </div>

          <!-- 總費用 -->
          <div class="info-row amount-row">
            <span class="icon">💰</span>
            <div class="info-content">
              <span class="label">總費用 (Total Fee)</span>
              <span class="value highlight-amount">{{ formatCurrency(order.totalFee) }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <button class="btn-action" @click="$router.push(`/transferOrder/edit/${order.transferId}`)">
            ✏️ 查看 / 修改
          </button>
          <button class="btn-action btn-danger" @click="handleDelete(order.transferId)">
            🗑️ 刪除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 容器 ===== */
.transfer-container {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 80vh;
  font-family: 'Inter', 'Roboto', sans-serif;
  color: var(--color-text);
}

/* ===== 標題區 ===== */
.header-section {
  text-align: center;
  margin-bottom: 3rem;
}

.title {
  font-size: 2.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 0.5rem;
}

.subtitle {
  font-size: 1.1rem;
  color: #888;
  letter-spacing: 1px;
}

.header-actions {
  margin-top: 1.5rem;
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

/* ===== 按鈕 ===== */
.btn-action,
.btn-retry {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  border: none;
  padding: 0.6rem 1.2rem;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.9rem;
}

.btn-action:hover,
.btn-retry:hover {
  background: #667eea;
  color: white;
  transform: translateY(-1px);
}

.btn-primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #5a6fd6, #6a4192);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.btn-danger {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.btn-danger:hover {
  background: #ef4444;
  color: white;
}

/* ===== 新增表單 ===== */
.add-form-card {
  background: var(--color-background-soft, #ffffff);
  border-radius: 16px;
  padding: 2rem;
  margin-bottom: 2.5rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--color-border, #eee);
}

@media (prefers-color-scheme: dark) {
  .add-form-card {
    background: #1e1e1e;
    border-color: #333;
  }
}

.form-title {
  font-size: 1.4rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  color: var(--color-heading, #222);
}

@media (prefers-color-scheme: dark) {
  .form-title {
    color: #eee;
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.2rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-group label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #888;
}

.form-group input {
  padding: 0.7rem 1rem;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 8px;
  font-size: 1rem;
  background: var(--color-background, #fff);
  color: var(--color-text, #333);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

@media (prefers-color-scheme: dark) {
  .form-group input {
    background: #2a2a2a;
    border-color: #444;
    color: #eee;
  }
}

.form-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 0.5rem;
}

.btn-submit {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  border: none;
  padding: 0.7rem 1.8rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-submit:hover {
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
  transform: translateY(-1px);
}

.btn-cancel {
  background: rgba(120, 120, 120, 0.1);
  color: #888;
  border: none;
  padding: 0.7rem 1.8rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel:hover {
  background: rgba(120, 120, 120, 0.2);
}

/* ===== 表單動畫 ===== */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(-15px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ===== Loading / Error / Empty 狀態 ===== */
.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: rgba(120, 120, 120, 0.05);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(120, 120, 120, 0.1);
}

.spinner {
  width: 50px;
  height: 50px;
  border: 5px solid rgba(102, 126, 234, 0.2);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

.error-icon,
.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

/* ===== 卡片列表 ===== */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 2rem;
}

.order-card {
  background: var(--color-background-soft, #ffffff);
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--color-border, #eee);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

@media (prefers-color-scheme: dark) {
  .order-card {
    background: #1e1e1e;
    border-color: #333;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }
}

.order-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.order-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 35px rgba(102, 126, 234, 0.15);
}

.order-card:hover::before {
  opacity: 1;
}

/* ===== 卡片 Header ===== */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border, #eee);
}

@media (prefers-color-scheme: dark) {
  .card-header {
    border-color: #333;
  }
}

.order-id {
  font-weight: 700;
  font-size: 1.2rem;
  color: var(--color-heading, #222);
}

@media (prefers-color-scheme: dark) {
  .order-id {
    color: #eee;
  }
}

.status-badge {
  padding: 0.3rem 0.8rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.status-badge.completed {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.status-badge.cancelled {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

/* ===== 卡片 Body ===== */
.card-body {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.icon {
  font-size: 1.2rem;
  margin-top: 0.1rem;
}

.info-content {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.label {
  font-size: 0.8rem;
  color: #888;
  margin-bottom: 0.2rem;
}

.value {
  font-size: 1rem;
  font-weight: 500;
  line-height: 1.4;
  word-break: break-all;
}

.amount-row {
  margin-top: 0.5rem;
  padding-top: 1rem;
  border-top: 1px dashed var(--color-border, #eee);
}

@media (prefers-color-scheme: dark) {
  .amount-row {
    border-color: #444;
  }
}

.highlight-amount {
  font-size: 1.2rem;
  color: #10b981;
  font-weight: 700;
}

/* ===== 卡片 Footer ===== */
.card-footer {
  margin-top: 1.5rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.8rem;
}
</style>
