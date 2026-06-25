package com.carrental.system.vehicle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.carrental.system.vehicle.entity.CarModel;


@Repository
public interface CarModelRepository extends JpaRepository<CarModel, Integer> {

//	查單筆「包含已軟刪除」
	@Query(value = "SELECT * FROM car_model WHERE model_id = ?1", nativeQuery = true)
	Optional<CarModel> findByIdIncludingDeleted(Integer modelId);
	
//	查全部「包含已軟刪除」
	@Query(value = "SELECT * FROM car_model", nativeQuery = true)
	List<CarModel> findAllIncludingDeleted();
}
