<script setup>
import { ref, onMounted, computed } from 'vue';
import { vehicleAPI } from '@/api/vehicle/vehicleApi';
import usedCarApi from '@/api/usedcar/usedCarApi';
// import BaseModal from '@/components/common/BaseModal.vue';
import AddModal from './UsedCarAddView.vue';
import EditModal from './UsedCarEditView.vue';
import DetailModal from './UsedCarDetailView.vue';

//狀態變數
const carList = ref([]);
const loading = ref(false);
// 搜尋用的變數
const searchQuery = ref('');    // 文字框內容
const searchType = ref('vehicleId'); // 預設搜尋類型

// --- 彈窗控制變數 ---
const addModal = ref(false); // 新增彈窗
const editModal = ref(false); // 修改彈窗
const detailModal = ref(false); // 詳情彈窗
const selectedCar = ref(null); // 存放點擊的車輛資料 (用於詳情和修改)
const selectedVehicle = ref(null);  // 存放點擊的車輛詳細資料 (用於詳情)


// 1. 定義「工廠函式」回傳初始資料  status 初始值設為字串代碼
// 這樣未來要增加欄位，只需要改這裡
// const getInitialAddForm = () => ({
//   vehicleId: '',
//   askingPrice: '',
//   conditionDesc: '',
//   listDate: new Date().toISOString().split('T')[0], // 預設今天
//   expireDate: '',
//   status: '',
//   createdTime: new Date().toISOString()
// });

// const addForm = ref(getInitialAddForm()); // 使用工廠函式初始化

// // 3. 開啟新增視窗的方法
// const openAddModal = () => {
//   // 呼叫工廠函式重設資料，保證每次開啟都是乾淨的
//   addForm.value = getInitialAddForm();
//   dateError.value = ''; // 清空錯誤訊息
//   addModal.value = true;
// };

// 4. 提交新增  處理 status 物件/字串轉化
// const submitAdd = async () => {
//   // 前端簡單檢查
//   if (!addForm.value.selectedCar) {
//     alert("請選擇車輛！");
//     return;
//   }

//   try {
//     loading.value = true;
//     // 增加強制前端驗證
//     if (!addForm.value.expireDate) {
//       alert("請務必填寫「刊登截止日」！");
//       return;
//     }

//     if (!addForm.value.listDate) {
//       alert("請務必填寫「上架日期」！");
//       return;
//     }
//     // 組裝 payload (省略部分重複邏輯...)
//     const payload = {
//       ...addForm.value,
//       vehicleId: addForm.value.selectedCar.vehicleId,
//       askingPrice: Number(addForm.value.askingPrice)
//     };

//     const response = await usedCarApi.insert(payload);

//     // 如果後端有回傳新的列表，這裡寫法是正確的
//     carList.value = response.data.data;
//     alert('資料新增成功！');
//     addModal.value = false;
//     // 重新呼叫 loadData 刷新列表資料
//     await loadData();

//   } catch (error) {
//     const res = error.response;

//     // 這裡處理後端統一回傳的 ApiResponse 格式
//     if (res && res.data && res.data.data) {
//       // 讀取後端回傳的欄位錯誤 Map
//       const fieldErrors = res.data.data;
//       // 顯示第一個錯誤訊息
//       const firstKey = Object.keys(fieldErrors)[0];
//       alert(`新增失敗：${fieldErrors[firstKey]}`);
//     } else {
//       // 處理系統級錯誤
//       alert('新增失敗：' + (res?.data?.message || '未知錯誤'));
//     }
//   } finally {
//     loading.value = false;
//   }
// };

// 查-------------------------------------------------------------

// 點擊車牌事件
// 點擊車牌/詳情：抓取單筆詳細資料 (對應原 openModal)
const openModal = async (id) => {
  try {
    const response = await usedCarApi.getOneDetail(id);
    const result = response.data.data || response.data;
    if (result) {
      selectedVehicle.value = result;
      detailModal.value = true;
    }
  } catch (error) {
    console.error("無法取得車輛明細", error);
  }
};

