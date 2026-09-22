package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class PaymentOutboxEventEntity {

	@Id
	private UUID id;

	@Column(name = "aggregate_type", nullable = false, length = 50)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(name = "schema_version", nullable = false)
	private int schemaVersion;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> payload;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "delivery_status", nullable = false, length = 16)
	private String deliveryStatus;

	protected PaymentOutboxEventEntity() {
	}

	public static PaymentOutboxEventEntity requested(UUID eventId, UUID paymentId, UUID merchantId,
			BigDecimal amount, String currency, String paymentMethodToken, Instant occurredAt) {
		var entity = new PaymentOutboxEventEntity();
		entity.id = eventId;
		entity.aggregateType = "PAYMENT";
		entity.aggregateId = paymentId;
		entity.eventType = "PaymentRequested";
		entity.schemaVersion = 2;
		entity.payload = Map.of(
				"paymentId", paymentId.toString(),
				"merchantId", merchantId.toString(),
				"amount", amount,
				"currency", currency,
				"paymentMethodToken", paymentMethodToken);
		entity.occurredAt = occurredAt;
		entity.deliveryStatus = "PENDING";
		return entity;
	}

	public UUID getId() {
		return id;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public UUID getAggregateId() {
		return aggregateId;
	}

	public String getEventType() {
		return eventType;
	}

	public int getSchemaVersion() {
		return schemaVersion;
	}

	public Map<String, Object> getPayload() {
		return payload;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void markPublished(Instant publishedAt) {
		this.publishedAt = publishedAt;
		this.deliveryStatus = "PUBLISHED";
	}
}
