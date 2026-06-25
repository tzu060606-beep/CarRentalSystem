<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/api/index';
import { taiwanDistricts } from '@/utils/taiwanDistricts';

// =============================================
// ===== 路由與基本參數 =====
// =============================================
const router = useRouter()
const route = useRoute()
const orderId = route.params.id  // 從網址取得訂單 ID

// =============================================
// ===== 響應式資料 =====
// =============================================
const rates = ref([])              // 費率方案列表
const availableVehicles = ref([])  // 可用車輛列表（依時段查詢）
const drivers = ref([])            // 司機列表
const order = ref({                // 訂單表單資料
  custId: null,
  custPhone: '',
  vehicleId: null,
  driverId: null,
  transferType: '',
  rateId: '',
  startMileage: null,
  endMileage: null,
  pickupLocation: '',
  dropoffLocation: '',
  scheduledPickupTime: '',
  scheduledDropoffTime: '',
  realDropoffTime: '',
  passengerCount: 1,
  totalAmount: 0,
  status: '',
  note: ''
})

// =============================================
// ===== 地址連動邏輯（縣市 → 區域 → 詳細地址）=====
// =============================================
const cities = Object.keys(taiwanDistricts)

// 上車地址欄位
const pickupCity = ref('')
const pickupDistrict = ref('')
const pickupDetail = ref('')

// 下車地址欄位
const dropoffCity = ref('')
const dropoffDistrict = ref('')
const dropoffDetail = ref('')

// 依選擇的縣市，動態計算可選區域
const availablePickupDistricts = computed(() => pickupCity.value ? taiwanDistricts[pickupCity.value] : [])
const availableDropoffDistricts = computed(() => dropoffCity.value ? taiwanDistricts[dropoffCity.value] : [])

// 切換縣市時，清空區域選擇
watch(pickupCity, (newVal, oldVal) => { if (oldVal) pickupDistrict.value = '' })
watch(dropoffCity, (newVal, oldVal) => { if (oldVal) dropoffDistrict.value = '' })

// 地址欄位變動時，自動組合完整地址（接機模式的上車地點為機場選單，不需組合）
watch([pickupCity, pickupDistrict, pickupDetail], () => {
  if (order.value.transferType !== '接機') {
    order.value.pickupLocation = `${pickupCity.value}${pickupDistrict.value}${pickupDetail.value}`
  }
})
watch([dropoffCity, dropoffDistrict, dropoffDetail], () => {
  if (order.value.transferType !== '送機') {
    order.value.dropoffLocation = `${dropoffCity.value}${dropoffDistrict.value}${dropoffDetail.value}`
  }
})

// 切換接送類型時，清空對應的地址欄位（接機 → 清上車地址；送機 → 清下車地址）
watch(() => order.value.transferType, (newVal, oldVal) => {
  if (oldVal !== '') {
    if (newVal === '接機') {
      order.value.pickupLocation = ''
      pickupCity.value = ''
      pickupDistrict.value = ''
      pickupDetail.value = ''
    } else if (newVal === '送機') {
      order.value.dropoffLocation = ''
      dropoffCity.value = ''
      dropoffDistrict.value = ''
      dropoffDetail.value = ''
    }
  }
})

// =============================================
// ===== API 呼叫：取得費率列表 =====
// =============================================
const fetchRates = async () => {
  try {
    const response = await request.get('/transferRate')
    rates.value = response.data
  } catch (error) {
    console.error('無法取得費率列表:', error)
  }
}

