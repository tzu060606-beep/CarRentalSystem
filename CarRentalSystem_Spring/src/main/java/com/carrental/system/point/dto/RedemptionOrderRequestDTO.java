package com.carrental.system.point.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedemptionOrderRequestDTO {

	// custId 移掉，因為它不應該從前端傳，後端從 token 取更安全。
	// 欄位：productId、productQuantity
	private Integer productId;
	private Integer productQuantity;

}
