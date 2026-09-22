package tech.paymenti7.paymentgatewaycore.application.shared.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

	public PaymentNotFoundException(UUID paymentId) {
		super("Payment not found: " + paymentId);
	}
}
