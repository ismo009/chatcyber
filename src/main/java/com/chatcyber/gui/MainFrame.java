package com.chatcyber.gui;

import com.chatcyber.crypto.SystemParameters;
import com.chatcyber.mail.MailConfig;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fenêtre principale de l'application client mail sécurisé.
 *
 * Organisation en onglets :
 *   1. Configuration  — Paramètres email (SMTP/IMAP) et Autorité de Confiance
 *   2. Sécurité IBE   — Gestion des clés IBE (connexion AC, extraction de clé)
 *   3. Composer        — Rédaction et envoi de mails avec pièces jointes chiffrées
 *   4. Boîte de réception — Consultation des mails reçus et déchiffrement
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;
    private ConfigPanel configPanel;
    private ComposePanel composePanel;
    private InboxPanel inboxPanel;

    // --- État partagé ---
    private MailConfig mailConfig;
    private SystemParameters systemParams;
    private byte[] privateKey;

    /** Barre de statut en bas de la fenêtre */
    private JLabel statusBar;

    public MainFrame() {
        super("ChatCyber — Messagerie Sécurisée IBE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);

        // Charger la configuration sauvegardée
        loadConfiguration();

        initComponents();

        // Icône de l'application
        try {
            setIconImage(new ImageIcon(getClass().getResource("/icon.png")).getImage());
        } catch (Exception ignored) {
            // Pas d'icône disponible, ce n'est pas critique
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // --- Barre de statut ---
        statusBar = new JLabel("  Prêt");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        add(statusBar, BorderLayout.SOUTH);

        // --- Panneau à onglets ---
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        configPanel = new ConfigPanel(this);
        composePanel = new ComposePanel(this);
        inboxPanel = new InboxPanel(this);

        tabbedPane.addTab("⚙ Configuration", configPanel);
        tabbedPane.addTab("✉ Composer", composePanel);
        tabbedPane.addTab("📥 Boîte de réception", inboxPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==================== GESTION DE L'ÉTAT ====================

    /**
     * Charge la configuration et l'état IBE depuis le disque.
     */
    private void loadConfiguration() {
        // Créer le répertoire de données si nécessaire
        String appDir = MailConfig.getAppDataDir();
        new File(appDir).mkdirs();

        // Charger la config mail
        try {
            String configPath = MailConfig.getConfigFilePath();
            if (new File(configPath).exists()) {
                mailConfig = MailConfig.load(configPath);
            } else {
                mailConfig = new MailConfig();
            }
        } catch (IOException e) {
            mailConfig = new MailConfig();
            System.err.println("Erreur lors du chargement de la configuration : " + e.getMessage());
        }

        // Charger les paramètres IBE
        try {
            String paramsPath = MailConfig.getSystemParamsFilePath();
            if (new File(paramsPath).exists()) {
                systemParams = SystemParameters.loadFromFile(paramsPath);
                System.out.println("Paramètres IBE chargés depuis le disque.");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des paramètres IBE : " + e.getMessage());
        }

        // Charger la clé privée
        try {
            String keyPath = MailConfig.getPrivateKeyFilePath();
            if (new File(keyPath).exists()) {
                privateKey = Files.readAllBytes(Path.of(keyPath));
                System.out.println("Clé privée IBE chargée depuis le disque.");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la clé privée : " + e.getMessage());
        }
    }

    /**
     * Sauvegarde la configuration sur le disque.
     */
    public void saveConfiguration() {
        try {
            mailConfig.save(MailConfig.getConfigFilePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de la sauvegarde : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sauvegarde les paramètres IBE sur le disque.
     */
    public void saveSystemParams() {
        if (systemParams != null) {
            try {
                systemParams.saveToFile(MailConfig.getSystemParamsFilePath());
            } catch (IOException e) {
                System.err.println("Erreur sauvegarde params IBE : " + e.getMessage());
            }
        }
    }

    /**
     * Sauvegarde la clé privée IBE sur le disque.
     */
    public void savePrivateKey() {
        if (privateKey != null) {
            try {
                String keyPath = MailConfig.getPrivateKeyFilePath();
                new File(keyPath).getParentFile().mkdirs();
                Files.write(Path.of(keyPath), privateKey);
            } catch (IOException e) {
                System.err.println("Erreur sauvegarde clé privée : " + e.getMessage());
            }
        }
    }

    // ==================== ACCESSEURS ÉTAT PARTAGÉ ====================

    public MailConfig getMailConfig() { return mailConfig; }

    public SystemParameters getSystemParams() { return systemParams; }
    public void setSystemParams(SystemParameters systemParams) {
        this.systemParams = systemParams;
        saveSystemParams();
        updateStatus("Paramètres IBE chargés.");
    }

    public byte[] getPrivateKey() { return privateKey; }
    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
        savePrivateKey();
        updateStatus("Clé privée IBE enregistrée.");
    }

    public boolean isIBEReady() {
        return systemParams != null && privateKey != null;
    }

    public boolean hasSystemParams() {
        return systemParams != null;
    }

    // ==================== UI ====================

    /**
     * Met à jour la barre de statut.
     */
    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusBar.setText("  " + message));
    }

    /**
     * Affiche un message d'erreur.
     */
    public void showError(String title, String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE));
    }

    /**
     * Affiche un message d'information.
     */
    public void showInfo(String title, String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE));
    }
}
