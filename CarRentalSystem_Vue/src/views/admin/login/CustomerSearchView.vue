<script>
import { customerAPI } from '../../../api/login/customerAPI.js'

export default {
  name: 'CustomerSearchView',
  data() {
    return {
      keyword: '',
      customers: [],
      searched: false,
      successMsg: ''
    }
  },
  methods: {
    async handleSearch() {
      try {
        const res = await customerAPI.search(this.keyword)
        this.customers = res.data
        this.searched = true
      } catch (err) {
        console.error('查詢失敗', err)
      }
    },

    clearSearch() {
      this.keyword = ''
      this.customers = []
      this.searched = false
    },

    async handleDelete(custId, custName) {
      if (!confirm(`您確定要將「${custName}」的帳號停權嗎？`)) {
        return
      }
      try {
        const res = await customerAPI.delete(custId)
        this.successMsg = res.data.message
        await this.handleSearch()
        setTimeout(() => { this.successMsg = '' }, 3000)
      } catch (err) {
        alert('操作失敗：' + (err.response?.data?.message || err.message))
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
        <font-awesome-icon :icon="['fas', 'magnifying-glass']" class="me-2" />
        查詢客戶資料
      </h2>
      <router-link to="/customers/add" class="btn btn-primary">
        <font-awesome-icon :icon="['fas', 'plus']" class="me-1" />
        新增客戶
      </router-link>
    </div>

    <!-- ── 搜尋列 ── -->
    <div class="card mb-4">
      <div class="card-body py-3">
        <form @submit.prevent="handleSearch" class="input-group" style="max-width: 480px;">
          <input
            type="text"
            class="form-control"
            v-model="keyword"
            placeholder="輸入關鍵字搜尋（姓名、電話、帳號、地址...）"
            id="search-keyword"
          />
          <button type="submit" class="btn btn-primary" id="btn-search">
            <font-awesome-icon :icon="['fas', 'magnifying-glass']" class="me-1" />
            搜尋
          </button>
          <button type="button" class="btn btn-outline-secondary" @click="clearSearch" id="btn-clear">
            <font-awesome-icon :icon="['fas', 'xmark']" class="me-1" />
            清除
          </button>
        </form>
      </div>
    </div>

    <!-- ── 提示訊息 ── -->
    <div v-if="successMsg" class="alert alert-success mb-3">
      <font-awesome-icon :icon="['fas', 'circle-check']" class="me-1" />
      {{ successMsg }}
    </div>

    <!-- ── 搜尋結果表格 ── -->
    <div v-if="searched" class="card">
      <div class="card-body p-0">

        <div v-if="customers.length > 0" class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>編號</th>
                <th>姓名</th>
                <th>電話</th>
                <th>E-mail</th>
                <th>帳號</th>
                <th>地址</th>
                <th>駕照</th>
                <th>駕照到期日</th>
                <th>狀態</th>
                <th>剩餘點數</th>
                <th>歷史點數</th>
                <th class="text-center">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cust in customers" :key="cust.custId">
                <td class="text-secondary">{{ cust.custId }}</td>
                <td class="fw-medium">{{ cust.custName }}</td>
                <td>{{ cust.custPhone }}</td>
                <td>{{ cust.custEmail }}</td>
                <td><code>{{ cust.custAccount }}</code></td>
                <td>{{ cust.custAddress }}</td>
                <td>{{ cust.custLicense }}</td>
                <td>{{ cust.custLicenseExpiry }}</td>
                <td>
                  <span class="badge" :class="cust.custStatus === '啟用' ? 'bg-success' : 'bg-gray'">
                    {{ cust.custStatus }}
                  </span>
                </td>
                <td>{{ cust.currentPoints }}</td>
                <td>{{ cust.totalAccumulated }}</td>
                <td class="text-center">
                  <router-link
                    :to="`/customers/${cust.custId}/edit`"
                    class="btn btn-outline-primary btn-sm me-1"
                  >
                    <font-awesome-icon :icon="['fas', 'pen']" class="me-1" />
                    編輯
                  </router-link>
                  <button
                    class="btn btn-outline-danger btn-sm"
                    @click="handleDelete(cust.custId, cust.custName)"
                  >
                    <font-awesome-icon :icon="['fas', 'ban']" class="me-1" />
                    停權
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="text-center py-5">
          <font-awesome-icon :icon="['fas', 'magnifying-glass']" style="font-size:2.5rem" class="text-secondary" />
          <p class="text-secondary mt-2">找不到符合條件的客戶資料</p>
        </div>

      </div>
    </div>

  </div>
</template>

<style scoped>
code {
  font-size: 0.875em;
}
</style>
