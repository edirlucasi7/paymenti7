package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.merchant;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantDetails;
import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.MerchantNotFoundException;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.MerchantServiceUnavailableException;

@Component
public class MerchantServiceClient {

	private final RestClient merchantServiceRestClient;

	public MerchantServiceClient(RestClient merchantServiceRestClient) {
		this.merchantServiceRestClient = merchantServiceRestClient;
	}

	public MerchantDetails getMerchant(UUID merchantId) {
		try {
			var response = merchantServiceRestClient.get()
					.uri("/internal/v2/merchants/{merchantId}", merchantId)
					.retrieve()
					.body(MerchantServiceMerchantResponse.class);
			if (response == null || !merchantId.equals(response.id()) || response.status() == null || response.revision() == null
					|| response.revision() < 0) {
				throw new MerchantServiceUnavailableException("Merchant service returned an invalid merchant response");
			}
			return new MerchantDetails(response.id(), MerchantStatus.valueOf(response.status()), response.revision());
		}
		catch (RestClientResponseException exception) {
			if (exception.getStatusCode().value() == 404) {
				throw new MerchantNotFoundException(merchantId);
			}
			throw new MerchantServiceUnavailableException("Merchant service request failed", exception);
		}
		catch (RestClientException | IllegalArgumentException exception) {
			throw new MerchantServiceUnavailableException("Merchant service is unavailable", exception);
		}
	}

}
