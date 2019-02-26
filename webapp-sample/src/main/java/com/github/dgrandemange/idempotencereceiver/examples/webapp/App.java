package com.github.dgrandemange.idempotencereceiver.examples.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.github.dgrandemange.idempotencereceiver.examples.webapp.config.SwaggerConfig;

@SpringBootApplication
@Import(SwaggerConfig.class)
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}
