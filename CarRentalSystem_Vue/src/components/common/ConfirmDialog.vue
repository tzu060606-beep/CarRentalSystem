<script setup>
// ConfirmDialog.vue - 確認對話框
// 使用方式：
// <ConfirmDialog
//   :isVisible="showConfirm"
//   message="確定要刪除嗎？"
//   confirmText="刪除"
//   confirmVariant="danger"
//   @confirm="handleConfirm"
//   @cancel="showConfirm = false"
// />

const props = defineProps({
  isVisible: { type: Boolean, required: true },
  title: { type: String, default: '確認操作' },
  message: { type: String, default: '確定要執行此操作嗎？' },
  confirmText: { type: String, default: '確認' },
  cancelText: { type: String, default: '取消' },
  confirmVariant: { type: String, default: 'primary' } // primary / danger / warning
})

const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <Teleport to="body">
    <div v-if="isVisible" class="modal fade show d-block" tabindex="-1" @click.self="emit('cancel')"       style="z-index: var(--z-index-tooltip);">
      <div class="modal-dialog modal-dialog-centered" style="max-width: 400px;">
        <div class="modal-content">
          <div class="modal-header border-0 pb-0">
            <h5 class="modal-title fw-bold">{{ title }}</h5>
            <button type="button" class="btn-close" @click="emit('cancel')"></button>
          </div>
          <div class="modal-body pt-2">
            <p class="text-secondary mb-0" style="white-space: pre-line;">{{ message }}</p>
          </div>
          <div class="modal-footer border-0 pt-0">
            <button class="btn btn-outline-secondary" @click="emit('cancel')">
              {{ cancelText }}
            </button>
            <button :class="`btn btn-${confirmVariant}`" @click="emit('confirm')">
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <div v-if="isVisible" class="modal-backdrop fade show" style="z-index: calc(var(--z-index-tooltip) - 1);"></div>
  </Teleport>
</template>
