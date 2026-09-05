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
public class InternalMerchantController {

	private final GetMerchantUseCase getMerchantUseCase;

	public InternalMerchantController(GetMerchantUseCase getMerchantUseCase) {
		this.getMerchantUseCase = getMerchantUseCase;
	}

	@GetMapping("/internal/v1/merchants/{merchantId}")
	MerchantResponseV1 getMerchantV1(@PathVariable UUID merchantId) {
		var merchant = getMerchantUseCase.getById(merchantId);
		return new MerchantResponseV1(merchant.id(), merchant.status().name());
	}

	@GetMapping("/internal/v2/merchants/{merchantId}")
	MerchantResponseV2 getMerchantV2(@PathVariable UUID merchantId) {
		var merchant = getMerchantUseCase.getById(merchantId);
		return new MerchantResponseV2(merchant.id(), merchant.status().name(), merchant.revision());
	}

	record MerchantResponseV1(UUID id, String status) {
	}

	record MerchantResponseV2(UUID id, String status, long revision) {
	}
}
