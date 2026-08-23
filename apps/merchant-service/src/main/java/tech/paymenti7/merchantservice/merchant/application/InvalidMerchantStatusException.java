package tech.paymenti7.merchantservice.merchant.application;

public class InvalidMerchantStatusException extends RuntimeException {

	public InvalidMerchantStatusException() {
		super("Merchant status is required");
	}
}
