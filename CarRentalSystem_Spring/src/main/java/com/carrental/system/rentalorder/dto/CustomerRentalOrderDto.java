package com.carrental.system.rentalorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.carrental.system.rentalorder.entity.LongTermDetailBean;
import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.rentalorder.entity.ShortTermDetailBean;
import com.carrental.system.vehicle.entity.CarModel;
import com.carrental.system.vehicle.entity.Location;
import com.carrental.system.vehicle.entity.Vehicle;

import lombok.Data;

@Data
public class CustomerRentalOrderDto {
    private Integer orderId;
    private LocalDateTime orderTime;
    private String orderType;
    private String orderTypeCode;
    private LocalDateTime pickupTime;
    private LocalDateTime returnTime;
    private String pickupLocationName;
    private String returnLocationName;
    private String orderStatus;
    private String orderStatusCode;
    private String payStatus;
    private String payStatusCode;

    private Integer vehicleId;
    private String vehicleName;
    private String vehicleBrand;
    private String vehicleModel;
    private String plateNo;
    private String vehicleType;
    private String vehicleImageUrl;

    private BigDecimal rentalFee;
    private BigDecimal extraFee;
    private BigDecimal deposit;
    private BigDecimal totalAmount;
    private BigDecimal remainingBalance;
    private String depositPayMethod;
    private String balancePayMethod;
    private String orderRemark;

    private Integer contractMonths;
    private BigDecimal monthlyPayment;
    private Integer billingDay;
    private Integer paidMonths;
    private LocalDateTime actualPickupTime;
    private LocalDateTime actualReturnTime;
    private Integer startMileage;
    private Integer endMileage;
    private String fuelLevelReturn;
    private String processNote;

    public static CustomerRentalOrderDto from(RentalOrderBean order) {
        CustomerRentalOrderDto dto = new CustomerRentalOrderDto();
        dto.setOrderId(order.getOrderId());
        dto.setOrderTime(order.getOrderTime());
        dto.setOrderType(order.getOrderType() == null ? "" : order.getOrderType().getDescription());
        dto.setOrderTypeCode(order.getOrderType() == null ? "" : order.getOrderType().name());
        dto.setPickupTime(order.getPickupTime());
        dto.setReturnTime(order.getReturnTime());
        dto.setPickupLocationName(locationName(order.getPickupLocation()));
        dto.setReturnLocationName(locationName(order.getReturnLocation()));
        dto.setOrderStatus(order.getOrderStatus() == null ? "" : order.getOrderStatus().getDescription());
        dto.setOrderStatusCode(order.getOrderStatus() == null ? "" : order.getOrderStatus().name());
        dto.setPayStatus(order.getPayStatus() == null ? "" : order.getPayStatus().getDescription());
        dto.setPayStatusCode(order.getPayStatus() == null ? "" : order.getPayStatus().name());

        Vehicle vehicle = order.getVehicle();
        if (vehicle != null) {
            dto.setVehicleId(vehicle.getVehicleId());
            dto.setPlateNo(vehicle.getPlateNo());
            dto.setVehicleName(vehicleName(vehicle));
            CarModel model = vehicle.getCarModel();
            if (model != null) {
                dto.setVehicleBrand(model.getBrand());
                dto.setVehicleModel(model.getModelName());
                dto.setVehicleType(model.getVehicleType());
                dto.setVehicleImageUrl(model.getVehicleImgUrl());
            }
        }

        dto.setRentalFee(order.getRentalFee());
        dto.setExtraFee(order.getExtraFee());
        dto.setDeposit(order.getDeposit());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setRemainingBalance(calculateRemainingBalance(order));
        dto.setDepositPayMethod(order.getDepositPayMethod() == null ? "" : order.getDepositPayMethod().getDescription());
        dto.setBalancePayMethod(order.getBalancePayMethod() == null ? "" : order.getBalancePayMethod().getDescription());
        dto.setOrderRemark(order.getOrderRemark());

        applyShortTermDetail(dto, order.getShortTermDetail());
        applyLongTermDetail(dto, order.getLongTermDetail());
        return dto;
    }

    private static void applyShortTermDetail(CustomerRentalOrderDto dto, ShortTermDetailBean detail) {
        if (detail == null) return;
        dto.setActualPickupTime(detail.getActualPickupTime());
        dto.setActualReturnTime(detail.getActualReturnTime());
        dto.setStartMileage(detail.getStartMileage());
        dto.setEndMileage(detail.getEndMileage());
        dto.setFuelLevelReturn(detail.getFuelLevelReturn() == null ? "" : detail.getFuelLevelReturn().name());
        dto.setProcessNote(detail.getNoteDesc());
    }

    private static void applyLongTermDetail(CustomerRentalOrderDto dto, LongTermDetailBean detail) {
        if (detail == null) return;
        dto.setActualPickupTime(detail.getActualPickupTime());
        dto.setActualReturnTime(detail.getActualReturnTime());
        dto.setContractMonths(detail.getContractMonths());
        dto.setMonthlyPayment(detail.getMonthlyPayment());
        dto.setBillingDay(detail.getBillingDay());
        dto.setPaidMonths(detail.getPaidMonths());
        dto.setStartMileage(detail.getStartMileage());
        dto.setEndMileage(detail.getEndMileage());
        dto.setProcessNote(detail.getNoteDesc());
    }

    private static String locationName(Location location) {
        return location == null ? "" : location.getLocationName();
    }

    private static String vehicleName(Vehicle vehicle) {
        CarModel model = vehicle.getCarModel();
        if (model == null) return vehicle.getPlateNo();
        return (safe(model.getBrand()) + " " + safe(model.getModelName())).trim();
    }

    private static BigDecimal calculateRemainingBalance(RentalOrderBean order) {
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal deposit = order.getDeposit() == null ? BigDecimal.ZERO : order.getDeposit();
        BigDecimal remaining = total.subtract(deposit);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
