<script setup>
// import { fileAPI } from '@/api/point/point_file';
import { ref, computed, watch } from 'vue';
import BaseModal from '@/components/common/BaseModal.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import ImageUploader from '@/components/common/ImageUploader.vue';
import AlertToast from '@/components/common/AlertToast.vue';

// 定義從父元件接收的屬性（props）
// isVisible：控制彈窗是否顯示（由父元件的 showModal 控制）
// mode：判斷目前是新增('insert')還是修改('update')模式
const props = defineProps({
    isVisible: {
        type: Boolean,
        required: true,
    },
    mode: {
        type: String,
        required: true,
    },
    productData: {
        type: Object,
        required: false,
    }
});

/// computed 會根據 props.mode 自動計算標題
// 當 mode 改變時，title 會自動跟著更新，不需要手動設定
const title = computed(() => {
    if (props.mode === 'insert') return '新增商品資料'
    if (props.mode === 'update') return '修改商品資料'
    return '商品資料';
});

// 定義這個元件可以發出的事件，用來通知父元件
// 'close'：使用者按取消或關閉 → 通知父元件關閉彈窗
// 'submit'：使用者按確認送出 → 通知父元件並帶著表單資料
const emit = defineEmits(['close', 'submit']);

// 定義表單的初始資料結構
// 使用 ref 讓資料變成響應式，當使用者輸入時畫面會自動更新
// 每個欄位對應後端 Product Entity 的欄位名稱
const form = ref({
    productName: '',
    description: '',
    pointsRequired: null,
    stockQuantity: null,
    isActive: false,
    imageUrl: '',
    category: '',
});

// 處理送出的函式
// 為什麼不直接在 @click 寫 emit('submit', form.value)：
// 因為在 template 裡直接寫，Vue 渲染按鈕時可能還沒有資料
// 改用函式，確保按下按鈕的當下才去取 form.value，資料一定是最新的
const handleSubmit = () => {
    if (!validateForm()) return  // 驗證不通過就不送出
    emit('submit', form.value)
}

//=============監聽資料是否改變========================

watch(() => props.productData, (newData) => {
    // 當 productData 從父元件傳進來時，這裡會自動執行
    // newData 就是新傳進來的商品資料
    if (newData) {
        // 把 newData 的每個欄位填入 form，id不用
        form.value = {
            productName: newData.productName,
            description: newData.description,
            pointsRequired: newData.pointsRequired,
            stockQuantity: newData.stockQuantity,
            isActive: newData.isActive,
            imageUrl: newData.imageUrl,// ← 直接存進 form，不需要 previewUrl
            category: newData.category,
        }
    } else {
        form.value = {
            productName: '',
            description: '',
            pointsRequired: null,
            stockQuantity: null,
            isActive: false,
            imageUrl: '',
            category: '',
        }
    }
});

// 監聽開遮罩時畫面不要捲
// 開啟時新增模式強制清空內容
// 把 isActiveChanged 的初始化移到這裡
watch(() => props.isVisible, (newVal) => {
    if (newVal) {
        // document.body.style.overflow = 'hidden'  // 已移至 BaseModal 處理

        if (props.mode === 'insert') {
            resetForm()
            originalForm.value = { ...form.value }  // 記錄空白狀態
        } else {
            // 開啟時，如果有 productData 就恢復圖片預覽
            // previewUrl 刪掉，ImageUploader 的 previewUrl prop 會處理
            //previewUrl.value = props.productData?.imageUrl || ''
            // ?.是可選鏈運算子，意思是「如果 productData 存在才取 imageUrl，否則回傳 undefined」，避免 productData 是 null 時報錯。
            originalForm.value = { ...form.value }  // 記錄修改前的資料

            // 修改模式開啟時，如果商品是上架中，跳 warning 提醒
            if (props.mode === 'update' && props.productData?.isActive) {
                toast.value.show('您要修改的商品目前上架中', 'warning', 4000)
            }
        }
        // 【關鍵】Modal 開啟後延遲才開始監聽，確保開啟警示不被蓋掉
        setTimeout(() => { isActiveChanged.value = true }, 500)
    } else {
        // document.body.style.overflow = ''  // 已移至 BaseModal 處理
        showConfirm.value = false  // ← 補上這行：Modal 關閉時一併清除 ConfirmDialog
        imageCleared.value = false//← 重置圖片上傳區塊
    }
})

