package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.paymentgatewaycore.application.port.in.CompletePaymentCommand;
import tech.paymenti7.paymentgatewaycore.application.port.in.CompletePaymentUseCase;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging.PaymentAuthorizationCompletedMessage;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.AuthorizationResultInboxJpaRepository;

@Service
public class TransactionalAuthorizationResultProcessor {

	private final AuthorizationResultInboxJpaRepository inboxRepository;
	private final CompletePaymentUseCase completePaymentUseCase;

	public TransactionalAuthorizationResultProcessor(AuthorizationResultInboxJpaRepository inboxRepository,
			CompletePaymentUseCase completePaymentUseCase) {
		this.inboxRepository = inboxRepository;
		this.completePaymentUseCase = completePaymentUseCase;
	}

	@Transactional
	public boolean process(PaymentAuthorizationCompletedMessage message) {
		int inserted = inboxRepository.insertIfAbsent(message.eventId(), message.payload().paymentId(),
				message.payload().status().name(), Instant.now());
		if (inserted == 0) {
			return false;
		}
		completePaymentUseCase.complete(new CompletePaymentCommand(message.payload().paymentId(),
				message.payload().status()));
		return true;
	}
}
