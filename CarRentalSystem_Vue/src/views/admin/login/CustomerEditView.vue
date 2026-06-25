<script>
import { customerAPI } from '../../../api/login/customerAPI.js'

export default {
  name: 'CustomerEditView',
  data() {
    return {
      form: {
        custId: null,
        custName: '',
        custPhone: '',
        custEmail: '',
        custAccount: '',
        custAddress: '',
        custLicense: '',
        custLicenseExpiry: '',
        custStatus: '啟用',
        currentPoints: 0,
        totalAccumulated: 0
      },
      errorMsg: '',
      licenseError: '',
      loading: true,
      submitting: false
    }
  },
  async created() {
    const custId = this.$route.params.id
    try {
      const res = await customerAPI.getById(custId)
      this.form = res.data
    } catch (err) {
      this.errorMsg = '載入客戶資料失敗'
    } finally {
      this.loading = false
    }
  },
  methods: {
    validateLicense() {
      const rule = /^[A-Z][0-9]{9}$/
      if (this.form.custLicense && !rule.test(this.form.custLicense)) {
        this.licenseError = '駕照格式錯誤，應為一個大寫字母加上9個數字'
      } else {
        this.licenseError = ''
      }
    },

    async handleSubmit() {
      this.errorMsg = ''
      this.validateLicense()
      if (this.licenseError) return

      this.submitting = true
      try {
        const res = await customerAPI.update(this.form.custId, this.form)
        if (res.data.success) {
          this.$router.push({ path: '/customers', query: { msg: res.data.message } })
        }
      } catch (err) {
        if (err.response && err.response.data) {
          this.errorMsg = err.response.data.message
        } else {
          this.errorMsg = '修改失敗，請稍後再試'
        }
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<template>
  <div>

    <!-- ── 頁面標題 ── -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="mb-0">
        <font-awesome-icon :icon="['fas', 'pen']" class="me-2" />
        修改客戶資料
      </h2>
    </div>

    <!-- 載入中 -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">載入中...</span>
      </div>
      <p class="mt-2 text-secondary">載入中...</p>
    </div>

    <div v-else class="card">
      <div class="card-body">

        <!-- 錯誤訊息 -->
        <div v-if="errorMsg" class="alert alert-danger py-2 mb-3">
          <font-awesome-icon :icon="['fas', 'circle-xmark']" class="me-1" />
          {{ errorMsg }}
        </div>

        <form @submit.prevent="handleSubmit">
          <div class="row g-3">

            <div class="col-md-6">
              <label class="form-label">編號</label>
              <input type="text" class="form-control" :value="form.custId" readonly id="input-custId" />
            </div>

            <div class="col-md-6">
              <label class="form-label">帳號</label>
              <input type="text" class="form-control" :value="form.custAccount" readonly id="input-custAccount" />
              <div class="form-text">帳號建立後無法修改</div>
            </div>

            <div class="col-md-6">
              <label class="form-label">姓名</label>
              <input type="text" class="form-control" v-model="form.custName" id="input-custName" />
            </div>

            <div class="col-md-6">
              <label class="form-label">電話</label>
              <input type="text" class="form-control" v-model="form.custPhone" id="input-custPhone" />
            </div>

            <div class="col-md-6">
              <label class="form-label">E-mail</label>
              <input type="text" class="form-control" v-model="form.custEmail" id="input-custEmail" />
            </div>

            <div class="col-md-6">
              <label class="form-label">地址</label>
              <input type="text" class="form-control" v-model="form.custAddress" id="input-custAddress" />
            </div>

            <div class="col-md-6">
              <label class="form-label">駕照字號</label>
              <input
                type="text"
                class="form-control"
                v-model="form.custLicense"
                @blur="validateLicense"
                id="input-custLicense"
              />
              <div v-if="licenseError" class="text-danger small mt-1">
                <font-awesome-icon :icon="['fas', 'circle-xmark']" class="me-1" />
                {{ licenseError }}
              </div>
            </div>

            <div class="col-md-6">
              <label class="form-label">駕照到期日</label>
              <input type="date" class="form-control" v-model="form.custLicenseExpiry" id="input-custLicenseExpiry" />
            </div>

            <div class="col-md-6">
              <label class="form-label">使用狀態</label>
              <select class="form-select" v-model="form.custStatus" id="select-custStatus">
                <option value="啟用">啟用</option>
                <option value="停權">停權</option>
              </select>
            </div>

            <div class="col-md-6">
              <label class="form-label">剩餘點數</label>
              <input type="number" class="form-control bg-light" :value="form.currentPoints" readonly id="input-currentPoints" />
              <div class="form-text">點數由系統自動管理，不可手動修改</div>
            </div>

            <div class="col-md-6">
              <label class="form-label">歷史點數總計</label>
              <input type="number" class="form-control bg-light" :value="form.totalAccumulated" readonly id="input-totalAccumulated" />
              <div class="form-text">歷史累積點數由系統自動計算</div>
            </div>

          </div>

          <!-- 操作按鈕 -->
          <div class="d-flex gap-2 mt-4">
            <button
              type="submit"
              class="btn btn-success"
              :disabled="submitting || !!licenseError"
              id="btn-submit"
            >
              <font-awesome-icon v-if="!submitting" :icon="['fas', 'floppy-disk']" class="me-1" />
              <span v-if="submitting" class="spinner-border spinner-border-sm me-1" role="status"></span>
              {{ submitting ? '修改中...' : '確認修改' }}
            </button>

            <router-link to="/customers/search" class="btn btn-outline-secondary">
              <font-awesome-icon :icon="['fas', 'arrow-left']" class="me-1" />
              前往查詢
            </router-link>
          </div>

        </form>
      </div>
    </div>

  </div>
</template>

<style scoped>
</style>
