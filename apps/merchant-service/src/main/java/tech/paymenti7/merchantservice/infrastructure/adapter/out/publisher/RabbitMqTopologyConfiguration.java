package tech.paymenti7.merchantservice.infrastructure.adapter.out.publisher;

import java.util.Arrays;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqTopologyConfiguration {

	@Bean
	Declarables outboxEventExchanges() {
		var exchanges = Arrays.stream(RabbitMqEventRouting.values())
				.map(RabbitMqEventRouting::exchangeName)
				.distinct()
				.map(exchangeName -> new TopicExchange(exchangeName, true, false))
				.toList();
		return new Declarables(exchanges);
	}
}
