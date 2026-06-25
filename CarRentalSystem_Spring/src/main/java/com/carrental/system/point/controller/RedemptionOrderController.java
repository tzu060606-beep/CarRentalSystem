package com.carrental.system.point.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.carrental.system.login.dto.CustomerResponseDTO;
import com.carrental.system.login.service.CustomerService;
import com.carrental.system.point.dto.ApiResponse;
import com.carrental.system.point.dto.RedemptionOrderRequestDTO;
import com.carrental.system.point.dto.RedemptionOrderResponseDTO;
import com.carrental.system.point.dto.RedemptionOrderStatusDTO;
import com.carrental.system.point.entity.RedemptionOrder;
import com.carrental.system.point.service.RedemptionOrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/orders")
public class RedemptionOrderController {

	// 建構子注入
	private final RedemptionOrderService redemptionOrderService;
	private final CustomerService customerService;

	// 新增兌換訂單
	//[重構]-因為有GlobalExceptionHandler的關係，不用再寫null驗證了，直接return成功
	@PostMapping
	public ResponseEntity<ApiResponse> insertRedemptionOrder(@RequestBody RedemptionOrderRequestDTO redemptionOrderRequestDTO) {
		
		//取得現在登入的客戶資料
		CustomerResponseDTO currentCustomer = customerService.getCurrentCustomer();
		//取出客戶id
		Integer custId = currentCustomer.getCustId();
		//用custId建立訂單
		RedemptionOrder targetRedemptionOrder = redemptionOrderService.insertRedemptionOrder(custId,redemptionOrderRequestDTO);
		return ResponseEntity.ok(new ApiResponse(true, "新增成功", targetRedemptionOrder));	
	}
	
	//  [舊版]
	//	@PostMapping
	//	public ResponseEntity<ApiResponse> insertRedemptionOrder(@RequestBody RedemptionOrderRequestDTO redemptionOrderRequestDTO) {
	//		RedemptionOrder targetRedemptionOrder = redemptionOrderService.insertRedemptionOrder(redemptionOrderRequestDTO);
	//		if (targetRedemptionOrder == null) {
	//			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "新增失敗", null));
	//		} else {
	//			return ResponseEntity.ok(new ApiResponse(true, "新增成功", targetRedemptionOrder));
	//		}
	//	}

	
	// 修改兌換訂單(只能修改狀態)
	// 改傳dto並拿掉null判斷，service會拋例外不會回傳null
	// [測試]如果修改過狀態再送一次一樣的狀態，updateTime 就不會變，因為狀態沒有改變，條件不成立。
	@PutMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> updateRedemptionOrder(@PathVariable Integer id,
			@RequestBody RedemptionOrderStatusDTO redemptionOrderStatusDTO) {
		RedemptionOrder targetRedemptionOrder = redemptionOrderService.updateRedemptionOrderStatus(id, redemptionOrderStatusDTO);
			return ResponseEntity.ok(new ApiResponse(true, "修改成功", targetRedemptionOrder));
	}
	
	
	// [舊版]修改兌換訂單
	// @PutMapping(path = "/{id}")
	// public ResponseEntity<ApiResponse> updateRedemptionOrder(@PathVariable Integer id,
	//			@RequestBody RedemptionOrder redemptionOrder) {
	//		RedemptionOrder targetRedemptionOrder = redemptionOrderService.updateRedemptionOrderStatus(id, redemptionOrder);
	//		if (targetRedemptionOrder == null) {
	//			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "修改失敗", null));
	//		} else {
	//			return ResponseEntity.ok(new ApiResponse(true, "修改成功", targetRedemptionOrder));
	//		}
	//	}


	// 刪除兌換訂單
	@DeleteMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> deleteRedemptionOrder(@PathVariable Integer id) {
		redemptionOrderService.deleteRedemptionOrderById(id);
			return ResponseEntity.ok(new ApiResponse(true, "刪除成功", null));
	}

	// 查詢單筆兌換訂單
	@GetMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> findRedemptionOrderById(@PathVariable Integer id) {
		RedemptionOrder targetRedemptionOrder = redemptionOrderService.findRedemptionOrderById(id);
			return ResponseEntity.ok(new ApiResponse(true, "查詢成功", targetRedemptionOrder));
	}

	// 查詢全部兌換訂單
	@GetMapping
	public ResponseEntity<ApiResponse> findAllRedemptionOrders() {
		List<RedemptionOrder> allRedemptionOrders = redemptionOrderService.findAllRedemptionOrders();
		return ResponseEntity.ok(new ApiResponse(true, "查詢成功", allRedemptionOrders));
	}

	// 查詢兌換訂單關鍵字
	@GetMapping(path = "/search")
	public ResponseEntity<ApiResponse> findRedemptionOrdersByKeyword(@RequestParam String keyword) {
		List<RedemptionOrder> targetRedemptionOrders = redemptionOrderService.findRedemptionOrdersByKeyword(keyword);
		return ResponseEntity.ok(new ApiResponse(true, "查詢成功", targetRedemptionOrders));

	}

	// 依訂單狀態篩選兌換訂單
	@GetMapping(path = "/status")
	public ResponseEntity<ApiResponse> findRedemptionOrdersByOrderStatus(@RequestParam String orderstatus) {
		List<RedemptionOrder> targetRedemptionOrders = redemptionOrderService
				.findRedemptionOrdersByOrderStatus(orderstatus);
		return ResponseEntity.ok(new ApiResponse(true, "查詢成功", targetRedemptionOrders));
	}
	
	// 後台專用：查全部訂單，附帶票券摘要
	// 【為什麼獨立一支 API】後台需要票券資訊，前台不需要，職責分開
	@GetMapping("/admin")
	public ResponseEntity<ApiResponse> findAllOrdersWithVouchers() {
		List<RedemptionOrderResponseDTO> allOrdersWithVouchers = redemptionOrderService.findAllOrdersWithVouchers();
		return ResponseEntity.ok(new ApiResponse(true, "查詢成功", allOrdersWithVouchers));
	}

}
