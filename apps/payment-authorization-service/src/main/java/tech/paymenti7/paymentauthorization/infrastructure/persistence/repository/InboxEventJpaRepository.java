package tech.paymenti7.paymentauthorization.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.paymenti7.paymentauthorization.infrastructure.persistence.entity.InboxEventEntity;

public interface InboxEventJpaRepository extends JpaRepository<InboxEventEntity, UUID> {

	@Modifying(flushAutomatically = true)
	@Query(value = """
			INSERT INTO inbox_events (event_id, event_type, aggregate_id, received_at)
			VALUES (:eventId, :eventType, :aggregateId, :receivedAt)
			ON CONFLICT (event_id) DO NOTHING
			""", nativeQuery = true)
	int insertIfAbsent(@Param("eventId") UUID eventId, @Param("eventType") String eventType,
			@Param("aggregateId") UUID aggregateId, @Param("receivedAt") Instant receivedAt);
}
