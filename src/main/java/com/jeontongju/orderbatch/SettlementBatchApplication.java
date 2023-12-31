package com.jeontongju.orderbatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class SettlementBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(SettlementBatchApplication.class, args);
	}

}
