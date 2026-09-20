package tech.paymenti7.paymentauthorization.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.paymenti7.paymentauthorization.application.domain.AuthorizationStatus;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.entity.AuthorizationRequestEntity;

public interface AuthorizationRequestJpaRepository extends JpaRepository<AuthorizationRequestEntity, UUID> {

	Optional<AuthorizationRequestEntity> findByPaymentId(UUID paymentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from AuthorizationRequestEntity request where request.paymentId = :paymentId")
	Optional<AuthorizationRequestEntity> findByPaymentIdForUpdate(@Param("paymentId") UUID paymentId);

	@Query("select request.paymentId from AuthorizationRequestEntity request "
			+ "where request.status = :status order by request.createdAt")
	List<UUID> findPaymentIdsByStatus(@Param("status") AuthorizationStatus status, Pageable pageable);

	@Query("select request.paymentId from AuthorizationRequestEntity request "
			+ "where request.status = 'CALL_IN_PROGRESS' and request.updatedAt < :staleBefore")
	List<UUID> findStaleCalls(@Param("staleBefore") Instant staleBefore, Pageable pageable);
}
