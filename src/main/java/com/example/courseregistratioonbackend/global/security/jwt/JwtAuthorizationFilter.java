package com.example.courseregistratioonbackend.global.security.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.courseregistratioonbackend.global.security.userdetails.UserDetailsServiceImpl;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "JWT 검증 후 인가")
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserDetailsServiceImpl userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws
		ServletException,
		IOException {

		String tokenValue = request.getHeader(JwtUtil.AUTHORIZATION_HEADER);

		if (!StringUtils.hasText(tokenValue)) { // 토큰이 존재하지 않을 때는 다음 필터로 이동
			filterChain.doFilter(request, response);
			return;
		}

		String token = jwtUtil.substringToken(tokenValue);

		if (!jwtUtil.validateToken(token)) { // 토큰 검증
			log.error("토큰 에러");
			return;
		}

		Claims info = jwtUtil.getClaimsFromToken(tokenValue);

		try {
			setAuthentication(info.getSubject());
		} catch (Exception e) {
			log.error("인증 처리 에러");
			return;
		}

		filterChain.doFilter(request, response);
	}

	// 인증 처리
	private void setAuthentication(String username) {

		SecurityContext context = SecurityContextHolder.createEmptyContext();

		Authentication authentication = createAuthentication(username);

		context.setAuthentication(authentication);

		SecurityContextHolder.setContext(context);
	}

	// 인증 객체 생성
	private Authentication createAuthentication(String username) {

		UserDetails userDetails = userDetailsService.loadUserByUsername(username);

		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	}
}