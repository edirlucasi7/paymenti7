package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tech.paymenti7.paymentgatewaycore.application.shared.exception.MerchantNotFoundException;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.MerchantServiceUnavailableException;

@RestControllerAdvice
public class PaymentApiExceptionHandler {

	@ExceptionHandler(MerchantNotFoundException.class)
	ProblemDetail handleMerchantNotFound(MerchantNotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Merchant not found");
		return problem;
	}

	@ExceptionHandler(MerchantServiceUnavailableException.class)
	ProblemDetail handleMerchantServiceUnavailable(MerchantServiceUnavailableException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
		problem.setTitle("Merchant service unavailable");
		return problem;
	}
}
