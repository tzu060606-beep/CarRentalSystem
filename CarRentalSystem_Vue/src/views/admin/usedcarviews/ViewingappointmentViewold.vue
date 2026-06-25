<script setup>
import { ref, onMounted, computed } from 'vue';
import viewingappointmentApi from '@/api/usedcar/viewingappointmentApi';
import usedCarApi from '@/api/usedcar/usedCarApi';

const viewList = ref([]);
const loading = ref(false);

const carList = ref([]); // 存放車輛清單

const loadCarList = async () => {
    try {
        // 確保這裡的 API 路徑與方法名稱正確
        const response = await usedCarApi.getAll();
        console.log("後端回傳的車輛列表:", response.data);
        carList.value = response.data;
    } catch (error) {
        console.error("無法載入車輛清單:", error);
    }
};

//搜尋用變數
const searchQuery = ref('');
const searchType = ref('usedCarId');

const showModal = ref(false);//詳情彈窗
const editModal = ref(false);//修改彈窗
const selectedView = ref(null);

// 1. 定義「工廠函式」回傳初始資料
const getInitialAddForm = () => ({

    apptId: '',
    usedCarId: '',
    custId: '',
    apptTime: '',
    status: '',
    locationId: '',
    message: '',
    notes: ''
});

// 2. 初始化響應式變數
const addModal = ref(false);
const addForm = ref(getInitialAddForm()); // 使用工廠函式初始化

// 3. 開啟新增視窗的方法
const openAddModal = () => {
    // 呼叫工廠函式重設資料，保證每次開啟都是乾淨的
    addForm.value = getInitialAddForm();
    addModal.value = true;
};

// 4. 提交新增
const submitAdd = async () => {
    try {
        loading.value = true;

        // 確保數字欄位真的是數字 (避免傳送空字串或文字)
        const payload = {
            ...addForm.value,
            usedCarId: Number(addForm.value.usedCarId),
            custId: Number(addForm.value.custId),
            locationId: Number(addForm.value.locationId),
            status: typeof addForm.value.status === 'object' ? addForm.value.status.dbCode : addForm.value.status
        };

        const response = await viewingappointmentApi.insert(payload);
        viewList.value = response.data; // 更新列表

        alert('資料新增成功！');
        addModal.value = false;
    } catch (error) {
        console.error("錯誤詳情:", error.response?.data || error.message);
        alert('新增失敗：' + (error.response?.data?.message || '格式錯誤'));
    } finally {
        loading.value = false;
    }
};


//查--------------------------------------------------
const filteredView = computed(() => {
    if (!searchQuery.value) return viewList.value;

    return viewList.value.filter(view => {
        let content = '';
        if (searchType.value === 'status') {
            // 搜尋狀態時，比對中文描述或英文代碼
            content = `${view.status.description} ${view.status.dbCode}`;
        } else {
            content = String(view[searchType.value] || '');
        }
        return content.toLowerCase().includes(searchQuery.value.toLowerCase());
    })
});

// 點擊「詳情」：抓取單筆資料並顯示視窗
const showDetail = async (id) => {
    try {
        const response = await viewingappointmentApi.getById(id);
        selectedView.value = response.data;
        showModal.value = true;
    } catch (error) {
        alert('無法取得詳細資料');
    }
};

//修改------------------------------------------------------------
// 用於存儲正在編輯的資料 (新增)
const editForm = ref({
    usedCarId: 0,
    custId: 0,
    apptTime: '',
    status: '',
    locationId: 0,
    message: '',
    notes: ''
});

const loadData = async () => {
    loading.value = true; // 補上這行
    try {
        const response = await viewingappointmentApi.getAll();
        viewList.value = response.data;
    } catch (error) {
        alert('無法取得資料');
    } finally {
        loading.value = false; // 補上這行
    }
};

const handleUpdate = async (id) => {
    try {
        const response = await viewingappointmentApi.getById(id);
        const data = response.data;

        // 關鍵處理：將日期格式轉換為 datetime-local 接受的 ISO 格式
        // 如果你的資料庫回傳 "2026-05-06 14:30:00"，需要把中間的空格換成 T
        let formattedTime = data.apptTime;
        if (formattedTime && formattedTime.includes(' ')) {
            formattedTime = formattedTime.replace(' ', 'T').substring(0, 16);
        }

        Object.assign(editForm.value, {
            ...data,
            status: data.status.dbCode,
            apptTime: formattedTime // 使用轉換後的格式
        });
        editModal.value = true;
    } catch (error) {
        alert('無法取得欲修改的資料');
    }
};

