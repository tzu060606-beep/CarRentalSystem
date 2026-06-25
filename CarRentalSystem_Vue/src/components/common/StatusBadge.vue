<!-- ==================== StatusBadge.vue ==================== -->
<!-- 儲存為 StatusBadge.vue -->
<!--
使用方式：
<StatusBadge status="ACTIVE" />
<StatusBadge status="CANCELLED" :map="customMap" />

預設 map：
ACTIVE/active → 啟用（success）
INACTIVE/inactive → 停用（secondary）
PENDING/pending → 待處理（warning）
CANCELLED/cancelled → 已取消（secondary）
COMPLETED/completed → 已完成（success）
CLOSED/closed → 已結案（info）
-->
<script setup>
const props = defineProps({
  status: { type: String, default: '' },
  map: {
    type: Object,
    default: () => ({
      ACTIVE:    { label: '啟用',   variant: 'success' },
      active:    { label: '啟用',   variant: 'success' },
      INACTIVE:  { label: '停用',   variant: 'secondary' },
      inactive:  { label: '停用',   variant: 'secondary' },
      PENDING:   { label: '待處理', variant: 'warning' },
      pending:   { label: '待處理', variant: 'warning' },
      CANCELLED: { label: '已取消', variant: 'secondary' },
      cancelled: { label: '已取消', variant: 'secondary' },
      COMPLETED: { label: '已完成', variant: 'success' },
      completed: { label: '已完成', variant: 'success' },
      CLOSED:    { label: '已結案', variant: 'info' },
      closed:    { label: '已結案', variant: 'info' },
    })
  }
})

const info = (status) => props.map[status] || { label: status, variant: 'secondary' }
</script>
<template>
  <span :class="`badge bg-${info(status).variant}`">{{ info(status).label }}</span>
</template>