// 【上傳本機圖片】
// A.宣告ref變數存預覽網址
//const previewUrl = ref('');
// B.宣告ref變數存使用者選的檔案（之後上傳用）
//const selectedFile = ref(null);
// C. handleFileChange 函式，當使用者選檔案時執行
//const handleFileChange = async (event) => {
// 從 event.target.files[0] 取得檔案存進 selectedFile
// selectedFile.value = event.target.files[0];
// 用 URL.createObjectURL(file) 產生預覽網址存進 previewUrl
//   previewUrl.value = URL.createObjectURL(selectedFile.value);
//  await handleUpload()  // 選完圖片後自動上傳轉 base64
//};

// 上傳圖片到系統
//const handleUpload = async () => {
//    const formData = new FormData(); // 建立FormData物件
//    formData.append('file', selectedFile.value)
// 呼叫uploadFile api
//    const response = await fileAPI.upload(formData);
// response回傳的是map，有兩個欄位取base64那個欄位
//   form.value.imageUrl = response.data.data.base64;
//};

// 程式模擬點擊那個隱藏的 input
//const fileInput = ref(null)

// 關閉前清空所有內容
const resetForm = () => {
    form.value = {
        productName: '',
        description: '',
        pointsRequired: null,
        stockQuantity: null,
        isActive: false,
        imageUrl: '',// ← 清空 imageUrl 就等於清空圖片，不需要另外清 previewUrl
        category: '',
    }
    imageCleared.value = false  // ← 重置
    //清空所有驗證錯誤
    errors.value = { productName: '', category: '', pointsRequired: '', stockQuantity: '', imageUrl: '' }
    //清空所有互動過的欄位驗證樣式
    touched.value = {
        productName: false,
        category: false,
        pointsRequired: false,
        stockQuantity: false,
        imageUrl: false
    }
    //清空是否手動控制的紀錄
    isActiveChanged.value = false
}


// 記錄原始資料
const originalForm = ref(null)

// 關閉前警示
const handleClose = () => {
    if (props.mode === 'view') {
        emit('close')
        return
    }
    // [dirty check]
    // 開啟彈窗時記錄「原始資料」，關閉時比較「現在的資料」和「原始資料」是否一樣，一樣就直接關，不一樣才跳警示。

    // 比較現在的資料和原始資料是否一樣
    const isDirty = JSON.stringify(form.value) !== JSON.stringify(originalForm.value)

    if (isDirty) {
        // 顯示 ConfirmDialog，剩下等使用者點按鈕
        showConfirm.value = true
        // 按取消就什麼都不做
    } else {
        // 沒有修改，直接關
        resetForm()
        emit('close')
    }
}


// ==================控制 ConfirmDialog 的顯示=====================

const showConfirm = ref(false)


const handleConfirmClose = () => {
    resetForm()
    emit('close')
    showConfirm.value = false
}



// ===============【Demo 用】一鍵填入假資料=====================

// 只在 insert 模式下顯示，方便 Demo 時快速新增商品
const fillDemoData = () => {
    form.value = {
        productName: '車用香氛擴香組',
        description: '精選天然植物精油，讓每次駕車都是一場嗅覺享受，有效去除車內異味。',
        pointsRequired: 800,
        stockQuantity: 50,
        isActive: true,
        imageUrl: 'https://media.gq.com.tw/photos/65d31557a2781944bf6a42e4/master/w_1600%2Cc_limit/2024%25E8%25BB%258A%25E7%2594%25A8%25E6%2593%25B4%25E9%25A6%2599%25E7%259A%25AE%25E5%25A5%2597%2520-%2520%25E9%25BB%2583.jpg',
        category: '汽車配件',
    }
    // 同步更新圖片預覽
    // previewUrl 刪掉，form.imageUrl 改了 ImageUploader 會自動更新

    // 觸發藍色 Toast
    toast.value.show('測試資料已填入！', 'info');
}

