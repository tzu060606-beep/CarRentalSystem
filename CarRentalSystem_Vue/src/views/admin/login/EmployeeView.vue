<script setup>
// ============================================================
//  員工管理頁面（查詢 + 新增 + 編輯 + 刪除）
// ============================================================
import { ref, onMounted, nextTick } from 'vue'
import { employeeAPI } from '@/api/login/employeeApi.js'

// ─── 資料狀態 ────────────────────────────────────────────────
const employees   = ref([])
const searchQuery = ref('')
const loading     = ref(false)
const successMsg  = ref('')
const errorMsg    = ref('')

// ─── Modal 狀態 ───────────────────────────────────────────────
const isEditing   = ref(false)
const submitting  = ref(false)

// ─── Bootstrap Modal 實例 ─────────────────────────────────────
// 用 ref 指向 DOM 元素，再用 window.bootstrap.Modal 建立實例
const modalEl     = ref(null)   // 綁定 <div id="employeeModal">
let   bsModal     = null        // Bootstrap Modal 物件

// ─── 表單資料 ─────────────────────────────────────────────────
const defaultForm = () => ({
  empId:      null,
  empName:    '',
  empAccount: '',
  empPassword:'',
  empRole:    'USER',
  empPhone:   '',
  empEmail:   '',
  empStatus:  '啟用'   // 資料庫欄位存的是中文字
})
const formData = ref(defaultForm())

// ─── 訊息工具 ─────────────────────────────────────────────────
const showSuccess = (msg) => {
  successMsg.value = msg
  errorMsg.value   = ''
  setTimeout(() => { successMsg.value = '' }, 3000)
}
const showError = (msg) => {
  errorMsg.value   = msg
  successMsg.value = ''
}

// ─── 初始化 Bootstrap Modal 實例 ─────────────────────────────
// nextTick 確保 DOM 已掛載後才初始化 Bootstrap Modal
onMounted(async () => {
  await nextTick()
  // window.bootstrap 由 main.js 掛載：import * as bootstrap from 'bootstrap'; window.bootstrap = bootstrap
  bsModal = new window.bootstrap.Modal(modalEl.value, {
    backdrop: 'static',   // 點背景遮罩不會關閉（防止誤觸）
    keyboard: true        // 按 ESC 可關閉
  })
  fetchEmployees()
})

// ─── 查詢員工 ─────────────────────────────────────────────────
const fetchEmployees = async () => {
  loading.value = true
  try {
    const res = searchQuery.value.trim()
      ? await employeeAPI.search(searchQuery.value)
      : await employeeAPI.getAll()
    employees.value = res.data
  } catch (err) {
    showError('載入員工資料失敗，請確認是否已登入')
    console.error(err)
  } finally {
    loading.value = false
  }
}

// ─── 開啟「新增」Modal ───────────────────────────────────────
const openAddModal = () => {
  isEditing.value = false
  formData.value  = defaultForm()
  errorMsg.value  = ''
  bsModal.show()          // 用 Bootstrap JS 打開 Modal
}

// ─── 開啟「編輯」Modal ───────────────────────────────────────
const openEditModal = (emp) => {
  isEditing.value = true
  formData.value  = { ...emp, empPassword: '' }   // 複製一份，密碼欄位留空
  errorMsg.value  = ''
  bsModal.show()          // 用 Bootstrap JS 打開 Modal
}

// ─── 關閉 Modal ───────────────────────────────────────────────
const closeModal = () => {
  bsModal.hide()          // 用 Bootstrap JS 關閉 Modal
}

// ─── 儲存（新增 / 修改）─────────────────────────────────────
const saveEmployee = async () => {
  submitting.value = true
  errorMsg.value   = ''
  try {
    if (isEditing.value) {
      await employeeAPI.update(formData.value.empId, formData.value)
      closeModal()
      showSuccess(`員工「${formData.value.empName}」修改成功！`)
    } else {
      // 密碼留空時，預設為手機號碼
      if (!formData.value.empPassword) {
        formData.value.empPassword = formData.value.empPhone
      }
      await employeeAPI.create(formData.value)
      closeModal()
      showSuccess('新增員工成功！')
    }
    fetchEmployees()
  } catch (err) {
    // 錯誤訊息顯示在 Modal 內部，不關閉 Modal
    const msg = err.response?.data?.message || '操作失敗，請檢查輸入資料'
    showError(msg)
    console.error(err)
  } finally {
    submitting.value = false
  }
}

// ─── 刪除（軟刪除）──────────────────────────────────────────
const handleDelete = async (empId, empName) => {
  if (!confirm(`您確定要停用「${empName}」的帳號嗎？`)) return
  try {
    await employeeAPI.delete(empId)
    showSuccess(`員工「${empName}」已停用`)
    fetchEmployees()
  } catch (err) {
    showError('操作失敗：' + (err.response?.data?.message || err.message))
  }
}

