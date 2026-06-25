<script setup>
import { computed } from 'vue';

const props = defineProps({ //defineProps：接收老闆派發的任務 (給資料)
  car: {
    type: Object,
    required: true
  }
});
//這是子元件在宣告：「老闆，如果你要叫我出來做事，你必須給我一個叫做 car 的物件。」
//type: Object：這是一種型別檢查。確保傳進來的是一整包車輛資料（物件），而不是一個數字或字串。
//required: true：這是一個強迫機制。如果沒傳這台車的資料，Vue 會在開發者主控台（Console）噴出警告，因為沒有資料這張卡片就畫不出來。
//OrderSelectView.vue，<OrderVehicleCard :car="car" />

const emit = defineEmits(['select']);
//defineEmits：向老闆回報進度 (傳信號)
// 這是子元件在宣告：「老闆，我身上裝了一個發報機，當使用者按下我這張卡片時，我會發出一個名為 select 的信號給你看。」
// 子元件通常不應該直接去修改全域的資料（Store），它應該只負責「通知」老闆。老闆收到信號後，再決定要執行什麼動作（例如跳轉頁面或存入選取的車輛）。

const formattedPrice = computed(() => Number(props.car.price || 0).toLocaleString());

const handleSelectCar = () => {
  emit('select', props.car);
  //第一個為自定義的「事件標籤」。
  // 對應關係： 父元件在 HTML 標籤上，必須使用 @（或 v-on:）加上這個名稱來「接聽」這個信號。
  // 如果例如：<OrderVehicleCard @select="要做的事情" />。
  // 如果第一個參數改成 emit('click-car', ...)，那父元件就要寫成 @click-car。

  //第二個參數為「攜帶的資料」（Payload），這裡把 props.car（也就是這張卡片目前顯示的那一整包 Altis 或 Tesla 資料）丟了進去。
  // 例如老闆那邊寫 const selectVehicle = (data) => { ... }，那個 data 就會是子元件傳回來的 props.car。
};


</script>

<template>
  <article class="card shadow-sm border border-light-subtle rounded-4 mb-4 overflow-hidden">
    <div class="row g-0">
      <div class="col-md-4 bg-light position-relative">
        <img
          :src="car.image"
          class="img-fluid w-100 h-100 object-fit-cover"
          :alt="`${car.brand} ${car.model}`"
          style="min-height: 220px;"
        >

        <div class="position-absolute top-0 start-0 p-3 d-flex flex-wrap gap-2">
          <span
            v-for="tag in car.tags"
            :key="tag.text"
            class="badge px-2 py-1"
            :class="tag.bg"
          >
            {{ tag.text }}
          </span>
        </div>
      </div>

      <div class="col-md-5 p-4 d-flex flex-column justify-content-center">
        <p class="text-primary fw-bold small text-uppercase mb-1">{{ car.brand }}</p>
        <h3 class="h4 fw-bold text-dark mb-3">{{ car.model }}</h3>
        <p v-if="car.availableCount" class="small text-secondary mb-3">
          此車款目前可租 {{ car.availableCount }} 台
        </p>

        <div class="d-flex flex-wrap gap-2 mb-3">
          <span class="badge bg-light text-secondary border">
            <i class="fa-solid fa-users me-1"></i>
            {{ car.specs.seats }} 人座
          </span>
          <span class="badge bg-light text-secondary border">
            <i class="fa-solid fa-suitcase me-1"></i>
            {{ car.specs.bags }} 件大型行李
          </span>
          <span class="badge bg-light text-secondary border">
            <i class="fa-solid fa-bolt me-1"></i>
            {{ car.specs.power }}
          </span>
        </div>

        <p class="text-secondary mb-0 d-flex align-items-start gap-2">
          <i class="fa-solid fa-shield-halved text-secondary mt-1"></i>
          <span>{{ car.features }}</span>
        </p>
      </div>

      <div class="col-md-3 p-4 d-flex flex-column justify-content-center bg-light bg-opacity-50 border-start border-light-subtle">
        <div class="text-md-end mb-3">
          <div class="d-flex align-items-baseline justify-content-md-end gap-1">
            <span class="text-primary fs-2 fw-bold">NT$ {{ formattedPrice }}</span>
            <span class="text-secondary small">/ {{ car.priceUnit || '日' }}</span>
          </div>
        </div>

        <button class="btn btn-success w-100 py-2 fw-bold" type="button" @click="handleSelectCar">
          選擇車輛
        </button>
      </div>
    </div>
  </article>
</template>
