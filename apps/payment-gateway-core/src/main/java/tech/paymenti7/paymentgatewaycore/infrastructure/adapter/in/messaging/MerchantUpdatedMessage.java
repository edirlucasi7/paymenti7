package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;

public record MerchantUpdatedMessage(@Min(1) int schemaVersion, @NotNull UUID eventId, @NotBlank String aggregateType,
		@NotNull UUID aggregateId, @NotBlank String eventType, @NotNull Instant occurredAt,
		@NotNull @Valid MerchantUpdatedPayload payload) {

	private static final int SUPPORTED_SCHEMA_VERSION = 1;
	private static final String MERCHANT_AGGREGATE_TYPE = "MERCHANT";
	private static final String MERCHANT_UPDATED_EVENT_TYPE = "MerchantUpdated";
	public void validate() {
		if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
			throw new InvalidMerchantUpdatedMessageException("Unsupported schema version " + schemaVersion);
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
	}

	public record MerchantUpdatedPayload(@NotNull UUID merchantId, @NotNull MerchantStatus status,
			@NotNull @PositiveOrZero Long revision) {
	}
}
