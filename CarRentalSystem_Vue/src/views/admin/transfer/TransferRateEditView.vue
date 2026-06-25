<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/api/index';

const router = useRouter()
const route = useRoute()
const rateId = route.params.id

const rate = ref({
  rateName: '',
  baseFee: 0,
  perKmFee: 0,
  vehicleType: '',
  isActive: true
})

const fetchRate = async () => {
  try {
    const response = await request.get(`/transferRate/${rateId}`)
    rate.value = response.data
  } catch (error) {
    console.error(error)
  }
}

const submitForm = async () => {
  try {
    await request.put(`/transferRate/${rateId}`, rate.value)
    alert('修改成功')
    router.push('/transferRate')
  } catch (error) {
    console.error(error)
    alert('修改失敗')
  }
}

onMounted(() => {
  fetchRate()
})
</script>
<!-- <script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const rateId = route.params.id

const rate = ref({
  rateName: '',
  baseFee: 0,
  perKmFee: 0,
  vehicleType: '',
  isActive: true
})

const fetchRate = async () => {
  try {
    const response = await axios.get(`http://localhost:8081/api/transferRate/${rateId}`)
    rate.value = response.data
  } catch (error) {
    console.error(error)
  }
}

const submitForm = async () => {
  try {
    await axios.put(`http://localhost:8081/api/transferRate/${rateId}`, rate.value)
    alert('修改成功')
    router.push('/transferRate')
  } catch (error) {
    console.error(error)
    alert('修改失敗')
  }
}

onMounted(() => {
  fetchRate()
})
</script> -->

<template>
  <div class="container-fluid py-4">
    <div class="card border-0 shadow-sm mb-4 bg-primary bg-gradient text-white rounded-4">
      <div class="card-body p-4">
        <h2 class="fw-bold mb-1"><i class="fa-solid fa-pen-to-square me-2"></i>修改費率</h2>
        <p class="mb-0 text-white-50">Edit Transfer Rate</p>
      </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <form @submit.prevent="submitForm">
          <div class="row g-3">
            
            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">費率名稱 <span class="text-danger">*</span></label>
              <input v-model="rate.rateName" type="text" class="form-control" required placeholder="例如: 桃園機場接送-一般轎車" />
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">車輛類型</label>
              <input v-model="rate.vehicleType" type="text" class="form-control" placeholder="例如: 轎車 / 休旅車" />
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">基本費用 <span class="text-danger">*</span></label>
              <div class="input-group">
                <span class="input-group-text">$</span>
                <input v-model="rate.baseFee" type="number" step="0.01" class="form-control" required placeholder="例如: 1000" />
              </div>
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted small fw-bold mb-1">每公里費用 <span class="text-danger">*</span></label>
              <div class="input-group">
                <span class="input-group-text">$</span>
                <input v-model="rate.perKmFee" type="number" step="0.01" class="form-control" required placeholder="例如: 20" />
              </div>
            </div>

            <div class="col-12 mt-4">
              <div class="form-check form-switch">
                <input v-model="rate.isActive" class="form-check-input" type="checkbox" id="isActiveCheck">
                <label class="form-check-label text-muted small fw-bold" for="isActiveCheck">
                  是否啟用此費率
                </label>
              </div>
            </div>

          </div>

          <div class="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
            <button type="button" class="btn btn-light" @click="$router.push('/transferRate')">取消返回</button>
            <button type="submit" class="btn btn-primary px-4">儲存變更</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* label {
  display: inline-block;
  width: 100px;
  margin-bottom: 5px;
} */
</style>
