package com.carrental.system.vehicle.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.vehicle.entity.Location;
import com.carrental.system.vehicle.service.LocationService;

@RestController
@RequestMapping("/api/location")
@CrossOrigin // 用來處理CORS(跨域資源共用)，會允許特定所有外部玉的網頁應用程式請求後端API(避開SOP)
public class LocationController {

	private LocationService locationService;
	
	@Autowired //建構子注入
	public LocationController(LocationService locationService) {
		this.locationService = locationService;
	}
	
//	查單筆
	@GetMapping("/{locationId}")
	public Location getLocation(@PathVariable("locationId") Integer locationId) {
		Location location = locationService.getLocationById(locationId);
		return location;
	}
	
//	查全部
	@GetMapping
	public List<Location> listLocations(){
		return locationService.getAllLocations();
	}
	
//	增
	@PostMapping
	public Location addLocation(@RequestBody Location location) {
		//TODO: 錯誤收集器
		
		return locationService.createLocation(location);
	}
	
//	改
	@PutMapping("/{locationId}")
	public Location editLocation(
			@PathVariable("locationId") Integer locationId,
			@RequestBody Location location) {
		location.setLocationId(locationId);
		return locationService.updateLocation(location);
	}
	
//	刪
	@DeleteMapping("/{locationId}")
	public String removeLocation(@PathVariable("locationId") Integer locationId) {
		Location location = locationService.getLocationById(locationId);
		if (location != null) {
			locationService.deleteLocation(locationId);
			return "刪除成功";
		}
		return "查無此ID據點，無法刪除";
	}
}
