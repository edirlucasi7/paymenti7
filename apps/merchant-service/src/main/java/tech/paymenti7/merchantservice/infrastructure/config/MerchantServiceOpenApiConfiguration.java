package tech.paymenti7.merchantservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class MerchantServiceOpenApiConfiguration {

	@Bean
	OpenAPI merchantServiceOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Merchant Service API")
						.description("API para gerenciamento do status de merchants.")
						.version("v1"))
				.addTagsItem(new Tag().name("Merchants").description("Operações administrativas de merchants."));
	}
}
