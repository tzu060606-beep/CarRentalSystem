package com.carrental.system.transfer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.carrental.system.transfer.entity.TransferOrder;
import com.carrental.system.transfer.service.TransferOrderService;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.service.VehicleService;

import java.util.List;

/**
 * 接送訂單控制器 (Controller)
 * 負責處理前端發送過來與「接送訂單」相關的 HTTP 請求 (API)
 */
@RestController
@RequestMapping("/api/transferOrder") // 定義此 Controller 的基礎 URL 路徑
@CrossOrigin // 允許跨網域請求 (讓 Vue 前端可以順利呼叫)
public class TransferOrderController {

    @Autowired
    private TransferOrderService transferOrderService;

    @Autowired
    private VehicleService vehicleService;

    /**
     * 取得所有接送訂單列表
     * HTTP Method: GET /api/transferOrder
     */
    @GetMapping
    public List<TransferOrder> getAll() {
        return transferOrderService.findAll();
    }

    /**
     * 根據會員編號 (custId) 取得該會員的所有接送訂單
     * HTTP Method: GET /api/transferOrder/member/{custId}
     */
    @GetMapping("/member/{custId}")
    public List<TransferOrder> getByCustId(@PathVariable Integer custId) {
        return transferOrderService.findByCustId(custId);
    }

    /**
     * 根據訂單編號 (id) 取得單筆接送訂單的詳細資料
     * HTTP Method: GET /api/transferOrder/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransferOrder> getById(@PathVariable Integer id) {
        TransferOrder order = transferOrderService.findById(id);
        if (order != null) {
            return ResponseEntity.ok(order); // 若找到，回傳 HTTP 200 (OK) 與訂單資料
        }
        return ResponseEntity.notFound().build(); // 若找不到，回傳 HTTP 404 (Not Found)
    }

    /**
     * 建立一筆新的接送訂單
     * HTTP Method: POST /api/transferOrder
     */
    @PostMapping
    public TransferOrder create(@RequestBody TransferOrder transferOrder) {
        // 如果沒有指定車輛，自動尋找可用車輛
        if (transferOrder.getVehicleId() == null && transferOrder.getScheduledPickupTime() != null) {
            java.time.LocalDateTime startTime = transferOrder.getScheduledPickupTime();
            java.time.LocalDateTime endTime = transferOrder.getScheduledDropoffTime();
            if (endTime == null) {
                // 預設接送服務佔用 4 小時
                endTime = startTime.plusHours(4);
            }
            List<Vehicle> availableVehicles = vehicleService.getAvailableVehiclesForTransfer(startTime, endTime);
            if (availableVehicles != null && !availableVehicles.isEmpty()) {
                Vehicle assignedVehicle = availableVehicles.get(0);
                transferOrder.setVehicleId(assignedVehicle.getVehicleId());
                transferOrder.setVehicle(assignedVehicle);
            }
        } else if (transferOrder.getVehicleId() != null) {
            // 前端傳 vehicleId，需手動轉換為 Vehicle 實體（因 vehicleId 欄位為 insertable=false）
            Vehicle vehicle = vehicleService.getVehicleById(transferOrder.getVehicleId());
            transferOrder.setVehicle(vehicle);
        }
        return transferOrderService.save(transferOrder);
    }

    /**
     * 更新現有的接送訂單資料
     * HTTP Method: PUT /api/transferOrder/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransferOrder> update(@PathVariable Integer id, @RequestBody TransferOrder transferOrder) {
        TransferOrder existing = transferOrderService.findById(id);
        if (existing != null) {
            transferOrder.setTransferId(id); // 確保 ID 一致
            transferOrder.setCreatedAt(existing.getCreatedAt()); // 保留原本的建立時間

            // 前端傳 vehicleId，需手動轉換為 Vehicle 實體（因 vehicleId 欄位為 updatable=false）
            if (transferOrder.getVehicleId() != null) {
                Vehicle vehicle = vehicleService.getVehicleById(transferOrder.getVehicleId());
                transferOrder.setVehicle(vehicle);
            } else {
                transferOrder.setVehicle(null); // 清空車輛指派
            }

            return ResponseEntity.ok(transferOrderService.save(transferOrder));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 刪除指定的接送訂單
     * HTTP Method: DELETE /api/transferOrder/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        TransferOrder existing = transferOrderService.findById(id);
        if (existing != null) {
            transferOrderService.deleteById(id);
            return ResponseEntity.ok().build(); // 刪除成功回傳 HTTP 200 (OK)
        }
        return ResponseEntity.notFound().build();
    }

    // ==================== 狀態流程 ====================

    // 開始接送
    @PostMapping("/{id}/start")
    public ResponseEntity<String> startTransfer(@PathVariable Integer id) {
        transferOrderService.startTransfer(id);
        return ResponseEntity.ok("專車接送已開始");
    }

    // 完成接送
    @PostMapping("/{id}/finish")
    public ResponseEntity<String> finishTransfer(
            @PathVariable Integer id,
            @RequestParam Integer endMileage) {

        transferOrderService.finishTransfer(id, endMileage);

        return ResponseEntity.ok("專車接送已完成");
    }

    // 取消接送
    @PostMapping("/{id}/cancel")
    public ResponseEntity<String> cancelTransfer(@PathVariable Integer id) {
        transferOrderService.cancelTransfer(id);
        return ResponseEntity.ok("專車訂單已取消");
    }
}
