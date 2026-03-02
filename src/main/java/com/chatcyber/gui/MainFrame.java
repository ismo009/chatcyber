package com.chatcyber.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.chatcyber.crypto.SystemParameters;
import com.chatcyber.mail.MailConfig;

/**
 * Fenetre principale de l'application client mail securise.
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;
    private ConfigPanel configPanel;
    private ComposePanel composePanel;
    private InboxPanel inboxPanel;

    private MailConfig mailConfig;
    private SystemParameters systemParams;
    private byte[] privateKey;

    private JLabel statusBar;
    private JLabel ibeBadge;

    public MainFrame() {
        super("ChatCyber - Messagerie Securisee IBE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 750);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);

        loadConfiguration();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_MAIN);

        // ── En-tete ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JLabel titleLabel = new JLabel("ChatCyber");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("    Messagerie Securisee  |  Chiffrement IBE Boneh-Franklin");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(191, 219, 254));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        header.add(titlePanel, BorderLayout.WEST);

        // Badge IBE
        JPanel ibePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        ibePanel.setOpaque(false);
        ibeBadge = new JLabel();
        updateIBEBadge();
        ibePanel.add(ibeBadge);
        header.add(ibePanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ── Barre de statut ──
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(UITheme.BG_CARD);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));

        statusBar = new JLabel("Pret");
        statusBar.setFont(UITheme.FONT_SMALL);
        statusBar.setForeground(UITheme.TEXT_SECONDARY);

        JLabel version = new JLabel("ChatCyber v1.0   ");
        version.setFont(UITheme.FONT_SMALL);
        version.setForeground(UITheme.TEXT_MUTED);

        statusPanel.add(statusBar, BorderLayout.WEST);
        statusPanel.add(version, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);

        // ── Onglets ──
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabbedPane.setBackground(UITheme.BG_SIDEBAR);
        tabbedPane.setBorder(null);

        configPanel = new ConfigPanel(this);
        composePanel = new ComposePanel(this);
        inboxPanel = new InboxPanel(this);

        tabbedPane.addTab("  Configuration  ", wrapTab(configPanel));
        tabbedPane.addTab("  Composer       ", wrapTab(composePanel));
        tabbedPane.addTab("  Reception      ", wrapTab(inboxPanel));

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Wrap un panel dans un conteneur avec fond et padding.
     */
    private JPanel wrapTab(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG_MAIN);
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private void updateIBEBadge() {
        boolean ready = isIBEReady();
        ibeBadge.setText(ready ? "  IBE Actif  " : "  IBE Inactif  ");
        ibeBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        ibeBadge.setForeground(Color.WHITE);
        ibeBadge.setOpaque(true);
        ibeBadge.setBackground(ready ? new Color(34, 197, 94) : new Color(239, 68, 68, 200));
        ibeBadge.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
    }

    // ==================== PERSISTENCE ====================

    private void loadConfiguration() {
        String appDir = MailConfig.getAppDataDir();
        new File(appDir).mkdirs();

        try {
            String configPath = MailConfig.getConfigFilePath();
            if (new File(configPath).exists()) {
                mailConfig = MailConfig.load(configPath);
            } else {
                mailConfig = new MailConfig();
            }
        } catch (IOException e) {
            mailConfig = new MailConfig();
        }

        try {
            String paramsPath = MailConfig.getSystemParamsFilePath();
            if (new File(paramsPath).exists()) {
                systemParams = SystemParameters.loadFromFile(paramsPath);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement params IBE : " + e.getMessage());
        }

        try {
            String keyPath = MailConfig.getPrivateKeyFilePath();
            if (new File(keyPath).exists()) {
                privateKey = Files.readAllBytes(Path.of(keyPath));
            }
        } catch (IOException e) {
            System.err.println("Erreur chargement cle privee : " + e.getMessage());
        }
    }

    public void saveConfiguration() {
        try {
            mailConfig.save(MailConfig.getConfigFilePath());
        } catch (IOException e) {
            showError("Erreur", "Erreur de sauvegarde : " + e.getMessage());
        }
    }

    public void saveSystemParams() {
        if (systemParams != null) {
            try { systemParams.saveToFile(MailConfig.getSystemParamsFilePath()); }
            catch (IOException e) { System.err.println("Erreur sauvegarde params : " + e.getMessage()); }
        }
    }

    public void savePrivateKey() {
        if (privateKey != null) {
            try {
                String keyPath = MailConfig.getPrivateKeyFilePath();
                new File(keyPath).getParentFile().mkdirs();
                Files.write(Path.of(keyPath), privateKey);
            } catch (IOException e) {
                System.err.println("Erreur sauvegarde cle : " + e.getMessage());
            }
        }
    }

    // ==================== ACCESSEURS ====================

    public MailConfig getMailConfig() { return mailConfig; }

    public SystemParameters getSystemParams() { return systemParams; }
    public void setSystemParams(SystemParameters systemParams) {
        this.systemParams = systemParams;
        saveSystemParams();
        updateIBEBadge();
        updateStatus("Parametres IBE charges avec succes.");
    }

    public byte[] getPrivateKey() { return privateKey; }
    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
        savePrivateKey();
        updateIBEBadge();
        updateStatus("Cle privee IBE enregistree.");
    }

    public boolean isIBEReady() { return systemParams != null && privateKey != null; }
    public boolean hasSystemParams() { return systemParams != null; }

    // ==================== UI HELPERS ====================

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusBar.setText(message));
    }

    public void showError(String title, String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE));
    }

    public void showInfo(String title, String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE));
    }
}
