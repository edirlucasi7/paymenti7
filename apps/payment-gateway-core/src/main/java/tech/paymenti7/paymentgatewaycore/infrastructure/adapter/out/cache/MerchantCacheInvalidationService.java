package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging.MerchantUpdatedMessage;

@Service
public class MerchantCacheInvalidationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MerchantCacheInvalidationService.class);

	private final StringRedisTemplate redisTemplate;
	private final Duration deduplicationTtl;

	public MerchantCacheInvalidationService(StringRedisTemplate redisTemplate,
			@Value("${payment.gateway.merchant-events.deduplication-ttl:24h}") Duration deduplicationTtl) {
		this.redisTemplate = redisTemplate;
		this.deduplicationTtl = deduplicationTtl;
	}

	public void invalidate(MerchantUpdatedMessage event) {
		String processedEventKey = processedEventKey(event);
		if (Boolean.TRUE.equals(redisTemplate.hasKey(processedEventKey))) {
			LOGGER.info("Ignoring duplicated merchant cache invalidation event: eventId={}", event.eventId());
			return;
		}

		redisTemplate.delete(merchantCacheKey(event));
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
}
