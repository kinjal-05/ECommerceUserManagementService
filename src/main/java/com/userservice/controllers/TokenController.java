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
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenController
{

	private final RefreshTokenRepository refreshTokenRepository;
	private final RefreshTokenService refreshTokenService;
	private final JwtService jwtService;

	@PostMapping("/refresh-token")
	public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request)
	{

		RefreshToken refreshToken = refreshTokenRepository
				.findByToken(request.getRefreshToken())
				.orElseThrow(() -> new RuntimeException("Invalid refresh token"));

		refreshTokenService.verifyExpiration(refreshToken);



		var user = refreshToken.getUser();

		CustomUserDetails userDetails = new CustomUserDetails(user);

// Convert single role to list
		List<String> roles = List.of(user.getRole().name());


		String accessToken = jwtService.generateToken(userDetails,roles);

		return ResponseEntity.ok(
				new AuthResponse(accessToken, request.getRefreshToken())
		);
	}
}

