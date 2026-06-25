<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const heroTags = ['甲租乙還', '日租', '長租'];

const planCards = [
  {
    icon: ['fas', 'sun'],
    title: '日租車',
    description:
      '適合短期旅遊、臨時代步、假日行程與商務出差。可依取車與還車時間查詢可租車輛，線上挑選車款並完成預約。',
    actionLabel: '立即搜尋車輛',
    accentClass: 'intro-card-day',
    action: () => goDailyRental(),
  },
  {
    icon: ['fas', 'moon'],
    title: '長租車',
    description:
      '適合長期代步、企業用車與專案用車。可依車型與租期選擇合適車款，降低自行購車與維護壓力。',
    actionLabel: '立即搜尋車輛',
    accentClass: 'intro-card-dusk',
    action: () => goLongTermRental(),
  },
];

const serviceFeatures = [
  { icon: ['fas', 'magnifying-glass'], title: '依時間查詢車輛' },
  { icon: ['fas', 'location-dot'], title: '多據點取還車' },
  { icon: ['fas', 'route'], title: '支援甲租乙還' },
  { icon: ['fas', 'sliders'], title: '多樣篩選條件' },
  { icon: ['fas', 'cart-shopping'], title: '線上建立訂單' },
  { icon: ['fas', 'wallet'], title: '支援多種支付' },
  { icon: ['fas', 'user'], title: '會員訂單管理' },
  { icon: ['fas', 'boxes-stacked'], title: '顯示可預約車款' },
];

const dailySteps = [
  '選擇取還車地點與時間',
  '篩選並挑選車款',
  '填寫承租資料',
  '選擇押金付款方式並送出訂單',
  '到據點取車',
];

const longTermSteps = [
  '選擇長租模式與租期',
  '查看月租車款',
  '填寫承租資料',
  '確認訂單並支付押金',
  '到據點取車',
];

const goDailyRental = () => {
  router.push({
    path: '/orders/custbooking',
    query: { rentalMode: 'daily' },
  });
};

const goLongTermRental = () => {
  router.push({
    path: '/orders/custbooking',
    query: { rentalMode: 'longTerm' },
  });
};

const goMyOrders = () => {
  router.push('/customer/member/order');
};
</script>

