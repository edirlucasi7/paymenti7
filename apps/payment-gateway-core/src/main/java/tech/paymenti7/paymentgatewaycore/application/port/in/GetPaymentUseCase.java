package tech.paymenti7.paymentgatewaycore.application.port.in;

import java.util.UUID;

public interface GetPaymentUseCase {

	GetPaymentResult get(UUID paymentId);
}
