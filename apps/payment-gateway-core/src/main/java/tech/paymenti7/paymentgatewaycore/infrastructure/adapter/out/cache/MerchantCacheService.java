package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantDetails;

@Service
public class MerchantCacheService {

	private static final Duration MINIMUM_TTL = Duration.ofMinutes(5);
	private static final Duration MAXIMUM_TTL = Duration.ofMinutes(15);
	private static final DefaultRedisScript<Long> STORE_IF_CURRENT_REVISION = new DefaultRedisScript<>("""
			local knownRevision = redis.call('GET', KEYS[2])
			local candidateRevision = tonumber(ARGV[2])
			if knownRevision and candidateRevision < tonumber(knownRevision) then
				return 0
			end
			redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[3])
			redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[4])
			return 1
			""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final Duration ttl;
	private final Duration revisionTtl;

	public MerchantCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
			@Value("${payment.gateway.merchant-cache.ttl}") Duration ttl,
			@Value("${payment.gateway.merchant-cache.revision-ttl:24h}") Duration revisionTtl) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		if (ttl.compareTo(MINIMUM_TTL) < 0 || ttl.compareTo(MAXIMUM_TTL) > 0) {
			throw new IllegalArgumentException("Merchant cache TTL must be between 5 and 15 minutes");
		}
		this.ttl = ttl;
		if (revisionTtl.compareTo(ttl) < 0) {
			throw new IllegalArgumentException("Merchant cache revision TTL must not be shorter than the cache TTL");
		}
		this.revisionTtl = revisionTtl;
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
			redisTemplate.execute(STORE_IF_CURRENT_REVISION,
					java.util.List.of(merchantCacheKey(merchant.id()), merchantRevisionKey(merchant.id())),
					objectMapper.writeValueAsString(merchant), merchant.revision().toString(), Long.toString(ttl.toMillis()),
					Long.toString(revisionTtl.toMillis()));
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("Could not serialize merchant cache entry", exception);
		}
	}

	public static String merchantCacheKey(UUID merchantId) {
		return "merchant:{" + merchantId + "}";
	}

	public static String merchantRevisionKey(UUID merchantId) {
		return "merchant:revision:{" + merchantId + "}";
	}
}
