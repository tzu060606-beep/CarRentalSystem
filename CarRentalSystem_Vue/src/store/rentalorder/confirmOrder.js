import { defineStore } from 'pinia';
import { authAPI } from '@/api/index';
import { confirmOrderAPI } from '@/api/rentalorder/confirmOrderAPI';
import { selectVehicleStore } from '@/store/rentalorder/selectVehicle';

const createEmptyDriverInfo = () => ({
  custId: null,
  fullName: '',
  phoneNumber: '',
  email: '',
  licenseNumber: '',
  orderRemark: ''
});

const createEmptyCardInfo = () => ({
  cardNumber: '',
  expiryDate: '',
  cvc: ''
});

const THIRD_PARTY_PAYMENT_METHODS = new Set(['CREDIT_CARD', 'TRANSFER', 'MOBILE_PAY']);
const PENDING_ECPAY_REQUEST_STORAGE_KEY = 'rentalPendingEcpayRequest';

const PAYMENT_CODE_MAP = {
  card: 'CREDIT_CARD',
  creditcard: 'CREDIT_CARD',
  credit_card: 'CREDIT_CARD',
  CREDIT_CARD: 'CREDIT_CARD',
  信用卡: 'CREDIT_CARD',
  transfer: 'TRANSFER',
  TRANSFER: 'TRANSFER',
  轉帳: 'TRANSFER',
  cash: 'CASH',
  CASH: 'CASH',
  現金: 'CASH',
  mobile: 'MOBILE_PAY',
  mobilepay: 'MOBILE_PAY',
  mobile_pay: 'MOBILE_PAY',
  MOBILE_PAY: 'MOBILE_PAY',
  line_pay: 'MOBILE_PAY',
  LINE_PAY: 'MOBILE_PAY',
  行動支付: 'MOBILE_PAY'
};

const PAYMENT_LABEL_MAP = {
  credit_card: '信用卡',
  CREDIT_CARD: '信用卡',
  card: '信用卡',
  cash: '現金',
  CASH: '現金',
  transfer: '轉帳',
  TRANSFER: '轉帳',
  mobile_pay: '行動支付',
  MOBILE_PAY: '行動支付',
  line_pay: 'LINE Pay'
};

const DISPLAY_TEXT_MAP = {
  'Credit Card': '信用卡',
  Cash: '現金',
  Transfer: '轉帳',
  'Mobile Pay': '行動支付',
  'Monthly Base Rate': '月租基本費',
  'Daily Base Rate': '日租基本費',
  'Insurance (Premium Plus)': '保險費（進階保障）',
  'Subscription Fee Discount': '長租優惠折抵',
  'Airport Surcharge': '機場附加費',
  'All taxes and fees included': '已含所有稅金與費用',
  'Free Cancellation': '免費取消',
  'Secure Payment': '安全付款'
};

const translateDisplayText = (value = '') => {
  const text = String(value || '').trim();
  return DISPLAY_TEXT_MAP[text] || text;
};

const normalizePaymentEnumCode = (value = '') => {
  const text = String(value || '').trim();
  if (!text) return '';

  return PAYMENT_CODE_MAP[text] || PAYMENT_CODE_MAP[text.toLowerCase()] || PAYMENT_CODE_MAP[text.toUpperCase()] || text.toUpperCase();
};

const normalizePaymentOption = (option, index) => {
  const code = option.code || option.type || option.value || '';
  const normalizedCode = normalizePaymentEnumCode(code);
  const iconMap = {
    credit_card: 'fa-solid fa-credit-card',
    CREDIT_CARD: 'fa-solid fa-credit-card',
    card: 'fa-solid fa-credit-card',
    cash: 'fa-solid fa-money-bill-wave',
    CASH: 'fa-solid fa-money-bill-wave',
    transfer: 'fa-solid fa-building-columns',
    TRANSFER: 'fa-solid fa-building-columns',
    mobile_pay: 'fa-solid fa-mobile-screen-button',
    MOBILE_PAY: 'fa-solid fa-mobile-screen-button',
    mobile: 'fa-solid fa-mobile-screen-button',
    行動支付: 'fa-solid fa-mobile-screen-button',
    line_pay: 'fa-solid fa-mobile-screen-button'
  };

  return {
    id: option.id || normalizedCode || code || `payment-${index}`,
    code: normalizedCode || code,
    label: PAYMENT_LABEL_MAP[code] || PAYMENT_LABEL_MAP[normalizedCode] || translateDisplayText(option.label || option.name || option.displayName || ''),
    icon: option.icon || option.iconClass || iconMap[code] || iconMap[normalizedCode] || 'fa-solid fa-wallet',
    requiresCard: Boolean(option.requiresCard ?? option.needCardInfo ?? ['credit_card', 'card'].includes(String(code).toLowerCase())),
    usesGateway: Boolean(option.usesGateway ?? THIRD_PARTY_PAYMENT_METHODS.has(normalizedCode)),
    disabled: Boolean(option.disabled)
  };
};

