package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;

class MerchantUpdatedMessageTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

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

	@Test
	void rejectsMissingRequiredFieldsThroughBeanValidation() {
		MerchantUpdatedMessage message = new MerchantUpdatedMessage(1, null, "MERCHANT", null, "MerchantUpdated", null,
				new MerchantUpdatedMessage.MerchantUpdatedPayload(null, null));

		assertThat(validator.validate(message)).isNotEmpty();
	}

	private MerchantUpdatedMessage message(UUID eventId, UUID merchantId) {
		return message(eventId, merchantId, merchantId);
	}

	private MerchantUpdatedMessage message(UUID eventId, UUID aggregateId, UUID payloadMerchantId) {
		return new MerchantUpdatedMessage(1, eventId, "MERCHANT", aggregateId, "MerchantUpdated", Instant.now(),
				new MerchantUpdatedMessage.MerchantUpdatedPayload(payloadMerchantId, MerchantStatus.ACTIVE));
	}
}