// =============================================
// ===== API 呼叫：取得單筆訂單（含地址拆解）=====
// =============================================
const fetchOrder = async () => {
  try {
    const response = await request.get(`/transferOrder/${orderId}`)
    const data = response.data

    // 將後端回傳的時間格式截斷為 YYYY-MM-DDTHH:mm，以符合 <input type="datetime-local"> 的格式要求
    if (data.scheduledPickupTime) data.scheduledPickupTime = data.scheduledPickupTime.substring(0, 16);
    if (data.scheduledDropoffTime) data.scheduledDropoffTime = data.scheduledDropoffTime.substring(0, 16);
    if (data.realDropoffTime) data.realDropoffTime = data.realDropoffTime.substring(0, 16);

    order.value = data

    // --- 輔助：將「臺」統一為「台」，以匹配 taiwanDistricts 的 key ---
    const normalize = (str) => str.replace(/臺/g, '台').trim()

    // --- 拆解上車地址：將完整地址還原為 縣市 / 區域 / 詳細 三個欄位 ---
    if (order.value.transferType !== '接機') {
      const pLocRaw = order.value.pickupLocation || ''
      const pLoc = normalize(pLocRaw)
      let matched = false
      for (const city of cities) {
        if (pLoc.startsWith(city)) {
          pickupCity.value = city
          const rest = pLoc.substring(city.length)
          for (const dist of taiwanDistricts[city]) {
            if (rest.startsWith(dist)) {
              pickupDistrict.value = dist
              pickupDetail.value = rest.substring(dist.length)
              matched = true
              break
            }
          }
          if (!matched) { pickupDetail.value = rest }
          matched = true
          break
        }
      }
      if (!matched) { pickupDetail.value = pLocRaw }
    }

    // --- 拆解下車地址 ---
    if (order.value.transferType !== '送機') {
      const dLocRaw = order.value.dropoffLocation || ''
      const dLoc = normalize(dLocRaw)
      let matched = false
      for (const city of cities) {
        if (dLoc.startsWith(city)) {
          dropoffCity.value = city
          const rest = dLoc.substring(city.length)
          for (const dist of taiwanDistricts[city]) {
            if (rest.startsWith(dist)) {
              dropoffDistrict.value = dist
              dropoffDetail.value = rest.substring(dist.length)
              matched = true
              break
            }
          }
          if (!matched) { dropoffDetail.value = rest }
          matched = true
          break
        }
      }
      if (!matched) { dropoffDetail.value = dLocRaw }
    }

  } catch (error) {
    console.error(error)
  }
}

// =============================================
// ===== API 呼叫：依預約時段查詢可用車輛 =====
// =============================================
const fetchAvailableVehicles = async () => {
  const start = order.value.scheduledPickupTime;
  const end = order.value.scheduledDropoffTime;

  // 必須同時填寫上車、下車時間才能查詢
  if (start && end) {
    try {
      // datetime-local 回傳 16 字元（YYYY-MM-DDTHH:mm），需補 :00 秒數給後端
      const reqStart = start.length === 16 ? start + ':00' : start;
      const reqEnd = end.length === 16 ? end + ':00' : end;
      const response = await request.get('/vehicle/available', {
        params: {
          reqStartTime: reqStart,
          reqEndTime: reqEnd
        }
      })
      availableVehicles.value = response.data
      
      // 若訂單原本已綁定車輛，但該車不在此時段的可用列表中，
      // 額外呼叫 API 取得該車資料並補入選項，確保下拉選單能顯示原選擇
      if (order.value.vehicleId && !availableVehicles.value.find(v => v.vehicleId === order.value.vehicleId)) {
        try {
          const vRes = await request.get(`/vehicle/${order.value.vehicleId}`)
          const vData = vRes.data
          availableVehicles.value.push({
            vehicleId: vData.vehicleId,
            plateNo: vData.plateNo || `(ID: ${order.value.vehicleId})`,
            // 確保自動帶入功能正常運作
            mileage: vData.mileage ?? null
          })
        } catch (e) {
          // 查詢車輛失敗時，僅顯示 ID 作為備援
          availableVehicles.value.push({
            vehicleId: order.value.vehicleId,
            plateNo: `(ID: ${order.value.vehicleId})`,
            mileage: null
          })
        }
      }
    } catch (error) {
      console.error('無法取得可用車輛:', error)
    }
  } else {
    availableVehicles.value = []
  }
}

// 監聽預約時間變動，自動重新查詢可用車輛
watch([() => order.value.scheduledPickupTime, () => order.value.scheduledDropoffTime], () => {
  fetchAvailableVehicles()
})

