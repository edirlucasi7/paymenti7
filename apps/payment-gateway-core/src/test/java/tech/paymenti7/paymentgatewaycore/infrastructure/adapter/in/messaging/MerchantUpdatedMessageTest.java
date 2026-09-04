package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MerchantUpdatedMessageTest {

	@Test
	void acceptsThePublishedMerchantUpdatedV1Contract() {
		MerchantUpdatedMessage message = message(UUID.randomUUID(), UUID.randomUUID());

		assertDoesNotThrow(message::validate);
	}

	@Test
	void rejectsPayloadForAnotherMerchant() {
		MerchantUpdatedMessage message = message(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThrows(InvalidMerchantUpdatedMessageException.class, message::validate);
	}

	private MerchantUpdatedMessage message(UUID eventId, UUID merchantId) {
		return message(eventId, merchantId, merchantId);
	}

	private MerchantUpdatedMessage message(UUID eventId, UUID aggregateId, UUID payloadMerchantId) {
		return new MerchantUpdatedMessage(1, eventId, "MERCHANT", aggregateId, "MerchantUpdated", Instant.now(),
				new MerchantUpdatedMessage.MerchantUpdatedPayload(payloadMerchantId, "ACTIVE"));
	}
}