// --- 搜尋過濾  ---
// 依照特定選項搜尋單筆資料
const filteredCars = computed(() => {
  if (!searchQuery.value) return carList.value; // 沒輸入文字就顯示全部

  return carList.value.filter(car => {
    let content = '';
    if (searchType.value === 'status') {
      // 搜尋狀態時，比對中文描述或英文代碼
      content = `${car.status.description} ${car.status.dbCode}`;
    } else {
      content = String(car[searchType.value] || '');
    }
    return content.toLowerCase().includes(searchQuery.value.toLowerCase());
  });
});

// 點擊「詳情」：抓取單筆資料並顯示視窗
// const showDetail = async (id) => {
//   try {
//     const response = await usedCarApi.getById(id);
//     selectedCar.value = response.data;
//     showModal.value = true;
//   } catch (error) {
//     alert('無法取得詳細資料');
//   }
// };

//修改------------------------------------------------------------
// 用於存儲正在編輯的資料 (新增)
// const editForm = ref({
//   usedCarId: '',
//   askingPrice: 0,
//   status: '',
//   conditionDesc: '',
//   listDate: '',
//   expireDate: ''
// });
// // 新增一個存放純庫存車輛的陣列
const availableVehicles = ref([]);

// 讀取列表與庫存車輛
const loadData = async () => {
  loading.value = true;
  try {
    // 1. 撈出所有二手車清單
    const resList = await usedCarApi.getAll();
    carList.value = resList.data.data || resList.data;

    // 2. [新增] 撈出所有庫存車輛 (假設你有一個 VehicleAPI)
    // 請確認你有類似的方法來撈出所有車輛
    const resVehicles = await vehicleAPI.getAll();
    availableVehicles.value = resVehicles.data.data || resVehicles.data || [];

  } catch (error) {
    alert('無法取得資料');
  } finally {
    loading.value = false;
  }
};

// const handleUpdate = async (id) => {
//   try {
//     const response = await usedCarApi.getById(id);
//     const data = response.data;
//     // 清空舊資料並填入新資料，這能強制 Vue 偵測到屬性變化
//     Object.assign(editForm.value, {
//       ...data,
//       status: data.status.dbCode
//     });
//     editModal.value = true;

//   } catch (error) {
//     alert('無法取得欲修改的資料');
//   }
// };

// 提交修改 
const submitUpdate = async () => {
  try {
    loading.value = true;
    const id = editForm.value.usedCarId;
    // API 回傳最新 List

    //準備發送的資料，確保 status 是字串
    const payload = {
      ...editForm.value,
      status: typeof editForm.value.status === 'object' ? editForm.value.status.dbCode : editForm.value.status
    };

    await usedCarApi.update(id, payload);

    alert('資料更新成功！');
    editModal.value = false;

    await loadData();
  } catch (error) {
    alert('修改失敗');
  } finally {
    loading.value = false;
  }
};

// 點擊編輯：抓取舊資料並開啟彈窗 (對應原 handleUpdate)
const handleEdit = async (id) => {
  try {
    const response = await usedCarApi.getById(id);
    selectedCar.value = response.data; // 這裡拿到的是二手車物件，包含 status.dbCode
    editModal.value = true;
  } catch (error) {
    alert('無法取得欲修改的資料');
  }
};


//------------------------------------------------------------

// 刪除資料
const handleDelete = async (id) => {
  if (confirm(`確定要刪除編號 ${id} 嗎？`)) {
    try {
      await usedCarApi.delete(id);
      alert('刪除成功');
      await loadData(); // 重新拉取完整清單
    } catch (error) {
      alert('刪除失敗');
    }
  }
};

// const fillTestData = () => {
//   // console.log('fillTestData 有觸發');
//   // 檢查是否有可用的車輛清單
//   if (availableVehicles.value && availableVehicles.value.length > 0) {
//     // 隨機選一個（或者選第一個 [0]）
//     const randomCar = availableVehicles.value[3];

