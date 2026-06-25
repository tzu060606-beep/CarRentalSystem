<script setup>
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import TermsPage from './TermsPage.vue'
import Step1BaseInfo from './Step1BaseInfo.vue'
import Step2BookingInfo from './Step2BookingInfo.vue'
import CharterRequestView from './CharterRequestView.vue'

const router = useRouter()

const props = defineProps({
  defaultPage: {
    type: String,
    default: 'terms'
  }
})

// 狀態機：控制目前顯示的頁面
const currentPage = ref(props.defaultPage)

// 共享的預約資料
const bookingData = reactive({
  custId: null,
  custName: '',
  email: '',
  custPhone: '',
  phone2: '',
  otherPhone: '',
  transferType: '' // 送機 / 接機
})

watch(() => props.defaultPage, (newVal) => {
  currentPage.value = newVal
})

const navigate = (page) => {
  if (page === 'landing') {
    router.push('/transfer')
    return
  }
  currentPage.value = page
  // 切換頁面時捲動到最上方
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<template>
  <div>
    <TermsPage v-if="currentPage === 'terms'" :bookingData="bookingData" @navigate="navigate" />
    <Step1BaseInfo v-else-if="currentPage === 'step1'" :bookingData="bookingData" @navigate="navigate" />
    <Step2BookingInfo v-else-if="currentPage === 'step2'" :bookingData="bookingData" @navigate="navigate" />
    <CharterRequestView v-else-if="currentPage === 'charterRequest'" @navigate="navigate" />
  </div>
</template>

<style scoped>
</style>
