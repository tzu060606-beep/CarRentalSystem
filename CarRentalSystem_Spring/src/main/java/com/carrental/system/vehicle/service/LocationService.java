package com.carrental.system.vehicle.service;

import com.carrental.system.vehicle.entity.Vehicle;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.vehicle.entity.Location;
import com.carrental.system.vehicle.repository.LocationRepository;

@Service
@Transactional
public class LocationService {
	
//	private final Vehicle vehicle;
	private LocationRepository locationRepository;
	
	@Autowired //建構子注入
	public LocationService(LocationRepository locationRepository) {
		this.locationRepository = locationRepository;
//		this.vehicle = vehicle;
	}
	
//	查單筆
	public Location getLocationById(Integer locationId) {
		Optional<Location> location = locationRepository.findById(locationId);
		if (location.isPresent()) {
			return location.get();
		}
		return null;
	}
	
//	查全部
	public List<Location> getAllLocations(){
		return locationRepository.findAll();
	}
	
//	增
	public Location createLocation(Location location) {
		Location savedLocation = locationRepository.save(location);
//		return savedLocation;
		return locationRepository.findById(savedLocation.getLocationId()).orElse(savedLocation);
	}
	
//	改
	public Location updateLocation(Location location) {
		return locationRepository.save(location);
	}
	
//	刪
	public boolean deleteLocation(Integer locationId) {
		locationRepository.deleteById(locationId);
		return true;
	}
	
}
