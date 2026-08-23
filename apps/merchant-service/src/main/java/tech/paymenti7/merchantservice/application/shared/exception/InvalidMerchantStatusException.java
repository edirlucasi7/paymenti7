package tech.paymenti7.merchantservice.application.shared.exception;

public class InvalidMerchantStatusException extends RuntimeException {

	public InvalidMerchantStatusException() {
		super("Merchant status is required");
	}
}
