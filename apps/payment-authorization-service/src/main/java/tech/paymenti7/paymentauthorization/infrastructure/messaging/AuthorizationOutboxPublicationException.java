package tech.paymenti7.paymentauthorization.infrastructure.messaging;

public class AuthorizationOutboxPublicationException extends RuntimeException {

	public AuthorizationOutboxPublicationException(String message) {
		super(message);
	}

	public AuthorizationOutboxPublicationException(String message, Throwable cause) {
		super(message, cause);
	}
}
