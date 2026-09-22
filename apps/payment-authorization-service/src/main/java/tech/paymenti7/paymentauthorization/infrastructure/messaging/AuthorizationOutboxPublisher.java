package tech.paymenti7.paymentauthorization.infrastructure.messaging;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.paymentauthorization.infrastructure.config.AuthorizationRabbitMqConfiguration;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.entity.AuthorizationOutboxEventEntity;

@Component
public class AuthorizationOutboxPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;
	private final Duration confirmationTimeout;

	public AuthorizationOutboxPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
			@Value("${payment.authorization.outbox.publisher-confirmation-timeout}") Duration confirmationTimeout) {
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
		this.confirmationTimeout = confirmationTimeout;
	}

	public void publish(AuthorizationOutboxEventEntity event) {
		var correlation = new CorrelationData(event.getId().toString());
		rabbitTemplate.send(AuthorizationRabbitMqConfiguration.PAYMENT_EVENTS_EXCHANGE,
				AuthorizationRabbitMqConfiguration.AUTHORIZATION_COMPLETED_ROUTING_KEY, toMessage(event), correlation);
		try {
			var confirmation = correlation.getFuture().get(confirmationTimeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!confirmation.ack()) {
				throw new AuthorizationOutboxPublicationException("RabbitMQ rejected event " + event.getId());
			}
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AuthorizationOutboxPublicationException("Interrupted while publishing " + event.getId(), exception);
		}
		catch (ExecutionException | TimeoutException exception) {
			throw new AuthorizationOutboxPublicationException("Could not confirm event " + event.getId(), exception);
		}
	}

	private Message toMessage(AuthorizationOutboxEventEntity event) {
		try {
			var properties = new MessageProperties();
			properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
			properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
			properties.setMessageId(event.getId().toString());
			properties.setType(event.getEventType());
			return new Message(objectMapper.writeValueAsBytes(new EventEnvelope(event.getSchemaVersion(),
					event.getId().toString(), "PAYMENT", event.getAggregateId().toString(), event.getEventType(),
					event.getOccurredAt().toString(), event.getPayload())), properties);
		}
		catch (JacksonException exception) {
			throw new AuthorizationOutboxPublicationException("Could not serialize event " + event.getId(), exception);
		}
	}

	private record EventEnvelope(int schemaVersion, String eventId, String aggregateType, String aggregateId,
			String eventType, String occurredAt, Map<String, Object> payload) {
	}
}
