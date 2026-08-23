package tech.paymenti7.merchantservice.merchant.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tech.paymenti7.merchantservice.merchant.application.InvalidMerchantStatusException;
import tech.paymenti7.merchantservice.merchant.application.MerchantNotFoundException;

@RestControllerAdvice
public class MerchantApiExceptionHandler {

	@ExceptionHandler(MerchantNotFoundException.class)
	ProblemDetail handleMerchantNotFound(MerchantNotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Merchant not found");
		return problem;
	}

	@ExceptionHandler(InvalidMerchantStatusException.class)
	ProblemDetail handleInvalidStatus(InvalidMerchantStatusException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid merchant status");
		return problem;
	}
}
