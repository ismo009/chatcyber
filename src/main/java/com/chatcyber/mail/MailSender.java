package com.chatcyber.mail;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class MailSender {

    private final MailConfig config;

    public MailSender(MailConfig config) {
        this.config = config;
    }

    public void sendEmail(String to, String subject, String bodyText, File attachment) throws MessagingException {
        // Configuration des propriétés SMTP
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isSmtpStartTls()));
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
        props.put("mail.smtp.ssl.trust", config.getSmtpHost());

        //Auth
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getEmail(), config.getPassword());
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getEmail()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(bodyText, "UTF-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);

        if (attachment != null && attachment.exists()) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            FileDataSource source = new FileDataSource(attachment);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(attachment.getName());
            multipart.addBodyPart(attachmentPart);
        }

        message.setContent(multipart);

        Transport.send(message);
        System.out.println("[Mail] Email envoyé avec succès à " + to);
    }

    public void sendEmail(String to, String subject, String bodyText) throws MessagingException {
        sendEmail(to, subject, bodyText, null);
    }

    public boolean testConnection() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isSmtpStartTls()));
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
        props.put("mail.smtp.ssl.trust", config.getSmtpHost());

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getEmail(), config.getPassword());
            }
        });

        try {
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            return true;
        } catch (MessagingException e) {
            System.err.println("[Mail] Erreur de connexion SMTP : " + e.getMessage());
            return false;
        }
    }
}
