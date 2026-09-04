package tech.paymenti7.paymentgatewaycore.application.shared.exception;

public class MerchantServiceUnavailableException extends RuntimeException {

	public MerchantServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public MerchantServiceUnavailableException(String message) {
		super(message);
	}
}
