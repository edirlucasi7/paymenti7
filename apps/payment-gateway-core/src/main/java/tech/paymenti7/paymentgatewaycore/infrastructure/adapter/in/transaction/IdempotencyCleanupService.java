package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.IdempotencyRequestJpaRepository;

@Service
public class IdempotencyCleanupService {

	private final IdempotencyRequestJpaRepository idempotencyRequestRepository;

	public IdempotencyCleanupService(IdempotencyRequestJpaRepository idempotencyRequestRepository) {
		this.idempotencyRequestRepository = idempotencyRequestRepository;
	}

	@Transactional
	public int deleteExpiredTerminalRequests(int batchSize) {
		return idempotencyRequestRepository.deleteExpiredTerminalRequests(Instant.now(), batchSize);
	}
}
