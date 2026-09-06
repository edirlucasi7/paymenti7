package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
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
		idempotencyRequest.complete(command.status(), command.httpStatus(), command.responseBody(), command.responseHeaders(), completedAt,
				completedAt.plus(retention));
	}
}
