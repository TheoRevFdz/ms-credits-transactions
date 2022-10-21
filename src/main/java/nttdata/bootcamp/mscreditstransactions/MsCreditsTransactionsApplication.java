package nttdata.bootcamp.mscreditstransactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication
public class MsCreditsTransactionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsCreditsTransactionsApplication.class, args);
	}

}
