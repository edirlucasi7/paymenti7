package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MerchantUpdatedMessage(int schemaVersion, UUID eventId, String aggregateType, UUID aggregateId,
		String eventType, Instant occurredAt, MerchantUpdatedPayload payload) {

	private static final int SUPPORTED_SCHEMA_VERSION = 1;
	private static final String MERCHANT_AGGREGATE_TYPE = "MERCHANT";
	private static final String MERCHANT_UPDATED_EVENT_TYPE = "MerchantUpdated";
	private static final Set<String> MERCHANT_STATUSES = Set.of("ACTIVE", "INACTIVE");

	public void validate() {
		if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
			throw new InvalidMerchantUpdatedMessageException("Unsupported schema version " + schemaVersion);
		}
		if (eventId == null || aggregateId == null || occurredAt == null || payload == null || payload.merchantId() == null) {
			throw new InvalidMerchantUpdatedMessageException("MerchantUpdated must contain event, aggregate, occurrence and payload identifiers");
		}
		if (!MERCHANT_AGGREGATE_TYPE.equals(aggregateType)) {
			throw new InvalidMerchantUpdatedMessageException("Unexpected aggregate type " + aggregateType);
		}
		if (!MERCHANT_UPDATED_EVENT_TYPE.equals(eventType)) {
			throw new InvalidMerchantUpdatedMessageException("Unexpected event type " + eventType);
		}
		if (!aggregateId.equals(payload.merchantId())) {
			throw new InvalidMerchantUpdatedMessageException("Payload merchantId must match aggregateId");
		}
		if (!MERCHANT_STATUSES.contains(payload.status())) {
			throw new InvalidMerchantUpdatedMessageException("Unexpected merchant status " + payload.status());
		}
	}

	public record MerchantUpdatedPayload(UUID merchantId, String status) {
	}
}
