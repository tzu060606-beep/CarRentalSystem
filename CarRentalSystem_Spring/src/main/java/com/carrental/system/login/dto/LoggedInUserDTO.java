package com.carrental.system.login.dto;

import lombok.Data;

@Data
public class LoggedInUserDTO {

	private Integer id; // 可能是會員或員工的 ID
	private String role; // emp, admin, customer
	private TableType table;

	public static enum TableType {
		EMPLOYEE, CUSTOMER;
	}
}
