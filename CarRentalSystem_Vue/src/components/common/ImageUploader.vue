<script setup>
// ImageUploader.vue - 圖片上傳元件
// 使用方式：
// <ImageUploader v-model="imageUrl" />
// <ImageUploader v-model="imageUrl" :previewUrl="product.imageUrl" />
//
// v-model 回傳的是 base64 字串（前端預覽用）
// 後端需要另外處理 base64 轉存

import { ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },   // base64 或 URL
  previewUrl: { type: String, default: '' },   // 現有圖片 URL（修改時使用）
  accept: { type: String, default: 'image/*' },
  maxSizeMB: { type: Number, default: 5 }
})

const emit = defineEmits(['update:modelValue'])

const isDragging = ref(false)
const errorMsg = ref('')
const fileInput = ref(null)

// 目前顯示的預覽圖：優先用 modelValue（剛選的），其次用 previewUrl（原有的）
const currentPreview = computed(() => props.modelValue || props.previewUrl)

import { computed } from 'vue'

const handleFile = (file) => {
  errorMsg.value = ''
  if (!file) return

  // 檔案大小驗證
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    errorMsg.value = `圖片大小不能超過 ${props.maxSizeMB}MB`
    return
  }

  // 讀取為 base64
  const reader = new FileReader()
  reader.onload = (e) => {
    emit('update:modelValue', e.target.result)
  }
  reader.readAsDataURL(file)
}

const onFileChange = (e) => {
  handleFile(e.target.files[0])
}

const onDrop = (e) => {
  isDragging.value = false
  handleFile(e.dataTransfer.files[0])
}

const clearImage = () => {
  emit('update:modelValue', '')
  errorMsg.value = ''
  if (fileInput.value) fileInput.value.value = ''
}
</script>

<template>
  <div>
    <!-- 已有圖片：顯示預覽 + 清除按鈕 -->
    <div v-if="currentPreview" class="position-relative d-block w-100">
      <img :src="currentPreview" alt="圖片預覽" class="rounded-3 border"
        style="width: 100%; height: 160px; object-fit: cover;" />
      <button type="button" @click="clearImage" class="btn btn-sm position-absolute top-0 end-0 rounded-circle"
        style="transform: translate(50%, -50%); width: 28px; height: 28px; padding: 0;background-color: #e0e0e0;"
        title="移除圖片">
        <font-awesome-icon icon="xmark" />
      </button>
    </div>

    <!-- 沒有圖片：顯示 Dropzone -->
    <div v-else class="rounded-3 border d-flex flex-column align-items-center justify-content-center gap-2 text-center"
      :class="isDragging ? 'border-primary bg-primary bg-opacity-10' : 'border-dashed'"
      style="width: 100%; height: 160px; cursor: pointer; border-style: dashed !important;" @click="fileInput.click()"
      @dragover.prevent="isDragging = true" @dragleave.prevent="isDragging = false" @drop.prevent="onDrop">
      <font-awesome-icon icon="cloud-arrow-up" style="font-size: 2rem; color: var(--color-text-muted);" />
      <span class="small" style="color: var(--color-text-muted);">點擊或拖曳上傳</span>
      <span class="small" style="color: var(--color-text-muted); font-size: 0.75rem;">最大 {{ maxSizeMB }}MB</span>
    </div>

    <!-- 隱藏的 file input -->
    <input ref="fileInput" type="file" :accept="accept" class="d-none" @change="onFileChange" />

    <!-- 錯誤訊息 -->
    <p v-if="errorMsg" class="text-danger small mt-1 mb-0">
      <font-awesome-icon icon="circle-exclamation" class="me-1" />
      {{ errorMsg }}
    </p>
  </div>
</template>
