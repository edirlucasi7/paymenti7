package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantDetails;
import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;
import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;
import tech.paymenti7.paymentgatewaycore.application.core.service.MerchantStatusResolutionService;
import tech.paymenti7.paymentgatewaycore.application.port.in.CompletePaymentCommand;
import tech.paymenti7.paymentgatewaycore.application.port.in.CompletePaymentUseCase;
import tech.paymenti7.paymentgatewaycore.application.port.in.SubmitPaymentCommand;
import tech.paymenti7.paymentgatewaycore.application.port.in.SubmitPaymentUseCase;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.IdempotencyConflictException;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.IdempotencyRequestJpaRepository;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.PaymentJpaRepository;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.PaymentOutboxEventJpaRepository;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.publisher.PaymentRabbitMqConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"payment.gateway.outbox.polling-delay=1h",
		"payment.gateway.idempotency.cleanup-delay=1h"
})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentIdempotencyIntegrationTest {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4.3.5-management-alpine");

	@DynamicPropertySource
	static void configureContainers(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
		registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
		registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
		registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
	}

	@MockitoBean
	private MerchantStatusResolutionService merchantStatusResolutionService;

	@LocalServerPort
	private int port;

	@Autowired
	private SubmitPaymentUseCase submitPaymentUseCase;

	@Autowired
	private CompletePaymentUseCase completePaymentUseCase;

	@Autowired
	private PaymentOutboxProcessingService outboxProcessingService;

	@Autowired
	private IdempotencyCleanupService cleanupService;

	@Autowired
	private IdempotencyRequestJpaRepository idempotencyRequestRepository;

	@Autowired
	private PaymentJpaRepository paymentRepository;

	@Autowired
	private PaymentOutboxEventJpaRepository outboxEventRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@BeforeEach
	void cleanDatabaseAndActivateMerchants() {
		outboxEventRepository.deleteAll();
		idempotencyRequestRepository.deleteAll();
		paymentRepository.deleteAll();
		when(merchantStatusResolutionService.resolve(any())).thenAnswer(invocation -> {
			UUID merchantId = invocation.getArgument(0);
			return new MerchantDetails(merchantId, MerchantStatus.ACTIVE, 1L);
		});
	}

	@Test
	void persistsOnceReplaysTheSameResponseAndPublishesOneCommand() {
		String queueName = "test.payment.requested." + UUID.randomUUID();
		amqpAdmin.declareQueue(new Queue(queueName, false, true, true));
		amqpAdmin.declareBinding(new Binding(queueName, Binding.DestinationType.QUEUE,
				PaymentRabbitMqConfiguration.PAYMENT_COMMANDS_EXCHANGE,
				PaymentRabbitMqConfiguration.PAYMENT_REQUESTED_ROUTING_KEY, null));
		var command = command(UUID.randomUUID(), UUID.randomUUID(), "10.00");

		var first = submitPaymentUseCase.submit(command);
		var repeated = submitPaymentUseCase.submit(command);
		outboxProcessingService.publishPending(20);
		Message published = rabbitTemplate.receive(queueName, Duration.ofSeconds(5).toMillis());

		assertThat(first.httpStatus()).isEqualTo(202);
		assertThat(repeated).isEqualTo(first);
		assertThat(paymentRepository.count()).isEqualTo(1);
		assertThat(idempotencyRequestRepository.count()).isEqualTo(1);
		assertThat(outboxEventRepository.count()).isEqualTo(1);
		assertThat(published).isNotNull();
		assertThat(new String(published.getBody(), StandardCharsets.UTF_8))
				.contains("PaymentRequested", first.paymentId().toString());
	}

	@Test
	void requiresTheHeaderAndExposesTheAcceptedResponseOverHttp() throws Exception {
		UUID merchantId = UUID.randomUUID();
		String body = """
				{"merchantId":"%s","amount":125.90,"currency":"BRL"}
				""".formatted(merchantId);
		var missingKey = postPayment(body, null);
		UUID idempotencyKey = UUID.randomUUID();
		var accepted = postPayment(body, idempotencyKey);

		assertThat(missingKey.statusCode()).isEqualTo(400);
		assertThat(accepted.statusCode()).isEqualTo(202);
		assertThat(accepted.body()).contains("paymentId", "PROCESSING");
	}

	@Test
	void rejectsTheSameKeyWithADifferentPayload() {
		UUID merchantId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();
		submitPaymentUseCase.submit(command(merchantId, idempotencyKey, "10.00"));

		assertThatThrownBy(() -> submitPaymentUseCase.submit(command(merchantId, idempotencyKey, "11.00")))
				.isInstanceOf(IdempotencyConflictException.class);
		assertThat(paymentRepository.count()).isEqualTo(1);
	}

	@Test
	void concurrentRequestsStartOnlyOnePayment() throws Exception {
		var command = command(UUID.randomUUID(), UUID.randomUUID(), "25.90");
		var ready = new CountDownLatch(2);
		var start = new CountDownLatch(1);

		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> submitTogether(command, ready, start));
			var second = executor.submit(() -> submitTogether(command, ready, start));
			ready.await();
			start.countDown();

			assertThat(first.get().paymentId()).isEqualTo(second.get().paymentId());
		}

		assertThat(paymentRepository.count()).isEqualTo(1);
		assertThat(idempotencyRequestRepository.count()).isEqualTo(1);
		assertThat(outboxEventRepository.count()).isEqualTo(1);
	}

	@Test
	void replaysATerminalResponseAndCleansItOnlyAfterExpiration() {
		var terminalCommand = command(UUID.randomUUID(), UUID.randomUUID(), "35.00");
		var processingCommand = command(UUID.randomUUID(), UUID.randomUUID(), "40.00");
		var terminal = submitPaymentUseCase.submit(terminalCommand);
		var processing = submitPaymentUseCase.submit(processingCommand);
		completePaymentUseCase.complete(new CompletePaymentCommand(terminal.paymentId(), PaymentStatus.APPROVED, 200,
				Map.of("paymentId", terminal.paymentId().toString(), "status", "APPROVED"),
				Map.of("Payment-Result", "replayed")));
		jdbcTemplate.update("""
				UPDATE idempotency_requests
				SET completed_at = CURRENT_TIMESTAMP - INTERVAL '25 hours',
					expires_at = CURRENT_TIMESTAMP - INTERVAL '1 hour'
				WHERE payment_id = ?
				""", terminal.paymentId());

		var replayed = submitPaymentUseCase.submit(terminalCommand);
		int deleted = cleanupService.deleteExpiredTerminalRequests(100);

		assertThat(replayed.paymentId()).isEqualTo(terminal.paymentId());
		assertThat(replayed.status()).isEqualTo(PaymentStatus.APPROVED);
		assertThat(replayed.httpStatus()).isEqualTo(200);
		assertThat(replayed.responseBody()).containsEntry("status", "APPROVED");
		assertThat(replayed.responseHeaders()).containsEntry("Payment-Result", "replayed");
		assertThat(deleted).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM idempotency_requests WHERE payment_id = ? AND status = 'PROCESSING'",
				Long.class, processing.paymentId())).isEqualTo(1);
		assertThat(idempotencyRequestRepository.count()).isEqualTo(1);
		assertThat(paymentRepository.count()).isEqualTo(2);
	}

	private tech.paymenti7.paymentgatewaycore.application.port.in.SubmitPaymentResult submitTogether(
			SubmitPaymentCommand command, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
		ready.countDown();
		start.await();
		return submitPaymentUseCase.submit(command);
	}

	private SubmitPaymentCommand command(UUID merchantId, UUID idempotencyKey, String amount) {
		return new SubmitPaymentCommand(merchantId, new BigDecimal(amount), "BRL", idempotencyKey);
	}

	private HttpResponse<String> postPayment(String body, UUID idempotencyKey) throws Exception {
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/payments"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body));
		if (idempotencyKey != null) {
			request.header("Idempotency-Key", idempotencyKey.toString());
		}
		return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
	}
}
