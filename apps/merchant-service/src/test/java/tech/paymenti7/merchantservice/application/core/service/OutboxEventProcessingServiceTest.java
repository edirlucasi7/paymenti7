package tech.paymenti7.merchantservice.application.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;
import tech.paymenti7.merchantservice.application.core.domain.OutboxEventDeliveryStatus;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPersistencePort;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPublisherPort;

class OutboxEventProcessingServiceTest {

	@Test
	void publishesOnlyTheLatestEventSelectedForEachMerchant() {
		var merchantId = UUID.randomUUID();
		var latestEvent = pendingEvent(merchantId, Instant.parse("2026-08-24T10:00:00Z"));
		var persistence = new RecordingOutboxPersistence(List.of(latestEvent));
		var publisher = new RecordingPublisher();
		var service = new OutboxEventProcessingService(persistence, publisher);

		service.processPendingEvents(20);

		assertEquals(20, persistence.requestedBatchSize);
		assertEquals(List.of(latestEvent.id()), publisher.publishedEventIds);
		assertEquals(List.of(new Suppression(merchantId, latestEvent.id())), persistence.suppressions);
		assertEquals(List.of(latestEvent.id()), persistence.publishedEventIds);
	}

	@Test
	void processesEachLatestEventForDifferentMerchants() {
		var first = pendingEvent(UUID.randomUUID(), Instant.parse("2026-08-24T10:00:00Z"));
		var second = pendingEvent(UUID.randomUUID(), Instant.parse("2026-08-24T10:01:00Z"));
		var persistence = new RecordingOutboxPersistence(List.of(first, second));
		var publisher = new RecordingPublisher();

		new OutboxEventProcessingService(persistence, publisher).processPendingEvents(20);

		assertEquals(List.of(first.id(), second.id()), publisher.publishedEventIds);
		assertEquals(List.of(first.id(), second.id()), persistence.publishedEventIds);
	}

	@Test
	void doesNotMarkLatestEventAsPublishedWhenPublishingFails() {
		var event = pendingEvent(UUID.randomUUID(), Instant.parse("2026-08-24T10:00:00Z"));
		var persistence = new RecordingOutboxPersistence(List.of(event));
		var publisher = new FailingPublisher();
		var service = new OutboxEventProcessingService(persistence, publisher);

		assertThrows(IllegalStateException.class, () -> service.processPendingEvents(20));

		assertEquals(List.of(), persistence.publishedEventIds);
		assertEquals(List.of(new Suppression(event.aggregateId(), event.id())), persistence.suppressions);
	}

	private OutboxEvent pendingEvent(UUID merchantId, Instant occurredAt) {
		return OutboxEvent.rehydrate(UUID.randomUUID(), "MERCHANT", merchantId, "MerchantUpdated",
				Map.of("merchantId", merchantId.toString(), "status", "ACTIVE"), occurredAt,
				OutboxEventDeliveryStatus.PENDING, null, null);
	}

	private static final class RecordingOutboxPersistence implements OutboxEventPersistencePort {

		private final List<OutboxEvent> lockedEvents;
		private final List<Suppression> suppressions = new ArrayList<>();
		private final List<UUID> publishedEventIds = new ArrayList<>();
		private int requestedBatchSize;

		private RecordingOutboxPersistence(List<OutboxEvent> lockedEvents) {
			this.lockedEvents = lockedEvents;
		}

		@Override
		public void save(OutboxEvent event) {
		}

		@Override
		public List<OutboxEvent> lockLatestPendingEvents(int batchSize) {
			requestedBatchSize = batchSize;
			return lockedEvents;
		}

		@Override
		public void suppressOlderPendingEvents(UUID aggregateId, UUID latestEventId, Instant processedAt) {
			suppressions.add(new Suppression(aggregateId, latestEventId));
		}

		@Override
		public void markPublished(UUID eventId, Instant processedAt) {
			publishedEventIds.add(eventId);
		}
	}

	private static final class RecordingPublisher implements OutboxEventPublisherPort {

		private final List<UUID> publishedEventIds = new ArrayList<>();

		@Override
		public void publish(OutboxEvent event) {
			publishedEventIds.add(event.id());
		}
	}

	private static final class FailingPublisher implements OutboxEventPublisherPort {

		@Override
		public void publish(OutboxEvent event) {
			throw new IllegalStateException("publisher unavailable");
		}
	}

	private record Suppression(UUID aggregateId, UUID latestEventId) {
	}
}