const normalizePriceItem = (item, index) => ({
  id: item.id || item.code || `price-${index}`,
  label: translateDisplayText(item.label || item.name || ''),
  amount: Number(item.amount ?? item.value ?? item.price ?? 0)
});

const normalizeTrustMarker = (marker, index) => ({
  id: marker.id || marker.code || `marker-${index}`,
  label: translateDisplayText(marker.label || marker.name || ''),
  icon: marker.icon || marker.iconClass || 'fa-solid fa-circle-check'
});

const normalizeVehicleSummary = (vehicle = {}) => ({
  id: vehicle.id || vehicle.vehicleId || null,
  modelId: vehicle.modelId || null,
  brand: vehicle.brand || '',
  model: vehicle.model || vehicle.modelName || '',
  name: vehicle.name || [vehicle.brand, vehicle.model || vehicle.modelName].filter(Boolean).join(' '),
  image: vehicle.image || vehicle.vehicleImgUrl || vehicle.imageUrl || '',
  alt: vehicle.alt || vehicle.imageAlt || vehicle.name || [vehicle.brand, vehicle.model || vehicle.modelName].filter(Boolean).join(' '),
  vehicleType: vehicle.vehicleType || '',
  dailyPrice: Number(vehicle.dailyPrice ?? vehicle.price ?? 0),
  monthlyPrice: Number(vehicle.monthlyPrice ?? vehicle.price ?? 0)
});

const toDateTime = (value) => {
  if (!value) return null;
  return new Date(value.length === 16 ? `${value}:00` : value);
};

const addMonthsToDate = (dateValue, months) => {
  const date = new Date(`${dateValue}T09:00:00`);
  date.setMonth(date.getMonth() + Number(months || 1));
  return date;
};

const formatDateTimeText = (value) => {
  const date = value instanceof Date ? value : toDateTime(value);
  if (!date || Number.isNaN(date.getTime())) return '';

  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date);
};

const formatDurationText = (start, end) => {
  if (!start || !end) return '';

  const durationMs = Math.max(end.getTime() - start.getTime(), 0);
  const dayMs = 24 * 60 * 60 * 1000;
  const hourMs = 60 * 60 * 1000;
  const minuteMs = 60 * 1000;
  const days = Math.floor(durationMs / dayMs);
  const hours = Math.floor((durationMs % dayMs) / hourMs);
  const minutes = Math.floor((durationMs % hourMs) / minuteMs);

  return `共 ${days} 日 ${hours} 時 ${minutes} 分`;
};

const getLocationName = (locations, locationId, fallback = '') => {
  const matchedLocation = locations.find((location) => Number(location.locationId) === Number(locationId));
  return matchedLocation?.locationName || fallback;
};

