package com.userservice.specifications;
import com.userservice.models.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
public class UserSpecification {

	public static Specification<User> searchUsers(
			String email,
			String firstname,
			String lastname,
			String streetAddress,
			String city,
			String state,
			String country,
			String postalCode
	) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			addLike(predicates, cb, root, "email", email);
			addLike(predicates, cb, root, "firstname", firstname);
			addLike(predicates, cb, root, "lastname", lastname);
			addLike(predicates, cb, root, "streetAddress", streetAddress);
			addLike(predicates, cb, root, "city", city);
			addLike(predicates, cb, root, "state", state);
			addLike(predicates, cb, root, "country", country);
			addLike(predicates, cb, root, "postalCode", postalCode);

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static void addLike(
			List<Predicate> predicates,
			jakarta.persistence.criteria.CriteriaBuilder cb,
			jakarta.persistence.criteria.Root<User> root,
			String field,
			String value
	) {
		if (value != null && !value.isBlank()) {
			predicates.add(
					cb.like(
							cb.lower(root.get(field)),
							"%" + value.toLowerCase() + "%"
					)
			);
		}
	}
}
