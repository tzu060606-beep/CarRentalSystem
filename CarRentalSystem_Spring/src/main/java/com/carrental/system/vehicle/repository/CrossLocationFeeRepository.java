package com.carrental.system.vehicle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.carrental.system.vehicle.entity.CrossLocationFee;

@Repository
public interface CrossLocationFeeRepository extends JpaRepository<CrossLocationFee, Integer> {
	
	@Query("SELECT c FROM CrossLocationFee c WHERE c.fromLocation.locationId = :fromLocationId AND c.toLocation.locationId = :toLocationId")
	Optional<CrossLocationFee> findFeeByLocationIds(
			@Param("fromLocationId") Integer fromLocationId, 
			@Param("toLocationId") Integer toLocationId);

}
