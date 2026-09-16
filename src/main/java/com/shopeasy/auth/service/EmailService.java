package com.shopeasy.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendOtpEmail(String recipientEmail, String recipientName, String otp) {
        // Prominently log to console for instant developer verification
        logOtpBanner(recipientEmail, otp);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = buildOtpEmailTemplate(recipientName, otp);

            helper.setText(htmlMsg, true);
            helper.setTo(recipientEmail);
            helper.setSubject("ShopEasy - Password Recovery OTP Code");
            helper.setFrom(senderEmail);

            mailSender.send(mimeMessage);
            logger.info("OTP email successfully dispatched to {}", recipientEmail);
        } catch (MessagingException | RuntimeException e) {
            logger.warn("Could not dispatch email via SMTP (using fallback console display): {}", e.getMessage());
            // We do not rethrow because the OTP is logged to console for testing/development
        }
    }

    private void logOtpBanner(String recipientEmail, String otp) {
        String border = "==========================================================";
        logger.info("\n{}\n  [SHOPEASY OTP DISPATCH]\n  Recipient: {}\n  Verification OTP Code: {}\n  Valid for: 10 minutes\n{}",
                border, recipientEmail, otp, border);
    }

    private String buildOtpEmailTemplate(String name, String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; }
                        .container { max-width: 540px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                        .header { background: linear-gradient(135deg, #4f46e5, #7c3aed); color: white; padding: 24px; text-align: center; }
                        .content { padding: 30px 24px; color: #334155; line-height: 1.6; }
                        .otp-box { font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #4f46e5; text-align: center; background: #eef2ff; padding: 18px; border-radius: 8px; margin: 24px 0; border: 1px dashed #6366f1; }
                        .footer { background: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin:0; font-size: 24px;">ShopEasy E-Commerce</h1>
                            <p style="margin:5px 0 0 0; opacity: 0.9;">Secure Password Recovery</p>
                        </div>
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>We received a request to reset the password for your ShopEasy account. Use the following One-Time Password (OTP) to complete your verification:</p>
                            <div class="otp-box">%s</div>
                            <p>This OTP is valid for <strong>10 minutes</strong>. If you did not request a password reset, please ignore this email or contact customer support immediately.</p>
                            <p>Best regards,<br><strong>The ShopEasy Security Team</strong></p>
                        </div>
                        <div class="footer">
                            &copy; 2026 ShopEasy Inc. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, otp);
    }
}
