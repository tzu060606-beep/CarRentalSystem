<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import request from '@/api/index'

const emit = defineEmits(['navigate'])

// 會員資訊載入
const custId = ref(null)

// 1. 人數與行李數
const selectedPassengerCount = ref(1)
const selectedLuggageCount = ref(0)

// 2. 車款選擇
const carModels = ref([])
const selectedModel = ref(null)
const showAllModels = ref(false)

// 模擬預設車款以防後端資料庫為空
const fallbackModels = [
  { modelId: 991, brand: 'Lexus', modelName: '豪華轎車', seats: 4, luggageCapacity: 2, vehicleImgUrl: '' },
  { modelId: 992, brand: 'Mercedes-Benz', modelName: '高級進口車', seats: 4, luggageCapacity: 3, vehicleImgUrl: '' },
  { modelId: 993, brand: 'Toyota', modelName: '豪華廂型車', seats: 8, luggageCapacity: 6, vehicleImgUrl: '' },
  { modelId: 994, brand: 'Volkswagen', modelName: '高級商旅座車', seats: 4, luggageCapacity: 4, vehicleImgUrl: '' }
]

// 顯示的車款列表 (預設前4個，展開後全部)
const displayModels = computed(() => {
  const list = carModels.value.length > 0 ? carModels.value : fallbackModels
  return showAllModels.value ? list : list.slice(0, 4)
})

// 根據選定車款計算乘客人數選項
const passengerOptions = computed(() => {
  const max = selectedModel.value ? selectedModel.value.seats : 8
  const options = []
  for (let i = 1; i <= max; i++) {
    options.push(i)
  }
  return options
})

// 根據選定車款計算行李數選項
const luggageOptions = computed(() => {
  const max = selectedModel.value ? (selectedModel.value.luggageCapacity || 4) : 8
  const options = []
  for (let i = 0; i <= max; i++) {
    options.push(i)
  }
  return options
})

// 當車款改變時，重置或限制人數/行李數
watch(selectedModel, (newVal) => {
  if (newVal) {
    if (selectedPassengerCount.value > newVal.seats) {
      selectedPassengerCount.value = newVal.seats
    }
    const maxLuggage = newVal.luggageCapacity || 4
    if (selectedLuggageCount.value > maxLuggage) {
      selectedLuggageCount.value = maxLuggage
    }
  }
})

// 3. 行程資訊
const pickupDate = ref('')
const pickupTime = ref('')
const dropoffDate = ref('')
const dropoffTime = ref('')
const pickupLocation = ref('')
const dropoffLocation = ref('')
const midpoints = ref([]) // 中途停靠點陣列
const note = ref('')

// 新增中途停靠點
const addMidpoint = () => {
  midpoints.value.push({ value: '' })
}

// 移除中途停靠點
const removeMidpoint = (idx) => {
  midpoints.value.splice(idx, 1)
}

// 4. 聯絡資訊
const bookerName = ref('')
const bookerEmail = ref('')
const bookerPhoneCode = ref('+886')
const bookerPhone = ref('')
const backupPhones = ref([]) // 備用手機號碼

const addBackupPhone = () => {
  backupPhones.value.push({ code: '+886', number: '' })
}
const removeBackupPhone = (idx) => {
  backupPhones.value.splice(idx, 1)
}

// 聯絡人同預約人
const sameAsBooker = ref(false)
const contactName = ref('')
const contactPhoneCode = ref('+886')
const contactPhone = ref('')
const rentalPurpose = ref('商務接送')

// 當聯絡人同預約人被打勾時，自動同步資料
watch([sameAsBooker, bookerName, bookerPhoneCode, bookerPhone], () => {
  if (sameAsBooker.value) {
    contactName.value = bookerName.value
    contactPhoneCode.value = bookerPhoneCode.value
    contactPhone.value = bookerPhone.value
  }
})

// 錯誤訊息收集
const errors = ref({})

