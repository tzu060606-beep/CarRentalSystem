package com.carrental.system.point.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {

	// 操作是否成功
	private Boolean success;
	// 前端看的訊息
	private String message;
	// 實際資料內容，無資料放null
	private Object data;

}
