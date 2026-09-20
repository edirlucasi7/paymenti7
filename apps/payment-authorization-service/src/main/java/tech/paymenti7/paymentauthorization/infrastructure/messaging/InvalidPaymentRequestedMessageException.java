package tech.paymenti7.paymentauthorization.infrastructure.messaging;

public class InvalidPaymentRequestedMessageException extends RuntimeException {

	public InvalidPaymentRequestedMessageException(String message) {
		super(message);
	}

	public InvalidPaymentRequestedMessageException(String message, Throwable cause) {
		super(message, cause);
	}
}