// 初始化載入
const fetchCarModels = async () => {
  try {
    const response = await request.get('/carmodel')
    if (response.data && response.data.length > 0) {
      carModels.value = response.data
    }
  } catch (error) {
    console.error('取得車款資料失敗，將使用預設車款:', error)
  }
}

const loadMemberInfo = () => {
  const customerStr = localStorage.getItem('customer')
  if (customerStr) {
    try {
      const customer = JSON.parse(customerStr)
      custId.value = customer.custId || null
      bookerName.value = customer.custName || ''
      bookerEmail.value = customer.custEmail || customer.email || ''
      bookerPhone.value = customer.custPhone || customer.phone || ''
    } catch (e) {
      console.error('解析會員資料失敗:', e)
    }
  }
}

onMounted(() => {
  fetchCarModels()
  loadMemberInfo()
})

// 欄位防呆驗證
const validateForm = () => {
  errors.value = {}
  
  if (!selectedModel.value) {
    errors.value.model = '請選擇車型'
  }
  if (!pickupDate.value || !pickupTime.value) {
    errors.value.pickupTime = '請輸入完整的行車開始時間'
  }
  if (!dropoffDate.value || !dropoffTime.value) {
    errors.value.dropoffTime = '請輸入完整的行車結束時間'
  }
  if (!pickupLocation.value.trim()) {
    errors.value.pickupLocation = '請輸入出發地點'
  }
  if (!dropoffLocation.value.trim()) {
    errors.value.dropoffLocation = '請輸入到達地點'
  }
  
  // 驗證開始時間不能晚於結束時間
  if (pickupDate.value && pickupTime.value && dropoffDate.value && dropoffTime.value) {
    const start = new Date(`${pickupDate.value}T${pickupTime.value}`)
    const end = new Date(`${dropoffDate.value}T${dropoffTime.value}`)
    if (start >= end) {
      errors.value.timeOrder = '開始時間必須早於結束時間'
    }
  }

  // 聯絡資訊驗證
  if (!bookerName.value.trim()) {
    errors.value.bookerName = '請輸入預約人姓名'
  }
  if (!bookerEmail.value.trim()) {
    errors.value.bookerEmail = '請輸入預約人電子信箱'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(bookerEmail.value)) {
    errors.value.bookerEmail = '電子信箱格式不正確'
  }
  if (!bookerPhone.value.trim()) {
    errors.value.bookerPhone = '請輸入預約人手機號碼'
  }

  if (!contactName.value.trim()) {
    errors.value.contactName = '請輸入聯絡人姓名'
  }
  if (!contactPhone.value.trim()) {
    errors.value.contactPhone = '請輸入聯絡人手機號碼'
  }

  return Object.keys(errors.value).length === 0
}

// 清除重填
const resetForm = () => {
  selectedPassengerCount.value = 1
  selectedLuggageCount.value = 0
  selectedModel.value = null
  pickupDate.value = ''
  pickupTime.value = ''
  dropoffDate.value = ''
  dropoffTime.value = ''
  pickupLocation.value = ''
  dropoffLocation.value = ''
  midpoints.value = []
  note.value = ''
  sameAsBooker.value = false
  contactName.value = ''
  contactPhone.value = ''
  backupPhones.value = []
  rentalPurpose.value = '商務接送'
  errors.value = {}
  loadMemberInfo()
}

