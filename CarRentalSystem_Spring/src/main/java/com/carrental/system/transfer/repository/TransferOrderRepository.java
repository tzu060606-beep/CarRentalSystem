package com.carrental.system.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.carrental.system.transfer.entity.TransferOrder;

@Repository
public interface TransferOrderRepository extends JpaRepository<TransferOrder, Integer> {
    List<TransferOrder> findByCustId(Integer custId);
}
