package tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.repository;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import tech.paymenti7.merchantservice.application.core.domain.Merchant;
import tech.paymenti7.merchantservice.application.core.domain.MerchantStatus;
import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;
import tech.paymenti7.merchantservice.application.core.domain.OutboxEventDeliveryStatus;
import tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.entity.MerchantEntity;
import tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PostgresSqlRepositoryIntegrationTest {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

	@Autowired
	private MerchantJpaRepository merchantJpaRepository;

	@Autowired
	private OutboxEventJpaRepository outboxEventJpaRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Test
	void persistsAndFindsMerchantById() {
		Instant createdAt = Instant.parse("2026-08-29T10:00:00Z");
		Merchant merchant = new Merchant(uuid(1), MerchantStatus.ACTIVE, createdAt, createdAt.plusSeconds(30));

		merchantJpaRepository.saveAndFlush(MerchantEntity.fromDomain(merchant));

		Merchant persistedMerchant = merchantJpaRepository.findById(merchant.id()).orElseThrow().toDomain();
		assertThat(persistedMerchant.id()).isEqualTo(merchant.id());
		assertThat(persistedMerchant.status()).isEqualTo(merchant.status());
		assertThat(persistedMerchant.createdAt()).isEqualTo(merchant.createdAt());
		assertThat(persistedMerchant.updatedAt()).isEqualTo(merchant.updatedAt());
	}

	@Test
	void locksOnlyTheLatestEligibleEventPerMerchantInOrderAndWithinBatchSize() {
		UUID firstMerchant = saveMerchant(10);
		UUID secondMerchant = saveMerchant(20);
		UUID thirdMerchant = saveMerchant(30);
		Instant baseTime = Instant.parse("2026-08-29T10:00:00Z");

		saveEvent(101, firstMerchant, baseTime, OutboxEventDeliveryStatus.PENDING, null);
		OutboxEventEntity firstMerchantLatest = saveEvent(102, firstMerchant, baseTime.plusSeconds(20),
				OutboxEventDeliveryStatus.PENDING, null);
		OutboxEventEntity secondMerchantLatest = saveEvent(201, secondMerchant, baseTime.plusSeconds(10),
				OutboxEventDeliveryStatus.PENDING, null);
		saveEvent(301, thirdMerchant, baseTime.plusSeconds(30), OutboxEventDeliveryStatus.PUBLISHED,
				baseTime.plusSeconds(31));
		saveEvent(302, thirdMerchant, baseTime.plusSeconds(40), OutboxEventDeliveryStatus.PENDING,
				baseTime.plusSeconds(41));

		List<OutboxEventEntity> events = outboxEventJpaRepository.lockLatestPendingEventsForAggregateId(2);

		assertThat(events)
				.extracting(event -> event.toDomain().id())
				.containsExactly(secondMerchantLatest.toDomain().id(), firstMerchantLatest.toDomain().id());
	}

	@Test
	void suppressesOnlyOlderPendingEventsAndKeepsTheLatestEvent() {
		UUID merchantId = saveMerchant(40);
		Instant baseTime = Instant.parse("2026-08-29T10:00:00Z");
		OutboxEventEntity olderEvent = saveEvent(401, merchantId, baseTime, OutboxEventDeliveryStatus.PENDING, null);
		OutboxEventEntity tieBreakerOlderEvent = saveEvent(402, merchantId, baseTime.plusSeconds(10),
				OutboxEventDeliveryStatus.PENDING, null);
		OutboxEventEntity latestEvent = saveEvent(403, merchantId, baseTime.plusSeconds(10),
				OutboxEventDeliveryStatus.PENDING, null);
		OutboxEventEntity publishedEvent = saveEvent(404, merchantId, baseTime.plusSeconds(5),
				OutboxEventDeliveryStatus.PUBLISHED, baseTime.plusSeconds(6));
		Instant processedAt = baseTime.plusSeconds(60);

		outboxEventJpaRepository.suppressOlderPendingEvents(merchantId, latestEvent.toDomain().id(),
				latestEvent.toDomain().occurredAt(), processedAt);

		assertEvent(olderEvent.toDomain().id(), OutboxEventDeliveryStatus.SUPPRESSED, processedAt, latestEvent.toDomain().id());
		assertEvent(tieBreakerOlderEvent.toDomain().id(), OutboxEventDeliveryStatus.SUPPRESSED, processedAt,
				latestEvent.toDomain().id());
		assertEvent(latestEvent.toDomain().id(), OutboxEventDeliveryStatus.PENDING, null, null);
		assertEvent(publishedEvent.toDomain().id(), OutboxEventDeliveryStatus.PUBLISHED, baseTime.plusSeconds(6), null);
	}

	@Test
	void marksOnlyAnUnprocessedPendingEventAsPublished() {
		UUID merchantId = saveMerchant(50);
		Instant baseTime = Instant.parse("2026-08-29T10:00:00Z");
		OutboxEventEntity pendingEvent = saveEvent(501, merchantId, baseTime, OutboxEventDeliveryStatus.PENDING, null);
		OutboxEventEntity alreadyPublished = saveEvent(502, merchantId, baseTime.plusSeconds(1),
				OutboxEventDeliveryStatus.PUBLISHED, baseTime.plusSeconds(2));
		OutboxEventEntity alreadyProcessed = saveEvent(503, merchantId, baseTime.plusSeconds(3),
				OutboxEventDeliveryStatus.PENDING, baseTime.plusSeconds(4));
		Instant publishedAt = baseTime.plusSeconds(60);

		outboxEventJpaRepository.markPublished(pendingEvent.toDomain().id(), publishedAt);
		outboxEventJpaRepository.markPublished(alreadyPublished.toDomain().id(), publishedAt);
		outboxEventJpaRepository.markPublished(alreadyProcessed.toDomain().id(), publishedAt);

		assertEvent(pendingEvent.toDomain().id(), OutboxEventDeliveryStatus.PUBLISHED, publishedAt, null);
		assertEvent(alreadyPublished.toDomain().id(), OutboxEventDeliveryStatus.PUBLISHED, baseTime.plusSeconds(2), null);
		assertEvent(alreadyProcessed.toDomain().id(), OutboxEventDeliveryStatus.PENDING, baseTime.plusSeconds(4), null);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void skipsAnEventLockedByAnotherTransaction() throws Exception {
		UUID merchantId = uuid(60);
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		transaction.executeWithoutResult(status -> {
			merchantJpaRepository.save(MerchantEntity.fromDomain(new Merchant(merchantId, MerchantStatus.ACTIVE,
					Instant.parse("2026-08-29T10:00:00Z"), Instant.parse("2026-08-29T10:00:00Z"))));
			saveEvent(601, merchantId, Instant.parse("2026-08-29T10:01:00Z"), OutboxEventDeliveryStatus.PENDING, null);
		});

		CountDownLatch lockAcquired = new CountDownLatch(1);
		CountDownLatch releaseLock = new CountDownLatch(1);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<List<UUID>> lockHolder = executor.submit(() -> transaction.execute(status -> {
			List<UUID> lockedIds = outboxEventJpaRepository.lockLatestPendingEventsForAggregateId(1).stream()
					.map(event -> event.toDomain().id())
					.toList();
			lockAcquired.countDown();
			try {
				if (!releaseLock.await(10, SECONDS)) {
					throw new IllegalStateException("Timed out waiting to release PostgreSQL row lock");
				}
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while holding PostgreSQL row lock", exception);
			}
			return lockedIds;
		}));

		try {
			assertThat(lockAcquired.await(10, SECONDS)).isTrue();
			List<OutboxEventEntity> skippedEvents = transaction.execute(
					status -> outboxEventJpaRepository.lockLatestPendingEventsForAggregateId(1));

			assertThat(skippedEvents).isEmpty();
			releaseLock.countDown();
			assertThat(lockHolder.get(10, SECONDS)).containsExactly(uuid(601));
		} finally {
			releaseLock.countDown();
			executor.shutdownNow();
			transaction.executeWithoutResult(status -> {
				outboxEventJpaRepository.deleteAll();
				merchantJpaRepository.deleteAll();
			});
		}
	}

	private UUID saveMerchant(long id) {
		UUID merchantId = uuid(id);
		Instant createdAt = Instant.parse("2026-08-29T10:00:00Z");
		merchantJpaRepository.saveAndFlush(MerchantEntity.fromDomain(
				new Merchant(merchantId, MerchantStatus.ACTIVE, createdAt, createdAt)));
		return merchantId;
	}

	private OutboxEventEntity saveEvent(long id, UUID merchantId, Instant occurredAt, OutboxEventDeliveryStatus deliveryStatus,
			Instant processedAt) {
		OutboxEvent event = OutboxEvent.rehydrate(uuid(id), "MERCHANT", merchantId, "MerchantUpdated",
				Map.of("merchantId", merchantId.toString(), "status", MerchantStatus.ACTIVE.name()), occurredAt, deliveryStatus,
				processedAt, null);
		return outboxEventJpaRepository.saveAndFlush(OutboxEventEntity.fromDomain(event));
	}

	private void assertEvent(UUID eventId, OutboxEventDeliveryStatus deliveryStatus, Instant processedAt, UUID supersededByEventId) {
		OutboxEvent event = outboxEventJpaRepository.findById(eventId).orElseThrow().toDomain();
		assertThat(event.deliveryStatus()).isEqualTo(deliveryStatus);
		assertThat(event.processedAt()).isEqualTo(processedAt);
		assertThat(event.supersededByEventId()).isEqualTo(supersededByEventId);
	}

	private static UUID uuid(long value) {
		return new UUID(0, value);
	}
}