//     addForm.value = {
//       selectedCar: randomCar, // 這裡要塞入整個物件，Select 才會正確選中
//       askingPrice: 588000,
//       conditionDesc: '車況良好，無事故',
//       listDate: new Date().toISOString().split('T')[0], // 預設今天,
//       expireDate: '2026-12-31',
//       status: 'ACTIVE',
//       createdTime: new Date().toISOString()
//     };
//     console.log("測試資料已填入:", addForm.value);
//   } else {
//     alert("目前沒有可選的庫存車輛，無法填入測試資料");
//   }
// };

//日期判斷邏輯

const todayStr = new Date().toISOString().split('T')[0];
// const dateError = ref('');

// const validateDates = (type = 'edit') => {
//   // 1. 根據傳入的類型決定檢查對象
//   const form = type === 'add' ? addForm.value : editForm.value;

//   const startStr = form.listDate;
//   const endStr = form.expireDate;

//   if (startStr && endStr) {
//     const start = new Date(startStr);
//     const end = new Date(endStr);

//     if (start > end) {
//       dateError.value = '上架日不得晚於截止日';
//     } else {
//       dateError.value = '';
//     }
//   }
// };


onMounted(() => {
  loadData();

});
</script>

<template>
  <div class="container">
    <h2 class="mb-0 fw-bold">🚗 二手車庫存管理</h2>

    <div class="action-bar d-flex align-items-center justify-content-between mb-3">
      <div class="search-group d-flex gap-2 flex-grow-1 me-3" style="max-width: 500px;">
        <select v-model="searchType" class="form-select w-auto">
          <option value="vehicleId">按原車輛</option>
          <option value="status">按出售狀態</option>
        </select>
        <div class="input-group">
          <input type="text" v-model="searchQuery" placeholder="輸入關鍵字查詢..." class="form-control" />
        </div>
      </div>
      <button @click="addModal = true" class="btn btn-primary text-nowrap">➕ 新增車輛</button>
    </div>

    <div class="bg-white border rounded shadow-sm overflow-hidden">
      <div class="table-responsive">
        <table class="table table-hover align-middle text-nowrap mb-0">
          <thead class="table-dark">
            <tr>
              <th>編號</th>
              <th>車輛編號</th>
              <th>預售價格</th>
              <th>上架日期</th>
              <th>刊登截止日</th>
              <th>狀態</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="car in filteredCars" :key="car.usedCarId" @click="openModal(car.usedCarId)"
              class="clickable-row">
              <td>{{ car.usedCarId }}</td>
              <td>{{ car.vehicleId }}</td>
              <td class="price">${{ car.askingPrice }}</td>
              <td>{{ car.listDate }}</td>
              <td>{{ car.expireDate }}</td>
              <td>{{ car.status?.description }}</td>
              <td @click.stop>
                <button @click="handleEdit(car.usedCarId)" class="btn btn-sm btn-outline-secondary me-2">編輯</button>
                <button @click="handleDelete(car.usedCarId)" class="btn btn-sm btn-outline-danger">刪除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <AddModal :isVisible="addModal" :availableVehicles="availableVehicles" :todayStr="todayStr"
      @close="addModal = false" @refresh="loadData" />

    <EditModal :isVisible="editModal" :editData="selectedCar" :todayStr="todayStr" @close="editModal = false"
      @refresh="loadData" />

    <DetailModal :isVisible="detailModal" :selectedVehicle="selectedVehicle" @close="detailModal = false" />

    <div class="footer mt-3">
      <h3>📊 統計資訊：共 {{ filteredCars?.length || 0 }} / {{ carList?.length || 0 }} 筆資料</h3>
    </div>
  </div>
</template>

