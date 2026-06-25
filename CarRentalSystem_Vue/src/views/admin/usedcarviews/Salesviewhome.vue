<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import usedCarApi from '@/api/usedcar/usedCarApi';
import salesrecordApi from '@/api/usedcar/salesrecordApi';
import viewingappointmentApi from '@/api/usedcar/viewingappointmentApi';
import { customerAPI } from '@/api/login/customerAPI';
import ViewingappointmentEditView from '@/views/admin/usedcarviews/ViewingappointmentEditView.vue';

const showEditModal = ref(false);
const appointmentToEdit = ref(null);

// 3. 修改「辦理」按鈕的方法：點擊時將該筆資料存入 appointmentToEdit 並開啟彈窗
const openEditModal = (appt) => {
    appointmentToEdit.value = appt;
    showEditModal.value = true;
};

// 4. (選填) 處理行事曆同步邏輯 (對應組件中的 @sync)
const handleCalendarSync = (formData) => {
    console.log("正在同步 Google 行事曆:", formData);
    // 這裡放您原本同步行事曆的 API 邏輯
};

const router = useRouter();

const totalCars = ref(0);
const totalSales = ref(0);
const totalViews = ref(0);

// 存放最近的預約名單
const recentAppointments = ref([]);

// 統計子狀態數據
const carStatusStats = ref({ removed: 0, active: 0, sold: 0 });
const viewStatusStats = ref({ pending: 0, confirmed: 0, completed: 0, cancelled: 0 });
const saleStatusStats = ref({ pending: 0, paid: 0, cancelled: 0 });
// 1. 抓取資料的方法
const fetchStats = async () => {
    try {
        // 使用 Promise.all 同時發出三個 API 請求，不用排隊，速度最快
        const [carRes, saleRes, viewRes, customerRes] = await Promise.all([
            usedCarApi.getAllDetails(),
            salesrecordApi.getAll(),
            viewingappointmentApi.getAll(),
            customerAPI.getAll()
        ]);
        // 將後端回傳的陣列長度（資料筆數）賦值給各自的 ref.value
        totalCars.value = carRes.data ? carRes.data.length : 0;
        totalSales.value = saleRes.data ? saleRes.data.length : 0;
        totalViews.value = viewRes.data ? viewRes.data.length : 0;

        // 計算在庫車輛的子狀態
        if (carRes.data) {
            // 用 filter 撈出 dbCode 等於 'ACTIVE' 的車輛，計算長度，就是「已上架數量」
            carStatusStats.value.active = carRes.data.filter(c => c.usedCarStatus?.dbCode === 'ACTIVE').length;
            carStatusStats.value.removed = carRes.data.filter(c => c.usedCarStatus?.dbCode === 'REMOVED').length;
            carStatusStats.value.sold = carRes.data.filter(c => c.usedCarStatus?.dbCode === 'SOLD').length;
        }
        // 核心邏輯：將預約表與顧客資料進行關聯
        if (viewRes.data && customerRes.data) {
            // 🌟 步驟 A：把預約表每一筆資料重新 map 組合，透過 customerId 尋找顧客真實資料
            const fullAppointments = viewRes.data.map(appt => {
                // 在顧客列表中，找到該預約的 customerId 對應的顧客物件
                // 註：請根據你實際後端欄位確認是 appt.customerId 還是 appt.customerId?.customerId
                const matchCustomer = customerRes.data.find(cust => cust.custId === appt.custId);

                return {
                    ...appt,
                    // 動態擴充欄位，如果找不到就顯示 '未知客戶' 或 '未提供'
                    custName: matchCustomer ? matchCustomer.custName : '線上客戶',
                    custPhone: matchCustomer ? matchCustomer.custPhone : '未提供'
                };
            });

            // 🌟 步驟 B：修正時區問題，計算「今日看車」的預約數量
            // 取得本地時間的 YYYY-MM-DD
            const localDate = new Date();
            const yyyy = localDate.getFullYear();
            const mm = String(localDate.getMonth() + 1).padStart(2, '0');
            const dd = String(localDate.getDate()).padStart(2, '0');
            const todayStr = `${yyyy}-${mm}-${dd}`;

            // 篩選出預約日期包含今天日期的資料筆數
            viewStatusStats.value.todayNew = fullAppointments.filter(appt => {
                return appt.apptTime && appt.apptTime.includes(todayStr);
            }).length;

            // 計算其他狀態的總數
            viewStatusStats.value.pending = fullAppointments.filter(c => c.status?.dbCode === 'PENDING').length;
            viewStatusStats.value.confirmed = fullAppointments.filter(c => c.status?.dbCode === 'CONFIRMED').length;
            viewStatusStats.value.completed = fullAppointments.filter(c => c.status?.dbCode === 'COMPLETED').length;
            viewStatusStats.value.cancelled = fullAppointments.filter(c => c.status?.dbCode === 'CANCELLED').length;

            // 🌟 步驟 C：擷取最新 3 筆看車名單（兼顧「待處理優先」與「ID最新」）

            // 1. 先把「待處理」和「其他狀態」分開
            const pendingAppts = fullAppointments.filter(c => c.status?.dbCode === 'PENDING');
            const otherAppts = fullAppointments.filter(c => c.status?.dbCode !== 'PENDING');

            // 2. 將「待處理」依照 apptId 由大到小排序（確保出來的待處理是最新的）
            pendingAppts.sort((a, b) => b.apptId - a.apptId);

            // 3. 將「其他狀態」（完成、取消等）依照 apptId 由大到小排序（號碼越大越新）
            otherAppts.sort((a, b) => b.apptId - a.apptId);

            // 4. 依據待處理的數量動態組合
            if (pendingAppts.length >= 3) {
                // 如果待處理有 3 筆以上，就只抓最新編號的前 3 筆待處理
                recentAppointments.value = pendingAppts.slice(0, 3);
            } else {
                // 如果待處理不夠 3 筆，就用編號最新（無論取消或完成）的其他資料補滿到 3 筆
                recentAppointments.value = [...pendingAppts, ...otherAppts].slice(0, 3);
            }
        }
        // 成交記錄的子狀態統計
        if (saleRes.data) {
            saleStatusStats.value.pending = saleRes.data.filter(c => c.payStatus?.dbCode === 'PENDING').length;
            saleStatusStats.value.paid = saleRes.data.filter(c => c.payStatus?.dbCode === 'PAID').length;
            saleStatusStats.value.cancelled = saleRes.data.filter(c => c.payStatus?.dbCode === 'CANCELLED').length;
        }

    } catch (error) {
        console.error("抓取統計資料失敗", error);
    }
};

