package tech.paymenti7.paymentauthorization.infrastructure.scheduling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tech.paymenti7.paymentauthorization.application.service.AuthorizationOutboxService;

@Component
public class AuthorizationOutboxScheduler {

	private final AuthorizationOutboxService outboxService;
	private final int batchSize;

	public AuthorizationOutboxScheduler(AuthorizationOutboxService outboxService,
			@Value("${payment.authorization.outbox.batch-size}") int batchSize) {
		this.outboxService = outboxService;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${payment.authorization.outbox.polling-delay}")
	public void publish() {
		outboxService.publishPending(batchSize);
	}
}
