package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction.PaymentOutboxProcessingService;

@Component
public class PaymentOutboxScheduler {

	private final PaymentOutboxProcessingService processingService;
	private final int batchSize;

	public PaymentOutboxScheduler(PaymentOutboxProcessingService processingService,
			@Value("${payment.gateway.outbox.batch-size}") int batchSize) {
		this.processingService = processingService;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${payment.gateway.outbox.polling-delay}")
	public void publishPending() {
		processingService.publishPending(batchSize);
	}
}
