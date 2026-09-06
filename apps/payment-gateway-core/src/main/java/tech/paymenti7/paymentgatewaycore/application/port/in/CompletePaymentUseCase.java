package tech.paymenti7.paymentgatewaycore.application.port.in;

public interface CompletePaymentUseCase {

	void complete(CompletePaymentCommand command);
}
