package tech.paymenti7.merchantservice.application.shared.exception;

import java.util.UUID;

public class MerchantNotFoundException extends RuntimeException {

	public MerchantNotFoundException(UUID merchantId) {
		super("Merchant not found: " + merchantId);
	}
}
