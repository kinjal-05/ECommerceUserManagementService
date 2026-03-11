package com.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);


		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			logger.debug("No Authorization header found for URI: {}", request.getRequestURI());
			filterChain.doFilter(request, response);
			return;
		}

		String jwt = authHeader.substring(7);
		String username;

		try {

			username = jwtService.extractUsername(jwt);
		} catch (Exception ex) {
			logger.warn("Failed to extract username from JWT: {}", ex.getMessage());
			filterChain.doFilter(request, response);
			return;
		}


		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails;
			try {
				userDetails = userDetailsService.loadUserByUsername(username);
			} catch (Exception ex) {
				logger.warn("User not found: {}", ex.getMessage());
				filterChain.doFilter(request, response);
				return;
			}

			if (jwtService.isTokenValid(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken authToken =
						new UsernamePasswordAuthenticationToken(
								userDetails,
								null,
								userDetails.getAuthorities()
						);
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
				logger.debug("JWT authentication successful for user: {}", username);
			} else {
				logger.warn("Invalid JWT token for user: {}", username);
			}
		}


		filterChain.doFilter(request, response);
	}
}

