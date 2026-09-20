package tech.paymenti7.paymentauthorization.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

import tech.paymenti7.paymentauthorization.application.domain.AuthorizationOutcome;

public interface AcquirerAuthorizationPort {

	String name();

	AcquirerResult authorize(AcquirerCommand command);

	record AcquirerCommand(UUID paymentId, UUID merchantId, BigDecimal amount, String currency,
			String paymentMethodToken, String idempotencyReference) {
	}

	record AcquirerResult(AuthorizationOutcome outcome, String providerReference, String reasonCode) {
	}
}
