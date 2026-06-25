<script setup>
import { ref, computed } from 'vue'
import StepProgressBar from '@/components/customer/transfer/StepProgressBar.vue'
import { taiwanDistricts } from '@/utils/taiwanDistricts'
import request from '@/api/index'

const emit = defineEmits(['navigate'])
const props = defineProps({
  bookingData: { type: Object, required: true }
})

// ===== 預約上車時間 =====
const pickupDate = ref('')
const pickupHour = ref('08')
const pickupMinute = ref('00')

// ===== 機場資訊 =====
const airportName = ref('')
const terminal = ref('')
const flightCode = ref('')
const flightHour = ref('08')
const flightMinute = ref('00')

const airports = [
  { name: '桃園國際機場', terminals: ['第一航廈T1', '第二航廈T2', '第三航廈T3'] },
  { name: '松山機場', terminals: ['國內航廈', '國際航廈'] }
]
const availableTerminals = computed(() => {
  const ap = airports.find(a => a.name === airportName.value)
  return ap ? ap.terminals : []
})

// ===== 上車地點（縣市連動）=====
const cities = Object.keys(taiwanDistricts)
const pickupCity = ref('')
const pickupDistrict = ref('')
const pickupAddress = ref('')
const availableDistricts = computed(() => pickupCity.value ? taiwanDistricts[pickupCity.value] : [])

// ===== 車型 =====
const carType = ref('豪華轎車')
const carTypes = [
  { value: '豪華轎車', maxPassenger: 3, maxLuggage: 2 },
  { value: '豪華七人座車', maxPassenger: 6, maxLuggage: 4 },
  { value: '高級進口轎車', maxPassenger: 3, maxLuggage: 2 }
]
const selectedCarInfo = computed(() => carTypes.find(c => c.value === carType.value))

const passengerCount = ref(1)
const luggageCount = ref(1)

// ===== 安全座椅 =====
const babySeat = ref(0)
const childSeat = ref(0)
const boosterSeat = ref(0)

// ===== 舉牌 =====
const needSign = ref('否')
const signName = ref('')

// ===== 提交 =====
const isSubmitting = ref(false)