// 送出需求單
const submitForm = async () => {
  if (!validateForm()) {
    const firstError = Object.values(errors.value)[0]
    alert(`請修正欄位錯誤: ${firstError}`)
    return
  }

  try {
    // 整合所有詳細的客製化欄位至 note 中
    const formattedMidpoints = midpoints.value
      .map(m => m.value.trim())
      .filter(v => v !== '')
      .join(' -> ')

    const formattedBackupPhones = backupPhones.value
      .map(bp => `${bp.code}${bp.number.trim()}`)
      .filter(num => num.length > 5)
      .join(', ')

    // 拼裝 note 文字
    let fullNote = `【專車接送需求單】\n`
    fullNote += `指定車款: ${selectedModel.value.brand} ${selectedModel.value.modelName} (${selectedModel.value.seats}人座)\n`
    fullNote += `租車用途: ${rentalPurpose.value}\n`
    if (formattedMidpoints) {
      fullNote += `中途停靠點: ${formattedMidpoints}\n`
    }
    fullNote += `預約人: ${bookerName.value} (${bookerEmail.value} / ${bookerPhoneCode.value}${bookerPhone.value})\n`
    if (formattedBackupPhones) {
      fullNote += `預約人備用手機: ${formattedBackupPhones}\n`
    }
    fullNote += `聯絡人: ${contactName.value} (${contactPhoneCode.value}${contactPhone.value})\n`
    if (note.value.trim()) {
      fullNote += `備註需求: ${note.value.trim()}\n`
    }

    const startDateTime = `${pickupDate.value}T${pickupTime.value}:00`
    const endDateTime = `${dropoffDate.value}T${dropoffTime.value}:00`

    // 組成 TransferOrder 送往後端
    const orderData = {
      custId: custId.value,
      custPhone: `${bookerPhoneCode.value}${bookerPhone.value}`,
      transferType: '包車接送', // 標註為包車客製需求
      pickupLocation: pickupLocation.value.trim(),
      dropoffLocation: dropoffLocation.value.trim(),
      scheduledPickupTime: startDateTime,
      scheduledDropoffTime: endDateTime,
      passengerCount: selectedPassengerCount.value,
      luggageCount: selectedLuggageCount.value,
      status: '待處理',
      note: fullNote,
      totalAmount: 0 // 待後端核算報價
    }

    await request.post('/transferOrder', orderData)
    alert('需求單已成功送出！客服人員將於 3 個工作天內與您聯繫報價。')
    emit('navigate', 'landing')
  } catch (error) {
    console.error('送出需求單失敗:', error)
    alert('需求單送出失敗，請稍後再試。')
  }
}
</script>

