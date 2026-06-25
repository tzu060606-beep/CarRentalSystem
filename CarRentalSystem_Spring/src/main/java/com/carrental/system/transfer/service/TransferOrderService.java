package com.carrental.system.transfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.carrental.system.transfer.entity.TransferOrder;
import com.carrental.system.transfer.entity.TransferRate;
import com.carrental.system.transfer.repository.TransferOrderRepository;
import com.carrental.system.transfer.repository.TransferRateRepository;
import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.repository.CustomerRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.carrental.system.vehicle.service.VehicleService;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.entity.VehicleStatus;
import com.carrental.system.point.service.PointsEventService;
import com.carrental.system.point.enums.ChangeType;

/**
 * 接送訂單服務層 (Service)
 * 負責處理接送訂單的核心商業邏輯，並與 Repository (資料庫層) 溝通
 */
@Service
public class TransferOrderService {

    @Autowired
    private TransferOrderRepository transferOrderRepository;

    @Autowired
    private TransferRateRepository transferRateRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    @Lazy
    private PointsEventService pointsEventService;

    /**
     * 輔助方法：將會員名稱 (custName) 查詢出來並塞入 TransferOrder 實體中
     * 這樣前端呼叫 API 時就可以直接拿到會員的姓名
     * 
     * @param order 準備要回傳的接送訂單
     */
    private void populateCustomerName(TransferOrder order) {
        if (order != null && order.getCustId() != null) {
            Optional<CustomerBean> customerOpt = customerRepository.findById(order.getCustId());
            customerOpt.ifPresent(customer -> order.setCustName(customer.getCustName()));
        }
    }

    /**
     * 取得系統內所有的接送訂單
     * 
     * @return 訂單列表 (包含會員名稱)
     */
    public List<TransferOrder> findAll() {
        List<TransferOrder> orders = transferOrderRepository.findAll();
        orders.forEach(this::populateCustomerName);
        return orders;
    }

    /**
     * 查詢特定會員的所有接送訂單
     * 
     * @param custId 會員編號
     * @return 該會員的訂單列表 (包含會員名稱)
     */
    public List<TransferOrder> findByCustId(Integer custId) {
        List<TransferOrder> orders = transferOrderRepository.findByCustId(custId);
        orders.forEach(this::populateCustomerName);
        return orders;
    }

    /**
     * 透過訂單編號查詢單筆接送訂單
     * 
     * @param id 訂單編號
     * @return 單筆訂單資料 (包含會員名稱)
     */
    public TransferOrder findById(Integer id) {
        Optional<TransferOrder> optional = transferOrderRepository.findById(id);
        TransferOrder order = optional.orElse(null);
        populateCustomerName(order);
        return order;
    }

    /**
     * 儲存或更新一筆接送訂單
     * 在儲存之前，會先根據費率、起訖里程數來自動計算並設定總費用 (Total Amount)
     * 
     * @param transferOrder 前端傳來的訂單資料
     * @return 儲存進資料庫後的訂單資料
     */
    public TransferOrder save(TransferOrder transferOrder) {
        // 自動代入起始里程：若訂單有指派車輛但尚未填寫起始里程，則從車輛目前里程帶入
        if (transferOrder.getStartMileage() == null && transferOrder.getVehicleId() != null) {
            Vehicle vehicle = vehicleService.getVehicleById(transferOrder.getVehicleId());
            if (vehicle != null && vehicle.getMileage() != null) {
                transferOrder.setStartMileage(vehicle.getMileage());
            }
        }

        // 自動計算費用邏輯
        if (transferOrder.getRateId() != null
                && transferOrder.getStartMileage() != null
                && transferOrder.getEndMileage() != null) {

            if (transferOrder.getEndMileage() >= transferOrder.getStartMileage()) {
                // 1. 根據費率 ID 去資料庫查詢當前費率 (TransferRate)
                Optional<TransferRate> rateOpt = transferRateRepository.findById(transferOrder.getRateId());
                if (rateOpt.isPresent()) {
                    TransferRate rate = rateOpt.get();
                    // 2. 總費用 = 基本費用 + [ (結束里程 - 起始里程) * 每公里費用 ]
                    BigDecimal distance = new BigDecimal(
                            transferOrder.getEndMileage() - transferOrder.getStartMileage());
                    BigDecimal variableFee = rate.getPerKmFee().multiply(distance);
                    BigDecimal totalAmount = rate.getBaseFee().add(variableFee);

                    // 3. 把計算好的總費用設定回訂單物件中
                    transferOrder.setTotalAmount(totalAmount);
                }
            }
        }
        // 4. 最後將資料正式寫入資料庫
        return transferOrderRepository.save(transferOrder);
    }

