package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity.AuthorizationResultInboxEntity;

public interface AuthorizationResultInboxJpaRepository extends JpaRepository<AuthorizationResultInboxEntity, UUID> {

	@Modifying(flushAutomatically = true)
	@Query(value = """
			INSERT INTO authorization_result_inbox (event_id, payment_id, status, processed_at)
			VALUES (:eventId, :paymentId, :status, :processedAt)
			ON CONFLICT (event_id) DO NOTHING
			""", nativeQuery = true)
	int insertIfAbsent(@Param("eventId") UUID eventId, @Param("paymentId") UUID paymentId,
			@Param("status") String status, @Param("processedAt") Instant processedAt);
}
