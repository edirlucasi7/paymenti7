package tech.paymenti7.paymentauthorization.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.paymentauthorization.infrastructure.messaging.AuthorizationOutboxPublisher;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.AuthorizationOutboxEventJpaRepository;

@Service
public class AuthorizationOutboxService {

	private final AuthorizationOutboxEventJpaRepository outboxRepository;
	private final AuthorizationOutboxPublisher publisher;

	public AuthorizationOutboxService(AuthorizationOutboxEventJpaRepository outboxRepository,
			AuthorizationOutboxPublisher publisher) {
		this.outboxRepository = outboxRepository;
		this.publisher = publisher;
	}

	@Transactional
	public void publishPending(int batchSize) {
		for (var event : outboxRepository.lockPending(batchSize)) {
			publisher.publish(event);
			event.markPublished(Instant.now());
		}
	}
}
