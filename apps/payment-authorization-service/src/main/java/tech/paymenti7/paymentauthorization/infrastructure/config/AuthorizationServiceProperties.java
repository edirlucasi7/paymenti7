package tech.paymenti7.paymentauthorization.infrastructure.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationServiceProperties {

	private static final Set<String> SUPPORTED_ACQUIRERS = Set.of("SIMULATOR_A", "SIMULATOR_B");

	private final List<String> acquirersOrder;
	private final Duration adapterTimeout;
	private final int workerBatchSize;
	private final Duration callStaleAfter;

	public AuthorizationServiceProperties(
			@Value("${payment.authorization.acquirers-order}") String acquirersOrder,
			@Value("${payment.authorization.adapter-timeout}") Duration adapterTimeout,
			@Value("${payment.authorization.worker.batch-size}") int workerBatchSize,
			@Value("${payment.authorization.worker.call-stale-after}") Duration callStaleAfter) {
		this.acquirersOrder = parseOrder(acquirersOrder);
		this.adapterTimeout = requirePositive(adapterTimeout, "adapter timeout");
		if (workerBatchSize < 1) {
			throw new IllegalArgumentException("worker batch size must be positive");
		}
		this.workerBatchSize = workerBatchSize;
		this.callStaleAfter = requirePositive(callStaleAfter, "call stale threshold");
	}

	public List<String> acquirersOrder() {
		return acquirersOrder;
	}

	public Duration adapterTimeout() {
		return adapterTimeout;
	}

	public int workerBatchSize() {
		return workerBatchSize;
	}

	public Duration callStaleAfter() {
		return callStaleAfter;
	}

	private List<String> parseOrder(String rawOrder) {
		List<String> parsed = Arrays.stream(rawOrder.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.toList();
		if (parsed.isEmpty() || new LinkedHashSet<>(parsed).size() != parsed.size()
				|| !SUPPORTED_ACQUIRERS.containsAll(parsed)) {
			throw new IllegalArgumentException("acquirers-order must contain unique supported acquirers");
		}
		return List.copyOf(parsed);
	}

	private Duration requirePositive(Duration value, String name) {
		if (value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException(name + " must be positive");
		}
		return value;
	}
}
