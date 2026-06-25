<script>
import { customerAPI } from '../../../api/login/customerAPI.js'

export default {
  name: 'CustomerListView',
  data() {
    return {
      customers: [],
      loading: true,
      successMsg: ''
    }
  },
  async created() {
    await this.fetchCustomers()

    // 檢查是否有從其他頁面帶來的成功訊息
    if (this.$route.query.msg) {
      this.successMsg = this.$route.query.msg
      setTimeout(() => { this.successMsg = '' }, 3000)
    }
  },
  methods: {
    async fetchCustomers() {
      this.loading = true
      try {
        const res = await customerAPI.getAll()
        this.customers = res.data
      } catch (err) {
        console.error('載入客戶資料失敗', err)
      } finally {
        this.loading = false
      }
    },
    async handleDelete(custId, custName) {
      // 確認停權
      if (!confirm(`您確定要將「${custName}」的帳號停權嗎？`)) {
        return
      }
      try {
        const res = await customerAPI.delete(custId)
        this.successMsg = res.data.message
        await this.fetchCustomers() // 重新載入列表
        setTimeout(() => { this.successMsg = '' }, 3000)
      } catch (err) {
        alert('刪除失敗：' + (err.response?.data?.message || err.message))
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
        <font-awesome-icon :icon="['fas', 'users']" class="me-2" />
        全部客戶資料
      </h2>
      <router-link to="/customers/add" class="btn btn-primary" id="btn-add-customer">
        <font-awesome-icon :icon="['fas', 'plus']" class="me-1" />
        新增客戶
      </router-link>
    </div>

    <!-- ── 提示訊息 ── -->
    <div v-if="successMsg" class="alert alert-success mb-3">
      <font-awesome-icon :icon="['fas', 'circle-check']" class="me-1" />
      {{ successMsg }}
    </div>

    <!-- ── 載入中 ── -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">載入中...</span>
      </div>
      <p class="mt-2 text-secondary">載入中...</p>
    </div>

    <!-- ── 資料表格 ── -->
    <div v-else class="card">
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
          <font-awesome-icon :icon="['fas', 'clipboard-list']" style="font-size:2.5rem" class="text-secondary" />
          <p class="text-secondary mt-2">目前沒有客戶資料</p>
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

