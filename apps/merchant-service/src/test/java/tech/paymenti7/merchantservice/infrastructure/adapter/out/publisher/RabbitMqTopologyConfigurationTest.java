package tech.paymenti7.merchantservice.infrastructure.adapter.out.publisher;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;

class RabbitMqTopologyConfigurationTest {

	@Test
	void declaresOneDurableTopicExchangeForEachDistinctEnumExchangeName() {
		var declarables = new RabbitMqTopologyConfiguration().outboxEventExchanges();
		var exchanges = declarables.getDeclarablesByType(TopicExchange.class);
		var expectedExchangeNames = Arrays.stream(RabbitMqEventRouting.values())
				.map(RabbitMqEventRouting::exchangeName)
				.distinct()
				.toList();
		var declaredExchangeNames = exchanges.stream().map(TopicExchange::getName).toList();

		assertThat(declaredExchangeNames).containsExactlyInAnyOrderElementsOf(expectedExchangeNames);
		assertThat(exchanges).allSatisfy(exchange -> {
			assertThat(exchange.isDurable()).isTrue();
			assertThat(exchange.isAutoDelete()).isFalse();
		});
	}
}
