package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;
import tech.paymenti7.paymentgatewaycore.application.port.in.SubmitPaymentResult;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.IdempotencyConflictException;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity.IdempotencyRequestEntity;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity.PaymentOutboxEventEntity;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.IdempotencyRequestJpaRepository;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.PaymentJpaRepository;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.PaymentOutboxEventJpaRepository;

@Service
public class TransactionalPaymentSubmission {

	private static final String OPERATION = "PAYMENT_CREATE";

	private final IdempotencyRequestJpaRepository idempotencyRequestRepository;
	private final PaymentJpaRepository paymentRepository;
	private final PaymentOutboxEventJpaRepository outboxEventRepository;

	public TransactionalPaymentSubmission(IdempotencyRequestJpaRepository idempotencyRequestRepository,
			PaymentJpaRepository paymentRepository, PaymentOutboxEventJpaRepository outboxEventRepository) {
		this.idempotencyRequestRepository = idempotencyRequestRepository;
		this.paymentRepository = paymentRepository;
		this.outboxEventRepository = outboxEventRepository;
	}

	@Transactional(readOnly = true)
	public Optional<SubmitPaymentResult> findExisting(UUID merchantId, UUID idempotencyKey, String requestHash) {
		return idempotencyRequestRepository
				.findByMerchantIdAndOperationAndIdempotencyKey(merchantId, OPERATION, idempotencyKey)
				.map(existing -> replay(existing, requestHash));
	}

	@Transactional
	public SubmitPaymentResult submit(UUID merchantId, BigDecimal amount, String currency, UUID idempotencyKey,
			String requestHash) {
		Instant now = Instant.now();
		UUID paymentId = UUID.randomUUID();
		int inserted = idempotencyRequestRepository.insertIfAbsent(UUID.randomUUID(), merchantId, OPERATION,
				idempotencyKey, requestHash, paymentId, now);

		if (inserted == 0) {
			return replay(merchantId, idempotencyKey, requestHash);
		}

		paymentRepository.save(PaymentEntity.processing(paymentId, merchantId, amount, currency, now));
		outboxEventRepository.save(PaymentOutboxEventEntity.requested(UUID.randomUUID(), paymentId, merchantId, amount,
				currency, now));
		return processing(paymentId);
	}

	private SubmitPaymentResult replay(UUID merchantId, UUID idempotencyKey, String requestHash) {
		var existing = idempotencyRequestRepository
				.findByMerchantIdAndOperationAndIdempotencyKey(merchantId, OPERATION, idempotencyKey)
				.orElseThrow(() -> new IllegalStateException("Conflicting idempotency request was not found"));
		return replay(existing, requestHash);
	}

	private SubmitPaymentResult replay(IdempotencyRequestEntity existing, String requestHash) {
		if (!existing.getRequestHash().equals(requestHash)) {
			throw new IdempotencyConflictException();
		}
		int httpStatus = existing.getStatus() == PaymentStatus.PROCESSING
				? HttpStatus.ACCEPTED.value()
				: existing.getResponseHttpStatus();
		if (existing.getStatus() == PaymentStatus.PROCESSING) {
			return processing(existing.getPaymentId());
		}
		return new SubmitPaymentResult(existing.getPaymentId(), existing.getStatus(), httpStatus,
				existing.getResponseBody(), existing.getResponseHeaders());
	}

	private SubmitPaymentResult processing(UUID paymentId) {
		return new SubmitPaymentResult(paymentId, PaymentStatus.PROCESSING, HttpStatus.ACCEPTED.value(),
				Map.of("paymentId", paymentId.toString(), "status", PaymentStatus.PROCESSING.name()), Map.of());
	}
}
