package com.chatcyber.mail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Représentation locale d'un message email (DTO).
 * Permet de stocker les informations nécessaires après lecture depuis le serveur IMAP
 * sans maintenir la connexion ouverte.
 */
public class EmailMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private int messageNumber;
    private String from;
    private String to;
    private String subject;
    private Date sentDate;
    private String bodyText;
    private List<AttachmentInfo> attachments;

    public EmailMessage() {
        this.attachments = new ArrayList<>();
    }

    public static class AttachmentInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String fileName;
        private int partIndex;
        private long size;

        public AttachmentInfo(String fileName, int partIndex, long size) {
            this.fileName = fileName;
            this.partIndex = partIndex;
            this.size = size;
        }

        public String getFileName() { return fileName; }
        public int getPartIndex() { return partIndex; }
        public long getSize() { return size; }

 
        public boolean isEncrypted() {
            return fileName != null && fileName.toLowerCase().endsWith(".ibe");
        }

        @Override
        public String toString() {
            String indicator = isEncrypted() ? " [Chiffré IBE]" : "";
            return fileName + indicator + " (" + formatSize(size) + ")";
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " o";
            if (bytes < 1024 * 1024) return String.format("%.1f Ko", bytes / 1024.0);
            return String.format("%.1f Mo", bytes / (1024.0 * 1024));
        }
    }

    public int getMessageNumber() { return messageNumber; }
    public void setMessageNumber(int messageNumber) { this.messageNumber = messageNumber; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Date getSentDate() { return sentDate; }
    public void setSentDate(Date sentDate) { this.sentDate = sentDate; }

    public String getBodyText() { return bodyText; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }

    public List<AttachmentInfo> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentInfo> attachments) { this.attachments = attachments; }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    public boolean hasEncryptedAttachments() {
        return attachments != null && attachments.stream().anyMatch(AttachmentInfo::isEncrypted);
    }

    @Override
    public String toString() {
        return String.format("De: %s | Objet: %s | Date: %s", from, subject, sentDate);
    }
}
