package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

import java.time.Instant;
import java.util.UUID;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;

public record PaymentAuthorizationCompletedMessage(int schemaVersion, UUID eventId, String aggregateType,
		UUID aggregateId, String eventType, Instant occurredAt, Payload payload) {

	public void validate() {
		if (schemaVersion != 1 || !"PAYMENT".equals(aggregateType)
				|| !"PaymentAuthorizationCompleted".equals(eventType) || eventId == null || aggregateId == null
				|| occurredAt == null || payload == null || payload.paymentId() == null || payload.status() == null
				|| !aggregateId.equals(payload.paymentId()) || payload.status() == PaymentStatus.PROCESSING) {
			throw new InvalidPaymentAuthorizationMessageException("Invalid PaymentAuthorizationCompleted envelope");
		}
	}

	public record Payload(UUID paymentId, PaymentStatus status, String acquirer, UUID authorizationAttemptId,
			String providerReference, String reasonCode) {
	}
}