// =============================================
// ===== 車輛選擇事件：自動帶入里程與司機 =====
// =============================================
const onVehicleSelect = () => {
  if (order.value.vehicleId) {
    // 帶入選中車輛的目前里程作為起始里程
    const v = availableVehicles.value.find(x => x.vehicleId === order.value.vehicleId)
    if (v && v.mileage != null) {
      order.value.startMileage = v.mileage
    }
    
    // 自動帶入綁定該車輛的司機
    const assignedDriver = drivers.value.find(d => d.vehicle && d.vehicle.vehicleId === order.value.vehicleId)
    if (assignedDriver) {
      order.value.driverId = assignedDriver.driverId
    } else {
      order.value.driverId = null
    }
  } else {
    // 清除車輛選擇時，同步清空司機與里程
    order.value.driverId = null
    order.value.startMileage = null
  }
}

// =============================================
// ===== API 呼叫：取得司機列表 =====
// =============================================
const fetchDrivers = async () => {
  try {
    const response = await request.get('/drivers')
    drivers.value = response.data
  } catch (error) {
    console.error('無法取得司機資料:', error)
  }
}

// =============================================
// ===== 表單送出：更新訂單 =====
// =============================================
const submitForm = async () => {
  try {
    await request.put(`/transferOrder/${orderId}`, order.value)
    alert('修改成功')
    router.push('/transferOrder')
  } catch (error) {
    console.error(error)
    alert('修改失敗')
  }
}

// =============================================
// ===== 頁面初始化：依序載入所有資料 =====
// =============================================
onMounted(async () => {
  await fetchRates()       // 1. 取得費率方案
  await fetchDrivers()     // 2. 取得司機列表
  await fetchOrder()       // 3. 取得訂單資料（含地址拆解）
  await fetchAvailableVehicles()  // 4. 依訂單時段查詢可用車輛

  // 5. 若訂單已有指派車輛且起始里程尚未填入，自動從車輛資料帶入 mileage
  if (order.value.vehicleId && order.value.startMileage == null) {
    const v = availableVehicles.value.find(x => x.vehicleId === order.value.vehicleId)
    if (v && v.mileage != null) {
      order.value.startMileage = v.mileage
    }
  }
})
</script>
<!-- <script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const orderId = route.params.id

const rates = ref([])
const order = ref({
  custId: null,
  custPhone: '',
  transferType: '',
  rateId: '',
  startMileage: null,
  endMileage: null,
  pickupLocation: '',
  dropoffLocation: '',
  totalAmount: 0,
  status: '',
  note: ''
})

const fetchRates = async () => {
  try {
    const response = await axios.get('http://localhost:8081/api/transferRate')
    rates.value = response.data
  } catch (error) {
    console.error('無法取得費率列表:', error)
  }
}

const fetchOrder = async () => {
  try {
    const response = await axios.get(`http://localhost:8081/api/transferOrder/${orderId}`)
    order.value = response.data
  } catch (error) {
    console.error(error)
  }
}

const submitForm = async () => {
  try {
    await axios.put(`http://localhost:8081/api/transferOrder/${orderId}`, order.value)
    alert('修改成功')
    router.push('/transferOrder/search')
  } catch (error) {
    console.error(error)
    alert('修改失敗')
  }
}

onMounted(() => {
  fetchRates()
  fetchOrder()
})
</script> -->

