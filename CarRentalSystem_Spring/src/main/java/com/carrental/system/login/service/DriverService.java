package com.carrental.system.login.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.login.dto.DriverRequestDTO;
import com.carrental.system.login.dto.DriverResponseDTO;
import com.carrental.system.login.entity.DriverBean;
import com.carrental.system.login.repository.DriverRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    // ========== DTO 轉換邏輯 ==========
    public DriverResponseDTO toDto(DriverBean entity) {
        if (entity == null) return null;
        DriverResponseDTO dto = new DriverResponseDTO();
        dto.setDriverId(entity.getDriverId());
        dto.setEmpId(entity.getEmpId());
        dto.setLicenseNo(entity.getLicenseNo());
        dto.setLicenseExpiry(entity.getLicenseExpiry());
        dto.setPetAvailable(entity.getPetAvailable());
        dto.setVehicle(entity.getVehicle());
        return dto;
    }

    // ========== 新增司機 ==========
    public boolean createDriver(DriverRequestDTO requestDto) {
        if (requestDto.getEmpId() != null && driverRepository.existsByEmpId(requestDto.getEmpId())) {
            return false;
        }
        if (requestDto.getLicenseNo() != null && driverRepository.existsByLicenseNo(requestDto.getLicenseNo())) {
            return false;
        }
        DriverBean driver = new DriverBean();
        driver.setEmpId(requestDto.getEmpId());
        driver.setLicenseNo(requestDto.getLicenseNo());
        driver.setLicenseExpiry(requestDto.getLicenseExpiry());
        driver.setPetAvailable(requestDto.getPetAvailable());
        driver.setVehicle(requestDto.getVehicle());
        driverRepository.save(driver);
        return true;
    }

    // ========== 查詢全部司機 ==========
    public List<DriverResponseDTO> getAllDrivers() {
        return driverRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    // ========== 根據 ID 查詢單一司機 ==========
    public Optional<DriverResponseDTO> getDriverById(Integer driverId) {
        return driverRepository.findById(driverId).map(this::toDto);
    }

    // ========== 根據員工 ID 查詢司機 ==========
    public Optional<DriverResponseDTO> getDriverByEmpId(Integer empId) {
        return driverRepository.findByEmpId(empId).map(this::toDto);
    }

    // ========== 修改司機 ==========
    public DriverResponseDTO updateDriver(Integer driverId, DriverRequestDTO updatedData) {
        return driverRepository.findById(driverId)
                .map(existing -> {
                    if (updatedData.getLicenseNo() != null &&
                            !updatedData.getLicenseNo().equals(existing.getLicenseNo()) &&
                            driverRepository.existsByLicenseNo(updatedData.getLicenseNo())) {
                        throw new RuntimeException("駕照號碼已存在，無法更新");
                    }
                    existing.setEmpId(updatedData.getEmpId());
                    existing.setLicenseNo(updatedData.getLicenseNo());
                    existing.setLicenseExpiry(updatedData.getLicenseExpiry());
                    existing.setPetAvailable(updatedData.getPetAvailable());
                    existing.setVehicle(updatedData.getVehicle());
                    return toDto(driverRepository.save(existing));
                })
                .orElseThrow(() -> new RuntimeException("找不到司機 ID: " + driverId));
    }
}
