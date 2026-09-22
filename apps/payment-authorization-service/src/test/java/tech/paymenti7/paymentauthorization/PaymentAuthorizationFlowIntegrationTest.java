package tech.paymenti7.paymentauthorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import tech.paymenti7.paymentauthorization.application.domain.AuthorizationOutcome;
import tech.paymenti7.paymentauthorization.application.port.out.AcquirerAuthorizationPort.AcquirerResult;
import tech.paymenti7.paymentauthorization.application.service.AuthorizationRoutingWorker;
import tech.paymenti7.paymentauthorization.application.service.AuthorizationStateService;
import tech.paymenti7.paymentauthorization.application.service.PaymentRequestedInboxService;
import tech.paymenti7.paymentauthorization.infrastructure.messaging.PaymentRequestedMessage;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.AuthorizationAttemptJpaRepository;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.AuthorizationOutboxEventJpaRepository;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.AuthorizationRequestJpaRepository;
import tech.paymenti7.paymentauthorization.infrastructure.persistence.repository.InboxEventJpaRepository;

@SpringBootTest(properties = {
		"payment.authorization.worker.polling-delay=1h",
		"payment.authorization.outbox.polling-delay=1h",
		"payment.authorization.simulators.simulator-a.outcome=SAFE_TO_FALLBACK",
		"payment.authorization.simulators.simulator-b.outcome=APPROVED"
})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentAuthorizationFlowIntegrationTest {

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

	@Autowired PaymentRequestedInboxService inboxService;
	@Autowired AuthorizationRoutingWorker routingWorker;
	@Autowired AuthorizationStateService stateService;
	@Autowired InboxEventJpaRepository inboxRepository;
	@Autowired AuthorizationRequestJpaRepository authorizationRepository;
	@Autowired AuthorizationAttemptJpaRepository attemptRepository;
	@Autowired AuthorizationOutboxEventJpaRepository outboxRepository;
	@Autowired JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		outboxRepository.deleteAll();
		attemptRepository.deleteAll();
		authorizationRepository.deleteAll();
		inboxRepository.deleteAll();
	}

	@Test
	void deduplicatesTheCommandAndFallsBackOnlyAfterAnExplicitlySafeOutcome() {
		UUID paymentId = UUID.randomUUID();
		var message = message(UUID.randomUUID(), paymentId);

		assertThat(inboxService.accept(message)).isTrue();
		assertThat(inboxService.accept(message)).isFalse();
		routingWorker.routePending();
		routingWorker.routePending();

		assertThat(authorizationRepository.count()).isEqualTo(1);
		assertThat(attemptRepository.count()).isEqualTo(2);
		assertThat(outboxRepository.count()).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT terminal_status FROM authorization_requests WHERE payment_id = ?", String.class, paymentId))
				.isEqualTo("APPROVED");
	}

	@Test
	void unknownOutcomeStopsRoutingAndWaitsForReconciliation() {
		UUID paymentId = UUID.randomUUID();
		inboxService.accept(message(UUID.randomUUID(), paymentId));
		var attempt = stateService.prepareAttempt(paymentId, 0, "SIMULATOR_A").orElseThrow();

		stateService.recordResult(paymentId, attempt.attemptId(),
				new AcquirerResult(AuthorizationOutcome.UNKNOWN, null, "TIMEOUT"), 2);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT status FROM authorization_requests WHERE payment_id = ?", String.class, paymentId))
				.isEqualTo("PENDING_RECONCILIATION");
		assertThat(attemptRepository.count()).isEqualTo(1);
		assertThat(outboxRepository.count()).isZero();
	}

	@Test
	void businessDeclineIsTerminalWithoutTryingTheSecondAcquirer() {
		UUID paymentId = UUID.randomUUID();
		inboxService.accept(message(UUID.randomUUID(), paymentId));
		var attempt = stateService.prepareAttempt(paymentId, 0, "SIMULATOR_A").orElseThrow();

		stateService.recordResult(paymentId, attempt.attemptId(),
				new AcquirerResult(AuthorizationOutcome.DECLINED, "provider-ref", "DO_NOT_HONOR"), 2);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT terminal_status FROM authorization_requests WHERE payment_id = ?", String.class, paymentId))
				.isEqualTo("DECLINED");
		assertThat(attemptRepository.count()).isEqualTo(1);
		assertThat(outboxRepository.count()).isEqualTo(1);
	}

	@Test
	void failsWhenSafeFallbackHasNoRemainingRoute() {
		UUID paymentId = UUID.randomUUID();
		inboxService.accept(message(UUID.randomUUID(), paymentId));
		var attempt = stateService.prepareAttempt(paymentId, 0, "SIMULATOR_A").orElseThrow();

		stateService.recordResult(paymentId, attempt.attemptId(),
				new AcquirerResult(AuthorizationOutcome.SAFE_TO_FALLBACK, null, "UNAVAILABLE"), 1);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT terminal_status FROM authorization_requests WHERE payment_id = ?", String.class, paymentId))
				.isEqualTo("FAILED");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT status FROM authorization_attempts WHERE payment_id = ?", String.class, paymentId))
				.isEqualTo("SAFE_TO_FALLBACK");
		assertThat(outboxRepository.count()).isEqualTo(1);
	}

	@Test
	void skipsAnUnavailableRouteWithoutCreatingAnAttempt() {
		UUID paymentId = UUID.randomUUID();
		inboxService.accept(message(UUID.randomUUID(), paymentId));

		stateService.skipUnavailableRoute(paymentId, 0, 2);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT status FROM authorization_requests WHERE payment_id = ?", String.class, paymentId))
				.isEqualTo("PENDING_ROUTING");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT next_route_index FROM authorization_requests WHERE payment_id = ?", Integer.class, paymentId))
				.isEqualTo(1);
		assertThat(attemptRepository.count()).isZero();
		assertThat(outboxRepository.count()).isZero();
	}

	@Test
	void failsOnceWhenAllRoutesAreUnavailable() {
		UUID paymentId = UUID.randomUUID();
		inboxService.accept(message(UUID.randomUUID(), paymentId));

		stateService.skipUnavailableRoute(paymentId, 0, 2);
		stateService.skipUnavailableRoute(paymentId, 1, 2);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT terminal_status FROM authorization_requests WHERE payment_id = ?", String.class, paymentId))
				.isEqualTo("FAILED");
		assertThat(attemptRepository.count()).isZero();
		assertThat(outboxRepository.count()).isEqualTo(1);
	}

	private PaymentRequestedMessage message(UUID eventId, UUID paymentId) {
		return new PaymentRequestedMessage(2, eventId, "PAYMENT", paymentId, "PaymentRequested", Instant.now(),
				new PaymentRequestedMessage.Payload(paymentId, UUID.randomUUID(), new BigDecimal("125.90"),
						"BRL", "pmt_test_token"));
	}
}
