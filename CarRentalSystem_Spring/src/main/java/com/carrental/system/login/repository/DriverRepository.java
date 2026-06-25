package com.carrental.system.login.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.carrental.system.login.entity.DriverBean;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<DriverBean, Integer> {

    // 透過員工ID尋找司機
    Optional<DriverBean> findByEmpId(Integer empId);

    // 檢查該員工是否已經是司機
    boolean existsByEmpId(Integer empId);

    // 檢查駕照號碼是否重複
    boolean existsByLicenseNo(String licenseNo);
}
