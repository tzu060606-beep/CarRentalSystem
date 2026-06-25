import api from '@/api/index';

const VEHICLE_BASE_URL = '/vehicle';

export const orderVehicleAPI = {
  getAvailable(reqStartTime, reqEndTime) {
    return api.get(`${VEHICLE_BASE_URL}/available`, {
      params: {
        reqStartTime,
        reqEndTime
      }
    });
  }
};
