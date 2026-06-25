<script setup>
import { ref, onMounted } from 'vue';
// 引入你原本寫好的 API (如果沒有 delete 方法，稍後可以在 api/rentalplan 裡補上)
import { rentalPlansAPI } from '@/api/rentalorder/rentalplan';
import Breadcrumb from '@/components/common/Breadcrumb.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';

const plans = ref([]);
const isLoading = ref(false);

// --- Modal 相關狀態與方法 ---
const showModal = ref(false);
const selectedPlan = ref(null);

// ---準備另一個跳出視窗，是要給新增和修改使用
const showFormModal = ref(false);
const isEditMode = ref(false);

const showDeleteConfirm = ref(false);
const pendingDeletePlanId = ref(null); 
      // true為編輯 false為新增模式

const formData = ref({

  
  planName: '',           
  planDesc: '',           
  isLongTerm: false, 
  appliedVehicleType:'',
  basePrice: 0,           
  overtimeFee: 0,         
  mileageLimit: null,    
  excessMileageFee: 0,    
  active: true            

// 注意欄位名稱要跟後端 DTO 的 field name 一致（駝峰式），
// 不然 axios 送出去後端會收不到。
})

const planActiveMap = {
  ACTIVE: { label: '上架中', variant: 'success' },
  INACTIVE: { label: '已下架', variant: 'secondary' },
};

// ----------------------------以下為函式---------------

const openDetails = (plan) => {
  selectedPlan.value = plan;
  showModal.value = true;
  document.body.style.overflow = 'hidden'; // 防止背景滾動
};

const closeDetails = () => {
  showModal.value = false;
  selectedPlan.value = null;
  document.body.style.overflow = ''; // 恢復背景滾動
};


// --------以上處理單筆查詢的show和close，以下處理編輯和新增

const openCreate = () =>{
//點點新增方案時觸發
  isEditMode.value = false;
  formData.value = {
    planName: '',
    planDesc: '',
    isLongTerm: false,
    appliedVehicleType:'',
    basePrice: 0,
    overtimeFee: 0,
    mileageLimit: null,
    excessMileageFee: 0,
    active: true
  };
  showFormModal.value = true;
  document.body.style.overflow = 'hidden'; // 防止背景滾動

}

const openEdit=(plan)=>{
                                                                                                                                                                       
  isEditMode.value = true;
  formData.value = {
    ...plan,
    isLongTerm: plan.isLongTerm ?? plan.longTerm ?? false
  };
  showFormModal.value = true;
  document.body.style.overflow = 'hidden'; // 防止背景滾動

}

const submitForm = async () => {
  try {
    const mileageLimit =
      formData.value.mileageLimit === '' ||
      formData.value.mileageLimit === null ||
      formData.value.mileageLimit === undefined
        ? null
        : Number(formData.value.mileageLimit);

    const payload = {
      planName: String(formData.value.planName || '').trim(),
      planDesc: String(formData.value.planDesc || '').trim(),
      isLongTerm: Boolean(formData.value.isLongTerm),
      appliedVehicleType: String(formData.value.appliedVehicleType || '').trim(),
      basePrice: Number(formData.value.basePrice || 0),
      overtimeFee: Number(formData.value.overtimeFee || 0),
      mileageLimit,
      excessMileageFee: Number(formData.value.excessMileageFee || 0),
      active: Boolean(formData.value.active),
    };

    console.log('送出的方案 payload:', payload);

    if (isEditMode.value) {
      await rentalPlansAPI.update(formData.value.planId, payload);
      alert('更新成功！');
    } else {
      await rentalPlansAPI.create(payload);
      alert('新增成功！');
    }

    showFormModal.value = false;
    document.body.style.overflow = '';
    fetchPlans();

  } catch (error) {
    // 4. 失敗時：捕捉錯誤並提示使用者
    console.error("表單送出失敗:", error);
    console.error("後端回傳:", error.response?.data);
    alert(error.response?.data?.error || '操作發生錯誤，請檢查資料或稍後再試！');
  }
};
const closeFormDetails = () => {
  showFormModal.value = false;
  document.body.style.overflow = ''; // 恢復背景滾動

  // 選項：重置表單資料，確保下次打開是乾淨的
  formData.value = {
    planName: '',
    planDesc: '',
    isLongTerm: false,
    appliedVehicleType:'',
    basePrice: 0,
    overtimeFee: 0,
    mileageLimit: null,
    excessMileageFee: 0,
    active: true
  };

};

