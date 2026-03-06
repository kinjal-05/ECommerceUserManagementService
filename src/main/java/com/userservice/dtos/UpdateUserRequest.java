package com.userservice.dtos;
import jakarta.validation.constraints.NotBlank;
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
public class UpdateUserRequest {
	@NotBlank(message = "First name is required")
	private String firstname;

	@NotBlank(message = "Last name is required")
	private String lastname;

	private String city;
	private String state;
	private String country;
	private String streetAddress;
	private String postalCode;
}