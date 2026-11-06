package com.sathya.co.biztrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class CreditMailConfiguration {

    @Bean
    @ConditionalOnProperty(name = "MAIL_HOST")
    public JavaMailSender creditMailSender(
            @Value("${MAIL_HOST}") String host,
            @Value("${MAIL_PORT:587}") int port,
            @Value("${MAIL_USERNAME:}") String username,
            @Value("${MAIL_PASSWORD:}") String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        if (!username.isBlank()) {
            sender.setUsername(username);
            sender.setPassword(password);
        }
        sender.getJavaMailProperties().put("mail.smtp.auth", !username.isBlank());
        sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
