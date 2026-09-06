package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.publisher;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitMqConfiguration {

	public static final String PAYMENT_COMMANDS_EXCHANGE = "payment.commands";
	public static final String PAYMENT_REQUESTED_ROUTING_KEY = "payment.requested";

	@Bean
	TopicExchange paymentCommandsExchange() {
		return new TopicExchange(PAYMENT_COMMANDS_EXCHANGE, true, false);
	}
}
