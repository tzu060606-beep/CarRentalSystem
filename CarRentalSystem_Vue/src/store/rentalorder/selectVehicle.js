import { defineStore } from 'pinia';
import { orderVehicleAPI } from '@/api/rentalorder/ordervehicleAPI';
import { rentalPlansAPI } from '@/api/rentalorder/rentalplan';
import { locationAPI } from '@/api/vehicle/locationApi';

const DEFAULT_IMAGE =
  'https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1200&q=80';

const ALL_CATEGORY = '全部車輛';
const ONE_WAY_DISPATCH_FEE = 500;
const ORDER_DRAFT_STORAGE_KEY = 'rentalOrderDraft';
// 定義了畫面上方車型標籤（Tabs）中，最左邊那個「重置過濾」的按鈕文字。

const RENTAL_PERIODS = [
  { value: 1, label: '1 個月' },
  { value: 3, label: '3 個月' },
  { value: 6, label: '半年' },
  { value: 12, label: '一年' }
];

// ---------------------以下為翻譯官和防呆------------

const toDateTimeLocal = (date) => {
  //專門給 <input type="datetime-local">（日租的取/還車時間）用的。
  //在 state 初始化時被呼叫：toDateTimeLocal(new Date(...))。這裡的 new Date() 產生的當下時間，就被塞進了 date 這個插槽裡。
  const pad = (value) => String(value).padStart(2, '0');
  // padStart(2, '0')： 這是補零神器。如果月份是 5 月，JS 吐出來的數字是 5，但 HTML 要求 05。
  // 字串.padStart(目標長度, 補位字元)，js本身的字串物件內建工具
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const toDateInputValue = (date) => {
  const pad = (value) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

const formatToBackendDate = (value) => {
  /*
  當在前端的 datetime-local 選好時間（例如 2026-05-13T11:28），這字串的長度剛好是 16 個字元。
  但是後端 Java 的 LocalDateTime 或 SQL Server 資料庫，通常強烈要求必須要有「秒數」（也就是 2026-05-13T11:28:00）。
  */

  if (!value) return '';
  return value.length === 16 ? `${value}:00` : value;
};

const addMonths = (dateValue, months) => {
  const date = new Date(`${dateValue}T09:00:00`);
  // 假設客人選了「2026-05-13」取車，租期「3個月」。程式必須自動算出還車日。
  // 強制加上了早上 9 點統一時間
  date.setMonth(date.getMonth() + Number(months || 1));
  // setMonth 是 JS 原生的日期加減法，會自動幫你處理大小月、潤年（例如 1 月 31 日加一個月，它會變成 2 月 28 日）。
  return date;
};

const isLongTermPlan = (plan) => {
  // 在 getters 的 currentPriceRanges 裡，程式碼執行 state.rentalPlans.filter((plan) => ...)。這時候，plan 代表的就是資料庫裡抓回來的「那一份方案物件」。
  const value = plan.longTerm ?? plan.isLongTerm;
  //?? (Nullish Coalescing)：後端傳來的 JSON 欄位名稱可能會變。有時候叫 longTerm，有時候叫 isLongTerm。這行確保兩種名字都能接住。
  // 「空值合併運算符 (Nullish Coalescing Operator)」，如果左邊的東西是null或undefined，才採用右邊的東西
  return value === true || value === 1 || value === '1';
};


const getPlanVehicleType = (plan) => plan.appliedVehicleType || plan.vehicleType || '';
//plan.vehicleType只是額外防呆，最後則是如果都沒資料，就回傳一個空字串

const getPlanPrice = (plan) => Number(plan?.basePrice ?? plan?.price ?? 0);
//Number(...)：資料庫傳回來的金額有時會帶有小數點（如 1500.00），在 JavaScript 有時會被誤判成字串。
// 這行強制將其轉換為數字型別

const normalizeFuelType = (fuelType = '') => {
  // 在 normalizeVehicle 裡呼叫：normalizeFuelType(carModel.fuelType)。這時候，從 API 拿回來的車輛動力資料（可能是 "EV" 或 "汽油"）就被傳進去了。
  const value = String(fuelType).trim();
  // 1. String(fuelType): 預防萬一後端傳來 null 或數字，先強制轉成字串
  // 2. .trim(): 把字串前後不小心多打的「空格」刪掉
  const upperValue = value.toUpperCase();

  if (upperValue.includes('ELECTRIC') || upperValue.includes('EV') || value.includes('電動')) return '電動';
  if (upperValue.includes('HYBRID') || value.includes('油電')) return '油電混合';
  if (upperValue.includes('DIESEL') || value.includes('柴油')) return '柴油';
  if (upperValue.includes('PETROL') || upperValue.includes('GAS') || value.includes('汽油')) return '汽油';

  return value || '未提供';
};
// ---------------------以上為翻譯官和防呆

// ---------------------以下為車輛圖片左上角的車型標籤
const getBadgeClass = (label) => {
  if (label === '電動車' || label === '電動') return 'bg-info';
  if (label === '油電混合') return 'bg-warning';
  if (label === '休旅車') return 'bg-secondary';
  if (label === '廂型車') return 'bg-purple';

  return 'bg-primary';
  
};
// ---------------------以上為車輛圖片左上角的車型標籤
// ---------------------以下為處理顯示的區間格式

const makePriceRangeLabel = (range, unit) => {
  // 傳進來的 range 是一個物件（例如 { min: 1000, max: 2000 }），而 unit 則是計費單位（'日' 或 '月'）：
  const format = (value) => `NT$ ${Number(value).toLocaleString()}`;
  // toLocaleString()： 這是 JavaScript 內建的超強大 API。如果你丟 1500 進去，它會自動變成字串 "1,500"

  if (range.max === Infinity) return `${format(range.min)} 以上 / ${unit}`;
  if (range.min === 0) return `${format(range.max)} 以下 / ${unit}`; //format(range.max)會丟進上面的小涵式處理
  return `${format(range.min)} - ${format(range.max)} / ${unit}`;
};

// ---------------------以上為處理顯示的區間格式
// ---------------------以下為動態計算價格，最終會和價格區間配合

const buildPriceRanges = (prices, unit) => {
  const validPrices = prices
    .map((price) => Number(price)) //.map()對陣列裡的每一個元素，執行相同的動作，然後吐出一個長度一模一樣的全新陣列。Number強制轉型。
    .filter((price) => Number.isFinite(price) && price > 0)//.filter()：嚴格把關，true才加入，false就丟掉
    // Number.isFinite(price)：規則一（是不是正常的數字？把 NaN 或 無窮大 踢掉）。
    .sort((a, b) => a - b); //a - b：這是 JavaScript 數字由小到大（遞增）排序的固定起手式寫法。如果想由大到小排，就寫 b - a。

  if (validPrices.length === 0) return [];
  //如果經過上一關的嚴格篩選後，陣列空了（代表後端傳來的價格全都是壞的，或者根本沒車可租）。

  const min = validPrices[0];//剛剛排序好了，這裡是最低價
  const max = validPrices[validPrices.length - 1];//最高價

  if (min === max) { //極端情況，最小值===最大值，
    return [
      {
        value: `all-${unit}-${min}`,
        label: makePriceRangeLabel({ min: 0, max: Infinity }, unit), // 例如產生出："NT$ 0 以上 / 日"
        min: 0,
        max: Infinity
      }
    ];
  }
  // 上面負責把壞掉的資料踢出去、找出最高與最低價
  //下面把價格切成漂亮的三等份，並把所有的選項打包出貨。

  const firstBreak = Math.ceil((min + (max - min) / 3) / 1000) * 1000; //Math.ceil無條件進位後*1000
  const secondBreak = Math.ceil((min + ((max - min) * 2) / 3) / 1000) * 1000;

  return [
    //建立三個過濾器骨架
    { value: `under-${firstBreak}`, min: 0, max: firstBreak },
    { value: `${firstBreak}-${secondBreak}`, min: firstBreak, max: secondBreak },
    { value: `over-${secondBreak}`, min: secondBreak, max: Infinity }
  ].map((range) => ({
    ...range,
    label: makePriceRangeLabel(range, unit)
  }));
};

/*
[
  { value: "under-2000", min: 0, max: 2000, label: "NT$ 2,000 以下 / 日" },
  { value: "2000-3000",  min: 2000, max: 3000, label: "NT$ 2,000 - NT$ 3,000 / 日" },
  { value: "over-3000",  min: 3000, max: Infinity, label: "NT$ 3,000 以上 / 日" }
]
*/

// ---------------------以上為動態計算價格，最終會和價格區間配合

const getPlanByType = (plans, vehicleType, rentalMode) => {
  const targetIsLongTerm = rentalMode === 'longTerm';
  return plans.find((plan) => {
    return getPlanVehicleType(plan) === vehicleType && isLongTermPlan(plan) === targetIsLongTerm;
  });
};

const getPlanPriceByType = (plans, vehicleType, rentalMode) => {
  const matchedPlan = getPlanByType(plans, vehicleType, rentalMode);
  return matchedPlan ? getPlanPrice(matchedPlan) : 0;
};

const getPlanId = (plan) => plan?.planId ?? plan?.id ?? null;

const getPlanOvertimeFee = (plan) => Number(plan?.overtimeFee ?? plan?.hourlyFee ?? 0);

const parseLocalDateTime = (value) => {
  if (!value) return null;
  const date = new Date(value.length === 16 ? `${value}:00` : value);
  return Number.isNaN(date.getTime()) ? null : date;
};

const addHoursToDateTimeLocal = (value, hours) => {
  const date = parseLocalDateTime(value) || new Date();
  date.setHours(date.getHours() + hours);
  return toDateTimeLocal(date);
};

const buildSearchSignature = (searchParams = {}) => {
  if (searchParams.rentalMode === 'longTerm') {
    return JSON.stringify({
      rentalMode: searchParams.rentalMode,
      pickupLocationId: Number(searchParams.pickupLocationId || 0),
      pickupDate: searchParams.pickupDate || '',
      rentalMonths: Number(searchParams.rentalMonths || 1)
    });
  }

  return JSON.stringify({
    rentalMode: searchParams.rentalMode,
    pickupLocationId: Number(searchParams.pickupLocationId || 0),
    pickupTime: formatToBackendDate(searchParams.pickupTime || ''),
    returnTime: formatToBackendDate(searchParams.returnTime || '')
  });
};

const normalizeVehicle = (vehicle, rentalPlans) => {
  const carModel = vehicle.carModel || {};
  const vehicleType = carModel.vehicleType || vehicle.vehicleType || '未提供車型';
  const powerSource = normalizeFuelType(carModel.fuelType);
  const location = vehicle.location || {};
  const dailyPlan = getPlanByType(rentalPlans, vehicleType, 'daily');
  const monthlyPlan = getPlanByType(rentalPlans, vehicleType, 'longTerm');

  return {
    id: vehicle.vehicleId || vehicle.id,
    modelId: carModel.modelId || vehicle.modelId || null,
    brand: carModel.brand || vehicle.brand || '未知品牌',
    model: carModel.modelName || vehicle.model || '未知車款',
    image: carModel.vehicleImgUrl || vehicle.vehicleImgUrl || DEFAULT_IMAGE,
    plateNo: vehicle.plateNo || vehicle.plate || '',
    color: vehicle.color || '',
    mileage: vehicle.mileage || null,
    vehicleType,
    powerSource,
    locationId: location.locationId || vehicle.locationId || null,
    locationName: location.locationName || '',
    dailyPlanId: getPlanId(dailyPlan),
    monthlyPlanId: getPlanId(monthlyPlan),
    dailyOvertimeFee: getPlanOvertimeFee(dailyPlan),
    dailyPrice: getPlanPrice(dailyPlan),
    monthlyPrice: getPlanPrice(monthlyPlan),
    originalPrice: null,
    tags: [
      { text: vehicleType, bg: getBadgeClass(vehicleType) },
      { text: powerSource, bg: getBadgeClass(powerSource) }
    ].filter((tag, index, tags) => tag.text && tags.findIndex((item) => item.text === tag.text) === index),
    specs: {
      seats: carModel.seats || vehicle.seats || 5,
      bags: carModel.luggageCapacity || vehicle.luggageCapacity || 2,
      power: powerSource
    },
    features: `${location.locationName || '可取車據點'} | 租車保險已包含`
  };
};

const getVehicleModelKey = (car) => {
  return car.modelId || `${car.brand}-${car.model}-${car.vehicleType}`;
};

export const selectVehicleStore = defineStore('vehicle', {
  //資料倉庫 (Pinia State)
  state: () => ({
    vehicles: [],
    lastFetchedSearchSignature: '',
    locations: [],
    rentalPlans: [],
    isLoading: false,
    isInitialLoading: false,
    errorMessage: '',
    selectedVehicle: null,
    // 訂單草稿提供下一頁結帳確認使用，統一保存甲租乙還與調度費資訊。
    orderDraft: {
      selectedVehicle: null,
      isOneWayRental: false,
      dispatchFee: 0
    },
    activeCategory: ALL_CATEGORY,
    currentPage: 1,
    pageSize: 3,
    rentalPeriods: RENTAL_PERIODS,
    searchParams: {
      rentalMode: 'daily',
      pickupLocationId: '',
      returnLocationId: '',
      // 甲租乙還關閉時，還車門市會自動同步取車門市。
      isOneWayRental: false,
      pickupTime: toDateTimeLocal(new Date(Date.now() + 24 * 60 * 60 * 1000)),
      returnTime: toDateTimeLocal(new Date(Date.now() + 2 * 24 * 60 * 60 * 1000)),
      pickupDate: toDateInputValue(new Date(Date.now() + 24 * 60 * 60 * 1000)),
      rentalMonths: 1
    },
    filters: {
      vehicleTypes: [],
      powerSources: [],
      priceRanges: []
    }
  }),

  getters: {
    vehicleTypeOptions(state) {
      const planTypes = state.rentalPlans
        .map((plan) => getPlanVehicleType(plan))
        .filter(Boolean);
      const vehicleTypes = state.vehicles
        .map((vehicle) => vehicle.vehicleType)
        .filter(Boolean);

      return [...new Set([...planTypes, ...vehicleTypes])];
    },

    powerSourceOptions(state) {
      return [...new Set(state.vehicles.map((vehicle) => vehicle.powerSource).filter(Boolean))];
    },

    categories() {
      return [ALL_CATEGORY, ...this.vehicleTypeOptions];
    },

    currentPriceRanges(state) {
      const targetIsLongTerm = state.searchParams.rentalMode === 'longTerm';
      const prices = state.rentalPlans
        .filter((plan) => isLongTermPlan(plan) === targetIsLongTerm)
        .map((plan) => getPlanPrice(plan));
      const unit = targetIsLongTerm ? '月' : '日';

      return buildPriceRanges(prices, unit);
    },

    selectedPriceUnit(state) {
      return state.searchParams.rentalMode === 'longTerm' ? '月' : '日';
    },

    isSearchResultCurrent(state) {
      return Boolean(state.lastFetchedSearchSignature) &&
        state.lastFetchedSearchSignature === buildSearchSignature(state.searchParams);
    },

    filteredVehicles(state) {
      const matchedVehicles = state.vehicles.filter((car) => {
        const currentPrice = state.searchParams.rentalMode === 'longTerm' ? car.monthlyPrice : car.dailyPrice;
        // 門市庫存制：只顯示取車門市實際擁有的車輛，不再做單車跨區調度。
        const pickupLocationMatched =
          Boolean(state.searchParams.pickupLocationId) &&
          Number(car.locationId) === Number(state.searchParams.pickupLocationId);
        const typeMatched =
          state.filters.vehicleTypes.length === 0 ||
          state.filters.vehicleTypes.includes(car.vehicleType);
        const powerMatched =
          state.filters.powerSources.length === 0 ||
          state.filters.powerSources.includes(car.powerSource);
        const priceMatched =
          state.filters.priceRanges.length === 0 ||
          state.filters.priceRanges.some((rangeValue) => {
            const range = this.currentPriceRanges.find((item) => item.value === rangeValue);
            return range && currentPrice >= range.min && currentPrice <= range.max;
          });
        return pickupLocationMatched && typeMatched && powerMatched && priceMatched;
      });

      const uniqueVehicles = new Map();
      matchedVehicles.forEach((car) => {
        const key = getVehicleModelKey(car);
        if (!uniqueVehicles.has(key)) {
          uniqueVehicles.set(key, {
            ...car,
            availableVehicleIds: [car.id],
            availableCount: 1
          });
          return;
        }

        const existingCar = uniqueVehicles.get(key);
        existingCar.availableVehicleIds = [...(existingCar.availableVehicleIds || []), car.id];
        existingCar.availableCount = Number(existingCar.availableCount || 1) + 1;
      });

      return [...uniqueVehicles.values()];
    },

    totalPages() {
      return Math.max(1, Math.ceil(this.filteredVehicles.length / this.pageSize));
    },

    pagedVehicles(state) {
      const start = (state.currentPage - 1) * state.pageSize;
      const isLongTerm = state.searchParams.rentalMode === 'longTerm';
      const priceUnit = isLongTerm ? '月' : '日';

      return this.filteredVehicles.slice(start, start + state.pageSize).map((car) => {
        const basePrice = isLongTerm ? car.monthlyPrice : car.dailyPrice;

        return {
          ...car,
          price: basePrice,
          planId: isLongTerm ? car.monthlyPlanId : car.dailyPlanId,
          overtimeFee: isLongTerm ? 0 : car.dailyOvertimeFee,
          originalPrice: null,
          priceUnit
        };
      });
    },

    isCategoryActive(state) {
      return (category) => {
        if (category === ALL_CATEGORY) {
          return state.filters.vehicleTypes.length === 0;
        }

        return state.filters.vehicleTypes.includes(category);
      };
    }
  },

  // Actions（動作指令）：它們負責發號施令，例如「去呼叫後端 API」或是「把分頁重置回第一頁」。
  //剛打開這個網頁的那一瞬間。在 OrderSelectView.vue 裡面有一段 onMounted(() => { store.initializeOptions(); })，就是它觸發的。
  // 拉取基礎設定參數，會進來「租車據點清單 (locations)」與「所有的定價方案 (rentalPlans)」
  actions: {
    invalidateVehicleSearch() {
      const hadSearchResults = this.vehicles.length > 0 || Boolean(this.lastFetchedSearchSignature);

      this.vehicles = [];
      this.lastFetchedSearchSignature = '';
      this.selectedVehicle = null;
      this.orderDraft = {
        selectedVehicle: null,
        isOneWayRental: Boolean(this.searchParams.isOneWayRental),
        dispatchFee: this.searchParams.isOneWayRental ? ONE_WAY_DISPATCH_FEE : 0
      };
      this.currentPage = 1;
      sessionStorage.removeItem(ORDER_DRAFT_STORAGE_KEY);

      if (hadSearchResults) {
        this.errorMessage = '搜尋條件已變更，請按「更新搜尋」取得最新可用車輛。';
      }
    },

    async initializeOptions() {
      if (this.isInitialLoading || (this.locations.length > 0 && this.rentalPlans.length > 0)) return;

      this.isInitialLoading = true;
      this.errorMessage = '';

      try {
        const [locationResponse, planResponse] = await Promise.all([
          locationAPI.getAll(),
          rentalPlansAPI.getAll()
        ]);

        this.locations = Array.isArray(locationResponse.data) ? locationResponse.data : locationResponse.data?.data || [];
        this.rentalPlans = Array.isArray(planResponse.data) ? planResponse.data : planResponse.data?.data || [];

        const firstLocation = this.locations[0];
        if (firstLocation) {
          const firstLocationId = firstLocation.locationId;
          this.searchParams.pickupLocationId ||= firstLocationId;
          this.searchParams.returnLocationId ||= firstLocationId;
        }
        this.normalizeSearchTimes();
      } catch (error) {
        console.error('Failed to initialize rental options', error);
        this.errorMessage = '目前無法取得租車地點或方案資料，請稍後再試。';
      } finally {
        this.isInitialLoading = false;
      }
    },

    setRentalMode(mode) {
      this.searchParams.rentalMode = mode;
      this.filters.priceRanges = [];
      this.filters.vehicleTypes = [];
      this.activeCategory = ALL_CATEGORY;
      this.currentPage = 1;
      this.normalizeSearchTimes();
      this.invalidateVehicleSearch();
    },

    setCategory(category) {
      // 上方車型按鈕與左側 Checkbox 共用 filters.vehicleTypes，避免兩邊狀態不同步。
      this.activeCategory = category;
      this.filters.vehicleTypes = category === ALL_CATEGORY ? [] : [category];
      this.currentPage = 1;
    },

    syncReturnLocationWithPickup() {
      // 未啟用甲租乙還時，還車地點固定等於取車地點。
      if (!this.searchParams.isOneWayRental) {
        this.searchParams.returnLocationId = this.searchParams.pickupLocationId;
      }
    },

    normalizeSearchTimes() {
      const now = new Date();

      if (this.searchParams.rentalMode === 'longTerm') {
        const today = toDateInputValue(now);
        if (!this.searchParams.pickupDate || this.searchParams.pickupDate < today) {
          this.searchParams.pickupDate = today;
        }
        return;
      }

      const minPickupTime = toDateTimeLocal(now);
      const pickupDate = parseLocalDateTime(this.searchParams.pickupTime);
      const returnDate = parseLocalDateTime(this.searchParams.returnTime);

      if (!pickupDate || pickupDate < now) {
        this.searchParams.pickupTime = minPickupTime;
      }

      const normalizedPickupDate = parseLocalDateTime(this.searchParams.pickupTime);
      if (!returnDate || (normalizedPickupDate && returnDate <= normalizedPickupDate)) {
        this.searchParams.returnTime = addHoursToDateTimeLocal(this.searchParams.pickupTime, 24);
      }
    },

    validateSearchTimes() {
      this.normalizeSearchTimes();

      if (this.searchParams.rentalMode === 'longTerm') return true;

      const pickupDate = parseLocalDateTime(this.searchParams.pickupTime);
      const returnDate = parseLocalDateTime(this.searchParams.returnTime);

      if (!pickupDate || !returnDate) {
        this.errorMessage = '請選擇取車與還車時間。';
        return false;
      }

      if (returnDate <= pickupDate) {
        this.errorMessage = '還車時間必須晚於取車時間。';
        return false;
      }

      return true;
    },

    setPage(page) {
      if (page < 1 || page > this.totalPages) return;
      this.currentPage = page;
    },

    resetPagination() {
      this.currentPage = Math.min(this.currentPage, this.totalPages);
      if (this.currentPage < 1) {
        this.currentPage = 1;
      }
    },

    selectVehicle(vehicle) {
      if (!this.isSearchResultCurrent) {
        this.invalidateVehicleSearch();
        this.errorMessage = '搜尋條件已變更，請先按「更新搜尋」重新取得可用車輛。';
        return false;
      }

      // 調度費改由「甲租乙還」統一決定，下一頁再呈現與計算總價。
      const isOneWayRental = Boolean(this.searchParams.isOneWayRental);
      const dispatchFee = isOneWayRental ? ONE_WAY_DISPATCH_FEE : 0;
      const selectedVehicle = {
        ...vehicle,
        isOneWayRental,
        dispatchFee,
        oneWayDispatchFee: dispatchFee
      };

      this.selectedVehicle = selectedVehicle;
      this.orderDraft = {
        selectedVehicle,
        isOneWayRental,
        dispatchFee
      };

      sessionStorage.setItem(ORDER_DRAFT_STORAGE_KEY, JSON.stringify({
        selectedVehicle,
        orderDraft: this.orderDraft,
        searchParams: this.searchParams
      }));

      return true;
    },

    restoreOrderDraft() {
      if (this.selectedVehicle) return;

      const storedDraft = sessionStorage.getItem(ORDER_DRAFT_STORAGE_KEY);
      if (!storedDraft) return;

      try {
        const parsedDraft = JSON.parse(storedDraft);

        this.selectedVehicle = parsedDraft.selectedVehicle || null;
        this.orderDraft = parsedDraft.orderDraft || this.orderDraft;

        if (parsedDraft.searchParams) {
          this.searchParams = {
            ...this.searchParams,
            ...parsedDraft.searchParams
          };
          this.normalizeSearchTimes();
        }
      } catch (error) {
        console.error('Failed to restore rental order draft', error);
      }
    },

    applySearchParamsFromQuery(query = {}) {
      const hasHomeSearchQuery =
        query.fromHome === '1' ||
        Boolean(query.pickupLocationId || query.returnLocationId || query.pickupTime || query.returnTime);

      if (!hasHomeSearchQuery) return false;

      const isOneWayRental = query.isOneWayRental === 'true' || query.isOneWayRental === true;

      this.searchParams = {
        ...this.searchParams,
        rentalMode: query.rentalMode || 'daily',
        pickupLocationId: query.pickupLocationId ? Number(query.pickupLocationId) : this.searchParams.pickupLocationId,
        returnLocationId: query.returnLocationId ? Number(query.returnLocationId) : this.searchParams.returnLocationId,
        isOneWayRental,
        pickupTime: query.pickupTime || this.searchParams.pickupTime,
        returnTime: query.returnTime || this.searchParams.returnTime,
        pickupDate: query.pickupDate || this.searchParams.pickupDate,
        rentalMonths: query.rentalMonths ? Number(query.rentalMonths) : this.searchParams.rentalMonths
      };

      this.filters.vehicleTypes = [];
      this.filters.powerSources = [];
      this.filters.priceRanges = [];
      this.activeCategory = ALL_CATEGORY;
      this.currentPage = 1;
      this.normalizeSearchTimes();

      return true;
    },

    getSearchTimeRange() {
      if (this.searchParams.rentalMode === 'longTerm') {
        const pickupStart = `${this.searchParams.pickupDate}T09:00:00`;
        const returnDate = addMonths(this.searchParams.pickupDate, this.searchParams.rentalMonths);
        return {
          start: pickupStart,
          end: formatToBackendDate(toDateTimeLocal(returnDate))
        };
      }

      return {
        start: formatToBackendDate(this.searchParams.pickupTime),
        end: formatToBackendDate(this.searchParams.returnTime)
      };
    },

    //拉取可租車輛 (fetchVehicles)，取/還車時間地點，「按下『更新搜尋』那顆按鈕的時候」。
    //
    async fetchVehicles() {
      await this.initializeOptions();
      this.syncReturnLocationWithPickup();

      if (!this.validateSearchTimes()) return;

      const { start, end } = this.getSearchTimeRange();

      if (!this.searchParams.pickupLocationId || !this.searchParams.returnLocationId) {
        this.errorMessage = '請選擇取車與還車地點。';
        return;
      }

      if (
        this.searchParams.isOneWayRental &&
        Number(this.searchParams.pickupLocationId) === Number(this.searchParams.returnLocationId)
      ) {
        this.errorMessage = '甲租乙還時，取車地點與還車地點不能相同。';
        return;
      }

      if (!start || !end) {
        this.errorMessage = this.searchParams.rentalMode === 'longTerm'
          ? '請選擇取車日期與租用期數。'
          : '請選擇取車與還車時間。';
        return;
      }

      this.isLoading = true;
      this.errorMessage = '';
      const requestedSearchSignature = buildSearchSignature(this.searchParams);

      try {
        const response = await orderVehicleAPI.getAvailable(start, end);
        if (requestedSearchSignature !== buildSearchSignature(this.searchParams)) return;

        const payload = Array.isArray(response.data) ? response.data : response.data?.data || [];
        this.vehicles = payload.map((vehicle) => normalizeVehicle(vehicle, this.rentalPlans));
        this.lastFetchedSearchSignature = requestedSearchSignature;
        this.currentPage = 1;
      } catch (error) {
        console.error('Failed to fetch available vehicles', error);
        const status = error.response?.status;
        this.errorMessage =
          status === 403
            ? '目前沒有權限查詢可租車輛，請確認是否已登入，或請後端開放 /api/vehicle/available 給前台使用。'
            : '目前無法取得可租車輛，請稍後再試。';
        this.vehicles = [];
        this.lastFetchedSearchSignature = '';
      } finally {
        this.isLoading = false;
      }
    }
  }
});




/*
系統運作流程：
客人透過畫面把條件塞進 State.searchParams ➡️ Action 拿著這些條件去敲後端 API 大門 
➡️ 後端把車子送進來，經過「區塊二」的加工後存入 State.vehicles 
➡️ Getters 發現倉庫有新車了，立刻根據客人的 filters 篩選出結果 
➡️ 畫面呈現！
*/


/*
第一階段：客人輸入 ➡️ State.searchParams
客人在畫面上操作下拉選單或日期選擇器，資料透過 v-model 即時同步到 Pinia 的倉庫。

state: () => ({
  searchParams: {
    rentalMode: 'daily',
    pickupLocationId: '',
    // ... 其他時間參數
  }
})

OrderSelectView.vue那端
<select v-model.number="store.searchParams.pickupLocationId">
  <option v-for="location in store.locations" :value="location.locationId">
    {{ location.locationName }}
  </option>
</select>
================================

第二階段：Action 帶參數敲門 ➡️ 後端 API

async fetchVehicles() {
  // 1. 先呼叫工具函式整理時間格式
  const { start, end } = this.getSearchTimeRange(); 

  // 2. 帶著整理好的參數，敲 API 大門
  const response = await orderVehicleAPI.getAvailable(start, end);

  // 3. 取得後端回傳的原物料 (payload)
  const payload = response.data; 
}
================================
第三階段：資料加工 ➡️ 存入 State.vehicles

後端給的是「原物料」，前端需要透過 normalizeVehicle 進行加工（補圖、算價格、美化文字），最後存入倉庫。

加工廠內部的轉換邏輯 (selectVehicle.js)：
// 將每一台車都送進加工站處理
this.vehicles = payload.map((vehicle) => normalizeVehicle(vehicle, this.rentalPlans));

加工站的詳細工序 (normalizeVehicle)：
const normalizeVehicle = (vehicle, rentalPlans) => {
  return {
    // 這裡就是加工過程：
    model: carModel.modelName,
    image: carModel.vehicleImgUrl || DEFAULT_IMAGE, // 沒圖的補預設圖
    dailyPrice: getPlanPriceByType(rentalPlans, vehicleType, 'daily'), // 算好價格
    // ... 封裝成前端元件看得懂的格式
  };
};
================================
第四階段：Getters 自動篩選 ➡️ 產出成品清單
Getters 像是一個自動感應的監視器，只要 state.vehicles 變了，或客人勾選了「側邊欄篩選」，它就會自動重新跑一遍篩選邏輯。
Pinia 端的篩選邏輯 (selectVehicle.js)：

filteredVehicles(state) {
  return state.vehicles.filter((car) => {
    // 比對車型是否符合勾選條件
    const typeMatched = state.filters.vehicleTypes.length === 0 || 
                        state.filters.vehicleTypes.includes(car.vehicleType);

    // 比對價格區間是否符合勾選條件
    const priceMatched = state.filters.priceRanges.some((rangeValue) => {
       const range = this.currentPriceRanges.find(r => r.value === rangeValue);
       return currentPrice >= range.min && currentPrice <= range.max;
    });

    return typeMatched && priceMatched; // 同時符合才過濾出來
  });
}

================================

第五階段：成品推上舞台 ➡️ 畫面完美呈現
最後，Vue 的 Template 只需要負責巡迴這份「最後產出的清單（pagedVehicles）」，把它們丟進元件裡渲染。

Vue 畫面端的渲染 (OrderSelectView.vue)：

<OrderVehicleCard
  v-for="car in store.pagedVehicles"
  :key="car.id"
  :car="car"
/>

每一個區塊都各司其職。

State 像「筆記本」，只記錄現在選了什麼、抓到了什麼。

Actions 像「外送員」，只負責把外面的資料帶進來。

Getters 像「大腦」，負責思考「根據現在的筆記，畫面上該顯示什麼」。

Template (Vue) 像「臉」，只負責把大腦想好的東西顯示出來。

這樣的設計讓你以後不論是要改 UI 樣式，還是要改過濾規則，都只要去對應的區塊修改就好，不會動一髮而牽全身！


*/
