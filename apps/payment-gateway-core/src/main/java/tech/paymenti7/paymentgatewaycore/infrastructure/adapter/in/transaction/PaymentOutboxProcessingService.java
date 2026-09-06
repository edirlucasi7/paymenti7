package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.PaymentOutboxEventJpaRepository;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.publisher.PaymentOutboxEventPublisher;

@Service
public class PaymentOutboxProcessingService {

	private final PaymentOutboxEventJpaRepository outboxEventRepository;
	private final PaymentOutboxEventPublisher outboxEventPublisher;

	public PaymentOutboxProcessingService(PaymentOutboxEventJpaRepository outboxEventRepository,
			PaymentOutboxEventPublisher outboxEventPublisher) {
		this.outboxEventRepository = outboxEventRepository;
		this.outboxEventPublisher = outboxEventPublisher;
	}

	@Transactional
	public void publishPending(int batchSize) {
		for (var event : outboxEventRepository.lockPendingEvents(batchSize)) {
			outboxEventPublisher.publish(event);
			event.markPublished(Instant.now());
		}
	}
}
