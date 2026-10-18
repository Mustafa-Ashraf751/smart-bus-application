package com.verysmartbus.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notifications.email")
public class EmailNotificationProperties {

    private boolean enabled = false;
    private String fromAddress;
    private String fromName = "VerySmartBus";
    private String frontendLoginUrl;
    private long pollDelayMs = 5_000;
    private int batchSize = 25;
    private long initialRetryDelaySeconds = 30;
    private long maxRetryDelaySeconds = 3_600;
    private int maxAttempts = 5;
    private long processingTimeoutSeconds = 120;

    public void assertReadyForSending() {
        require("MAIL_FROM_ADDRESS", fromAddress);
        require("APP_FRONTEND_LOGIN_URL", frontendLoginUrl);
    }

    private void require(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Email notifications are enabled but " + name + " is not configured.");
        }
    }
}
