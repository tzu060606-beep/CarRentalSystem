package com.carrental.system.transfer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.carrental.system.transfer.entity.TransferRate;
import com.carrental.system.transfer.service.TransferRateService;

import java.util.List;

/**
 * 接送服務費率控制器 (Controller)
 * 負責處理前端發送過來與「接送費率設定」相關的 HTTP 請求 (API)
 */
@RestController
@RequestMapping("/api/transferRate")
@CrossOrigin
public class TransferRateController {

    @Autowired
    private TransferRateService transferRateService;

    /**
     * 取得所有接送費率設定列表
     * HTTP Method: GET /api/transferRate
     */
    @GetMapping
    public List<TransferRate> getAll() {
        return transferRateService.findAll();
    }

    /**
     * 根據費率編號 (id) 取得單筆費率設定的詳細資料
     * HTTP Method: GET /api/transferRate/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransferRate> getById(@PathVariable Integer id) {
        TransferRate rate = transferRateService.findById(id);
        if (rate != null) {
            return ResponseEntity.ok(rate); // 找到則回傳 200 OK
        }
        return ResponseEntity.notFound().build(); // 找不到回傳 404
    }

    /**
     * 建立一筆新的接送費率設定
     * HTTP Method: POST /api/transferRate
     */
    @PostMapping
    public TransferRate create(@RequestBody TransferRate transferRate) {
        return transferRateService.save(transferRate);
    }

    /**
     * 更新現有的接送費率設定
     * HTTP Method: PUT /api/transferRate/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransferRate> update(@PathVariable Integer id, @RequestBody TransferRate transferRate) {
        TransferRate existing = transferRateService.findById(id);
        if (existing != null) {
            transferRate.setRateId(id); // 確保是更新指定的 ID
            transferRate.setCreatedAt(existing.getCreatedAt()); // 保留原本建立時間
            return ResponseEntity.ok(transferRateService.save(transferRate));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 刪除指定的接送費率設定
     * HTTP Method: DELETE /api/transferRate/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        TransferRate existing = transferRateService.findById(id);
        if (existing != null) {
            transferRateService.deleteById(id);
            return ResponseEntity.ok().build(); // 刪除成功
        }
        return ResponseEntity.notFound().build();
    }
}