//====================圖片預覽=====================

// 記錄使用者是否已手動清除圖片
const imageCleared = ref(false)


// 當 form.imageUrl 被清空時，記錄使用者已手動清除圖片
watch(() => form.value.imageUrl, (newVal) => {
    if (newVal === '') {
        imageCleared.value = true
    } else {
        imageCleared.value = false
    }
})

//================欄位驗證===========================

const errors = ref({
    productName: '',
    category: '',
    pointsRequired: '',
    stockQuantity: '',
    imageUrl: ''
})

const validateForm = () => {

    // 按送出時，把所有欄位標記為 touched，讓錯誤樣式顯示出來
    touched.value = {
        productName: true,
        category: true,
        pointsRequired: true,
        stockQuantity: true,
        imageUrl: true
    }

    errors.value = { productName: '', category: '', pointsRequired: '', stockQuantity: '', imageUrl: '' }

    let valid = true

    // 商品名稱：不可空白
    if (!form.value.productName.trim()) {
        errors.value.productName = '商品名稱為必填'
        valid = false
    }

    // 商品類別：不可為空字串
    if (!form.value.category.trim()) {
        errors.value.category = '請選擇商品類別'
        valid = false
    }

    // 所需點數：必填、必須為正整數
    if (!form.value.pointsRequired || form.value.pointsRequired < 0 || Number(form.value.pointsRequired) % 1 !== 0) {
        errors.value.pointsRequired = '所需點數必須為正整數'
        valid = false
    }
    // 庫存數量：必填、必須為正整數，可為 0
    if (form.value.stockQuantity === null || form.value.stockQuantity === '' || form.value.stockQuantity < 0 || Number(form.value.stockQuantity) % 1 !== 0) {
        errors.value.stockQuantity = '庫存數量必須為正整數'
        valid = false
    }


    // 驗證圖片網址
    if (form.value.imageUrl.trim() &&
        !form.value.imageUrl.startsWith('http') &&
        !form.value.imageUrl.startsWith('data:image')) {
        errors.value.imageUrl = '圖片網址格式不正確，請以 http 開頭';
        valid = false;
    }

    return valid
}


//=======================即時監聽欄位驗證=============================

// 即時驗證：欄位有值時清除錯誤，讓 is-valid 可以顯示
watch(() => form.value.productName, (val) => {
    if (!val || !val.trim()) {
        errors.value.productName = '商品名稱為必填'
    } else {
        errors.value.productName = ''
    }
})

watch(() => form.value.category, (val) => {
    if (!val) {
        errors.value.category = '請選擇商品類別'
    } else {
        errors.value.category = ''
    }
})

watch(() => form.value.pointsRequired, (val) => {
    if (!val || val < 0 || Number(val) % 1 !== 0) {
        errors.value.pointsRequired = '所需點數必須為正整數'
    } else {
        errors.value.pointsRequired = ''
    }
})

watch(() => form.value.stockQuantity, (val) => {
    if (val === null || val === '' || val < 0 || Number(val) % 1 !== 0) {
        errors.value.stockQuantity = '庫存數量必須為正整數'
    } else {
        errors.value.stockQuantity = ''
    }
})

watch(() => form.value.imageUrl, (val) => {
    if (val && !val.startsWith('http') && !val.startsWith('data:image')) {
        errors.value.imageUrl = '圖片網址格式不正確，請以 http 開頭'
    } else {
        errors.value.imageUrl = ''
    }
})


//=====================dirty state==========================

// 驗證樣式只在「使用者有互動過」之後才顯示。

// 記錄哪些欄位被使用者碰過（touched = 互動過）
const touched = ref({
    productName: false,
    category: false,
    pointsRequired: false,
    stockQuantity: false,
    imageUrl: false
})


//================宣告用來控制 Toast 的 ref 變數==============

const toast = ref(null);


//=================紀錄是否有手動切換過上下架，避免警示訊息被疊蓋過去====================
const isActiveChanged = ref(false)  // 記錄使用者是否手動切換過

