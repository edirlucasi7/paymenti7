package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache.MerchantCacheInvalidationService;

@Component
public class MerchantUpdatedMessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(MerchantUpdatedMessageListener.class);

	private final ObjectMapper objectMapper;
	private final Validator validator;
	private final MerchantCacheInvalidationService merchantCacheInvalidationService;

	public MerchantUpdatedMessageListener(ObjectMapper objectMapper, Validator validator,
			MerchantCacheInvalidationService merchantCacheInvalidationService) {
		this.objectMapper = objectMapper;
		this.validator = validator;
		this.merchantCacheInvalidationService = merchantCacheInvalidationService;
	}

	@RabbitListener(queues = "${payment.gateway.merchant-events.queue}")
	public void consume(Message message) {
		MerchantUpdatedMessage merchantUpdated = deserialize(message);
		var violations = validator.validate(merchantUpdated);
		if (!violations.isEmpty()) {
			throw invalidMessage(violations);
		}
		merchantUpdated.validate();
		merchantCacheInvalidationService.invalidate(merchantUpdated);
		LOGGER.info("Processed merchant cache invalidation event: eventId={}, merchantId={}", merchantUpdated.eventId(),
				merchantUpdated.aggregateId());
	}

	private MerchantUpdatedMessage deserialize(Message message) {
		try {
			return objectMapper.readValue(message.getBody(), MerchantUpdatedMessage.class);
		}
		catch (JacksonException exception) {
			throw new InvalidMerchantUpdatedMessageException("Could not deserialize MerchantUpdated event", exception);
		}
	}

	private InvalidMerchantUpdatedMessageException invalidMessage(
			Set<ConstraintViolation<MerchantUpdatedMessage>> violations) {
		String details = violations.stream()
				.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
				.collect(Collectors.joining(", "));
		return new InvalidMerchantUpdatedMessageException("Invalid MerchantUpdated event: " + details);
	}
}
