package tech.paymenti7.paymentgatewaycore.application.core.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantDetails;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache.MerchantCacheService;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.merchant.MerchantServiceClient;

@Service
public class MerchantStatusResolutionService {

	private final MerchantCacheService merchantCacheService;
	private final MerchantServiceClient merchantServiceClient;

	public MerchantStatusResolutionService(MerchantCacheService merchantCacheService,
			MerchantServiceClient merchantServiceClient) {
		this.merchantCacheService = merchantCacheService;
		this.merchantServiceClient = merchantServiceClient;
	}

	public MerchantDetails resolve(UUID merchantId) {
		return merchantCacheService.find(merchantId).orElseGet(() -> {
			var merchant = merchantServiceClient.getMerchant(merchantId);
			merchantCacheService.store(merchant);
			return merchant;
		});
	}
}
