package tech.paymenti7.merchantservice.infrastructure.adapter.out.publisher;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RabbitMqEventRouting {

	MERCHANT_UPDATED("MerchantUpdated", "merchant.events", "merchant.updated");

	private static final Map<String, RabbitMqEventRouting> ROUTING_BY_EVENT_TYPE = Arrays.stream(values())
			.collect(Collectors.toUnmodifiableMap(RabbitMqEventRouting::eventType, Function.identity()));

	private final String eventType;
	private final String exchangeName;
	private final String routingKey;

	RabbitMqEventRouting(String eventType, String exchangeName, String routingKey) {
		this.eventType = eventType;
		this.exchangeName = exchangeName;
		this.routingKey = routingKey;
	}

	public static RabbitMqEventRouting forEventType(String eventType) {
		var routing = ROUTING_BY_EVENT_TYPE.get(eventType);
		if (routing == null) {
			throw new OutboxEventPublicationException("No RabbitMQ routing configured for event type " + eventType);
		}
		return routing;
	}

	public String eventType() {
		return eventType;
	}

	public String exchangeName() {
		return exchangeName;
	}

	public String routingKey() {
		return routingKey;
	}
}
