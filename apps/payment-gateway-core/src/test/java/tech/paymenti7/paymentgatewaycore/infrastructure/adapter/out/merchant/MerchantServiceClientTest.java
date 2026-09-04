package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.merchant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;
import tech.paymenti7.paymentgatewaycore.application.shared.exception.MerchantNotFoundException;

class MerchantServiceClientTest {

	private HttpServer server;
	private MerchantServiceClient merchantServiceClient;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.start();
		merchantServiceClient = new MerchantServiceClient(RestClient.builder()
				.baseUrl("http://localhost:" + server.getAddress().getPort())
				.build());
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void deserializesTheMerchantReturnedByTheInternalApi() {
		UUID merchantId = UUID.randomUUID();
		server.createContext("/internal/v1/merchants/" + merchantId,
				exchange -> respond(exchange, 200, "{\"id\":\"" + merchantId + "\",\"status\":\"ACTIVE\"}"));

		var merchant = merchantServiceClient.getMerchant(merchantId);

		assertThat(merchant.id()).isEqualTo(merchantId);
		assertThat(merchant.status()).isEqualTo(MerchantStatus.ACTIVE);
	}

	@Test
	void mapsAnAbsentMerchantToNotFound() {
		UUID merchantId = UUID.randomUUID();
		server.createContext("/internal/v1/merchants/" + merchantId, exchange -> respond(exchange, 404, "{}"));

		assertThatThrownBy(() -> merchantServiceClient.getMerchant(merchantId))
				.isInstanceOf(MerchantNotFoundException.class);
	}

	private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
		byte[] response = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, response.length);
		try (var output = exchange.getResponseBody()) {
			output.write(response);
		}
	}
}
