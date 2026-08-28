package tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;
import tech.paymenti7.merchantservice.application.core.domain.OutboxEventDeliveryStatus;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

	@Id
	private UUID id;

	@Column(name = "aggregate_type", nullable = false, length = 50)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, String> payload;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_status", nullable = false, length = 16)
	private OutboxEventDeliveryStatus deliveryStatus;

	@Column(name = "superseded_by_event_id")
	private UUID supersededByEventId;

	protected OutboxEventEntity() {
	}

	public static OutboxEventEntity fromDomain(OutboxEvent event) {
		OutboxEventEntity entity = new OutboxEventEntity();
		entity.id = event.id();
		entity.aggregateType = event.aggregateType();
		entity.aggregateId = event.aggregateId();
		entity.eventType = event.eventType();
		entity.payload = event.payload();
		entity.occurredAt = event.occurredAt();
		entity.deliveryStatus = event.deliveryStatus();
		entity.processedAt = event.processedAt();
		entity.supersededByEventId = event.supersededByEventId();
		return entity;
	}

	public OutboxEvent toDomain() {
		return OutboxEvent.rehydrate(id, aggregateType, aggregateId, eventType, payload, occurredAt, deliveryStatus, processedAt,
				supersededByEventId);
	}
}
