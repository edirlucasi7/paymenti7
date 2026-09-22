package tech.paymenti7.paymentgatewaycore.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentAuthorizationResultsRabbitMqConfiguration {

	public static final String PAYMENT_EVENTS_EXCHANGE = "payment.events";
	public static final String AUTHORIZATION_COMPLETED_ROUTING_KEY = "payment.authorization.completed";

	@Bean
	TopicExchange paymentEventsExchange() {
		return new TopicExchange(PAYMENT_EVENTS_EXCHANGE, true, false);
	}

	@Bean
	DirectExchange paymentAuthorizationResultsDeadLetterExchange(
			@Value("${payment.gateway.authorization-results.dead-letter-exchange}") String name) {
		return new DirectExchange(name, true, false);
	}

	@Bean
	Queue paymentAuthorizationResultsQueue(
			@Value("${payment.gateway.authorization-results.queue}") String queue,
			@Value("${payment.gateway.authorization-results.dead-letter-exchange}") String deadLetterExchange,
			@Value("${payment.gateway.authorization-results.dead-letter-queue}") String deadLetterQueue) {
		return QueueBuilder.durable(queue).deadLetterExchange(deadLetterExchange)
				.deadLetterRoutingKey(deadLetterQueue).build();
	}

	@Bean
	Queue paymentAuthorizationResultsDeadLetterQueue(
			@Value("${payment.gateway.authorization-results.dead-letter-queue}") String queue) {
		return QueueBuilder.durable(queue).build();
	}

	@Bean
	Binding paymentAuthorizationResultsBinding(Queue paymentAuthorizationResultsQueue,
			TopicExchange paymentEventsExchange) {
		return BindingBuilder.bind(paymentAuthorizationResultsQueue).to(paymentEventsExchange)
				.with(AUTHORIZATION_COMPLETED_ROUTING_KEY);
	}

	@Bean
	Binding paymentAuthorizationResultsDeadLetterBinding(Queue paymentAuthorizationResultsDeadLetterQueue,
			DirectExchange paymentAuthorizationResultsDeadLetterExchange) {
		return BindingBuilder.bind(paymentAuthorizationResultsDeadLetterQueue)
				.to(paymentAuthorizationResultsDeadLetterExchange)
				.with(paymentAuthorizationResultsDeadLetterQueue.getName());
	}
}
