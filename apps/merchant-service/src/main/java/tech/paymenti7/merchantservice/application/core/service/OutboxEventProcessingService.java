package tech.paymenti7.merchantservice.application.core.service;

import tech.paymenti7.merchantservice.application.port.out.OutboxEventPersistencePort;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPublisherPort;

import java.time.Instant;

public class OutboxEventProcessingService {

	private final OutboxEventPersistencePort outboxEventPersistencePort;
	private final OutboxEventPublisherPort outboxEventPublisherPort;

	public OutboxEventProcessingService(OutboxEventPersistencePort outboxEventPersistencePort,
			OutboxEventPublisherPort outboxEventPublisherPort) {
		this.outboxEventPersistencePort = outboxEventPersistencePort;
		this.outboxEventPublisherPort = outboxEventPublisherPort;
	}

	// TODO aqui ainda acredito que dê para ficar mais simples, mas quando tiver integrado com rabbitmq, revemos.
	public void processPendingEvents(int batchSize) {
		for (var event : outboxEventPersistencePort.lockLatestPendingEvents(batchSize)) {
			var processedAt = Instant.now();
			outboxEventPersistencePort.suppressOlderPendingEvents(event.aggregateId(), event.id(), event.occurredAt(), processedAt);
			outboxEventPublisherPort.publish(event);
			outboxEventPersistencePort.markPublished(event.id(), processedAt);
		}
	}
}
