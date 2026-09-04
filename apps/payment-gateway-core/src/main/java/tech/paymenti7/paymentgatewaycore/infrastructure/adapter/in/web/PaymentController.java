package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ProblemDetail;
import tech.paymenti7.paymentgatewaycore.application.core.service.MerchantStatusResolutionService;

@Tag(name = "Payments", description = "Validação de merchants antes do processamento.")
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

	private final MerchantStatusResolutionService merchantStatusResolutionService;

	public PaymentController(MerchantStatusResolutionService merchantStatusResolutionService) {
		this.merchantStatusResolutionService = merchantStatusResolutionService;
	}

	@Operation(summary = "Valida um merchant para pagamento",
			description = "Resolve o status atual do merchant. Em cache miss, consulta o merchant-service e reidrata o cache.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Merchant validado com sucesso.",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentValidationResponse.class),
							examples = @ExampleObject(name = "MerchantValidated", value = """
									{"merchantId":"11111111-1111-1111-1111-111111111111","status":"ACTIVE"}
									"""))),
			@ApiResponse(responseCode = "400", description = "merchantId ausente ou inválido.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
							examples = @ExampleObject(name = "InvalidRequest", value = """
									{"title":"Bad Request","status":400,"detail":"merchantId is required"}
									"""))),
			@ApiResponse(responseCode = "404", description = "Merchant não encontrado.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
							examples = @ExampleObject(name = "MerchantNotFound", value = """
									{"title":"Merchant not found","status":404,"detail":"Merchant not found: 11111111-1111-1111-1111-111111111111"}
									"""))),
			@ApiResponse(responseCode = "503", description = "merchant-service indisponível.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
							examples = @ExampleObject(name = "MerchantServiceUnavailable", value = """
									{"title":"Merchant service unavailable","status":503,"detail":"Merchant service is unavailable"}
									""")))
	})
	@PostMapping
	ResponseEntity<PaymentValidationResponse> validateMerchant(@Valid @RequestBody PaymentValidationRequest request) {
		var merchant = merchantStatusResolutionService.resolve(request.merchantId());
		return ResponseEntity.ok(new PaymentValidationResponse(merchant.id(), merchant.status()));
	}
}
