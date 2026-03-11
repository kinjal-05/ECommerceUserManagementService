package com.userservice.eventPublish;

import java.util.function.Consumer;
import com.userservice.commondtos.UserFetchRequestEvent;
import com.userservice.services.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserKafkaConfig {

	@Bean
	public Consumer<UserFetchRequestEvent> userFetchConsumer(AuthService userService) {
		return event -> {
			System.out.println("🔥 Received in consume-------------------------------:" );
			userService.handleUserFetchRequest(event);
		};
	}
}