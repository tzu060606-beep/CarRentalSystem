<script setup>
import { ref, onMounted, computed } from 'vue'
import { pointsRuleAPI } from '@/api/point/point_rule'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import AlertToast from '@/components/common/AlertToast.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const pointsRules = ref([])
const toast = ref(null)

// 切換狀態前的確認
const showToggleConfirm = ref(false)
const pendingRule = ref(null)

const fetchPointsRules = async () => {
    const response = await pointsRuleAPI.getAll()
    pointsRules.value = response.data.data
}

// 點擊 toggle → 先暫存，跳確認
const handleToggleClick = (rule) => {
    pendingRule.value = rule
    showToggleConfirm.value = true
}

// 確認後才真正切換
const confirmToggle = async () => {
    try {
        const updated = { ...pendingRule.value, isActive: !pendingRule.value.isActive }
        await pointsRuleAPI.updateById(pendingRule.value.ruleId, updated)
        await fetchPointsRules()
        const status = !pendingRule.value.isActive ? '啟用' : '停用'
        toast.value.show(`已${status}：${pendingRule.value.ruleName}`, 'success', 3000)
    } catch (error) {
        toast.value.show('操作失敗，請稍後再試', 'danger', 3000)
    } finally {
        showToggleConfirm.value = false
        pendingRule.value = null
    }
}

// 根據 ruleKey 對應圖示和顏色
const getRuleStyle = (ruleKey) => {
    const map = {
        'RENTAL': { icon: 'fa-car', color: 'var(--color-primary)', bg: 'var(--color-blue-100)' },
        'TRANSFER': { icon: 'fa-van-shuttle', color: 'var(--color-primary)', bg: 'var(--color-blue-100)' },
        'BIRTHDAY': { icon: 'fa-cake-candles', color: '#D85A30', bg: '#FAECE7' },
        'WELCOME_BONUS': { icon: 'fa-gift', color: '#1D9E75', bg: '#E1F5EE' },
        'FIRST_RENTAL': { icon: 'fa-star', color: '#BA7517', bg: '#FAEEDA' },
        'EXPIRED': { icon: 'fa-clock', color: 'var(--color-danger)', bg: '#FCEBEB' },
        'REDEMPTION': { icon: 'fa-ticket-simple', color: '#534AB7', bg: '#EEEDFE' },
    }
    return map[ruleKey] || { icon: 'fa-circle-dot', color: 'var(--color-text-secondary)', bg: 'var(--color-bg-sunken)' }
}

const activeCount = computed(() => pointsRules.value.filter(r => r.isActive).length)
const inactiveCount = computed(() => pointsRules.value.filter(r => !r.isActive).length)

onMounted(() => {
    fetchPointsRules()
})
</script>

<template>
    <div>
        <!-- 麵包屑 -->
        <div class="mb-3">
            <Breadcrumb :items="[
                { label: '首頁', to: '/' },
                { label: '點數管理', to: '/point' },
                { label: '點數規則管理' }
            ]" />
        </div>

        <!-- 標題 + 統計 -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h3 class="mb-1 fw-bold">點數規則管理</h3>
                <p class="text-secondary mb-0" style="font-size: var(--text-sm);">
                    管理所有點數累積與扣除規則，點擊卡片右上角開關可切換啟用狀態
                </p>
            </div>
            <div class="d-flex gap-2">
                <div class="text-center px-3 py-2 rounded-3"
                    style="background: var(--color-bg-surface); border: 1px solid var(--color-border);">
                    <div class="fw-bold" style="font-size: var(--text-xl); color: var(--color-success);">
                        {{ activeCount }}
                    </div>
                    <div class="text-secondary" style="font-size: var(--text-xs);">啟用中</div>
                </div>
                <div class="text-center px-3 py-2 rounded-3"
                    style="background: var(--color-bg-surface); border: 1px solid var(--color-border);">
                    <div class="fw-bold" style="font-size: var(--text-xl); color: var(--color-text-secondary);">
                        {{ inactiveCount }}
                    </div>
                    <div class="text-secondary" style="font-size: var(--text-xs);">已停用</div>
                </div>
            </div>
        </div>

        <!-- 卡片列表 -->
        <div class="row g-3">
            <div v-for="rule in pointsRules" :key="rule.ruleId" class="col-md-6 col-lg-4">
                <div class="bg-white border rounded-4 p-4 h-100 rule-card"
                    :class="{ 'rule-card--inactive': !rule.isActive }">

                    <!-- 卡片頂部：圖示 + 開關 -->
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <!-- 圖示圓圈 -->
                        <div class="d-flex align-items-center justify-content-center rounded-3" :style="{
                            width: '44px', height: '44px',
                            background: getRuleStyle(rule.ruleKey).bg,
                            color: getRuleStyle(rule.ruleKey).color
                        }">
                            <i :class="`fa-solid ${getRuleStyle(rule.ruleKey).icon}`" style="font-size: 1.1rem;"></i>
                        </div>

                        <!-- 啟用開關 -->
                        <div class="form-check form-switch mb-0">
                            <input class="form-check-input" type="checkbox" :checked="rule.isActive"
                                @click.prevent="handleToggleClick(rule)"
                                style="width: 2.5em; height: 1.25em; cursor: pointer;">
                        </div>
                    </div>

                    <!-- 規則名稱 + 狀態 badge -->
                    <div class="d-flex align-items-center gap-2 mb-1">
                        <h6 class="fw-bold mb-0">{{ rule.ruleName }}</h6>
                        <span v-if="rule.isActive" class="badge bg-success"
                            style="font-size: var(--text-xs);">啟用中</span>
                        <span v-else class="badge bg-secondary" style="font-size: var(--text-xs);">已停用</span>
                    </div>

                    <!-- 描述 -->
                    <p class="text-secondary mb-3"
                        style="font-size: var(--text-sm); line-height: 1.5; min-height: 40px;">
                        {{ rule.description || '（無描述）' }}
                    </p>

                    <!-- 底部資訊：比例 + ruleKey -->
                    <div class="d-flex justify-content-between align-items-center pt-3"
                        style="border-top: 1px solid var(--color-border);">
                        <div>
                            <span class="text-secondary" style="font-size: var(--text-xs);">累點比例</span>
                            <div class="fw-bold" style="color: var(--color-primary);">
                                × {{ rule.ratio }}
                            </div>
                        </div>
                        <code style="font-size: var(--text-xs); color: var(--color-text-secondary);">
                            {{ rule.ruleKey }}
                        </code>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 切換確認 -->
    <ConfirmDialog :isVisible="showToggleConfirm" :title="pendingRule?.isActive ? '確認停用規則' : '確認啟用規則'" :message="pendingRule?.isActive
        ? `確定要停用「${pendingRule?.ruleName}」嗎？停用後將不再累積此類點數。`
        : `確定要啟用「${pendingRule?.ruleName}」嗎？啟用後將開始累積此類點數。`" :confirmText="pendingRule?.isActive ? '確認停用' : '確認啟用'"
        :confirmVariant="pendingRule?.isActive ? 'warning' : 'primary'" @confirm="confirmToggle"
        @cancel="showToggleConfirm = false" />

    <AlertToast ref="toast" />
</template>

<style scoped>
.rule-card {
    transition: box-shadow 0.2s, transform 0.2s;
    box-shadow: var(--shadow-sm);
}

.rule-card:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
}

.rule-card--inactive {
    opacity: 0.6;
}

.rule-card--inactive:hover {
    opacity: 0.8;
}
</style>