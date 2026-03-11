package com.userservice.models;

import com.userservice.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Email is Required")
	@Email(message = "Invalid Email Format")
	@Size(max = 150)
	@Column(nullable = false)
	private String email;

	@NotBlank(message = "Password is Required")
	@Column(nullable = false)
	String password;

	@NotBlank(message = "First Name is Required")
	@Size(max = 150)
	@Column(nullable = false)
	private String firstName;

	@NotBlank(message = "Last Name is Required")
	@Size(max = 150)
	@Column(nullable = false)
	private String lastName;

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

	@Enumerated(EnumType.STRING)
	private Role role;

}
