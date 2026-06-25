<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'

// ── 共用元件 import ──────────────────────────────────────────
import AlertToast from '@/components/common/AlertToast.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ActionButtons from '@/components/common/ActionButtons.vue'
import SearchBar from '@/components/common/SearchBar.vue'
import Pagination from '@/components/common/Pagination.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import PointsBadge from '@/components/common/PointsBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import NavBar from '@/components/common/NavBar.vue'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import TableSkeleton from '@/components/common/TableSkeleton.vue'
import BackButton from '@/components/common/BackButton.vue'
import ImageUploader from '@/components/common/ImageUploader.vue'

// ── AlertToast ───────────────────────────────────────────────
const toast = ref(null)

// ── ConfirmDialog ────────────────────────────────────────────
const showConfirm = ref(false)
const handleConfirmTest = () => {
    console.log('確認按下了')
    showConfirm.value = false
}

// ── BaseModal ────────────────────────────────────────────────
const showModal = ref(false)

// ── LoadingSpinner ───────────────────────────────────────────
const isLoading = ref(false)
const triggerLoading = () => {
    isLoading.value = true
    setTimeout(() => { isLoading.value = false }, 2000)
}

// ── ActionButtons ────────────────────────────────────────────
const handleEdit = () => console.log('編輯按下了')
const handleDelete = () => console.log('刪除按下了')

// ── SearchBar ────────────────────────────────────────────────
const keyword = ref('')
const handleSearch = () => console.log('搜尋關鍵字：', keyword.value)

// ── Pagination ───────────────────────────────────────────────
const currentPage = ref(1)
const totalPages = ref(5)

// ── TableSkeleton ────────────────────────────────────────────
const showSkeleton = ref(false)
const triggerSkeleton = () => {
    showSkeleton.value = true
    setTimeout(() => { showSkeleton.value = false }, 3000)
}

// ── ImageUploader ────────────────────────────────────────────
const uploadedImage = ref('')
</script>

