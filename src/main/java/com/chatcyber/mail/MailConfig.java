package com.chatcyber.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

public class MailConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    //SMTP Defaut
    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private boolean smtpStartTls = true;

    //IMAP Defaut
    private String imapHost = "imap.gmail.com";
    private int imapPort = 993;
    private boolean imapSsl = true;

    private String email = "";
    private String password = "";

    private String taHost = "localhost";
    private int taPort = 7777;

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

    //Pour sauvegarder la config
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

    //Convertit une Properties en MailConfig
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

    public void save(String path) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(path)) {
            toProperties().store(fos, "ChatCyber - Configuration Email & IBE");
        }
    }

    public static MailConfig load(String path) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        }
        return fromProperties(props);
    }

    public boolean isEmailConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    public static String getAppDataDir() {
        return System.getProperty("user.home") + File.separator + ".chatcyber";
    }

    public static String getConfigFilePath() {
        return getAppDataDir() + File.separator + "mail_config.properties";
    }

    public static String getSystemParamsFilePath() {
        return getAppDataDir() + File.separator + "system_params.dat";
    }

    public static String getPrivateKeyFilePath() {
        return getAppDataDir() + File.separator + "private_key.dat";
    }

    public static String getAttachmentsDir() {
        return getAppDataDir() + File.separator + "attachments";
    }
}
