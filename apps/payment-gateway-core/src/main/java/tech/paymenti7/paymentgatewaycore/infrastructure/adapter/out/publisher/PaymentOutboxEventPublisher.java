package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.publisher;

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

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity.PaymentOutboxEventEntity;

@Component
public class PaymentOutboxEventPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;
	private final Duration confirmationTimeout;

	public PaymentOutboxEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
			@Value("${payment.gateway.outbox.publisher-confirmation-timeout}") Duration confirmationTimeout) {
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
		this.confirmationTimeout = confirmationTimeout;
	}

	public void publish(PaymentOutboxEventEntity event) {
		var correlation = new CorrelationData(event.getId().toString());
		rabbitTemplate.send(PaymentRabbitMqConfiguration.PAYMENT_COMMANDS_EXCHANGE,
				PaymentRabbitMqConfiguration.PAYMENT_REQUESTED_ROUTING_KEY, toMessage(event), correlation);
		awaitConfirmation(correlation, event);
	}

	private Message toMessage(PaymentOutboxEventEntity event) {
		try {
			var properties = new MessageProperties();
			properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
			properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
			properties.setMessageId(event.getId().toString());
			properties.setType(event.getEventType());
			return new Message(objectMapper.writeValueAsBytes(PaymentRequestedMessage.from(event)), properties);
		}
		catch (JacksonException exception) {
			throw new PaymentOutboxPublicationException("Could not serialize payment event " + event.getId(), exception);
		}
	}

	private void awaitConfirmation(CorrelationData correlation, PaymentOutboxEventEntity event) {
		try {
			var confirmation = correlation.getFuture().get(confirmationTimeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!confirmation.ack()) {
				throw new PaymentOutboxPublicationException(
						"RabbitMQ rejected payment event " + event.getId() + ": " + confirmation.reason());
			}
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new PaymentOutboxPublicationException(
					"Interrupted while awaiting confirmation for payment event " + event.getId(), exception);
		}
		catch (ExecutionException | TimeoutException exception) {
			throw new PaymentOutboxPublicationException(
					"Could not confirm payment event " + event.getId(), exception);
		}
	}

	private record PaymentRequestedMessage(int schemaVersion, String eventId, String aggregateType, String aggregateId,
			String eventType, String occurredAt, Map<String, Object> payload) {

		private static PaymentRequestedMessage from(PaymentOutboxEventEntity event) {
			return new PaymentRequestedMessage(1, event.getId().toString(), event.getAggregateType(),
					event.getAggregateId().toString(), event.getEventType(), event.getOccurredAt().toString(),
					event.getPayload());
		}
	}
}
