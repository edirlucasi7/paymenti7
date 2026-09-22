package tech.paymenti7.paymentauthorization.application.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tech.paymenti7.paymentauthorization.application.domain.AuthorizationOutcome;
import tech.paymenti7.paymentauthorization.application.port.out.AcquirerAuthorizationPort;
import tech.paymenti7.paymentauthorization.application.port.out.AcquirerAuthorizationPort.AcquirerResult;
import tech.paymenti7.paymentauthorization.infrastructure.config.AuthorizationServiceProperties;
import tech.paymenti7.resilience.AcquirerCircuitBreaker;

@Component
public class AuthorizationRoutingWorker {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationRoutingWorker.class);

	private final AuthorizationStateService stateService;
	private final AuthorizationServiceProperties properties;
	private final Map<String, AcquirerAuthorizationPort> adapters;
	private final Map<String, AcquirerCircuitBreaker> circuitBreakers;
	private final ExecutorService executor;
	private final MeterRegistry meterRegistry;

	public AuthorizationRoutingWorker(AuthorizationStateService stateService,
			AuthorizationServiceProperties properties, Map<String, AcquirerAuthorizationPort> acquirerAdapters,
			Map<String, AcquirerCircuitBreaker> acquirerCircuitBreakers, ExecutorService acquirerExecutor,
			MeterRegistry meterRegistry) {
		this.stateService = stateService;
		this.properties = properties;
		this.adapters = acquirerAdapters;
		this.circuitBreakers = acquirerCircuitBreakers;
		this.executor = acquirerExecutor;
		this.meterRegistry = meterRegistry;
	}

	@Scheduled(fixedDelayString = "${payment.authorization.worker.polling-delay}")
	public void routePending() {
		for (UUID paymentId : stateService.pendingPaymentIds(properties.workerBatchSize())) {
			process(paymentId);
		}
	}

	@Scheduled(fixedDelayString = "${payment.authorization.worker.polling-delay}")
	public void recoverStaleCallsAndEvaluateBreakers() {
		Instant staleBefore = Instant.now().minus(properties.callStaleAfter());
		for (UUID paymentId : stateService.staleCalls(staleBefore, properties.workerBatchSize())) {
			stateService.markStaleCallUnknown(paymentId);
			LOGGER.warn("Authorization requires reconciliation after an interrupted call: paymentId={}", paymentId);
		}
		circuitBreakers.values().forEach(AcquirerCircuitBreaker::evaluateRecoveryWindow);
	}

	private void process(UUID paymentId) {
		var route = stateService.pendingRoute(paymentId).orElse(null);
		if (route == null || route.routeIndex() >= properties.acquirersOrder().size()) {
			return;
		}
		String acquirer = properties.acquirersOrder().get(route.routeIndex());
		AcquirerCircuitBreaker breaker = circuitBreakers.get(acquirer);
		var permit = breaker.tryAcquire(paymentId);
		if (!permit.allowed()) {
			counter("skipped", acquirer).increment();
			stateService.skipUnavailableRoute(paymentId, route.routeIndex(), properties.acquirersOrder().size());
			return;
		}

		var prepared = stateService.prepareAttempt(paymentId, route.routeIndex(), acquirer).orElse(null);
		if (prepared == null) {
			breaker.cancel(permit);
			return;
		}
		AcquirerResult result = call(adapters.get(acquirer), prepared.command());
		breaker.record(permit, result.outcome().isTechnicalSuccess());
		counter(result.outcome().name().toLowerCase(), acquirer).increment();
		stateService.recordResult(paymentId, prepared.attemptId(), result, properties.acquirersOrder().size());
		if (result.outcome() == AuthorizationOutcome.UNKNOWN) {
			LOGGER.warn("Authorization result is uncertain and requires reconciliation: paymentId={}, acquirer={}",
					paymentId, acquirer);
		}
	}

	private AcquirerResult call(AcquirerAuthorizationPort adapter,
			AcquirerAuthorizationPort.AcquirerCommand command) {
		var future = executor.submit(() -> adapter.authorize(command));
		try {
			return future.get(properties.adapterTimeout().toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException exception) {
			future.cancel(true);
			return new AcquirerResult(AuthorizationOutcome.UNKNOWN, null, "TIMEOUT");
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return new AcquirerResult(AuthorizationOutcome.UNKNOWN, null, "WORKER_INTERRUPTED");
		}
		catch (ExecutionException exception) {
			return new AcquirerResult(AuthorizationOutcome.UNKNOWN, null, "TECHNICAL_ERROR");
		}
	}

	private Counter counter(String outcome, String acquirer) {
		return Counter.builder("payment.authorization.routing.decisions")
				.tag("outcome", outcome).tag("acquirer", acquirer).register(meterRegistry);
	}
}
