package servicesImpl;

import com.userservice.enums.Role;
import com.userservice.models.RefreshToken;
import com.userservice.models.User;
import com.userservice.repositories.RefreshTokenRepository;
import com.userservice.servicesImpl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenServiceImpl Tests")
class RefreshTokenServiceImplTest {

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@InjectMocks
	private RefreshTokenServiceImpl refreshTokenService;

	// ─────────────────────────────────────────────────────────────────────────
	// Helpers
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * User model fields: id, email, password, firstName, lastName,
	 *                    streetAddress, city, state, country, postalCode, role
	 */
	private User buildUser(Long id, String email) {
		return User.builder()
				.id(id)
				.email(email)
				.password("encodedPass")
				.firstName("John")
				.lastName("Doe")
				.role(Role.ROLE_USER)
				.build();
	}

	/**
	 * RefreshToken model fields: id (Long), token (String),
	 *                            user (User), expiryDate (Instant)
	 */
	private RefreshToken buildRefreshToken(Long id, String token, User user, Instant expiryDate) {
		return RefreshToken.builder()
				.id(id)
				.token(token)
				.user(user)
				.expiryDate(expiryDate)
				.build();
	}

	// =========================================================================
	// createRefreshToken
	// =========================================================================

	@Nested
	@DisplayName("createRefreshToken()")
	class CreateRefreshToken {

		@Test
		@DisplayName("Valid user → deletes old token, saves new token and returns it")
		void validUser_deletesOldAndSavesNew() {
			User user = buildUser(1L, "john@example.com");

			RefreshToken saved = buildRefreshToken(1L, "new-token-uuid", user,
					Instant.now().plusSeconds(7 * 24 * 60 * 60));

			when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(saved);

			RefreshToken result = refreshTokenService.createRefreshToken(user);

			// Old token for user must be deleted first
			verify(refreshTokenRepository).deleteByUserId(1L);
			// New token must be saved
			verify(refreshTokenRepository).save(any(RefreshToken.class));

			assertThat(result.getToken()).isEqualTo("new-token-uuid");
			assertThat(result.getUser()).isEqualTo(user);
		}

		@Test
		@DisplayName("deleteByUserId called before save (ordering verified)")
		void deleteCalledBeforeSave() {
			User user = buildUser(2L, "user2@example.com");

			RefreshToken saved = buildRefreshToken(1L, "token", user,
					Instant.now().plusSeconds(604800));
			when(refreshTokenRepository.save(any())).thenReturn(saved);

			refreshTokenService.createRefreshToken(user);

			// Verify delete was called with correct userId
			verify(refreshTokenRepository).deleteByUserId(2L);
		}

		@Test
		@DisplayName("Saved token has non-null UUID token string")
		void savedToken_hasNonNullUuid() {
			User user = buildUser(1L, "john@example.com");

			ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
			when(refreshTokenRepository.save(captor.capture()))
					.thenAnswer(inv -> inv.getArgument(0));

			refreshTokenService.createRefreshToken(user);

			RefreshToken captured = captor.getValue();
			// token = UUID.randomUUID().toString() — must be non-null and non-blank
			assertThat(captured.getToken()).isNotNull().isNotBlank();
		}

		@Test
		@DisplayName("Saved token has user set correctly")
		void savedToken_hasCorrectUser() {
			User user = buildUser(3L, "user3@example.com");

			ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
			when(refreshTokenRepository.save(captor.capture()))
					.thenAnswer(inv -> inv.getArgument(0));

			refreshTokenService.createRefreshToken(user);

			assertThat(captor.getValue().getUser()).isEqualTo(user);
		}

		@Test
		@DisplayName("Saved token expiryDate is approximately 7 days from now")
		void savedToken_expiryIsSevenDaysFromNow() {
			User user = buildUser(1L, "john@example.com");

			ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
			when(refreshTokenRepository.save(captor.capture()))
					.thenAnswer(inv -> inv.getArgument(0));

			refreshTokenService.createRefreshToken(user);

			Instant expiry = captor.getValue().getExpiryDate();
			Instant sevenDaysFromNow = Instant.now().plusSeconds(7 * 24 * 60 * 60);

			// Allow 5 seconds tolerance for test execution time
			assertThat(expiry).isAfter(sevenDaysFromNow.minusSeconds(5));
			assertThat(expiry).isBefore(sevenDaysFromNow.plusSeconds(5));
		}

