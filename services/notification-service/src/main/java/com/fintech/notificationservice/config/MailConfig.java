package com.fintech.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:false}")
    private boolean starttlsRequired;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:5000}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:5000}")
    private int timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:5000}")
    private int writeTimeout;

    /**
     * Configure JavaMailSender with SMTP properties for sending emails
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Basic SMTP configuration
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        
        // SMTP Properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.smtp.starttls.required", starttlsRequired);
        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.writetimeout", writeTimeout);
        
        // Security and debugging
        props.put("mail.debug", "false"); // Set to true for debugging
        props.put("mail.smtp.ssl.trust", mailHost);
        
        return mailSender;
    }

    /**
     * Email template service for creating HTML email content
     */
    @Bean
    public EmailTemplateService emailTemplateService() {
        return new EmailTemplateService();
    }

    /**
     * Configuration properties for email sending
     */
    @Bean
    public EmailConfigProperties emailConfigProperties() {
        return new EmailConfigProperties();
    }

    /**
     * Simple email template service without Thymeleaf dependency
     */
    public static class EmailTemplateService {
        
        public String generateWelcomeEmail(String userName, String loginUrl) {
            return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Welcome to FinTech</title>
                    <style>
                        body { font-family: 'Segoe UI', sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f8f9fa; }
                        .container { background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); }
                        .header { background: linear-gradient(135deg, #007bff, #0056b3); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; margin: -30px -30px 30px -30px; }
                        .header h1 { margin: 0; font-size: 28px; }
                        .button { display: inline-block; padding: 15px 30px; background: linear-gradient(135deg, #28a745, #20c997); color: white; text-decoration: none; border-radius: 6px; margin: 20px 0; font-weight: bold; }
                        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Welcome to FinTech!</h1>
                            <p>Your secure financial services platform</p>
                        </div>
                        <div style="text-align: center; margin: 30px 0;">
                            <h2>Welcome %s!</h2>
                            <p>Thank you for joining FinTech. Your account has been successfully created.</p>
                        </div>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Get Started</a>
                        </div>
                        <div class="footer">
                            <p>© 2025 FinTech. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, loginUrl);
        }
        
        public String generateTransactionEmail(String userName, String transactionId, String amount, String date, String type) {
            return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Transaction Confirmation</title>
                    <style>
                        body { font-family: 'Segoe UI', sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f8f9fa; }
                        .container { background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); }
                        .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; margin: -30px -30px 30px -30px; }
                        .transaction-details { background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin: 20px 0; }
                        .detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #dee2e6; }
                        .detail-row:last-child { border-bottom: none; font-weight: bold; font-size: 18px; }
                        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✓ Transaction Successful</h1>
                        </div>
                        <h2>Transaction Confirmation</h2>
                        <p>Dear %s,</p>
                        <p>Your transaction has been successfully processed:</p>
                        <div class="transaction-details">
                            <div class="detail-row"><span>Transaction ID:</span><span>%s</span></div>
                            <div class="detail-row"><span>Date:</span><span>%s</span></div>
                            <div class="detail-row"><span>Type:</span><span>%s</span></div>
                            <div class="detail-row"><span>Amount:</span><span>%s</span></div>
                        </div>
                        <div class="footer">
                            <p>© 2025 FinTech. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, transactionId, date, type, amount);
        }
        
        public String generateDefaultEmail(String subject, String body) {
            return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <style>
                        body { font-family: 'Segoe UI', sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f8f9fa; }
                        .container { background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); }
                        .header { background-color: #007bff; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; margin: -30px -30px 30px -30px; }
                        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>FinTech</h1>
                        </div>
                        <h2>%s</h2>
                        <p>%s</p>
                        <div class="footer">
                            <p>© 2025 FinTech. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(subject, subject, body);
        }
    }

    /**
     * Email configuration properties class
     */
    public static class EmailConfigProperties {
        private String fromEmail = "noreply@fintech.com";
        private String fromName = "FinTech Notification Service";
        
        // Template names for different email types
        public static final String WELCOME_TEMPLATE = "welcome";
        public static final String TRANSACTION_CONFIRMATION_TEMPLATE = "transaction-confirmation";
        public static final String PAYMENT_NOTIFICATION_TEMPLATE = "payment-notification";
        public static final String SECURITY_ALERT_TEMPLATE = "security-alert";
        public static final String PASSWORD_RESET_TEMPLATE = "password-reset";
        public static final String ACCOUNT_VERIFICATION_TEMPLATE = "account-verification";
        
        public String getFromEmail() {
            return fromEmail;
        }
        
        public void setFromEmail(String fromEmail) {
            this.fromEmail = fromEmail;
        }
        
        public String getFromName() {
            return fromName;
        }
        
        public void setFromName(String fromName) {
            this.fromName = fromName;
        }
        
        /**
         * Get template name based on email type
         */
        public String getTemplateForType(String type) {
            return switch (type.toLowerCase()) {
                case "welcome" -> WELCOME_TEMPLATE;
                case "transaction" -> TRANSACTION_CONFIRMATION_TEMPLATE;
                case "payment" -> PAYMENT_NOTIFICATION_TEMPLATE;
                case "security" -> SECURITY_ALERT_TEMPLATE;
                case "password_reset" -> PASSWORD_RESET_TEMPLATE;
                case "verification" -> ACCOUNT_VERIFICATION_TEMPLATE;
                default -> "default";
            };
        }
    }
}
