<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const mapElement = ref(null);
const routeInfo = ref(null);
const userLoc = ref(null);
const dealerLoc = ref({ lat: 25.0339, lng: 121.5644 });
const mapInstance = ref(null);

// 響應式資料，同步 URL 參數與左側卡片文字
const appointmentData = ref({
    carInfo: {
        brand: route.query.carName || '未知車款',
        locationName: route.query.dealerName || '特約展示店',
        address: route.query.address || '未提供地址'
    }
});

// 1. 動態載入 SDK (確保只載入一次，且帶有 async/defer)
const loadGoogleMapsSDK = () => {
    return new Promise((resolve, reject) => {
        if (window.google && window.google.maps) {
            resolve(window.google.maps);
            return;
        }
        const script = document.createElement('script');
        const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
        // 這裡加上 loading=async 解決效能警告
        script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=geometry&loading=async`;
        script.async = true;
        script.defer = true;
        script.onload = () => resolve(window.google.maps);
        script.onerror = (err) => reject(err);
        document.head.appendChild(script);
    });
};

// 2. 地址轉座標邏輯 (增加對 Google 物件是否存在的檢查)
const updateMapByAddress = (address) => {
    if (!address) return;

    // 確保 Geocoder 已經就緒
    if (typeof google === 'undefined' || !google.maps || !google.maps.Geocoder) {
        setTimeout(() => updateMapByAddress(address), 200);
        return;
    }

    const geocoder = new google.maps.Geocoder();
    geocoder.geocode({ address: address }, (results, status) => {
        if (status === 'OK' && results[0]) {
            const newPos = results[0].geometry.location.toJSON();
            dealerLoc.value = newPos;

            if (mapInstance.value) {
                mapInstance.value.setCenter(newPos);
                mapInstance.value.setZoom(15);
                calculateRoute(mapInstance.value);
            } else {
                renderMap();
            }
        } else {
            console.error('地址轉換失敗：', status);
            if (status === 'REQUEST_DENIED') {
                console.error('請檢查 Google Console 是否已將 "Geocoding API" 加入金鑰限制清單。');
            }
        }
    });
};

// 3. 渲染地圖
const renderMap = () => {
    if (!mapElement.value || !window.google) return;

    mapInstance.value = new google.maps.Map(mapElement.value, {
        zoom: 15,
        center: dealerLoc.value,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false
    });

    calculateRoute(mapInstance.value);
};

// 4. 導航邏輯 (串接使用者位置與車行位置)
const calculateRoute = (mapObj) => {
    const directionsService = new google.maps.DirectionsService();
    const directionsRenderer = new google.maps.DirectionsRenderer({
        map: mapObj,
        suppressMarkers: false
    });

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition((pos) => {
            userLoc.value = { lat: pos.coords.latitude, lng: pos.coords.longitude };
            directionsService.route({
                origin: userLoc.value,
                destination: dealerLoc.value,
                travelMode: google.maps.TravelMode.DRIVING,
            }, (response, status) => {
                if (status === 'OK') {
                    directionsRenderer.setDirections(response);
                    const leg = response.routes[0].legs[0];
                    routeInfo.value = { distance: leg.distance.text, duration: leg.duration.text };
                }
            });
        }, (err) => {
            console.warn("無法取得定位", err);
        });
    }
};

// 5. 監聽 URL 變化 (當 User 從詳情頁跳轉過來時，立即更新地址)
watch(
    () => route.query.address,
    (newAddress) => {
        if (newAddress) {
            appointmentData.value.carInfo.address = newAddress;
            appointmentData.value.carInfo.brand = route.query.carName || '未知車款';
            updateMapByAddress(newAddress);
        }
    }
);

onMounted(async () => {
    try {
        await loadGoogleMapsSDK();
        const targetAddress = route.query.address;
        if (targetAddress) {
            updateMapByAddress(targetAddress);
        } else {
            renderMap();
        }
    } catch (err) {
        console.error("SDK 加載出錯", err);
    }
});

const openExternalMap = () => {
    const address = appointmentData.value.carInfo.address;
    if (address) {
        window.open(`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(address)}`, '_blank');
    }
};
</script>

<template>
    <div class="container py-4">
        <div class="row mb-4">
            <div class="col">
                <h1 class="display-6 fw-bold">位置導航</h1>
            </div>
        </div>

        <div class="row g-4">
            <div class="col-md-4">
                <div class="card shadow-sm border-0">
                    <div class="card-body">
                        <h5 class="card-title text-primary fw-bold mb-3">車輛資訊</h5>
                        <hr />
                        <p class="mb-2"><strong>車款：</strong> {{ appointmentData.carInfo.brand }}</p>
                        <p class="mb-2"><strong>車行：</strong> {{ appointmentData.carInfo.locationName }}</p>
                        <p class="mb-3"><strong>地址：</strong> {{ appointmentData.carInfo.address }}</p>
                        <div class="alert alert-secondary py-2 small">
                            請提前預約以確保專人服務
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-md-8">
                <div class="card shadow-sm border-0">
                    <div class="card-body p-0 position-relative">
                        <div ref="mapElement" style="height: 450px; width: 100%;" class="rounded">
                            <div class="d-flex justify-content-center align-items-center h-100 bg-light">
                                <span class="text-muted">地圖載入中...</span>
                            </div>
                        </div>

                        <div v-if="routeInfo" class="position-absolute bottom-0 start-0 w-100 p-3">
                            <div class="bg-white shadow rounded p-3 d-flex justify-content-between align-items-center">
                                <div>
                                    <p class="mb-0 small text-muted">目前距離</p>
                                    <p class="mb-0 fw-bold">{{ routeInfo.distance }} (約 {{ routeInfo.duration }})</p>
                                </div>
                                <button @click="openExternalMap" class="btn btn-primary">
                                    開啟導航
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>