    /**
     * 刪除特定訂單
     * 
     * @param id 訂單編號
     */
    public void deleteById(Integer id) {
        transferOrderRepository.deleteById(id);
    }

    // ==================== 狀態流程 ====================

    /**
     * 開始接送
     */
    public void startTransfer(Integer id) {
        Optional<TransferOrder> orderOpt = transferOrderRepository.findById(id);
        if (orderOpt.isPresent()) {
            TransferOrder order = orderOpt.get();
            order.setStatus("處理中");

            // 車輛狀態連動：改為專車接送中 (SHUTTLING)
            // 同時將車輛目前里程自動帶入訂單的「起始里程」，確保結算時有正確基準
            if (order.getVehicleId() != null) {
                Vehicle vehicle = vehicleService.getVehicleById(order.getVehicleId());
                if (vehicle != null && vehicle.getMileage() != null) {
                    order.setStartMileage(vehicle.getMileage());
                }
                vehicleService.updateVehicleStatus(order.getVehicleId(), VehicleStatus.SHUTTLING);
            }

            transferOrderRepository.save(order);
        }
    }

    /**
     * 完成接送
     */
    public void finishTransfer(Integer id, Integer endMileage) {
        Optional<TransferOrder> orderOpt = transferOrderRepository.findById(id);
        if (orderOpt.isPresent()) {
            TransferOrder order = orderOpt.get();

            // 更新訂單狀態與結算資料
            order.setStatus("已完成");

            // 連動設定實際下車時間為當下時間
            order.setRealDropoffTime(java.time.LocalDateTime.now());

            // 更新結束里程
            order.setEndMileage(endMileage);

            // 若起始里程尚未設定（舊資料補救），嘗試從車輛帶入
            if (order.getStartMileage() == null && order.getVehicleId() != null) {
                Vehicle vehicle = vehicleService.getVehicleById(order.getVehicleId());
                if (vehicle != null && vehicle.getMileage() != null) {
                    order.setStartMileage(vehicle.getMileage());
                }
            }

            // 結算總費用：totalAmount = baseFee + (endMileage - startMileage) × perKmFee
            if (order.getRateId() != null && order.getStartMileage() != null) {
                Optional<TransferRate> rateOpt = transferRateRepository.findById(order.getRateId());
                if (rateOpt.isPresent()) {
                    TransferRate rate = rateOpt.get();
                    int dist = endMileage - order.getStartMileage();
                    if (dist < 0) dist = 0; // 防呆：結束里程不應小於起始里程
                    BigDecimal distance = new BigDecimal(dist);
                    BigDecimal variableFee = rate.getPerKmFee().multiply(distance);
                    order.setTotalAmount(rate.getBaseFee().add(variableFee));
                } else {
                    // 找不到費率，設為 0 避免 null
                    order.setTotalAmount(BigDecimal.ZERO);
                }
            } else {
                // 缺少費率或起始里程，無法計算，設為 0
                order.setTotalAmount(BigDecimal.ZERO);
            }

            // =========================// 車輛模組連動 //=========================
            if (order.getVehicleId() != null) {

                // 更新車輛總里程
                vehicleService.updateVehicleMileage(
                        order.getVehicleId(),
                        endMileage);

                // 更新車輛狀態為整理中
                vehicleService.updateVehicleStatus(
                        order.getVehicleId(),
                        VehicleStatus.CLEANING);
            }

            // TODO: 發放點數 (等待 PointsService 開放此 API 後解開註解)

            pointsEventService.addPoints(order.getCustId(), id, ChangeType.TRANSFER);

            transferOrderRepository.save(order);
        }
    }

    /**
     * 取消接送
     */
    public void cancelTransfer(Integer id) {
        Optional<TransferOrder> orderOpt = transferOrderRepository.findById(id);
        if (orderOpt.isPresent()) {
            TransferOrder order = orderOpt.get();

            boolean wasInProgress = "處理中".equals(order.getStatus()) || "接送中".equals(order.getStatus());
            order.setStatus("已取消");

            // 車輛狀態連動：如果車子已經出發 (處理中)，取消時改為清潔中
            // 如果還沒出發，車子就維持原狀態(通常是 AVAILABLE)
            if (order.getVehicleId() != null && wasInProgress) {
                vehicleService.updateVehicleStatus(order.getVehicleId(), VehicleStatus.CLEANING);
            }

            transferOrderRepository.save(order);
        }
    }
}
