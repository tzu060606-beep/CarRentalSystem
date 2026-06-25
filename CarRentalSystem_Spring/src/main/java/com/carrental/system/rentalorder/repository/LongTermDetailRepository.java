package com.carrental.system.rentalorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.carrental.system.rentalorder.entity.LongTermDetailBean;




public interface LongTermDetailRepository extends JpaRepository<LongTermDetailBean, Integer>, JpaSpecificationExecutor<LongTermDetailBean> {

}
