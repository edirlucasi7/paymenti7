package tech.paymenti7.paymentgatewaycore.infrastructure.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MerchantServiceHttpConfiguration {

	@Bean
	RestClient merchantServiceRestClient(
			@Value("${payment.gateway.merchant-service.base-url}") String merchantServiceBaseUrl,
			@Value("${payment.gateway.merchant-service.timeout}") Duration timeout) {
		var httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(timeout);
		return RestClient.builder().baseUrl(merchantServiceBaseUrl).requestFactory(requestFactory).build();
	}
}
