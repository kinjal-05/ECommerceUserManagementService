package com.userservice.servicesImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.userservice.commondtos.UserDeletedEvent;
import com.userservice.commondtos.UserEmailDto;
import com.userservice.commondtos.UserEmailsEvent;
import com.userservice.commondtos.UserFetchRequestEvent;
import com.userservice.dtos.*;
import com.userservice.enums.Role;
import com.userservice.eventPublish.UserEventPublisher;
import com.userservice.eventPublish.UserKafkaConfig;
import com.userservice.exceptions.EmailAlreadyExistsException;
import com.userservice.exceptions.ResourceNotFoundException;
import com.userservice.models.User;
import com.userservice.repositories.UserRepository;
import com.userservice.security.CustomUserDetails;
import com.userservice.security.JwtService;
import com.userservice.services.AuthService;
import com.userservice.services.RefreshTokenService;
import com.userservice.specifications.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	private final AuthenticationManager authenticationManager;
	private final UserKafkaConfig producer;
	private final StreamBridge streamBridge;
	private final UserEventPublisher userEventPublisher;

	@Override
	public UserResponse register(RegisterRequest request) {

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new EmailAlreadyExistsException("Email already exists");
		}

		User user = User.builder().email(request.getEmail()).password(passwordEncoder.encode(request.getPassword()))
				.firstName(request.getFirstname()).lastName(request.getLastname()).city(request.getCity())
				.state(request.getState()).country(request.getCountry()).streetAddress(request.getStreetAddress())
				.postalCode(request.getPostalCode()).role(Role.ROLE_USER).build();

		return mapToUserResponse(userRepository.save(user));
	}

	private UserResponse mapToUserResponse(User user) {
		return UserResponse.builder().id(user.getId()).email(user.getEmail()).firstname(user.getFirstName())
				.lastname(user.getLastName()).streetAddress(user.getStreetAddress()).city(user.getCity())
				.state(user.getState()).country(user.getCountry()).postalCode(user.getPostalCode()).role(user.getRole())
				.build();
	}

	@Override
	public void handleUserFetchRequest(UserFetchRequestEvent request) {
		List<UserEmailDto> users = userRepository.findAll().stream()
				.map(user -> new UserEmailDto(user.getId(), user.getEmail())).collect(Collectors.toList());
		System.out.println(users);
		UserEmailsEvent event = new UserEmailsEvent();
		event.setRequestId(request.getRequestId());
		event.setUsers(users);
		streamBridge.send("userEmails-out-0", event);
		System.out.println("Sent " + users.size() + " user emails");
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		User user = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		List<String> roles = List.of(user.getRole().name());
		String accessToken = jwtService.generateToken(userDetails, roles);
		String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
		return new AuthResponse(accessToken, refreshToken);
	}

	@Override
	public ResponseEntity<String> logoutByEmail(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
		refreshTokenService.deleteByUserId(user.getId());
		SecurityContextHolder.clearContext();
		return ResponseEntity.ok("Logged out successfully");
	}

	@Override
	public UserResponse getUserByEmail(String email) {
		return mapToUserResponse(
				userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found")));
	}

	@Override
	public UserResponse getUserById(Long id) {
		return mapToUserResponse(
				userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found")));
	}

	@Override
	public Page<UserResponse> getAllUsers(Pageable pageable) {
		return userRepository.findAll(pageable).map(this::mapToUserResponse);
	}

	@Override
	public UserResponse updateUserProfile(String email, UpdateUserRequest request) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		User updatedUser = user.toBuilder().firstName(request.getFirstname()).lastName(request.getLastname())
				.city(request.getCity()).state(request.getState()).country(request.getCountry())
				.streetAddress(request.getStreetAddress()).postalCode(request.getPostalCode()).build();
		return mapToUserResponse(userRepository.save(updatedUser));
	}

	@Override
	public void deleteUserByEmail(String email) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		refreshTokenService.deleteByUserId(user.getId());

		UserDeletedEvent event = UserDeletedEvent.builder().userId(user.getId()).email(user.getEmail())
				.deletedAt(LocalDateTime.now()).build();

		boolean isPublished = userEventPublisher.publishUserDeleted(event);

		if (isPublished) {
			userRepository.delete(user);
			System.out.println("User deleted after event published");
		} else {
			throw new RuntimeException("Failed to publish UserDeleted event");
		}
	}

	@Override
	public UserResponse changeUserRole(Long userId, Role role) {
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Role roleEnum = Role.ROLE_ADMIN;
		user.setRole(roleEnum);
		User updatedUser = userRepository.save(user);
		return mapToUserResponse(updatedUser);
	}

	@Override
	public Page<UserResponse> searchUsers(String email, String firstname, String lastname, String streetAddress,
	                                      String city, String state, String country, String postalCode, Pageable pageable) {
		Specification<User> spec = UserSpecification.searchUsers(email, firstname, lastname, streetAddress, city, state,
				country, postalCode);
		return userRepository.findAll(spec, pageable).map(this::mapToUserResponse);
	}

	@Override
	public UserResponse getUser1ById(Long id) {
		return mapToUserResponse(
				userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found")));
	}
}

