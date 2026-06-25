<script setup>
import { voucherAPI } from '@/api/point/point_voucher';
import { customerAPI } from '@/api/login/customerAPI';
import { productAPI } from '@/api/point/point_product';
import { pointsHistoryAPI } from '@/api/point/point_history';
import { pointsRuleAPI } from '@/api/point/point_rule';
import { redemptionOrderAPI } from '@/api/point/point_order';
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute } from 'vue-router'

const route = useRoute()
const keyword = ref(''); //搜尋輸入框
const customer = ref(null); //客戶資訊
const histories = ref([]); //點數異動列表
const vouchers = ref([]); //票券列表
const isSearched = ref(false);//是否已經搜尋過（控制顯示搜尋前/後的畫面）
const isSelecting = ref(false)

//頁面載入時呼叫，拿全部異動
const fetchHistories = async () => {
    const response = await pointsHistoryAPI.getAll();
    histories.value = response.data.data
}

// 按查詢時呼叫，同時打三支 API
const handleSearch = async () => {
    const customerResponse = await customerAPI.getById(keyword.value);
    // F1 的 Controller 回傳的是 ResponseEntity < CustomerResponseDTO >
    customer.value = customerResponse.data;
    const vouchersResponse = await voucherAPI.getByCustId(keyword.value);
    vouchers.value = vouchersResponse.data.data;
    const historiesResponse = await pointsHistoryAPI.getByCustId(keyword.value);
    histories.value = historiesResponse.data.data;
    isSearched.value = true;
}

const resetSearch = async () => {
    keyword.value = ''
    customer.value = null
    vouchers.value = []
    histories.value = []
    isSearched.value = false
    await fetchHistories()  // 重設後重新載入全部異動
}

//把點數異動紀錄加上+號
const formatPoints = (points) => {
    if (points > 0) return '+' + points
    return String(points)  // 負數本身就有 - 號
}

//根據正負決定顏色
const getPointsColorClass = (points) => {
    if (points > 0) return 'text-success'
    return 'text-danger'
}

//樣式轉換用
function getStatusLabel(status) {
    if (status === 'UNUSED') return '未使用'
    if (status === 'USED') return '已使用'
    if (status === 'EXPIRED') return '已過期'
    return status
}

const getChangeTypeLabel = (changeType) => {
    const map = {
        'RENTAL': '租車累點',
        'TRANSFER': '專車累點',
        'BIRTHDAY': '生日贈點',
        'EXPIRED': '點數過期',
        'REDEMPTION': '兌換消耗',
        'FIRST_RENTAL': '首租獎勵',
        'WELCOME_BONUS': '新會員贈點',
    }
    return map[changeType] || changeType
}


//changeType增加Badge樣式
const getChangeTypeBadgeClass = (changeType) => {
    if (changeType === 'RENTAL' || changeType === 'TRANSFER') {
        return 'badge bg-success'   // 獲得點數 → 綠色
    }
    if (changeType === 'REDEMPTION') {
        return 'badge bg-warning'   // 兌換消耗 → 黃色
    }
    if (changeType === 'EXPIRED') {
        return 'badge bg-danger' // 點數過期 → 灰色
    }
    if (changeType === 'BIRTHDAY') {
        return 'badge bg-orange' // 生日贈點 → 灰色
    }
    return 'badge bg-secondary'
}

// 輸入時動態跳出選項（autocomplete typehead 效果）
const searchResults = ref([])  // 存搜尋結果列表
const showDropdown = ref(false)  // 控制下拉選單顯示

// 監聽 keyword 變化，自動搜尋
watch(keyword, async (newVal) => {
    if (isSelecting.value) return  // 如果正在選擇中，跳過
    if (newVal.trim() === '') {
        searchResults.value = []
        showDropdown.value = false
        return
    }
    const response = await customerAPI.search(newVal)
    searchResults.value = response.data  // 確認 F1 回傳格式
    showDropdown.value = searchResults.value.length > 0
})

// 點擊某個客戶選項
const selectCustomer = async (selectedCust) => {
    isSelecting.value = true  // 設定 flag
    keyword.value = selectedCust.custName
    showDropdown.value = false
    // 直接用選到的 custId 查詢
    // 依序執行（慢）
    // const vouchersResponse = await voucherAPI.getByCustId(selectedCust.custId)
    // const historiesResponse = await pointsHistoryAPI.getByCustId(selectedCust.custId)
    // const customerResponse = await customerAPI.getById(selectedCust.custId)

    // 同時發出（快）
    const [vouchersResponse, historiesResponse, customerResponse] = await Promise.all([
        voucherAPI.getByCustId(selectedCust.custId),
        pointsHistoryAPI.getByCustId(selectedCust.custId),
        customerAPI.getById(selectedCust.custId)
    ])
    vouchers.value = vouchersResponse.data.data
    histories.value = historiesResponse.data.data
    customer.value = selectedCust
    isSearched.value = true
    isSelecting.value = false  // 重設 flag
    // 再打一支 API 拿完整的客戶資料：
    customer.value = customerResponse.data
}


