package com.carrental.system.point.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherSummaryDTO {

	private String voucherCode; // 票券號碼
	private String status;      // UNUSED / USED / EXPIRED
	private String expiryDate;  // 到期日
}
