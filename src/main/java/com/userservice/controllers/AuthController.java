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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.ok(authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@GetMapping("/users")
	public ResponseEntity<Page<UserResponse>> getAllUsers(
			@RequestHeader("X-USER-ID") @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "id") String sortBy) {

		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		return ResponseEntity.ok(authService.getAllUsers(pageable));
	}

	@PatchMapping("/me")
	public ResponseEntity<UserResponse> updateProfile(@RequestHeader("X-USER-ID") String email,
	                                                  @Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(authService.updateUserProfile(email, request));
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponse> getProfile(@RequestHeader("X-USER-ID") String email) {
		System.out.println("Logged-in user email: " + email);

		if (email == null || email.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		UserResponse user = authService.getUserByEmail(email);
		return ResponseEntity.ok(user);
	}

	@DeleteMapping("/me")
	public ResponseEntity<String> deleteProfile(@RequestHeader("X-USER-EMAIL") String email) {
		authService.deleteUserByEmail(email);
		return ResponseEntity.ok("Your account has been deleted successfully");
	}

	private String getCurrentUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication.getName();
	}

	@GetMapping("/users/{id}")
	public ResponseEntity<UserResponse> getUserById(@RequestHeader("X-USER-ID") @PathVariable Long id) {
		return ResponseEntity.ok(authService.getUserById(id));
	}

	@GetMapping("/getUser1ById/{id}")
	public ResponseEntity<UserResponse> getUser1ById(@PathVariable Long id) {
		System.out.println(id);
		return ResponseEntity.ok(authService.getUserById(id));
	}

	@PutMapping("/users/{id}/role")
	public ResponseEntity<UserResponse> changeUserRole(@RequestHeader("X-USER-ID") @PathVariable Long id,
	                                                   @Valid @RequestBody ChangeUserRoleRequest request) {

		return ResponseEntity.ok(authService.changeUserRole(id, request.getRole()));
	}

	@PostMapping("/logout")
	public ResponseEntity<String> logout(@RequestHeader("X-USER-EMAIL") String email) {
		authService.logoutByEmail(email);
		return ResponseEntity.ok("Logged out successfully");
	}

	@GetMapping("/users/search")
	public ResponseEntity<Page<UserResponse>> searchUsers(
			@RequestHeader("X-USER-ID") @RequestParam(required = false) String email,
			@RequestParam(required = false) String firstname, @RequestParam(required = false) String lastname,
			@RequestParam(required = false) String streetAddress, @RequestParam(required = false) String city,
			@RequestParam(required = false) String state, @RequestParam(required = false) String country,
			@RequestParam(required = false) String postalCode, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "id") String sortBy) {

		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

		Page<UserResponse> users = authService.searchUsers(email, firstname, lastname, streetAddress, city, state,
				country, postalCode, pageable);

		return ResponseEntity.ok(users);
	}
}
