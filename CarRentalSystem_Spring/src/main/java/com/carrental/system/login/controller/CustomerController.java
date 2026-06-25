package com.carrental.system.login.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.carrental.system.login.dto.CustomerRequestDTO;
import com.carrental.system.login.dto.CustomerResponseDTO;
import com.carrental.system.login.service.CustomerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponseDTO>> getAllCustomers() {
        List<CustomerResponseDTO> list = customerService.getAllCustomers();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{custId}")
    public ResponseEntity<CustomerResponseDTO> getCustomerById(@PathVariable Integer custId) {
        Optional<CustomerResponseDTO> customer = customerService.getCustomerById(custId);
        return customer.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponseDTO>> searchCustomers(@RequestParam(defaultValue = "") String keyword) {
        List<CustomerResponseDTO> list = customerService.searchByKeyword(keyword);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCustomer(@RequestBody CustomerRequestDTO requestDto) {
        Map<String, Object> result = new HashMap<>();

        if (requestDto.getCustPassword() == null || requestDto.getCustPassword().isEmpty()) {
            requestDto.setCustPassword(requestDto.getCustPhone());
        }

        boolean success = customerService.registerCustomer(requestDto);

        if (success) {
            result.put("success", true);
            result.put("message", "新增客戶成功！");
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", "此帳號已存在，請更換帳號。");
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/{custId}")
    public ResponseEntity<Map<String, Object>> updateCustomer(
            @PathVariable Integer custId,
            @RequestBody CustomerRequestDTO requestDto) {
        Map<String, Object> result = new HashMap<>();

        try {
            CustomerResponseDTO updatedCustomer = customerService.updateCustomer(custId, requestDto);
            result.put("success", true);
            result.put("message", "修改成功！");
            result.put("data", updatedCustomer);  // ← 回傳最新的會員資料（含新頭像路徑）
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @DeleteMapping("/{custId}")
    public ResponseEntity<Map<String, Object>> deleteCustomer(@PathVariable Integer custId) {
        Map<String, Object> result = new HashMap<>();

        try {
            customerService.deleteCustomer(custId);
            result.put("success", true);
            result.put("message", "已將客戶停權");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "刪除失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
