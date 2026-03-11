package com.userservice.controllers;

import com.userservice.dtos.*;
import com.userservice.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("${api.auth.base}")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("${api.auth.register}")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(authService.register(request));
	}

	@PostMapping("${api.auth.login}")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("${api.auth.logout}")
	public ResponseEntity<Void> logout(@RequestHeader("X-USER-EMAIL") String email) {
		authService.logoutByEmail(email);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("${api.auth.me}")
	public ResponseEntity<UserResponse> getProfile(@RequestHeader("X-USER-EMAIL") String email) {
		return ResponseEntity.ok(authService.getUserByEmail(email));
	}

	@PatchMapping("${api.auth.me}")
	public ResponseEntity<UserResponse> updateProfile(
			@RequestHeader("X-USER-EMAIL") String email,
			@Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(authService.updateUserProfile(email, request));
	}

	@DeleteMapping("${api.auth.me}")
	public ResponseEntity<Void> deleteProfile(@RequestHeader("X-USER-EMAIL") String email) {
		authService.deleteUserByEmail(email);
		return ResponseEntity.noContent().build();
	}


	@GetMapping("${api.auth.users}")
	public ResponseEntity<Page<UserResponse>> getAllUsers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy) {

		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		return ResponseEntity.ok(authService.getAllUsers(pageable));
	}

	@GetMapping("${api.auth.users-search}")
	public ResponseEntity<Page<UserResponse>> searchUsers(
			@RequestParam(required = false) String email,
			@RequestParam(required = false) String firstname,
			@RequestParam(required = false) String lastname,
			@RequestParam(required = false) String streetAddress,
			@RequestParam(required = false) String city,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String country,
			@RequestParam(required = false) String postalCode,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy) {

		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		return ResponseEntity.ok(
				authService.searchUsers(email, firstname, lastname,
						streetAddress, city, state, country, postalCode, pageable)
		);
	}

	@GetMapping("${api.auth.users-by-id}")
	public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
		return ResponseEntity.ok(authService.getUserById(id));
	}

	@PutMapping("${api.auth.users-role}")
	public ResponseEntity<UserResponse> changeUserRole(
			@PathVariable Long id,
			@Valid @RequestBody ChangeUserRoleRequest request) {
		return ResponseEntity.ok(authService.changeUserRole(id, request.getRole()));
	}
}