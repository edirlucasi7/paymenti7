package tech.paymenti7.paymentauthorization.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import tech.paymenti7.paymentauthorization.application.domain.PaymentTerminalStatus;

@Entity
@Table(name = "outbox_events")
public class AuthorizationOutboxEventEntity {

	@Id
	private UUID id;

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

	protected AuthorizationOutboxEventEntity() {
	}

	public static AuthorizationOutboxEventEntity completed(UUID paymentId, PaymentTerminalStatus status,
			AuthorizationAttemptEntity attempt, Instant now) {
		var entity = new AuthorizationOutboxEventEntity();
		entity.id = UUID.randomUUID();
		entity.aggregateId = paymentId;
		entity.eventType = "PaymentAuthorizationCompleted";
		entity.schemaVersion = 1;
		var payload = new LinkedHashMap<String, Object>();
		payload.put("paymentId", paymentId.toString());
		payload.put("status", status.name());
		if (attempt != null) {
			payload.put("acquirer", attempt.getAcquirer());
			payload.put("authorizationAttemptId", attempt.getId().toString());
			if (attempt.getProviderReference() != null) {
				payload.put("providerReference", attempt.getProviderReference());
			}
		}
		payload.put("reasonCode", attempt != null && attempt.getReasonCode() != null
				? attempt.getReasonCode()
				: status.name());
		entity.payload = Map.copyOf(payload);
		entity.occurredAt = now;
		entity.deliveryStatus = "PENDING";
		return entity;
	}

	public UUID getId() { return id; }
	public UUID getAggregateId() { return aggregateId; }
	public String getEventType() { return eventType; }
	public int getSchemaVersion() { return schemaVersion; }
	public Map<String, Object> getPayload() { return payload; }
	public Instant getOccurredAt() { return occurredAt; }

	public void markPublished(Instant now) {
		publishedAt = now;
		deliveryStatus = "PUBLISHED";
	}
}
