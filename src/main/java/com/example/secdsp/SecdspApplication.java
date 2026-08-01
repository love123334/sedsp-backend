package com.example.secdsp;

import com.example.secdsp.config.StartupFailureLogger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecdspApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SecdspApplication.class);
		app.addListeners(new StartupFailureLogger());
		app.run(args);
	}

}
