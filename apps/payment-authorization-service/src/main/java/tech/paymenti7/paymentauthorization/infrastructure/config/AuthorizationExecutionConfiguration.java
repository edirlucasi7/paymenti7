package tech.paymenti7.paymentauthorization.infrastructure.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.paymenti7.paymentauthorization.application.port.out.AcquirerAuthorizationPort;
import tech.paymenti7.resilience.AcquirerCircuitBreaker;

@Configuration
public class AuthorizationExecutionConfiguration {

	@Bean(destroyMethod = "close")
	ExecutorService acquirerExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	@Bean
	Map<String, AcquirerAuthorizationPort> acquirerAdapters(List<AcquirerAuthorizationPort> adapters,
			AuthorizationServiceProperties properties) {
		Map<String, AcquirerAuthorizationPort> byName = adapters.stream()
				.collect(Collectors.toUnmodifiableMap(AcquirerAuthorizationPort::name, Function.identity()));
		if (!byName.keySet().containsAll(properties.acquirersOrder())) {
			throw new IllegalStateException("No adapter is registered for every configured acquirer");
		}
		return byName;
	}

	@Bean
	Map<String, AcquirerCircuitBreaker> acquirerCircuitBreakers(AuthorizationServiceProperties properties,
			MeterRegistry meterRegistry,
			@Value("${payment.authorization.circuit-breaker.closed-window}") Duration closedWindow,
			@Value("${payment.authorization.circuit-breaker.minimum-calls}") int minimumCalls,
			@Value("${payment.authorization.circuit-breaker.open-failure-rate}") double openFailureRate,
			@Value("${payment.authorization.circuit-breaker.open-duration}") Duration openDuration,
			@Value("${payment.authorization.circuit-breaker.probe-sample-percent}") int probeSamplePercent,
			@Value("${payment.authorization.circuit-breaker.maximum-concurrent-probes}") int maximumConcurrentProbes,
			@Value("${payment.authorization.circuit-breaker.required-completed-probes}") int requiredCompletedProbes,
			@Value("${payment.authorization.circuit-breaker.recovery-success-rate}") double recoverySuccessRate,
			@Value("${payment.authorization.circuit-breaker.minimum-recovery-observation}") Duration minimumObservation,
			@Value("${payment.authorization.circuit-breaker.maximum-recovery-observation}") Duration maximumObservation) {
		var configuration = new AcquirerCircuitBreaker.Configuration(closedWindow, minimumCalls, openFailureRate,
				openDuration, probeSamplePercent, maximumConcurrentProbes, requiredCompletedProbes,
				recoverySuccessRate, minimumObservation, maximumObservation);
		return properties.acquirersOrder().stream().collect(Collectors.toUnmodifiableMap(Function.identity(), name -> {
			var breaker = new AcquirerCircuitBreaker(name, configuration);
			breaker.delegate().getEventPublisher().onStateTransition(event -> Counter
					.builder("payment.authorization.circuit.transitions")
					.tag("acquirer", name)
					.tag("from", event.getStateTransition().getFromState().name())
					.tag("to", event.getStateTransition().getToState().name())
					.register(meterRegistry).increment());
			Gauge.builder("payment.authorization.circuit.state", breaker,
					value -> value.state().getOrder()).tag("acquirer", name).register(meterRegistry);
			Gauge.builder("payment.authorization.circuit.failure.rate", breaker,
					value -> value.delegate().getMetrics().getFailureRate()).tag("acquirer", name)
					.register(meterRegistry);
			Gauge.builder("payment.authorization.circuit.probes.inflight", breaker,
					AcquirerCircuitBreaker::inFlightProbes).tag("acquirer", name).register(meterRegistry);
			Gauge.builder("payment.authorization.circuit.probes.completed", breaker,
					AcquirerCircuitBreaker::completedProbes).tag("acquirer", name).register(meterRegistry);
			return breaker;
		}));
	}
}
