package com.carrental.system.rentalorder.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.rentalorder.dto.AdminPlanReqDto;
import com.carrental.system.rentalorder.entity.RentalPlansBean;
import com.carrental.system.rentalorder.service.RentalPlansService;

@RestController
@RequestMapping("/api/admin/rentalplans") // 統一的前綴網址，標明是後台管理員專用
public class RentalPlansController {


    @Autowired
    private RentalPlansService planService;

    // ==========================================
    // 1. 查詢全部 (GET /api/admin/rentalplans)
    // ==========================================
    @GetMapping("/all")
    public ResponseEntity<List<RentalPlansBean>> getAllPlans() {
        List<RentalPlansBean> plans = planService.queryAll();
        return ResponseEntity.ok(plans); // 回傳 HTTP 200 OK
    }

    // ==========================================
    // 2. 查詢單筆 (GET /api/admin/rentalplans/{planId})
    // ==========================================
    @GetMapping("/{planId}")
    public ResponseEntity<?> getPlanById(@PathVariable Integer planId) {
        try {
            RentalPlansBean plan = planService.query(planId);
            return ResponseEntity.ok(plan); // 成功找到，回傳 200 OK
        } catch (RuntimeException e) {

            Map<String, String> error = new HashMap<>();
            error.put("error", "查無此方案，請確認方案編號：" + planId);
            // Service 找不到資料拋出的錯誤，回傳 404 Not Found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // ==========================================
    // 3. 新增方案 (POST /api/admin/rentalplans)
    // ==========================================
    @PostMapping("/newplan")
    public ResponseEntity<?> createPlan(@RequestBody AdminPlanReqDto planDto) {
        try {
            RentalPlansBean newPlan = planService.insert(planDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(newPlan); // 成功新增，回傳 201 Created
        } catch (IllegalArgumentException e) {
            // 攔截 Service 寫的「參數防呆錯誤」，回傳 400 Bad Request
            Map<String,String> error = new HashMap<>();
            error.put("error",  "錯誤："+e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            // 其他未知的系統錯誤，回傳 500
            Map<String,String> error = new HashMap<>();
            error.put("error",  "錯誤："+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("系統發生錯誤：" +error);
        }
    }

    // ==========================================
    // 4. 更新方案 (PUT /api/admin/rentalplans/{planId})
    // ==========================================
    @PutMapping("/{planId}")
    public ResponseEntity<?> updatePlan(@PathVariable Integer planId, @RequestBody AdminPlanReqDto planDto) {
        try {
            RentalPlansBean updatedPlan = planService.update(planId, planDto);
            return ResponseEntity.ok(updatedPlan); // 成功更新，回傳 200 OK
        } catch (IllegalArgumentException e) {
            // 參數防呆錯誤，回傳 400 Bad Request
            Map<String,String> error = new HashMap<>();
            error.put("error",  "錯誤："+e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            // 找不到資料的錯誤，回傳 404 Not Found
            Map<String,String> error = new HashMap<>();
            error.put("error",  "錯誤："+e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // ==========================================
    // 5. 刪除方案 (DELETE /api/admin/rentalplans/{planId})
    // ==========================================
    @DeleteMapping("/{planId}")
    public ResponseEntity<?> deletePlan(@PathVariable Integer planId) {
        try {
            planService.delete(planId);
            return ResponseEntity.ok("方案已成功刪除"); // 成功刪除，回傳 200 OK 與成功訊息
        } catch (RuntimeException e) {
            // 找不到資料的錯誤，回傳 404 Not Found
            Map<String,String> error = new HashMap<>();
            error.put("error",  "錯誤："+e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
}
