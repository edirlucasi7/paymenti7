package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tech.paymenti7.paymentgatewaycore.application.shared.exception.MerchantNotFoundException;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.MerchantServiceUnavailableException;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.IdempotencyConflictException;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.InactiveMerchantException;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.PaymentNotFoundException;

@RestControllerAdvice
public class PaymentApiExceptionHandler {

	@ExceptionHandler(PaymentNotFoundException.class)
	ProblemDetail handlePaymentNotFound(PaymentNotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Payment not found");
		return problem;
	}

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

	@ExceptionHandler(IdempotencyConflictException.class)
	ProblemDetail handleIdempotencyConflict(IdempotencyConflictException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Idempotency conflict");
		return problem;
	}

	@ExceptionHandler(InactiveMerchantException.class)
	ProblemDetail handleInactiveMerchant(InactiveMerchantException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
		problem.setTitle("Inactive merchant");
		return problem;
	}
}
