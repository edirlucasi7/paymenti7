package tech.paymenti7.paymentauthorization.infrastructure.acquirer;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatorAAcquirerAdapter extends AbstractSimulatorAcquirerAdapter {

	public SimulatorAAcquirerAdapter(
			@Value("${payment.authorization.simulators.simulator-a.outcome}") String outcome,
			@Value("${payment.authorization.simulators.simulator-a.delay}") Duration delay) {
		super("SIMULATOR_A", outcome, delay);
	}
}
