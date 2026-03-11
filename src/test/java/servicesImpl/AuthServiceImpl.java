package servicesImpl;

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
import com.userservice.models.RefreshToken;
import com.userservice.models.User;
import com.userservice.repositories.UserRepository;
import com.userservice.security.CustomUserDetails;
import com.userservice.security.JwtService;
import com.userservice.services.RefreshTokenService;
import com.userservice.servicesImpl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private JwtService jwtService;
	@Mock private RefreshTokenService refreshTokenService;
	@Mock private AuthenticationManager authenticationManager;
	@Mock private UserKafkaConfig producer;
	@Mock private StreamBridge streamBridge;
	@Mock private UserEventPublisher userEventPublisher;

	@InjectMocks
	private AuthServiceImpl authService;

	// ─────────────────────────────────────────────────────────────────────────
	// Helpers
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * User model fields: id, email, password, firstName, lastName,
	 *                    streetAddress, city, state, country, postalCode, role
	 */
	private User buildUser(Long id, String email, String password, Role role) {
		return User.builder()
				.id(id)
				.email(email)
				.password(password)
				.firstName("John")
				.lastName("Doe")
				.streetAddress("123 Main St")
				.city("Ahmedabad")
				.state("Gujarat")
				.country("India")
				.postalCode("380001")
				.role(role)
				.build();
	}

	/**
	 * RegisterRequest fields: email, password, firstname, lastname,
	 *                         streetAddress, city, state, country, postalCode
	 */
	private RegisterRequest buildRegisterRequest(String email, String password) {
		RegisterRequest req = new RegisterRequest();
		req.setEmail(email);
		req.setPassword(password);
		req.setFirstname("John");
		req.setLastname("Doe");
		req.setStreetAddress("123 Main St");
		req.setCity("Ahmedabad");
		req.setState("Gujarat");
		req.setCountry("India");
		req.setPostalCode("380001");
		return req;
	}

	/**
	 * LoginRequest fields: email (String), password (String)
	 */
	private LoginRequest buildLoginRequest(String email, String password) {
		LoginRequest req = new LoginRequest();
		req.setEmail(email);
		req.setPassword(password);
		return req;
	}

	/**
	 * UpdateUserRequest fields: firstname, lastname, city, state,
	 *                           country, streetAddress, postalCode
	 */
	private UpdateUserRequest buildUpdateRequest() {
		UpdateUserRequest req = new UpdateUserRequest();
		req.setFirstname("Jane");
		req.setLastname("Smith");
		req.setCity("Surat");
		req.setState("Gujarat");
		req.setCountry("India");
		req.setStreetAddress("456 Park Ave");
		req.setPostalCode("395001");
		return req;
	}

	// =========================================================================
	// register
	// =========================================================================

	@Nested
	@DisplayName("register()")
	class Register {

		@Test
		@DisplayName("Valid request → user saved and UserResponse returned")
		void validRequest_savesUserAndReturnsResponse() {
			RegisterRequest request = buildRegisterRequest("john@example.com", "pass123");
			User saved = buildUser(1L, "john@example.com", "encodedPass", Role.ROLE_USER);

			when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
			when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");
			when(userRepository.save(any(User.class))).thenReturn(saved);

			// UserResponse fields: id, email, firstname, lastname, streetAddress,
			//                      city, state, country, postalCode, role
			UserResponse response = authService.register(request);

			assertThat(response.getId()).isEqualTo(1L);
			assertThat(response.getEmail()).isEqualTo("john@example.com");
			assertThat(response.getFirstname()).isEqualTo("John");
			assertThat(response.getLastname()).isEqualTo("Doe");
			assertThat(response.getRole()).isEqualTo(Role.ROLE_USER);
			verify(userRepository).save(any(User.class));
		}

		@Test
		@DisplayName("Email already exists → EmailAlreadyExistsException")
		void emailExists_throwsException() {
			RegisterRequest request = buildRegisterRequest("existing@example.com", "pass");
			when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

			assertThatThrownBy(() -> authService.register(request))
					.isInstanceOf(EmailAlreadyExistsException.class)
					.hasMessageContaining("Email already exists");

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("Password is encoded before saving")
		void password_isEncoded() {
			RegisterRequest request = buildRegisterRequest("user@example.com", "rawPass");
			User saved = buildUser(1L, "user@example.com", "hashed", Role.ROLE_USER);

			when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
			when(passwordEncoder.encode("rawPass")).thenReturn("hashed");

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			when(userRepository.save(captor.capture())).thenReturn(saved);

			authService.register(request);

			assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
		}

		@Test
		@DisplayName("Registered user always gets ROLE_USER role")
		void registeredUser_hasRoleUser() {
			RegisterRequest request = buildRegisterRequest("new@example.com", "pass");
			User saved = buildUser(1L, "new@example.com", "enc", Role.ROLE_USER);

			when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
			when(passwordEncoder.encode(any())).thenReturn("enc");

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			when(userRepository.save(captor.capture())).thenReturn(saved);

			authService.register(request);

			assertThat(captor.getValue().getRole()).isEqualTo(Role.ROLE_USER);
		}

		@Test
		@DisplayName("User built with correct address fields from request")
		void userBuilt_withCorrectAddressFields() {
			RegisterRequest request = buildRegisterRequest("addr@example.com", "pass");
			User saved = buildUser(1L, "addr@example.com", "enc", Role.ROLE_USER);

			when(userRepository.existsByEmail(any())).thenReturn(false);
			when(passwordEncoder.encode(any())).thenReturn("enc");

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			when(userRepository.save(captor.capture())).thenReturn(saved);

			authService.register(request);

			User captured = captor.getValue();
			assertThat(captured.getCity()).isEqualTo("Ahmedabad");
			assertThat(captured.getState()).isEqualTo("Gujarat");
			assertThat(captured.getCountry()).isEqualTo("India");
			assertThat(captured.getPostalCode()).isEqualTo("380001");
		}
	}

	// =========================================================================
	// login
	// =========================================================================

	@Nested
	@DisplayName("login()")
	class Login {

		@Test
		@DisplayName("Valid credentials → returns AuthResponse with tokens")
		void validCredentials_returnsTokens() {
			User user = buildUser(1L, "john@example.com", "enc", Role.ROLE_USER);
			CustomUserDetails userDetails = new CustomUserDetails(user);

			Authentication auth = mock(Authentication.class);
			when(auth.getPrincipal()).thenReturn(userDetails);
			when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
					.thenReturn(auth);
			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
			when(jwtService.generateToken(eq(userDetails), anyList())).thenReturn("access-token");

			RefreshToken refreshToken = new RefreshToken();
			refreshToken.setToken("refresh-token");
			when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

			LoginRequest request = buildLoginRequest("john@example.com", "pass");

			// AuthResponse fields: accessToken (String), refreshToken (String)
			AuthResponse response = authService.login(request);

			assertThat(response.getAccessToken()).isEqualTo("access-token");
			assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
		}

		@Test
		@DisplayName("User not found after authentication → ResourceNotFoundException")
		void userNotFound_throwsException() {
			User user = buildUser(1L, "ghost@example.com", "enc", Role.ROLE_USER);
			CustomUserDetails userDetails = new CustomUserDetails(user);

			Authentication auth = mock(Authentication.class);
			when(auth.getPrincipal()).thenReturn(userDetails);
			when(authenticationManager.authenticate(any())).thenReturn(auth);
			when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

			LoginRequest request = buildLoginRequest("ghost@example.com", "pass");

			assertThatThrownBy(() -> authService.login(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("User not found");
		}

		@Test
		@DisplayName("JWT generated with correct roles list")
		void jwt_generatedWithCorrectRoles() {
			User user = buildUser(1L, "admin@example.com", "enc", Role.ROLE_ADMIN);
			CustomUserDetails userDetails = new CustomUserDetails(user);

			Authentication auth = mock(Authentication.class);
			when(auth.getPrincipal()).thenReturn(userDetails);
			when(authenticationManager.authenticate(any())).thenReturn(auth);
			when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
			when(jwtService.generateToken(any(), anyList())).thenReturn("token");

			RefreshToken rt = new RefreshToken();
			rt.setToken("rt");
			when(refreshTokenService.createRefreshToken(user)).thenReturn(rt);

			authService.login(buildLoginRequest("admin@example.com", "pass"));

			// roles list should contain "ROLE_ADMIN"
			ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
			verify(jwtService).generateToken(any(), rolesCaptor.capture());
			assertThat(rolesCaptor.getValue()).containsExactly("ROLE_ADMIN");
		}
	}

	// =========================================================================
	// logoutByEmail
	// =========================================================================

	@Nested
	@DisplayName("logoutByEmail()")
	class LogoutByEmail {

		@Test
		@DisplayName("Existing user → refresh token deleted and 200 OK returned")
		void validEmail_logsOut() {
			User user = buildUser(1L, "john@example.com", "enc", Role.ROLE_USER);
			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

			ResponseEntity<String> response = authService.logoutByEmail("john@example.com");

			assertThat(response.getStatusCodeValue()).isEqualTo(200);
			assertThat(response.getBody()).isEqualTo("Logged out successfully");
			verify(refreshTokenService).deleteByUserId(1L);
		}

		@Test
		@DisplayName("User not found → ResourceNotFoundException")
		void notFound_throwsException() {
			when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.logoutByEmail("ghost@example.com"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("User not found with email: ghost@example.com");

			verify(refreshTokenService, never()).deleteByUserId(any());
		}
	}

	// =========================================================================
	// getUserByEmail
	// =========================================================================

	@Nested
	@DisplayName("getUserByEmail()")
	class GetUserByEmail {

		@Test
		@DisplayName("Existing email → returns mapped UserResponse")
		void existingEmail_returnsResponse() {
			User user = buildUser(1L, "john@example.com", "enc", Role.ROLE_USER);
			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

			UserResponse response = authService.getUserByEmail("john@example.com");

			assertThat(response.getEmail()).isEqualTo("john@example.com");
			assertThat(response.getFirstname()).isEqualTo("John");
			assertThat(response.getRole()).isEqualTo(Role.ROLE_USER);
		}

		@Test
		@DisplayName("Non-existent email → ResourceNotFoundException")
		void notFound_throwsException() {
			when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.getUserByEmail("x@x.com"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("User not found");
		}
	}

	// =========================================================================
	// getUserById
	// =========================================================================

	@Nested
	@DisplayName("getUserById()")
	class GetUserById {

		@Test
		@DisplayName("Existing id → returns mapped UserResponse")
		void existingId_returnsResponse() {
			User user = buildUser(5L, "jane@example.com", "enc", Role.ROLE_ADMIN);
			when(userRepository.findById(5L)).thenReturn(Optional.of(user));

			UserResponse response = authService.getUserById(5L);

			assertThat(response.getId()).isEqualTo(5L);
			assertThat(response.getEmail()).isEqualTo("jane@example.com");
			assertThat(response.getRole()).isEqualTo(Role.ROLE_ADMIN);
		}

		@Test
		@DisplayName("Non-existent id → ResourceNotFoundException")
		void notFound_throwsException() {
			when(userRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.getUserById(99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("User not found");
		}
	}

	// =========================================================================
	// getUser1ById
	// =========================================================================

	@Nested
	@DisplayName("getUser1ById()")
	class GetUser1ById {

		@Test
		@DisplayName("Existing id → returns mapped UserResponse")
		void existingId_returnsResponse() {
			User user = buildUser(3L, "user1@example.com", "enc", Role.ROLE_USER);
			when(userRepository.findById(3L)).thenReturn(Optional.of(user));

			UserResponse response = authService.getUser1ById(3L);

			assertThat(response.getId()).isEqualTo(3L);
			assertThat(response.getEmail()).isEqualTo("user1@example.com");
		}

		@Test
		@DisplayName("Non-existent id → ResourceNotFoundException")
		void notFound_throwsException() {
			when(userRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.getUser1ById(99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("User not found");
		}
	}

	// =========================================================================
	// getAllUsers
	// =========================================================================

	@Nested
	@DisplayName("getAllUsers()")
	class GetAllUsers {

		@Test
		@DisplayName("Returns paged UserResponse list")
		void returnsMappedPage() {
			User u1 = buildUser(1L, "a@a.com", "enc", Role.ROLE_USER);
			User u2 = buildUser(2L, "b@b.com", "enc", Role.ROLE_ADMIN);
			Page<User> page = new PageImpl<>(List.of(u1, u2));
			Pageable pageable = PageRequest.of(0, 10);

			when(userRepository.findAll(pageable)).thenReturn(page);

			Page<UserResponse> result = authService.getAllUsers(pageable);

			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getContent().get(0).getEmail()).isEqualTo("a@a.com");
			assertThat(result.getContent().get(1).getRole()).isEqualTo(Role.ROLE_ADMIN);
		}

		@Test
		@DisplayName("Empty repository → returns empty page")
		void emptyRepository_returnsEmptyPage() {
			Page<User> emptyPage = new PageImpl<>(List.of());
			Pageable pageable = PageRequest.of(0, 10);

			when(userRepository.findAll(pageable)).thenReturn(emptyPage);

			Page<UserResponse> result = authService.getAllUsers(pageable);

			assertThat(result.getContent()).isEmpty();
		}
	}

	// =========================================================================
	// updateUserProfile
	// =========================================================================

	@Nested
	@DisplayName("updateUserProfile()")
	class UpdateUserProfile {

		@Test
		@DisplayName("Valid update → user saved with new fields and response returned")
		void validUpdate_returnsUpdatedResponse() {
			User existing = buildUser(1L, "john@example.com", "enc", Role.ROLE_USER);
			UpdateUserRequest request = buildUpdateRequest();
			User updated = User.builder()
					.id(1L).email("john@example.com").password("enc")
					.firstName("Jane").lastName("Smith")
					.city("Surat").state("Gujarat").country("India")
					.streetAddress("456 Park Ave").postalCode("395001")
					.role(Role.ROLE_USER).build();

			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existing));
			when(userRepository.save(any(User.class))).thenReturn(updated);

			UserResponse response = authService.updateUserProfile("john@example.com", request);

			assertThat(response.getFirstname()).isEqualTo("Jane");
			assertThat(response.getLastname()).isEqualTo("Smith");
			assertThat(response.getCity()).isEqualTo("Surat");
		}

		@Test
		@DisplayName("toBuilder preserves id and email after update")
		void toBuilder_preservesIdAndEmail() {
			User existing = buildUser(10L, "john@example.com", "enc", Role.ROLE_USER);
			UpdateUserRequest request = buildUpdateRequest();

			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existing));

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

			authService.updateUserProfile("john@example.com", request);

			assertThat(captor.getValue().getId()).isEqualTo(10L);
			assertThat(captor.getValue().getEmail()).isEqualTo("john@example.com");
			assertThat(captor.getValue().getFirstName()).isEqualTo("Jane");
		}

		@Test
		@DisplayName("User not found → ResourceNotFoundException")
		void notFound_throwsException() {
			when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.updateUserProfile("ghost@example.com", buildUpdateRequest()))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("User not found");
		}
	}

	// =========================================================================
	// deleteUserByEmail
	// =========================================================================

	@Nested
	@DisplayName("deleteUserByEmail()")
	class DeleteUserByEmail {

		@Test
		@DisplayName("Event published successfully → refresh token deleted and user deleted")
		void eventPublished_deletesUser() {
			User user = buildUser(1L, "john@example.com", "enc", Role.ROLE_USER);

			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
			when(userEventPublisher.publishUserDeleted(any(UserDeletedEvent.class))).thenReturn(true);

			authService.deleteUserByEmail("john@example.com");

			verify(refreshTokenService).deleteByUserId(1L);
			verify(userRepository).delete((User) user);
		}

		@Test
		@DisplayName("UserDeletedEvent has correct userId, email, and non-null deletedAt")
		void deletedEvent_hasCorrectFields() {
			User user = buildUser(2L, "delete@example.com", "enc", Role.ROLE_USER);

			when(userRepository.findByEmail("delete@example.com")).thenReturn(Optional.of(user));

			// UserDeletedEvent fields: userId (Long), email (String), deletedAt (LocalDateTime)
			ArgumentCaptor<UserDeletedEvent> eventCaptor =
					ArgumentCaptor.forClass(UserDeletedEvent.class);
			when(userEventPublisher.publishUserDeleted(eventCaptor.capture())).thenReturn(true);

			authService.deleteUserByEmail("delete@example.com");

			UserDeletedEvent event = eventCaptor.getValue();
			assertThat(event.getUserId()).isEqualTo(2L);
			assertThat(event.getEmail()).isEqualTo("delete@example.com");
			assertThat(event.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("Event publish fails → RuntimeException, user NOT deleted")
		void eventPublishFails_throwsException() {
			User user = buildUser(1L, "john@example.com", "enc", Role.ROLE_USER);

			when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
			when(userEventPublisher.publishUserDeleted(any())).thenReturn(false);

			assertThatThrownBy(() -> authService.deleteUserByEmail("john@example.com"))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Failed to publish UserDeleted event");

			verify(userRepository, never()).delete((User) any());
		}

		@Test
		@DisplayName("User not found → ResourceNotFoundException")
		void notFound_throwsException() {
			when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.deleteUserByEmail("ghost@example.com"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("User not found");

			verify(refreshTokenService, never()).deleteByUserId(any());
			verify(userRepository, never()).delete((User) any());
		}
	}

	// =========================================================================
	// changeUserRole
	// =========================================================================

	@Nested
	@DisplayName("changeUserRole()")
	class ChangeUserRole {

		@Test
		@DisplayName("Existing user → role set to ROLE_ADMIN and saved")
		void existingUser_roleChangedToAdmin() {
			User user = buildUser(1L, "user@example.com", "enc", Role.ROLE_USER);
			User saved = buildUser(1L, "user@example.com", "enc", Role.ROLE_ADMIN);

			when(userRepository.findById(1L)).thenReturn(Optional.of(user));
			when(userRepository.save(any(User.class))).thenReturn(saved);

			UserResponse response = authService.changeUserRole(1L, Role.ROLE_ADMIN);

			// Note: service always sets ROLE_ADMIN regardless of passed role param
			assertThat(response.getRole()).isEqualTo(Role.ROLE_ADMIN);
			assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
			verify(userRepository).save(user);
		}

		@Test
		@DisplayName("User not found → ResourceNotFoundException")
		void notFound_throwsException() {
			when(userRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.changeUserRole(99L, Role.ROLE_ADMIN))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("User not found");
		}
	}

	// =========================================================================
	// handleUserFetchRequest
	// =========================================================================

	@Nested
	@DisplayName("handleUserFetchRequest()")
	class HandleUserFetchRequest {

		@Test
		@DisplayName("Sends UserEmailsEvent to userEmails-out-0 with all user emails")
		void sendsUserEmailsEvent() {
			User u1 = buildUser(1L, "a@a.com", "enc", Role.ROLE_USER);
			User u2 = buildUser(2L, "b@b.com", "enc", Role.ROLE_ADMIN);

			when(userRepository.findAll()).thenReturn(List.of(u1, u2));
			when(streamBridge.send(anyString(), any())).thenReturn(true);

			// UserFetchRequestEvent fields: requestId (Long)
			UserFetchRequestEvent request = new UserFetchRequestEvent();
			request.setRequestId(42L);

			authService.handleUserFetchRequest(request);

			// UserEmailsEvent fields: requestId (Long), users (List<UserEmailDto>)
			ArgumentCaptor<UserEmailsEvent> eventCaptor =
					ArgumentCaptor.forClass(UserEmailsEvent.class);
			verify(streamBridge).send(eq("userEmails-out-0"), eventCaptor.capture());

			UserEmailsEvent event = eventCaptor.getValue();
			assertThat(event.getRequestId()).isEqualTo(42L);
			assertThat(event.getUsers()).hasSize(2);
			// UserEmailDto fields: userId (Long), email (String)
			assertThat(event.getUsers().get(0).getUserId()).isEqualTo(1L);
			assertThat(event.getUsers().get(0).getEmail()).isEqualTo("a@a.com");
			assertThat(event.getUsers().get(1).getEmail()).isEqualTo("b@b.com");
		}

		@Test
		@DisplayName("No users in repository → sends event with empty users list")
		void noUsers_sendsEmptyEvent() {
			when(userRepository.findAll()).thenReturn(List.of());
			when(streamBridge.send(anyString(), any())).thenReturn(true);

			UserFetchRequestEvent request = new UserFetchRequestEvent();
			request.setRequestId(99L);

			authService.handleUserFetchRequest(request);

			ArgumentCaptor<UserEmailsEvent> eventCaptor =
					ArgumentCaptor.forClass(UserEmailsEvent.class);
			verify(streamBridge).send(eq("userEmails-out-0"), eventCaptor.capture());

			assertThat(eventCaptor.getValue().getUsers()).isEmpty();
		}
	}

	// =========================================================================
	// searchUsers
	// =========================================================================

	@Nested
	@DisplayName("searchUsers()")
	class SearchUsers {

		@Test
		@DisplayName("Returns paged UserResponse filtered by spec")
		void returnsFilteredPage() {
			User user = buildUser(1L, "search@example.com", "enc", Role.ROLE_USER);
			Page<User> page = new PageImpl<>(List.of(user));
			Pageable pageable = PageRequest.of(0, 10);

			when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

			Page<UserResponse> result = authService.searchUsers(
					"search@example.com", null, null, null,
					null, null, null, null, pageable);

			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).getEmail()).isEqualTo("search@example.com");
		}

		@Test
		@DisplayName("No matching users → returns empty page")
		void noMatches_returnsEmptyPage() {
			Page<User> emptyPage = new PageImpl<>(List.of());
			Pageable pageable = PageRequest.of(0, 10);

			when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

			Page<UserResponse> result = authService.searchUsers(
					null, null, null, null, null, null, null, null, pageable);

			assertThat(result.getContent()).isEmpty();
		}
	}
}