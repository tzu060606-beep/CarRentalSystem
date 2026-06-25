<script setup>
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router'; // 🌟 引入 router 處理跳轉
import usedCarApi from '@/api/usedcar/usedCarApi';
import BaseModal from '@/components/common/BaseModal.vue';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import AlertToast from '@/components/common/AlertToast.vue';

const router = useRouter(); // 🌟 初始化 router

const props = defineProps({
    isVisible: Boolean,
    editData: Object, // 從父組件傳入點選的資料
    todayStr: String
});

const emit = defineEmits(['close', 'refresh']);
const loading = ref(false);
const toast = ref(null); // Toast 實例

const editForm = ref({
    usedCarId: '',
    askingPrice: 0,
    status: '',
    conditionDesc: '',
    listDate: '',
    expireDate: ''
});

// 監聽傳入的資料，當父組件開啟時填入內容
watch(() => props.editData, (newVal) => {
    if (newVal) {
        Object.assign(editForm.value, {
            ...newVal,
            status: newVal.status?.dbCode || newVal.status
        });
    }
}, { immediate: true });

// 🌟 核心：提交前的權限與 Token 檢查
const submitUpdate = async () => {
    // 1. 檢查 Token 是否還在 (避免閒置過久或換帳號)
    const currentToken = localStorage.getItem('token');
    if (!currentToken) {
        alert('登入時效已過，請重新登入');
        router.push('/employee/login');
        return;
    }

    try {
        loading.value = true;

        // 2. 格式化 Payload
        const id = editForm.value.usedCarId;
        const payload = {
            ...editForm.value,
            // 確保 status 傳給後端的是純字串代碼 (如: 'ACTIVE')
            status: typeof editForm.value.status === 'object'
                ? editForm.value.status.dbCode
                : editForm.value.status
        };

        // 3. 呼叫 API (usedCarApi 應該已經配置好 axios 攔截器自動帶 Token)
        await usedCarApi.update(id, payload);

        toast.value.show('資料更新成功！', 'success');

        setTimeout(() => {
            emit('refresh');
            emit('close');
        }, 800);

    } catch (error) {
        // 🌟 處理 401 錯誤 (Token 過期或無效)
        if (error.response?.status === 401) {
            // ... 原有的 401 處理 ...
        } else if (error.response?.status === 500 || error.response?.status === 400) {
            // 🌟 這裡動態抓取後端的 message
            // 假設後端回傳格式是 { message: "資料驗證失敗: 截止日期錯誤" }
            const backendMsg = error.response.data?.message || '資料驗證失敗，請檢查欄位格式';

            // 讓 Toast 顯示具體的錯誤原因
            toast.value.show(backendMsg, 'danger');

            // 💡 进階：如果是日期錯誤，我們可以把輸入框標示為紅色
            if (backendMsg.includes('日期')) {
                // 你可以在這裡增加一個變數來控制 UI 樣式
                // dateError.value = true;
            }
        }
        if (error.response?.status === 401) {
            toast.value.show('連線逾時，請重新登入', 'danger');
            router.push('/login');
        } else {
            const msg = error.response?.data?.message || '更新失敗';
            toast.value.show(msg, 'danger');
        }
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <BaseModal :isVisible="isVisible" :title="'📝 修改車輛資訊 (編號: ' + editForm.usedCarId + ')'" size="lg"
        @close="$emit('close')">
        <template #body>
            <!-- 狀態反饋元件 -->
            <LoadingSpinner :isLoading="loading" overlay />
            <AlertToast ref="toast" />

            <form @submit.prevent>
                <div class="row g-4">
                    <!-- 預售價格 -->
                    <div class="col-md-6">
                        <label class="form-label fw-bold small text-secondary">
                            預售價格 (TWD) <span class="text-danger">*</span>
                        </label>
                        <div class="input-group input-group-lg">
                            <span class="input-group-text bg-light">$</span>
                            <input type="number" v-model="editForm.askingPrice" class="form-control border-2" />
                        </div>
                    </div>

                    <!-- 車輛狀態 -->
                    <div class="col-md-6">
                        <label class="form-label fw-bold small text-secondary">
                            車輛狀態 <span class="text-danger">*</span>
                        </label>
                        <select v-model="editForm.status" class="form-select form-select-lg border-2">
                            <option value="ACTIVE">上架</option>
                            <option value="SOLD">已售出</option>
                            <option value="REMOVED">下架</option>
                        </select>
                    </div>

                    <!-- 上架日期 -->
                    <div class="col-md-6">
                        <label class="form-label fw-bold small text-secondary">上架日期</label>
                        <input type="date" v-model="editForm.listDate" class="form-control" />
                    </div>

                    <!-- 刊登截止日 -->
                    <div class="col-md-6">
                        <label class="form-label fw-bold small text-secondary">刊登截止日</label>
                        <input type="date" v-model="editForm.expireDate" class="form-control"
                            :min="editForm.listDate" />
                    </div>



                    <!-- 車況描述 -->
                    <div class="col-12">
                        <label class="form-label fw-bold small text-secondary">車況詳細描述</label>
                        <textarea v-model="editForm.conditionDesc" rows="4" class="form-control bg-light"
                            placeholder="請輸入車況備註..."></textarea>
                    </div>
                </div>
            </form>
        </template>

        <template #footer>
            <div class="d-flex w-100 justify-content-between align-items-center">
                <!-- 修改為紅色的必填提醒 -->
                <span class="text-danger small fw-bold">* 為必填欄位</span>
                <div>
                    <button class="btn btn-link text-decoration-none text-secondary me-2"
                        @click="$emit('close')">取消</button>
                    <button class="btn btn-primary px-4 shadow-sm fw-bold" @click="submitUpdate" :disabled="loading">
                        <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                        確認修改資料
                    </button>
                </div>
            </div>
        </template>
    </BaseModal>
</template>