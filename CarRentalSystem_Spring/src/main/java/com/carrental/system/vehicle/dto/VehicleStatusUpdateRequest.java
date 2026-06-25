package com.carrental.system.vehicle.dto;

import com.carrental.system.vehicle.entity.VehicleStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VehicleStatusUpdateRequest {

	private VehicleStatus newStatus;

//	for「轉為CLEANING時，里程數自動更新」
	private Integer currentMileage;
	
//	for「轉為TOBEDISPATCHED」時，可能要知道是哪一張調度單
	private Integer dipatchLogId;
}
