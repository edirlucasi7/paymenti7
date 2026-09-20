package tech.paymenti7.paymentgatewaycore.application.core.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.paymentgatewaycore.application.port.in.GetPaymentUseCase;
import tech.paymenti7.paymentgatewaycore.application.port.in.GetPaymentResult;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.PaymentNotFoundException;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository.PaymentJpaRepository;

@Service
public class PaymentQueryService implements GetPaymentUseCase {

	private final PaymentJpaRepository paymentRepository;

	public PaymentQueryService(PaymentJpaRepository paymentRepository) {
		this.paymentRepository = paymentRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public GetPaymentResult get(UUID paymentId) {
		var payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new PaymentNotFoundException(paymentId));
		return new GetPaymentResult(payment.getId(), payment.getStatus());
	}
}
