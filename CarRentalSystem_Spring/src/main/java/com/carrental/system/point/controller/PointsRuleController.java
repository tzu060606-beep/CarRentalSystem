package com.carrental.system.point.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.point.dto.ApiResponse;
import com.carrental.system.point.entity.PointsRule;
import com.carrental.system.point.service.PointsRuleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/rules")
public class PointsRuleController {

	// 建構子注入
	private final PointsRuleService pointsRuleService;

	// 新增規則
	@PostMapping
	public ResponseEntity<ApiResponse> insertPointsRule(@RequestBody PointsRule pointsRule) {
		PointsRule targetPointsRule = pointsRuleService.insertPointsRule(pointsRule);
			return ResponseEntity.ok(new ApiResponse(true, "新增成功", targetPointsRule));
	}

	// 修改規則
	@PutMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> updatePointsRule(@PathVariable Integer id,
			@RequestBody PointsRule pointsRule) {
		PointsRule targetPointsRule = pointsRuleService.updatePointsRule(id, pointsRule);
			return ResponseEntity.ok(new ApiResponse(true,  "修改Id為" + id + "的點數規則", targetPointsRule));
	}

	// 刪除規則
	@DeleteMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> deletePointsRule(@PathVariable Integer id) {
		pointsRuleService.deletePointsRuleById(id);
			return ResponseEntity.ok(new ApiResponse(true, "刪除Id為" + id + "的點數規則", null));	
	}

	// 查詢單筆規則
	@GetMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> findPointsRuleById(@PathVariable Integer id) {
		PointsRule targetPointsRule = pointsRuleService.findPointsRuleById(id);
			return ResponseEntity.ok(new ApiResponse(true, "查詢成功", targetPointsRule));
	}

	// 查詢全部規則
	@GetMapping
	public ResponseEntity<ApiResponse> findAllPointsRules() {
		List<PointsRule> allPointsRules = pointsRuleService.findAllPointsRules();
		return ResponseEntity.ok(new ApiResponse(true,  "查詢成功", allPointsRules));
	}

	// 查詢規則關鍵字
	@GetMapping(path = "/search")
	public ResponseEntity<ApiResponse> findPointsRulesByKeyword(@RequestParam String keyword) {
		List<PointsRule> allPointsRules = pointsRuleService.findPointsRulesByKeyword(keyword);
		return ResponseEntity.ok(new ApiResponse(true,  "查詢成功", allPointsRules));
	}

}
