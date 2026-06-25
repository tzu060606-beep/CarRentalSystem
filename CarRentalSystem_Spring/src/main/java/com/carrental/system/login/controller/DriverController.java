package com.carrental.system.login.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.carrental.system.login.dto.DriverRequestDTO;
import com.carrental.system.login.dto.DriverResponseDTO;
import com.carrental.system.login.dto.LoggedInUserDTO;
import com.carrental.system.login.service.AuthService;
import com.carrental.system.login.service.DriverService;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final AuthService authService;

    
    // 取得我的服務客戶清單
//    public void getCustomerList() {
//    	LoggedInUserDTO loggedInUser = authService.getLoggedInUser();//知道我是誰
//    	
//    	Integer id = loggedInUser.getId();
//    	service.getList(id)
//    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DriverResponseDTO>> getAllDrivers() {
        List<DriverResponseDTO> list = driverService.getAllDrivers();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DriverResponseDTO> getDriverById(@PathVariable Integer driverId) {
        Optional<DriverResponseDTO> driver = driverService.getDriverById(driverId);
        return driver.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{empId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DriverResponseDTO> getDriverByEmpId(@PathVariable Integer empId) {
        Optional<DriverResponseDTO> driver = driverService.getDriverByEmpId(empId);
        return driver.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createDriver(@RequestBody DriverRequestDTO requestDto) {
        Map<String, Object> result = new HashMap<>();

        boolean success = driverService.createDriver(requestDto);

        if (success) {
            result.put("success", true);
            result.put("message", "新增司機成功！");
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", "新增失敗：員工可能已是司機或駕照號碼重複。");
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateDriver(
            @PathVariable Integer driverId,
            @RequestBody DriverRequestDTO requestDto) {
        Map<String, Object> result = new HashMap<>();

        try {
            driverService.updateDriver(driverId, requestDto);
            result.put("success", true);
            result.put("message", "修改司機資料成功！");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
