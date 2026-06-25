package com.carrental.system.login.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.carrental.system.login.dto.LoggedInUserDTO;

@Service
public class AuthService {

	public LoggedInUserDTO getLoggedInUser() {
		try {
			LoggedInUserDTO loggedInUserDTO = (LoggedInUserDTO) SecurityContextHolder.getContext().getAuthentication()
					.getPrincipal();

			return loggedInUserDTO;
		} catch (Exception e) {

			// 只要有任何錯誤，表示沒登入
			throw new RuntimeException("這個錯誤表示，有某個組員呼叫「取得登入者」的方法，但是他沒登入");
		}

	}
}