<template>
    <div>
        <!-- ════════════════════════════════════════════════════ -->
        <!-- 共用元件測試區塊（測試完成後可以刪除）               -->
        <!-- ════════════════════════════════════════════════════ -->
         
        <!-- ⑬ Breadcrumb：麵包屑導覽 -->
        <!-- 預期：前兩個可以點擊，最後一個不可點擊 -->
        <div class="mb-3">
            <p class="text-muted small mb-1">⑬ Breadcrumb：</p>
            <Breadcrumb :items="[
                { label: '首頁', to: '/' },
                { label: '點數管理', to: '/point' },
                { label: '商品管理' }
            ]" />
        </div>
        
        <!-- ⑪ NavBar：後台導覽列，必須在最外層 div 的最上方 -->
         <p class="text-muted small mb-1">⑬ NavBar：</p>
        <NavBar />

        <!-- Teleport 元件（渲染到 body，放這裡即可） -->
        <AlertToast ref="toast" />
        <ConfirmDialog :isVisible="showConfirm" title="確定要刪除?" message="刪除後將無法復原？" confirmText="確定"
            confirmVariant="danger" @confirm="handleConfirmTest" @cancel="showConfirm = false" />
        <BaseModal :isVisible="showModal" title="測試 BaseModal" size="md" @close="showModal = false">
            <template #body>
                <p>這是 BaseModal 的 body slot 內容</p>
                <p>可以放表單、圖片、任何內容</p>
            </template>
            <template #footer>
                <button class="btn btn-outline-secondary" @click="showModal = false">取消</button>
                <button class="btn btn-primary" @click="showModal = false">確認</button>
            </template>
        </BaseModal>
        <LoadingSpinner :isLoading="isLoading" text="載入中..." overlay />

        <div class="m-4">



            <!-- ⑯ BackButton：返回按鈕 -->
            <!-- 預期：第一個 router.back()，第二個跳到 /point -->
            <div class="mb-3">
                <p class="text-muted small mb-1">⑯ BackButton：</p>
                <div class="d-flex gap-2">
                    <BackButton />
                    <BackButton to="/point" label="返回點數管理" />
                </div>
            </div>

            <!-- ① ② ③ ④ 觸發按鈕區 -->
            <div class="mb-3">
                <p class="text-muted small mb-1">① ② ③ ④ 觸發按鈕：</p>
                <div class="d-flex flex-wrap gap-2">
                    <button @click="toast.show('成功訊息', 'success')" class="btn btn-sm btn-success">① Toast
                        success</button>
                    <button @click="toast.show('危險訊息', 'danger')" class="btn btn-sm btn-danger">① Toast danger</button>
                    <button @click="toast.show('警告訊息', 'warning')" class="btn btn-sm btn-warning">① Toast
                        warning</button>
                    <button @click="toast.show('資訊訊息', 'info')" class="btn btn-sm btn-info">① Toast info</button>
                    <button @click="showConfirm = true" class="btn btn-sm btn-outline-danger">② ConfirmDialog</button>
                    <button @click="showModal = true" class="btn btn-sm btn-outline-primary">③ BaseModal</button>
                    <button @click="triggerLoading" class="btn btn-sm btn-outline-secondary">④
                        LoadingSpinner（2秒）</button>
                </div>
            </div>

            <!-- ⑤ ActionButtons：編輯＋刪除按鈕組 -->
            <!-- 預期：點擊後 console 印出對應訊息 -->
            <div class="mb-3">
                <p class="text-muted small mb-1">⑤ ActionButtons：</p>
                <ActionButtons @edit="handleEdit" @delete="handleDelete" />
            </div>

            <!-- ⑥ SearchBar：搜尋列 -->
            <!-- 預期：輸入文字按搜尋或 Enter，console 印出關鍵字 -->
            <div class="mb-3" style="max-width: 400px;">
                <p class="text-muted small mb-1">⑥ SearchBar：</p>
                <SearchBar v-model="keyword" @search="handleSearch" placeholder="輸入關鍵字測試..." />
            </div>

            <!-- ⑦ Pagination：分頁元件 -->
            <!-- 預期：點頁碼切換，數字改變 -->
            <div class="mb-3">
                <p class="text-muted small mb-1">⑦ Pagination（目前第 {{ currentPage }} 頁，共 {{ totalPages }} 頁）：</p>
                <Pagination :currentPage="currentPage" :totalPages="totalPages" @change="currentPage = $event" />
            </div>

            <!-- ⑧ StatusBadge：狀態標籤 -->
            <!-- 預期：各種顏色標籤顯示正確 -->
            <div class="mb-3">
                <p class="text-muted small mb-1">⑧ StatusBadge：</p>
                <div class="d-flex gap-2 flex-wrap">
                    <StatusBadge status="ACTIVE" />
                    <StatusBadge status="INACTIVE" />
                    <StatusBadge status="PENDING" />
                    <StatusBadge status="CANCELLED" />
                    <StatusBadge status="COMPLETED" />
                    <StatusBadge status="USED" :map="{
                        UNUSED: { label: '未使用', variant: 'primary' },
                        USED: { label: '已使用', variant: 'success' },
                        EXPIRED: { label: '已過期', variant: 'warning' }
                    }" />
                </div>
            </div>

            <!-- ⑨ PointsBadge：點數顯示 -->
            <!-- 預期：FA 星星圖示 + 點數數字，三種大小 -->
            <div class="mb-3">
                <p class="text-muted small mb-1">⑨ PointsBadge（確認 FA 星星圖示有出現）：</p>
                <div class="d-flex gap-3 align-items-center">
                    <PointsBadge :points="300" size="sm" />
                    <PointsBadge :points="1500" size="md" />
                    <PointsBadge :points="99999" size="lg" />
                </div>
            </div>

            <!-- ⑩ EmptyState：空狀態提示 -->
            <!-- 預期：FA inbox 圖示 + 文字（注意 icon 用 FA 名稱，不是 bi bi-inbox） -->
            <div class="mb-3 border rounded-3">
                <p class="text-muted small p-2 mb-0">⑩ EmptyState（確認 FA inbox 圖示有出現）：</p>
                <EmptyState icon="inbox" message="目前沒有兌換紀錄" subMessage="完成兌換後將在此顯示" />
            </div>

            <!-- ⑭ TableSkeleton：表格骨架屏 -->
            <!-- 預期：點按鈕後出現骨架動畫，3秒後消失 -->
            <div class="mb-3">
                <p class="text-muted small mb-1">⑭ TableSkeleton：</p>
                <button @click="triggerSkeleton" class="btn btn-sm btn-outline-secondary mb-2">觸發骨架屏（3秒）</button>
                <TableSkeleton v-if="showSkeleton" :rows="3" :cols="4" />
                <p v-else class="text-muted small">點擊按鈕觸發骨架屏</p>
            </div>

            <!-- ⑮ ImageUploader：圖片上傳 -->
            <!-- 預期：FA 上傳圖示有出現，點擊或拖曳上傳，出現預覽，X 可清除 -->
            <div class="mb-3">
                <p class="text-muted small mb-1">⑮ ImageUploader（確認 FA 上傳圖示有出現）：</p>
                <ImageUploader v-model="uploadedImage" />
                <p v-if="uploadedImage" class="text-success small mt-1">
                    ✅ 圖片已上傳，base64 長度：{{ uploadedImage.length }} 字元
                </p>
            </div>

            <!-- ⑫ NotFoundView 提示 -->
            <div class="p-3 bg-light rounded">
                <p class="text-muted small mb-0">
                    ⑫ NotFoundView：在瀏覽器輸入不存在的路徑測試，例如 <code>/point/abc123</code>
                </p>
            </div>

        </div>
    </div>
</template>

<style scoped></style>