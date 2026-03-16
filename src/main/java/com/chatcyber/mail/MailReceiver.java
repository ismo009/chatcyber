package com.chatcyber.mail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeUtility;

public class MailReceiver {

    private final MailConfig config;
    private Store store;
    private Folder inbox;

    public MailReceiver(MailConfig config) {
        this.config = config;
    }

    //Connexion IMAP
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

    public List<EmailMessage> fetchMessages(int maxMessages) throws MessagingException {
        if (inbox == null || !inbox.isOpen()) {
            connect();
        }

        int totalMessages = inbox.getMessageCount();
        int start = Math.max(1, totalMessages - maxMessages + 1);

        Message[] messages = inbox.getMessages(start, totalMessages);
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

        throw new IOException("Piece jointe echec (message=" + messageNumber + ", i=" + partIndex + ")");
    }

    private EmailMessage convertMessage(Message message) throws Exception {
        EmailMessage em = new EmailMessage();
        em.setMessageNumber(message.getMessageNumber());

        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            em.setFrom(fromAddresses[0].toString());
        } else {
            em.setFrom("(inconnu)");
        }

        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        if (toAddresses != null && toAddresses.length > 0) {
            em.setTo(toAddresses[0].toString());
        }

        em.setSubject(message.getSubject() != null ? message.getSubject() : "(sans objet)");

        em.setSentDate(message.getSentDate());

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

//Extract le texte depuis un multipart
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
                text.append("[HTML] ").append(bodyPart.getContent().toString());
            } else if (bodyPart.getContent() instanceof Multipart) {
                text.append(extractTextFromMultipart((Multipart) bodyPart.getContent()));
            }
        }

        return text.toString();
    }


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
