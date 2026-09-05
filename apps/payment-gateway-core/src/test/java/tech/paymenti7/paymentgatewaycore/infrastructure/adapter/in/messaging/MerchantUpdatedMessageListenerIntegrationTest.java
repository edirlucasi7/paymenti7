package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.redis.testcontainers.RedisContainer;

import tools.jackson.databind.ObjectMapper;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;
import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantDetails;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache.MerchantCacheService;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.cache.MerchantCacheInvalidationService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"payment.gateway.merchant-events.retry.initial-interval=10ms",
		"payment.gateway.merchant-events.retry.max-interval=10ms"
})
@Testcontainers
class MerchantUpdatedMessageListenerIntegrationTest {

	private static final String EXCHANGE = "merchant.events";
	private static final String ROUTING_KEY = "merchant.updated";
	private static final String DLQ = "payment-gateway-core.merchant-cache-invalidation.v1.dlq";

	@Container
	static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4.3.5-management-alpine");

	@Container
	static final RedisContainer REDIS = new RedisContainer("redis:7.4-alpine");

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MerchantCacheService merchantCacheService;

	@LocalServerPort
	private int port;

	@DynamicPropertySource
	static void configureContainers(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
		registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
		registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
		registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
		registry.add("spring.data.redis.host", REDIS::getRedisHost);
		registry.add("spring.data.redis.port", REDIS::getRedisPort);
	}

	@Test
	void invalidatesMerchantCacheAndRecordsTheProcessedEvent() throws Exception {
		UUID merchantId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		redisTemplate.opsForValue().set(cacheKey(merchantId), "cached", Duration.ofMinutes(10));

		send(validMessage(eventId, merchantId));

		awaitUntil(() -> !Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey(merchantId)))
				&& Boolean.TRUE.equals(redisTemplate.hasKey(processedEventKey(eventId))));
	}

	@Test
	void ignoresARepeatedEventAfterItsFirstSuccessfulProcessing() throws Exception {
		UUID merchantId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();

		send(validMessage(eventId, merchantId));
		awaitUntil(() -> Boolean.TRUE.equals(redisTemplate.hasKey(processedEventKey(eventId))));
		redisTemplate.opsForValue().set(cacheKey(merchantId), "rehydrated", Duration.ofMinutes(10));

		send(validMessage(eventId, merchantId));
		awaitUntil(() -> "rehydrated".equals(redisTemplate.opsForValue().get(cacheKey(merchantId))));
	}

	@Test
	void preservesACacheValueWhenAnOlderEventArrivesAfterANewerOne() throws Exception {
		UUID merchantId = UUID.randomUUID();
		send(validMessage(UUID.randomUUID(), merchantId, 4L));
		awaitUntil(() -> "4".equals(redisTemplate.opsForValue().get(revisionKey(merchantId))));
		redisTemplate.opsForValue().set(cacheKey(merchantId), "newer", Duration.ofMinutes(10));
		UUID olderEventId = UUID.randomUUID();

		send(validMessage(olderEventId, merchantId, 3L));

		awaitUntil(() -> Boolean.TRUE.equals(redisTemplate.hasKey(processedEventKey(olderEventId)))
				&& "newer".equals(redisTemplate.opsForValue().get(cacheKey(merchantId))));
	}

	@Test
	void doesNotStoreAResponseOlderThanTheKnownRevision() {
		UUID merchantId = UUID.randomUUID();
		redisTemplate.opsForValue().set(revisionKey(merchantId), "4", Duration.ofHours(24));

		merchantCacheService.store(new MerchantDetails(merchantId, MerchantStatus.ACTIVE, 3L));

		assertThat(redisTemplate.opsForValue().get(cacheKey(merchantId))).isNull();
	}

	@Test
	void routesAnInvalidMessageToTheDeadLetterQueue() {
		rabbitTemplate.send(EXCHANGE, ROUTING_KEY, new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties()));

		Message deadLetter = rabbitTemplate.receive(DLQ, Duration.ofSeconds(5).toMillis());

		assertThat(deadLetter).isNotNull();
		assertThat(deadLetter.getBody()).isEqualTo("{}".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void routesAnUnknownMerchantStatusToTheDeadLetterQueue() {
		UUID merchantId = UUID.randomUUID();
		String event = """
				{"schemaVersion":1,"eventId":"%s","aggregateType":"MERCHANT","aggregateId":"%s","eventType":"MerchantUpdated","occurredAt":"%s","payload":{"merchantId":"%s","status":"SUSPENDED"}}
				""".formatted(UUID.randomUUID(), merchantId, Instant.now(), merchantId);
		rabbitTemplate.send(EXCHANGE, ROUTING_KEY,
				new Message(event.getBytes(StandardCharsets.UTF_8), new MessageProperties()));

		Message deadLetter = rabbitTemplate.receive(DLQ, Duration.ofSeconds(5).toMillis());

		assertThat(deadLetter).isNotNull();
		assertThat(deadLetter.getBody()).isEqualTo(event.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void exposesOnlyThePublicPaymentApiInOpenApi() throws Exception {
		HttpResponse<String> response = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		var specification = objectMapper.readTree(response.body());
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(specification.get("info").get("title").asText()).isEqualTo("Payment Gateway Core API");
		assertThat(specification.get("paths").has("/v1/payments")).isTrue();
		assertThat(specification.get("paths").get("/v1/payments").has("post")).isTrue();

		HttpResponse<Void> swaggerUi = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/swagger-ui.html")).GET().build(),
				HttpResponse.BodyHandlers.discarding());
		assertThat(swaggerUi.statusCode()).isBetween(300, 399);
	}

	private void send(MerchantUpdatedMessage event) throws Exception {
		byte[] body = objectMapper.writeValueAsBytes(event);
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send(EXCHANGE, ROUTING_KEY, new Message(body, properties));
	}

	private MerchantUpdatedMessage validMessage(UUID eventId, UUID merchantId) {
		return validMessage(eventId, merchantId, 3L);
	}

	private MerchantUpdatedMessage validMessage(UUID eventId, UUID merchantId, long revision) {
		return new MerchantUpdatedMessage(1, eventId, "MERCHANT", merchantId, "MerchantUpdated", Instant.now(),
				new MerchantUpdatedMessage.MerchantUpdatedPayload(merchantId, MerchantStatus.ACTIVE, revision));
	}

	private String cacheKey(UUID merchantId) {
		return MerchantCacheInvalidationService.merchantCacheKey(validMessage(UUID.randomUUID(), merchantId));
	}

	private String processedEventKey(UUID eventId) {
		return MerchantCacheInvalidationService.processedEventKey(validMessage(eventId, UUID.randomUUID()));
	}

	private String revisionKey(UUID merchantId) {
		return MerchantCacheInvalidationService.merchantRevisionKey(validMessage(UUID.randomUUID(), merchantId));
	}

	private void awaitUntil(Check condition) {
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		while (System.nanoTime() < deadline) {
			if (condition.matches()) {
				return;
			}
			try {
				Thread.sleep(25);
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupted while waiting for asynchronous message processing", exception);
			}
		}
		throw new AssertionError("Timed out waiting for asynchronous message processing");
	}

	@FunctionalInterface
	private interface Check {

		boolean matches();
	}
}