// --- 一鍵輸入 (測試與 Demo 用) ---
const fillDemoData = () => {
  formData.value = {
    planName: '夏季週末出遊特惠專案',
    planDesc: '專為週末小旅行設計的超值方案，包含高額免費里程與極低的超時費用。',
    isLongTerm: false,
    appliedVehicleType:'小型轎車',
    basePrice: 1500,
    overtimeFee: 100,
    mileageLimit: 300,
    excessMileageFee: 15,
    active: true
  };
};



// --- API 呼叫 ---
const fetchPlans = async () => {
  isLoading.value = true;

  try {
    const response = await rentalPlansAPI.getAll();
    plans.value = response.data;
  } catch (error) {
    console.error("抓取失敗", error);
    alert('無法載入資料，請檢查網路或後端狀態');
  } finally {
    isLoading.value = false;
  }
};

const deletePlan = (id) => {
  pendingDeletePlanId.value = id;
  showDeleteConfirm.value = true;
};

const confirmDeletePlan = async () => {
  if (!pendingDeletePlanId.value) return;

  try {
    await rentalPlansAPI.delete(pendingDeletePlanId.value);
    alert("刪除成功！");
    fetchPlans();
  } catch (error) {
    console.error("刪除發生錯誤", error);
    alert("刪除發生錯誤");
  } finally {
    showDeleteConfirm.value = false;
    pendingDeletePlanId.value = null;
  }
};

// --- 路由跳轉 ---
// const goToCreate = () => router.push('/plans/new');
// const goToEdit = (id) => router.push(`/plans/edit/${id}`);

// 元件載入時執行
onMounted(() => {
  fetchPlans();
});
</script>

