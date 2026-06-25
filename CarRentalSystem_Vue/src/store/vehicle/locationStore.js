import { defineStore } from "pinia";
import api from "@/api";

const API_URL = '/location';

export const userLocationStore = defineStore('location', {
    state: () => ({
        locations: [],
        isLoading: false,
        errorMessage: ''
    }),

    actions: {

        // 1. 查全部
        async fetchLocations() {
            this.isLoading = true;
            try {
                const response = await axios.get(API_URL);
                this.locations = response.data;
            } catch (error) {
                this.errorMessage = '載入據點失敗';
                console.error(error);
            } finally {
                this.isLoading = false;
            }
        },
        // 2. 新增
        async addLocation(locationData) {
            try {
                const response = await axios.post(API_URL, locationData);
                this.locations.push(response.data);
                return true;
            } catch (error) {
                console.error('新增失敗', error);
                return false;
            }
        },
        // 3. 修改 
        async updateLocation(locationId, locationData) {
            try {
                const response = await axios.put(`${API_URL}/${locationId}`, locationData);
                // 找到原本的並替換掉
                const index = this.locations.findIndex(loc => loc.locationId === locationId);
                if (index !== -1) {
                    this.locations[index] = response.data;
                }
                return true;
            } catch (error) {
                console.error('更新失敗', error);
                return false;
            }
        },
        // 4. 刪除
        async deleteLocation(locationId) {
            try {
                await axios.delete(`${API_URL}/${locationId}`);
                // 從state中濾掉被刪除的資料
                this.locations = this.locations.filter(loc => loc.locationId !== locationId);
                return true;
            } catch (error) {
                console.error('刪除失敗', error);
                return false;
            }
        }
    }
})