const submitBooking = async () => {
  // 組合預約時間
  const scheduledPickupTime = `${pickupDate.value}T${pickupHour.value}:${pickupMinute.value}:00`

  // 組合上車/下車地點
  let pickupLocation = ''
  let dropoffLocation = ''
  const transferType = props.bookingData.transferType

  if (transferType === '送機') {
    // 送機：上車=市區地址，下車=機場
    pickupLocation = `${pickupCity.value}${pickupDistrict.value}${pickupAddress.value}`
    dropoffLocation = `${airportName.value}${terminal.value}`
  } else {
    // 接機：上車=機場，下車=市區地址
    pickupLocation = `${airportName.value}${terminal.value}`
    dropoffLocation = `${pickupCity.value}${pickupDistrict.value}${pickupAddress.value}`
  }

  // 組合備註
  const noteLines = []
  if (flightCode.value) noteLines.push(`班機代碼: ${flightCode.value}`)
  if (flightHour.value && flightMinute.value) noteLines.push(`班機時間: ${flightHour.value}:${flightMinute.value}`)
  noteLines.push(`車型: ${carType.value}`)
  noteLines.push(`行李數: ${luggageCount.value}`)
  if (babySeat.value > 0) noteLines.push(`嬰兒座椅(0-2歲): ${babySeat.value}`)
  if (childSeat.value > 0) noteLines.push(`幼童座椅(2-4歲): ${childSeat.value}`)
  if (boosterSeat.value > 0) noteLines.push(`兒童增高墊(4-12歲): ${boosterSeat.value}`)
  if (needSign.value === '是' && signName.value) noteLines.push(`舉牌姓名: ${signName.value}`)
  if (props.bookingData.email) noteLines.push(`Email: ${props.bookingData.email}`)

  const orderData = {
    custId: props.bookingData.custId,
    custPhone: props.bookingData.custPhone,
    transferType: transferType,
    pickupLocation: pickupLocation,
    dropoffLocation: dropoffLocation,
    scheduledPickupTime: scheduledPickupTime,
    passengerCount: passengerCount.value,
    status: '待處理',
    note: noteLines.join('\n')
  }

  isSubmitting.value = true
  try {
    await request.post('/transferOrder', orderData)
    alert('預約成功！我們將盡快聯繫您確認訂單。')
    emit('navigate', 'landing')
  } catch (error) {
    console.error('預約失敗:', error)
    alert('預約失敗，請稍後再試或聯繫客服。')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="container-fluid py-4">
    <!-- Hero Banner -->
    <div class="card border-0 shadow-sm mb-4 bg-primary bg-gradient text-white rounded-4">
      <div class="card-body p-4 text-center">
        <i class="fa-solid fa-clipboard-list fa-2x mb-2 opacity-75"></i>
        <h2 class="fw-bold mb-1">接送服務線上預約</h2>
        <p class="mb-0 text-white-50">Online Booking</p>
      </div>
    </div>

    <!-- 步驟進度條 -->
    <StepProgressBar :currentStep="2" />

    <!-- 表單卡片 -->
    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <h4 class="fw-bold text-center mb-4">預約資訊</h4>

        <div class="row g-3">

          <!-- 預約上車時間 -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">預約上車時間 <span class="text-danger">*</span></label>
              </div>
              <div class="col-md-9">
                <div class="d-flex gap-2 flex-wrap">
                  <input v-model="pickupDate" type="date" class="form-control" style="max-width: 180px;" required />
                  <select v-model="pickupHour" class="form-select" style="max-width: 90px;">
                    <option v-for="h in 24" :key="h-1" :value="String(h-1).padStart(2, '0')">{{ String(h-1).padStart(2, '0') }} 時</option>
                  </select>
                  <select v-model="pickupMinute" class="form-select" style="max-width: 90px;">
                    <option v-for="m in 60" :key="m-1" :value="String(m-1).padStart(2, '0')">{{ String(m-1).padStart(2, '0') }} 分</option>
                  </select>
                </div>
                <small class="text-primary d-block mt-1"><i class="fa-solid fa-circle-info me-1"></i>請至少提前 24 小時預約</small>
              </div>
            </div>
          </div>

          <!-- 機場 -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">機場 <span class="text-danger">*</span></label>
              </div>
              <div class="col-md-9">
                <div class="d-flex gap-2 flex-wrap">
                  <select v-model="airportName" class="form-select" style="max-width: 200px;" required>
                    <option value="" disabled>選擇機場</option>
                    <option v-for="ap in airports" :key="ap.name" :value="ap.name">{{ ap.name }}</option>
                  </select>
                  <select v-model="terminal" class="form-select" style="max-width: 160px;" :disabled="!airportName" required>
                    <option value="" disabled>選擇航廈</option>
                    <option v-for="t in availableTerminals" :key="t" :value="t">{{ t }}</option>
                  </select>
                  <input v-model="flightCode" type="text" class="form-control" placeholder="班次代碼" style="max-width: 140px;" />
                </div>
              </div>
            </div>
          </div>

          <!-- 班機起飛時間 -->
          <div class="col-12">
            <div class="row align-items-center">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">班機起飛時間</label>
              </div>
              <div class="col-md-9">
                <div class="d-flex gap-2">
                  <select v-model="flightHour" class="form-select" style="max-width: 90px;">
                    <option v-for="h in 24" :key="h-1" :value="String(h-1).padStart(2, '0')">{{ String(h-1).padStart(2, '0') }} 時</option>
                  </select>
                  <select v-model="flightMinute" class="form-select" style="max-width: 90px;">
                    <option v-for="m in 60" :key="m-1" :value="String(m-1).padStart(2, '0')">{{ String(m-1).padStart(2, '0') }} 分</option>
                  </select>
                </div>
              </div>
            </div>
          </div>

          <!-- 上車/下車地點（市區端） -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">
                  {{ bookingData.transferType === '送機' ? '上車地點' : '下車地點' }}
                  <span class="text-danger">*</span>
                </label>
              </div>
              <div class="col-md-9">
                <div class="d-flex gap-2 flex-wrap">
                  <select v-model="pickupCity" class="form-select" style="max-width: 120px;" required>
                    <option value="" disabled>縣市</option>
                    <option v-for="city in cities" :key="city" :value="city">{{ city }}</option>
                  </select>
                  <select v-model="pickupDistrict" class="form-select" style="max-width: 120px;" :disabled="!pickupCity" required>
                    <option value="" disabled>區域</option>
                    <option v-for="dist in availableDistricts" :key="dist" :value="dist">{{ dist }}</option>
                  </select>
                  <input v-model="pickupAddress" type="text" class="form-control" placeholder="詳細地址（路/街/巷/弄/號）" required />
                </div>
              </div>
            </div>
          </div>

          <hr />

          <!-- 選擇車型 -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">選擇車型 <span class="text-danger">*</span></label>
              </div>
              <div class="col-md-9">
                <div class="d-flex gap-3 flex-wrap mb-3">
                  <div v-for="ct in carTypes" :key="ct.value" class="form-check">
                    <input class="form-check-input" type="radio" :id="'car-' + ct.value" :value="ct.value" v-model="carType" />
                    <label class="form-check-label" :for="'car-' + ct.value">{{ ct.value }}</label>
                  </div>
                </div>
                <div class="d-flex gap-3 flex-wrap mb-2">
                  <div>
                    <label class="form-label text-muted small mb-1">乘客人數</label>
                    <select v-model.number="passengerCount" class="form-select" style="max-width: 100px;">
                      <option v-for="n in (selectedCarInfo?.maxPassenger || 3)" :key="n" :value="n">{{ n }} 人</option>
                    </select>
                  </div>
                  <div>
                    <label class="form-label text-muted small mb-1">行李數</label>
                    <select v-model.number="luggageCount" class="form-select" style="max-width: 100px;">
                      <option v-for="n in (selectedCarInfo?.maxLuggage || 2)" :key="n" :value="n">{{ n }} 件</option>
                    </select>
                  </div>
                </div>
                <small class="text-danger fw-bold d-block"><i class="fa-solid fa-triangle-exclamation me-1"></i>行李數如超出標準數量，可能需要加訂車輛，客服人員將與您確認。</small>
              </div>
            </div>
          </div>

          <hr />

          <!-- 安全座椅 -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">安全座椅</label>
              </div>
              <div class="col-md-9">
                <div class="d-flex gap-3 flex-wrap">
                  <div>
                    <label class="form-label text-muted small mb-1">嬰兒型 (0-2歲)</label>
                    <select v-model.number="babySeat" class="form-select" style="max-width: 80px;">
                      <option v-for="n in 4" :key="n-1" :value="n-1">{{ n-1 }}</option>
                    </select>
                  </div>
                  <div>
                    <label class="form-label text-muted small mb-1">幼童型 (2-4歲)</label>
                    <select v-model.number="childSeat" class="form-select" style="max-width: 80px;">
                      <option v-for="n in 4" :key="n-1" :value="n-1">{{ n-1 }}</option>
                    </select>
                  </div>
                  <div>
                    <label class="form-label text-muted small mb-1">兒童增高墊 (4-12歲)</label>
                    <select v-model.number="boosterSeat" class="form-select" style="max-width: 80px;">
                      <option v-for="n in 4" :key="n-1" :value="n-1">{{ n-1 }}</option>
                    </select>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 舉牌 -->
          <div class="col-12">
            <div class="row align-items-start">
              <div class="col-md-3">
                <label class="form-label text-muted small fw-bold mb-1">是否需舉牌</label>
              </div>
              <div class="col-md-9">
                <div class="d-flex gap-3 mb-2">
                  <div class="form-check">
                    <input class="form-check-input" type="radio" id="sign-no" value="否" v-model="needSign" />
                    <label class="form-check-label" for="sign-no">否</label>
                  </div>
                  <div class="form-check">
                    <input class="form-check-input" type="radio" id="sign-yes" value="是" v-model="needSign" />
                    <label class="form-check-label" for="sign-yes">是</label>
                  </div>
                </div>
                <input v-if="needSign === '是'" v-model="signName" type="text" class="form-control" placeholder="請輸入舉牌姓名" style="max-width: 300px;" />
              </div>
            </div>
          </div>

        </div>

        <!-- 動作按鈕 -->
        <div class="d-flex justify-content-center gap-3 mt-4 pt-3 border-top">
          <button class="btn btn-light px-4" @click="emit('navigate', 'step1')">
            <i class="fa-solid fa-arrow-left me-1"></i>上一步
          </button>
          <button class="btn btn-primary px-5 rounded-pill" @click="submitBooking" :disabled="isSubmitting">
            <span v-if="isSubmitting"><i class="fa-solid fa-spinner fa-spin me-1"></i>提交中...</span>
            <span v-else>送出預約 <i class="fa-solid fa-paper-plane ms-1"></i></span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@media (max-width: 768px) {
  .row.align-items-center .col-md-3,
  .row.align-items-start .col-md-3 {
    margin-bottom: 0.25rem;
  }
}
</style>
