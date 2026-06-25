import { defineStore } from "pinia";
import { carModelAPI } from "@/api/vehicle/carModelApi";

export const useCarmodelStore = defineStore('carModel', {
    state: () => ({
        carModels: [],
        isLoading: false,
    }),

    actions: {

        // 查詢全部
        async fetchCarModels() {
            this.isLoading = true;
            try {
                // 使用封裝好的API
                const response = await carModelAPI.getAll();
                // axios會自動把JSON轉好放在response.data裡面
                this.carModels = response.data;
            } catch (error) {
                console.error("抓取車款資料失敗：", error);
            } finally {
                this.isLoading = false;
            }
        },

        // 刪除
        async deleteCarModel(id) {
            if (confirm('確定要刪除這筆車款資料嗎？')) {
                try {
                    await carModelAPI.delete(id);
                    alert('刪除成功！');
                    //刪除成功後呼叫此方法更新畫面
                    await this.fetchCarModels();
                } catch (error) {
                    console.error("刪除失敗：", error);
                    alert('刪除失敗，請稍後再試');
                }
            }
        },
    }
})