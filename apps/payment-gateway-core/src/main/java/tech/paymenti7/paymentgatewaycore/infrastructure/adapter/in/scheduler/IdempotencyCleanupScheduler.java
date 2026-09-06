package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction.IdempotencyCleanupService;

@Component
public class IdempotencyCleanupScheduler {

	private final IdempotencyCleanupService cleanupService;
	private final int batchSize;

	public IdempotencyCleanupScheduler(IdempotencyCleanupService cleanupService,
			@Value("${payment.gateway.idempotency.cleanup-batch-size}") int batchSize) {
		this.cleanupService = cleanupService;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${payment.gateway.idempotency.cleanup-delay}")
	public void cleanup() {
		cleanupService.deleteExpiredTerminalRequests(batchSize);
	}
}
