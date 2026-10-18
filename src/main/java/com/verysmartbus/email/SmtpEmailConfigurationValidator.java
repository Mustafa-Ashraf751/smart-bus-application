package com.verysmartbus.email;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
public class SmtpEmailConfigurationValidator {

    private final EmailNotificationProperties emailProperties;
    private final Environment environment;

    /** Fail clearly before a real SMTP attempt if production email was explicitly enabled. */
    @Bean
    @ConditionalOnProperty(prefix = "app.notifications.email", name = "enabled", havingValue = "true")
    ApplicationRunner validateSmtpEmailConfiguration() {
        return args -> {
            emailProperties.assertReadyForSending();
            require("SMTP_HOST", environment.getProperty("spring.mail.host"));
            require("SMTP_USERNAME", environment.getProperty("spring.mail.username"));
            require("SMTP_PASSWORD", environment.getProperty("spring.mail.password"));
        };
    }

    private void require(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Email notifications are enabled but " + name + " is not configured.");
        }
    }
}
