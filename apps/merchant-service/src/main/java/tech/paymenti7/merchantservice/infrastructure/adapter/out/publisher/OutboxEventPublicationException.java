package tech.paymenti7.merchantservice.infrastructure.adapter.out.publisher;

public class OutboxEventPublicationException extends RuntimeException {

	public OutboxEventPublicationException(String message) {
		super(message);
	}

	public OutboxEventPublicationException(String message, Throwable cause) {
		super(message, cause);
	}
}