<template>
  <main class="rental-daily-long-term-page min-vh-100">
    <section class="rental-intro-hero position-relative overflow-hidden">
      <div class="rental-intro-ring rental-intro-ring-one"></div>
      <div class="rental-intro-ring rental-intro-ring-two"></div>
      <div class="rental-intro-horizon"></div>
      <div class="rental-intro-route-line"></div>

      <div class="container position-relative z-1 py-5">
        <div class="row justify-content-center text-center">
          <div class="col-12 col-lg-9 col-xl-8">
            <div class="d-flex justify-content-center flex-wrap gap-2 mb-4">
              <span v-for="tag in heroTags" :key="tag" class="badge rounded-pill rental-intro-glass-badge">
                {{ tag }}
              </span>
            </div>

            <p class="rental-intro-eyebrow mb-3">OneRent 租車服務</p>
            <h1 class="rental-intro-hero-title fw-bold mb-4">
              日租與長租<br />
              <span>自在出行每一程</span>
            </h1>
            <p class="rental-intro-hero-copy lead mb-4 mx-auto">
              短期出遊、商務移動、長期用車，一次找到適合的車款。
            </p>

            <button
              type="button"
              class="btn btn-light btn-lg px-5 py-3 rounded-4 shadow-sm fw-bold rental-intro-main-cta"
              @click="goDailyRental"
            >
              <font-awesome-icon :icon="['fas', 'magnifying-glass']" class="me-2" />
              立即搜尋車輛
            </button>
          </div>
        </div>

        <div class="rental-intro-scroll-cue text-center">
          <div class="small fw-semibold mb-2">向下探索</div>
          <font-awesome-icon :icon="['fas', 'chevron-down']" />
        </div>
      </div>
    </section>

    <section class="container rental-intro-overlap-section">
      <div class="row g-4">
        <div v-for="plan in planCards" :key="plan.title" class="col-12 col-lg-6">
          <article class="intro-glass-card h-100 rounded-5 p-4 p-lg-5" :class="plan.accentClass">
            <div class="intro-card-icon d-inline-flex align-items-center justify-content-center rounded-4 mb-4">
              <font-awesome-icon :icon="plan.icon" size="xl" />
            </div>
            <h2 class="h3 fw-bold mb-3">{{ plan.title }}</h2>
            <p class="text-secondary lh-lg mb-4">{{ plan.description }}</p>
            <button type="button" class="btn btn-primary rounded-4 px-4 py-3 fw-bold" @click="plan.action">
              {{ plan.actionLabel }}
              <font-awesome-icon :icon="['fas', 'arrow-right']" class="ms-2" />
            </button>
          </article>
        </div>
      </div>
    </section>

    <section class="intro-feature-section py-5">
      <div class="container py-lg-4">
        <div class="text-center mb-5">
          <span class="badge rounded-pill text-bg-primary bg-opacity-10 text-primary mb-3">Service</span>
          <h2 class="h2 fw-bold mb-3">卓越租車體驗</h2>
          <p class="text-secondary mb-0">從搜尋、預約到取車，讓每一段出行都更清楚、更安心。</p>
        </div>

        <div class="row g-3 g-lg-4">
          <div v-for="feature in serviceFeatures" :key="feature.title" class="col-6 col-lg-3">
            <div class="intro-feature-card h-100 rounded-4 p-4 text-center">
              <div class="intro-feature-icon d-inline-flex align-items-center justify-content-center rounded-circle mb-3">
                <font-awesome-icon :icon="feature.icon" />
              </div>
              <div class="fw-semibold">{{ feature.title }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="container py-5">
      <div class="text-center mb-5">
        <h2 class="h2 fw-bold mb-3">輕鬆預約流程</h2>
        <div class="intro-title-rule mx-auto"></div>
      </div>

      <div class="row g-4 g-xl-5 position-relative">
        <div class="intro-center-divider d-none d-lg-block"></div>

        <div class="col-12 col-lg-6">
          <div class="intro-process-card rounded-5 p-4 p-lg-5 h-100">
            <h3 class="h4 fw-bold text-primary d-flex align-items-center gap-2 mb-4">
              <font-awesome-icon :icon="['fas', 'sun']" />
              日租流程
            </h3>
            <div class="intro-step-list">
              <div v-for="(step, index) in dailySteps" :key="step" class="intro-step-item">
                <div class="intro-step-number intro-step-day">{{ index + 1 }}</div>
                <div class="fw-semibold">{{ step }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 col-lg-6">
          <div class="intro-process-card rounded-5 p-4 p-lg-5 h-100">
            <h3 class="h4 fw-bold intro-dusk-text d-flex align-items-center gap-2 mb-4">
              <font-awesome-icon :icon="['fas', 'moon']" />
              長租流程
            </h3>
            <div class="intro-step-list intro-step-list-dusk">
              <div v-for="(step, index) in longTermSteps" :key="step" class="intro-step-item">
                <div class="intro-step-number intro-step-dusk">{{ index + 1 }}</div>
                <div class="fw-semibold">{{ step }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="intro-bottom-cta py-5">
      <div class="container">
        <div class="intro-glass-card rounded-5 p-4 p-lg-5 mx-auto text-center intro-bottom-card">
          <h2 class="h4 fw-bold mb-3">已完成預約？</h2>
          <p class="text-secondary mb-4">登入會員後可查看自己的租車訂單與預約狀態。</p>
          <button type="button" class="btn btn-primary btn-lg rounded-4 px-5 fw-bold" @click="goMyOrders">
            <font-awesome-icon :icon="['fas', 'clipboard-list']" class="me-2" />
            我的租車訂單
          </button>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.rental-daily-long-term-page {
  --intro-primary: #0059bb;
  --intro-primary-deep: #003885;
  --intro-sky: #dff3ff;
  --intro-sky-strong: #9ed8ff;
  --intro-sunset: #ffe2b8;
  --intro-sunset-strong: #ffc57a;
  --intro-ink: #172033;
  padding-top: 0;
  color: var(--intro-ink);
  background:
    radial-gradient(ellipse at 8% 5%, rgba(255, 248, 230, 0.28), transparent 30rem),
    radial-gradient(ellipse at 88% 15%, rgba(232, 245, 255, 0.32), transparent 32rem),
    linear-gradient(180deg, #ffffff 0%, #fbfdff 52%, #fffdf9 100%);
}

.rental-intro-hero {
  min-height: 680px;
  display: flex;
  align-items: center;
  padding: 6.5rem 0 8rem;
  background:
    linear-gradient(
      120deg,
      rgba(250, 253, 255, 0.99) 0%,
      rgba(229, 245, 255, 0.58) 34%,
      rgba(255, 247, 232, 0.62) 68%,
      rgba(255, 250, 246, 0.7) 100%
    ),
    radial-gradient(ellipse at 50% 42%, rgba(255, 255, 255, 0.9), transparent 19rem);
}

.rental-intro-hero::before,
.rental-intro-hero::after {
  content: "";
  position: absolute;
  inset: auto 0 0;
  height: 34%;
  pointer-events: none;
}

.rental-intro-hero::before {
  background:
    radial-gradient(120% 90% at 50% 100%, rgba(255, 255, 255, 0.9), transparent 58%),
    linear-gradient(180deg, transparent, rgba(255, 255, 255, 0.72));
}

.rental-intro-hero::after {
  background:
    linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.36) 48%, transparent 100%),
    repeating-linear-gradient(118deg, transparent 0 56px, rgba(255, 255, 255, 0.16) 57px 58px);
  opacity: 0.72;
}

.rental-intro-eyebrow {
  color: var(--intro-primary);
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.rental-intro-hero-title {
  color: var(--intro-primary-deep);
  font-size: clamp(2.4rem, 7vw, 4.9rem);
  line-height: 1.12;
  letter-spacing: 0;
}

.rental-intro-hero-title span {
  color: #1f6ed4;
}

.rental-intro-hero-copy {
  max-width: 650px;
  color: #41546f;
}

.rental-intro-main-cta {
  color: var(--intro-primary);
  border: 1px solid rgba(255, 255, 255, 0.66);
  margin-bottom: 3.5rem;
}

.rental-intro-main-cta:hover {
  color: var(--intro-primary-deep);
  transform: translateY(-1px);
}

.rental-intro-glass-badge {
  padding: 0.65rem 1.1rem;
  color: var(--intro-primary-deep);
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.78);
  box-shadow: 0 10px 30px rgba(0, 56, 133, 0.08);
  backdrop-filter: blur(12px);
}

.rental-intro-ring {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  border: 1px solid rgba(255, 255, 255, 0.58);
  box-shadow: inset 0 0 80px rgba(255, 255, 255, 0.24);
}

.rental-intro-ring-one {
  width: clamp(18rem, 36vw, 34rem);
  height: clamp(18rem, 36vw, 34rem);
  left: -8rem;
  top: 6.5rem;
}

.rental-intro-ring-two {
  width: clamp(22rem, 44vw, 42rem);
  height: clamp(22rem, 44vw, 42rem);
  right: -14rem;
  bottom: -6rem;
  border-color: rgba(255, 246, 224, 0.78);
}

.rental-intro-horizon {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 18%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.76), transparent);
}

