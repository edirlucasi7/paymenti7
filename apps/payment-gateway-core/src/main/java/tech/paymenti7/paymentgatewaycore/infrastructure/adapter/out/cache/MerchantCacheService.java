package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantDetails;

@Service
public class MerchantCacheService {

	private static final Duration MINIMUM_TTL = Duration.ofMinutes(5);
	private static final Duration MAXIMUM_TTL = Duration.ofMinutes(15);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final Duration ttl;

	public MerchantCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
			@Value("${payment.gateway.merchant-cache.ttl}") Duration ttl) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		if (ttl.compareTo(MINIMUM_TTL) < 0 || ttl.compareTo(MAXIMUM_TTL) > 0) {
			throw new IllegalArgumentException("Merchant cache TTL must be between 5 and 15 minutes");
		}
		this.ttl = ttl;
	}

	public Optional<MerchantDetails> find(UUID merchantId) {
		String key = merchantCacheKey(merchantId);
		String cached = redisTemplate.opsForValue().get(key);
		if (cached == null) {
			return Optional.empty();
		}

		try {
			MerchantDetails merchant = objectMapper.readValue(cached, MerchantDetails.class);
			if (!merchantId.equals(merchant.id())) {
				redisTemplate.delete(key);
				return Optional.empty();
			}
			return Optional.of(merchant);
		}
		catch (JacksonException exception) {
			redisTemplate.delete(key);
			return Optional.empty();
		}
	}

	public void store(MerchantDetails merchant) {
		try {
			redisTemplate.opsForValue().set(merchantCacheKey(merchant.id()), objectMapper.writeValueAsString(merchant), ttl);
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("Could not serialize merchant cache entry", exception);
		}
	}

	public static String merchantCacheKey(UUID merchantId) {
		return "merchant:{" + merchantId + "}";
	}
}
