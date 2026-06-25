package com.carrental.system.usedCar.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carrental.system.usedCar.entity.UsedCarBean;

public interface UsedCarRepository extends JpaRepository<UsedCarBean, Integer> {

}
