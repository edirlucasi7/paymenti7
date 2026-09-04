package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import tech.paymenti7.paymentgatewaycore.application.core.service.MerchantStatusResolutionService;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

	private final MerchantStatusResolutionService merchantStatusResolutionService;

	public PaymentController(MerchantStatusResolutionService merchantStatusResolutionService) {
		this.merchantStatusResolutionService = merchantStatusResolutionService;
	}

	@PostMapping
	ResponseEntity<PaymentValidationResponse> validateMerchant(@Valid @RequestBody PaymentValidationRequest request) {
		var merchant = merchantStatusResolutionService.resolve(request.merchantId());
		return ResponseEntity.ok(new PaymentValidationResponse(merchant.id(), merchant.status()));
	}
}