// 表頭欄位排序功能
// 排序狀態
const sortKey = ref('')      // 目前排序的欄位名稱
const sortOrder = ref('asc') // 'asc' 升冪 / 'desc' 降冪

// 點擊表頭時切換排序
const toggleSort = (key) => {
    if (sortKey.value === key) {
        // 同一欄位再點一次，切換升降冪
        sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
    } else {
        // 切換到新欄位，預設升冪
        sortKey.value = key
        sortOrder.value = 'asc'
    }
}

// 類別篩選
const changeTypeFilter = ref('')

// 篩選 + 排序後的 histories
const sortedHistories = computed(() => {
    let list = histories.value

    // 先做類別篩選
    if (changeTypeFilter.value !== '') {
        list = list.filter(h => h.changeType === changeTypeFilter.value)
    }

    // 再做排序
    if (!sortKey.value) return list
    return [...list].sort((a, b) => {
        const valA = a[sortKey.value]
        const valB = b[sortKey.value]
        if (valA === null || valA === undefined) return 1
        if (valB === null || valB === undefined) return -1
        if (typeof valA === 'string') {
            return sortOrder.value === 'asc'
                ? valA.localeCompare(valB)
                : valB.localeCompare(valA)
        }
        return sortOrder.value === 'asc' ? valA - valB : valB - valA
    })
})


// 轉換時間格式
const formatDateTime = (timeStr) => {
    if (!timeStr) return '-'
    return timeStr.slice(0, 19).replace('T', ' ')
}


//===================所有點數效期紀錄=========================
// 點數效期：依 expireTime 分組，同一天到期的點數合併加總
const expiryRecords = computed(() => {
    const grouped = {}

    // 只取 availablePoints > 0 的紀錄（有效點數）
    histories.value
        .filter(h => h.availablePoints > 0)
        .forEach(h => {
            // 取到期日的前 10 個字元（2027-01-01）當作 key
            const dateKey = h.expireTime?.slice(0, 10)
            if (!dateKey) return

            // 如果這個日期還沒有，建立一個新的群組
            if (!grouped[dateKey]) {
                grouped[dateKey] = { expireTime: dateKey, totalPoints: 0 }
            }
            // 把這筆的 availablePoints 加進對應日期的 totalPoints
            grouped[dateKey].totalPoints += h.availablePoints
        })

    // 轉成陣列，按到期日從近到遠排序
    return Object.values(grouped).sort((a, b) =>
        new Date(a.expireTime) - new Date(b.expireTime)
    )
})
// 搜尋客戶 → histories 更新 → expiryRecords 自動重新計算 → 依到期日分組合併 → template 顯示

onMounted(async () => {
    // 如果網址有帶 custId（例如從兌換訂單頁點過來），直接查詢該客戶
    if (route.query.custId) {
        await selectCustomer({ custId: Number(route.query.custId) })
    }
    fetchHistories();
})

</script>

