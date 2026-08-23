package tech.paymenti7.merchantservice.application.core.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class OutboxEvent {

	private final UUID id;
	private final String aggregateType;
	private final UUID aggregateId;
	private final String eventType;
	private final Map<String, String> payload;
	private final Instant occurredAt;

	private OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, Map<String, String> payload,
			Instant occurredAt) {
		this.id = id;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
		this.occurredAt = occurredAt;
	}

	public static OutboxEvent merchantUpdated(UUID merchantId, MerchantStatus status) {
		return new OutboxEvent(
				UUID.randomUUID(),
				"MERCHANT",
				merchantId,
				"MerchantUpdated",
				Map.of("merchantId", merchantId.toString(), "status", status.name()),
				Instant.now());
	}

	public UUID id() { return id; }

	public String aggregateType() { return aggregateType; }

	public UUID aggregateId() { return aggregateId; }

	public String eventType() { return eventType; }

	public Map<String, String> payload() { return payload; }

	public Instant occurredAt() { return occurredAt; }
}
