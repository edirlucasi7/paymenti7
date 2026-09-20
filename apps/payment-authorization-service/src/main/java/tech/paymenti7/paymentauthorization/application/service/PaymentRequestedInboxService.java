package tech.paymenti7.paymentauthorization.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.paymentauthorization.infrastructure.messaging.PaymentRequestedMessage;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.entity.AuthorizationRequestEntity;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.AuthorizationRequestJpaRepository;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.InboxEventJpaRepository;

@Service
public class PaymentRequestedInboxService {

	private final InboxEventJpaRepository inboxRepository;
	private final AuthorizationRequestJpaRepository authorizationRepository;

	public PaymentRequestedInboxService(InboxEventJpaRepository inboxRepository,
			AuthorizationRequestJpaRepository authorizationRepository) {
		this.inboxRepository = inboxRepository;
		this.authorizationRepository = authorizationRepository;
	}

	@Transactional
	public boolean accept(PaymentRequestedMessage message) {
		Instant now = Instant.now();
		int inserted = inboxRepository.insertIfAbsent(message.eventId(), message.eventType(), message.aggregateId(), now);
		if (inserted == 0) {
			return false;
		}
		if (authorizationRepository.findByPaymentId(message.payload().paymentId()).isEmpty()) {
			authorizationRepository.save(AuthorizationRequestEntity.pending(UUID.randomUUID(),
					message.payload().paymentId(), message.payload().merchantId(), message.payload().amount(),
					message.payload().currency(), message.payload().paymentMethodToken(), now));
		}
		return true;
	}
}
