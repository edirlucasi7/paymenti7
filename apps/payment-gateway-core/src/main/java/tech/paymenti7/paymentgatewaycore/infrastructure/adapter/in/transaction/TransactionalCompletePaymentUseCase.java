package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;
import tech.paymenti7.paymentgatewaycore.application.port.in.CompletePaymentCommand;
import tech.paymenti7.paymentgatewaycore.application.port.in.CompletePaymentUseCase;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.IdempotencyRequestJpaRepository;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.PaymentJpaRepository;

@Service
public class TransactionalCompletePaymentUseCase implements CompletePaymentUseCase {

	private final PaymentJpaRepository paymentRepository;
	private final IdempotencyRequestJpaRepository idempotencyRequestRepository;
	private final Duration retention;

	public TransactionalCompletePaymentUseCase(PaymentJpaRepository paymentRepository,
			IdempotencyRequestJpaRepository idempotencyRequestRepository,
			@Value("${payment.gateway.idempotency.retention}") Duration retention) {
		this.paymentRepository = paymentRepository;
		this.idempotencyRequestRepository = idempotencyRequestRepository;
		this.retention = retention;
	}

	@Override
	@Transactional
	public void complete(CompletePaymentCommand command) {
		if (command.status() == PaymentStatus.PROCESSING) {
			throw new IllegalArgumentException("A terminal payment status is required");
		}
		Instant completedAt = Instant.now();
		var idempotencyRequest = idempotencyRequestRepository.findByPaymentId(command.paymentId())
				.orElseThrow(() -> new IllegalStateException(
						"Idempotency request not found for payment " + command.paymentId()));
		if (idempotencyRequest.getStatus() != PaymentStatus.PROCESSING) {
			if (idempotencyRequest.getStatus() != command.status()) {
				throw new IllegalStateException("Payment already completed with status " + idempotencyRequest.getStatus());
			}
			return;
		}
		var payment = paymentRepository.findById(command.paymentId())
				.orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.paymentId()));
		payment.complete(command.status(), completedAt);
		int httpStatus = command.status() == PaymentStatus.FAILED
				? HttpStatus.BAD_GATEWAY.value()
				: HttpStatus.OK.value();
		var responseBody = Map.<String, Object>of(
				"paymentId", command.paymentId().toString(),
				"status", command.status().name());
		idempotencyRequest.complete(command.status(), httpStatus, responseBody, Map.of(), completedAt,
				completedAt.plus(retention));
	}
}
