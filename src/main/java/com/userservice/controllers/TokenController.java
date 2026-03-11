package com.userservice.controllers;

import com.userservice.dtos.AuthResponse;
import com.userservice.dtos.RefreshTokenRequest;
import com.userservice.models.RefreshToken;
import com.userservice.repositories.RefreshTokenRepository;
import com.userservice.security.CustomUserDetails;
import com.userservice.security.JwtService;
import com.userservice.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Route path is resolved from api-paths.yml at startup.
 *
 * api.auth.base  → /api/auth           (shared base with AuthController)
 * api.token.refresh → /refresh-token
 *
 * Final resolved URL: POST /api/auth/refresh-token
 */
@RestController
@RequestMapping("${api.auth.base}")
@RequiredArgsConstructor
public class TokenController {

	private final RefreshTokenRepository refreshTokenRepository;
	private final RefreshTokenService refreshTokenService;
	private final JwtService jwtService;

	@PostMapping("${api.token.refresh}")
	public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {

		RefreshToken refreshToken = refreshTokenRepository
				.findByToken(request.getRefreshToken())
				.orElseThrow(() -> new RuntimeException("Invalid refresh token"));

		refreshTokenService.verifyExpiration(refreshToken);

		var user = refreshToken.getUser();

		CustomUserDetails userDetails = new CustomUserDetails(user);

		List<String> roles = List.of(user.getRole().name());

		String accessToken = jwtService.generateToken(userDetails, roles);

		return ResponseEntity.ok(
				new AuthResponse(accessToken, request.getRefreshToken())
		);
	}
}