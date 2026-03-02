package com.chatcyber.mail;

import java.io.*;
import java.util.Properties;

/**
 * Configuration de connexion email (SMTP pour l'envoi, IMAP pour la réception).
 * Pré-configuré pour Gmail par défaut.
 *
 * Pour Gmail, il faut créer un "Mot de passe d'application" :
 * https://support.google.com/mail/answer/185833?hl=fr
 */
public class MailConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- SMTP (envoi) ---
    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private boolean smtpStartTls = true;

    // --- IMAP (réception) ---
    private String imapHost = "imap.gmail.com";
    private int imapPort = 993;
    private boolean imapSsl = true;

    // --- Identifiants ---
    private String email = "";
    private String password = "";  // Mot de passe d'application pour Gmail

    // --- Autorité de Confiance ---
    private String taHost = "localhost";
    private int taPort = 7777;

    // ==================== GETTERS / SETTERS ====================

    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }

    public boolean isSmtpStartTls() { return smtpStartTls; }
    public void setSmtpStartTls(boolean smtpStartTls) { this.smtpStartTls = smtpStartTls; }

    public String getImapHost() { return imapHost; }
    public void setImapHost(String imapHost) { this.imapHost = imapHost; }

    public int getImapPort() { return imapPort; }
    public void setImapPort(int imapPort) { this.imapPort = imapPort; }

    public boolean isImapSsl() { return imapSsl; }
    public void setImapSsl(boolean imapSsl) { this.imapSsl = imapSsl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTaHost() { return taHost; }
    public void setTaHost(String taHost) { this.taHost = taHost; }

    public int getTaPort() { return taPort; }
    public void setTaPort(int taPort) { this.taPort = taPort; }

    // ==================== PERSISTANCE ====================

    /**
     * Convertit la configuration en Properties Java.
     */
    public Properties toProperties() {
        Properties props = new Properties();
        props.setProperty("smtp.host", smtpHost);
        props.setProperty("smtp.port", String.valueOf(smtpPort));
        props.setProperty("smtp.starttls", String.valueOf(smtpStartTls));
        props.setProperty("imap.host", imapHost);
        props.setProperty("imap.port", String.valueOf(imapPort));
        props.setProperty("imap.ssl", String.valueOf(imapSsl));
        props.setProperty("email", email);
        props.setProperty("password", password); // En production, chiffrer !
        props.setProperty("ta.host", taHost);
        props.setProperty("ta.port", String.valueOf(taPort));
        return props;
    }

    /**
     * Crée une configuration depuis des Properties Java.
     */
    public static MailConfig fromProperties(Properties props) {
        MailConfig config = new MailConfig();
        config.smtpHost = props.getProperty("smtp.host", "smtp.gmail.com");
        config.smtpPort = Integer.parseInt(props.getProperty("smtp.port", "587"));
        config.smtpStartTls = Boolean.parseBoolean(props.getProperty("smtp.starttls", "true"));
        config.imapHost = props.getProperty("imap.host", "imap.gmail.com");
        config.imapPort = Integer.parseInt(props.getProperty("imap.port", "993"));
        config.imapSsl = Boolean.parseBoolean(props.getProperty("imap.ssl", "true"));
        config.email = props.getProperty("email", "");
        config.password = props.getProperty("password", "");
        config.taHost = props.getProperty("ta.host", "localhost");
        config.taPort = Integer.parseInt(props.getProperty("ta.port", "7777"));
        return config;
    }

    /**
     * Sauvegarde la configuration dans un fichier properties.
     */
    public void save(String path) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(path)) {
            toProperties().store(fos, "ChatCyber - Configuration Email & IBE");
        }
    }

    /**
     * Charge la configuration depuis un fichier properties.
     */
    public static MailConfig load(String path) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        }
        return fromProperties(props);
    }

    /**
     * Vérifie si la configuration email est renseignée.
     */
    public boolean isEmailConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    /**
     * Retourne le chemin du répertoire de données de l'application.
     */
    public static String getAppDataDir() {
        return System.getProperty("user.home") + File.separator + ".chatcyber";
    }

    /**
     * Retourne le chemin du fichier de configuration.
     */
    public static String getConfigFilePath() {
        return getAppDataDir() + File.separator + "mail_config.properties";
    }

    /**
     * Retourne le chemin du fichier de paramètres IBE.
     */
    public static String getSystemParamsFilePath() {
        return getAppDataDir() + File.separator + "system_params.dat";
    }

    /**
     * Retourne le chemin du fichier de clé privée IBE.
     */
    public static String getPrivateKeyFilePath() {
        return getAppDataDir() + File.separator + "private_key.dat";
    }

    /**
     * Retourne le chemin du répertoire des pièces jointes.
     */
    public static String getAttachmentsDir() {
        return getAppDataDir() + File.separator + "attachments";
    }
}