<template>
    <div>
        <!-- 返回INDEX麵包屑 -->
        <div class="mb-3">
            <Breadcrumb :items="[
                { label: '首頁', to: '/' },
                { label: '點數管理', to: '/point' },
                { label: '會員點數查詢' }
            ]" />
        </div>

        <!-- 標題 -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h3 class="mb-0 fw-bold">會員點數查詢</h3>
        </div>

        <!-- 搜尋列 -->
        <div class="d-flex gap-2 mb-4 position-relative">
            <div class="position-relative flex-grow-1">
                <input type="text" class="form-control" placeholder="輸入會員 ID 或姓名" v-model="keyword" />
                <!-- 下拉選單 -->
                <div v-if="showDropdown" class="position-absolute w-100 bg-white border rounded-3 shadow-sm"
                    style="top: 100%; left: 0; z-index: var(--z-index-dropdown); margin-top: 4px;">
                    <div v-for="cust in searchResults" :key="cust.custId"
                        class="px-3 py-2 d-flex justify-content-between align-items-center"
                        style="cursor: pointer; font-size: var(--text-sm);" @click="selectCustomer(cust)"
                        @mouseover="$event.currentTarget.style.backgroundColor = 'var(--color-primary-light)'"
                        @mouseleave="$event.currentTarget.style.backgroundColor = ''">
                        <span class="fw-bold">{{ cust.custName }}</span>
                        <code>ID: {{ cust.custId }}</code>
                    </div>
                </div>
            </div>
            <button class="btn btn-outline-secondary text-nowrap" @click="resetSearch">重設</button>
        </div>

        <!-- 搜尋前：顯示最近點數異動（全部會員） -->
        <div v-if="!isSearched">
            <div class="d-flex align-items-center justify-content-between mb-2">
                <div class="customer-section-label">
                    <span class="text-caption">最近點數異動</span>
                </div>
                <!-- 類別篩選下拉 -->
                <select class="form-select w-auto" style="font-size: var(--text-sm);" v-model="changeTypeFilter">
                    <option value="">所有類型</option>
                    <option value="RENTAL">租車累點</option>
                    <option value="TRANSFER">專車累點</option>
                    <option value="BIRTHDAY">生日贈送</option>
                    <option value="EXPIRED">點數過期</option>
                    <option value="REDEMPTION">兌換消耗</option>
                    <option value="FIRST_RENTAL">首租獎勵</option>
                    <option value="WELCOME_BONUS">新會員贈點</option>
                </select>
            </div>

            <div class="bg-white border rounded-4 shadow-sm overflow-hidden mb-4">
                <table class="table table-hover align-middle text-nowrap mb-0">
                    <thead>
                        <tr>
                            <th @click="toggleSort('recordId')" class="sort-th">
                                <div class="d-flex align-items-center gap-1">
                                    紀錄編號
                                    <span class="sort-icon" :class="{ active: sortKey === 'recordId' }">
                                        <span class="sort-asc"
                                            :class="{ on: sortKey === 'recordId' && sortOrder === 'asc' }">▲</span>
                                        <span class="sort-desc"
                                            :class="{ on: sortKey === 'recordId' && sortOrder === 'desc' }">▼</span>
                                    </span>
                                </div>
                            </th>
                            <th @click="toggleSort('custId')" class="sort-th">
                                <div class="d-flex align-items-center gap-1">
                                    客戶編號
                                    <span class="sort-icon" :class="{ active: sortKey === 'custId' }">
                                        <span class="sort-asc"
                                            :class="{ on: sortKey === 'custId' && sortOrder === 'asc' }">▲</span>
                                        <span class="sort-desc"
                                            :class="{ on: sortKey === 'custId' && sortOrder === 'desc' }">▼</span>
                                    </span>
                                </div>
                            </th>
                            <th @click="toggleSort('custName')" class="sort-th">
                                <div class="d-flex align-items-center gap-1">
                                    客戶姓名
                                    <span class="sort-icon" :class="{ active: sortKey === 'custName' }">
                                        <span class="sort-asc"
                                            :class="{ on: sortKey === 'custName' && sortOrder === 'asc' }">▲</span>
                                        <span class="sort-desc"
                                            :class="{ on: sortKey === 'custName' && sortOrder === 'desc' }">▼</span>
                                    </span>
                                </div>
                            </th>
                            <th>異動類型</th>
                            <th @click="toggleSort('pointsChange')" class="sort-th text-end">
                                <div class="d-flex align-items-center justify-content-end gap-1">
                                    異動點數
                                    <span class="sort-icon" :class="{ active: sortKey === 'pointsChange' }">
                                        <span class="sort-asc"
                                            :class="{ on: sortKey === 'pointsChange' && sortOrder === 'asc' }">▲</span>
                                        <span class="sort-desc"
                                            :class="{ on: sortKey === 'pointsChange' && sortOrder === 'desc' }">▼</span>
                                    </span>
                                </div>
                            </th>
                            <th @click="toggleSort('remainPoints')" class="sort-th text-end">
                                <div class="d-flex align-items-center justify-content-end gap-1">
                                    剩餘點數
                                    <span class="sort-icon" :class="{ active: sortKey === 'remainPoints' }">
                                        <span class="sort-asc"
                                            :class="{ on: sortKey === 'remainPoints' && sortOrder === 'asc' }">▲</span>
                                        <span class="sort-desc"
                                            :class="{ on: sortKey === 'remainPoints' && sortOrder === 'desc' }">▼</span>
                                    </span>
                                </div>
                            </th>
                            <th>備註</th>
                            <th @click="toggleSort('createTime')" class="sort-th">
                                <div class="d-flex align-items-center gap-1">
                                    建立時間
                                    <!-- 排序 -->
                                    <span class="sort-icon" :class="{ active: sortKey === 'createTime' }">
                                        <span class="sort-asc"
                                            :class="{ on: sortKey === 'createTime' && sortOrder === 'asc' }">▲</span>
                                        <span class="sort-desc"
                                            :class="{ on: sortKey === 'createTime' && sortOrder === 'desc' }">▼</span>
                                    </span>
                                </div>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="history in sortedHistories" :key="history.recordId">
                            <td><code>{{ history.recordId }}</code></td>
                            <td class="fw-bold text-primary">{{ history.custId }}</td>
                            <td class="fw-bold text-primary" style="cursor: pointer;"
                                @click="selectCustomer({ custId: history.custId, custName: history.custName })">
                                {{ history.custName }}
                            </td>
                            <td><span :class="getChangeTypeBadgeClass(history.changeType)">{{
                                getChangeTypeLabel(history.changeType) }}</span></td>
                            <td class="text-end fw-bold" :class="getPointsColorClass(history.pointsChange)">
                                {{ formatPoints(history.pointsChange) }}
                            </td>
                            <td class="text-end">{{ history.remainPoints }}</td>
                            <td class="text-secondary" style="font-size: var(--text-sm);">{{ history.notes }}</td>
                            <td class="text-secondary" style="font-size: var(--text-sm);">{{
                                formatDateTime(history.createTime) }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- 搜尋後：顯示以下內容（之後加 v-if="isSearched"） -->
        <div v-if="isSearched">

            <!-- 會員資訊 + 點數概覽 -->
            <div class="row g-3 mb-4">

                <!-- 會員資訊 -->
                <div class="col-md-6">
                    <div class="bg-white border rounded-4 shadow-sm p-4 h-100">
                        <p class="text-caption mb-3" style="text-transform: uppercase; letter-spacing: 0.06em;">
                            會員資訊
                        </p>
                        <div class="d-flex flex-column gap-3">
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="text-secondary" style="font-size: var(--text-sm);">姓名</span>
                                <span class="fw-bold">{{ customer.custName }}</span>
                            </div>
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="text-secondary" style="font-size: var(--text-sm);">電話</span>
                                <span>{{ customer.custPhone }}</span>
                            </div>
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="text-secondary" style="font-size: var(--text-sm);">會員編號</span>
                                <code>{{ customer.custId }}</code>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 點數概覽 -->
                <div class="col-md-6">
                    <div class="bg-white border rounded-4 shadow-sm p-4 h-100">
                        <p class="text-caption mb-3" style="text-transform: uppercase; letter-spacing: 0.06em;">
                            點數概覽
                        </p>
                        <div class="row g-3">
                            <div class="col-6">
                                <div class="rounded-3 p-3 text-center"
                                    style="background-color: var(--color-primary-light); border: 1px solid var(--color-primary-border-light);">
                                    <p class="text-caption mb-1">目前點數</p>
                                    <p class="mb-0 fw-bold"
                                        style="font-size: var(--text-2xl); color: var(--color-primary); font-family: var(--font-mono);">
                                        {{ customer.currentPoints }}
                                    </p>
                                    <p class="text-caption mb-0" style="color: var(--color-primary);">pt</p>
                                </div>
                            </div>
                            <div class="col-6">
                                <div class="rounded-3 p-3 text-center"
                                    style="background-color: var(--color-bg-sunken); border: 1px solid var(--color-border);">
                                    <p class="text-caption mb-1">累積總點數</p>
                                    <p class="mb-0 fw-bold"
                                        style="font-size: var(--text-2xl); color: var(--color-text-primary); font-family: var(--font-mono);">
                                        {{ customer.totalAccumulated }}
                                    </p>
                                    <p class="text-caption mb-0">pt</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

            </div>

            <!-- 點數異動 Timeline -->
            <div class="bg-white border rounded-4 shadow-sm overflow-hidden mb-4">
                <div class="px-4 py-3 border-bottom d-flex align-items-center gap-2"
                    style="background-color: var(--color-bg-sunken);">
                    <span class="text-caption" style="text-transform: uppercase; letter-spacing: 0.06em;">
                        點數異動 Timeline
                    </span>
                    <span class="badge bg-primary ms-auto">{{ histories.length }} 筆</span>
                </div>
                <table class="table table-hover align-middle mb-0">
                    <thead>
                        <tr>
                            <th>時間</th>
                            <th>類型</th>
                            <th class="text-end">異動點數</th>
                            <th class="text-end">餘額</th>
                            <th class="text-start ps-5">備註</th>
                        </tr>
                    </thead>
                    <tbody>
                        <!--  v-for 串起資料-->
                        <tr v-for="history in histories" :key="history.recordId">
                            <td class="text-secondary" style="font-size: var(--text-sm);">{{
                                formatDateTime(history.createTime) }}</td>
                            <td><span :class="getChangeTypeBadgeClass(history.changeType)">{{
                                getChangeTypeLabel(history.changeType) }}</span></td>
                            <td class="text-end fw-bold" :class="getPointsColorClass(history.pointsChange)">
                                {{ formatPoints(history.pointsChange) }}
                            </td>
                            <td class="text-end text-secondary">{{ history.remainPoints }} pt</td>
                            <td class="text-secondary ps-5" style="font-size: var(--text-sm);">{{ history.notes }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 票券列表 -->
            <div class="bg-white border rounded-4 shadow-sm overflow-hidden">
                <div class="px-4 py-3 border-bottom d-flex align-items-center gap-2"
                    style="background-color: var(--color-bg-sunken);">
                    <span class="text-caption" style="text-transform: uppercase; letter-spacing: 0.06em;">
                        票券列表
                    </span>
                    <span class="badge bg-primary ms-auto">{{ vouchers.length }} 張</span>
                </div>
                <table class="table table-hover align-middle text-nowrap mb-0">
                    <thead>
                        <tr>
                            <th>票券號碼</th>
                            <th>商品名稱</th>
                            <th>狀態</th>
                            <th class="text-end">花費點數</th>
                            <th class="text-center">到期日</th>
                            <th class="text-center">所屬訂單編號</th>
                            <th>訂單建立時間</th>
                        </tr>
                    </thead>
                    <tbody>
                        <!-- v-for 迴圈抓真實客戶票券資料-->
                        <tr v-for="voucher in vouchers" :key="voucher.voucherId">
                            <td><code>{{ voucher.voucherCode }}</code></td>
                            <td class="fw-bold">{{ voucher.redemptionOrder?.product?.productName }}</td>
                            <td><span class="badge bg-gray">{{ getStatusLabel(voucher.status) }}</span></td>
                            <td class="text-end fw-bold">{{ voucher.redemptionOrder?.pointsSpent /
                                voucher.redemptionOrder?.productQuantity }} pt</td>
                            <td class="text-end text-secondary" style="font-size: var(--text-sm);">{{ voucher.expiryDate
                                }}</td>
                            <td class="text-center"><code>{{ voucher.redemptionOrder?.redemptionId }}</code></td>
                            <td class="text-secondary" style="font-size: var(--text-sm);">{{
                                formatDateTime(voucher.redemptionOrder?.createTime) }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 點數效期 -->
            <div class="bg-white border rounded-4 shadow-sm overflow-hidden mt-4">
                <div class="px-4 py-3 border-bottom d-flex align-items-center gap-2"
                    style="background-color: var(--color-bg-sunken);">
                    <span class="text-caption" style="text-transform: uppercase; letter-spacing: 0.06em;">
                        點數效期
                    </span>
                    <span class="badge bg-primary ms-auto">{{ expiryRecords.length }} 筆</span>
                </div>
                <table class="table table-hover align-middle mb-0">
                    <thead>
                        <tr>
                            <th>到期日</th>
                            <th class="text-end">剩餘可用點數</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="record in expiryRecords" :key="record.expireTime">
                            <td>{{ record.expireTime }}</td>
                            <td class="text-end fw-bold text-primary">
                                {{ record.totalPoints.toLocaleString() }} 點
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div v-if="expiryRecords.length === 0" class="text-center py-3 text-secondary">
                    目前沒有有效點數
                </div>
            </div>

        </div>

    </div>
</template>

<style scoped>
.customer-section-label {
    display: flex;
    align-items: center;
    gap: var(--space-2);
}

.customer-section-label::before {
    content: '';
    display: inline-block;
    width: 3px;
    height: 14px;
    background-color: var(--color-primary);
    border-radius: var(--radius-full);
}

/* 排序樣式 */
.sort-th {
    cursor: pointer;
    user-select: none;
}

.sort-th:hover {
    background-color: var(--color-primary-light);
}

.sort-icon {
    display: inline-flex;
    flex-direction: column;
    font-size: 8px;
    line-height: 1;
    gap: 1px;
    color: var(--color-gray-300);
}

.sort-asc,
.sort-desc {
    display: block;
    transition: color var(--transition-fast);
}

.sort-asc.on {
    color: var(--color-primary);
}

.sort-desc.on {
    color: var(--color-primary);
}
</style>