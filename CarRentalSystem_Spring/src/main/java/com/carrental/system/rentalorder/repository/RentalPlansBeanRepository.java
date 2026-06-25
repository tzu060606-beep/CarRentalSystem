package com.carrental.system.rentalorder.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.carrental.system.rentalorder.entity.RentalPlansBean;




public interface RentalPlansBeanRepository extends JpaRepository<RentalPlansBean, Integer>, JpaSpecificationExecutor<RentalPlansBean> {

    // 用「車型 + 長短租」自動配對方案
    Optional<RentalPlansBean> findByAppliedVehicleTypeAndIsLongTerm(String appliedVehicleType, boolean isLongTerm);

}
