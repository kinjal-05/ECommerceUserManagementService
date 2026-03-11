package com.userservice.eventPublish;

import com.userservice.commondtos.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

	private final StreamBridge streamBridge;

	public boolean publishUserDeleted(UserDeletedEvent event) {

		boolean orderEvent = streamBridge.send("userDeleted-out-0", event);
		boolean notificationEvent = streamBridge.send("userDeletedNotification-out-0", event);
		boolean inventoryEvent = streamBridge.send("userDeletedInventory-out-0", event);

		boolean allPublished = orderEvent && notificationEvent && inventoryEvent;

		if (allPublished) {
			log.info("✅ User deleted events published for userId: {}", event.getUserId());
		} else {
			log.error("❌ Failed to publish some user deleted events for userId: {}", event.getUserId());
		}

		return allPublished;
	}
}