// 提交修改 
// 提交修改 
const submitUpdate = async () => {
    try {
        loading.value = true;
        const id = editForm.value.apptId;

        // 1. 準備 payload
        const payload = {
            ...editForm.value,
            status: typeof editForm.value.status === 'object' ? editForm.value.status.dbCode : editForm.value.status
        };

        // 2. 處理時間格式：將 "T" 換成 " " (空格) 並確保有秒數
        if (payload.apptTime) {
            // 替換 T 為空格
            let timeStr = payload.apptTime.replace('T', ' ');

            // 如果只有 YYYY-MM-DD HH:mm (長度 16)，補上 :00
            if (timeStr.length === 16) {
                timeStr += ':00';
            }

            payload.apptTime = timeStr;
        }

        // 3. 發送 payload (不是 editForm.value)
        const response = await viewingappointmentApi.update(id, payload);

        viewList.value = response.data;

        alert('資料更新成功！');
        editModal.value = false;
    } catch (error) {
        console.error("錯誤詳情:", error.response?.data || error.message);
        alert('修改失敗：' + (error.response?.data?.message || '格式錯誤'));
    } finally {
        loading.value = false;
    }
};
//------------------------------------------------------------

// 刪除資料
const handleDelete = async (id) => {
    if (confirm(`確定要刪除編號 ${id} 嗎？`)) {
        try {
            const response = await viewingappointmentApi.delete(id);
            viewList.value = response.data; // 刪除後立即刷新
            alert('刪除成功');
        } catch (error) {
            alert('刪除失敗');
        }
    }
};

const now = new Date();
const apptTime =
    now.getFullYear() + '-' +
    String(now.getMonth() + 1).padStart(2, '0') + '-' +
    String(now.getDate() + 1).padStart(2, '0') + ' ' +
    String(now.getHours()).padStart(2, '0') + ':' +
    String(now.getMinutes()).padStart(2, '0') + ':' +
    String(now.getSeconds()).padStart(2, '0');

const fillTestData = () => {

    if (carList.value.length === 0) {
        alert("系統中沒有車輛，無法填入測試資料");
        return;
    }
    const firstCarId = carList.value[0].usedCarId;

    addForm.value = {
        ...addForm.value,
        usedCarId: firstCarId,
        custId: 1,
        apptTime: apptTime,
        status: 'PENDING',
        locationId: 1,
        message: '確認車況',
        notes: ''
    };
};

const getStatusClass = (dbCode) => {
    // 這裡的 Key (PENDING, CONFIRMED...) 必須跟資料庫傳回來的 dbCode 字串完全一樣
    const statusMap = {
        'PENDING': 'bg-warning text-dark', // 待確認 - 黃色
        'CONFIRMED': 'bg-primary',           // 已預定 - 藍色
        'COMPLETED': 'bg-success',           // 已完成 - 綠色
        'CANCELLED': 'bg-danger'             // 已取消 - 紅色
    };

    return statusMap[dbCode] || 'bg-secondary'; // 找不到時預設灰色
};


//日期判斷邏輯

// 取得現在時間，補上秒數，並轉換為 ISO 格式
const taipeiTime = new Date(new Date().getTime() + 8 * 60 * 60 * 1000);
// 格式化為 "YYYY-MM-DDTHH:mm:ss"
const todayStr = taipeiTime.toISOString().slice(0, 19);


//行事曆
import axios from 'axios';

const syncToGoogleCalendar = async (id) => {
    if (loading.value) return;

    try {
        loading.value = true;
        // 注意：根據你的錯誤圖片，API 似乎直接回傳了 Google 的錯誤
        const response = await axios.post(`/api/appointments/${id}/sync-google`);

        // 成功後更新前端資料狀態
        const item = viewList.value.find(v => v.apptId === id);
        if (item) {
            item.isSynced = true;
        }

        alert("同步成功！");
    } catch (error) {
        console.error("同步詳細錯誤:", error);

        // 判斷 401 Unauthorized (憑證失效)
        if (error.response && error.response.status === 401) {
            console.log("偵測到憑證失效，準備自動跳轉授權...");
            // 直接執行授權彈窗邏輯
            autoRedirectToGoogle();
        } else {
            // 針對圖片中的錯誤訊息進行友善提示
            const msg = error.response?.data || error.message;
            alert("同步失敗：身分驗證已過期，請重新嘗試登入 Google。");
        }
    } finally {
        loading.value = false;
    }
};

