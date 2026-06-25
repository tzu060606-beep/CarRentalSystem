package com.carrental.system.rentalorder.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
	
	//http://localhost:8081/hello
    @GetMapping("/hello")
    public String sayHello() {
        return "車輛租賃系統啟動成功！快來寫 Code！";
    }
}