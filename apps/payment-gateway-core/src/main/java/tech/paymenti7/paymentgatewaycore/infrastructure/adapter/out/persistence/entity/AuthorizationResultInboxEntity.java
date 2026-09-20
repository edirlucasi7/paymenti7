package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity;

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
@Table(name = "authorization_result_inbox")
public class AuthorizationResultInboxEntity {

	@Id
	@Column(name = "event_id")
	private UUID eventId;

	@Column(name = "payment_id", nullable = false)
	private UUID paymentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PaymentStatus status;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;

	protected AuthorizationResultInboxEntity() {
	}
}
