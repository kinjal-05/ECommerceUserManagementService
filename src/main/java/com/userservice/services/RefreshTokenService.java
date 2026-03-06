package com.userservice.services;

import com.userservice.models.RefreshToken;
import com.userservice.models.User;

public interface RefreshTokenService
{
	RefreshToken createRefreshToken(User user);
	RefreshToken verifyExpiration(RefreshToken token);
	void deleteByUserId(Long userId);
}
