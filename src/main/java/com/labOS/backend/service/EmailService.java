package com.labOS.backend.service;

/**
 * Email Service Interface
 * Handles sending emails, including verification codes
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
public interface EmailService {
    /**
     * Send verification code email
     * 
     * @param toEmail Recipient email address
     * @param code 6-digit verification code
     */
    void sendVerificationCode(String toEmail, String code);
}

