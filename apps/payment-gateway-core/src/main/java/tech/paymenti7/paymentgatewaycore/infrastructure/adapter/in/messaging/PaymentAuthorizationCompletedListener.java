package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction.TransactionalAuthorizationResultProcessor;

@Component
public class PaymentAuthorizationCompletedListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentAuthorizationCompletedListener.class);

	private final ObjectMapper objectMapper;
	private final TransactionalAuthorizationResultProcessor processor;

	public PaymentAuthorizationCompletedListener(ObjectMapper objectMapper,
			TransactionalAuthorizationResultProcessor processor) {
		this.objectMapper = objectMapper;
		this.processor = processor;
	}

	@RabbitListener(queues = "${payment.gateway.authorization-results.queue}")
	public void consume(Message rawMessage) {
		PaymentAuthorizationCompletedMessage message = deserialize(rawMessage);
		message.validate();
		if (processor.process(message)) {
			LOGGER.info("Processed payment authorization result: eventId={}, paymentId={}, status={}",
					message.eventId(), message.payload().paymentId(), message.payload().status());
		}
	}

	private PaymentAuthorizationCompletedMessage deserialize(Message message) {
		try {
			return objectMapper.readValue(message.getBody(), PaymentAuthorizationCompletedMessage.class);
		}
		catch (JacksonException exception) {
			throw new InvalidPaymentAuthorizationMessageException(
					"Could not deserialize PaymentAuthorizationCompleted event", exception);
		}
	}
}
