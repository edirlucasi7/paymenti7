package tech.paymenti7.paymentauthorization;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRabbit
@EnableScheduling
public class PaymentAuthorizationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentAuthorizationServiceApplication.class, args);
	}
}
