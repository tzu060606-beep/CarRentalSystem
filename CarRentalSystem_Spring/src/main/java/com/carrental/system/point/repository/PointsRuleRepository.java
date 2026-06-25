package com.carrental.system.point.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carrental.system.point.entity.PointsRule;

public interface PointsRuleRepository extends JpaRepository<PointsRule, Integer> {
	
	//查詢規則關鍵字
	@Query("SELECT p FROM PointsRule p WHERE p.ruleName LIKE CONCAT('%', :keyword, '%') OR p.description LIKE CONCAT('%', :keyword, '%')")
	List<PointsRule> findByKeyword(@Param("keyword")String keyword);

	//根據changeType找規則(獲取點數用)
	PointsRule findByRuleKey(String changeType);
	
	

}
