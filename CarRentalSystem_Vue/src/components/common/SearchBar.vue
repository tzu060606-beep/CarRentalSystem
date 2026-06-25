<script setup>
// SearchBar.vue - 搜尋列
// 使用方式：
// <SearchBar v-model="keyword" @search="handleSearch" placeholder="請輸入關鍵字..." />
// 即時搜尋（不需要按鈕）：
// <SearchBar v-model="keyword" :showButton="false" />

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '請輸入關鍵字...' },
  showButton: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue', 'search'])
</script>

<template>
  <div class="d-flex gap-2">
    <input
      :value="modelValue"
      @input="emit('update:modelValue', $event.target.value)"
      @keyup.enter="emit('search')"
      class="form-control"
      type="text"
      :placeholder="placeholder"
    />
    <button
      v-if="showButton"
      @click="emit('search')"
      class="btn btn-primary text-nowrap"
    >
      搜尋
    </button>
  </div>
</template>
