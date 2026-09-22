package tech.paymenti7.paymentgatewaycore.application.core.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.stereotype.Service;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;
import tech.paymenti7.paymentgatewaycore.application.port.in.SubmitPaymentCommand;
import tech.paymenti7.paymentgatewaycore.application.port.in.SubmitPaymentResult;
import tech.paymenti7.paymentgatewaycore.application.port.in.SubmitPaymentUseCase;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.InactiveMerchantException;
import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.transaction.TransactionalPaymentSubmission;

@Service
public class SubmitPaymentService implements SubmitPaymentUseCase {

	private final MerchantStatusResolutionService merchantStatusResolutionService;
	private final TransactionalPaymentSubmission transactionalPaymentSubmission;

	public SubmitPaymentService(MerchantStatusResolutionService merchantStatusResolutionService,
			TransactionalPaymentSubmission transactionalPaymentSubmission) {
		this.merchantStatusResolutionService = merchantStatusResolutionService;
		this.transactionalPaymentSubmission = transactionalPaymentSubmission;
	}

	@Override
	public SubmitPaymentResult submit(SubmitPaymentCommand command) {
		BigDecimal normalizedAmount = command.amount().stripTrailingZeros();
		String normalizedCurrency = command.currency().toUpperCase(Locale.ROOT);
		String requestHash = requestHash(command.merchantId().toString(), normalizedAmount.toPlainString(),
				normalizedCurrency, command.paymentMethodToken());
		var existing = transactionalPaymentSubmission.findExisting(command.merchantId(), command.idempotencyKey(), requestHash);
		if (existing.isPresent()) {
			return existing.get();
		}

		var merchant = merchantStatusResolutionService.resolve(command.merchantId());
		if (merchant.status() != MerchantStatus.ACTIVE) {
			throw new InactiveMerchantException(command.merchantId());
		}

		return transactionalPaymentSubmission.submit(command.merchantId(), normalizedAmount, normalizedCurrency,
				command.paymentMethodToken(), command.idempotencyKey(), requestHash);
	}

	private String requestHash(String merchantId, String amount, String currency, String paymentMethodToken) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest((merchantId + "\n" + amount + "\n" + currency + "\n" + paymentMethodToken)
					.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
