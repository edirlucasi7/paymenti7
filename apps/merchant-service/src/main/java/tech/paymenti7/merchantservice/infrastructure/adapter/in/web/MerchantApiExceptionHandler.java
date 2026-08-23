package tech.paymenti7.merchantservice.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tech.paymenti7.merchantservice.application.shared.exception.InvalidMerchantStatusException;
import tech.paymenti7.merchantservice.application.shared.exception.MerchantNotFoundException;

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

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		String detail = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage())
				.findFirst()
				.orElse("Request validation failed");
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid request");
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "status is mandatory");
		problem.setTitle("Invalid request");
		return problem;
	}
}
