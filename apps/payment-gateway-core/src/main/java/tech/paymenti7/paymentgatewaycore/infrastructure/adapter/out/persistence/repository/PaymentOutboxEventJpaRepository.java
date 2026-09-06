package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity.PaymentOutboxEventEntity;

public interface PaymentOutboxEventJpaRepository extends JpaRepository<PaymentOutboxEventEntity, UUID> {

	@Query(value = """
			SELECT event.*
			FROM outbox_events event
			WHERE event.delivery_status = 'PENDING'
			ORDER BY event.occurred_at, event.id
			LIMIT :batchSize
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<PaymentOutboxEventEntity> lockPendingEvents(int batchSize);
}
