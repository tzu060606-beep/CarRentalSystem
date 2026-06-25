<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/index';
import { taiwanDistricts } from '@/utils/taiwanDistricts';

const router = useRouter()
const rates = ref([])
const customerName = ref('') // 用來顯示查詢到的客戶名稱
const availableVehicles = ref([])
const drivers = ref([])

const order = ref({
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
  passengerCount: 1,
  totalAmount: 0,
  status: '待處理',
  note: ''
})

const fetchRates = async () => {
  try {
    const response = await request.get('/transferRate')
    rates.value = response.data
  } catch (error) {
    console.error('無法取得費率列表:', error)
  }
}

// ===== 地址連動邏輯 =====
const cities = Object.keys(taiwanDistricts)
const pickupCity = ref('')
const pickupDistrict = ref('')
const pickupDetail = ref('')

const dropoffCity = ref('')
const dropoffDistrict = ref('')
const dropoffDetail = ref('')

const availablePickupDistricts = computed(() => pickupCity.value ? taiwanDistricts[pickupCity.value] : [])
const availableDropoffDistricts = computed(() => dropoffCity.value ? taiwanDistricts[dropoffCity.value] : [])

watch(pickupCity, (newVal, oldVal) => { if (oldVal) pickupDistrict.value = '' })
watch(dropoffCity, (newVal, oldVal) => { if (oldVal) dropoffDistrict.value = '' })

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

// 根據客戶編號自動帶入會員資料
const fetchCustomerData = async () => {
  if (!order.value.custId) {
    customerName.value = ''
    order.value.custPhone = ''
    return
  }
  
  try {
    const response = await request.get(`/customers/${order.value.custId}`)
    if (response && response.data) {
      customerName.value = response.data.custName
      order.value.custPhone = response.data.custPhone
    }
  } catch (error) {
    console.error('無法取得客戶資料:', error)
    customerName.value = '查無此客戶'
    // 若查無此人，也可選擇清空電話
    order.value.custPhone = ''
  }
}

const fetchAvailableVehicles = async () => {
  const start = order.value.scheduledPickupTime;
  const end = order.value.scheduledDropoffTime;
  if (start && end) {
    try {
      const reqStart = start.length === 16 ? start + ':00' : start;
      const reqEnd = end.length === 16 ? end + ':00' : end;
      const response = await request.get('/vehicle/available', {
        params: {
          reqStartTime: reqStart,
          reqEndTime: reqEnd
        }
      })
      availableVehicles.value = response.data
    } catch (error) {
      console.error('無法取得可用車輛:', error)
    }
  } else {
    availableVehicles.value = []
  }
}

watch([() => order.value.scheduledPickupTime, () => order.value.scheduledDropoffTime], () => {
  fetchAvailableVehicles()
})

const onVehicleSelect = () => {
  if (order.value.vehicleId) {
    const v = availableVehicles.value.find(x => x.vehicleId === order.value.vehicleId)
    if (v && v.mileage != null) {
      order.value.startMileage = v.mileage
    }
    
    // 尋找綁定該車輛的司機
    const assignedDriver = drivers.value.find(d => d.vehicle && d.vehicle.vehicleId === order.value.vehicleId)
    if (assignedDriver) {
      order.value.driverId = assignedDriver.driverId
    } else {
      order.value.driverId = null
    }
  } else {
    order.value.driverId = null
    order.value.startMileage = null
  }
}

const fetchDrivers = async () => {
  try {
    const response = await request.get('/drivers')
    drivers.value = response.data
  } catch (error) {
    console.error('無法取得司機資料:', error)
  }
}

onMounted(() => {
  fetchRates()
  fetchDrivers()
})

const submitForm = async () => {
  try {
    await request.post('/transferOrder', order.value)
    alert('新增成功')
    router.push('/transferOrder')
  } catch (error) {
    console.error(error)
    alert('新增失敗')
  }
}
</script>
<!-- <script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
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
  scheduledPickupTime: '',
  scheduledDropoffTime: '',
  passengerCount: 1,
  totalAmount: 0,
  status: '待處理',
  note: ''
})

const fetchRates = async () => {
  try {
    const response = await axios.get('/api/transferRate')
    rates.value = response.data
  } catch (error) {
    console.error('無法取得費率列表:', error)
  }
}

onMounted(() => {
  fetchRates()
})

const submitForm = async () => {
  try {
    await axios.post('http://localhost:8081/api/transferOrder', order.value)
    alert('新增成功')
    router.push('/transferOrder/search')
  } catch (error) {
    console.error(error)
    alert('新增失敗')
  }
}
</script> -->

<template>
  <div class="container-fluid py-4">
    <div class="card border-0 shadow-sm mb-4 bg-primary bg-gradient text-white rounded-4">
      <div class="card-body p-4">
        <h2 class="fw-bold mb-1"><i class="fa-solid fa-van-shuttle me-2"></i>新增訂單</h2>
        <p class="mb-0 text-white-50">Create New Transfer Order</p>
      </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <form @submit.prevent="submitForm">
          <div class="row g-3">
            
            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">客戶編號 <span class="text-danger">*</span></label>
              <input v-model="order.custId" type="number" class="form-control" required placeholder="例如: 1" @blur="fetchCustomerData" />
              <!-- 顯示查詢結果提示 -->
              <span v-if="customerName" class="d-block mt-1 small" :class="customerName === '查無此客戶' ? 'text-danger' : 'text-success'">
                <i class="fa-solid" :class="customerName === '查無此客戶' ? 'fa-circle-xmark' : 'fa-circle-check'"></i>
                {{ customerName === '查無此客戶' ? '查無此客戶' : `客戶名稱: ${customerName}` }}
              </span>
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">聯絡電話 <span class="text-danger">*</span></label>
              <input v-model="order.custPhone" type="text" class="form-control" required placeholder="09XX-XXX-XXX" />
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
                  <option value="桃園機場第一航廈T1">桃園機場 第一航廈T1</option>
                  <option value="桃園機場第二航廈T2">桃園機場 第二航廈T2</option>
                  <option value="桃園機場第三航廈T3">桃園機場 第三航廈T3</option>
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
                  <option value="桃園機場第一航廈T1">桃園機場 第一航廈T1</option>
                  <option value="桃園機場第二航廈T2">桃園機場 第二航廈T2</option>
                  <option value="桃園機場第三航廈T3">桃園機場 第三航廈T3</option>
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

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">預計上車時間</label>
              <input v-model="order.scheduledPickupTime" type="datetime-local" class="form-control" />
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">預計下車時間</label>
              <input v-model="order.scheduledDropoffTime" type="datetime-local" class="form-control" />
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
            <button type="submit" class="btn btn-primary px-4">送出儲存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>



<style scoped>

</style>
