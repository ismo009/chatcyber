package com.chatcyber.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.chatcyber.crypto.SystemParameters;
import com.chatcyber.crypto.TrustAuthorityClient;
import com.chatcyber.mail.MailConfig;

/**
 * Panneau de configuration : Email, Autorite de Confiance et etat IBE.
 */
public class ConfigPanel extends JPanel {

    private final MainFrame mainFrame;

    private JTextField tfEmail;
    private JPasswordField tfPassword;
    private JTextField tfSmtpHost;
    private JTextField tfSmtpPort;
    private JTextField tfImapHost;
    private JTextField tfImapPort;

    private JTextField tfTaHost;
    private JTextField tfTaPort;

    private JLabel lblIbeStatus;
    private JLabel lblKeyStatus;

    public ConfigPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_MAIN);
        initComponents();
        loadConfig();
    }

    private void initComponents() {
        // Conteneur principal scrollable
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.BG_MAIN);
        content.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        // ── Header ──
        JPanel headerCard = UITheme.headerPanel(
                "Configuration",
                "Configurez vos parametres de messagerie et la connexion a l'Autorite de Confiance."
        );
        headerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(headerCard);
        content.add(Box.createVerticalStrut(12));

        // ── Email ──
        content.add(createEmailCard());
        content.add(Box.createVerticalStrut(12));

        // ── Autorite de Confiance ──
        content.add(createTACard());
        content.add(Box.createVerticalStrut(12));

        // ── Etat IBE ──
        content.add(createStatusCard());
        content.add(Box.createVerticalStrut(12));

        // ── Bouton sauvegarder ──
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        savePanel.setOpaque(false);
        savePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        savePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnSave = UITheme.primaryButton("Sauvegarder la configuration");
        btnSave.addActionListener(e -> saveConfig());
        savePanel.add(btnSave);
        content.add(savePanel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(UITheme.BG_MAIN);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createEmailCard() {
        JPanel card = UITheme.card("Configuration Email");
        card.setLayout(new GridBagLayout());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Email
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        card.add(UITheme.formLabel("Adresse email"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        tfEmail = UITheme.styledTextField(25);
        tfEmail.setToolTipText("Votre adresse email complete");
        card.add(tfEmail, gbc);

        // Mot de passe
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        card.add(UITheme.formLabel("Mot de passe"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        tfPassword = UITheme.styledPasswordField(25);
        tfPassword.setToolTipText("Mot de passe d'application (pas votre mot de passe principal)");
        card.add(tfPassword, gbc);

        // SMTP
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        card.add(UITheme.formLabel("Serveur SMTP"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7; gbc.gridwidth = 1;
        tfSmtpHost = UITheme.styledTextField(15);
        card.add(tfSmtpHost, gbc);
        gbc.gridx = 2; gbc.weightx = 0.3;
        tfSmtpPort = UITheme.styledTextField(5);
        tfSmtpPort.setToolTipText("Port (ex: 587)");
        card.add(tfSmtpPort, gbc);

        // IMAP
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        card.add(UITheme.formLabel("Serveur IMAP"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7; gbc.gridwidth = 1;
        tfImapHost = UITheme.styledTextField(15);
        card.add(tfImapHost, gbc);
        gbc.gridx = 2; gbc.weightx = 0.3;
        tfImapPort = UITheme.styledTextField(5);
        tfImapPort.setToolTipText("Port (ex: 993)");
        card.add(tfImapPort, gbc);

        // Note Gmail
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3; gbc.weightx = 1;
        JLabel lblNote = UITheme.descriptionLabel(
                "<html>Pour Gmail, utilisez un <b>mot de passe d'application</b> " +
                        "(Google > Securite > Mots de passe des applications)</html>");
        card.add(lblNote, gbc);

        return card;
    }

    private JPanel createTACard() {
        JPanel card = UITheme.card("Autorite de Confiance (IBE)");
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        // Ligne serveur
        JPanel serverLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        serverLine.setOpaque(false);
        serverLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        serverLine.add(UITheme.formLabel("Serveur AC"));
        tfTaHost = UITheme.styledTextField(14);
        serverLine.add(tfTaHost);
        serverLine.add(UITheme.formLabel("Port"));
        tfTaPort = UITheme.styledTextField(5);
        serverLine.add(tfTaPort);
        card.add(serverLine);
        card.add(Box.createVerticalStrut(12));

        // Boutons
        JPanel btnLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnLine.setOpaque(false);
        btnLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnGetParams = UITheme.outlineButton("Recuperer parametres IBE");
        btnGetParams.addActionListener(e -> fetchSystemParams());

        JButton btnExtractKey = UITheme.primaryButton("Demander ma cle privee");
        btnExtractKey.addActionListener(e -> extractPrivateKey());

        JButton btnLaunchTA = UITheme.successButton("Lancer serveur AC");
        btnLaunchTA.addActionListener(e -> launchTrustAuthority());

        btnLine.add(btnGetParams);
        btnLine.add(btnExtractKey);
        btnLine.add(btnLaunchTA);
        card.add(btnLine);

        return card;
    }

    private JPanel createStatusCard() {
        JPanel card = UITheme.card("Etat du systeme IBE");
        card.setLayout(new GridBagLayout());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 12);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        card.add(UITheme.formLabel("Parametres IBE"), gbc);
        gbc.gridx = 1;
        lblIbeStatus = UITheme.statusBadge(
                mainFrame.hasSystemParams() ? "OK - Charges" : "Non charges",
                mainFrame.hasSystemParams());
        card.add(lblIbeStatus, gbc);

        gbc.gridx = 2;
        card.add(Box.createHorizontalStrut(30), gbc);

        gbc.gridx = 3;
        card.add(UITheme.formLabel("Cle privee"), gbc);
        gbc.gridx = 4;
        lblKeyStatus = UITheme.statusBadge(
                mainFrame.getPrivateKey() != null ? "OK - Disponible" : "Non disponible",
                mainFrame.getPrivateKey() != null);
        card.add(lblKeyStatus, gbc);

        return card;
    }

    // ==================== ACTIONS ====================

    private void loadConfig() {
        MailConfig cfg = mainFrame.getMailConfig();
        tfEmail.setText(cfg.getEmail());
        tfPassword.setText(cfg.getPassword());
        tfSmtpHost.setText(cfg.getSmtpHost());
        tfSmtpPort.setText(String.valueOf(cfg.getSmtpPort()));
        tfImapHost.setText(cfg.getImapHost());
        tfImapPort.setText(String.valueOf(cfg.getImapPort()));
        tfTaHost.setText(cfg.getTaHost());
        tfTaPort.setText(String.valueOf(cfg.getTaPort()));
    }

    private void saveConfig() {
        MailConfig cfg = mainFrame.getMailConfig();
        cfg.setEmail(tfEmail.getText().trim());
        cfg.setPassword(new String(tfPassword.getPassword()));
        cfg.setSmtpHost(tfSmtpHost.getText().trim());
        try { cfg.setSmtpPort(Integer.parseInt(tfSmtpPort.getText().trim())); } catch (NumberFormatException ignored) {}
        cfg.setImapHost(tfImapHost.getText().trim());
        try { cfg.setImapPort(Integer.parseInt(tfImapPort.getText().trim())); } catch (NumberFormatException ignored) {}
        cfg.setTaHost(tfTaHost.getText().trim());
        try { cfg.setTaPort(Integer.parseInt(tfTaPort.getText().trim())); } catch (NumberFormatException ignored) {}

        mainFrame.saveConfiguration();
        mainFrame.updateStatus("Configuration sauvegardee.");
        mainFrame.showInfo("Sauvegarde", "Configuration sauvegardee avec succes.");
    }

    private void fetchSystemParams() {
        mainFrame.updateStatus("Connexion a l'Autorite de Confiance...");

        new Thread(() -> {
            try {
                TrustAuthorityClient taClient = new TrustAuthorityClient(
                        tfTaHost.getText().trim(),
                        Integer.parseInt(tfTaPort.getText().trim())
                );
                SystemParameters params = taClient.getParameters();
                mainFrame.setSystemParams(params);

                SwingUtilities.invokeLater(() -> {
                    updateStatusBadge(lblIbeStatus, true, "OK - Charges");
                    mainFrame.showInfo("Succes",
                            "Parametres IBE recuperes avec succes depuis l'Autorite de Confiance.");
                });
            } catch (Exception ex) {
                mainFrame.showError("Erreur de connexion",
                        "Impossible de contacter l'Autorite de Confiance :\n" + ex.getMessage());
                mainFrame.updateStatus("Erreur de connexion a l'AC.");
            }
        }).start();
    }

    private void extractPrivateKey() {
        String email = tfEmail.getText().trim();
        if (email.isEmpty()) {
            mainFrame.showError("Erreur", "Veuillez renseigner votre adresse email.");
            return;
        }

        mainFrame.updateStatus("Demande de cle privee pour " + email + "...");

        new Thread(() -> {
            try {
                TrustAuthorityClient taClient = new TrustAuthorityClient(
                        tfTaHost.getText().trim(),
                        Integer.parseInt(tfTaPort.getText().trim())
                );
                byte[] key = taClient.extractKey(email);
                mainFrame.setPrivateKey(key);

                SwingUtilities.invokeLater(() -> {
                    updateStatusBadge(lblKeyStatus, true, "OK - Disponible");
                    mainFrame.showInfo("Succes",
                            "Cle privee IBE recue pour : " + email + "\n(" + key.length + " octets)");
                });
            } catch (Exception ex) {
                mainFrame.showError("Erreur d'extraction",
                        "Impossible d'extraire la cle privee :\n" + ex.getMessage());
                mainFrame.updateStatus("Erreur d'extraction de la cle privee.");
            }
        }).start();
    }

    private void launchTrustAuthority() {
        int port;
        try { port = Integer.parseInt(tfTaPort.getText().trim()); }
        catch (NumberFormatException e) { port = 7777; }
        TrustAuthorityFrame taFrame = new TrustAuthorityFrame(port);
        taFrame.setVisible(true);
    }

    private void updateStatusBadge(JLabel badge, boolean ok, String text) {
        badge.setText(text);
        if (ok) {
            badge.setBackground(new Color(209, 250, 229));
            badge.setForeground(new Color(6, 95, 70));
        } else {
            badge.setBackground(new Color(254, 226, 226));
            badge.setForeground(new Color(153, 27, 27));
        }
    }

    public void refreshStatus() {
        updateStatusBadge(lblIbeStatus, mainFrame.hasSystemParams(),
                mainFrame.hasSystemParams() ? "OK - Charges" : "Non charges");
        updateStatusBadge(lblKeyStatus, mainFrame.getPrivateKey() != null,
                mainFrame.getPrivateKey() != null ? "OK - Disponible" : "Non disponible");
    }
}
