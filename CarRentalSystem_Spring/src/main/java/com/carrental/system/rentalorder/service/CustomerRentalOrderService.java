package com.carrental.system.rentalorder.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.login.dto.CustomerResponseDTO;
import com.carrental.system.login.service.CustomerService;
import com.carrental.system.rentalorder.dto.CustomerRentalOrderDto;
import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.rentalorder.repository.RentalOrderRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class CustomerRentalOrderService {

    
    @Autowired
    private CustomerService customerService;

    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    
    @Transactional(readOnly = true)
    public List<CustomerRentalOrderDto> findMyOrders() {
        Integer custId = getCurrentCustomerId();

        return rentalOrderRepository
                .findByCustomer_CustIdOrderByOrderTimeDesc(custId)
                .stream()
                .map(CustomerRentalOrderDto::from)//.map(order -> CustomerRentalOrderDto.from(order))
                .collect(Collectors.toList());
                //collect 的中文是「收集」。在 Java Stream API 中，它被稱為「終端操作 (Terminal Operation)」。
                
                /*
                如果沒有在最後面加上終端操作，這條輸送帶根本連動都不會動！
                白話文：「加工結束了！現在請啟動輸送帶，並把掉下來的成品通通收集起來。」

                Collectors 是一個工具箱，裡面提供了各種「打包方式」。
                toList()：告訴系統「請幫我拿一個標準的 List (清單) 紙箱，把成品全部裝進去。」
                */
    }

    @Transactional(readOnly = true)
    public CustomerRentalOrderDto findMyOrder(Integer orderId) {
        Integer custId = getCurrentCustomerId();

        RentalOrderBean order = rentalOrderRepository
                .findByOrderIdAndCustomer_CustId(orderId, custId)
                .orElseThrow(() -> new EntityNotFoundException("找不到這筆租車訂單"));

        return CustomerRentalOrderDto.from(order);
    }

    private Integer getCurrentCustomerId() {
        CustomerResponseDTO currentCustomer = customerService.getCurrentCustomer();

        if (currentCustomer == null || currentCustomer.getCustId() == null) {
            throw new SecurityException("尚未登入會員");
        }

        return currentCustomer.getCustId();
    }
}