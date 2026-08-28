package tech.paymenti7.merchantservice.infrastructure.adapter.out.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;
import tech.paymenti7.merchantservice.application.core.domain.OutboxEventDeliveryStatus;

@ExtendWith(OutputCaptureExtension.class)
class RabbitMqOutboxEventPublisherTest {

	private final RabbitTemplate rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
	private final RabbitMqOutboxEventPublisher publisher = new RabbitMqOutboxEventPublisher(rabbitTemplate,
			new ObjectMapper(), Duration.ofSeconds(2));

	@Test
	void publishesPersistentVersionedJsonAndLogsBrokerAck(CapturedOutput output) throws Exception {
		var event = pendingEvent();
		var messageCaptor = ArgumentCaptor.forClass(Message.class);

		doAnswer(invocation -> {
			((CorrelationData) invocation.getArgument(3)).getFuture()
					.complete(new CorrelationData.Confirm(true, null));
			return null;
		}).when(rabbitTemplate).send(eq(RabbitMqEventRouting.MERCHANT_UPDATED.exchangeName()),
				eq(RabbitMqEventRouting.MERCHANT_UPDATED.routingKey()), messageCaptor.capture(), any(CorrelationData.class));

		publisher.publish(event);

		var message = messageCaptor.getValue();
		var body = new ObjectMapper().readTree(message.getBody());
		assertEquals(1, body.get("schemaVersion").asInt());
		assertEquals(event.id().toString(), body.get("eventId").asText());
		assertEquals(event.aggregateId().toString(), body.get("aggregateId").asText());
		assertEquals("ACTIVE", body.get("payload").get("status").asText());
		assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
		assertEquals("application/json", message.getMessageProperties().getContentType());
		assertThat(output).contains("INFO").contains("Publishing outbox event to RabbitMQ")
				.contains("RabbitMQ acknowledged outbox event").contains(event.id().toString());
	}

	@Test
	void failsAndLogsWarningWhenBrokerNacksThePublication(CapturedOutput output) {
		doAnswer(invocation -> {
			((CorrelationData) invocation.getArgument(3)).getFuture()
					.complete(new CorrelationData.Confirm(false, "broker unavailable"));
			return null;
		}).when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

		assertThrows(OutboxEventPublicationException.class, () -> publisher.publish(pendingEvent()));
		assertThat(output).contains("WARN").contains("RabbitMQ negatively acknowledged outbox event")
				.contains("broker unavailable");
	}

	@Test
	void failsWhenBrokerDoesNotConfirmWithinTheConfiguredTimeout() {
		var timeoutPublisher = new RabbitMqOutboxEventPublisher(rabbitTemplate, new ObjectMapper(), Duration.ZERO);

		assertThrows(OutboxEventPublicationException.class, () -> timeoutPublisher.publish(pendingEvent()));
	}

	@Test
	void failsBeforeSendingWhenEventTypeHasNoConfiguredRouting() {
		var unknownEvent = OutboxEvent.rehydrate(UUID.randomUUID(), "MERCHANT", UUID.randomUUID(), "MerchantDeleted",
				Map.of(), Instant.parse("2026-08-28T10:00:00Z"), OutboxEventDeliveryStatus.PENDING, null, null);

		assertThrows(OutboxEventPublicationException.class, () -> publisher.publish(unknownEvent));

		verifyNoInteractions(rabbitTemplate);
	}

	private OutboxEvent pendingEvent() {
		var merchantId = UUID.randomUUID();
		return OutboxEvent.rehydrate(UUID.randomUUID(), "MERCHANT", merchantId, "MerchantUpdated",
				Map.of("merchantId", merchantId.toString(), "status", "ACTIVE"), Instant.parse("2026-08-28T10:00:00Z"),
				OutboxEventDeliveryStatus.PENDING, null, null);
	}
}
