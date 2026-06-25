import { defineStore } from "pinia";
import api from "@/api";

const API_URL = '/crosslocationfee';

export const userCrossLocationFeeStore = defineStore('location', {
    state: () => ({
        fees: [],
        isLoading: false,
        errorMessage: ''
    }),

    actions: {

        // 1. 查全部
        async fetchFees() {
            this.isLoading = true;
            try {
                const response = await axios.get(API_URL);
                this.fees = response.data;
            } catch (error) {
                this.errorMessage = '載入調度費用失敗';
                console.error(error);
            } finally {
                this.isLoading = false;
            }
        },
        // 2. 新增
        async addFee(crossLocationFeeData) {
            try {
                const response = await axios.post(API_URL, crossLocationFeeData);
                this.fees.push(response.data);
                return true;
            } catch (error) {
                console.error('新增失敗', error);
                return false;
            }
        },
        // 3. 修改 
        async updateFee(feeId, crossLocationFeeData) {
            try {
                const response = await axios.put(`${API_URL}/${feeId}`, crossLocationFeeData);
                // 找到原本的並替換掉
                const index = this.fees.findIndex(loc => loc.feeId === feeId);
                if (index !== -1) {
                    this.fees[index] = response.data;
                }
                return true;
            } catch (error) {
                console.error('更新失敗', error);
                return false;
            }
        },
        // 4. 刪除
        async deleteFee(feeId) {
            try {
                await axios.delete(`${API_URL}/${feeId}`);
                // 從state中濾掉被刪除的資料
                this.fees = this.fees.filter(loc => loc.feeId !== feeId);
                return true;
            } catch (error) {
                console.error('刪除失敗', error);
                return false;
            }
        }
    }
})