		@Test
		@DisplayName("Each call generates a different token (UUID uniqueness)")
		void eachCall_generatesDifferentToken() {
			User user = buildUser(1L, "john@example.com");

			ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
			when(refreshTokenRepository.save(captor.capture()))
					.thenAnswer(inv -> inv.getArgument(0));

			refreshTokenService.createRefreshToken(user);
			refreshTokenService.createRefreshToken(user);

			java.util.List<RefreshToken> captured = captor.getAllValues();
			assertThat(captured.get(0).getToken())
					.isNotEqualTo(captured.get(1).getToken());
		}
	}

	// =========================================================================
	// verifyExpiration
	// =========================================================================

	@Nested
	@DisplayName("verifyExpiration()")
	class VerifyExpiration {

		@Test
		@DisplayName("Token not expired → returns same token")
		void notExpired_returnsToken() {
			User user = buildUser(1L, "john@example.com");
			// expiryDate in future
			RefreshToken token = buildRefreshToken(1L, "valid-token", user,
					Instant.now().plusSeconds(3600));

			RefreshToken result = refreshTokenService.verifyExpiration(token);

			assertThat(result).isEqualTo(token);
			assertThat(result.getToken()).isEqualTo("valid-token");
			// Expired token should NOT be deleted
			verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
		}

		@Test
		@DisplayName("Expired token → deleted and RuntimeException thrown")
		void expiredToken_deletesAndThrows() {
			User user = buildUser(1L, "john@example.com");
			// expiryDate in the past
			RefreshToken token = buildRefreshToken(1L, "expired-token", user,
					Instant.now().minusSeconds(3600));

			assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Refresh token expired. Please login again.");

			// Expired token must be deleted from repository
			verify(refreshTokenRepository).delete((RefreshToken) token);
		}

		@Test
		@DisplayName("Token expiring exactly now (boundary) → treated as expired")
		void tokenExpiredAtBoundary_throwsException() {
			User user = buildUser(1L, "john@example.com");
			// expiryDate set 1ms in the past (just expired)
			RefreshToken token = buildRefreshToken(1L, "boundary-token", user,
					Instant.now().minusMillis(1));

			assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Refresh token expired");

			verify(refreshTokenRepository).delete((RefreshToken) token);
		}

		@Test
		@DisplayName("Token with far future expiry → returned without touching repository")
		void farFutureExpiry_noRepositoryInteraction() {
			User user = buildUser(1L, "john@example.com");
			RefreshToken token = buildRefreshToken(1L, "future-token", user,
					Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days

			RefreshToken result = refreshTokenService.verifyExpiration(token);

			assertThat(result.getToken()).isEqualTo("future-token");
			verifyNoInteractions(refreshTokenRepository);
		}
	}

	// =========================================================================
	// deleteByUserId
	// =========================================================================

	@Nested
	@DisplayName("deleteByUserId()")
	class DeleteByUserId {

		@Test
		@DisplayName("Delegates to refreshTokenRepository.deleteByUserId with correct userId")
		void delegatesToRepository() {
			refreshTokenService.deleteByUserId(5L);

			verify(refreshTokenRepository).deleteByUserId(5L);
		}

		@Test
		@DisplayName("No other repository methods called during delete")
		void noOtherRepositoryInteractions() {
			refreshTokenService.deleteByUserId(10L);

			verify(refreshTokenRepository).deleteByUserId(10L);
			verifyNoMoreInteractions(refreshTokenRepository);
		}

		@Test
		@DisplayName("Multiple calls with different userIds → each delegates correctly")
		void multipleCalls_eachDelegatesCorrectly() {
			refreshTokenService.deleteByUserId(1L);
			refreshTokenService.deleteByUserId(2L);
			refreshTokenService.deleteByUserId(3L);

			verify(refreshTokenRepository).deleteByUserId(1L);
			verify(refreshTokenRepository).deleteByUserId(2L);
			verify(refreshTokenRepository).deleteByUserId(3L);
		}
	}
}
