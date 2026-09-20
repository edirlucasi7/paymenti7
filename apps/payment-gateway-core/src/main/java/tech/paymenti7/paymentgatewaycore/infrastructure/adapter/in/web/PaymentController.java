package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import java.util.UUID;
import java.util.Map;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import tech.paymenti7.paymentgatewaycore.application.port.in.SubmitPaymentCommand;
import tech.paymenti7.paymentgatewaycore.application.port.in.SubmitPaymentUseCase;
import tech.paymenti7.paymentgatewaycore.application.port.in.GetPaymentUseCase;

@Tag(name = "Payments", description = "Entrada idempotente de intenções de pagamento.")
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

	private final SubmitPaymentUseCase submitPaymentUseCase;
	private final GetPaymentUseCase getPaymentUseCase;

	public PaymentController(SubmitPaymentUseCase submitPaymentUseCase, GetPaymentUseCase getPaymentUseCase) {
		this.submitPaymentUseCase = submitPaymentUseCase;
		this.getPaymentUseCase = getPaymentUseCase;
	}

	@Operation(summary = "Submete uma intenção de pagamento",
			description = "Aceita a intenção uma única vez por Idempotency-Key e inicia seu processamento assíncrono.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "202", description = "Pagamento aceito ou ainda em processamento.",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class),
							examples = @ExampleObject(name = "PaymentProcessing", value = """
									{"paymentId":"22222222-2222-2222-2222-222222222222","status":"PROCESSING"}
									"""))),
			@ApiResponse(responseCode = "400", description = "Header ou payload ausente ou inválido.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
							examples = @ExampleObject(name = "InvalidRequest", value = """
									{"title":"Bad Request","status":400,"detail":"amount must be greater than zero"}
									"""))),
			@ApiResponse(responseCode = "404", description = "Merchant não encontrado.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
							examples = @ExampleObject(name = "MerchantNotFound", value = """
									{"title":"Merchant not found","status":404,"detail":"Merchant not found: 11111111-1111-1111-1111-111111111111"}
									"""))),
			@ApiResponse(responseCode = "409", description = "Idempotency-Key reutilizada com outro payload.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "422", description = "Merchant inativo.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "503", description = "merchant-service indisponível.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
							examples = @ExampleObject(name = "MerchantServiceUnavailable", value = """
									{"title":"Merchant service unavailable","status":503,"detail":"Merchant service is unavailable"}
									""")))
	})
	@PostMapping
	ResponseEntity<Map<String, Object>> submit(
			@Parameter(description = "UUID único da intenção de pagamento", required = true)
			@RequestHeader("Idempotency-Key") UUID idempotencyKey,
			@Valid @RequestBody PaymentRequest request) {
		var result = submitPaymentUseCase.submit(
				new SubmitPaymentCommand(request.merchantId(), request.amount(), request.currency(),
						request.paymentMethodToken(), idempotencyKey));
		var response = ResponseEntity.status(result.httpStatus());
		result.responseHeaders().forEach(response::header);
		return response.body(result.responseBody());
	}

	@Operation(summary = "Consulta o estado atual de um pagamento")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Pagamento encontrado",
					content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
			@ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@GetMapping("/{paymentId}")
	PaymentResponse get(@PathVariable UUID paymentId) {
		var payment = getPaymentUseCase.get(paymentId);
		return new PaymentResponse(payment.paymentId(), payment.status());
	}
}
