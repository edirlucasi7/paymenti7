package tech.paymenti7.paymentgatewaycore.application.core.domain;

import java.util.UUID;

public record MerchantDetails(UUID id, MerchantStatus status, Long revision) {

	public MerchantDetails {
		if (revision == null || revision < 0) {
			throw new IllegalArgumentException("Merchant revision must be non-negative");
		}
	}
}
