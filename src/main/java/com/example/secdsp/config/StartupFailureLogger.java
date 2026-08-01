package com.example.secdsp.config;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

/**
 * Registered from {@code main} (not as a late {@code @Component}) so Railway still
 * prints the root Flyway/JPA cause when context refresh fails early.
 */
public class StartupFailureLogger implements ApplicationListener<ApplicationFailedEvent> {

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        Throwable t = event.getException();
        System.err.println("==== SEDSP STARTUP FAILED — cause chain ====");
        int i = 0;
        while (t != null && i < 20) {
            System.err.println("[cause " + i + "] " + t.getClass().getName() + ": " + t.getMessage());
            t = t.getCause();
            i++;
        }
        System.err.println("==== end cause chain ====");
    }
}
