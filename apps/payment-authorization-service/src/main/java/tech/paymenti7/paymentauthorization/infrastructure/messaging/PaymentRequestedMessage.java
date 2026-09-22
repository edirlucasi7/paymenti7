package tech.paymenti7.paymentauthorization.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequestedMessage(int schemaVersion, UUID eventId, String aggregateType, UUID aggregateId,
		String eventType, Instant occurredAt, Payload payload) {

	public void validate() {
		if (schemaVersion != 2 || eventId == null || !"PAYMENT".equals(aggregateType) || aggregateId == null
				|| !"PaymentRequested".equals(eventType) || occurredAt == null || payload == null
				|| payload.paymentId() == null || !aggregateId.equals(payload.paymentId()) || payload.merchantId() == null
				|| payload.amount() == null || payload.amount().signum() <= 0 || payload.currency() == null
				|| !payload.currency().matches("[A-Z]{3}") || payload.paymentMethodToken() == null
				|| payload.paymentMethodToken().isBlank() || payload.paymentMethodToken().length() > 512) {
			throw new InvalidPaymentRequestedMessageException("Invalid PaymentRequested v2 envelope");
		}
	}

	public record Payload(UUID paymentId, UUID merchantId, BigDecimal amount, String currency,
			String paymentMethodToken) {
	}
}
