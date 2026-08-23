package tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence;

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
}