.rental-intro-route-line {
  position: absolute;
  left: 50%;
  top: 16%;
  width: min(72vw, 980px);
  height: 220px;
  transform: translateX(-50%) rotate(-7deg);
  border-bottom: 2px solid rgba(255, 255, 255, 0.68);
  border-radius: 50%;
  opacity: 0.75;
  pointer-events: none;
}

.rental-intro-scroll-cue {
  position: absolute;
  left: 50%;
  bottom: 1.25rem;
  transform: translateX(-50%);
  color: rgba(0, 56, 133, 0.62);
}

.rental-intro-overlap-section {
  position: relative;
  z-index: 2;
  margin-top: -6rem;
}

.intro-glass-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.82);
  box-shadow: 0 24px 70px rgba(0, 56, 133, 0.1);
  backdrop-filter: blur(18px);
}

.intro-card-day {
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(229, 246, 255, 0.72)),
    rgba(255, 255, 255, 0.78);
}

.intro-card-dusk {
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(255, 235, 208, 0.72)),
    rgba(255, 255, 255, 0.78);
}

.intro-card-icon {
  width: 4rem;
  height: 4rem;
  color: var(--intro-primary);
  background: linear-gradient(135deg, rgba(217, 226, 255, 0.95), rgba(255, 238, 211, 0.85));
}

.intro-feature-section {
  margin-top: 5rem;
  background:
    radial-gradient(ellipse at 15% 20%, rgba(232, 245, 255, 0.22), transparent 28rem),
    radial-gradient(ellipse at 85% 10%, rgba(255, 248, 232, 0.22), transparent 28rem),
    rgba(253, 254, 255, 0.96);
}

.intro-feature-card,
.intro-process-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(214, 227, 246, 0.78);
  box-shadow: 0 16px 44px rgba(0, 56, 133, 0.06);
}

.intro-feature-icon {
  width: 3.5rem;
  height: 3.5rem;
  color: var(--intro-primary);
  background: linear-gradient(135deg, rgba(216, 226, 255, 0.92), rgba(255, 236, 207, 0.82));
}

.intro-title-rule {
  width: 5rem;
  height: 0.25rem;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--intro-primary), var(--intro-sunset-strong));
}

.intro-center-divider {
  display: none;
}

.intro-step-list {
  position: relative;
  display: grid;
  gap: 1.65rem;
}

.intro-step-list::before {
  content: "";
  position: absolute;
  left: 1rem;
  top: 1rem;
  bottom: 1rem;
  width: 2px;
  border-left: 2px dashed rgba(0, 89, 187, 0.22);
}

.intro-step-list-dusk::before {
  border-left-color: rgba(255, 166, 87, 0.32);
}

.intro-step-item {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.intro-step-number {
  width: 2rem;
  height: 2rem;
  flex: 0 0 2rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 8px 22px rgba(0, 56, 133, 0.14);
}

.intro-step-day {
  background: var(--intro-primary);
}

.intro-step-dusk {
  background: #e99442;
}

.intro-dusk-text {
  color: #c16d1d;
}

.intro-bottom-cta {
  background: linear-gradient(180deg, transparent, rgba(255, 250, 242, 0.5));
}

.intro-bottom-card {
  max-width: 640px;
}

@media (max-width: 767.98px) {
  .rental-daily-long-term-page {
    padding-top: 0;
  }

  .rental-intro-hero {
    min-height: 620px;
    padding: 5.5rem 0 7rem;
  }

  .rental-intro-overlap-section {
    margin-top: -4rem;
  }

  .rental-intro-hero-title {
    font-size: clamp(2.25rem, 11vw, 3.35rem);
  }

  .rental-intro-main-cta {
    margin-bottom: 3rem;
  }
}

@media (max-height: 760px) {
  .rental-intro-scroll-cue {
    display: none;
  }

  .rental-intro-main-cta {
    margin-bottom: 0;
  }
}
</style>



