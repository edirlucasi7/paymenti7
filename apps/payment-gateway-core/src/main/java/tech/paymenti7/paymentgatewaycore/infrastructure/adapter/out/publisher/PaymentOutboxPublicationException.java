package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.publisher;

public class PaymentOutboxPublicationException extends RuntimeException {

	public PaymentOutboxPublicationException(String message) {
		super(message);
	}

	public PaymentOutboxPublicationException(String message, Throwable cause) {
		super(message, cause);
	}
}
