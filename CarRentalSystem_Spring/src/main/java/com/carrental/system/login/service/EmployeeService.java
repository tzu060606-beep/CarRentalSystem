package com.carrental.system.login.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.login.dto.EmployeeRequestDTO;
import com.carrental.system.login.dto.EmployeeResponseDTO;
import com.carrental.system.login.entity.EmployeeBean;
import com.carrental.system.login.repository.EmployeeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // ========== DTO 轉換邏輯 ==========
    public EmployeeResponseDTO toDto(EmployeeBean entity) {
        if (entity == null) return null;
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setEmpId(entity.getEmpId());
        dto.setEmpName(entity.getEmpName());
        dto.setEmpAccount(entity.getEmpAccount());
        dto.setEmpRole(entity.getEmpRole());
        dto.setEmpPhone(entity.getEmpPhone());
        dto.setEmpEmail(entity.getEmpEmail());
        dto.setEmpStatus(entity.getEmpStatus());
        dto.setResignDate(entity.getResignDate());
        dto.setEmpPhoto(entity.getEmpPhoto());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // ========== 註冊/新增員工 ==========
    public boolean createEmployee(EmployeeRequestDTO requestDto) {
        if (employeeRepository.existsByEmpAccount(requestDto.getEmpAccount())) {
            return false; // 帳號重複
        }
        
        EmployeeBean employee = new EmployeeBean();
        employee.setEmpName(requestDto.getEmpName());
        employee.setEmpAccount(requestDto.getEmpAccount());
        employee.setEmpPassword(requestDto.getEmpPassword());
        employee.setEmpRole(requestDto.getEmpRole());
        employee.setEmpPhone(requestDto.getEmpPhone());
        employee.setEmpEmail(requestDto.getEmpEmail());
        employee.setEmpPhoto(requestDto.getEmpPhoto());

        // 預設狀態為「在職」
        if (requestDto.getEmpStatus() == null || requestDto.getEmpStatus().trim().isEmpty()) {
            employee.setEmpStatus("在職");
        } else {
            employee.setEmpStatus(requestDto.getEmpStatus());
        }
        
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        employeeRepository.save(employee);
        return true;
    }

    // ========== 查詢全部員工 ==========
    public List<EmployeeResponseDTO> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ========== 根據 ID 查詢單一員工 ==========
    public Optional<EmployeeResponseDTO> getEmployeeById(Integer empId) {
        return employeeRepository.findById(empId).map(this::toDto);
    }

    // ========== 模糊查詢 ==========
    public List<EmployeeResponseDTO> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllEmployees();
        }
        return employeeRepository.searchByKeyword(keyword)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ========== 修改員工 ==========
    public EmployeeResponseDTO updateEmployee(Integer empId, EmployeeRequestDTO updatedData) {
        return employeeRepository.findById(empId)
                .map(existing -> {
                    existing.setEmpName(updatedData.getEmpName());
                    existing.setEmpPhone(updatedData.getEmpPhone());
                    existing.setEmpEmail(updatedData.getEmpEmail());
                    existing.setEmpRole(updatedData.getEmpRole());
                    
                    if (updatedData.getEmpPassword() != null && !updatedData.getEmpPassword().trim().isEmpty()) {
                        existing.setEmpPassword(updatedData.getEmpPassword());
                    }
                    if (updatedData.getEmpStatus() != null) {
                        existing.setEmpStatus(updatedData.getEmpStatus());
                        // 如果變更狀態為「離職」且未設置離職日期，則自動填入今日
                        if ("離職".equals(updatedData.getEmpStatus())) {
                            existing.setResignDate(
                                updatedData.getResignDate() != null
                                    ? updatedData.getResignDate()
                                    : java.time.LocalDate.now()
                            );
                        } else {
                            // 變更為在職時清除離職日期
                            existing.setResignDate(null);
                        }
                    }
                    if (updatedData.getEmpPhoto() != null) {
                        existing.setEmpPhoto(updatedData.getEmpPhoto());
                    }
                    existing.setUpdatedAt(LocalDateTime.now());
                    return toDto(employeeRepository.save(existing));
                })
                .orElseThrow(() -> new RuntimeException("找不到員工 ID: " + empId));
    }

    // ========== 軟刪除員工（標記為「離職」並自動記錄離職日期）==========
    public void deleteEmployee(Integer empId) {
        employeeRepository.findById(empId).ifPresent(existing -> {
            existing.setEmpStatus("離職");
            existing.setResignDate(java.time.LocalDate.now());
            existing.setUpdatedAt(LocalDateTime.now());
            employeeRepository.save(existing);
        });
    }
}
