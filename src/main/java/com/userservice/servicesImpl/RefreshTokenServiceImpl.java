package com.userservice.servicesImpl;
import java.time.Instant;
import java.util.UUID;

import com.userservice.models.RefreshToken;
import com.userservice.models.User;
import com.userservice.repositories.RefreshTokenRepository;
import com.userservice.services.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;

	@Override
	public RefreshToken createRefreshToken(User user) {
		refreshTokenRepository.deleteByUserId(user.getId());
		RefreshToken refreshToken = RefreshToken.builder().user(user).token(UUID.randomUUID().toString())
				.expiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60)).build();
		return refreshTokenRepository.save(refreshToken);
	}

	@Override
	public RefreshToken verifyExpiration(RefreshToken token) {
		if (token.getExpiryDate().isBefore(Instant.now())) {
			refreshTokenRepository.delete(token);
			throw new RuntimeException("Refresh token expired. Please login again.");
		}
		return token;
	}

	@Override
	@Transactional
	public void deleteByUserId(Long userId) {
		refreshTokenRepository.deleteByUserId(userId);
	}
}

