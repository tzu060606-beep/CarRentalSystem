//package com.carrental.system.point.controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.carrental.system.point.dto.ApiResponse;
//import com.carrental.system.point.enums.ChangeType;
//import com.carrental.system.point.service.PointsEventService;
//import lombok.RequiredArgsConstructor;
//
//@RestController
//@RequestMapping("/api/points")
//@RequiredArgsConstructor
//public class PointsController {
//
//	private final PointsEventService pointsEventService;
//	
//	 // 測試用，測完刪除
//    @PostMapping("/test/add-points")
//    //<?> 是萬用型別，代表「不確定回傳什麼型別」
//    public ResponseEntity<?> testAddPoints(
//        @RequestParam Integer custId,
//        @RequestParam Integer orderId,
//        @RequestParam String changeType) {
//        pointsEventService.addPoints(custId, orderId, ChangeType.valueOf(changeType));
//        return ResponseEntity.ok(new ApiResponse(true, "點數發放成功",null));
//    }
//}
