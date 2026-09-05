package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging.MerchantUpdatedMessage;

@Service
public class MerchantCacheInvalidationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MerchantCacheInvalidationService.class);
	private static final DefaultRedisScript<Long> INVALIDATE_IF_NEWER_REVISION = new DefaultRedisScript<>("""
			local knownRevision = redis.call('GET', KEYS[2])
			local eventRevision = tonumber(ARGV[1])
			if knownRevision and eventRevision <= tonumber(knownRevision) then
				return 0
			end
			redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[2])
			redis.call('DEL', KEYS[1])
			return 1
			""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final Duration deduplicationTtl;
	private final Duration revisionTtl;

	public MerchantCacheInvalidationService(StringRedisTemplate redisTemplate,
			@Value("${payment.gateway.merchant-events.deduplication-ttl:24h}") Duration deduplicationTtl,
			@Value("${payment.gateway.merchant-cache.revision-ttl:24h}") Duration revisionTtl) {
		this.redisTemplate = redisTemplate;
		this.deduplicationTtl = deduplicationTtl;
		this.revisionTtl = revisionTtl;
	}

	public void invalidate(MerchantUpdatedMessage event) {
		String processedEventKey = processedEventKey(event);
		if (Boolean.TRUE.equals(redisTemplate.hasKey(processedEventKey))) {
			LOGGER.info("Ignoring duplicated merchant cache invalidation event: eventId={}", event.eventId());
			return;
		}

		redisTemplate.execute(INVALIDATE_IF_NEWER_REVISION,
				java.util.List.of(merchantCacheKey(event), merchantRevisionKey(event)), event.payload().revision().toString(),
				Long.toString(revisionTtl.toMillis()));
		Boolean recorded = redisTemplate.opsForValue().setIfAbsent(processedEventKey, "1", deduplicationTtl);
		if (Boolean.FALSE.equals(recorded)) {
			LOGGER.info("Merchant cache invalidation event was concurrently processed: eventId={}", event.eventId());
		}
	}

	public static String merchantCacheKey(MerchantUpdatedMessage event) {
		return "merchant:{" + event.aggregateId() + "}";
	}

	public static String processedEventKey(MerchantUpdatedMessage event) {
		return "merchant:processed-event:" + event.eventId();
	}

	public static String merchantRevisionKey(MerchantUpdatedMessage event) {
		return MerchantCacheService.merchantRevisionKey(event.aggregateId());
	}
}
