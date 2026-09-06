package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity.IdempotencyRequestEntity;

public interface IdempotencyRequestJpaRepository extends JpaRepository<IdempotencyRequestEntity, UUID> {

	@Modifying(flushAutomatically = true)
	@Query(value = """
			INSERT INTO idempotency_requests (
				id, merchant_id, operation, idempotency_key, request_hash,
				status, payment_id, created_at
			) VALUES (
				:id, :merchantId, :operation, :idempotencyKey, :requestHash,
				'PROCESSING', :paymentId, :createdAt
			)
			ON CONFLICT (merchant_id, operation, idempotency_key) DO NOTHING
			""", nativeQuery = true)
	int insertIfAbsent(@Param("id") UUID id, @Param("merchantId") UUID merchantId,
			@Param("operation") String operation, @Param("idempotencyKey") UUID idempotencyKey,
			@Param("requestHash") String requestHash, @Param("paymentId") UUID paymentId,
			@Param("createdAt") Instant createdAt);

	Optional<IdempotencyRequestEntity> findByMerchantIdAndOperationAndIdempotencyKey(UUID merchantId, String operation,
			UUID idempotencyKey);

	Optional<IdempotencyRequestEntity> findByPaymentId(UUID paymentId);

	long countByStatusAndCreatedAtBefore(PaymentStatus status, Instant createdBefore);

	@Modifying
	@Query(value = """
			WITH expired AS (
				SELECT id
				FROM idempotency_requests
				WHERE status <> 'PROCESSING'
					AND expires_at < :now
				ORDER BY expires_at
				LIMIT :batchSize
				FOR UPDATE SKIP LOCKED
			)
			DELETE FROM idempotency_requests request
			USING expired
			WHERE request.id = expired.id
			""", nativeQuery = true)
	int deleteExpiredTerminalRequests(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
