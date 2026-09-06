package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;

@Entity
@Table(name = "payments")
public class PaymentEntity {

	@Id
	private UUID id;

	@Column(name = "merchant_id", nullable = false)
	private UUID merchantId;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PaymentStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PaymentEntity() {
	}

	public static PaymentEntity processing(UUID id, UUID merchantId, BigDecimal amount, String currency, Instant now) {
		var entity = new PaymentEntity();
		entity.id = id;
		entity.merchantId = merchantId;
		entity.amount = amount;
		entity.currency = currency;
		entity.status = PaymentStatus.PROCESSING;
		entity.createdAt = now;
		entity.updatedAt = now;
		return entity;
	}

	public UUID getId() {
		return id;
	}

	public void complete(PaymentStatus terminalStatus, Instant completedAt) {
		if (terminalStatus == PaymentStatus.PROCESSING) {
			throw new IllegalArgumentException("A terminal payment status is required");
		}
		status = terminalStatus;
		updatedAt = completedAt;
	}
}
