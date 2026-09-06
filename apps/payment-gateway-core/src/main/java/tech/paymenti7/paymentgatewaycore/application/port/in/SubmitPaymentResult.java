package tech.paymenti7.paymentgatewaycore.application.port.in;

import java.util.Map;
import java.util.UUID;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;

public record SubmitPaymentResult(UUID paymentId, PaymentStatus status, int httpStatus,
		Map<String, Object> responseBody, Map<String, String> responseHeaders) {

	public SubmitPaymentResult {
		responseBody = Map.copyOf(responseBody);
		responseHeaders = Map.copyOf(responseHeaders);
	}
}
