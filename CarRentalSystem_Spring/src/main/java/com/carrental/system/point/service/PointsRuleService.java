package com.carrental.system.point.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.carrental.system.point.entity.PointsRule;
import com.carrental.system.point.exception.ResourceNotFoundException;
import com.carrental.system.point.repository.PointsRuleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PointsRuleService {

	private final PointsRuleRepository pointsRuleRepository;

	// 新增規則
	public PointsRule insertPointsRule(PointsRule pointsRule) {
		PointsRule newPointsRule = pointsRuleRepository.save(pointsRule);
		return newPointsRule;
	}

	// 修改規則
	public PointsRule updatePointsRule(Integer id, PointsRule pointsRule) {
		Optional<PointsRule> targetPointsRule = pointsRuleRepository.findById(id);

		PointsRule p = targetPointsRule.orElseThrow(() -> new ResourceNotFoundException("查無id為" + id + "的點數規則"));
		p.setRuleName(pointsRule.getRuleName());
		p.setRuleKey(pointsRule.getRuleKey());
		p.setRatio(pointsRule.getRatio());
		p.setDescription(pointsRule.getDescription());
		p.setIsActive(pointsRule.getIsActive());
		PointsRule updatePointsRule = pointsRuleRepository.save(p);
		return updatePointsRule;

	}

	// 刪除規則
	public void deletePointsRuleById(Integer id) {
		pointsRuleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("查無id為" + id + "的點數規則"));
		pointsRuleRepository.deleteById(id);

	}

	// 查詢單筆規則
	public PointsRule findPointsRuleById(Integer id) {
		Optional<PointsRule> targetPointsRule = pointsRuleRepository.findById(id);
		return targetPointsRule.orElseThrow(() -> new ResourceNotFoundException("查無id為" + id + "的點數規則"));
	}

	// 查詢全部規則
	public List<PointsRule> findAllPointsRules() {
		List<PointsRule> pointsRulesList = pointsRuleRepository.findAll();
		return pointsRulesList;
	}

	// 查詢規則關鍵字
	public List<PointsRule> findPointsRulesByKeyword(String keyword) {
		List<PointsRule> pointsRulesList = pointsRuleRepository.findByKeyword(keyword);
		return pointsRulesList;
	}

}
