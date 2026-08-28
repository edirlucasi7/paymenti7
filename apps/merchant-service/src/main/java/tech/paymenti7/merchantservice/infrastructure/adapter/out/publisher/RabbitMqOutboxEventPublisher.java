package tech.paymenti7.merchantservice.infrastructure.adapter.out.publisher;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPublisherPort;

@Component
public class RabbitMqOutboxEventPublisher implements OutboxEventPublisherPort {

	private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqOutboxEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;
	private final Duration confirmationTimeout;

	public RabbitMqOutboxEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
			@Value("${merchant.outbox.publisher-confirmation-timeout:2s}") Duration confirmationTimeout) {
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
		this.confirmationTimeout = confirmationTimeout;
	}

	@Override
	public void publish(OutboxEvent event) {
		var routing = RabbitMqEventRouting.forEventType(event.eventType());
		var correlationData = new CorrelationData(event.id().toString());
		var confirmationStartedAt = System.nanoTime();
		LOGGER.info("Publishing outbox event to RabbitMQ: eventId={}, aggregateId={}, eventType={}, exchange={}, routingKey={}",
				event.id(), event.aggregateId(), event.eventType(), routing.exchangeName(), routing.routingKey());
		try {
			rabbitTemplate.send(routing.exchangeName(), routing.routingKey(), toMessage(event), correlationData);
		}
		catch (RuntimeException exception) {
			LOGGER.warn("Could not send outbox event to RabbitMQ: eventId={}, aggregateId={}, eventType={}", event.id(),
					event.aggregateId(), event.eventType(), exception);
			throw exception;
		}
		awaitBrokerConfirmation(correlationData, event, confirmationStartedAt);
	}

	private Message toMessage(OutboxEvent event) {
		try {
			var properties = new MessageProperties();
			properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
			properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
			properties.setMessageId(event.id().toString());
			properties.setType(event.eventType());
			return new Message(objectMapper.writeValueAsBytes(OutboxEventMessage.from(event)), properties);
		}
		catch (JacksonException exception) {
			LOGGER.warn("Could not serialize outbox event: eventId={}, aggregateId={}, eventType={}", event.id(),
					event.aggregateId(), event.eventType(), exception);
			throw new OutboxEventPublicationException("Could not serialize outbox event " + event.id(), exception);
		}
	}

	private void awaitBrokerConfirmation(CorrelationData correlationData, OutboxEvent event, long confirmationStartedAt) {
		try {
			var confirm = correlationData.getFuture().get(confirmationTimeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!confirm.ack()) {
				LOGGER.warn("RabbitMQ negatively acknowledged outbox event: eventId={}, aggregateId={}, eventType={}, reason={}",
						event.id(), event.aggregateId(), event.eventType(), confirm.reason());
				throw new OutboxEventPublicationException(
						"RabbitMQ rejected outbox event " + event.id() + ": " + confirm.reason());
			}
			LOGGER.info("RabbitMQ acknowledged outbox event: eventId={}, aggregateId={}, eventType={}, confirmationTimeMs={}",
					event.id(), event.aggregateId(), event.eventType(), elapsedMilliseconds(confirmationStartedAt));
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Interrupted while awaiting RabbitMQ confirmation: eventId={}, aggregateId={}, eventType={}", event.id(),
					event.aggregateId(), event.eventType(), exception);
			throw new OutboxEventPublicationException("Interrupted while awaiting RabbitMQ confirmation for " + event.id(),
					exception);
		}
		catch (TimeoutException exception) {
			LOGGER.warn("Timed out awaiting RabbitMQ confirmation: eventId={}, aggregateId={}, eventType={}, timeout={}",
					event.id(), event.aggregateId(), event.eventType(), confirmationTimeout);
			throw new OutboxEventPublicationException("Timed out awaiting RabbitMQ confirmation for " + event.id(), exception);
		}
		catch (ExecutionException exception) {
			LOGGER.warn("Could not receive RabbitMQ confirmation: eventId={}, aggregateId={}, eventType={}", event.id(),
					event.aggregateId(), event.eventType(), exception.getCause());
			throw new OutboxEventPublicationException("Could not publish outbox event " + event.id(), exception.getCause());
		}
	}

	private long elapsedMilliseconds(long confirmationStartedAt) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - confirmationStartedAt);
	}

	private record OutboxEventMessage(int schemaVersion, String eventId, String aggregateType, String aggregateId,
			String eventType, String occurredAt, Map<String, String> payload) {

		private static OutboxEventMessage from(OutboxEvent event) {
			return new OutboxEventMessage(1, event.id().toString(), event.aggregateType(), event.aggregateId().toString(),
					event.eventType(), event.occurredAt().toString(), event.payload());
		}
	}
}
