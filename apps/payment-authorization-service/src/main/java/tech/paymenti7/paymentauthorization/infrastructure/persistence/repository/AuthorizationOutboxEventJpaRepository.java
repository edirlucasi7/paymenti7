package tech.paymenti7.paymentauthorization.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.paymenti7.paymentauthorization.infrastructure.persistence.entity.AuthorizationOutboxEventEntity;

public interface AuthorizationOutboxEventJpaRepository extends JpaRepository<AuthorizationOutboxEventEntity, UUID> {

	@Query(value = """
			SELECT * FROM outbox_events
			WHERE delivery_status = 'PENDING'
			ORDER BY occurred_at, id
			LIMIT :batchSize
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<AuthorizationOutboxEventEntity> lockPending(@Param("batchSize") int batchSize);
}
