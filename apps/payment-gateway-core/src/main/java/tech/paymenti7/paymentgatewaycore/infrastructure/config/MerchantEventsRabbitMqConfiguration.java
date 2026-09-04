package tech.paymenti7.paymentgatewaycore.infrastructure.config;

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
public class MerchantEventsRabbitMqConfiguration {

	static final String MERCHANT_EVENTS_EXCHANGE = "merchant.events";
	static final String MERCHANT_UPDATED_ROUTING_KEY = "merchant.updated";

	@Bean
	TopicExchange merchantEventsExchange() {
		return new TopicExchange(MERCHANT_EVENTS_EXCHANGE, true, false);
	}

	@Bean
	DirectExchange merchantEventsDeadLetterExchange(
			@Value("${payment.gateway.merchant-events.dead-letter-exchange}") String deadLetterExchange) {
		return new DirectExchange(deadLetterExchange, true, false);
	}

	@Bean
	Queue merchantCacheInvalidationQueue(
			@Value("${payment.gateway.merchant-events.queue}") String queueName,
			@Value("${payment.gateway.merchant-events.dead-letter-exchange}") String deadLetterExchange,
			@Value("${payment.gateway.merchant-events.dead-letter-queue}") String deadLetterQueue) {
		return QueueBuilder.durable(queueName)
				.deadLetterExchange(deadLetterExchange)
				.deadLetterRoutingKey(deadLetterQueue)
				.build();
	}

	@Bean
	Queue merchantCacheInvalidationDeadLetterQueue(
			@Value("${payment.gateway.merchant-events.dead-letter-queue}") String deadLetterQueue) {
		return QueueBuilder.durable(deadLetterQueue).build();
	}

	@Bean
	Binding merchantUpdatedBinding(Queue merchantCacheInvalidationQueue, TopicExchange merchantEventsExchange) {
		return BindingBuilder.bind(merchantCacheInvalidationQueue)
				.to(merchantEventsExchange)
				.with(MERCHANT_UPDATED_ROUTING_KEY);
	}

	@Bean
	Binding merchantCacheInvalidationDeadLetterBinding(Queue merchantCacheInvalidationDeadLetterQueue,
			DirectExchange merchantEventsDeadLetterExchange) {
		return BindingBuilder.bind(merchantCacheInvalidationDeadLetterQueue)
				.to(merchantEventsDeadLetterExchange)
				.with(merchantCacheInvalidationDeadLetterQueue.getName());
	}

	@Bean
	SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
			@Value("${payment.gateway.merchant-events.retry.initial-interval}") Duration initialInterval,
			@Value("${payment.gateway.merchant-events.retry.multiplier}") double multiplier,
			@Value("${payment.gateway.merchant-events.retry.max-interval}") Duration maxInterval) {
		var factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setDefaultRequeueRejected(false);
		factory.setAdviceChain(RetryInterceptorBuilder.stateless()
				.maxRetries(3)
				.backOffOptions(initialInterval.toMillis(), multiplier, maxInterval.toMillis())
				.recoverer(new RejectAndDontRequeueRecoverer())
				.build());
		return factory;
	}
}
