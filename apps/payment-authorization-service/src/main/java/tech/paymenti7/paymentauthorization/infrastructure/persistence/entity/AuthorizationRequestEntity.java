package tech.paymenti7.paymentauthorization.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import tech.paymenti7.paymentauthorization.application.domain.AuthorizationStatus;
import tech.paymenti7.paymentauthorization.application.domain.PaymentTerminalStatus;

@Entity
@Table(name = "authorization_requests")
public class AuthorizationRequestEntity {

	@Id
	private UUID id;

	@Column(name = "payment_id", nullable = false, unique = true)
	private UUID paymentId;

	@Column(name = "merchant_id", nullable = false)
	private UUID merchantId;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "payment_method_token", nullable = false, length = 512)
	private String paymentMethodToken;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AuthorizationStatus status;

	@Column(name = "next_route_index", nullable = false)
	private int nextRouteIndex;

	@Enumerated(EnumType.STRING)
	@Column(name = "terminal_status", length = 16)
	private PaymentTerminalStatus terminalStatus;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AuthorizationRequestEntity() {
	}

	public static AuthorizationRequestEntity pending(UUID id, UUID paymentId, UUID merchantId, BigDecimal amount,
			String currency, String paymentMethodToken, Instant now) {
		var entity = new AuthorizationRequestEntity();
		entity.id = id;
		entity.paymentId = paymentId;
		entity.merchantId = merchantId;
		entity.amount = amount;
		entity.currency = currency;
		entity.paymentMethodToken = paymentMethodToken;
		entity.status = AuthorizationStatus.PENDING_ROUTING;
		entity.createdAt = now;
		entity.updatedAt = now;
		return entity;
	}

	public UUID getId() { return id; }
	public UUID getPaymentId() { return paymentId; }
	public UUID getMerchantId() { return merchantId; }
	public BigDecimal getAmount() { return amount; }
	public String getCurrency() { return currency; }
	public String getPaymentMethodToken() { return paymentMethodToken; }
	public AuthorizationStatus getStatus() { return status; }
	public int getNextRouteIndex() { return nextRouteIndex; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void startCall(Instant now) {
		if (status != AuthorizationStatus.PENDING_ROUTING) {
			throw new IllegalStateException("Authorization is not ready for routing");
		}
		status = AuthorizationStatus.CALL_IN_PROGRESS;
		updatedAt = now;
	}

	public void moveToNextRoute(Instant now) {
		status = AuthorizationStatus.PENDING_ROUTING;
		nextRouteIndex++;
		updatedAt = now;
	}

	public void pendingReconciliation(Instant now) {
		status = AuthorizationStatus.PENDING_RECONCILIATION;
		updatedAt = now;
	}

	public void complete(PaymentTerminalStatus terminalStatus, Instant now) {
		status = AuthorizationStatus.COMPLETED;
		this.terminalStatus = terminalStatus;
		updatedAt = now;
	}
}