<!--
<template>
  <div class="container">
    <h2 class="mb-0 fw-bold">🚗 二手車庫存管理</h2>

    <div class="action-bar d-flex align-items-center justify-content-between mb-3">
      <div class="search-group d-flex gap-2 flex-grow-1 me-3" style="max-width: 500px;">
        <select v-model="searchType" class="form-select w-auto">
          <option value="vehicleId">按原車輛</option>
          <option value="status">按出售狀態</option>
        </select>
        <div class="input-group">
          <input type="text" v-model="searchQuery" placeholder="輸入關鍵字查詢..." class="form-control" />
          <button class="btn btn-outline-secondary">查詢</button>
        </div>
      </div>
      <button @click="openAddModal" class="btn btn-primary text-nowrap">
        ➕ 新增車輛
      </button>
    </div>

    <div class="bg-white border rounded shadow-sm overflow-hidden">
      <div class="table-responsive">
        <table class="table table-hover align-middle text-nowrap mb-0">
          <thead class="table-dark">
            <tr>
              <th>編號</th>
              <th>車輛編號</th>
              <th>預售價格</th>
              <th>上架日期</th>
              <th>刊登截止日</th>
              <th>狀態</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="car in filteredCars" :key="car.usedCarId" @click="openModal(car.usedCarId)"
              class="clickable-row">
              <td>{{ car.usedCarId }}</td>
              <td>{{ car.vehicleId }}</td>
              <td class="price">${{ car.askingPrice }}</td>
              <td>{{ car.listDate }}</td>
              <td>{{ car.expireDate }}</td>
              <td>{{ car.status?.description }}</td>
              <td @click.stop>
                <button @click="handleUpdate(car.usedCarId)" class="btn btn-sm btn-outline-secondary me-2">編輯</button>
                <button @click="handleDelete(car.usedCarId)" class="btn btn-sm btn-outline-danger">刪除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="footer">
      <h3>📊 統計資訊：共 {{ filteredCars?.length || 0 }} / {{ carList?.length || 0 }}筆資料</h3>
    </div>
  </div>


  <!-- 車輛詳情Modal -->
