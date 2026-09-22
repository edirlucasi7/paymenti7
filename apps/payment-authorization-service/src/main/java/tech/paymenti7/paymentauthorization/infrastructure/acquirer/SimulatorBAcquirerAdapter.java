package tech.paymenti7.paymentauthorization.infrastructure.acquirer;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatorBAcquirerAdapter extends AbstractSimulatorAcquirerAdapter {

	public SimulatorBAcquirerAdapter(
			@Value("${payment.authorization.simulators.simulator-b.outcome}") String outcome,
			@Value("${payment.authorization.simulators.simulator-b.delay}") Duration delay) {
		super("SIMULATOR_B", outcome, delay);
	}
}
