package com.pieca.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PiecaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PiecaBackendApplication.class, args);
	}

}
