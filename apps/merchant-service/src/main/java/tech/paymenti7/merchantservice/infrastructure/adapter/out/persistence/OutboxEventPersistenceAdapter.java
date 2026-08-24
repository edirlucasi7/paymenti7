package tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPersistencePort;
import tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.repository.OutboxEventJpaRepository;

@Component
public class OutboxEventPersistenceAdapter implements OutboxEventPersistencePort {

	private final OutboxEventJpaRepository outboxEventJpaRepository;

	public OutboxEventPersistenceAdapter(OutboxEventJpaRepository outboxEventJpaRepository) {
		this.outboxEventJpaRepository = outboxEventJpaRepository;
	}

	@Override
	public void save(OutboxEvent event) {
		outboxEventJpaRepository.save(OutboxEventEntity.fromDomain(event));
	}

	@Override
	public List<OutboxEvent> lockLatestPendingEvents(int batchSize) {
		return outboxEventJpaRepository.lockLatestPendingEvents(batchSize).stream()
				.map(OutboxEventEntity::toDomain)
				.toList();
	}

	@Override
	public void suppressOlderPendingEvents(UUID aggregateId, UUID latestEventId, Instant processedAt) {
		outboxEventJpaRepository.suppressOlderPendingEvents(aggregateId, latestEventId, processedAt);
	}

	@Override
	public void markPublished(UUID eventId, Instant processedAt) {
		outboxEventJpaRepository.markPublished(eventId, processedAt);
	}
}
