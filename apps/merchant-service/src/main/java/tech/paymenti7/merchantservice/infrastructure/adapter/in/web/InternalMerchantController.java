package tech.paymenti7.merchantservice.infrastructure.adapter.in.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

import tech.paymenti7.merchantservice.application.port.in.GetMerchantUseCase;

@Hidden
@RestController
@RequestMapping("/internal/v1/merchants")
public class InternalMerchantController {

	private final GetMerchantUseCase getMerchantUseCase;

	public InternalMerchantController(GetMerchantUseCase getMerchantUseCase) {
		this.getMerchantUseCase = getMerchantUseCase;
	}

	@GetMapping("/{merchantId}")
	MerchantResponse getMerchant(@PathVariable UUID merchantId) {
		var merchant = getMerchantUseCase.getById(merchantId);
		return new MerchantResponse(merchant.id(), merchant.status().name());
	}

	record MerchantResponse(UUID id, String status) {
	}
}
