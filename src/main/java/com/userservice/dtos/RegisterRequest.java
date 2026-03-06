package com.userservice.dtos;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
	@NotBlank(message = "Email is Required")
	@Email(message = "Invalid email format")
	@Size(max = 150)
	@Column(nullable = false)
	private String email;

	@NotBlank(message = "Password is required")
	@Column(nullable = false)
	String password;

	@NotBlank(message = "first name is required")
	@Size(max = 150)
	@Column(nullable = false)
	private String firstname;

	@NotBlank(message = "last name is required")
	@Size(max = 150)
	@Column(nullable = false)
	private String lastname;

	@Size(max = 255)
	private String streetAddress;

	@Size(max = 100)
	private String city;

	@Size(max = 100)
	private String state;

	@Size(max = 100)
	private String country;

	@Size(max = 20)
	private String postalCode;
}
