package tech.paymenti7.paymentgatewaycore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@EnableRabbit
public class PaymentGatewayCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentGatewayCoreApplication.class, args);
	}
}
