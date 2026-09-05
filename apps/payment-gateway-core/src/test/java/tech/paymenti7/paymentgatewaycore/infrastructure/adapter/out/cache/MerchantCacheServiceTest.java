package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantDetails;
import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;

class MerchantCacheServiceTest {

	private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
	private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final MerchantCacheService merchantCacheService = new MerchantCacheService(redisTemplate, objectMapper,
			Duration.ofMinutes(10), Duration.ofHours(24));

	@Test
	void storesMerchantOnlyWhenItsRevisionIsCurrent() throws Exception {
		UUID merchantId = UUID.randomUUID();
		MerchantDetails merchant = new MerchantDetails(merchantId, MerchantStatus.ACTIVE, 3L);

		merchantCacheService.store(merchant);

		verify(redisTemplate).execute(any(), eq(java.util.List.of(MerchantCacheService.merchantCacheKey(merchantId),
				MerchantCacheService.merchantRevisionKey(merchantId))), eq(objectMapper.writeValueAsString(merchant)), eq("3"),
				eq("600000"), eq("86400000"));
	}

	@Test
	void removesMalformedEntriesAndTreatsThemAsCacheMisses() {
		UUID merchantId = UUID.randomUUID();
		String key = MerchantCacheService.merchantCacheKey(merchantId);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(key)).thenReturn("not-json");

		var result = merchantCacheService.find(merchantId);

		assertThat(result).isEmpty();
		verify(redisTemplate).delete(key);
	}

	@Test
	void rejectsTtlOutsideTheArchitecturalRange() {
		assertThatIllegalArgumentException().isThrownBy(() -> new MerchantCacheService(redisTemplate, objectMapper,
				Duration.ofMinutes(4), Duration.ofHours(24)))
				.withMessage("Merchant cache TTL must be between 5 and 15 minutes");
	}

	@Test
	void rejectsRevisionTtlShorterThanTheCacheTtl() {
		assertThatIllegalArgumentException().isThrownBy(() -> new MerchantCacheService(redisTemplate, objectMapper,
				Duration.ofMinutes(10), Duration.ofMinutes(5)))
				.withMessage("Merchant cache revision TTL must not be shorter than the cache TTL");
	}
}
