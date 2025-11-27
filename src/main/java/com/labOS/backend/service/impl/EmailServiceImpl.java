package com.labOS.backend.service.impl;

import com.labOS.backend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Email Service Implementation
 * Sends verification code emails to users
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    @Resource
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Override
    @Async
    public void sendVerificationCode(String toEmail, String code) {
        try {
            if (mailSender == null || fromEmail == null || fromEmail.isEmpty()) {
                log.warn("Email service not configured. Verification code for {}: {}", toEmail, code);
                return;
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("labOS - Email Verification Code");
            message.setText(String.format(
                "Hello,\n\n" +
                "Your verification code is: %s\n\n" +
                "This code will expire in 5 minutes.\n\n" +
                "If you did not request this code, please ignore this email.\n\n" +
                "Best regards,\n" +
                "labOS Team",
                code
            ));
            
            mailSender.send(message);
            log.info("Verification code email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification code email to: {}", toEmail, e);
            // Log the code anyway so development can continue
            log.warn("Verification code for {}: {}", toEmail, code);
        }
    }
}

