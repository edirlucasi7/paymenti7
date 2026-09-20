package tech.paymenti7.paymentauthorization.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.paymentauthorization.application.service.PaymentRequestedInboxService;

@Component
public class PaymentRequestedListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentRequestedListener.class);

	private final ObjectMapper objectMapper;
	private final PaymentRequestedInboxService inboxService;

	public PaymentRequestedListener(ObjectMapper objectMapper, PaymentRequestedInboxService inboxService) {
		this.objectMapper = objectMapper;
		this.inboxService = inboxService;
	}

	@RabbitListener(queues = "${payment.authorization.input.queue}",
			containerFactory = "authorizationRabbitListenerContainerFactory")
	public void consume(Message rawMessage) {
		PaymentRequestedMessage message = deserialize(rawMessage);
		message.validate();
		if (inboxService.accept(message)) {
			LOGGER.info("Accepted payment authorization command: eventId={}, paymentId={}",
					message.eventId(), message.payload().paymentId());
		}
	}

	private PaymentRequestedMessage deserialize(Message message) {
		try {
			return objectMapper.readValue(message.getBody(), PaymentRequestedMessage.class);
		}
		catch (JacksonException exception) {
			throw new InvalidPaymentRequestedMessageException("Could not deserialize PaymentRequested event", exception);
		}
	}
}
