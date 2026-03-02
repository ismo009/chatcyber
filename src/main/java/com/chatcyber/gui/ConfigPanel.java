package com.chatcyber.gui;

import com.chatcyber.crypto.SystemParameters;
import com.chatcyber.crypto.TrustAuthorityClient;
import com.chatcyber.mail.MailConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panneau de configuration de l'application.
 *
 * Sections :
 *   1. Configuration Email — Paramètres SMTP/IMAP et identifiants
 *   2. Autorité de Confiance — Connexion à l'AC et gestion des clés IBE
 */
public class ConfigPanel extends JPanel {

    private final MainFrame mainFrame;

    // --- Champs Email ---
    private JTextField tfEmail;
    private JPasswordField tfPassword;
    private JTextField tfSmtpHost;
    private JTextField tfSmtpPort;
    private JTextField tfImapHost;
    private JTextField tfImapPort;

    // --- Champs AC ---
    private JTextField tfTaHost;
    private JTextField tfTaPort;

    // --- Indicateurs d'état ---
    private JLabel lblIbeStatus;
    private JLabel lblKeyStatus;

    public ConfigPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        initComponents();
        loadConfig();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // ====== Section Email ======
        JPanel emailPanel = createEmailPanel();
        mainPanel.add(emailPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // ====== Section Autorité de Confiance ======
        JPanel taPanel = createTAPanel();
        mainPanel.add(taPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // ====== Section État IBE ======
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel);

        // Scroll si la fenêtre est petite
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // ====== Bouton Sauvegarder ======
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("💾 Sauvegarder la configuration");
        btnSave.addActionListener(e -> saveConfig());
        buttonPanel.add(btnSave);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Crée le panneau de configuration email.
     */
    private JPanel createEmailPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Configuration Email ",
                TitledBorder.LEFT, TitledBorder.TOP));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Email
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Adresse email :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        tfEmail = new JTextField(25);
        panel.add(tfEmail, gbc);

        // Mot de passe
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Mot de passe app :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        tfPassword = new JPasswordField(25);
        panel.add(tfPassword, gbc);

        // SMTP Host + Port
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Serveur SMTP :"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        tfSmtpHost = new JTextField(15);
        panel.add(tfSmtpHost, gbc);
        gbc.gridx = 2; gbc.weightx = 0.3;
        tfSmtpPort = new JTextField(5);
        panel.add(tfSmtpPort, gbc);

        // IMAP Host + Port
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Serveur IMAP :"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        tfImapHost = new JTextField(15);
        panel.add(tfImapHost, gbc);
        gbc.gridx = 2; gbc.weightx = 0.3;
        tfImapPort = new JTextField(5);
        panel.add(tfImapPort, gbc);

        // Note Gmail
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3; gbc.weightx = 1;
        JLabel lblNote = new JLabel(
                "<html><i>Pour Gmail, utilisez un <b>mot de passe d'application</b> " +
                        "(Paramètres Google → Sécurité → Mots de passe des applications)</i></html>");
        lblNote.setForeground(Color.GRAY);
        lblNote.setFont(lblNote.getFont().deriveFont(11f));
        panel.add(lblNote, gbc);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    /**
     * Crée le panneau de configuration de l'Autorité de Confiance.
     */
    private JPanel createTAPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Autorité de Confiance (IBE) ",
                TitledBorder.LEFT, TitledBorder.TOP));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // TA Host
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Serveur AC :"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        tfTaHost = new JTextField(15);
        panel.add(tfTaHost, gbc);
        gbc.gridx = 2; gbc.weightx = 0.3;
        tfTaPort = new JTextField(5);
        panel.add(tfTaPort, gbc);

        // Boutons AC
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3; gbc.weightx = 1;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        JButton btnGetParams = new JButton("🔑 Récupérer les paramètres IBE");
        btnGetParams.addActionListener(e -> fetchSystemParams());

        JButton btnExtractKey = new JButton("🔐 Demander ma clé privée");
        btnExtractKey.addActionListener(e -> extractPrivateKey());

        JButton btnLaunchTA = new JButton("🖥 Lancer le serveur AC");
        btnLaunchTA.addActionListener(e -> launchTrustAuthority());

        btnPanel.add(btnGetParams);
        btnPanel.add(btnExtractKey);
        btnPanel.add(btnLaunchTA);
        panel.add(btnPanel, gbc);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    /**
     * Crée le panneau d'état IBE.
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " État du système IBE ",
                TitledBorder.LEFT, TitledBorder.TOP));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Paramètres IBE :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        lblIbeStatus = new JLabel(mainFrame.hasSystemParams() ? "✅ Chargés" : "❌ Non chargés");
        panel.add(lblIbeStatus, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Clé privée :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        lblKeyStatus = new JLabel(mainFrame.getPrivateKey() != null ? "✅ Disponible" : "❌ Non disponible");
        panel.add(lblKeyStatus, gbc);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    // ==================== ACTIONS ====================

    /**
     * Charge les valeurs de la configuration dans les champs de l'interface.
     */
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

    /**
     * Sauvegarde les valeurs des champs dans la configuration.
     */
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
        mainFrame.updateStatus("Configuration sauvegardée.");
        mainFrame.showInfo("Sauvegarde", "Configuration sauvegardée avec succès.");
    }

    /**
     * Récupère les paramètres publics IBE depuis l'Autorité de Confiance.
     */
    private void fetchSystemParams() {
        mainFrame.updateStatus("Connexion à l'Autorité de Confiance...");

        new Thread(() -> {
            try {
                TrustAuthorityClient taClient = new TrustAuthorityClient(
                        tfTaHost.getText().trim(),
                        Integer.parseInt(tfTaPort.getText().trim())
                );

                SystemParameters params = taClient.getParameters();
                mainFrame.setSystemParams(params);

                SwingUtilities.invokeLater(() -> {
                    lblIbeStatus.setText("✅ Chargés");
                    mainFrame.showInfo("Succès",
                            "Paramètres IBE récupérés avec succès depuis l'Autorité de Confiance.");
                });

            } catch (Exception ex) {
                mainFrame.showError("Erreur de connexion",
                        "Impossible de contacter l'Autorité de Confiance :\n" + ex.getMessage());
                mainFrame.updateStatus("Erreur de connexion à l'AC.");
            }
        }).start();
    }

    /**
     * Demande l'extraction de la clé privée à l'Autorité de Confiance.
     */
    private void extractPrivateKey() {
        String email = tfEmail.getText().trim();
        if (email.isEmpty()) {
            mainFrame.showError("Erreur", "Veuillez renseigner votre adresse email dans la configuration.");
            return;
        }

        mainFrame.updateStatus("Demande de clé privée pour " + email + "...");

        new Thread(() -> {
            try {
                TrustAuthorityClient taClient = new TrustAuthorityClient(
                        tfTaHost.getText().trim(),
                        Integer.parseInt(tfTaPort.getText().trim())
                );

                byte[] key = taClient.extractKey(email);
                mainFrame.setPrivateKey(key);

                SwingUtilities.invokeLater(() -> {
                    lblKeyStatus.setText("✅ Disponible");
                    mainFrame.showInfo("Succès",
                            "Clé privée IBE reçue pour : " + email +
                                    "\n(" + key.length + " octets)");
                });

            } catch (Exception ex) {
                mainFrame.showError("Erreur d'extraction",
                        "Impossible d'extraire la clé privée :\n" + ex.getMessage());
                mainFrame.updateStatus("Erreur d'extraction de la clé privée.");
            }
        }).start();
    }

    /**
     * Lance le serveur de l'Autorité de Confiance dans une fenêtre séparée.
     */
    private void launchTrustAuthority() {
        int port;
        try {
            port = Integer.parseInt(tfTaPort.getText().trim());
        } catch (NumberFormatException e) {
            port = 7777;
        }
        TrustAuthorityFrame taFrame = new TrustAuthorityFrame(port);
        taFrame.setVisible(true);
    }

    /**
     * Met à jour l'affichage de l'état IBE.
     */
    public void refreshStatus() {
        lblIbeStatus.setText(mainFrame.hasSystemParams() ? "✅ Chargés" : "❌ Non chargés");
        lblKeyStatus.setText(mainFrame.getPrivateKey() != null ? "✅ Disponible" : "❌ Non disponible");
    }
}
