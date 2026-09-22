package tech.paymenti7.paymentauthorization.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.paymenti7.paymentauthorization.application.domain.AttemptStatus;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.entity.AuthorizationAttemptEntity;

public interface AuthorizationAttemptJpaRepository extends JpaRepository<AuthorizationAttemptEntity, UUID> {

	Optional<AuthorizationAttemptEntity> findFirstByPaymentIdAndStatusOrderByCreatedAtDesc(UUID paymentId,
			AttemptStatus status);
}
