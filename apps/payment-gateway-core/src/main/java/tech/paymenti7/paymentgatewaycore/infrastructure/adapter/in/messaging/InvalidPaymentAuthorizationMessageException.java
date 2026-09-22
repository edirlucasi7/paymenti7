package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

public class InvalidPaymentAuthorizationMessageException extends RuntimeException {

	public InvalidPaymentAuthorizationMessageException(String message) {
		super(message);
	}

	public InvalidPaymentAuthorizationMessageException(String message, Throwable cause) {
		super(message, cause);
	}
}