// ─── 輔助：角色 / 狀態的 Badge 顯示 ─────────────────────────
const roleLabel = (role) => ({ ADMIN: '管理員', USER: '員工', DRIVER: '司機' }[role] || role)
const roleBadge = (role) => ({ ADMIN: 'bg-primary', USER: 'bg-success', DRIVER: 'bg-info' }[role] || 'bg-secondary')
const statusLabel = (s) => s === '啟用' ? '啟用' : '停用'
const statusBadge = (s) => s === '啟用' ? 'bg-success' : 'bg-gray'
</script>

<template>
  <div>

    <!-- ── 頁面標題 ── -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="mb-0">
        <font-awesome-icon :icon="['fas', 'users']" class="me-2" />
        員工管理
      </h2>
      <button class="btn btn-primary" @click="openAddModal" id="btn-add-employee">
        <font-awesome-icon :icon="['fas', 'plus']" class="me-1" />
        新增員工
      </button>
    </div>

    <!-- ── 全域提示訊息 ── -->
    <div v-if="successMsg" class="alert alert-success mb-3">
      <font-awesome-icon :icon="['fas', 'circle-check']" class="me-1" />
      {{ successMsg }}
    </div>
    <div v-if="errorMsg && !bsModal?.show" class="alert alert-danger mb-3">
      <font-awesome-icon :icon="['fas', 'circle-xmark']" class="me-1" />
      {{ errorMsg }}
    </div>

    <!-- ── 搜尋列 ── -->
    <div class="card mb-4">
      <div class="card-body py-3">
        <div class="input-group" style="max-width: 480px;">
          <input
            type="text"
            class="form-control"
            v-model="searchQuery"
            placeholder="搜尋姓名、帳號、電話、Email..."
            id="input-search"
            @keyup.enter="fetchEmployees"
          />
          <button class="btn btn-primary" @click="fetchEmployees" id="btn-search">
            <font-awesome-icon :icon="['fas', 'magnifying-glass']" class="me-1" />
            搜尋
          </button>
          <button
            class="btn btn-outline-secondary"
            @click="searchQuery = ''; fetchEmployees()"
            id="btn-reset-search"
          >
            <font-awesome-icon :icon="['fas', 'xmark']" class="me-1" />
            清除
          </button>
        </div>
      </div>
    </div>

    <!-- ── 資料表格 ── -->
    <div class="card">
      <div class="card-body p-0">

        <!-- 載入中 -->
        <div v-if="loading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">載入中...</span>
          </div>
          <p class="mt-2 text-secondary">載入中...</p>
        </div>

        <!-- 無資料 -->
        <div v-else-if="employees.length === 0" class="text-center py-5">
          <font-awesome-icon :icon="['fas', 'clipboard-list']" style="font-size:2.5rem" class="text-secondary" />
          <p class="text-secondary mt-2">目前沒有符合的員工資料</p>
        </div>

        <!-- 表格 -->
        <div v-else class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>編號</th>
                <th>姓名</th>
                <th>帳號</th>
                <th>角色</th>
                <th>電話</th>
                <th>Email</th>
                <th>狀態</th>
                <th class="text-center">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="emp in employees" :key="emp.empId">
                <td class="text-secondary">{{ emp.empId }}</td>
                <td class="fw-medium">{{ emp.empName }}</td>
                <td><code>{{ emp.empAccount }}</code></td>
                <td>
                  <span class="badge" :class="roleBadge(emp.empRole)">
                    {{ roleLabel(emp.empRole) }}
                  </span>
                </td>
                <td>{{ emp.empPhone }}</td>
                <td>{{ emp.empEmail }}</td>
                <td>
                  <span class="badge" :class="statusBadge(emp.empStatus)">
                    {{ statusLabel(emp.empStatus) }}
                  </span>
                </td>
                <td class="text-center">
                  <button
                    class="btn btn-outline-primary btn-sm me-1"
                    @click="openEditModal(emp)"
                    :id="`btn-edit-${emp.empId}`"
                  >
                    <font-awesome-icon :icon="['fas', 'pen']" class="me-1" />
                    編輯
                  </button>
                  <button
                    class="btn btn-outline-danger btn-sm"
                    @click="handleDelete(emp.empId, emp.empName)"
                    :id="`btn-delete-${emp.empId}`"
                  >
                    <font-awesome-icon :icon="['fas', 'ban']" class="me-1" />
                    停用
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         Bootstrap Modal (用 ref + window.bootstrap.Modal 控制)
         ref="modalEl" 讓 Vue 拿到這個 DOM 元素給 Bootstrap 使用
         ══════════════════════════════════════════════════════ -->
    <div class="modal fade" id="employeeModal" tabindex="-1"
         aria-labelledby="employeeModalLabel" aria-hidden="true"
         ref="modalEl">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">

          <!-- Modal 標頭 -->
          <div class="modal-header">
            <h5 class="modal-title" id="employeeModalLabel">
              <font-awesome-icon :icon="isEditing ? ['fas', 'pen'] : ['fas', 'plus']" class="me-2" />
              {{ isEditing ? '修改員工資料' : '新增員工' }}
            </h5>
            <button type="button" class="btn-close" @click="closeModal" id="btn-modal-close"></button>
          </div>

          <!-- Modal 主體 -->
          <div class="modal-body">
            <!-- 錯誤訊息顯示在 Modal 內 -->
            <div v-if="errorMsg" class="alert alert-danger py-2 mb-3">
              <font-awesome-icon :icon="['fas', 'circle-xmark']" class="me-1" />
              {{ errorMsg }}
            </div>

            <form @submit.prevent="saveEmployee" id="form-employee">
              <div class="row g-3">

                <!-- 姓名 -->
                <div class="col-md-6">
                  <label class="form-label">姓名 <span class="text-danger">*</span></label>
                  <input
                    type="text" class="form-control"
                    v-model="formData.empName"
                    required placeholder="例：王大明"
                    id="input-empName"
                  />
                </div>

                <!-- 帳號 -->
                <div class="col-md-6">
                  <label class="form-label">帳號 <span class="text-danger">*</span></label>
                  <input
                    type="text" class="form-control"
                    v-model="formData.empAccount"
                    required placeholder="例：admin001"
                    :disabled="isEditing"
                    id="input-empAccount"
                  />
                  <div v-if="isEditing" class="form-text">帳號建立後無法修改</div>
                </div>

                <!-- 密碼 -->
                <div class="col-md-6">
                  <label class="form-label">
                    密碼
                    <span v-if="!isEditing" class="text-danger">*</span>
                    <span v-else class="text-secondary small">（留空表示不修改）</span>
                  </label>
                  <input
                    type="password" class="form-control"
                    v-model="formData.empPassword"
                    :required="!isEditing"
                    placeholder="留空則預設為手機號碼"
                    id="input-empPassword"
                  />
                </div>

                <!-- 角色 -->
                <div class="col-md-6">
                  <label class="form-label">角色 <span class="text-danger">*</span></label>
                  <select class="form-select" v-model="formData.empRole" required id="select-empRole">
                    <option value="ADMIN">管理員</option>
                    <option value="USER">員工</option>
                    <option value="DRIVER">司機</option>
                  </select>
                </div>

                <!-- 電話 -->
                <div class="col-md-6">
                  <label class="form-label">電話</label>
                  <input
                    type="text" class="form-control"
                    v-model="formData.empPhone"
                    placeholder="例：0912-345-678"
                    id="input-empPhone"
                  />
                </div>

                <!-- Email -->
                <div class="col-md-6">
                  <label class="form-label">Email</label>
                  <input
                    type="email" class="form-control"
                    v-model="formData.empEmail"
                    placeholder="例：wang@car.com"
                    id="input-empEmail"
                  />
                </div>

                <!-- 狀態（修改模式才顯示） -->
                <div v-if="isEditing" class="col-md-6">
                  <label class="form-label">帳號狀態</label>
                  <select class="form-select" v-model="formData.empStatus" id="select-empStatus">
                    <option value="啟用">啟用</option>
                    <option value="停用">停用</option>
                  </select>
                </div>

              </div>
            </form>
          </div>

          <!-- Modal 頁尾 -->
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" @click="closeModal" id="btn-cancel">
              <font-awesome-icon :icon="['fas', 'xmark']" class="me-1" />
              取消
            </button>
            <button
              type="submit" form="form-employee"
              class="btn btn-primary"
              :disabled="submitting"
              id="btn-save"
            >
              <font-awesome-icon v-if="!submitting" :icon="isEditing ? ['fas', 'floppy-disk'] : ['fas', 'check']" class="me-1" />
              <span v-if="submitting" class="spinner-border spinner-border-sm me-1" role="status"></span>
              {{ submitting ? '儲存中...' : (isEditing ? '儲存修改' : '確認新增') }}
            </button>
          </div>

        </div>
      </div>
    </div>
    <!-- ── Modal 結束 ── -->

  </div>
</template>

<style scoped>
/* 表格內的 code 標籤讓帳號更顯眼 */
code {
  font-size: 0.875em;
}
</style>
