import { defineStore } from "pinia";
import { vehicleAPI } from "@/api/vehicle/vehicleApi";

export const useVehicleStore = defineStore('vehicle', {
    state: () => ({
        vehicles: [],
        isLoading: false,
    }),

    actions: {

        // 查詢全部
        async fetchVehicles() {
            this.isLoading = true;
            try {
                // 使用封裝好的API
                const response = await vehicleAPI.getAll();
                // axios會自動把JSON轉好放在response.data裡面
                this.vehicles = response.data;
            } catch (error) {
                console.error("抓取車輛資料失敗：", error);
            } finally {
                this.isLoading = false;
            }
        },

        // 刪除
        async deleteVehicle(id) {
            if (confirm('確定要刪除這筆車輛資料嗎？')) {
                try {
                    await vehicleAPI.delete(id);
                    alert('刪除成功！');
                    //刪除成功後呼叫此方法更新畫面
                    await this.fetchVehicles();
                } catch (error) {
                    console.error("刪除失敗：", error);
                    alert('刪除失敗，請稍後再試');
                }
            }
        },
    }
})