<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import usedCarApi from '@/api/usedcar/usedCarApi';
import api from '@/api/index.js';

// 🌟 嚴格遵循 v2 規範：引入共用元件
import AlertToast from '@/components/common/AlertToast.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import BackButton from '@/components/common/BackButton.vue';

const route = useRoute();
const router = useRouter();

const toast = ref(null);
const isSubmitting = ref(false);

// 預約資料物件
const apptData = ref({
    usedCarId: null,
    custId: 1,
    apptTime: '',
    status: 'PENDING',
    locationId: null,
    message: '',
    notes: ''
});

// 畫面上輔助顯示的變數
const locationName = ref('載入中...');
const carPreview = ref({
    name: '載入中...',
    image: '',
    price: ''
});

// 設定預約時間最小值
const minDateTime = computed(() => {
    const now = new Date();
    now.setHours(now.getHours() + 1);
    now.setMinutes(0);
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
});

onMounted(async () => {
    if (route.query.carId) {
        const carId = parseInt(route.query.carId);
        apptData.value.usedCarId = carId;

        try {
            const response = await usedCarApi.getOneDetail(carId);
            if (response.data) {
                apptData.value.locationId = response.data.locationId;
                locationName.value = response.data.locationName || '現場洽詢';

                const year = response.data.manufactureYear ? `${response.data.manufactureYear}年` : '';
                const brand = response.data.brandName || '';
                const model = response.data.modelName || '';
                carPreview.value.name = `${year} ${brand} ${model}`.trim() || '優質中古車';
                carPreview.value.image = response.data.vehicleImgUrl || 'https://placehold.co/600x400?text=OneRent+Car';
                carPreview.value.price = response.data.askingPrice ? `${parseFloat((response.data.askingPrice / 10000).toFixed(1))} 萬` : '面議';
            }
        } catch (error) {
            console.error('資料載入失敗:', error);
        }
    }
});

const submitForm = async () => {
    if (!apptData.value.apptTime) {
        toast.value.show('請選擇預約時間', 'warning');
        return;
    }

    isSubmitting.value = true;
    try {
        const response = await api.post('/viewingappointment/insert', apptData.value);
        if (response.status === 201 || response.status === 200) {
            toast.value.show('預約資料送出成功！', 'success', 1000);

            // 🌟 關鍵修正：跳轉到「成功結果頁」
            setTimeout(() => {
                router.push('/usedcar/success');
            }, 1000);
        }
    } catch (error) {
        console.error('預約失敗:', error);
        toast.value.show('系統繁忙，請稍後再試。', 'danger');
    } finally {
        isSubmitting.value = false;
    }
};
</script>