// 2. 計算成交率 (成交件數 / 總預約資料)
const conversionRate = computed(() => {
    if (totalViews.value === 0) return 0;// 防止總預約數為 0 時，除 crimson 產生 NaN（非數字）錯誤
    // 計算公式：(成交件數 / 總預約數) * 100
    const rate = (totalSales.value - saleStatusStats.value.cancelled) / totalViews.value * 100;
    return parseFloat(rate.toFixed(1));// 四捨五入保留小數點後第一位
});

// 計算預約到店率 (已完成看車 / 總預約)
const attendanceRate = computed(() => {
    if (totalViews.value === 0) return 0;
    // status.dbCode 為 'COMPLETED' 代表客戶已實際到店看車
    const completedViews = viewStatusStats.value.completed;
    const rate = (completedViews / totalViews.value) * 100;
    return parseFloat(rate.toFixed(1));
});
// 網頁一掛載完成 (DOM 準備好) 就立刻執行 fetchStats 抓資料
onMounted(() => {
    fetchStats();
});

// 跳轉邏輯 管理員點擊卡片時，觸發路由調轉到各個詳細的管理總表頁面
const gocar = () => router.push('/usedcargetall');
const gosale = () => router.push('/salesrecordgetall');
const goview = () => router.push('/viewingappointmentgetall');

</script>

