package tech.paymenti7.merchantservice.application.core.service;

import java.time.Instant;

import tech.paymenti7.merchantservice.application.port.out.OutboxEventPersistencePort;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPublisherPort;

public class OutboxEventProcessingService {

	private final OutboxEventPersistencePort outboxEventPersistencePort;
	private final OutboxEventPublisherPort outboxEventPublisherPort;

	public OutboxEventProcessingService(OutboxEventPersistencePort outboxEventPersistencePort,
			OutboxEventPublisherPort outboxEventPublisherPort) {
		this.outboxEventPersistencePort = outboxEventPersistencePort;
		this.outboxEventPublisherPort = outboxEventPublisherPort;
	}

	public void processPendingEvents(int batchSize) {
		for (var event : outboxEventPersistencePort.lockLatestPendingEvents(batchSize)) {
			var processedAt = Instant.now();
			outboxEventPersistencePort.suppressOlderPendingEvents(event.aggregateId(), event.id(), processedAt);
			outboxEventPublisherPort.publish(event);
			outboxEventPersistencePort.markPublished(event.id(), processedAt);
		}
	}
}
