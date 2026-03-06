package com.userservice.security;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long expirationMs;

	public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {

		// ✅ Always use UTF-8 (safe + simple)
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	// ✅ Generate Token
	public String generateToken(CustomUserDetails userDetails, List<String> roles) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);

		return Jwts.builder().setSubject(userDetails.getUsername()).claim("roles", roles) // ✅ custom claim
				.claim("userId", userDetails.getUser().getId()).setIssuedAt(now).setExpiration(expiry)
				.signWith(signingKey, SignatureAlgorithm.HS256).compact();
	}

	// ✅ Extract username
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	// ✅ Extract expiration
	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	// ✅ Generic claim extractor
	public <T> T extractClaim(String token, Function<Claims, T> resolver) {
		return resolver.apply(extractAllClaims(token));
	}

	// ✅ Validate token
	public boolean isTokenValid(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return Objects.equals(username, userDetails.getUsername()) && !isTokenExpired(token);
	}

	// ✅ Check expiration
	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	// ✅ Parse token
	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
	}
}