<script setup>
import MemberSidebar from '@/components/common/layout/MemberSidebar.vue'
</script>

<template>
    <div class="member-layout">
        <!-- 側邊欄 -->
         <MemberSidebar />
        <!-- 主內容區 -->
        <main class="member-main">
            <RouterView />
        </main>
    </div>
</template>

<style scoped>
/* 左右分欄容器 */
.member-layout {
    display: flex;
    min-height: calc(100vh - 92px);
    background: transparent;
    /* var(--color-bg-page); 原本的 background: */
    max-width: 1400px;
    /* 最大寬度 */
    margin: 0 auto;
    /* 置中 */
    padding: var(--space-4) var(--space-4);
    /* 左右留間距 */
    gap: var(--space-4);
    /* sidebar 和內容區的間距 */
}


/* ── 主內容區 ── */
.member-main {
    flex: 1;
    min-width: 0;/* 偉:讓元素不會被內容撐破 */
    width: 100%;
    padding: var(--space-8);


}
</style>