watch(() => form.value.isActive, (newVal) => {
    // 【為什麼這樣判斷】
    // oldVal === undefined → 初始化，不觸發
    // !isActiveChanged → 還沒手動切換過，不觸發（避免跟開啟警示打架）
    if (!isActiveChanged.value) return
    if (newVal) {
        toast.value.show('商品已設為上架', 'success', 3000)
    } else {
        toast.value.show('商品已設為下架', 'warning', 3000)
    }
})

</script>

<template>
    <!-- 
        重構說明：原本 ProductFormModal 自己做 overlay + header + footer 外殼
        現在改用 BaseModal 當外殼，只負責表單內容和業務邏輯（dirty check）
        
        :isVisible="props.isVisible" → 把父元件傳進來的顯示狀態往下傳給 BaseModal
        :title="title"               → title 是 computed，根據 mode 自動計算，傳給 BaseModal 顯示在 header
        @close="handleClose"         → BaseModal 的 ✕ 按下去會 emit('close')，這裡接住後交給 handleClose 執行 dirty check
        size="lg"                    → 對應原本 modal-box 的 500px 寬度
    -->
    <BaseModal :isVisible="props.isVisible" :title="title" @close="handleClose" size="lg">

        <!-- #body slot：表單內容放這裡 -->
        <template #body>
            <form class="p-1">

                <!-- 上半：左右兩欄 -->
                <!-- Bootstrap row/col 網格：col-5 左欄圖片、col-7 右欄資料，合計 12 -->
                <div class="row g-3">

                    <!-- ══════════════ 左欄：圖片區 ══════════════ -->
                    <div class="col-5">
                        <!--
                            ImageUploader：Dropzone 拖曳上傳
                            v-model="form.imageUrl" → 上傳後自動把 base64 存進 form.imageUrl
                            :previewUrl → 修改模式帶入現有圖片；imageCleared 為 true 時不傳（使用者已手動清除）
                            view 模式：不顯示上傳元件，改顯示唯讀圖片
                        -->
                        <div v-if="props.mode !== 'view'">
                            <!-- form.imageUrl 有值 → previewUrl 傳空字串（已有新圖不需要舊圖）
                               form.imageUrl 是空的 → 傳入原始圖片網址當預覽 -->
                            <ImageUploader v-model="form.imageUrl"
                                :previewUrl="imageCleared ? '' : props.productData?.imageUrl || ''" />

                            <!-- URL 輸入框（兩種方式並存：Dropzone 上傳 or 貼上網址） -->
                            <div class="mt-2">
                                <label class="form-label small text-muted mb-1">或貼上圖片網址</label>
                                <input v-model="form.imageUrl" class="form-control" type="url" :class="{
                                    'is-invalid': errors.imageUrl && touched.imageUrl,
                                    'is-valid': !errors.imageUrl && form.imageUrl && touched.imageUrl
                                }" placeholder="請貼上圖片連結" @input="touched.imageUrl = true">
                                <!-- 圖片網址錯誤訊息（touched 後才顯示） -->
                                <div v-if="errors.imageUrl && touched.imageUrl" class="invalid-feedback d-block">
                                    {{ errors.imageUrl }}
                                </div>
                            </div>
                        </div>

                        <!-- view 模式：只顯示圖片，不可上傳 -->
                        <div v-else
                            class="border rounded-2 bg-light d-flex align-items-center justify-content-center overflow-hidden"
                            style="height: 180px;">
                            <img v-if="form.imageUrl" :src="form.imageUrl" class="w-100 h-100 object-fit-cover">
                            <span v-else class="text-muted small">尚未上傳圖片</span>
                        </div>
                    </div>

                    <!-- ══════════════ 右欄：資料區 ══════════════ -->
                    <!--
                        每個欄位都有兩種顯示模式：
                        v-if="props.mode === 'view'"  → 純文字（唯讀，有層次感的排版）
                        v-else                         → input/select（可編輯，含驗證樣式）
                    -->
                    <div class="col-7">

                        <!-- 商品名稱 -->
                        <!-- 每個欄位用 v-model 雙向綁定到 form 對應的屬性 -->
                        <!-- 使用者輸入時 form.productName 自動更新，不需要手動讀取 -->
                        <div class="mb-3">
                            <label class="form-label small text-muted mb-1">
                                商品名稱 <span v-if="props.mode !== 'view'" class="text-danger">*</span>
                            </label>
                            <!-- view 模式：大字純文字，視覺上有層次感 -->
                            <p v-if="props.mode === 'view'" class="fw-bold fs-5 mb-0">{{ form.productName }}</p>
                            <!-- 編輯模式：input + 驗證樣式 + 錯誤訊息 -->
                            <input v-else v-model="form.productName" class="form-control" type="text" name="productName"
                                :class="{
                                    'is-invalid': errors.productName && touched.productName,
                                    'is-valid': !errors.productName && form.productName && touched.productName
                                }" placeholder="請輸入商品名稱" required @input="touched.productName = true">
                            <div v-if="errors.productName && touched.productName" class="invalid-feedback d-block">
                                {{ errors.productName }}
                            </div>
                        </div>

                        <!-- 商品類別 下拉選單 -->
                        <div class="mb-3">
                            <label class="form-label small text-muted mb-1">
                                商品類別 <span v-if="props.mode !== 'view'" class="text-danger">*</span>
                            </label>
                            <!-- view 模式：純文字 -->
                            <p v-if="props.mode === 'view'" class="fw-semibold mb-0">{{ form.category }}</p>
                            <!-- 編輯模式：select + 驗證樣式 + 錯誤訊息 -->
                            <select v-else v-model="form.category" class="form-select" name="productCategory" required
                                :class="{
                                    'is-invalid': errors.category && touched.category,
                                    'is-valid': !errors.category && form.category && touched.category
                                }" @change="touched.category = true">
                                <option value="" disabled>請選擇商品類別</option>
                                <option value="汽車配件">汽車配件</option>
                                <option value="加值服務">加值服務</option>
                                <option value="禮品兌換">禮品兌換</option>
                                <option value="合作品牌">合作品牌</option>
                            </select>
                            <div v-if="errors.category && touched.category" class="invalid-feedback d-block">
                                {{ errors.category }}
                            </div>
                        </div>

                        <!-- 點數 + 庫存：同一行各佔一半 -->
                        <!-- col-6 各佔 6 格，合計 12 -->
                        <div class="row g-2 mb-3">
                            <div class="col-6">
                                <label class="form-label small text-muted mb-1">
                                    所需點數 <span v-if="props.mode !== 'view'" class="text-danger">*</span>
                                </label>
                                <!-- view 模式：用品牌 accent 色顯示點數，視覺上有層次感 -->
                                <p v-if="props.mode === 'view'" class="fw-bold mb-0"
                                    style="color: var(--color-accent); font-size: var(--text-lg);">
                                    <i class="fa-solid fa-star me-1" style="font-size: 0.85em;"></i>
                                    {{ form.pointsRequired }} 點
                                </p>
                                <!-- 編輯模式：number input + 驗證樣式 -->
                                <input v-else v-model="form.pointsRequired" class="form-control" type="number" :class="{
                                    'is-invalid': errors.pointsRequired && touched.pointsRequired,
                                    'is-valid': !errors.pointsRequired && form.pointsRequired && touched.pointsRequired
                                }" name="pointsRequired" placeholder="點數" min="0" step="1" required
                                    @input="touched.pointsRequired = true">
                                <div v-if="errors.pointsRequired && touched.pointsRequired"
                                    class="invalid-feedback d-block">
                                    {{ errors.pointsRequired }}
                                </div>
                            </div>
                            <div class="col-6">
                                <label class="form-label small text-muted mb-1">
                                    庫存數量 <span v-if="props.mode !== 'view'" class="text-danger">*</span>
                                </label>
                                <!-- view 模式：純文字加單位 -->
                                <p v-if="props.mode === 'view'" class="fw-semibold mb-0">
                                    {{ form.stockQuantity }} 件
                                </p>
                                <!-- 編輯模式：number input + 驗證樣式 -->
                                <input v-else v-model="form.stockQuantity" class="form-control" type="number" :class="{
                                    'is-invalid': errors.stockQuantity && touched.stockQuantity,
                                    'is-valid': !errors.stockQuantity && form.stockQuantity !== null && form.stockQuantity >= 0 && touched.stockQuantity
                                }" name="stockQuantity" placeholder="庫存" min="0" step="1" required
                                    @input="touched.stockQuantity = true">
                                <div v-if="errors.stockQuantity && touched.stockQuantity"
                                    class="invalid-feedback d-block">
                                    {{ errors.stockQuantity }}
                                </div>
                            </div>
                        </div>

                        <!-- 上架狀態 -->
                        <!--
                            view 模式：用 badge 顯示狀態，視覺清楚
                            編輯模式：Bootstrap 5 form-switch（toggle 開關）
                            form-check form-switch → Bootstrap 內建 toggle 樣式
                            role="switch" → 語意標記，讓螢幕閱讀器知道這是開關
                            checkbox 的 v-model 綁定布林值，勾選為 true，未勾選為 false
                        -->
                        <div class="mb-1">
                            <label class="form-label small text-muted mb-1">上架狀態</label>
                            <!-- view 模式：badge 顯示 -->
                            <div v-if="props.mode === 'view'">
                                <span :class="form.isActive ? 'badge bg-success' : 'badge bg-gray'">
                                    {{ form.isActive ? '上架中' : '已下架' }}
                                </span>
                            </div>
                            <!-- 編輯模式：toggle switch + 旁邊動態文字 -->
                            <div v-else class="form-check form-switch d-flex align-items-center gap-2 ps-0">
                                <input v-model="form.isActive" class="form-check-input ms-0" type="checkbox"
                                    role="switch" name="isActive"
                                    style="width: 2.5em; height: 1.25em; cursor: pointer;">
                                <!-- toggle 旁邊動態顯示目前狀態 -->
                                <span :class="form.isActive ? 'text-primary' : 'text-muted'" class="small">
                                    {{ form.isActive ? '上架中' : '已下架' }}
                                </span>
                            </div>
                        </div>

                    </div>
                </div>

                <!-- 下半：商品描述橫跨全寬 -->
                <div class="mt-3">
                    <label class="form-label small text-muted mb-1">商品描述</label>
                    <!-- view 模式：純文字，行高寬鬆好閱讀；無描述時顯示提示文字 -->
                    <p v-if="props.mode === 'view'" class="mb-0" style="line-height: var(--leading-loose);">
                        {{ form.description || '（無描述）' }}
                    </p>
                    <!-- 編輯模式：textarea -->
                    <textarea v-else v-model="form.description" class="form-control" name="description"
                        placeholder="請輸入商品描述" rows="3"></textarea>
                </div>

            </form>
        </template>

        <!-- #footer slot：按鈕區放這裡，對應原本 modal-box 裡的 <div class="modal-footer"> -->
        <template #footer>
            <!-- Demo 用：一鍵填入假資料（只在新增模式顯示） -->
            <button v-if="props.mode === 'insert'" type="button" @click="fillDemoData"
                class="btn btn-outline-warning me-auto">
                <font-awesome-icon icon="fa-solid fa-wand-magic-sparkles me-1" />一鍵填入
            </button>

            <!-- 按送出時呼叫 handleSubmit，把 form.value 傳給父元件 -->
            <button v-if="props.mode !== 'view'" @click="handleSubmit" class="btn btn-primary">確認送出</button>
            <!-- 按取消時呼叫 handleClose，執行 dirty check 後才關閉 -->
            <button @click="handleClose()" class="btn btn-outline-secondary">取消</button>
        </template>

    </BaseModal>

    <ConfirmDialog :isVisible="showConfirm" title="確認關閉" message="確定要關閉嗎？關閉將會遺失所有內容" confirmText="確定關閉"
        confirmVariant="danger" @confirm="handleConfirmClose()" @cancel="showConfirm = false" />

    <AlertToast ref="toast" />

</template>

<style scoped>
/* 移除 is-valid 的綠色勾勾圖示，只保留綠框 */
.form-control.is-valid,
.form-select.is-valid {
    background-image: none;
}
</style>