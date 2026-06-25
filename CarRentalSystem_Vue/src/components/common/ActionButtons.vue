<!-- ==================== ActionButtons.vue ==================== -->
<!-- 儲存為 ActionButtons.vue -->
<!--
使用方式：
<ActionButtons
  @edit="handleEdit(item.id)"
  @delete="handleDelete(item.id)"
/>
// 只顯示編輯：
<ActionButtons :showDelete="false" @edit="handleEdit(item.id)" />
-->
<script setup>
const props = defineProps({
  showEdit: { type: Boolean, default: true },
  showDelete: { type: Boolean, default: true },
  editText: { type: String, default: '修改' },
  deleteText: { type: String, default: '刪除' }
})
const emit = defineEmits(['edit', 'delete'])
</script>
<template>
  <div class="d-flex gap-2">
    <button v-if="showEdit" @click="emit('edit')" class="btn btn-sm btn-outline-primary">
      {{ editText }}
    </button>
    <button v-if="showDelete" @click="emit('delete')" class="btn btn-sm btn-outline-danger">
      {{ deleteText }}
    </button>
  </div>
</template>
