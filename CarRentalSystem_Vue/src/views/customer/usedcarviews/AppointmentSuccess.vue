<script setup>
import { useRouter } from 'vue-router';
// 🌟 嚴格遵循 v2 規範

const router = useRouter();

const goToMyAppointments = () => {
    router.push('/usedcar/appointment');
};
</script>

<template>
    <div class="bg-light min-vh-100 py-5">
        <div class="container">
            <div class="row justify-content-center">
                <div class="col-12 col-md-8 col-lg-6">

                    <div class="d-flex justify-content-between position-relative align-items-center mb-5 px-5">
                        <div class="position-absolute top-50 start-0 end-0 translate-middle-y bg-primary"
                            style="height: 2px; z-index: 0;"></div>

                        <div class="text-center position-relative" style="z-index: 1;">
                            <div class="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center mx-auto mb-2 shadow-sm"
                                style="width: 36px; height: 36px;">
                                <font-awesome-icon icon="user-edit" class="small" />
                            </div>
                            <span class="fw-bold text-primary small">1. 輸入資訊</span>
                        </div>

                        <div class="text-center position-relative" style="z-index: 1;">
                            <div class="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center mx-auto mb-2 shadow-sm"
                                style="width: 36px; height: 36px;">
                                <font-awesome-icon icon="check" class="small" />
                            </div>
                            <span class="fw-bold text-primary small">2. 預約完成</span>
                        </div>
                    </div>

                    <div class="card border border-light-subtle rounded-4 bg-white p-5 text-center shadow-sm">
                        <div class="text-success mb-4">
                            <font-awesome-icon icon="circle-check" style="font-size: 4.5rem;" />
                        </div>

                        <h3 class="fw-bold text-dark mb-3">預約資料已成功送出！</h3>

                        <p class="text-muted fs-6 mb-4 px-3">
                            我們已收到您的賞車預約申請。<br>
                            <span class="text-primary fw-medium">OneRent 客服專人將於 24 小時內致電與您確認詳細行程</span>，請保持電話暢通，謝謝您！
                        </p>

                        <hr class="my-4 border-secondary-subtle opacity-25">

                        <div class="d-grid gap-2 px-4">
                            <button @click="goToMyAppointments"
                                class="btn btn-primary btn-lg rounded-pill fw-bold py-2.5 fs-6 shadow-sm">
                                <font-awesome-icon icon="list-ul" class="me-2" />
                                前往我的預約清單查看狀態
                            </button>

                            <button type="button" @click="router.push('/usedcarshop')"
                                class="btn btn-outline-secondary btn-lg rounded-pill fw-medium py-2.5 fs-6 w-100 mt-2">
                                返回二手車商城
                            </button>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    </div>
</template>