package com.example.secdsp;

import com.example.secdsp.config.StartupFailureLogger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SecdspApplication {

	private static final String APPLICATION_TIME_ZONE = "Asia/Ho_Chi_Minh";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_TIME_ZONE));
		SpringApplication app = new SpringApplication(SecdspApplication.class);
		app.addListeners(new StartupFailureLogger());
		app.run(args);
	}

}
