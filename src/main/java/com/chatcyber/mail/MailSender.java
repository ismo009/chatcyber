package com.chatcyber.mail;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;

/**
 * Envoi de mails via SMTP avec support des pièces jointes.
 * Configuré par défaut pour Gmail (SMTP + STARTTLS).
 *
 * Utilise l'API JavaMail pour composer et envoyer des messages MIME multipart.
 */
public class MailSender {

    private final MailConfig config;

    public MailSender(MailConfig config) {
        this.config = config;
    }

    /**
     * Envoie un email avec une pièce jointe optionnelle.
     *
     * @param to         Adresse email du destinataire
     * @param subject    Objet du mail
     * @param bodyText   Corps du message (texte brut)
     * @param attachment Fichier à joindre (peut être null)
     * @throws MessagingException En cas d'erreur d'envoi
     */
    public void sendEmail(String to, String subject, String bodyText, File attachment) throws MessagingException {
        // Configuration des propriétés SMTP
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isSmtpStartTls()));
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
        props.put("mail.smtp.ssl.trust", config.getSmtpHost());

        // Authentification
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getEmail(), config.getPassword());
            }
        });

        // Construction du message MIME
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getEmail()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);

        // Corps du message (partie texte)
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(bodyText, "UTF-8");

        // Assemblage multipart
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);

        // Pièce jointe (si fournie)
        if (attachment != null && attachment.exists()) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            FileDataSource source = new FileDataSource(attachment);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(attachment.getName());
            multipart.addBodyPart(attachmentPart);
        }

        message.setContent(multipart);

        // Envoi
        Transport.send(message);
        System.out.println("[Mail] Email envoyé avec succès à " + to);
    }

    /**
     * Envoie un email sans pièce jointe.
     */
    public void sendEmail(String to, String subject, String bodyText) throws MessagingException {
        sendEmail(to, subject, bodyText, null);
    }

    /**
     * Teste la connexion SMTP.
     *
     * @return true si la connexion est réussie
     */
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
