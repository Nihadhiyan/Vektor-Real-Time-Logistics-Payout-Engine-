package com.vektor.dispatch_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DispatchEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(DispatchEngineApplication.class, args);
	}

}
