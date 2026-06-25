package com.carrental.system.point.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.point.dto.ApiResponse;
import com.carrental.system.point.dto.PointsHistoryResponseDTO;
import com.carrental.system.point.entity.PointsHistory;
import com.carrental.system.point.service.PointsHistoryService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/histories")
public class PointsHistoryController {

	// 建構子注入
	private final PointsHistoryService pointsHistoryService;

	// =====================================================
	// API: F1呼叫，前端頁面顯示客戶的點數異動紀錄需要
	// 查詢某位客戶的所有點數異動紀錄
	// =====================================================
	@GetMapping("/customer/{custId}")
	public ResponseEntity<ApiResponse> findHistoryByCustId(@PathVariable Integer custId) {
		List<PointsHistoryResponseDTO> historyList = pointsHistoryService.findByCustId(custId);
		return ResponseEntity.ok(new ApiResponse(true, "查詢成功", historyList));
	}

	// 新增異動紀錄
	@PostMapping
	public ResponseEntity<ApiResponse> insertPointsHistory(@RequestBody PointsHistory pointsHistory) {
		PointsHistory targetPointsHistory = pointsHistoryService.insertPointsHistory(pointsHistory);
		return ResponseEntity.ok(new ApiResponse(true, "新增成功", targetPointsHistory));
	}

	// 修改異動紀錄
	@PutMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> updatePointsHistory(@PathVariable Integer id,
			@RequestBody PointsHistory pointsHistory) {
		PointsHistory targetPointsHistory = pointsHistoryService.updatePointsHistory(id, pointsHistory);
		return ResponseEntity.ok(new ApiResponse(true, "修改成功", targetPointsHistory));
	}

	// 刪除異動紀錄
	// 要如何區分傳入的參數要採用@PathVariable還是@ResponseBody還是@RequestParam?
	@DeleteMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> deletePointsHistory(@PathVariable Integer id) {
		pointsHistoryService.deletePointHistoryById(id);
		// 可以將ResponseEntity.ok()換成這種寫法嗎?效果相同嗎?
		return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(true, "刪除Id為" + id + "的點數異動紀錄", null));

	}

	// 查詢單筆異動紀錄
	@GetMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> findPointsHistoryById(@PathVariable Integer id) {
		PointsHistory targetPointsHistory = pointsHistoryService.findPointsHistoryById(id);
		return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(true, "查詢成功", targetPointsHistory));
	}

	// 查詢全部異動紀錄
	@GetMapping
	public ResponseEntity<ApiResponse> findAllPointsHistory() {
		List<PointsHistoryResponseDTO> allPointsHistories = pointsHistoryService.findAllPointsHistories();
		return ResponseEntity.ok(new ApiResponse(true, "查詢成功", allPointsHistories));
	}

	// 查詢異動紀錄關鍵字
	@GetMapping(path = "/search")
	public ResponseEntity<ApiResponse> findPointsHistoriesByKeyword(@RequestParam String keyword) {
		List<PointsHistory> allPointsHistories = pointsHistoryService.findPointsHistoriesByKeyword(keyword);
		return ResponseEntity.ok(new ApiResponse(true, "查詢成功", allPointsHistories));
	}

}
