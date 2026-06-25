<script>
import { customerAPI } from '../../../api/login/customerAPI.js'

export default {
  name: 'CustomerAddView',
  data() {
    return {
      form: {
        custName: '',
        custPhone: '',
        custEmail: '',
        custAccount: '',
        custAddress: '',
        custLicense: '',
        custLicenseExpiry: '',
        custStatus: '啟用'
      },
      errorMsg: '',
      licenseError: '',
      loading: false
    }
  },
  methods: {
    validateLicense() {
      const rule = /^[A-Z][0-9]{9}$/
      if (this.form.custLicense && !rule.test(this.form.custLicense)) {
        this.licenseError = '格式錯誤，應為一個大寫字母加上9個數字'
      } else {
        this.licenseError = ''
      }
    },

    async handleSubmit() {
      this.errorMsg = ''
      this.validateLicense()
      if (this.licenseError) return

      this.loading = true
      try { 
        const res = await customerAPI.create(this.form)
        if (res.data.success) {
          this.$router.push({ path: '/customers', query: { msg: res.data.message } })
        }
      } catch (err) {
        if (err.response && err.response.data) {
          this.errorMsg = err.response.data.message
        } else {
          this.errorMsg = '新增失敗，請稍後再試'
        }
      } finally {
        this.loading = false
      }
    },

    resetForm() {
      this.form = {
        custName: '', custPhone: '', custEmail: '', custAccount: '',
        custAddress: '', custLicense: '', custLicenseExpiry: '', custStatus: '啟用'
      }
      this.errorMsg = ''
      this.licenseError = ''
    },

    fillTestData() {
      const rand = Math.floor(Math.random() * 900) + 100
      this.form = {
        custName: `測試用戶${rand}`,
        custPhone: `09${rand}000111`,
        custEmail: `test${rand}@example.com`,
        custAccount: `testuser${rand}`,
        custAddress: `台北市中正區測試路${rand}號`,
        custLicense: `A${rand}000000`,
        custLicenseExpiry: '2030-12-31',
        custStatus: '啟用'
      }
      this.errorMsg = ''
      this.licenseError = ''
    }
  }
}
</script>

<template>
  <div>

    <!-- ── 頁面標題 ── -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="mb-0">
        <font-awesome-icon :icon="['fas', 'user-plus']" class="me-2" />
        新增客戶資料
      </h2>
    </div>

    <div class="card">
      <div class="card-body">

        <!-- 錯誤訊息 -->
        <div v-if="errorMsg" class="alert alert-danger py-2 mb-3">
          <font-awesome-icon :icon="['fas', 'circle-xmark']" class="me-1" />
          {{ errorMsg }}
        </div>

        <form @submit.prevent="handleSubmit">
          <div class="row g-3">

            <div class="col-md-6">
              <label class="form-label">客戶姓名 <span class="text-danger">*</span></label>
              <input type="text" class="form-control" v-model="form.custName" required id="input-custName" placeholder="例：王小明" />
            </div>

            <div class="col-md-6">
              <label class="form-label">手機號碼 <span class="text-danger">*</span></label>
              <input type="text" class="form-control" v-model="form.custPhone" required id="input-custPhone" placeholder="例：0912-345-678" />
            </div>

            <div class="col-md-6">
              <label class="form-label">電子郵件</label>
              <input type="email" class="form-control" v-model="form.custEmail" id="input-custEmail" placeholder="例：wang@gmail.com" />
            </div>

            <div class="col-md-6">
              <label class="form-label">登入帳號 <span class="text-danger">*</span></label>
              <input type="text" class="form-control" v-model="form.custAccount" required id="input-custAccount" placeholder="例：wang001" />
            </div>

            <div class="col-12">
              <label class="form-label">通訊地址</label>
              <input type="text" class="form-control" v-model="form.custAddress" id="input-custAddress" placeholder="例：台北市中正區..." />
            </div>

            <div class="col-md-6">
              <label class="form-label">駕照字號 <span class="text-danger">*</span></label>
              <input
                type="text"
                class="form-control"
                v-model="form.custLicense"
                required
                @blur="validateLicense"
                id="input-custLicense"
                placeholder="例：A123456789"
              />
              <div v-if="licenseError" class="text-danger small mt-1">
                <font-awesome-icon :icon="['fas', 'circle-xmark']" class="me-1" />
                {{ licenseError }}
              </div>
            </div>

            <div class="col-md-6">
              <label class="form-label">駕照到期日 <span class="text-danger">*</span></label>
              <input type="date" class="form-control" v-model="form.custLicenseExpiry" required id="input-custLicenseExpiry" />
            </div>

            <div class="col-md-6">
              <label class="form-label">帳號狀態</label>
              <select class="form-select" v-model="form.custStatus" id="select-custStatus">
                <option value="啟用">啟用</option>
                <option value="停權">停權</option>
              </select>
            </div>

          </div>

          <!-- 操作按鈕 -->
          <div class="d-flex gap-2 mt-4">
            <button
              type="submit"
              class="btn btn-success"
              :disabled="loading || !!licenseError"
              id="btn-submit"
            >
              <font-awesome-icon v-if="!loading" :icon="['fas', 'check']" class="me-1" />
              <span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status"></span>
              {{ loading ? '新增中...' : '確認新增' }}
            </button>

            <button type="button" class="btn btn-outline-secondary" @click="resetForm" id="btn-reset">
              <font-awesome-icon :icon="['fas', 'rotate-left']" class="me-1" />
              清除重填
            </button>

            <button type="button" class="btn btn-outline-warning" @click="fillTestData" id="btn-fill-test">
              <font-awesome-icon :icon="['fas', 'wand-magic-sparkles']" class="me-1" />
              一鍵帶入
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