<template>
  <div class="container-fluid py-4">
    <LoadingSpinner :isLoading="isLoading" overlay text="方案資料載入中..." />
    <div class="mb-3">
      <Breadcrumb :items="[
        { label: '首頁', to: '/orders' },
        { label: '租訂系統', to: '/orders' },
        { label: '訂單管理', to: '/orders/list' },
        { label: '方案管理' }
      ]" />
    </div>
    <!-- 標題與操作區 -->
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h3 class="mb-0 fw-bold">租賃方案管理</h3>
      <button class="btn btn-primary px-4" @click="openCreate">新增方案</button>
    </div>
    
    <!-- 表格區塊 -->
    <div class="bg-white border rounded shadow-sm overflow-hidden">
      <div class="table-responsive">
        <table class="table table-hover align-middle text-nowrap mb-0">
          <thead class="table-dark">
            <tr>
              <th scope="col" width="5%">方案 ID</th>
              <th scope="col" width="20%">方案資訊</th>
              <th scope="col" width="10%">類型</th>
              <th scope="col" width="15%">基礎價格</th>
              <th scope="col" width="20%">超額計費規則</th>
              <th scope="col" width="15%">狀態</th>
              <th scope="col" width="15%" class="text-center ">操作</th>
            </tr>
          </thead>
          <tbody>
            <!-- 若無資料 -->
            <tr v-if="plans.length === 0">
              <td colspan="7">
                <EmptyState
                  icon="box-open"
                  message="目前尚無方案資料"
                  subMessage="點擊新增方案建立第一筆租賃方案"
                >
                  <button type="button" class="btn btn-primary btn-sm mt-2" @click="openCreate">
                    新增方案
                  </button>
                </EmptyState>
              </td>
            </tr>
            
            <!-- 資料迴圈 -->
            <tr v-for="plan in plans" :key="plan.planId">
              <!-- 1. ID (點擊可開啟 Modal) -->
              <td class="fw-bold">
                <a href="#" @click.prevent="openDetails(plan)" class="text-primary text-decoration-none">
                  #{{ plan.planId }}

                  <!-- 如果不寫 .prevent，它會執行<a>的預設行為，會導致網頁滾回最頂端-->
                </a>
              </td>
              
              <!-- 2. 方案資訊 -->
              <td>
                <div class="fw-bold">{{ plan.planName }}</div>
                <small class="text-muted">{{ plan.planDesc }}</small>
              </td>
              
              <!-- 3. 類型 -->
              <td>
                <span :class="['badge', plan.isLongTerm ? 'bg-primary' : 'bg-info text-dark']">
                  {{ plan.isLongTerm ? '長租方案' : '日租方案' }}
                </span>
              </td>
              
              <!-- 4. 基礎價格 -->
              <td><span class="text-danger fw-bold">${{ plan.basePrice }}</span></td>
              
              <!-- 5. 超額計費 -->
              <td>
                <ul class="list-unstyled mb-0 font-monospace small text-muted">
                  <li>超時: ${{ plan.overtimeFee }}/時</li>
                  <li>限程: {{ plan.mileageLimit }}km</li>
                  <li>超程: ${{ plan.excessMileageFee }}/km</li>
                </ul>
              </td>
              
              <!-- 6. 狀態 -->
              <td>
                <StatusBadge
                  :status="plan.active ? 'ACTIVE' : 'INACTIVE'"
                  :map="planActiveMap"
                />
              </td>
              
              <!-- 7. 操作按鈕 -->
              <td class="text-center ">
                <button class="btn btn-sm btn-outline-secondary me-2 px-3" @click="openEdit(plan)">編輯</button>
                <button class="btn btn-sm btn-outline-danger px-3" @click="deletePlan(plan.planId)">刪除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal 遮罩與主體 -->
    <div v-if="showModal" class="modal-backdrop fade show"></div>
            <!-- 這個是蓋在網頁最上層的半透明黑底，fade show有淡入的動畫 -->
    <div v-if="showModal" class="modal fade show d-block" tabindex="-1" @click.self="closeDetails">
      <!-- 它的真面目：這是一個尺寸跟螢幕一樣大、但本身是透明的隱形容器。
       那個有著圓角、白底的真正視窗（modal-content），是放在這個隱形容器裡面的。 -->

            <!-- 視窗主控台(滿版)，d-block是強制顯示真正的視窗，覆蓋Bootstrap預設的隱藏，
              tabindex="-1"代表按tab鍵時請跳過這個元素，不要停在它身上，加上self後點擊外面的黑色區域才會關閉
              不然點裡面的白色區域也會跟著關-->
      <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
            <!-- modal-dialog-centered是指讓視窗保持在螢幕的正中央;
             modal-dialog-scrollable 確保裡面資料太多時，只有內部會出現上下卷軸，不會連帶整個網頁一起滾動 -->
        <div class="modal-content">
           <!-- 這裡是白色卡片本體 -->
          <div class="modal-header ">
            <!-- 這裡是標題區，含btn-close這是打叉按鈕 -->
            <h5 class="modal-title fw-bold">方案詳情：#{{ selectedPlan.planId }}</h5>
            <button type="button" class="btn-close" @click="closeDetails"></button>
          </div>
          <div class="modal-body p-0">
            <ul class="list-group list-group-flush">
              <!-- list-group: 將列表變成有邊框的群組區塊。 list-group-flush: 移除外邊框，完美貼合父容器。 -->
              <li class="list-group-item"><strong>方案名稱：</strong> {{ selectedPlan.planName }}</li>
              <li class="list-group-item"><strong>方案描述：</strong> {{ selectedPlan.planDesc }}</li>
              <li class="list-group-item">
                <!-- list-group-item: 每一個列表項目 (`<li>`) 的標準橫線與間距。 -->
                <strong>類型：</strong> 
                <span :class="selectedPlan.isLongTerm ? 'badge bg-primary' : 'badge bg-info text-dark'">
                  {{ selectedPlan.isLongTerm ? '長租' : '日租' }}
                </span>
              </li>
              <li class="list-group-item"><strong>適用車型：</strong> {{ selectedPlan.appliedVehicleType }}</li>
              <li class="list-group-item"><strong>基礎價格：</strong> ${{ selectedPlan.basePrice }}</li>
              <li class="list-group-item"><strong>超時費用：</strong> ${{ selectedPlan.overtimeFee }} / 小時</li>
              <li class="list-group-item"><strong>里程限制：</strong> {{ selectedPlan.mileageLimit }} km</li>
              <li class="list-group-item"><strong>超程費用：</strong> ${{ selectedPlan.excessMileageFee }} / km</li>
              <li class="list-group-item">
                <strong>狀態：</strong> 
                <StatusBadge
                  :status="selectedPlan.active ? 'ACTIVE' : 'INACTIVE'"
                  :map="planActiveMap"
                />
              </li>
              <li class="list-group-item"><strong>建立時間：</strong> {{ selectedPlan.createdAt }}</li>
              <li class="list-group-item"><strong>最後更新：</strong> {{ selectedPlan.updatedAt }}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showFormModal" class="modal-backdrop fade show"></div>
    <div v-if="showFormModal" class="modal fade show d-block" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
          <div class="modal-content">

            <div class="modal-header align-items-center">
              <h5 class="modal-title fw-bold">
                {{ isEditMode ? '編輯方案' : '新增方案' }}
              </h5>


              <div class="d-flex align-items-center ms-auto">

              <!-- ⚡ 一鍵帶入按鈕 -->
              <button 
                v-if="!isEditMode" 
                type="button" 
                class="btn btn-sm btn-outline-info ms-auto me-3" 
                @click="fillDemoData"
              >
                ⚡ 一鍵帶入
              </button>

              <!-- X 關閉按鈕 -->
              <button type="button" class="btn-close" @click="closeFormDetails"></button>
              </div>
            </div>

              <!-- modal-body -->
              <div class="modal-body">
                <form>
                  <!-- 1. 方案名稱 (String) -->
                  <div class="mb-3">
                    <label class="form-label text-secondary small mb-1">方案名稱 <span class="text-danger">*</span></label>
                    <input type="text" class="form-control" v-model="formData.planName" placeholder="請輸入方案名稱">
                  </div>

                  <!-- 2. 方案描述 (String) -->
                  <div class="mb-3">
                    <label class="form-label text-secondary small mb-1">方案描述</label>
                    <textarea class="form-control" rows="2" v-model="formData.planDesc" placeholder="請輸入描述(可留白)"></textarea>
                  </div>

                  <!-- 3. 類型 (Boolean) -->
                  <div class="mb-3">
                    <label class="form-label text-secondary small mb-1">方案類型</label>
                    <select class="form-select" v-model="formData.isLongTerm">
                      <option :value="false">日租方案</option>
                      <option :value="true">長租方案</option>
                    </select>
                  </div>


                  <!-- 3.5 車輛類型-->
                  <div class="mb-3">
                    <label class="form-label text-secondary small mb-1">
                      適用車型 <span class="text-danger">*</span>
                    </label>
                    <select class="form-select" v-model="formData.appliedVehicleType">
                      <option value="">請選擇車型</option>
                      <option value="小型轎車">小型轎車</option>
                      <option value="中型轎車">中型轎車</option>
                      <option value="休旅車">休旅車</option>
                      <option value="廂型車">廂型車</option>
                      <option value="電動車">電動車</option>
                      <option value="其他">其他</option>
                    </select>
                  </div>

                  <!-- 4. 基礎價格 (BigDecimal) -->
                  <div class="mb-3">
                    <label class="form-label text-secondary small mb-1">基礎價格 ($)</label>
                    <input type="number" class="form-control" v-model="formData.basePrice" min="0">
                  </div>

                  <!-- 5. 超時費用 (BigDecimal) -->
                  <div class="mb-3">
                    <label class="form-label text-secondary small mb-1">超時費用 ($ / 每小時)</label>
                    <input type="number" class="form-control" v-model="formData.overtimeFee" min="0">
                  </div>

                  <!-- 6. 里程限制 (Integer) -->
                  <div class="mb-3">
                    <label class="form-label text-secondary small mb-1">里程限制 (km)</label>
                    <!-- 加上 placeholder 提示，用 null 代表無限 -->
                    <input type="number" class="form-control" v-model="formData.mileageLimit" placeholder="留空代表無限里程">
                  </div>

                  <!-- 7. 超程費用 (BigDecimal) -->
                  <div class="mb-3">
                    <label class="form-label text-secondary small mb-1">超程費用 ($ / 每公里)</label>
                    <input type="number" class="form-control" v-model="formData.excessMileageFee" min="0">
                  </div>

                  <!-- 8. 上架狀態 (Boolean) -->
                  <div class="mb-3 form-check form-switch mt-4">
                    <input class="form-check-input" type="checkbox" role="switch" id="activeSwitch" v-model="formData.active">
                    <label class="form-check-label" for="activeSwitch">
                      {{ formData.active ? '🟢 目前為上架狀態' : '⚫ 暫時下架' }}
                    </label>
                  </div>
                </form>
              </div>

              <!-- modal-footer -->
              <div class="modal-footer">
                <!-- 取消按鈕 -->
                <button type="button" class="btn btn-outline-secondary" @click="closeFormDetails">取消</button>
                <!-- 確定按鈕 (呼叫你寫好的 submitForm) -->
                <button type="button" class="btn btn-primary px-4" @click="submitForm">{{ isEditMode ? '儲存修改' : '建立方案' }}</button>
              </div>

            
          </div>
        </div>
    </div>
    <ConfirmDialog
      :isVisible="showDeleteConfirm"
      title="刪除方案"
      :message="`確定要刪除方案 #${pendingDeletePlanId} 嗎？`"
      confirmText="刪除"
      cancelText="取消"
      confirmVariant="danger"
      @confirm="confirmDeletePlan"
      @cancel="showDeleteConfirm = false"
    />
  </div>
</template>
