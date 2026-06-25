package com.carrental.system.login.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.carrental.system.login.dto.LoggedInUserDTO;
import com.carrental.system.login.dto.LoggedInUserDTO.TableType;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	// OncePerRequestFilter = 每個 Request 只會執行一次，每次 API 被呼叫，這個 Filter 都會先執行

	private final JwtUtil jwtUtil; // 驗證工具

	public JwtAuthenticationFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		// 從 Request 的 Header 中找 "Authorization" 欄位
		String header = request.getHeader("Authorization");

		// 檢查是不是 JWT 格式 (JWT 標準格式：Bearer 空格 Token)
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7); // 取出 Token，把 "Bearer " 切掉

			try {
				// 驗證 Token + 解析內容
				Claims claims = jwtUtil.getClaimsFromToken(token);

				// 取出帳號 & 權限

				String id = claims.getSubject(); // Subject = sub 是 JWT 規範中「官方建議」拿來放使用者身份的欄位）
				String role = claims.get("role", String.class);
				
				if (Strings.isBlank(role)) {
					role = "CUSTOMER";
				}

				LoggedInUserDTO loggedInUserDTO = new LoggedInUserDTO();
				loggedInUserDTO.setId(Integer.valueOf(id));
				loggedInUserDTO.setRole(role);
				loggedInUserDTO.setTable("CUSTOMER".equals(role) ? TableType.CUSTOMER : TableType.EMPLOYEE);

				List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
				// 向 Spring 總部報告：「這個人身分合法，帳號是 account，權限是 authority，請幫他掛上識別證」
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loggedInUserDTO,
						null, authorities);

				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (Exception e) {
				// 如果驗正失敗 (例如過期了)什麼都不做。後面他就會被系統擋下。
				SecurityContextHolder.clearContext();
				// 偉:我自己測試多加一行
				e.printStackTrace();
				System.out.println("🚨 Token 驗證失敗原因：" + e.getMessage());
			}
		}
		// 檢查完畢，放行給系統的下一關去處理
		chain.doFilter(request, response);
	}
}
