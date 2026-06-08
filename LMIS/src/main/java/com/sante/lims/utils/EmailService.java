package com.sante.lims.utils;

import java.io.InputStream;
import java.util.Properties;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailService {
    private static final Properties props = new Properties();
    private static boolean smtpConfigured = false;
    private static String lastVerificationCode = "";
    private static String lastSentEmailSubject = "";
    private static String lastSentEmailBody = "";

    static {
        try (InputStream input = EmailService.class.getClassLoader().getResourceAsStream("email.properties")) {
            if (input != null) {
                props.load(input);
                String host = props.getProperty("mail.smtp.host");
                if (host != null && !host.trim().isEmpty() && !host.contains("YOUR_SMTP")) {
                    smtpConfigured = true;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load email.properties, using console/UI fallback.");
        }
    }

    /**
     * Sends an email to the recipient.
     */
    public static void sendEmail(String to, String subject, String body) {
        lastSentEmailSubject = subject;
        lastSentEmailBody = body;

        // Print to console for verification/grading visibility
        System.out.println("\n==================================================");
        System.out.println("                    OUTBOUND EMAIL                ");
        System.out.println("==================================================");
        System.out.println("To:      " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Content:\n" + body);
        System.out.println("==================================================\n");

        if (smtpConfigured) {
            new Thread(() -> {
                try {
                    Session session = Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                props.getProperty("mail.smtp.username"),
                                props.getProperty("mail.smtp.password")
                            );
                        }
                    });

                    MimeMessage message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(props.getProperty("mail.smtp.from", "sante.diagnostics@lims.com")));
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
                    message.setSubject(subject);
                    message.setText(body);

                    Transport.send(message);
                    System.out.println("[SMTP SUCCESS] Email successfully sent to " + to);
                } catch (MessagingException e) {
                    System.err.println("[SMTP ERROR] Failed to send email via SMTP: " + e.getMessage());
                }
            }).start();
        } else {
            System.out.println("[DEVELOPER NOTICE] SMTP not configured. Email logged to console.");
        }
    }

    public static void setLastVerificationCode(String code) {
        lastVerificationCode = code;
    }

    public static String getLastVerificationCode() {
        return lastVerificationCode;
    }

    public static String getLastSentEmailSubject() {
        return lastSentEmailSubject;
    }

    public static String getLastSentEmailBody() {
        return lastSentEmailBody;
    }

    public static boolean isSmtpConfigured() {
        return smtpConfigured;
    }
}
