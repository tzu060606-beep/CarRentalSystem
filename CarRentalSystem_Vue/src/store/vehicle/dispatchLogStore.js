import { defineStore } from "pinia";
import { dispatchAPI } from "@/api/vehicle/dispatchLogApi";

export const useDispatchStore = defineStore('dispatch', {
    state: () => ({
        dispatchLogs: [],
        isLoading: false,
    }),

    actions: {

        // 查詢全部
        async fetchDispatches() {
            this.isLoading = true;
            try {
                // 使用封裝好的API
                const response = await dispatchAPI.getAll();
                // axios會自動把JSON轉好放在response.data裡面
                // 把資料(物件清單)塞進上方定義的 useDispatchStore 的 state 的 dispatchLogs 裡
                this.dispatchLogs = response.data;
            } catch (error) {
                console.error("抓取車輛資料失敗：", error);
            } finally {
                this.isLoading = false;
            }
        },

        // 刪除
        async deleteDispatch(id) {
            if (confirm('確定要刪除這筆車輛資料嗎？')) {
                try {
                    await dispatchAPI.delete(id);
                    alert('刪除成功！');
                    //刪除成功後呼叫此方法更新畫面
                    await this.fetchDispatches();
                } catch (error) {
                    console.error("刪除失敗：", error);
                    alert('刪除失敗，請稍後再試');
                }
            }
        },
    }
})