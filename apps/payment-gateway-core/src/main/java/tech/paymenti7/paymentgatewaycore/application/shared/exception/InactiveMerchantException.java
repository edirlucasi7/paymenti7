package tech.paymenti7.paymentgatewaycore.application.shared.exception;

import java.util.UUID;

public class InactiveMerchantException extends RuntimeException {

	public InactiveMerchantException(UUID merchantId) {
		super("Merchant is not active: " + merchantId);
	}
}