<!-- v-if: 若有被點擊車輛，才顯示這個div(詳情modal) -->
<!--
  <BaseModal :isVisible="detailModal" :title="'🔍 車輛詳情：' + (selectedVehicle?.plateNo || '')"
    @close="detailModal = false">
    <template #body>
      <div v-if="selectedVehicle">
        <img v-if="selectedVehicle.vehicleImgUrl" :src="selectedVehicle.vehicleImgUrl" alt="車輛圖片"
          class="img-fluid mb-3">
        <img v-else src="https://via.placeholder.com/400x200?text=No+Image" alt="尚無圖片" class="img-fluid mb-3">
        <hr>
        <p><strong>車輛狀態：</strong>{{ selectedVehicle.status?.description }}</p>
        <p><strong>車色：</strong>{{ selectedVehicle.color }}</p>
        <p><strong>出廠日期：</strong>{{ selectedVehicle.manufactureDate }}</p>
        <p><strong>發照日期：</strong>{{ selectedVehicle.issuedDate }}</p>
        <p><strong>法定驗車日：</strong>{{ selectedVehicle.inspectionDate }}</p>
        <p><strong>累積里程數：</strong>{{ selectedVehicle.mileage }}</p>
        <p><strong>需維修里程數：</strong>{{ selectedVehicle.nextMaintainenceMileage }}</p>
        <p><strong>車況：</strong>{{ selectedVehicle.description }}</p>
        <hr>
        <p><strong>品牌：</strong>{{ selectedVehicle.brand }}</p>
        <p><strong>車款：</strong>{{ selectedVehicle.modelName }}</p>
        <p><strong>排氣量：</strong>{{ selectedVehicle.displacement }}</p>
        <p><strong>迴轉半徑：</strong>{{ selectedVehicle.turningRadius }}</p>
        <p><strong>車型：</strong>{{ selectedVehicle.vehicleType }}</p>
        <p><strong>座位數：</strong>{{ selectedVehicle.seats }}</p>
        <p><strong>行李數：</strong>{{ selectedVehicle.luggageCapacity }}</p>
        <p><strong>動力：</strong>{{ selectedVehicle.fuelType }}</p>
        <hr>
        <p><strong>所在據點：</strong>{{ selectedVehicle.locationName }}</p>
        <p><strong>據點地址：</strong>{{ selectedVehicle.address }}</p>
        <p><strong>據點電話：</strong>{{ selectedVehicle.phone }}</p>
        <hr>
        <p>資料建立時間：{{ selectedVehicle.createdTime }}</p>

      </div>
    </template>
  </BaseModal>

  <!-- 修改------------------------------------------ 
  <BaseModal :isVisible="editModal" :title="'📝 修改車輛資訊 (編號: ' + editForm.usedCarId + ')'" size="lg"
    @close="editModal = false">
    <template #body>
      <form>
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">預售價格 <span class="text-danger">*</span></label>
            <input type="number" v-model="editForm.askingPrice" class="form-control" />
          </div>
          <div class="col-md-6">
            <label class="form-label">車輛狀態 <span class="text-danger">*</span></label>
            <select v-model="editForm.status" class="form-select">
              <option value="ACTIVE">上架</option>
              <option value="SOLD">已售</option>
              <option value="REMOVED">下架</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">上架日期</label>
            <input type="date" v-model="editForm.listDate" @change="validateDates('edit')" class="form-control" />
          </div>
          <div class="col-md-6">
            <label class="form-label">刊登截止日</label>
            <input type="date" v-model="editForm.expireDate" @change="validateDates('edit')" class="form-control"
              :min="todayStr" />
          </div>
          <div class="col-12" v-if="dateError">
            <div class="alert alert-danger py-2 small">{{ dateError }}</div>
          </div>
          <div class="col-12">
            <label class="form-label">車況描述</label>
            <textarea v-model="editForm.conditionDesc" rows="3" class="form-control"></textarea>
          </div>
        </div>
      </form>
    </template>
    <template #footer>
      <button class="btn btn-secondary" @click="editModal = false">取消</button>
      <button class="btn btn-primary" @click="submitUpdate" :disabled="loading || !!dateError">
        <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
        確認修改
      </button>
    </template>
  </BaseModal>

  <!--新增 ------------------------------------------- 
  <BaseModal :isVisible="addModal" title="✨ 新增二手車入庫" size="lg" @close="addModal = false">
    <template #body>
      <div class="mb-3">
        <button class="btn btn-outline-info btn-sm" @click.prevent="fillTestData">💡 一鍵填入測試資料</button>
      </div>
      <form>
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">選擇車輛 <span class="text-danger">*</span></label>
            <select v-model="addForm.selectedCar" class="form-select">
              <option value="" disabled>-- 請選擇一台車 --</option>
              <option v-for="v in availableVehicles" :key="v.vehicleId" :value="v"
                :disabled="v.status.dbCode !== 'RETIRED'">
                {{ v.vehicleId }} - {{ v.modelName }} ({{ v.status.description }})
              </option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">預售價格 <span class="text-danger">*</span></label>
            <input type="number" v-model="addForm.askingPrice" class="form-control" placeholder="0" />
          </div>
          <div class="col-md-6">
            <label class="form-label">上架日期</label>
            <input type="date" v-model="addForm.listDate" @change="validateDates('add')" class="form-control" />
          </div>
          <div class="col-md-6">
            <label class="form-label">刊登截止日</label>
            <input type="date" v-model="addForm.expireDate" @change="validateDates('add')" class="form-control"
              :min="todayStr" />
          </div>
          <div class="col-md-12">
            <label class="form-label">初始狀態</label>
            <select v-model="addForm.status" class="form-select">
              <option value="ACTIVE">上架</option>
              <option value="SOLD">已售出</option>
              <option value="REMOVED">下架</option>
            </select>
          </div>
          <div class="col-12">
            <label class="form-label">車況描述</label>
            <textarea v-model="addForm.conditionDesc" rows="3" class="form-control"></textarea>
          </div>
        </div>
      </form>
    </template>
    <template #footer>
      <button class="btn btn-secondary" @click="addModal = false">取消</button>
      <button class="btn btn-primary" @click="submitAdd" :disabled="loading || !!dateError">
        <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
        確認新增入庫
      </button>
    </template>
  </BaseModal>
</template>

-->

<!-- <style scoped>

</style> -->