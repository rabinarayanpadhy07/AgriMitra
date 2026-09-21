package com.shopeasy.auth.service;

import com.shopeasy.auth.exception.ApiException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:AgriMitra <onboarding@resend.dev>}")
    private String resendFromEmail;

    public void sendOtpEmail(String recipientEmail, String recipientName, String otp) {
        logOtpBanner(recipientEmail, otp);

        // 1. If Brevo HTTP API is configured, use it (bypasses Render's SMTP port block via port 443)
        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty() && !brevoApiKey.contains("your_brevo_key")) {
            sendViaBrevoApi(recipientEmail, recipientName, otp);
            return;
        }

        // 2. If Resend HTTP API is configured, use it (bypasses Render's SMTP port block via port 443)
        if (resendApiKey != null && !resendApiKey.trim().isEmpty() && !resendApiKey.contains("your_resend_key")) {
            sendViaResendApi(recipientEmail, recipientName, otp);
            return;
        }

        // 3. Fallback to standard SMTP
        sendViaSmtp(recipientEmail, recipientName, otp);
    }

    private void sendViaBrevoApi(String recipientEmail, String recipientName, String otp) {
        try {
            logger.info("Dispatching OTP email to {} via Brevo HTTPS API...", recipientEmail);
            String htmlMsg = buildOtpEmailTemplate(recipientName, otp);
            String from = (senderEmail != null && !senderEmail.isBlank() && !senderEmail.contains("your_email"))
                    ? senderEmail : "support@agrimitra.com";

            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", "AgriMitra", "email", from),
                    "to", List.of(Map.of("email", recipientEmail, "name", recipientName != null ? recipientName : "Farmer")),
                    "subject", "AgriMitra - Password Recovery OTP Code",
                    "htmlContent", htmlMsg
            );

            RestClient.create()
                    .post()
                    .uri("https://api.brevo.com/v3/smtp/email")
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("OTP email successfully dispatched via Brevo API to {}", recipientEmail);
        } catch (Exception e) {
            logger.error("Failed to send OTP email via Brevo API: {}", e.getMessage(), e);
            throw new ApiException("Failed to send email via Brevo API: " + e.getMessage());
        }
    }

    private void sendViaResendApi(String recipientEmail, String recipientName, String otp) {
        try {
            logger.info("Dispatching OTP email to {} via Resend HTTPS API...", recipientEmail);
            String htmlMsg = buildOtpEmailTemplate(recipientName, otp);

            Map<String, Object> payload = Map.of(
                    "from", resendFromEmail != null ? resendFromEmail : "AgriMitra <onboarding@resend.dev>",
                    "to", List.of(recipientEmail),
                    "subject", "AgriMitra - Password Recovery OTP Code",
                    "html", htmlMsg
            );

            RestClient.create()
                    .post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("OTP email successfully dispatched via Resend API to {}", recipientEmail);
        } catch (Exception e) {
            logger.error("Failed to send OTP email via Resend API: {}", e.getMessage(), e);
            throw new ApiException("Failed to send email via Resend API: " + e.getMessage());
        }
    }

    private void sendViaSmtp(String recipientEmail, String recipientName, String otp) {
        if (senderEmail == null || senderEmail.trim().isEmpty() || senderEmail.contains("your_email") || senderEmail.equals("${MAIL_USERNAME:}")) {
            logger.error("SMTP email not configured (MAIL_USERNAME is empty or placeholder). Cannot dispatch email.");
            throw new ApiException("Email service is currently not configured on the server. Please set MAIL_USERNAME/MAIL_PASSWORD or use BREVO_API_KEY.");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = buildOtpEmailTemplate(recipientName, otp);

            helper.setText(htmlMsg, true);
            helper.setTo(recipientEmail);
            helper.setSubject("AgriMitra - Password Recovery OTP Code");
            helper.setFrom(senderEmail);

            mailSender.send(mimeMessage);
            logger.info("OTP email successfully dispatched via SMTP to {}", recipientEmail);
        } catch (MessagingException | RuntimeException e) {
            logger.error("Could not dispatch email via SMTP: {}", e.getMessage(), e);
            String msg = e.getMessage();
            if (msg != null && (msg.contains("SocketTimeoutException") || msg.contains("Connect timed out") || msg.contains("timeout"))) {
                throw new ApiException("Render Free Tier blocks outbound SMTP ports (25, 465, 587). Please configure BREVO_API_KEY (port 443) on Render, or upgrade Render to a paid plan.");
            }
            throw new ApiException("Failed to send OTP email: " + msg);
        }
    }

    private void logOtpBanner(String recipientEmail, String otp) {
        String border = "==========================================================";
        logger.info("\n{}\n  [AGRIMITRA OTP DISPATCH]\n  Recipient: {}\n  Verification OTP Code: {}\n  Valid for: 10 minutes\n{}",
                border, recipientEmail, otp, border);
    }

    private String buildOtpEmailTemplate(String name, String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4fdf7; margin: 0; padding: 20px; }
                        .container { max-width: 540px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(5,150,105,0.1); border: 1px solid #d1fae5; }
                        .header { background: linear-gradient(135deg, #065f46, #16a34a); color: white; padding: 26px 24px; text-align: center; }
                        .content { padding: 30px 24px; color: #1e293b; line-height: 1.6; }
                        .otp-box { font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #065f46; text-align: center; background: #ecfdf5; padding: 18px; border-radius: 8px; margin: 24px 0; border: 1.5px dashed #059669; }
                        .footer { background: #f0fdf4; padding: 16px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #dcfce7; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin:0; font-size: 24px; font-weight: bold; letter-spacing: -0.5px;">🌱 AgriMitra</h1>
                            <p style="margin:6px 0 0 0; font-size: 13px; opacity: 0.95; letter-spacing: 0.5px;">Sowing Prosperity. Growing Trust.</p>
                        </div>
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>We received a request to reset the password for your AgriMitra account. Use the following One-Time Password (OTP) to complete your verification:</p>
                            <div class="otp-box">%s</div>
                            <p>This OTP is valid for <strong>10 minutes</strong>. If you did not request a password reset, please ignore this email or contact support immediately.</p>
                            <p>Best regards,<br><strong>The AgriMitra Team</strong></p>
                        </div>
                        <div class="footer">
                            &copy; 2026 AgriMitra. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, otp);
    }
}
