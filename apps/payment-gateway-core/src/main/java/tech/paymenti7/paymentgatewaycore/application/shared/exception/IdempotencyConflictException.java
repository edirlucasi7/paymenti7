package tech.paymenti7.paymentgatewaycore.application.shared.exception;

public class IdempotencyConflictException extends RuntimeException {

	public IdempotencyConflictException() {
		super("Idempotency-Key was already used with a different payment request");
	}
}
