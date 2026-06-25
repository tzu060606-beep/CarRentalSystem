<script setup>
// BackButton.vue - 返回按鈕
// 使用方式：
// <BackButton />                          → 返回上一頁（router.back()）
// <BackButton to="/point" label="返回點數管理" />  → 返回指定路由
// <BackButton label="返回列表" />          → 自訂文字，返回上一頁

import { useRouter } from 'vue-router'

const props = defineProps({
  to: { type: String, default: '' },
  label: { type: String, default: '返回' }
})

const router = useRouter()

const handleBack = () => {
  if (props.to) {
    router.push(props.to)
  } else {
    router.back()
  }
}
</script>

<template>
  <button
    @click="handleBack"
    class="btn btn-outline-secondary d-inline-flex align-items-center gap-2"
  >
    <font-awesome-icon icon="arrow-left" />
    {{ label }}
  </button>
</template>
