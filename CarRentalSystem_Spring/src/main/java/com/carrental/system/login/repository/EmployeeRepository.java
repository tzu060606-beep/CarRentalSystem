package com.carrental.system.login.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.carrental.system.login.entity.EmployeeBean;

import java.util.Optional;


@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeBean, Integer> {

    // 登入功能：根據帳號和密碼查詢
    Optional<EmployeeBean> findByEmpAccountAndEmpPassword(String empAccount, String empPassword);

    // 檢查帳號是否存在
    boolean existsByEmpAccount(String empAccount);

    // 根據帳號查詢單一員工
    Optional<EmployeeBean> findByEmpAccount(String empAccount);

    // 模糊查詢
    @org.springframework.data.jpa.repository.Query("SELECT e FROM EmployeeBean e WHERE " +
           "e.empName LIKE %:keyword% OR " +
           "e.empAccount LIKE %:keyword% OR " +
           "e.empPhone LIKE %:keyword% OR " +
           "e.empEmail LIKE %:keyword%")
    java.util.List<EmployeeBean> searchByKeyword(String keyword);
}
