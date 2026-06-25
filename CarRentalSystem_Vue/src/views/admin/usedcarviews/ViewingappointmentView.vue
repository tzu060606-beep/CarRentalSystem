<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

import viewingappointmentApi from '@/api/usedcar/viewingappointmentApi';
import usedCarApi from '@/api/usedcar/usedCarApi';
// 請確保此路徑正確指向你的 Google Calendar Composable
import { useGoogleCalendar } from '@/components/admin/usedcar/googleCalendar.js';

// 共用元件
import AddModal from './ViewingappointmentAddView.vue';
import EditModal from './ViewingappointmentEditView.vue';
import DetailModal from './ViewingappointmentDetailView.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import AlertToast from '@/components/common/AlertToast.vue';
import SearchBar from '@/components/common/SearchBar.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';

// 狀態管理
const apptList = ref([]);
const carList = ref([]);
const loading = ref(false);
const searchQuery = ref('');
const searchType = ref('apptId');
const toast = ref(null);

const addModal = ref(false);
const editModal = ref(false);
const detailModal = ref(false);
const selectedAppt = ref(null);
const todayStr = ref(new Date().toISOString().slice(0, 19));

// Google Calendar 整合 - 使用官方 Composable
const calendarLoading = ref(false);

const loadData = async () => {
    loading.value = true;
    try {
        const [apptRes, carRes] = await Promise.all([
            viewingappointmentApi.getAll(),
            usedCarApi.getAll()
        ]);
        apptList.value = apptRes.data.data || apptRes.data;
        carList.value = carRes.data.data || carRes.data;
    } catch (error) {
        toast.value?.show('資料讀取失敗', 'danger');
    } finally {
        loading.value = false;
    }
};

const openDetail = async (id) => {
    try {
        loading.value = true;
        const res = await viewingappointmentApi.getById(id);
        selectedAppt.value = res.data.data || res.data;
        detailModal.value = true;
    } catch (error) {
        toast.value?.show('無法取得預約資料', 'warning');
    } finally {
        loading.value = false;
    }
};

const handleEdit = async (id) => {
    try {
        loading.value = true;
        const res = await viewingappointmentApi.getById(id);
        selectedAppt.value = res.data.data || res.data;
        editModal.value = true;
    } catch (error) {
        toast.value?.show('取得資料失敗', 'danger');
    } finally {
        loading.value = false;
    }
};



// 同步邏輯 (最簡化且正確)
// 修改後的同步邏輯
const syncToGoogleCalendar = async (appt) => {

    // ⭐ 先存資料
    pendingSyncData = appt;

    // ⭐ 直接開授權 popup
    openGoogleAuthPopup();
};

let pendingSyncData = null; // 🔥 記住剛剛要同步的資料

const openGoogleAuthPopup = () => {
    // 記得要加上 width/height 確保它是彈出視窗 (Popup)
    const url = "http://localhost:8081/oauth2/authorization/google";
    const authWindow = window.open(url, "GoogleAuth", "width=600,height=700,menubar=no,toolbar=no,location=no,status=no");

    // 監聽來自彈出視窗的訊息
    const messageHandler = async (event) => {
        console.log("收到來自子視窗的訊息:", event.data);
        // 確保來源正確
        if (event.origin !== "http://localhost:8081") return;

        // 如果是授權成功
        if (event.data === "GOOGLE_AUTH_SUCCESS") {
            console.log("授權訊號已捕捉，準備執行同步..."); // 確認是否走到這裡
            window.removeEventListener("message", messageHandler);
            authWindow.close();

            // ⭐ 授權成功後，立即發送 API 請求同步
            if (pendingSyncData) {
                try {
                    loading.value = true;
                    const start = new Date(pendingSyncData.apptTime);
                    const end = new Date(start.getTime() + 60 * 60 * 1000);

                    await axios.post("http://localhost:8081/api/calendar/create", payload, {
                        withCredentials: true
                    });

                    toast.value?.show('已成功同步至 Google 行事曆！', 'success');
                } catch (err) {
                    // ⭐ 這裡判斷 401
                    if (err.response && err.response.status === 401) {
                        console.warn("Token 過期或未授權，準備重新導向...");
                        toast.value?.show('授權已過期，請再次點擊同步按鈕', 'warning');
                        // 這裡可以選擇不自動開視窗，而是提示使用者點擊重試
                    } else {
                        console.error("同步失敗", err);
                        toast.value?.show('同步失敗，請稍後再試', 'danger');
                    }
                } finally {
                    loading.value = false;
                    pendingSyncData = null;
                }
            }
        }
    };

    window.addEventListener("message", messageHandler);
};

