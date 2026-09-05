package tech.paymenti7.merchantservice.application.core.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class OutboxEvent {

	private final UUID id;
	private final String aggregateType;
	private final UUID aggregateId;
	private final String eventType;
	private final Map<String, Object> payload;
	private final Instant occurredAt;
	private final OutboxEventDeliveryStatus deliveryStatus;
	private final Instant processedAt;
	private final UUID supersededByEventId;

	private OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload,
			Instant occurredAt, OutboxEventDeliveryStatus deliveryStatus, Instant processedAt, UUID supersededByEventId) {
		this.id = id;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
		this.occurredAt = occurredAt;
		this.deliveryStatus = deliveryStatus;
		this.processedAt = processedAt;
		this.supersededByEventId = supersededByEventId;
	}

	public static OutboxEvent merchantUpdated(UUID merchantId, MerchantStatus status, long revision) {
		return new OutboxEvent(
				UUID.randomUUID(),
				"MERCHANT",
				merchantId,
				"MerchantUpdated",
				Map.of("merchantId", merchantId.toString(), "status", status.name(), "revision", revision),
				Instant.now(),
				OutboxEventDeliveryStatus.PENDING,
				null,
				null);
	}

	public static OutboxEvent rehydrate(UUID id, String aggregateType, UUID aggregateId, String eventType,
			Map<String, Object> payload, Instant occurredAt, OutboxEventDeliveryStatus deliveryStatus, Instant processedAt,
			UUID supersededByEventId) {
		return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, occurredAt, deliveryStatus, processedAt,
				supersededByEventId);
	}

	public UUID id() { return id; }

	public String aggregateType() { return aggregateType; }

	public UUID aggregateId() { return aggregateId; }

	public String eventType() { return eventType; }

	public Map<String, Object> payload() { return payload; }

	public Instant occurredAt() { return occurredAt; }

	public OutboxEventDeliveryStatus deliveryStatus() { return deliveryStatus; }

	public Instant processedAt() { return processedAt; }

	public UUID supersededByEventId() { return supersededByEventId; }
}
