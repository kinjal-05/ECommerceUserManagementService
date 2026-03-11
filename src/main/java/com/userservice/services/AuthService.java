package com.userservice.services;

import com.userservice.commondtos.UserFetchRequestEvent;
import com.userservice.dtos.*;
import com.userservice.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface AuthService {
	UserResponse register(RegisterRequest request);

	AuthResponse login(LoginRequest request);

	Page<UserResponse> getAllUsers(Pageable pageable);

	UserResponse updateUserProfile(String email, UpdateUserRequest request);

	UserResponse getUserByEmail(String email);

	void deleteUserByEmail(String email);

	UserResponse getUserById(Long userId);

	UserResponse getUser1ById(Long id);

	UserResponse changeUserRole(Long userId, Role role);

	ResponseEntity<String> logoutByEmail(String email);

	Page<UserResponse> searchUsers(String email, String firstname, String lastname, String streetAddress, String city,
	                               String state, String country, String postalCode, Pageable pageable);

	public void handleUserFetchRequest(UserFetchRequestEvent request);
}
