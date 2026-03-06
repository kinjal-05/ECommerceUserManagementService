package com.userservice.dtos;
import com.userservice.enums.Role;
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
public class UserResponse {
	private Long id;

	private String email;

	private String firstname;

	private String lastname;

	private String streetAddress;

	private String city;

	private String state;

	private String country;

	private String postalCode;

	private Role role;
}