<template>
  <div class="container py-4">
    <!-- 麵包屑 -->
    <nav aria-label="breadcrumb" class="mb-4">
      <ol class="breadcrumb">
        <li class="breadcrumb-item"><a href="#" @click.prevent="emit('navigate', 'landing')" class="text-decoration-none">首頁</a></li>
        <li class="breadcrumb-item"><a href="#" @click.prevent="emit('navigate', 'landing')" class="text-decoration-none">專車接送</a></li>
        <li class="breadcrumb-item active" aria-current="page">專車接送需求單</li>
      </ol>
    </nav>

    <!-- 標題與說明 -->
    <div class="text-center mb-5">
      <h2 class="fw-bold text-dark mb-3">專車接送需求單</h2>
      <p class="text-muted">若您有特別的租車相關需求，請填寫表單並詳細描述內容，我們會盡快與您連絡。</p>
    </div>

    <!-- 表單容器 -->
    <div class="row justify-content-center">
      <div class="col-lg-10">
        
        <!-- 卡片一：人數／行李 -->
        <div class="card shadow-sm border-0 rounded-4 p-4 mb-4">
          <h5 class="fw-bold mb-4 border-start border-primary border-4 ps-2 text-primary">人數／行李</h5>
          <div class="row g-3">
            <!-- 人數 -->
            <div class="col-md-6">
              <div class="form-floating mb-3">
                <select class="form-select" id="passengerCountSelect" v-model="selectedPassengerCount">
                  <option v-for="opt in passengerOptions" :key="opt" :value="opt">{{ opt }} 人</option>
                </select>
                <label for="passengerCountSelect"><i class="fa-solid fa-user me-2 text-muted"></i>人數</label>
              </div>
            </div>
            <!-- 行李數 -->
            <div class="col-md-6">
              <div class="form-floating mb-3">
                <select class="form-select" id="luggageCountSelect" v-model="selectedLuggageCount">
                  <option v-for="opt in luggageOptions" :key="opt" :value="opt">{{ opt }} 件</option>
                </select>
                <label for="luggageCountSelect"><i class="fa-solid fa-suitcase me-2 text-muted"></i>行李數 (選填)</label>
              </div>
            </div>
          </div>
        </div>

        <!-- 卡片二：車型 -->
        <div class="card shadow-sm border-0 rounded-4 p-4 mb-4">
          <h5 class="fw-bold mb-3 border-start border-primary border-4 ps-2 text-primary">車型</h5>
          <div class="row g-3 mb-3">
            <div v-for="model in displayModels" :key="model.modelId" class="col-md-6">
              <div 
                class="car-card border rounded-3 p-3 d-flex align-items-center justify-content-between cursor-pointer transition-all"
                :class="{ 'border-primary bg-primary bg-opacity-10 active-card': selectedModel && selectedModel.modelId === model.modelId }"
                @click="selectedModel = model"
              >
                <div class="d-flex align-items-center gap-3">
                  <!-- 車款圖片或預設icon -->
                  <div class="car-img-wrapper bg-light rounded d-flex align-items-center justify-content-center" style="width: 80px; height: 50px; overflow: hidden;">
                    <img v-if="model.vehicleImgUrl" :src="model.vehicleImgUrl" class="w-100 h-100 object-fit-cover" />
                    <i v-else class="fa-solid fa-car fa-2x text-muted"></i>
                  </div>
                  <div>
                    <h6 class="fw-bold mb-1">{{ model.brand }} {{ model.modelName }}</h6>
                    <small class="text-muted"><i class="fa-solid fa-user me-1"></i>{{ model.seats }}人座</small>
                  </div>
                </div>
                <div class="select-indicator fs-5 text-primary">
                  <i v-if="selectedModel && selectedModel.modelId === model.modelId" class="fa-solid fa-circle-check"></i>
                  <i v-else class="fa-regular fa-circle text-muted"></i>
                </div>
              </div>
            </div>
          </div>

          <!-- 展開/折疊更多車型 -->
          <div class="text-center mt-2">
            <button class="btn btn-link text-decoration-none fw-bold" @click="showAllModels = !showAllModels">
              {{ showAllModels ? '收合車型' : '更多車型' }}
              <i class="fa-solid ms-1" :class="showAllModels ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
            </button>
          </div>
          <div v-if="errors.model" class="text-danger small mt-2"><i class="fa-solid fa-circle-info me-1"></i>{{ errors.model }}</div>
        </div>

        <!-- 卡片三：行程資訊 -->
        <div class="card shadow-sm border-0 rounded-4 p-4 mb-4">
          <h5 class="fw-bold mb-4 border-start border-primary border-4 ps-2 text-primary">行程資訊</h5>
          
          <div class="alert alert-info border-0 rounded-3 small text-muted mb-4">
            <ul class="mb-0 ps-3">
              <li>請至少提前 3 個工作天預約。遇連續假期或春節期間，請提前 7 個工作天預約。</li>
              <li>若來不及提前預約，請聯絡客服中心 <a href="tel:0800222568" class="fw-bold text-decoration-none">0800-222-568</a>。</li>
            </ul>
          </div>

          <!-- 行車時間 -->
          <div class="row g-3 mb-4">
            <div class="col-md-6">
              <label class="form-label small fw-bold text-secondary"><span class="text-danger">*</span> 行車開始時間（司機報到）</label>
              <div class="d-flex gap-2">
                <input type="date" class="form-control" v-model="pickupDate" :class="{ 'is-invalid': errors.pickupTime || errors.timeOrder }" />
                <input type="time" class="form-control" v-model="pickupTime" :class="{ 'is-invalid': errors.pickupTime || errors.timeOrder }" />
              </div>
            </div>
            <div class="col-md-6">
              <label class="form-label small fw-bold text-secondary"><span class="text-danger">*</span> 行車結束時間</label>
              <div class="d-flex gap-2">
                <input type="date" class="form-control" v-model="dropoffDate" :class="{ 'is-invalid': errors.dropoffTime || errors.timeOrder }" />
                <input type="time" class="form-control" v-model="dropoffTime" :class="{ 'is-invalid': errors.dropoffTime || errors.timeOrder }" />
              </div>
            </div>
            <div v-if="errors.pickupTime" class="text-danger small mt-1"><i class="fa-solid fa-circle-info me-1"></i>{{ errors.pickupTime }}</div>
            <div v-if="errors.dropoffTime" class="text-danger small mt-1"><i class="fa-solid fa-circle-info me-1"></i>{{ errors.dropoffTime }}</div>
            <div v-if="errors.timeOrder" class="text-danger small mt-1"><i class="fa-solid fa-circle-info me-1"></i>{{ errors.timeOrder }}</div>
          </div>

          <!-- 起迄地點 -->
          <div class="mb-4">
            <label class="form-label small fw-bold text-secondary"><span class="text-danger">*</span> 出發地點</label>
            <input type="text" class="form-control py-2" placeholder="地點" v-model="pickupLocation" :class="{ 'is-invalid': errors.pickupLocation }" />
            <div v-if="errors.pickupLocation" class="invalid-feedback">{{ errors.pickupLocation }}</div>
          </div>

          <!-- 中途停靠點 -->
          <div class="mb-4">
            <label class="form-label small fw-bold text-secondary">中途停靠點</label>
            <div v-for="(mid, idx) in midpoints" :key="idx" class="d-flex gap-2 mb-2">
              <input type="text" class="form-control" placeholder="輸入停靠點" v-model="mid.value" />
              <button class="btn btn-outline-danger btn-sm px-3" @click="removeMidpoint(idx)">
                <i class="fa-solid fa-trash"></i>
              </button>
            </div>
            <button class="btn btn-link text-decoration-none fw-bold p-0 mt-1 d-flex align-items-center gap-1" @click="addMidpoint">
              <i class="fa-solid fa-plus"></i> 新增停靠點
            </button>
          </div>

          <div class="mb-4">
            <label class="form-label small fw-bold text-secondary"><span class="text-danger">*</span> 到達地點</label>
            <input type="text" class="form-control py-2" placeholder="地點" v-model="dropoffLocation" :class="{ 'is-invalid': errors.dropoffLocation }" />
            <div v-if="errors.dropoffLocation" class="invalid-feedback">{{ errors.dropoffLocation }}</div>
          </div>

          <!-- 備註 -->
          <div class="mb-2">
            <label class="form-label small fw-bold text-secondary">備註</label>
            <textarea class="form-control" rows="4" placeholder="請詳述您的需求" maxlength="500" v-model="note"></textarea>
            <div class="text-end text-muted small mt-1">{{ note.length }}/500</div>
          </div>
        </div>

        <!-- 卡片四：聯絡資訊 -->
        <div class="card shadow-sm border-0 rounded-4 p-4 mb-4">
          <h5 class="fw-bold mb-4 border-start border-primary border-4 ps-2 text-primary">聯絡資訊</h5>

          <div class="row g-3 mb-4">
            <div class="col-md-6">
              <label class="form-label small fw-bold text-secondary"><span class="text-danger">*</span> 預約人姓名</label>
              <input type="text" class="form-control py-2" placeholder="姓名" v-model="bookerName" :class="{ 'is-invalid': errors.bookerName }" />
              <div v-if="errors.bookerName" class="invalid-feedback">{{ errors.bookerName }}</div>
            </div>
            <div class="col-md-6">
              <label class="form-label small fw-bold text-secondary">
                <span class="text-danger">*</span> 預約人電子信箱
                <i class="fa-solid fa-circle-info text-muted ms-1" title="我們將發送預約明細至此信箱"></i>
              </label>
              <input type="email" class="form-control py-2" placeholder="account@domain.com" v-model="bookerEmail" :class="{ 'is-invalid': errors.bookerEmail }" />
              <div v-if="errors.bookerEmail" class="invalid-feedback">{{ errors.bookerEmail }}</div>
            </div>
          </div>

          <div class="mb-4">
            <label class="form-label small fw-bold text-secondary"><span class="text-danger">*</span> 預約人手機號碼</label>
            <div class="d-flex gap-2">
              <input type="tel" class="form-control py-2" placeholder="請輸入手機號碼" v-model="bookerPhone" :class="{ 'is-invalid': errors.bookerPhone }" />
            </div>
            <div v-if="errors.bookerPhone" class="text-danger small mt-1"><i class="fa-solid fa-circle-info me-1"></i>{{ errors.bookerPhone }}</div>
          </div>

          <!-- 備用手機號碼 -->
          <div class="mb-4">
            <div v-for="(bp, idx) in backupPhones" :key="idx" class="d-flex gap-2 mb-2 align-items-center">
              <input type="tel" class="form-control" placeholder="請輸入備用手機號碼" v-model="bp.number" />
              <button class="btn btn-outline-danger btn-sm px-3 py-2" @click="removeBackupPhone(idx)">
                <i class="fa-solid fa-trash"></i>
              </button>
            </div>
            <button class="btn btn-link text-decoration-none fw-bold p-0 mt-1 d-flex align-items-center gap-1" @click="addBackupPhone">
              <i class="fa-solid fa-plus"></i> 新增備用手機號碼
            </button>
          </div>

          <div class="form-check mb-4">
            <input class="form-check-input" type="checkbox" id="sameAsBookerCheck" v-model="sameAsBooker">
            <label class="form-check-label fw-bold text-secondary" for="sameAsBookerCheck">
              聯絡人同預約人
            </label>
          </div>

          <!-- 聯絡人資訊 -->
          <div class="row g-3 mb-4" v-if="!sameAsBooker">
            <div class="col-md-6">
              <label class="form-label small fw-bold text-secondary"><span class="text-danger">*</span> 聯絡人姓名</label>
              <input type="text" class="form-control py-2" placeholder="姓名" v-model="contactName" :class="{ 'is-invalid': errors.contactName }" />
              <div v-if="errors.contactName" class="invalid-feedback">{{ errors.contactName }}</div>
            </div>
            <div class="col-md-6">
              <label class="form-label small fw-bold text-secondary"><span class="text-danger">*</span> 聯絡人手機號碼</label>
              <div class="d-flex gap-2">
                <input type="tel" class="form-control py-2" placeholder="請輸入手機號碼" v-model="contactPhone" :class="{ 'is-invalid': errors.contactPhone }" />
              </div>
              <div v-if="errors.contactPhone" class="text-danger small mt-1"><i class="fa-solid fa-circle-info me-1"></i>{{ errors.contactPhone }}</div>
            </div>
          </div>

          <div class="mb-2">
            <label class="form-label small fw-bold text-secondary"><span class="text-danger">*</span> 租車用途</label>
            <select class="form-select py-2" v-model="rentalPurpose">
              <option value="商務接送">商務接送</option>
              <option value="婚禮用車">婚禮用車</option>
              <option value="旅遊包車">旅遊包車</option>
              <option value="高鐵/火車站接送">高鐵/火車站接送</option>
              <option value="其他">其他</option>
            </select>
          </div>
        </div>

        <!-- 提交按鈕與重填 -->
        <div class="d-grid gap-3 mt-5">
          <button class="btn btn-primary btn-lg py-3 rounded-3 fw-bold" @click="submitForm">
            送出需求單
          </button>
          <button class="btn btn-link text-secondary text-decoration-none fw-medium text-center" @click="resetForm">
            清除重填
          </button>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.car-card {
  border-color: #dee2e6 !important;
}
.car-card:hover {
  border-color: #0d6efd !important;
  background-color: rgba(13, 110, 253, 0.03);
}
.active-card {
  border-color: #0d6efd !important;
  box-shadow: 0 4px 12px rgba(13, 110, 253, 0.15);
}
.cursor-pointer {
  cursor: pointer;
}
.transition-all {
  transition: all 0.25s ease-in-out;
}
.breadcrumb-item a {
  color: #6c757d;
}
.breadcrumb-item a:hover {
  color: #0d6efd;
}
</style>
