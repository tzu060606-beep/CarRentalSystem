package com.carrental.system.point.dto;

import com.carrental.system.point.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
///Q:為什麼需要無參建構子
///當前端送來{"orderStatus": "USED"}這個json，Spring 要把這個 JSON 轉成 Java 的 RedemptionOrderStatusDTO 物件，這個過程叫做反序列化（Deserialization）。
///Jackson（Spring 內建的 JSON 處理工具）的做法是：
///1.先用無參數建構子建立一個空的物件
///2.再把 JSON 裡的值一個一個 set 進去
///如果沒有無參數建構子，第一步就會失敗，Spring 會報錯。
public class RedemptionOrderStatusDTO {

	// 只需更新狀態欄位
	private OrderStatus orderStatus;
}
