package com.carrental.system.rentalorder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiInsertRequest {
	private AdminOrderReqDto orders;
	private AdminLongTermReqDto longTerm;
	private AdminShortTermReqDto shortTerm;

}
