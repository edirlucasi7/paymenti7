package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

public class InvalidMerchantUpdatedMessageException extends RuntimeException {

	public InvalidMerchantUpdatedMessageException(String message) {
		super(message);
	}

	public InvalidMerchantUpdatedMessageException(String message, Throwable cause) {
		super(message, cause);
	}
}
