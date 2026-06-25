package com.carrental.system.transfer.repository;

import com.carrental.system.transfer.entity.TransferRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRateRepository extends JpaRepository<TransferRate, Integer> {
    // 這裡不需要寫程式碼，Spring 會自動處理連線與抓取
}