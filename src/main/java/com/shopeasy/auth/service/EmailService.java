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
            helper.setSubject("AgriMitra - Password Recovery OTP Code");
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
