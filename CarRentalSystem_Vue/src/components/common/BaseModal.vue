<script setup>
// BaseModal.vue - 通用 Modal 外框
// 使用方式：
// <BaseModal :isVisible="showModal" title="標題" @close="showModal = false">
//   <template #body> 內容放這裡 </template>
//   <template #footer> 按鈕放這裡 </template>
// </BaseModal>

// 更新1：處理滾動鎖定
// 更新2：加入開關動畫（Modal 本體從上滑入、Backdrop 淡入淡出）
import { watch } from 'vue'


const props = defineProps({
  isVisible: { type: Boolean, required: true },
  title: { type: String, default: '' },
  size: { type: String, default: 'md' } // sm / md / lg / xl
})

const emit = defineEmits(['close'])


watch(() => props.isVisible, (newVal) => {
  if (newVal) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})
</script>

<template>
  <Teleport to="body">

    <!-- 
      Modal 本體動畫：從上方滑入
      name="modal-slide" → CSS class 前綴是 modal-slide
      對應下方 style 的 .modal-slide-enter-from / .modal-slide-leave-to 等
    -->
    <Transition name="modal-slide">
      <div v-if="isVisible" class="modal fade show d-block" tabindex="-1" @click.self="emit('close')">
        <div :class="`modal-dialog modal-${size} modal-dialog-centered modal-dialog-scrollable`">
          <div class="modal-content">
            <!-- Header -->
            <div class="modal-header">
              <h5 class="modal-title fw-bold">{{ title }}</h5>
              <button type="button" class="btn-close" @click="emit('close')"></button>
            </div>
            <!-- Body -->
            <div class="modal-body">
              <slot name="body" />
            </div>
            <!-- Footer -->
            <div v-if="$slots.footer" class="modal-footer">
              <slot name="footer" />
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 
      Backdrop 動畫：單純淡入淡出
      name="modal-fade" → CSS class 前綴是 modal-fade
    -->
    <Transition name="modal-fade">
      <!-- Backdrop -->
      <div v-if="isVisible" class="modal-backdrop fade show"></div>
    </Transition>

  </Teleport>
</template>

<style scoped>
/* 
  ========================================
  Modal 本體動畫：從上方滑入 (modal-slide)
  ========================================
  Vue 的 Transition 會在對的時機自動套上這些 class：

  enter-from   = 出現的「起點」狀態（元素剛插入 DOM 的那一幀）
  enter-active = 出現「過程中」（transition 寫在這裡）
  enter-to     = 出現的「終點」狀態（不用寫，就是元素原本的樣子）

  leave-from   = 消失的「起點」狀態（不用寫）
  leave-active = 消失「過程中」
  leave-to     = 消失的「終點」狀態
*/

/* 出現起點 + 消失終點：透明、往上偏移 20px */
.modal-slide-enter-from,
.modal-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
  /* 往上偏移，製造從上滑入的效果 */
}

/* 出現過程：定義動畫時間和速度曲線 */
.modal-slide-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

/* 消失過程：比出現稍快，關閉感覺俐落 */
.modal-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

/* 
  ========================================
  Backdrop 動畫：淡入淡出 (modal-fade)
  ========================================
*/

/* 出現起點 + 消失終點：完全透明 */
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* 出現和消失過程：0.25s 淡入淡出 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}
</style>