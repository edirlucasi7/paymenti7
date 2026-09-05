package tech.paymenti7.paymentgatewaycore.application.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantDetails;
import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache.MerchantCacheService;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.merchant.MerchantServiceClient;

@ExtendWith(MockitoExtension.class)
class MerchantStatusResolutionServiceTest {

	@Mock
	private MerchantCacheService merchantCacheService;

	@Mock
	private MerchantServiceClient merchantServiceClient;

	@InjectMocks
	private MerchantStatusResolutionService merchantStatusResolutionService;

	@Test
	void returnsTheCachedMerchantWithoutCallingMerchantService() {
		UUID merchantId = UUID.randomUUID();
		MerchantDetails cachedMerchant = new MerchantDetails(merchantId, MerchantStatus.ACTIVE, 3L);
		given(merchantCacheService.find(merchantId)).willReturn(Optional.of(cachedMerchant));

		MerchantDetails result = merchantStatusResolutionService.resolve(merchantId);

		assertThat(result).isEqualTo(cachedMerchant);
		verify(merchantServiceClient, never()).getMerchant(merchantId);
		verify(merchantCacheService, never()).store(cachedMerchant);
	}

	@Test
	void loadsAndCachesTheMerchantWhenTheCacheIsMissing() {
		UUID merchantId = UUID.randomUUID();
		MerchantDetails merchant = new MerchantDetails(merchantId, MerchantStatus.INACTIVE, 3L);
		given(merchantCacheService.find(merchantId)).willReturn(Optional.empty());
		given(merchantServiceClient.getMerchant(merchantId)).willReturn(merchant);

		MerchantDetails result = merchantStatusResolutionService.resolve(merchantId);

		assertThat(result).isEqualTo(merchant);
		verify(merchantServiceClient).getMerchant(merchantId);
		verify(merchantCacheService).store(merchant);
	}
}
