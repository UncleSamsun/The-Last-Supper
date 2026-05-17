package com.goorm.thelastsupper.common.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.goorm.thelastsupper.common.security.exception.AuthException;
import com.goorm.thelastsupper.common.util.HeaderUtil;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {
	private final TokenProvider tokenProvider;

	private final List<String> notJwtPaths = List.of(
		"/api/v1/signup",
		"/api/v1/login",
		"/api/v1/refresh",
		"/actuator",
		"/metrics",
		"/prometheus",
		"/swagger-ui",
		"/v3/api-docs"
	);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String uri = request.getRequestURI();

		String userAgent = request.getHeader("User-Agent");
		if (userAgent != null && userAgent.contains("Apache-HttpClient")) {
			filterChain.doFilter(request, response);
			return;
		}

		if (notJwtPaths.stream().noneMatch(uri::startsWith)) {
			try {
				String token = HeaderUtil.getAccessToken(request);
				Authentication auth = tokenProvider.getAuthentication(token);
				SecurityContextHolder.getContext().setAuthentication(auth);
			} catch (ExpiredJwtException e) {
				log.info("1");
				throw new AuthException.TokenExpiredException();
			} catch (UnsupportedJwtException e) {
				log.info("2");
				throw new AuthException.UnsupportedTokenException();
			} catch (SignatureException | SecurityException | MalformedJwtException e) {
				log.info("3");
				throw new AuthException.TokenParsingException();
			} catch (IllegalArgumentException e) {
				log.info("4");
				throw new AuthException.InvalidAuthHeaderException();
			}
		}

		filterChain.doFilter(request, response);
	}
}
