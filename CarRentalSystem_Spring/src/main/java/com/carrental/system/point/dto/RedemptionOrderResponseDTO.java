package com.carrental.system.point.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.carrental.system.point.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedemptionOrderResponseDTO {

	// 訂單資訊
	private Integer redemptionId; // 訂單編號
	private Integer custId; // 客戶編號
	private String custName; // 客戶姓名
	private Integer productId; // 商品編號
	private String productName; // 商品名稱
	private Integer productQuantity; // 兌換數量
	private Integer pointsSpent; // 花費點數
	private OrderStatus orderStatus; // 訂單狀態

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	private LocalDateTime createTime; // 建立時間

	// 票券摘要列表（後端直接組好，前端不需要自己對應）
	private List<VoucherSummaryDTO> vouchers;

	// 收據本身是「訂單摘要」，裡面的每一行商品是「明細」。
	// RedemptionOrderResponseDTO = 收據（訂單摘要）
	// VoucherSummaryDTO = 收據裡的每一行商品（票券明細）

	// DTO 是「為了特定 API 回應組裝的資料結構」，不受正規化約束，可以把需要的欄位平鋪進去。
	// RequestDTO → 前端傳進來的資料，代表「使用者的意圖」
	// ResponseDTO → 後端回傳的資料，代表「API 的回應」
}
