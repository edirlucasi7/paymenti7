package tech.paymenti7.merchantservice.infrastructure.adapter.out.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPublisherPort;

@Component
public class LoggingOutboxEventPublisher implements OutboxEventPublisherPort {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingOutboxEventPublisher.class);

	@Override
	public void publish(OutboxEvent event) {
		LOGGER.info("Mock outbox event published: eventId={}, aggregateId={}, eventType={}, payload={}", event.id(),
				event.aggregateId(), event.eventType(), event.payload());
	}
}
