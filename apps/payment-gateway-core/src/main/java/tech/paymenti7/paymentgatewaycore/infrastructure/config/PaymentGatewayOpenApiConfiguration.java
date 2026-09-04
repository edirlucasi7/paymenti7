package tech.paymenti7.paymentgatewaycore.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class PaymentGatewayOpenApiConfiguration {

	@Bean
	OpenAPI paymentGatewayOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Payment Gateway Core API")
							.description("API de validação de merchant para o fluxo de pagamento.")
							.version("v1"))
				.addTagsItem(new Tag().name("Payments").description("Validação de merchants antes do processamento."));
	}
}