<template>
    <div class="bg-light min-vh-100 py-5">

        <AlertToast ref="toast" />
        <LoadingSpinner :isLoading="isSubmitting" overlay text="正在處理您的預約申請..." />

        <div class="container">
            <div class="row justify-content-center">
                <div class="col-12 col-md-9 col-lg-6">

                    <div class="d-flex justify-content-between position-relative align-items-center mb-5 px-5">
                        <div class="position-absolute top-50 start-0 end-0 translate-middle-y bg-secondary-subtle"
                            style="height: 2px; z-index: 0;"></div>

                        <div class="text-center position-relative" style="z-index: 1;">
                            <div class="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center mx-auto mb-2 shadow-sm"
                                style="width: 36px; height: 36px;">
                                <font-awesome-icon icon="user-edit" class="small" />
                            </div>
                            <span class="fw-bold text-primary small">1. 輸入資訊</span>
                        </div>

                        <div class="text-center position-relative" style="z-index: 1;">
                            <div class="rounded-circle bg-white text-secondary border border-secondary-subtle d-flex align-items-center justify-content-center mx-auto mb-2"
                                style="width: 36px; height: 36px;">
                                <font-awesome-icon icon="check" class="small" />
                            </div>
                            <span class="text-muted small">2. 預約完成</span>
                        </div>
                    </div>

                    <div class="card border border-light-subtle rounded-4 overflow-hidden bg-white shadow-sm">

                        <div class="card-header bg-primary text-white py-4 border-0">
                            <div class="d-flex align-items-center px-2">
                                <font-awesome-icon icon="calendar-plus" class="fs-3 me-3 text-white-50" />
                                <div>
                                    <h4 class="mb-0 fw-bold text-white">填寫預約資料</h4>
                                </div>
                            </div>
                        </div>

                        <div class="card-body p-4 p-md-5">
                            <form @submit.prevent="submitForm">

                                <div
                                    class="row g-0 border border-light-subtle rounded-3 p-3 bg-light mb-4 align-items-center">
                                    <div class="col-4 col-sm-3">
                                        <div class="ratio ratio-4x3 rounded-2 overflow-hidden bg-secondary-subtle">
                                            <img :src="carPreview.image" class="img-fluid object-fit-cover" alt="車輛圖片">
                                        </div>
                                    </div>
                                    <div class="col-8 col-sm-9 ps-3">
                                        <div class="text-primary fw-bold small mb-1">您選擇的車輛</div>
                                        <h5 class="fw-bold text-dark mb-1 fs-6 text-truncate">{{ carPreview.name }}</h5>
                                        <div class="d-flex justify-content-between align-items-center mt-2">
                                            <span
                                                class="badge bg-secondary-subtle text-secondary rounded-pill font-monospace fw-normal">#{{
                                                    apptData.usedCarId }}</span>
                                            <span class="text-danger fw-bold fs-6">{{ carPreview.price }}</span>
                                        </div>
                                    </div>
                                </div>

                                <div class="mb-4">
                                    <label class="form-label fw-bold text-secondary small">賞車地點</label>
                                    <div class="input-group input-group-lg">
                                        <span
                                            class="input-group-text bg-light border-light-subtle text-primary rounded-start-pill px-3">
                                            <font-awesome-icon icon="location-dot" class="fs-6" />
                                        </span>
                                        <input type="text"
                                            class="form-control bg-light text-muted border-light-subtle fw-medium rounded-end-pill fs-6"
                                            :value="locationName" readonly>
                                    </div>
                                </div>

                                <hr class="my-4 border-secondary-subtle opacity-25">

                                <div class="mb-4">
                                    <label class="form-label fw-bold text-dark fs-6 mb-2">預約賞車時間 <span
                                            class="text-danger">*</span></label>
                                    <div class="input-group input-group-lg">
                                        <span
                                            class="input-group-text bg-white border-secondary-subtle text-primary rounded-start-pill px-3">
                                            <font-awesome-icon icon="calendar-days" class="fs-6" />
                                        </span>
                                        <input type="datetime-local" v-model="apptData.apptTime"
                                            class="form-control border-secondary-subtle rounded-end-pill fw-medium fs-6"
                                            required :min="minDateTime">
                                    </div>
                                    <div class="form-text text-muted ps-2 pt-1">
                                        <font-awesome-icon icon="circle-info" class="me-1 small text-primary" />
                                        請選擇您方便看車的時間，送出後我們將有專人致電與您確認詳細行程。
                                    </div>
                                </div>

                                <div class="mb-4">
                                    <label class="form-label fw-bold text-dark fs-6 mb-2">預約訊息 (選填)</label>
                                    <textarea v-model="apptData.message"
                                        class="form-control border-secondary-subtle rounded-4 p-3 fs-6" rows="4"
                                        placeholder="例如：我想詢問實際車況、是否可試駕..."></textarea>
                                </div>

                                <div class="d-grid gap-2 pt-3 border-top border-light-subtle mt-4">
                                    <button type="submit"
                                        class="btn btn-primary btn-lg rounded-pill fw-bold py-2.5 fs-6 shadow-sm"
                                        :disabled="isSubmitting">
                                        <font-awesome-icon icon="calendar-check" class="me-2" />
                                        確認送出預約申請
                                    </button>
                                    <BackButton label="取消返回" class="mt-2 text-center" />
                                </div>
                            </form>
                        </div>
                    </div>

                    <div class="text-center text-muted small pt-4 opacity-50">
                        &copy; OneRent 輕鬆租車 · 自在出行. All rights reserved.
                    </div>

                </div>
            </div>
        </div>
    </div>
</template>