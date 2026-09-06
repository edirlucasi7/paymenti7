package tech.paymenti7.paymentgatewaycore.application.port.in;

public interface SubmitPaymentUseCase {

	SubmitPaymentResult submit(SubmitPaymentCommand command);
}