const filteredAppts = computed(() => {
    if (!searchQuery.value) return apptList.value;
    return apptList.value.filter(item => {
        const content = searchType.value === 'status'
            ? `${item.status?.dbCode || item.status}`
            : String(item[searchType.value] || '');
        return content.toLowerCase().includes(searchQuery.value.toLowerCase());
    });
});

onMounted(loadData);
</script>

<template>
    <div class="container py-4">
        <LoadingSpinner :isLoading="loading || calendarLoading" overlay />
        <AlertToast ref="toast" />

        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h2 class="mb-1 fw-bold">看車預約管理</h2>
                <small class="text-secondary">追蹤客戶看車預約狀態與時間安排</small>
            </div>
            <button @click="addModal = true" class="btn btn-primary px-4 shadow-sm">
                <font-awesome-icon icon="calendar-plus" class="me-2" />新增預約
            </button>
        </div>

        <div class="card shadow-sm mb-4">
            <div class="card-body p-3">
                <div class="row g-3">
                    <div class="col-md-3">
                        <select v-model="searchType" class="form-select">
                            <option value="apptId">預約編號</option>
                            <option value="custId">客戶編號</option>
                            <option value="usedCarId">二手車編號</option>
                            <option value="status">處理狀態</option>
                        </select>
                    </div>
                    <div class="col-md-9">
                        <SearchBar v-model="searchQuery" :showButton="false" />
                    </div>
                </div>
            </div>
        </div>

        <div class="card border-0 shadow-sm overflow-hidden">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">預約 ID</th>
                            <th>預約時間</th>
                            <th>車輛編號</th>
                            <th>客戶編號</th>
                            <th>預約地點</th>
                            <th>狀態</th>
                            <th class="text-center pe-4">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="appt in filteredAppts" :key="appt.apptId" @click="openDetail(appt.apptId)"
                            role="button">
                            <td class="ps-4 fw-bold">#{{ appt.apptId }}</td>
                            <td>
                                <font-awesome-icon icon="clock" class="me-2 text-primary" />
                                {{ appt.apptTime }}
                            </td>
                            <td><span class="badge bg-light text-dark border">CAR-{{ appt.usedCarId }}</span></td>
                            <td class="text-muted">CUST-{{ appt.custId }}</td>
                            <td>{{ appt.locationName || '未設定' }}</td>
                            <td>
                                <StatusBadge :status="appt.status?.dbCode || appt.status" :map="{
                                    PENDING: { label: '待處理', variant: 'warning' },
                                    CONFIRMED: { label: '已確認', variant: 'success' },
                                    COMPLETED: { label: '已完成', variant: 'primary' },
                                    CANCELLED: { label: '已取消', variant: 'danger' }
                                }" />
                            </td>
                            <td @click.stop class="text-center pe-4">
                                <button @click="handleEdit(appt.apptId)" class="btn btn-sm btn-outline-primary">
                                    <font-awesome-icon icon="edit" class="me-2" />編輯
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <AddModal :isVisible="addModal" :carList="carList" @close="addModal = false" @refresh="loadData" />
        <EditModal :isVisible="editModal" :editData="selectedAppt" @close="editModal = false" @refresh="loadData"
            @sync="syncToGoogleCalendar" />
        <DetailModal :isVisible="detailModal" :selectedAppt="selectedAppt" @close="detailModal = false"
            @sync="syncToGoogleCalendar" />
    </div>
</template>