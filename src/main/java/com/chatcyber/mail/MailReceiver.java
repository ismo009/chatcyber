package com.chatcyber.mail;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.util.*;

/**
 * Réception de mails via IMAP avec support des pièces jointes.
 * Configuré par défaut pour Gmail (IMAPS + SSL).
 *
 * Fonctionnalités :
 * - Récupération des messages de la boîte de réception
 * - Parsing des messages MIME multipart
 * - Téléchargement des pièces jointes
 */
public class MailReceiver {

    private final MailConfig config;
    private Store store;
    private Folder inbox;

    public MailReceiver(MailConfig config) {
        this.config = config;
    }

    /**
     * Ouvre une connexion au serveur IMAP.
     */
    public void connect() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", config.getImapHost());
        props.put("mail.imaps.port", String.valueOf(config.getImapPort()));
        props.put("mail.imaps.ssl.enable", String.valueOf(config.isImapSsl()));
        props.put("mail.imaps.ssl.trust", config.getImapHost());

        Session session = Session.getInstance(props);
        store = session.getStore("imaps");
        store.connect(config.getImapHost(), config.getEmail(), config.getPassword());

        inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        System.out.println("[Mail] Connecté à la boîte de réception (" + inbox.getMessageCount() + " messages).");
    }

    /**
     * Ferme la connexion au serveur IMAP.
     */
    public void disconnect() {
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (MessagingException e) {
            System.err.println("[Mail] Erreur lors de la déconnexion : " + e.getMessage());
        }
    }

    /**
     * Récupère les N derniers messages de la boîte de réception.
     *
     * @param maxMessages Nombre maximum de messages à récupérer
     * @return Liste de messages convertis en EmailMessage (DTO)
     */
    public List<EmailMessage> fetchMessages(int maxMessages) throws MessagingException {
        if (inbox == null || !inbox.isOpen()) {
            connect();
        }

        int totalMessages = inbox.getMessageCount();
        int start = Math.max(1, totalMessages - maxMessages + 1);

        Message[] messages = inbox.getMessages(start, totalMessages);
        // Pré-chargement des en-têtes pour de meilleures performances
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        fp.add(FetchProfile.Item.CONTENT_INFO);
        inbox.fetch(messages, fp);

        List<EmailMessage> result = new ArrayList<>();
        for (int i = messages.length - 1; i >= 0; i--) {
            try {
                EmailMessage em = convertMessage(messages[i]);
                result.add(em);
            } catch (Exception e) {
                System.err.println("[Mail] Erreur de lecture du message #" + (i + start) + ": " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Télécharge une pièce jointe d'un message.
     *
     * @param messageNumber Numéro du message dans la boîte
     * @param partIndex     Index de la pièce jointe dans le message
     * @param destDir       Répertoire de destination
     * @return Le fichier téléchargé
     */
    public File downloadAttachment(int messageNumber, int partIndex, File destDir) throws Exception {
        if (inbox == null || !inbox.isOpen()) {
            connect();
        }

        Message message = inbox.getMessage(messageNumber);
        Object content = message.getContent();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            int currentAttachIndex = 0;

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                        (bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty())) {

                    if (currentAttachIndex == partIndex) {
                        String fileName = decodeFileName(bodyPart.getFileName());
                        if (!destDir.exists()) {
                            destDir.mkdirs();
                        }

                        File outputFile = new File(destDir, fileName);
                        try (InputStream is = bodyPart.getInputStream();
                             FileOutputStream fos = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }

                        System.out.println("[Mail] Pièce jointe téléchargée : " + outputFile.getAbsolutePath());
                        return outputFile;
                    }
                    currentAttachIndex++;
                }
            }
        }

        throw new IOException("Pièce jointe introuvable (message=" + messageNumber + ", index=" + partIndex + ")");
    }

    /**
     * Convertit un Message JavaMail en EmailMessage (DTO local).
     */
    private EmailMessage convertMessage(Message message) throws Exception {
        EmailMessage em = new EmailMessage();
        em.setMessageNumber(message.getMessageNumber());

        // Expéditeur
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            em.setFrom(fromAddresses[0].toString());
        } else {
            em.setFrom("(inconnu)");
        }

        // Destinataire
        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        if (toAddresses != null && toAddresses.length > 0) {
            em.setTo(toAddresses[0].toString());
        }

        // Objet
        em.setSubject(message.getSubject() != null ? message.getSubject() : "(sans objet)");

        // Date d'envoi
        em.setSentDate(message.getSentDate());

        // Corps et pièces jointes
        String bodyText = "";
        List<EmailMessage.AttachmentInfo> attachments = new ArrayList<>();
        Object content = message.getContent();

        if (content instanceof String) {
            bodyText = (String) content;
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            bodyText = extractTextFromMultipart(multipart);
            extractAttachments(multipart, attachments);
        }

        em.setBodyText(bodyText);
        em.setAttachments(attachments);

        return em;
    }

    /**
     * Extrait le texte brut d'un message multipart.
     */
    private String extractTextFromMultipart(Multipart multipart) throws Exception {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (bodyPart.isMimeType("text/plain") &&
                    (bodyPart.getDisposition() == null ||
                     !Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()))) {
                text.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html") && text.length() == 0 &&
                    (bodyPart.getDisposition() == null ||
                     !Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()))) {
                // Fallback : utiliser le HTML si pas de texte brut
                text.append("[HTML] ").append(bodyPart.getContent().toString());
            } else if (bodyPart.getContent() instanceof Multipart) {
                text.append(extractTextFromMultipart((Multipart) bodyPart.getContent()));
            }
        }

        return text.toString();
    }

    /**
     * Extrait les informations sur les pièces jointes d'un message multipart.
     */
    private void extractAttachments(Multipart multipart, List<EmailMessage.AttachmentInfo> attachments) throws Exception {
        int attachIndex = 0;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                    (bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty())) {

                String fileName = decodeFileName(bodyPart.getFileName());
                long size = bodyPart.getSize();
                if (size < 0) size = 0; // Taille inconnue

                attachments.add(new EmailMessage.AttachmentInfo(fileName, attachIndex, size));
                attachIndex++;
            } else if (bodyPart.getContent() instanceof Multipart) {
                extractAttachments((Multipart) bodyPart.getContent(), attachments);
            }
        }
    }

    /**
     * Décode le nom d'un fichier joint (gestion de l'encodage MIME).
     */
    private String decodeFileName(String fileName) {
        if (fileName == null) return "piece_jointe";
        try {
            return MimeUtility.decodeText(fileName);
        } catch (Exception e) {
            return fileName;
        }
    }

    /**
     * Teste la connexion IMAP.
     *
     * @return true si la connexion est réussie
     */
    public boolean testConnection() {
        try {
            connect();
            disconnect();
            return true;
        } catch (MessagingException e) {
            System.err.println("[Mail] Erreur de connexion IMAP : " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        return store != null && store.isConnected();
    }
}
