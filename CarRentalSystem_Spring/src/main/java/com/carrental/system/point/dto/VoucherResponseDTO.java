package com.carrental.system.point.dto;

import java.time.LocalDateTime;

import com.carrental.system.point.enums.VoucherStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponseDTO {

	private String voucherCode;
	private VoucherStatus status;
	private LocalDateTime expiryDate;
	private String productName; // 從 redemptionOrder → product 取得

}
