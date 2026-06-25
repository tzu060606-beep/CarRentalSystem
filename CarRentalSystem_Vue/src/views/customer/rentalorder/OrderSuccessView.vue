<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goToMyOrders = () => {
  router.push('/customer/member/order');
};
</script>

<template>
  <main class="bg-light min-vh-100 py-5 rental-front-page">
    <div class="container">
      <div class="d-flex flex-wrap align-items-center justify-content-center gap-3 mb-5">
        <div class="d-flex align-items-center gap-2 fw-bold text-primary">
          <span class="badge rounded-pill bg-primary text-white">1</span>
          <span>選擇車輛</span>
        </div>
        <div class="border-top flex-grow-1 d-none d-md-block"></div>
        <div class="d-flex align-items-center gap-2 fw-bold text-primary">
          <span class="badge rounded-pill bg-primary text-white">2</span>
          <span>確認訂單</span>
        </div>
        <div class="border-top flex-grow-1 d-none d-md-block"></div>
        <div class="d-flex align-items-center gap-2 fw-bold text-primary">
          <span class="badge rounded-pill bg-primary text-white">3</span>
          <span>完成預約</span>
        </div>
      </div>

      <section class="card rounded-3 shadow-sm border mx-auto col-lg-7 col-xl-6">
        <div class="card-body p-5 text-center">
          <div class="display-4 text-success mb-4">
            <i class="fa-solid fa-circle-check"></i>
          </div>
          <h1 class="h3 fw-bold text-primary mb-3">訂單資料已完成</h1>
          <p class="text-secondary mb-4">
            我們已收到您的租車訂單，後續可於訂單頁確認預約狀態。
          </p>

          <button class="btn btn-primary px-4 fw-bold" type="button" @click="goToMyOrders">
            查看我的訂單
          </button>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.rental-front-page {
  padding-top: calc(92px + 3rem) !important;
}

@media (max-width: 991.98px) {
  .rental-front-page {
    padding-top: calc(76px + 3rem) !important;
  }
}
</style>

