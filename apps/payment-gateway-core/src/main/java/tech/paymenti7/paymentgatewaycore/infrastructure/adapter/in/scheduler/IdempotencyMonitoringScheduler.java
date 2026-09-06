package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.scheduler;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.IdempotencyRequestJpaRepository;

@Component
public class IdempotencyMonitoringScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotencyMonitoringScheduler.class);

	private final IdempotencyRequestJpaRepository idempotencyRequestRepository;
	private final Duration staleAfter;

	public IdempotencyMonitoringScheduler(IdempotencyRequestJpaRepository idempotencyRequestRepository,
			@Value("${payment.gateway.idempotency.processing-stale-after}") Duration staleAfter) {
		this.idempotencyRequestRepository = idempotencyRequestRepository;
		this.staleAfter = staleAfter;
	}

	@Scheduled(fixedDelayString = "${payment.gateway.idempotency.monitoring-delay}")
	public void reportStaleProcessingRequests() {
		Instant threshold = Instant.now().minus(staleAfter);
		long staleRequests = idempotencyRequestRepository.countByStatusAndCreatedAtBefore(PaymentStatus.PROCESSING, threshold);
		if (staleRequests > 0) {
			LOGGER.warn("Payment idempotency requests require reconciliation: count={}, olderThan={}", staleRequests,
					threshold);
		}
	}
}
