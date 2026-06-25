package com.carrental.system.login.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.carrental.system.login.entity.CustomerBean;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerBean, Integer> {

    Optional<CustomerBean> findByCustAccountAndCustPassword(String account, String password);

    boolean existsByCustAccount(String account);

    Optional<CustomerBean> findByCustEmail(String email);

    // 忘記密碼：透過 token 找到客戶
    Optional<CustomerBean> findByResetToken(String resetToken);

    //模糊查詢
    @Query("SELECT c FROM CustomerBean c WHERE " +
           "c.custName LIKE %:keyword% OR " +
           "c.custPhone LIKE %:keyword% OR " +
           "c.custEmail LIKE %:keyword% OR " +
           "c.custAccount LIKE %:keyword% OR " +
           "c.custAddress LIKE %:keyword%")
    List<CustomerBean> searchByKeyword(String keyword);
    
    //客戶登入，可以同時使用帳號或email
    @Query("SELECT c FROM CustomerBean c WHERE (c.custAccount = :accountOrEmail OR c.custEmail = :accountOrEmail) AND c.custPassword = :password")
    Optional<CustomerBean> findByAccountOrEmailAndPassword(
        @Param("accountOrEmail") String accountOrEmail, 
        @Param("password") String password
    );
}

