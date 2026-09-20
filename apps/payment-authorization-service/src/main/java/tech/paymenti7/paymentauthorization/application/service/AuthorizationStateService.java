package tech.paymenti7.paymentauthorization.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.paymentauthorization.application.domain.AttemptStatus;
import tech.paymenti7.paymentauthorization.application.domain.AuthorizationOutcome;
import tech.paymenti7.paymentauthorization.application.domain.AuthorizationStatus;
import tech.paymenti7.paymentauthorization.application.domain.PaymentTerminalStatus;
import tech.paymenti7.paymentauthorization.application.port.out.AcquirerAuthorizationPort.AcquirerCommand;
import tech.paymenti7.paymentauthorization.application.port.out.AcquirerAuthorizationPort.AcquirerResult;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.entity.AuthorizationAttemptEntity;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.entity.AuthorizationOutboxEventEntity;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.AuthorizationAttemptJpaRepository;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.AuthorizationOutboxEventJpaRepository;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.AuthorizationRequestJpaRepository;

@Service
public class AuthorizationStateService {

	private final AuthorizationRequestJpaRepository authorizationRepository;
	private final AuthorizationAttemptJpaRepository attemptRepository;
	private final AuthorizationOutboxEventJpaRepository outboxRepository;

	public AuthorizationStateService(AuthorizationRequestJpaRepository authorizationRepository,
			AuthorizationAttemptJpaRepository attemptRepository,
			AuthorizationOutboxEventJpaRepository outboxRepository) {
		this.authorizationRepository = authorizationRepository;
		this.attemptRepository = attemptRepository;
		this.outboxRepository = outboxRepository;
	}

	@Transactional(readOnly = true)
	public List<UUID> pendingPaymentIds(int batchSize) {
		return authorizationRepository.findPaymentIdsByStatus(AuthorizationStatus.PENDING_ROUTING,
				PageRequest.of(0, batchSize));
	}

	@Transactional(readOnly = true)
	public Optional<PendingRoute> pendingRoute(UUID paymentId) {
		return authorizationRepository.findByPaymentId(paymentId)
				.filter(request -> request.getStatus() == AuthorizationStatus.PENDING_ROUTING)
				.map(request -> new PendingRoute(request.getPaymentId(), request.getNextRouteIndex()));
	}

	@Transactional
	public Optional<PreparedAttempt> prepareAttempt(UUID paymentId, int expectedRouteIndex, String acquirer) {
		var authorization = authorizationRepository.findByPaymentIdForUpdate(paymentId).orElseThrow();
		if (authorization.getStatus() != AuthorizationStatus.PENDING_ROUTING
				|| authorization.getNextRouteIndex() != expectedRouteIndex) {
			return Optional.empty();
		}
		Instant now = Instant.now();
		var attempt = AuthorizationAttemptEntity.dispatching(UUID.randomUUID(), authorization, acquirer, now);
		attemptRepository.save(attempt);
		authorization.startCall(now);
		return Optional.of(new PreparedAttempt(attempt.getId(), new AcquirerCommand(authorization.getPaymentId(),
				authorization.getMerchantId(), authorization.getAmount(), authorization.getCurrency(),
				authorization.getPaymentMethodToken(), attempt.getIdempotencyReference())));
	}

	@Transactional
	public void skipSafeRoute(UUID paymentId, int expectedRouteIndex, int routeCount) {
		var authorization = authorizationRepository.findByPaymentIdForUpdate(paymentId).orElseThrow();
		if (authorization.getStatus() != AuthorizationStatus.PENDING_ROUTING
				|| authorization.getNextRouteIndex() != expectedRouteIndex) {
			return;
		}
		Instant now = Instant.now();
		if (expectedRouteIndex + 1 >= routeCount) {
			complete(authorization, PaymentTerminalStatus.FAILED, null, now);
		}
		else {
			authorization.moveToNextRoute(now);
		}
	}

	@Transactional
	public void recordResult(UUID paymentId, UUID attemptId, AcquirerResult result, int routeCount) {
		var authorization = authorizationRepository.findByPaymentIdForUpdate(paymentId).orElseThrow();
		var attempt = attemptRepository.findById(attemptId).orElseThrow();
		if (authorization.getStatus() != AuthorizationStatus.CALL_IN_PROGRESS
				|| attempt.getStatus() != AttemptStatus.DISPATCHING) {
			return;
		}
		Instant now = Instant.now();
		attempt.complete(result, now);
		if (result.outcome() == AuthorizationOutcome.APPROVED) {
			complete(authorization, PaymentTerminalStatus.APPROVED, attempt, now);
		}
		else if (result.outcome() == AuthorizationOutcome.DECLINED) {
			complete(authorization, PaymentTerminalStatus.DECLINED, attempt, now);
		}
		else if (result.outcome() == AuthorizationOutcome.UNKNOWN) {
			authorization.pendingReconciliation(now);
		}
		else if (authorization.getNextRouteIndex() + 1 >= routeCount) {
			complete(authorization, PaymentTerminalStatus.FAILED, attempt, now);
		}
		else {
			authorization.moveToNextRoute(now);
		}
	}

	@Transactional(readOnly = true)
	public List<UUID> staleCalls(Instant staleBefore, int batchSize) {
		return authorizationRepository.findStaleCalls(staleBefore, PageRequest.of(0, batchSize));
	}

	@Transactional
	public void markStaleCallUnknown(UUID paymentId) {
		var authorization = authorizationRepository.findByPaymentIdForUpdate(paymentId).orElseThrow();
		if (authorization.getStatus() != AuthorizationStatus.CALL_IN_PROGRESS) {
			return;
		}
		Instant now = Instant.now();
		attemptRepository.findFirstByPaymentIdAndStatusOrderByCreatedAtDesc(paymentId, AttemptStatus.DISPATCHING)
				.ifPresent(attempt -> attempt.markUnknown("WORKER_INTERRUPTED_AFTER_DISPATCH", now));
		authorization.pendingReconciliation(now);
	}

	private void complete(tech.paymenti7.paymentauthorization.infrastructure.persistence.entity.AuthorizationRequestEntity authorization,
			PaymentTerminalStatus status, AuthorizationAttemptEntity attempt, Instant now) {
		authorization.complete(status, now);
		outboxRepository.save(AuthorizationOutboxEventEntity.completed(authorization.getPaymentId(), status, attempt, now));
	}

	public record PendingRoute(UUID paymentId, int routeIndex) {
	}

	public record PreparedAttempt(UUID attemptId, AcquirerCommand command) {
	}
}
