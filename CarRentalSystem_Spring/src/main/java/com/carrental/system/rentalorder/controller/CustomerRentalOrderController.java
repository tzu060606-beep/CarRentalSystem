package com.carrental.system.rentalorder.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.rentalorder.dto.CustomerRentalOrderDto;
import com.carrental.system.rentalorder.service.CustomerRentalOrderService;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/customer/rentalorders")
public class CustomerRentalOrderController {
    
    
    @Autowired
    private CustomerRentalOrderService customerRentalOrderService;


    @GetMapping
    public ResponseEntity<?> getMyOrders() {
        try {
            List<CustomerRentalOrderDto> orders = customerRentalOrderService.findMyOrders();
            return ResponseEntity.ok(orders);
        } catch (SecurityException e) {
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getMyOrderDetail(@PathVariable Integer orderId) {
        try {
            CustomerRentalOrderDto order = customerRentalOrderService.findMyOrder(orderId);
            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (EntityNotFoundException e) {
            // return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));

            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // private Map<String, String> error(String message) {
    //     Map<String, String> body = new HashMap<>();
    //     body.put("error", message);
    //     return body;
    // }
}
