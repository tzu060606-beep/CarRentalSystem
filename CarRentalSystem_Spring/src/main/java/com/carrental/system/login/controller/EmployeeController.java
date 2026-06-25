package com.carrental.system.login.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.carrental.system.login.dto.EmployeeRequestDTO;
import com.carrental.system.login.dto.EmployeeResponseDTO;
import com.carrental.system.login.service.EmployeeService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ========== 查詢全部員工 ==========
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeResponseDTO>> getAllEmployees() {
        List<EmployeeResponseDTO> list = employeeService.getAllEmployees();
        return ResponseEntity.ok(list);
    }

    // ========== 根據 ID 查詢單一員工 ==========
    @GetMapping("/{empId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponseDTO> getEmployeeById(@PathVariable Integer empId) {
        Optional<EmployeeResponseDTO> employee = employeeService.getEmployeeById(empId);
        return employee.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    // ========== 模糊查詢 ==========
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeResponseDTO>> searchEmployees(@RequestParam(defaultValue = "") String keyword) {
        List<EmployeeResponseDTO> list = employeeService.searchByKeyword(keyword);
        return ResponseEntity.ok(list);
    }

    // ========== 新增員工 ==========
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createEmployee(@RequestBody EmployeeRequestDTO requestDto) {
        Map<String, Object> result = new HashMap<>();

        // 預設密碼邏輯：如果沒填，預設為手機號碼 (可依業務需求調整)
        if (requestDto.getEmpPassword() == null || requestDto.getEmpPassword().isEmpty()) {
            requestDto.setEmpPassword(requestDto.getEmpPhone());
        }

        boolean success = employeeService.createEmployee(requestDto);

        if (success) {
            result.put("success", true);
            result.put("message", "新增員工成功！");
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", "此帳號已存在，請更換帳號。");
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ========== 修改員工 ==========
    @PutMapping("/{empId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateEmployee(
            @PathVariable Integer empId,
            @RequestBody EmployeeRequestDTO requestDto) {
        Map<String, Object> result = new HashMap<>();

        try {
            employeeService.updateEmployee(empId, requestDto);
            result.put("success", true);
            result.put("message", "修改員工資料成功！");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ========== 刪除員工 (實作軟刪除) ==========
    @DeleteMapping("/{empId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteEmployee(@PathVariable Integer empId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 底層 Service 會把 empStatus 改為 0 (離職/停用)
            employeeService.deleteEmployee(empId);
            result.put("success", true);
            result.put("message", "員工已成功停用/離職 (軟刪除)！");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "刪除失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