<template>
    <div class="bg-light min-vh-100 py-4">
        <div class="container">

            <div
                class="d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center mb-4 border-bottom pb-3 g-3">
                <div>
                    <h2 class="fw-bold mb-1 text-dark">二手車管理系統</h2>
                    <p class="text-muted small mb-0">歡迎回來！以下是今日系統營運概況與核心數據統計。</p>
                </div>

                <router-link to="/usedcarshop"
                    class="btn btn-dark btn-sm px-4 py-2 rounded-pill shadow-sm fw-medium mt-2 mt-md-0">
                    <font-awesome-icon icon="shop" class="me-2" />進入客戶商城
                </router-link>
            </div>

            <div class="row row-cols-1 row-cols-md-3 g-4 mb-4">

                <div class="col">
                    <div class="card h-100 bg-white shadow-sm border border-light-subtle rounded-3" @click="gocar"
                        role="button">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-start mb-3">
                                <div class="bg-light text-dark p-2 rounded-3 d-inline-flex align-items-center justify-content-center"
                                    style="width: 42px; height: 42px;">
                                    <font-awesome-icon icon="car" class="fs-5" />
                                </div>
                                <span
                                    class="badge bg-light text-secondary border px-2 py-1 rounded-pill small fw-normal">車輛統計</span>
                            </div>
                            <div class="d-flex justify-content-between align-items-end">
                                <div>
                                    <h6 class="text-secondary fw-medium mb-1 small">在庫車輛總數</h6>
                                    <h3 class="fw-bold text-dark mb-0">{{ totalCars - carStatusStats.sold }} <small
                                            class="fs-6 fw-normal text-muted">台</small></h3>
                                </div>
                            </div>
                            <hr class="my-3 border-light-subtle">
                            <div class="d-flex justify-content-between small fw-medium">
                                <span class="text-warning"><font-awesome-icon icon="screwdriver-wrench" class="me-1" />
                                    待整備：{{ carStatusStats.removed }}</span>
                                <span class="text-success"><font-awesome-icon icon="circle-check" class="me-1" /> 已上架：{{
                                    carStatusStats.active }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col">
                    <div class="card h-100 bg-white shadow-sm border border-light-subtle rounded-3" @click="goview"
                        role="button">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-start mb-3">
                                <div class="bg-primary-subtle text-primary p-2 rounded-3 d-inline-flex align-items-center justify-content-center"
                                    style="width: 42px; height: 42px;">
                                    <font-awesome-icon icon="calendar-check" class="fs-5" />
                                </div>
                                <span
                                    class="badge bg-light text-secondary border px-2 py-1 rounded-pill small fw-normal">看車預約</span>
                            </div>
                            <div class="d-flex justify-content-between align-items-end">
                                <div>
                                    <h6 class="text-secondary fw-medium mb-1 small">預約資料總量</h6>
                                    <h3 class="fw-bold text-dark mb-0">{{ totalViews }} <small
                                            class="fs-6 fw-normal text-muted">筆</small></h3>
                                </div>
                            </div>
                            <hr class="my-3 border-light-subtle">
                            <div class="d-flex justify-content-between small fw-medium">
                                <span class="text-danger"><font-awesome-icon icon="bell" class="me-1" /> 今日看車：{{
                                    viewStatusStats.todayNew }}</span>
                                <span class="text-dark"><font-awesome-icon icon="comment-dots" class="me-1" /> 待聯繫：{{
                                    viewStatusStats.pending }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col">
                    <div class="card h-100 bg-white shadow-sm border border-light-subtle rounded-3" @click="gosale"
                        role="button">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-start mb-3">
                                <div class="bg-success-subtle text-success p-2 rounded-3 d-inline-flex align-items-center justify-content-center"
                                    style="width: 42px; height: 42px;">
                                    <font-awesome-icon icon="handshake" class="fs-5" />
                                </div>
                                <span
                                    class="badge bg-light text-secondary border px-2 py-1 rounded-pill small fw-normal">成交概況</span>
                            </div>
                            <div class="d-flex justify-content-between align-items-end">
                                <div>
                                    <h6 class="text-secondary fw-medium mb-1 small">累計成交件數</h6>
                                    <h3 class="fw-bold text-dark mb-0">{{ totalSales - saleStatusStats.cancelled
                                        }} <small class="fs-6 fw-normal text-muted">件</small></h3>
                                </div>
                            </div>
                            <hr class="my-3 border-light-subtle">
                            <div class="d-flex justify-content-between small fw-medium">
                                <span class="text-success"><font-awesome-icon icon="arrow-trend-up" class="me-1" />
                                    本週穩定成交</span>
                                <span class="text-muted">目標達成率：85%</span>
                            </div>
                        </div>
                    </div>
                </div>

            </div>

            <div class="row g-4">

                <div class="col-12 col-lg-4">
                    <div class="card h-100 bg-white shadow-sm border border-light-subtle rounded-3">
                        <div class="card-header bg-transparent border-0 pt-4 px-4 pb-0">
                            <h5 class="fw-bold text-dark mb-1">
                                <font-awesome-icon icon="filter" class="text-primary me-2" />營運概況
                            </h5>
                            <p class="text-muted small mb-0">從預約到到店看車的轉換效率</p>
                        </div>
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="small fw-medium text-dark">到店看車率</span>
                                <span class="fw-bold text-primary">{{ attendanceRate }}%</span>
                            </div>
                            <div class="progress mb-4 rounded-pill shadow-sm" style="height: 10px;">
                                <div class="progress-bar bg-primary rounded-pill" role="progressbar"
                                    :style="{ width: attendanceRate + '%' }"></div>
                            </div>

                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="small fw-medium text-dark">實際成交率</span>
                                <span class="fw-bold text-success">{{ conversionRate }}%</span>
                            </div>
                            <div class="progress mb-4 rounded-pill shadow-sm" style="height: 10px;">
                                <div class="progress-bar bg-success rounded-pill" role="progressbar"
                                    :style="{ width: conversionRate + '%' }"></div>
                            </div>

                            <div class="row text-center mt-3 g-2">
                                <div class="col-4 border-end">
                                    <div class="text-muted small">預約</div>
                                    <div class="fw-bold text-dark">{{ totalViews }}</div>
                                </div>
                                <div class="col-4 border-end">
                                    <div class="text-muted small">到店</div>
                                    <div class="fw-bold text-primary">{{ viewStatusStats.completed }}</div>
                                </div>
                                <div class="col-4">
                                    <div class="text-muted small">成交</div>
                                    <div class="fw-bold text-success">{{ totalSales - saleStatusStats.cancelled }}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col-12 col-lg-8">
                    <div class="card h-100 bg-white shadow-sm border border-light-subtle rounded-3">
                        <div
                            class="card-header bg-transparent border-0 pt-4 px-4 pb-2 d-flex justify-content-between align-items-center">
                            <div>
                                <h5 class="fw-bold text-dark mb-1"><font-awesome-icon icon="list-check"
                                        class="text-primary me-2" />最新預約看車名單</h5>
                                <p class="text-muted small mb-0">系統最新進來的預約，請管理員盡速聯繫</p>
                            </div>
                            <button @click="goview" class="btn btn-link text-dark btn-sm text-decoration-none fw-bold">
                                查看全部 <font-awesome-icon icon="chevron-right" class="ms-1 small" />
                            </button>
                        </div>

                        <div class="card-body px-4 pb-4 pt-2">
                            <div v-if="recentAppointments.length > 0" class="table-responsive">
                                <table class="table align-middle table-hover mb-0">
                                    <thead class="table-light text-secondary small">
                                        <tr>
                                            <th class="fw-semibold py-2">客戶姓名</th>
                                            <th class="fw-semibold py-2">聯絡電話</th>
                                            <th class="fw-semibold py-2">預約日期</th>
                                            <th class="fw-semibold py-2">狀態</th>
                                            <th class="fw-semibold py-2 text-end">操作</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="appt in recentAppointments" :key="appt.apptId">
                                            <td class="py-3">
                                                <div class="fw-bold text-dark">{{ appt.custName || '線上客戶' }}</div>
                                            </td>
                                            <td class="text-muted small py-3">{{ appt.custPhone || '未提供' }}</td>
                                            <td class="small fw-medium text-dark py-3">{{ appt.apptTime || '時間未定' }}
                                            </td>
                                            <td class="py-3">
                                                <span class="badge rounded-pill px-3 py-1.5 small"
                                                    :class="appt.status?.dbCode === 'PENDING' ? 'bg-warning-subtle text-warning' : 'bg-secondary-subtle text-secondary'">
                                                    {{ appt.status?.description || '未知' }}
                                                </span>
                                            </td>
                                            <td class="text-end py-3">
                                                <button @click.stop="openEditModal(appt)"
                                                    class="btn btn-outline-dark btn-sm rounded-pill px-3 py-1"
                                                    style="font-size: 0.75rem;">
                                                    辦理
                                                </button>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>

                            <div v-else class="text-center py-5 text-muted small">
                                <font-awesome-icon icon="folder-open" class="fs-2 mb-2 text-secondary opacity-50" />
                                <p class="mb-0">目前暫無看車預約資料</p>
                            </div>
                        </div>
                    </div>
                </div>

            </div>

        </div>
    </div>
    <ViewingappointmentEditView :isVisible="showEditModal" :editData="appointmentToEdit" @close="showEditModal = false"
        @refresh="fetchStats" @sync="handleCalendarSync" />
</template>