<template>
  <div class="container-fluid py-4">
    <div class="card border-0 shadow-sm mb-4 bg-primary bg-gradient text-white rounded-4">
      <div class="card-body p-4">
        <h2 class="fw-bold mb-1"><i class="fa-solid fa-pen-to-square me-2"></i>修改訂單</h2>
        <p class="mb-0 text-white-50">Edit Transfer Order</p>
      </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <form @submit.prevent="submitForm">
          <div class="row g-3">
            
            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">客戶編號 <span class="text-danger">*</span></label>
              <input v-model="order.custId" type="number" class="form-control" required placeholder="例如: 1" />
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">聯絡電話 <span class="text-danger">*</span></label>
              <input v-model="order.custPhone" type="text" class="form-control" required placeholder="09XX-XXX-XXX" />
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">指派車輛</label>
              <select v-model="order.vehicleId" class="form-select" :disabled="!availableVehicles.length && (!order.scheduledPickupTime || !order.scheduledDropoffTime)" @change="onVehicleSelect">
                <option :value="null">-- 請選擇可用車輛 --</option>
                <option v-for="v in availableVehicles" :key="v.vehicleId" :value="v.vehicleId">
                  {{ v.plateNo }} (ID: {{ v.vehicleId }})
                </option>
              </select>
              <span v-if="!order.scheduledPickupTime || !order.scheduledDropoffTime" class="text-warning small d-block mt-1">
                <i class="fa-solid fa-circle-info"></i> 請先填寫預計上、下車時間
              </span>
              <span v-else-if="availableVehicles.length === 0" class="text-danger small d-block mt-1">
                <i class="fa-solid fa-triangle-exclamation"></i> 該時段無可用車輛
              </span>
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">指派司機編號</label>
              <input v-model="order.driverId" type="number" class="form-control" placeholder="系統自動帶入" readonly />
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">接送類型 <span class="text-danger">*</span></label>
              <select v-model="order.transferType" class="form-select" required>
                <option value="" disabled>請選擇接送類型</option>
                <option value="送機">送機</option>
                <option value="接機">接機</option>
                <option value="一般">一般</option>
              </select>
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">選擇費率 <span class="text-danger">*</span></label>
              <select v-model="order.rateId" class="form-select" required>
                <option value="" disabled>請選擇費率方案</option>
                <option v-for="rate in rates" :key="rate.rateId" :value="rate.rateId">
                  {{ rate.rateName }} (基本費: {{ rate.baseFee }})
                </option>
              </select>
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">總費用 (自動計算)</label>
              <input v-model="order.totalAmount" type="number" step="0.01" class="form-control bg-light" placeholder="儲存後自動計算" readonly />
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">乘客數</label>
              <select v-model.number="order.passengerCount" class="form-select">
                <option v-for="n in 8" :key="n" :value="n">{{ n }} 人</option>
              </select>
            </div>

            <div class="col-12">
              <label class="form-label text-muted small fw-bold mb-1">上車地點 <span class="text-danger">*</span></label>
              <template v-if="order.transferType === '接機'">
                <select v-model="order.pickupLocation" class="form-select" required>
                  <option value="" disabled>請選擇上車機場/航廈</option>
                  <option value="桃園機場第一航廈T1">桃園機場第一航廈T1</option>
                  <option value="桃園機場第二航廈T2">桃園機場第二航廈T2</option>
                  <option value="桃園機場第三航廈T3">桃園機場第三航廈T3</option>
                  <option value="松山機場">松山機場</option>
                </select>
              </template>
              <template v-else>
                <div class="d-flex gap-2">
                  <select v-model="pickupCity" class="form-select" style="max-width: 120px;" required>
                    <option value="" disabled>縣市</option>
                    <option v-for="city in cities" :key="city" :value="city">{{ city }}</option>
                  </select>
                  <select v-model="pickupDistrict" class="form-select" style="max-width: 120px;" :disabled="!pickupCity" required>
                    <option value="" disabled>區域</option>
                    <option v-for="dist in availablePickupDistricts" :key="dist" :value="dist">{{ dist }}</option>
                  </select>
                  <input v-model="pickupDetail" type="text" class="form-control" required placeholder="路/街/巷/弄/號" />
                </div>
              </template>
            </div>

            <div class="col-12">
              <label class="form-label text-muted small fw-bold mb-1">下車地點 <span class="text-danger">*</span></label>
              <template v-if="order.transferType === '送機'">
                <select v-model="order.dropoffLocation" class="form-select" required>
                  <option value="" disabled>請選擇下車機場/航廈</option>
                  <option value="桃園機場第一航廈T1">桃園機場第一航廈T1</option>
                  <option value="桃園機場第二航廈T2">桃園機場第二航廈T2</option>
                  <option value="桃園機場第三航廈T3">桃園機場第三航廈T3</option>
                  <option value="松山機場">松山機場</option>
                </select>
              </template>
              <template v-else>
                <div class="d-flex gap-2">
                  <select v-model="dropoffCity" class="form-select" style="max-width: 120px;" required>
                    <option value="" disabled>縣市</option>
                    <option v-for="city in cities" :key="city" :value="city">{{ city }}</option>
                  </select>
                  <select v-model="dropoffDistrict" class="form-select" style="max-width: 120px;" :disabled="!dropoffCity" required>
                    <option value="" disabled>區域</option>
                    <option v-for="dist in availableDropoffDistricts" :key="dist" :value="dist">{{ dist }}</option>
                  </select>
                  <input v-model="dropoffDetail" type="text" class="form-control" required placeholder="路/街/巷/弄/號" />
                </div>
              </template>
            </div>

            <div class="col-md-4">
              <label class="form-label text-muted small fw-bold mb-1">預計上車時間</label>
              <input v-model="order.scheduledPickupTime" type="datetime-local" class="form-control" />
            </div>

            <div class="col-md-4">
              <label class="form-label text-muted small fw-bold mb-1">預計下車時間</label>
              <input v-model="order.scheduledDropoffTime" type="datetime-local" class="form-control" />
            </div>

            <div class="col-md-4">
              <label class="form-label text-muted small fw-bold mb-1">實際下車時間</label>
              <input v-model="order.realDropoffTime" type="datetime-local" class="form-control" />
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">起始里程</label>
              <input v-model="order.startMileage" type="number" class="form-control" placeholder="系統自動帶入" readonly />
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">結束里程</label>
              <input v-model="order.endMileage" type="number" class="form-control" placeholder="系統自動帶入" readonly />
            </div>

            <div class="col-md-12">
              <label class="form-label text-muted small fw-bold mb-1">訂單狀態</label>
              <select v-model="order.status" class="form-select">
                <option value="待處理">待處理</option>
                <option value="處理中">處理中</option>
                <option value="已完成">已完成</option>
                <option value="已取消">已取消</option>
              </select>
            </div>

            <div class="col-12">
              <label class="form-label text-muted small fw-bold mb-1">備註</label>
              <textarea v-model="order.note" rows="3" class="form-control" placeholder="其他特殊需求或備註說明..."></textarea>
            </div>

          </div>

          <div class="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
            <button type="button" class="btn btn-light" @click="$router.push('/transferOrder')">取消返回</button>
            <button type="submit" class="btn btn-primary px-4">儲存變更</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* .transfer-container {
  padding: 2rem;
  max-width: 900px;
  margin: 0 auto;
  font-family: 'Inter', 'Roboto', sans-serif;
}
.header-section {
  text-align: center;
  margin-bottom: 2rem;
}
.title {
  font-size: 2.2rem;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.5rem;
}
.subtitle {
  color: #888;
  letter-spacing: 1px;
}
.form-card {
  background: white;
  border-radius: 16px;
  padding: 2.5rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  border: 1px solid #eee;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}
.form-group {
  display: flex;
  flex-direction: column;
}
.full-width {
  grid-column: 1 / -1;
}
.form-group label {
  font-size: 0.95rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.5rem;
}
.required {
  color: #ef4444;
  margin-left: 2px;
}
.modern-form input,
.modern-form select,
.modern-form textarea {
  padding: 0.8rem 1rem;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background-color: #f8fafc;
  font-size: 1rem;
  transition: all 0.2s ease;
  font-family: inherit;
}
.modern-form input:focus,
.modern-form select:focus,
.modern-form textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background-color: white;
}
.readonly-input {
  background-color: #e2e8f0 !important;
  color: #64748b;
  cursor: not-allowed;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #eee;
}
.btn-cancel, .btn-submit {
  padding: 0.8rem 1.8rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}
.btn-cancel {
  background: #f1f5f9;
  color: #64748b;
}
.btn-cancel:hover {
  background: #e2e8f0;
  color: #0f172a;
}
.btn-submit {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}
.btn-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
} */
</style>
