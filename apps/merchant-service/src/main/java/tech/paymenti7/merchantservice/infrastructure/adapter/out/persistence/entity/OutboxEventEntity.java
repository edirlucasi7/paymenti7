package tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;

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
		return entity;
	}
}
