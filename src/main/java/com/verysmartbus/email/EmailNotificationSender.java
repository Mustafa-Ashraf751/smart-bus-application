package com.verysmartbus.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verysmartbus.entity.enums.NotificationType;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class EmailNotificationSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final EmailNotificationProperties properties;

    public void send(EmailNotificationDelivery delivery) {
        try {
            properties.assertReadyForSending();
        } catch (IllegalStateException exception) {
            throw new PermanentEmailDeliveryException(exception.getMessage());
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new PermanentEmailDeliveryException(
                    "SMTP is not configured. Set SMTP_HOST, SMTP_USERNAME, and SMTP_PASSWORD.");
        }

        RegistrationApprovedEmailPayload payload = parseRegistrationApprovalPayload(delivery);
        String html = renderRegistrationApprovedTemplate(payload);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getFromAddress(), properties.getFromName());
            helper.setTo(delivery.recipientEmail());
            helper.setSubject("Your VerySmartBus account has been approved");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailAuthenticationException exception) {
            throw new PermanentEmailDeliveryException("SMTP authentication failed. Verify the Brevo SMTP settings.");
        } catch (MailParseException | MailPreparationException exception) {
            throw new PermanentEmailDeliveryException("The registration approval email could not be prepared.");
        } catch (MailSendException exception) {
            if (containsPermanentSmtpCode(exception.getMessage())) {
                throw new PermanentEmailDeliveryException("The recipient address was rejected by the SMTP server.");
            }
            throw exception;
        } catch (MailException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PermanentEmailDeliveryException("The registration approval email could not be prepared.");
        }
    }

    private RegistrationApprovedEmailPayload parseRegistrationApprovalPayload(EmailNotificationDelivery delivery) {
        if (delivery.notificationType() != NotificationType.REGISTRATION_APPROVED) {
            throw new PermanentEmailDeliveryException("No email template is configured for this notification type.");
        }
        try {
            return objectMapper.readValue(delivery.payloadJson(), RegistrationApprovedEmailPayload.class);
        } catch (JsonProcessingException exception) {
            throw new PermanentEmailDeliveryException("The stored email data is invalid.");
        }
    }

    private String renderRegistrationApprovedTemplate(RegistrationApprovedEmailPayload payload) {
        try {
            Context context = new Context(Locale.ENGLISH);
            context.setVariable("name", payload.name());
            context.setVariable("username", payload.username());
            context.setVariable("email", payload.email());
            context.setVariable("loginUrl", payload.loginUrl());
            return templateEngine.process("email/registration-approved", context);
        } catch (TemplateEngineException exception) {
            throw new PermanentEmailDeliveryException("The registration approval email template could not be rendered.");
        }
    }

    private boolean containsPermanentSmtpCode(String message) {
        return message != null && message.matches("(?s).*\\b5\\d{2}\\b.*");
    }
}
