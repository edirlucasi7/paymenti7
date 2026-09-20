package tech.paymenti7.paymentauthorization.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import tech.paymenti7.paymentauthorization.application.domain.AttemptStatus;
import tech.paymenti7.paymentauthorization.application.port.out.AcquirerAuthorizationPort.AcquirerResult;

@Entity
@Table(name = "authorization_attempts")
public class AuthorizationAttemptEntity {

	@Id
	private UUID id;

	@Column(name = "authorization_id", nullable = false)
	private UUID authorizationId;

	@Column(name = "payment_id", nullable = false)
	private UUID paymentId;

	@Column(nullable = false, length = 64)
	private String acquirer;

	@Column(name = "idempotency_reference", nullable = false, unique = true, length = 128)
	private String idempotencyReference;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AttemptStatus status;

	@Column(name = "provider_reference", length = 256)
	private String providerReference;

	@Column(name = "reason_code", length = 128)
	private String reasonCode;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected AuthorizationAttemptEntity() {
	}

	public static AuthorizationAttemptEntity dispatching(UUID id, AuthorizationRequestEntity authorization,
			String acquirer, Instant now) {
		var entity = new AuthorizationAttemptEntity();
		entity.id = id;
		entity.authorizationId = authorization.getId();
		entity.paymentId = authorization.getPaymentId();
		entity.acquirer = acquirer;
		entity.idempotencyReference = "paymenti7-authorization-" + id;
		entity.status = AttemptStatus.DISPATCHING;
		entity.createdAt = now;
		return entity;
	}

	public UUID getId() { return id; }
	public UUID getAuthorizationId() { return authorizationId; }
	public UUID getPaymentId() { return paymentId; }
	public String getAcquirer() { return acquirer; }
	public String getIdempotencyReference() { return idempotencyReference; }
	public AttemptStatus getStatus() { return status; }
	public String getProviderReference() { return providerReference; }
	public String getReasonCode() { return reasonCode; }

	public void complete(AcquirerResult result, Instant now) {
		status = AttemptStatus.valueOf(result.outcome().name());
		providerReference = result.providerReference();
		reasonCode = result.reasonCode();
		completedAt = now;
	}

	public void markUnknown(String reason, Instant now) {
		status = AttemptStatus.UNKNOWN;
		reasonCode = reason;
		completedAt = now;
	}
}