const autoRedirectToGoogle = () => {
    const authUrl = "http://localhost:8081/oauth2/authorization/google";
    const authWindow = window.open(authUrl, "GoogleAuth", "width=500,height=600");

    // 因為 COOP 限制，我們無法可靠地偵測視窗關閉
    // 建議在彈窗開啟後，直接給使用者一個提示
    alert("請在彈出的視窗中完成 Google 驗證。驗證完成後，請再次點擊同步按鈕即可。");
};

const handleGoogleAuth = () => {
    const confirmAuth = confirm("尚未開啟 Google 授權，是否現在前往驗證？");
    if (confirmAuth) {
        // 3. 開啟小視窗導向後端的 OAuth2 入口
        const authUrl = "http://localhost:8081/oauth2/authorization/google";
        const width = 500, height = 600;
        const left = (window.screen.width - width) / 2;
        const top = (window.screen.height - height) / 2;

        const authWindow = window.open(
            authUrl,
            "GoogleAuth",
            `width=${width},height=${height},left=${left},top=${top}`
        );

        // 4. 定時檢查小視窗是否關閉，關閉後提示使用者再次點擊同步
        const checkWindow = setInterval(() => {
            if (authWindow.closed) {
                clearInterval(checkWindow);
                alert("驗證視窗已關閉，現在您可以再次點擊同步按鈕。");
            }
        }, 1000);
    }
};

onMounted(() => {
    loadData();
    loadCarList();
});

</script>

