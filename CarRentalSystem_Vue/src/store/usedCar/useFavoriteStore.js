// src/stores/useFavoriteStore.js
import { defineStore } from "pinia";

export const useFavoriteStore = defineStore("favorite", {
  state: () => ({
    // 這裡存放所有被收藏的車輛物件
    favorites: [],
  }),
  getters: {
    // 建立一個檢查器：傳入 carId，回傳 true/false
    isFavorite: (state) => (carId) => {
      return state.favorites.some((f) => f.usedCarId === carId);
    },
  },
  actions: {
    // 切換收藏狀態：如果已存在就移除，不存在就加入
    toggleFavorite(car) {
      const index = this.favorites.findIndex(
        (f) => f.usedCarId === car.usedCarId,
      );
      if (index === -1) {
        this.favorites.push(car);
        console.log(`已收藏：${car.brand} ${car.modelName}`);
      } else {
        this.favorites.splice(index, 1);
        console.log(`已移除收藏`);
      }
    },
  },
  // 關鍵：開啟持久化，這樣登入期間重新整理頁面，收藏也不會消失
  persist: true,
});
