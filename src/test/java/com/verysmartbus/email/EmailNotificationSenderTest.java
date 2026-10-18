package com.verysmartbus.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verysmartbus.entity.enums.NotificationType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationSenderTest {

    @Test
    void rendersTheLocalTemplateAndUsesTheVerifiedSenderAndSnapshotRecipient() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = providerFor(mailSender);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        EmailNotificationSender sender = new EmailNotificationSender(
                provider, templateEngine(), new ObjectMapper(), readyProperties());
        String payload = new ObjectMapper().writeValueAsString(new RegistrationApprovedEmailPayload(
                "Nour Ahmed", "nour.ahmed", "nour@example.com", "http://localhost:5173/login"));

        sender.send(new EmailNotificationDelivery(
                "nour@example.com", NotificationType.REGISTRATION_APPROVED, payload));

        verify(mailSender).send(message);
        assertThat(message.getFrom()[0].toString()).contains("no-reply@example.com", "VerySmartBus");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("nour@example.com");
        assertThat(message.getSubject()).isEqualTo("Your VerySmartBus account has been approved");
        assertThat(message.getContent().toString())
                .contains("Nour Ahmed", "nour.ahmed", "http://localhost:5173/login");
    }

    @Test
    void refusesToSendWhenTheVerifiedSenderConfigurationIsMissing() {
        EmailNotificationProperties properties = readyProperties();
        properties.setFromAddress(" ");
        EmailNotificationSender sender = new EmailNotificationSender(
                providerFor(mock(JavaMailSender.class)), templateEngine(), new ObjectMapper(), properties);

        assertThrows(PermanentEmailDeliveryException.class, () -> sender.send(new EmailNotificationDelivery(
                "nour@example.com", NotificationType.REGISTRATION_APPROVED, "{}")));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> providerFor(JavaMailSender mailSender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        return provider;
    }

    private static EmailNotificationProperties readyProperties() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setFromAddress("no-reply@example.com");
        properties.setFromName("VerySmartBus");
        properties.setFrontendLoginUrl("http://localhost:5173/login");
        return properties;
    }

    private static SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