<template>

    <div class="container">
        <h2 class="mb-0 fw-bold">預約表管理</h2>

        <div class="action-bar d-flex align-items-center justify-content-between mb-3">
            <div class="search-group d-flex gap-2 flex-grow-1 me-3" style="max-width: 500px;">
                <select v-model="searchType" class="form-select w-auto">
                    <option value="usedCarId">按車輛編號</option>
                    <option value="custId">按客戶編號</option>
                    <option value="status">按預約狀態</option>
                    <option value="locationId">按位置編號</option>
                </select>

                <div class="input-group">
                    <input type="text" v-model="searchQuery" placeholder="輸入關鍵字查詢" class="form-control" />
                    <button class="btn btn-outline-secondary">查詢</button>
                </div>
            </div>
            <button @click="openAddModal" class="btn btn-primary text-nowrap">
                ➕ 新增預約
            </button>
        </div>

        <div v-if="loading" class="text-center py-5">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2 text-muted">資料載入中，請稍候...</p>
        </div>

        <div v-else class="bg-white border rounded shadow-sm overflow-hidden">
            <div class="table-responsive">
                <table class="table table-hover align-middle text-nowrap mb-0">
                    <thead class="table-dark">

                        <tr>
                            <th>預約編號</th>
                            <th>車輛邊號</th>
                            <th>客戶編號</th>
                            <th>預約時間</th>
                            <th>預約狀態</th>
                            <th>位置編號</th>
                            <th>預約訊息</th>
                            <!-- <th>預約備註</th> -->
                            <th class="text-center sticky-end-header">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="view in filteredView" :key="view.apptId" @click="showDetail(view.apptId)"
                            class="clickable-row">
                            <td>{{ view.apptId }}</td>
                            <td>{{ view.usedCarId }}</td>
                            <td>{{ view.custId }}</td>
                            <td>{{ view.apptTime }}</td>
                            <td>{{ view.status.description }}</td>
                            <td>{{ view.locationName }}</td>
                            <td>{{ view.message }}</td>
                            <!-- <td>{{ view.notes }}</td> -->
                            <td class="text-nowrap">
                                <!-- 只有狀態是 CONFIRMED (已預訂) 才顯示這個按鈕 -->
                                <button v-if="view.status.dbCode === 'CONFIRMED'" class="btn btn-sm"
                                    :class="view.isSynced ? 'btn-outline-secondary' : 'btn-outline-primary'"
                                    @click="syncToGoogleCalendar(view.apptId)" :disabled="view.isSynced || loading">
                                    <i class="bi" :class="view.isSynced ? 'bi-check-circle' : 'bi-google'"></i>
                                    {{ view.isSynced ? '已同步' : '同步日曆' }}
                                </button>
                                <button @click.stop="handleUpdate(view.apptId)"
                                    class="btn btn-sm btn-outline-secondary me-1">編輯</button>
                                <button @click.stop="handleDelete(view.apptId)"
                                    class="btn btn-sm btn-outline-danger">刪除</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="footer">
            <h3>統計:共{{ filteredView.length }} / {{ viewList.length }}筆資料</h3>
        </div>

        <div class="modal fade" :class="{ 'show d-block': showModal }" tabindex="-1" v-if="showModal"
            style="background: rgba(0,0,0,0.5)">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content shadow border-0">

                    <div class="modal-header bg-info text-white">
                        <h5 class="modal-title fw-bold">📋 預約詳細表單</h5>
                        <button type="button" class="btn-close btn-close-white" @click="showModal = false"></button>
                    </div>

                    <div class="modal-body p-0">
                        <div v-if="selectedView" class="detail-container">
                            <ul class="list-group list-group-flush">

                                <li class="list-group-item d-flex justify-content-between align-items-center">
                                    <span class="text-muted">預約編號</span>
                                    <span class="fw-bold text-primary"># {{ selectedView.apptId }}</span>
                                </li>

                                <li class="list-group-item d-flex justify-content-between align-items-center">
                                    <span class="text-muted">🚗 車輛編號</span>
                                    <span class="badge bg-secondary rounded-pill">ID: {{ selectedView.usedCarId
                                    }}</span>
                                </li>

                                <li class="list-group-item d-flex justify-content-between align-items-center">
                                    <span class="text-muted">👤 客戶編號</span>
                                    <span class="badge bg-light text-dark border rounded-pill">ID: {{
                                        selectedView.custId }}</span>
                                </li>

                                <li class="list-group-item d-flex justify-content-between align-items-center">
                                    <span class="text-muted">預約看車時間</span>
                                    <span class="fw-bold">{{ selectedView.apptTime }}</span>
                                </li>

                                <li class="list-group-item d-flex justify-content-between align-items-center">
                                    <span class="text-muted">當前狀態</span>
                                    <span class="badge" :class="getStatusClass(selectedView.status.dbCode)">
                                        {{ selectedView.status.description }}
                                    </span>
                                </li>

                                <li class="list-group-item d-flex justify-content-between align-items-center">
                                    <span class="text-muted">看車地點 </span>
                                    <span>{{ selectedView.locationName }}</span>
                                </li>

                                <li class="list-group-item">
                                    <div class="text-muted small mb-1">客戶預約訊息：</div>
                                    <div class="p-2 bg-light rounded text-break italic" style="min-height: 40px;">
                                        {{ selectedView.message || '（客戶未留下訊息）' }}
                                    </div>
                                </li>

                                <li class="list-group-item border-bottom-0">
                                    <div class="text-muted small mb-1">內部管理備註：</div>
                                    <div class="p-2 bg-warning bg-opacity-10 rounded text-break"
                                        style="border-left: 4px solid #ffc107;">
                                        {{ selectedView.notes || '無內部備註' }}
                                    </div>
                                </li>

                            </ul>
                        </div>

                        <div v-else class="text-center p-5">
                            <div class="spinner-border text-primary" role="status"></div>
                            <p class="mt-2 text-muted">資料載入中...</p>
                        </div>
                    </div>

                    <div class="modal-footer bg-light">
                        <button type="button" class="btn btn-secondary px-4" @click="showModal = false">關閉</button>
                    </div>

                </div>
            </div>
        </div>
    </div>
    <!-- 修改----------------------------------------------- -->
    <div class="modal fade" :class="{ 'show d-block': editModal }" tabindex="-1" v-if="editModal"
        style="background: rgba(0,0,0,0.5)">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content shadow">

                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold">📝 修改預約資料 (編號: {{ editForm.apptId }})</h5>
                    <button type="button" class="btn-close" @click="editModal = false"></button>
                </div>

                <div class="modal-body">
                    <form>
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label">車輛編號 <span class="text-danger">*</span></label>
                                <input type="text" v-model="editForm.usedCarId" class="form-control"
                                    placeholder="輸入車輛 ID" />
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">客戶編號 <span class="text-danger">*</span></label>
                                <input type="text" v-model="editForm.custId" class="form-control"
                                    placeholder="輸入客戶 ID" />
                            </div>

                            <div class="col-md-6">
                                <label class="form-label">預約時間 <span class="text-danger">*</span></label>
                                <input type="datetime-local" v-model="editForm.apptTime" :min="todayStr"
                                    class="form-control" placeholder="YYYY-MM-DD HH:mm" />
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">看車地點 (位置 ID) <span class="text-danger">*</span></label>
                                <select v-model="editForm.locationId" class="form-select">
                                    <option value="1">台北總站</option>
                                    <option value="2">新竹站</option>
                                </select>
                            </div>

                            <div class="col-12">
                                <label class="form-label">預約狀態 <span class="text-danger">*</span></label>
                                <select v-model="editForm.status" class="form-select">
                                    <option value="PENDING">🕒 待確認 (PENDING)</option>
                                    <option value="CONFIRMED">✅ 已預定 (CONFIRMED)</option>
                                    <option value="COMPLETED">🏁 已完成看車 (COMPLETED)</option>
                                    <option value="CANCELLED">❌ 已取消 (CANCELLED)</option>
                                </select>
                            </div>

                            <div class="col-12">
                                <label class="form-label">預約訊息</label>
                                <textarea v-model="editForm.message" rows="2" class="form-control"
                                    placeholder="客戶留下的訊息..."></textarea>
                            </div>

                            <div class="col-12">
                                <label class="form-label">內部管理備註</label>
                                <textarea v-model="editForm.notes" rows="2"
                                    class="form-control bg-warning bg-opacity-10" placeholder="填寫內部追蹤備註..."></textarea>
                            </div>
                        </div>
                    </form>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" @click="editModal = false">取消</button>
                    <button type="button" class="btn btn-primary px-4" @click="submitUpdate" :disabled="loading">
                        <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
                        {{ loading ? '儲存中...' : '確認修改' }}
                    </button>
                </div>

            </div>
        </div>
    </div>
    <!--新增 ------------------------------------------- -->
    <div class="modal fade" :class="{ 'show d-block': addModal }" tabindex="-1" v-if="addModal"
        style="background: rgba(0,0,0,0.5)">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content shadow">

                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold">➕ 新增預約表</h5>
                    <button type="button" class="btn-close" @click="addModal = false"></button>
                </div>

                <div class="modal-body px-4">
                    <div class="mb-3">
                        <button @click.prevent="fillTestData" class="btn btn-sm btn-outline-info">
                            ⚡ 一鍵填入測試資料
                        </button>
                    </div>

                    <form>
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label">車輛編號 <span class="text-danger">*</span></label>
                                <select v-model="addForm.usedCarId" class="form-select" required>
                                    <option value="" disabled selected>-- 請選擇車輛 --</option>
                                    <option v-for="car in carList" :key="car.usedCarId" :value="car.usedCarId"
                                        :disabled="car.status.dbCode !== 'ACTIVE'">
                                        {{ car.usedCarId }} - {{ car.modelName }}
                                        {{ car.status.dbCode !== 'ACTIVE' ? '(不可預約)' : '(可預約)' }}
                                    </option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">客戶編號 <span class="text-danger">*</span></label>
                                <input type="text" v-model="addForm.custId" class="form-control"
                                    placeholder="請輸入客戶編號" />
                            </div>

                            <div class="col-md-6">
                                <label class="form-label">預約時間 <span class="text-danger">*</span></label>
                                <input type="datetime-local" v-model="addForm.apptTime" :min="todayStr"
                                    class="form-control" placeholder="YYYY-MM-DD HH:mm" />
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">看車地點 <span class="text-danger">*</span></label>
                                <select v-model="addForm.locationId" class="form-select">
                                    <option :value="null" disabled>請選擇地點</option>
                                    <option value="1">1 (台北總站)</option>
                                    <option value="2">2 (新竹站)</option>
                                </select>
                            </div>

                            <div class="col-12">
                                <label class="form-label">預約狀態 <span class="text-danger">*</span></label>
                                <select v-model="addForm.status" class="form-select">
                                    <option value="PENDING">待確認</option>
                                    <option value="CONFIRMED">已預定</option>
                                    <option value="COMPLETED">已完成看車</option>
                                    <option value="CANCELLED">已取消</option>
                                </select>
                            </div>

                            <div class="col-12">
                                <label class="form-label">預約訊息</label>
                                <textarea v-model="addForm.message" rows="2" class="form-control"
                                    placeholder="請輸入客戶預約訊息..."></textarea>
                            </div>

                            <div class="col-12">
                                <label class="form-label">預約備註</label>
                                <textarea v-model="addForm.notes" rows="2" class="form-control bg-light"
                                    placeholder="內部管理用備註..."></textarea>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-info me-auto"
                                    @click="syncToGoogleCalendar(editForm.apptId)">
                                    📅 同步到 Google 日曆
                                </button>
                            </div>
                        </div>
                    </form>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary px-4" @click="addModal = false">取消</button>
                    <button type="button" class="btn btn-primary px-4" @click="submitAdd" :disabled="loading">
                        <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
                        {{ loading ? '儲存中...' : '確認新增' }}
                    </button>
                </div>

            </div>
        </div>
        <!-- <div>
            <a href="http://localhost:8081/oauth2/authorization/google">
                <button>使用 Google 帳號同步行事曆</button>
            </a>
        </div> -->
    </div>

</template>



<style scoped></style>
