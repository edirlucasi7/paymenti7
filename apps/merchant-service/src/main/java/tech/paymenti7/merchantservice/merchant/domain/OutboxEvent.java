package tech.paymenti7.merchantservice.merchant.domain;

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
public class OutboxEvent {

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

	protected OutboxEvent() {
	}

	public static OutboxEvent merchantUpdated(UUID merchantId, MerchantStatus status) {
		OutboxEvent event = new OutboxEvent();
		event.id = UUID.randomUUID();
		event.aggregateType = "MERCHANT";
		event.aggregateId = merchantId;
		event.eventType = "MerchantUpdated";
		event.payload = Map.of("merchantId", merchantId.toString(), "status", status.name());
		event.occurredAt = Instant.now();
		return event;
	}
}