const toBackendDateTime = (value) => {
  const date = value instanceof Date ? value : toDateTime(value);
  if (!date || Number.isNaN(date.getTime())) return null;

  const pad = (number) => String(number).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const getOrderTimeRange = (searchParams) => {
  if (searchParams.rentalMode === 'longTerm') {
    const pickupTime = searchParams.pickupDate ? new Date(`${searchParams.pickupDate}T09:00:00`) : null;
    const returnTime = searchParams.pickupDate ? addMonthsToDate(searchParams.pickupDate, searchParams.rentalMonths) : null;

    return {
      pickupTime: toBackendDateTime(pickupTime),
      returnTime: toBackendDateTime(returnTime)
    };
  }

  return {
    pickupTime: toBackendDateTime(searchParams.pickupTime),
    returnTime: toBackendDateTime(searchParams.returnTime)
  };
};

const sumPriceItems = (priceItems, matcher) => {
  return priceItems
    .filter(matcher)
    .reduce((total, item) => total + Number(item.amount || 0), 0);
};

const normalizePaymentCode = (paymentOption, fallback = '') => {
  const code = String(paymentOption?.code || fallback || '').trim();
  return normalizePaymentEnumCode(code) || null;
};

const extractCreatedOrderId = (response) => {
  return response?.data?.orderId || response?.data?.data?.orderId || response?.data?.id || '';
};

const getFrontendReturnUrl = () => {
  if (typeof window === 'undefined') return '/orders/custsuccess';
  return `${window.location.origin}/orders/custsuccess`;
};

const getEcpayServerReturnUrl = () => {
  return import.meta.env?.VITE_ECPAY_RETURN_URL || '';
};

const buildPaymentGatewayRequest = ({ response, payload, paymentMethod, selectedPaymentOption }) => {
  if (!THIRD_PARTY_PAYMENT_METHODS.has(paymentMethod)) return null;

  return {
    orderId: extractCreatedOrderId(response),
    paymentMethod,
    provider: selectedPaymentOption?.provider || 'ECPAY',
    amount: payload.deposit,
    totalAmount: payload.totalAmount,
    description: 'OneRent 租車訂金',
    returnUrl: getEcpayServerReturnUrl(),
    clientBackUrl: getFrontendReturnUrl(),
    createdAt: toBackendDateTime(new Date())
  };
};

const savePendingEcpayRequest = (request) => {
  if (typeof sessionStorage === 'undefined') return;

  if (!request) {
    sessionStorage.removeItem(PENDING_ECPAY_REQUEST_STORAGE_KEY);
    return;
  }

  sessionStorage.setItem(PENDING_ECPAY_REQUEST_STORAGE_KEY, JSON.stringify(request));
};

const getApiErrorMessage = (error, fallback) => {
  const data = error?.response?.data;
  if (typeof data === 'string' && data.trim()) return data;
  return data?.error || data?.message || fallback;
};

const buildSubmitPayload = ({ driverInfo, selectedPaymentMethod, selectedPaymentOption, orderPreview, vehicleStore, selectedVehicle }) => {
  const searchParams = vehicleStore.searchParams || {};
  const isLongTerm = searchParams.rentalMode === 'longTerm';
  const priceItems = orderPreview.priceItems || [];
  const { pickupTime, returnTime } = getOrderTimeRange(searchParams);
  const rentalFee = sumPriceItems(priceItems, (item) =>
    ['daily-base-rate', 'monthly-base-rate', 'short-term-overtime-fee'].includes(item.id)
  );
  const extraFee = sumPriceItems(priceItems, (item) =>
    ['one-way-dispatch-fee'].includes(item.id)
  );
  const totalAmount = Number(orderPreview.totalAmount || rentalFee + extraFee || 0);
  const deposit = Number(orderPreview.refundableDeposit || Math.round(totalAmount * 0.1));
  const paymentMethod = normalizePaymentCode(selectedPaymentOption, selectedPaymentMethod);
  const contractMonths = Number(searchParams.rentalMonths || 1);

  return {
    custId: Number(driverInfo.custId || 1),
    vehicleId: Number(selectedVehicle?.id || selectedVehicle?.vehicleId || 0) || null,
    orderType: isLongTerm ? 'LONG_TERM' : 'SHORT_TERM',
    planId: selectedVehicle?.planId || null,
    pickupLocationId: Number(searchParams.pickupLocationId || 0) || null,
    returnLocationId: Number(searchParams.returnLocationId || searchParams.pickupLocationId || 0) || null,
    pickupTime,
    returnTime,
    rentalFee,
    extraFee,
    deposit,
    totalAmount,
    payStatus: THIRD_PARTY_PAYMENT_METHODS.has(paymentMethod) ? 'UNPAID' : 'DEPOSIT_PAID',
    orderTime: toBackendDateTime(new Date()),
    orderStatus: 'RESERVED',
    depositPayMethod: paymentMethod,
    balancePayMethod: null,
    orderRemark: String(driverInfo.orderRemark || '').slice(0, 255),
    invoiceNo: null,
    contract: null,
    remainingBalance: Math.max(totalAmount - deposit, 0),
    shortTerm: isLongTerm
      ? null
      : {
          actualPickupTime: null,
          actualReturnTime: null,
          startMileage: null,
          endMileage: null,
          noteDesc: String(driverInfo.orderRemark || '').slice(0, 255),
          fuelLevelReturn: null
        },
    longTerm: isLongTerm
      ? {
          actualPickupTime: null,
          actualReturnTime: null,
          contractMonths,
          monthlyPayment: Number(selectedVehicle?.monthlyPrice || selectedVehicle?.price || 0),
          billingDay: pickupTime ? Number(pickupTime.slice(8, 10)) : null,
          paidMonths: 0,
          startMileage: null,
          endMileage: null,
          noteDesc: String(driverInfo.orderRemark || '').slice(0, 255)
        }
      : null
  };
};

const buildLocalOrderPreview = (selectedVehicle = {}, vehicleStore) => {
  const searchParams = vehicleStore.searchParams || {};
  const orderDraft = vehicleStore.orderDraft || {};
  const isLongTerm = searchParams.rentalMode === 'longTerm';
  const vehicle = normalizeVehicleSummary(selectedVehicle || {});
  const pickupLocationText = getLocationName(vehicleStore.locations, searchParams.pickupLocationId, selectedVehicle?.locationName || '');
  const returnLocationText = getLocationName(vehicleStore.locations, searchParams.returnLocationId, pickupLocationText);
  const priceItems = [];

  let rentalPeriodText = '';
  let durationText = '';

  if (isLongTerm) {
    const months = Number(searchParams.rentalMonths || 1);
    const start = searchParams.pickupDate ? new Date(`${searchParams.pickupDate}T09:00:00`) : null;
    const end = searchParams.pickupDate ? addMonthsToDate(searchParams.pickupDate, months) : null;
    const monthlyPrice = Number(selectedVehicle?.monthlyPrice ?? selectedVehicle?.price ?? 0);
    const baseAmount = monthlyPrice * months;

    rentalPeriodText = `${formatDateTimeText(start)} - ${formatDateTimeText(end)}`;
    durationText = `${months} 個月`;

    if (baseAmount > 0) {
      priceItems.push({
        id: 'monthly-base-rate',
        label: `月租基本費（${months} 個月）`,
        amount: baseAmount
      });
    }
  } else {
    const start = toDateTime(searchParams.pickupTime);
    const end = toDateTime(searchParams.returnTime);
    const dailyPrice = Number(selectedVehicle?.dailyPrice ?? selectedVehicle?.price ?? 0);
    const durationMs = start && end ? Math.max(end.getTime() - start.getTime(), 0) : 0;
    const totalHours = Math.floor(durationMs / (60 * 60 * 1000));
    const fullDays = Math.floor(totalHours / 24);
    const extraHours = fullDays > 0 ? totalHours % 24 : 0;
    const baseDayCount = Math.max(fullDays, 1);
    const baseAmount = dailyPrice * baseDayCount;
    const overtimeFee = Number(selectedVehicle?.overtimeFee ?? selectedVehicle?.dailyOvertimeFee ?? 0);
    const overtimeAmount = Math.min(overtimeFee * extraHours, dailyPrice);

    rentalPeriodText = `${formatDateTimeText(start)} - ${formatDateTimeText(end)}`;
    durationText = formatDurationText(start, end);

    if (baseAmount > 0) {
      priceItems.push({
        id: 'daily-base-rate',
        label: `日租基本費（${baseDayCount} 日）`,
        amount: baseAmount
      });
    }

    if (extraHours > 0 && overtimeAmount > 0) {
      priceItems.push({
        id: 'short-term-overtime-fee',
        label: `加時計費（${extraHours} 小時）`,
        amount: overtimeAmount
      });
    }
  }

  const dispatchFee = Number(orderDraft.dispatchFee ?? selectedVehicle?.dispatchFee ?? 0);
  const isOneWayRental = Boolean(orderDraft.isOneWayRental ?? selectedVehicle?.isOneWayRental);

  if (isOneWayRental && dispatchFee > 0) {
    priceItems.push({
      id: 'one-way-dispatch-fee',
      label: '甲地乙還調度費',
      amount: dispatchFee
    });
  }

  return {
    vehicle,
    rentalMode: searchParams.rentalMode || '',
    rentalPeriodText,
    pickupLocationText,
    returnLocationText,
    durationText,
    vehicleTypeText: selectedVehicle?.vehicleType || '',
    priceItems,
    totalAmount: priceItems.reduce((total, item) => total + Number(item.amount || 0), 0),
    refundableDeposit: Math.round(priceItems.reduce((total, item) => total + Number(item.amount || 0), 0) * 0.1),
    includedTaxText: '已含租車基本費用',
    trustMarkers: []
  };
};

const normalizeOrderPreview = (payload = {}, selectedVehicle = null, fallbackPreview = {}) => {
  const sourceVehicle = payload.vehicle || payload.selectedVehicle || selectedVehicle || {};
  const vehicle = normalizeVehicleSummary(sourceVehicle);
  const sourcePriceItems = Array.isArray(payload.priceItems) ? payload.priceItems : payload.priceBreakdown || [];
  const priceItems = sourcePriceItems.length > 0 ? sourcePriceItems.map(normalizePriceItem) : fallbackPreview.priceItems || [];
  const payloadTotalAmount = Number(payload.totalAmount ?? payload.total ?? 0);
  const priceItemsTotal = priceItems.reduce((total, item) => total + Number(item.amount || 0), 0);
  const totalAmount = payloadTotalAmount > 0 ? payloadTotalAmount : priceItemsTotal;

  return {
    vehicle,
    rentalMode: payload.rentalMode || payload.mode || fallbackPreview.rentalMode || '',
    rentalPeriodText: payload.rentalPeriodText || payload.rentalPeriod || fallbackPreview.rentalPeriodText || '',
    pickupLocationText: payload.pickupLocationText || payload.pickupLocation || fallbackPreview.pickupLocationText || '',
    returnLocationText: payload.returnLocationText || payload.returnLocation || fallbackPreview.returnLocationText || '',
    durationText: payload.durationText || fallbackPreview.durationText || '',
    vehicleTypeText: payload.vehicleTypeText || fallbackPreview.vehicleTypeText || vehicle.vehicleType || '',
    priceItems,
    totalAmount,
    refundableDeposit: Number(payload.refundableDeposit ?? payload.deposit ?? fallbackPreview.refundableDeposit ?? 0),
    includedTaxText: translateDisplayText(payload.includedTaxText || fallbackPreview.includedTaxText || ''),
    trustMarkers: (Array.isArray(payload.trustMarkers) ? payload.trustMarkers : fallbackPreview.trustMarkers || []).map(normalizeTrustMarker).filter((marker) => marker.label)
  };
};

export const useOrderConfirmStore = defineStore('orderConfirm', {
  state: () => ({
    driverInfo: createEmptyDriverInfo(),
    paymentOptions: [],
    selectedPaymentMethod: '',
    cardInfo: createEmptyCardInfo(),
    pendingPaymentGatewayRequest: null,
    orderPreview: normalizeOrderPreview(),
    isLoadingProfile: false,
    isLoadingPaymentOptions: false,
    isLoadingPreview: false,
    isSubmitting: false,
    errorMessage: '',
    successMessage: ''
  }),

  getters: {
    selectedPaymentOption(state) {
      return state.paymentOptions.find((option) => option.id === state.selectedPaymentMethod) || null;
    },

    shouldShowCardForm() {
      return Boolean(this.selectedPaymentOption?.requiresCard);
    },

    isLongTermOrder(state) {
      const vehicleStore = selectVehicleStore();
      return vehicleStore.searchParams.rentalMode === 'longTerm' || state.orderPreview.rentalMode === 'longTerm';
    }
  },

  actions: {
    async initializeOrderConfirm(selectedVehicle = null) {
      this.errorMessage = '';
      await Promise.all([
        this.fetchUserProfile(),
        this.fetchPaymentOptions(),
        this.fetchOrderPreview(selectedVehicle)
      ]);
    },

    async fetchUserProfile() {
      this.isLoadingProfile = true;

      try {
        if (typeof localStorage !== 'undefined') {
          localStorage.setItem('system_context', 'customer');
        }

        const response = await authAPI.checkLogin();
        const customer = response.data?.customer;

        if (!response.data?.loggedIn || !customer) {
          throw new Error('尚未登入會員');
        }

        this.driverInfo = {
          custId: customer.custId || this.driverInfo.custId || null,
          fullName: customer.custName || '',
          phoneNumber: customer.custPhone || '',
          email: customer.custEmail || '',
          licenseNumber: customer.custLicense || '',
          orderRemark: this.driverInfo.orderRemark || ''
        };
      } catch (error) {
        console.error('Failed to fetch user profile', error);
        this.errorMessage = '無法取得會員資料，請先登入會員後再確認訂單。';
      } finally {
        this.isLoadingProfile = false;
      }
    },

    async fetchPaymentOptions() {
      this.isLoadingPaymentOptions = true;

      try {

        // 🚧 開發階段：先註解掉真實 API，等後端寫好再打開
        // const response = await axios.get('/api/payment/options');
        // const payload = Array.isArray(response.data) ? response.data : response.data?.data || [];
        // this.paymentOptions = payload.map(normalizePaymentOption).filter((option) => option.label);
        
        //以下是假資料，之後註解
        const payload = [
          { id: 'CREDIT_CARD', code: 'CREDIT_CARD', label: '信用卡', icon: 'fa-solid fa-credit-card', requiresCard: false, usesGateway: true }, // 為了 demo 方便，先不顯示輸入卡號的表單
          { id: 'MOBILE_PAY', code: 'MOBILE_PAY', label: '行動支付', icon: 'fa-solid fa-mobile-screen-button', requiresCard: false, usesGateway: true },
          { id: 'TRANSFER', code: 'TRANSFER', label: '轉帳', icon: 'fa-solid fa-building-columns', requiresCard: false, usesGateway: true },
          { id: 'CASH', code: 'CASH', label: '現金', icon: 'fa-solid fa-money-bill', requiresCard: false }
        ];
        

        this.paymentOptions = payload.map(normalizePaymentOption);
        this.selectedPaymentMethod = this.paymentOptions[0].id; // 預設選第一個


        //開發階段:這兩行先註解
        // if (!this.selectedPaymentMethod && this.paymentOptions.length > 0) {
        //   this.selectedPaymentMethod = this.paymentOptions[0].id;
        // }
      } catch (error) {
        console.error('Failed to fetch payment options', error);
        this.paymentOptions = [];
        this.errorMessage = '無法載入押金付款方式，請稍後再試。';
      } finally {
        this.isLoadingPaymentOptions = false;
      }
    },

    async fetchOrderPreview(selectedVehicle = null) {
      const vehicleStore = selectVehicleStore();
      const vehicle = selectedVehicle || vehicleStore.selectedVehicle;
      const orderDraft = vehicleStore.orderDraft || {};
      const fallbackPreview = buildLocalOrderPreview(vehicle, vehicleStore);

      this.isLoadingPreview = true;

      try {
        const response = await confirmOrderAPI.getOrderPreview({
          params: {
            vehicleId: vehicle?.id || vehicle?.vehicleId || null,
            pickupLocationId: vehicleStore.searchParams.pickupLocationId,
            returnLocationId: vehicleStore.searchParams.returnLocationId,
            // 甲租乙還與調度費由選車 Store 統一保存，確認頁只負責帶給預覽 API。
            isOneWayRental: Boolean(orderDraft.isOneWayRental ?? vehicle?.isOneWayRental),
            dispatchFee: Number(orderDraft.dispatchFee ?? vehicle?.dispatchFee ?? 0),
            rentalMode: vehicleStore.searchParams.rentalMode,
            pickupTime: vehicleStore.searchParams.pickupTime,
            returnTime: vehicleStore.searchParams.returnTime,
            pickupDate: vehicleStore.searchParams.pickupDate,
            rentalMonths: vehicleStore.searchParams.rentalMonths
          }
        });
        const payload = response.data?.data || response.data || {};
        this.orderPreview = normalizeOrderPreview(payload, vehicle, fallbackPreview);
      } catch (error) {
        console.error('Failed to fetch order preview', error);
        this.orderPreview = normalizeOrderPreview({}, vehicle, fallbackPreview);
        this.errorMessage = vehicle ? '' : '無法載入訂單明細，請確認車輛與租期資訊。';
      } finally {
        this.isLoadingPreview = false;
      }
    },

    updateDriverInfo(value) {
      this.driverInfo = {
        ...this.driverInfo,
        ...value
      };
    },

    updateCardInfo(value) {
      this.cardInfo = {
        ...this.cardInfo,
        ...value
      };
    },

    setPaymentMethod(paymentMethodId) {
      this.selectedPaymentMethod = paymentMethodId;
    },

    async submitOrder() {
      const vehicleStore = selectVehicleStore();
      const selectedVehicle = vehicleStore.selectedVehicle || this.orderPreview.vehicle;

      this.isSubmitting = true;
      this.errorMessage = '';
      this.successMessage = '';

      try {
        const existingPendingRequest = this.pendingPaymentGatewayRequest;
        if (existingPendingRequest?.orderId) {
          this.pendingPaymentGatewayRequest = existingPendingRequest;
          this.successMessage = '訂單已建立，請繼續前往綠界支付押金。';
          return { data: { orderId: existingPendingRequest.orderId, reusedPendingPayment: true } };
        }

        if (!selectedVehicle?.id && !selectedVehicle?.vehicleId) {
          this.errorMessage = '尚未選擇車輛，請返回選車頁重新選擇。';
          return null;
        }

        if (!this.selectedPaymentMethod) {
          this.errorMessage = '請選擇押金付款方式後再送出訂單。';
          return null;
        }

        const payload = buildSubmitPayload({
          driverInfo: this.driverInfo,
          selectedPaymentMethod: this.selectedPaymentMethod,
          selectedPaymentOption: this.selectedPaymentOption,
          orderPreview: this.orderPreview,
          vehicleStore,
          selectedVehicle
        });
        const paymentMethod = payload.depositPayMethod;

        console.info('Create rental order payload', payload);

        const response = await confirmOrderAPI.createRentalOrder(payload);
        this.pendingPaymentGatewayRequest = buildPaymentGatewayRequest({
          response,
          payload,
          paymentMethod,
          selectedPaymentOption: this.selectedPaymentOption
        });
        savePendingEcpayRequest(this.pendingPaymentGatewayRequest);

        this.successMessage = this.pendingPaymentGatewayRequest
          ? '訂單已建立，準備前往綠界支付押金。'
          : '訂單已建立，準備前往完成頁。';
        return response;
      } catch (error) {
        console.error('Failed to submit order', error);
        this.errorMessage = getApiErrorMessage(error, '訂單送出失敗，請檢查資料後再試一次。');
        throw error;
      } finally {
        this.isSubmitting = false;
      }
    },

    async createEcpayCheckout() {
      const request = this.pendingPaymentGatewayRequest;
      if (!request?.orderId) {
        this.errorMessage = '訂單已建立，但缺少綠界付款所需的訂單編號。';
        return null;
      }

      this.isSubmitting = true;
      this.errorMessage = '';

      try {
        const response = await confirmOrderAPI.createEcpayCheckout({
          orderId: Number(request.orderId),
          paymentMethod: normalizePaymentEnumCode(request.paymentMethod) || 'CREDIT_CARD',
          returnUrl: request.returnUrl || null,
          clientBackUrl: request.clientBackUrl
        }, {
          withCredentials: true
        });

        return response.data?.data || response.data;
      } catch (error) {
        console.error('Failed to create ECPay checkout', error?.response?.data || error);
        this.clearPendingEcpayRequest();
        this.errorMessage = getApiErrorMessage(error, '綠界付款建立失敗，請稍後再試或改用其他押金付款方式。');
        throw error;
      } finally {
        this.isSubmitting = false;
      }
    },

    clearPendingEcpayRequest() {
      this.pendingPaymentGatewayRequest = null;
      savePendingEcpayRequest(null);
    }
  }
});
