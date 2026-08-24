package tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

	@Query(value = """
			SELECT e.*
			FROM outbox_events e
			WHERE e.delivery_status = 'PENDING'
			  AND e.processed_at IS NULL
			  AND NOT EXISTS (
				  SELECT 1
				  FROM outbox_events newer
				  WHERE newer.aggregate_id = e.aggregate_id
				    AND newer.delivery_status = 'PENDING'
				    AND newer.processed_at IS NULL
				    AND (newer.occurred_at > e.occurred_at
					  OR (newer.occurred_at = e.occurred_at AND newer.id > e.id))
			  )
			ORDER BY e.occurred_at ASC, e.id ASC
			LIMIT :batchSize
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<OutboxEventEntity> lockLatestPendingEvents(@Param("batchSize") int batchSize);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE outbox_events
			SET delivery_status = 'SUPPRESSED',
				superseded_by_event_id = :latestEventId,
				processed_at = :processedAt
			WHERE aggregate_id = :aggregateId
			  AND id <> :latestEventId
			  AND delivery_status = 'PENDING'
			  AND processed_at IS NULL
			""", nativeQuery = true)
	int suppressOlderPendingEvents(@Param("aggregateId") UUID aggregateId, @Param("latestEventId") UUID latestEventId,
			@Param("processedAt") Instant processedAt);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE outbox_events
			SET delivery_status = 'PUBLISHED', processed_at = :processedAt
			WHERE id = :eventId
			  AND delivery_status = 'PENDING'
			  AND processed_at IS NULL
			""", nativeQuery = true)
	int markPublished(@Param("eventId") UUID eventId, @Param("processedAt") Instant processedAt);
}
