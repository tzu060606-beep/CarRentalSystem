package com.carrental.system.point.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.point.dto.ApiResponse;
import com.carrental.system.point.dto.VoucherResponseDTO;
import com.carrental.system.point.entity.Voucher;
import com.carrental.system.point.service.VoucherService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

	private final VoucherService voucherService;

	// 新增票券(insertVoucher內部呼叫，不是前端呼叫，這邊不實作)

	// 使用票券
	@PutMapping("/{voucherCode}/use")
	public ResponseEntity<ApiResponse> useVoucher(@PathVariable String voucherCode) {
		VoucherResponseDTO targetVoucher = voucherService.useVoucher(voucherCode);
		return ResponseEntity.ok(new ApiResponse(true, "兌換券已使用", targetVoucher));
	}

	// 查單筆票券
	@GetMapping("/{voucherCode}")
	public ResponseEntity<ApiResponse> findVoucherByVoucherCode(@PathVariable String voucherCode) {
		Voucher targetVoucher = voucherService.findByVoucherCode(voucherCode);
			return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(true, "查詢成功", targetVoucher));
	}

	// 查全部票券
	@GetMapping
	public ResponseEntity<ApiResponse> findAllVouchers() {
		List<Voucher> vouchersList = voucherService.findAllVouchers();
		return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(true, "查詢成功", vouchersList));
	}

	// 查某訂單的票券
	@GetMapping("/redemption/{redemptionId}")
	public ResponseEntity<ApiResponse> findVouchersByRemptionOrderId(@PathVariable Integer redemptionId) {
		List<Voucher> vouchersList = voucherService.findByRedemptionId(redemptionId);
			return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(true, "查詢成功", vouchersList));
	}

	// 查某客戶的票券
	@GetMapping("/customer/{custId}")
	public ResponseEntity<ApiResponse> findVouchersByCustId(@PathVariable Integer custId) {
		List<Voucher> vouchersList = voucherService.findByCustId(custId);
			return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(true, "查詢成功", vouchersList));
	}
}
