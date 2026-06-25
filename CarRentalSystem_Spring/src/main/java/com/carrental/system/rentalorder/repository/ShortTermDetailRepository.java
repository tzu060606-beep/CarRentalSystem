package com.carrental.system.rentalorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.carrental.system.rentalorder.entity.ShortTermDetailBean;



public interface ShortTermDetailRepository extends JpaRepository<ShortTermDetailBean, Integer>, JpaSpecificationExecutor<ShortTermDetailBean> {
	
	

}
