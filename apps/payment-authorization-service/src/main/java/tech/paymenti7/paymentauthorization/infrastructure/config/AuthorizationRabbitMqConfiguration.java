package tech.paymenti7.paymentauthorization.infrastructure.config;

import java.time.Duration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthorizationRabbitMqConfiguration {

	public static final String PAYMENT_COMMANDS_EXCHANGE = "payment.commands";
	public static final String PAYMENT_REQUESTED_ROUTING_KEY = "payment.requested";
	public static final String PAYMENT_EVENTS_EXCHANGE = "payment.events";
	public static final String AUTHORIZATION_COMPLETED_ROUTING_KEY = "payment.authorization.completed";

	@Bean
	TopicExchange authorizationPaymentCommandsExchange() {
		return new TopicExchange(PAYMENT_COMMANDS_EXCHANGE, true, false);
	}

	@Bean
	TopicExchange authorizationPaymentEventsExchange() {
		return new TopicExchange(PAYMENT_EVENTS_EXCHANGE, true, false);
	}

	@Bean
	DirectExchange authorizationDeadLetterExchange(
			@Value("${payment.authorization.input.dead-letter-exchange}") String name) {
		return new DirectExchange(name, true, false);
	}

	@Bean
	Queue paymentRequestedQueue(@Value("${payment.authorization.input.queue}") String queue,
			@Value("${payment.authorization.input.dead-letter-exchange}") String deadLetterExchange,
			@Value("${payment.authorization.input.dead-letter-queue}") String deadLetterQueue) {
		return QueueBuilder.durable(queue).deadLetterExchange(deadLetterExchange)
				.deadLetterRoutingKey(deadLetterQueue).build();
	}

	@Bean
	Queue paymentRequestedDeadLetterQueue(
			@Value("${payment.authorization.input.dead-letter-queue}") String queue) {
		return QueueBuilder.durable(queue).build();
	}

	@Bean
	Binding paymentRequestedBinding(Queue paymentRequestedQueue, TopicExchange authorizationPaymentCommandsExchange) {
		return BindingBuilder.bind(paymentRequestedQueue).to(authorizationPaymentCommandsExchange)
				.with(PAYMENT_REQUESTED_ROUTING_KEY);
	}

	@Bean
	Binding paymentRequestedDeadLetterBinding(Queue paymentRequestedDeadLetterQueue,
			DirectExchange authorizationDeadLetterExchange) {
		return BindingBuilder.bind(paymentRequestedDeadLetterQueue).to(authorizationDeadLetterExchange)
				.with(paymentRequestedDeadLetterQueue.getName());
	}

	@Bean
	SimpleRabbitListenerContainerFactory authorizationRabbitListenerContainerFactory(
			ConnectionFactory connectionFactory,
			@Value("${payment.authorization.input.retry.initial-interval}") Duration initialInterval,
			@Value("${payment.authorization.input.retry.multiplier}") double multiplier,
			@Value("${payment.authorization.input.retry.max-interval}") Duration maxInterval) {
		var factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setDefaultRequeueRejected(false);
		factory.setAdviceChain(RetryInterceptorBuilder.stateless().maxRetries(3)
				.backOffOptions(initialInterval.toMillis(), multiplier, maxInterval.toMillis())
				.recoverer(new RejectAndDontRequeueRecoverer()).build());
		return factory;
	}
}
