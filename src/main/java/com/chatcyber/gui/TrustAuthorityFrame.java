package com.chatcyber.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.chatcyber.DebugFlags;
import com.chatcyber.crypto.TrustAuthorityServer;
import com.chatcyber.mail.MailConfig;

/**
 * Fenetre de gestion de l'Autorite de Confiance (Trust Authority).
 * Theme sombre professionnel avec terminal de logs.
 */
public class TrustAuthorityFrame extends JFrame {

    private static final Color DARK_BG      = new Color(24, 24, 27);
    private static final Color DARK_CARD    = new Color(39, 39, 42);
    private static final Color DARK_BORDER  = new Color(63, 63, 70);
    private static final Color DARK_TEXT    = new Color(228, 228, 231);
    private static final Color DARK_MUTED   = new Color(161, 161, 170);
    private static final Color GREEN_TERM   = new Color(52, 211, 153);
    private static final Color GREEN_BRIGHT = new Color(16, 185, 129);
    private static final Color RED_BRIGHT   = new Color(248, 113, 113);
    private static final Color BLUE_ACCENT  = new Color(96, 165, 250);

        /** Flag de debug : expose la clé privée IBE en clair dans l'UI (voir DebugFlags). */
        private static final boolean DEBUG_EXPOSE_IBE_PRIVATE_KEY = DebugFlags.EXPOSE_IBE_PRIVATE_KEY;

    private final int port;
    private TrustAuthorityServer server;
    private MailConfig mailConfig;

    private JTextArea taLog;
    private JTextArea taIbePrivateKeyPlaintext;
    private JButton btnStart;
    private JButton btnStop;
    private JButton btnConfigEmail;
    private JLabel lblStatus;
    private JLabel lblEmailStatus;
    private JTextField tfPort;
    private JLabel lblConnections;
    private JLabel lblIbePrivateKeyIdentity;
    private int connectionCount;

    public TrustAuthorityFrame(int port) {
        super("Autorite de Confiance - Serveur IBE Boneh-Franklin");
        this.port = port;
        this.connectionCount = 0;
        setSize(750, 550);
        setMinimumSize(new Dimension(550, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
            }
        });
    }

    private void initComponents() {
        getContentPane().setBackground(DARK_BG);
        setLayout(new BorderLayout(0, 0));

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 35));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DARK_BORDER),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        JLabel titleLabel = new JLabel("Autorite de Confiance");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(DARK_TEXT);

        JLabel subLabel = new JLabel("   Serveur IBE | Boneh-Franklin | Courbe Type A");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(DARK_MUTED);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(subLabel);
        header.add(titlePanel, BorderLayout.WEST);

        // Status badge
        lblStatus = new JLabel("  Arrete  ");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(DARK_MUTED);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(DARK_CARD);
        lblStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        header.add(lblStatus, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ── Controls ──
        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        controlBar.setBackground(DARK_CARD);
        controlBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DARK_BORDER),
                BorderFactory.createEmptyBorder(4, 16, 4, 16)
        ));

        JLabel portLabel = new JLabel("Port :");
        portLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        portLabel.setForeground(DARK_TEXT);
        controlBar.add(portLabel);

        tfPort = new JTextField(String.valueOf(port), 6);
        tfPort.setFont(new Font("Consolas", Font.PLAIN, 13));
        tfPort.setBackground(DARK_BG);
        tfPort.setForeground(DARK_TEXT);
        tfPort.setCaretColor(DARK_TEXT);
        tfPort.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        controlBar.add(tfPort);

        controlBar.add(Box.createHorizontalStrut(8));

        btnStart = createDarkButton("Demarrer", GREEN_BRIGHT);
        btnStart.addActionListener(e -> startServer());
        controlBar.add(btnStart);

        btnStop = createDarkButton("Arreter", RED_BRIGHT);
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopServer());
        controlBar.add(btnStop);

        controlBar.add(Box.createHorizontalStrut(20));

        btnConfigEmail = createDarkButton("Config Email", BLUE_ACCENT);
        btnConfigEmail.addActionListener(e -> showEmailConfigDialog());
        controlBar.add(btnConfigEmail);

        lblEmailStatus = new JLabel("E-mail : ✗ Non configuré");
        lblEmailStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEmailStatus.setForeground(RED_BRIGHT);
        controlBar.add(Box.createHorizontalStrut(20));
        controlBar.add(lblEmailStatus);

        controlBar.add(Box.createHorizontalStrut(20));

        lblConnections = new JLabel("Connexions : 0");
        lblConnections.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblConnections.setForeground(DARK_MUTED);
        controlBar.add(lblConnections);

        add(controlBar, BorderLayout.BEFORE_FIRST_LINE);
        // Since NORTH is taken, use a wrapper
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setBackground(DARK_BG);
        topWrapper.add(header, BorderLayout.NORTH);
        topWrapper.add(controlBar, BorderLayout.SOUTH);
        add(topWrapper, BorderLayout.NORTH);

        // ── Terminal ──
        taLog = new JTextArea();
        taLog.setEditable(false);
        taLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        taLog.setLineWrap(true);
        taLog.setWrapStyleWord(true);
        taLog.setBackground(DARK_BG);
        taLog.setForeground(GREEN_TERM);
        taLog.setCaretColor(GREEN_TERM);
        taLog.setMargin(new Insets(12, 16, 12, 16));
        taLog.setSelectionColor(new Color(55, 65, 81));
        taLog.setSelectedTextColor(GREEN_BRIGHT);

        JScrollPane logScroll = new JScrollPane(taLog);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.getViewport().setBackground(DARK_BG);
        logScroll.getVerticalScrollBar().setBackground(DARK_CARD);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(DARK_BG);
        centerWrapper.add(logScroll, BorderLayout.CENTER);

        if (DEBUG_EXPOSE_IBE_PRIVATE_KEY) {
            JPanel debugKeyPanel = new JPanel(new BorderLayout(0, 8));
            debugKeyPanel.setBackground(DARK_CARD);
            debugKeyPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DARK_BORDER),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
            ));

            JPanel debugHeader = new JPanel(new BorderLayout());
            debugHeader.setOpaque(false);

            JLabel keyTitle = new JLabel("Clé privée IBE (en clair) — Base64");
            keyTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
            keyTitle.setForeground(DARK_TEXT);
            debugHeader.add(keyTitle, BorderLayout.WEST);

            lblIbePrivateKeyIdentity = new JLabel("Identité : —");
            lblIbePrivateKeyIdentity.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblIbePrivateKeyIdentity.setForeground(DARK_MUTED);
            debugHeader.add(lblIbePrivateKeyIdentity, BorderLayout.EAST);

            debugKeyPanel.add(debugHeader, BorderLayout.NORTH);

            taIbePrivateKeyPlaintext = new JTextArea(3, 20);
            taIbePrivateKeyPlaintext.setEditable(false);
            taIbePrivateKeyPlaintext.setFont(new Font("Consolas", Font.PLAIN, 12));
            taIbePrivateKeyPlaintext.setLineWrap(true);
            taIbePrivateKeyPlaintext.setWrapStyleWord(false);
            taIbePrivateKeyPlaintext.setBackground(DARK_BG);
            taIbePrivateKeyPlaintext.setForeground(DARK_TEXT);
            taIbePrivateKeyPlaintext.setCaretColor(DARK_TEXT);
            taIbePrivateKeyPlaintext.setMargin(new Insets(8, 10, 8, 10));

            JScrollPane keyScroll = new JScrollPane(taIbePrivateKeyPlaintext);
            keyScroll.setBorder(BorderFactory.createLineBorder(DARK_BORDER, 1));
            keyScroll.getViewport().setBackground(DARK_BG);
            debugKeyPanel.add(keyScroll, BorderLayout.CENTER);

            centerWrapper.add(debugKeyPanel, BorderLayout.SOUTH);
        }

        add(centerWrapper, BorderLayout.CENTER);

        // ── Footer ──
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(DARK_CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DARK_BORDER),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        JLabel footerText = new JLabel(
                "L'AC genere les parametres IBE et distribue les cles privees aux utilisateurs.");
        footerText.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        footerText.setForeground(DARK_MUTED);
        footer.add(footerText, BorderLayout.WEST);

        add(footer, BorderLayout.SOUTH);

        // Welcome message
        appendLog("  +-------------------------------------------+\n");
        appendLog("  |  ChatCyber - Autorite de Confiance        |\n");
        appendLog("  |  Schema IBE de Boneh-Franklin             |\n");
        appendLog("  |  Courbe bilineaire Type A (r=160, q=512)  |\n");
        appendLog("  +-------------------------------------------+\n\n");
        appendLog("  Cliquez sur \"Demarrer\" pour initialiser le serveur.\n\n");
    }

    private JButton createDarkButton(String text, Color accent) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(accent);
        btn.setBackground(DARK_BG);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(accent.darker().darker());
                }
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(DARK_BG);
            }
        });
        return btn;
    }

    private void startServer() {
        int serverPort;
        try {
            serverPort = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnStart.setEnabled(false);
        btnConfigEmail.setEnabled(false);
        tfPort.setEnabled(false);
        connectionCount = 0;

        appendLog(timestamp() + " Demarrage du serveur sur le port " + serverPort + "...\n");

        new Thread(() -> {
            try {
                server = new TrustAuthorityServer(serverPort);
                server.setMessageListener(msg -> SwingUtilities.invokeLater(() -> {
                    appendLog(timestamp() + " " + msg + "\n");
                    if (msg.contains("Connexion entrante")) {
                        connectionCount++;
                        lblConnections.setText("Connexions : " + connectionCount);
                    }
                }));

                // Configurer le serveur avec les paramètres email
                if (mailConfig != null) {
                    server.setMailConfig(mailConfig);
                    appendLog(timestamp() + " Configuration email chargée : " + mailConfig.getEmail() + "\n");
                }

                if (DEBUG_EXPOSE_IBE_PRIVATE_KEY) {
                    server.setIbePrivateKeyListener((identity, ibePrivateKey) -> SwingUtilities.invokeLater(() -> {
                        updateIbePrivateKeyPlaintext(identity, ibePrivateKey);
                    }));
                }
                server.start();

                SwingUtilities.invokeLater(() -> {
                    btnStop.setEnabled(true);
                    lblStatus.setText("  En marche (:" + serverPort + ")  ");
                    lblStatus.setForeground(GREEN_BRIGHT);
                    lblStatus.setBackground(new Color(6, 78, 59));
                    lblStatus.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(GREEN_BRIGHT.darker(), 1),
                            BorderFactory.createEmptyBorder(4, 12, 4, 12)
                    ));
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    appendLog(timestamp() + " ERREUR : " + ex.getMessage() + "\n");
                    btnStart.setEnabled(true);
                    btnConfigEmail.setEnabled(true);
                    tfPort.setEnabled(true);
                    lblStatus.setText("  Erreur  ");
                    lblStatus.setForeground(RED_BRIGHT);
                    lblStatus.setBackground(new Color(127, 29, 29));
                });
            }
        }).start();
    }

    private void updateIbePrivateKeyPlaintext(String identity, byte[] ibePrivateKey) {
        if (taIbePrivateKeyPlaintext == null || lblIbePrivateKeyIdentity == null) {
            return;
        }
        lblIbePrivateKeyIdentity.setText("Identité : " + identity);
        taIbePrivateKeyPlaintext.setText(Base64.getEncoder().encodeToString(ibePrivateKey));
        taIbePrivateKeyPlaintext.setCaretPosition(0);
    }

    private void stopServer() {
        if (server != null && server.isRunning()) {
            server.stop();
            appendLog(timestamp() + " Serveur arrete.\n");
        }

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnConfigEmail.setEnabled(true);
        tfPort.setEnabled(true);
        lblStatus.setText("  Arrete  ");
        lblStatus.setForeground(DARK_MUTED);
        lblStatus.setBackground(DARK_CARD);
        lblStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
    }

    private void showEmailConfigDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(DARK_BG);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        if (mailConfig == null) {
            mailConfig = new MailConfig();
        }
        
        // Email
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblEmail = new JLabel("E-mail :");
        lblEmail.setForeground(DARK_TEXT);
        panel.add(lblEmail, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField tfEmail = new JTextField(mailConfig.getEmail(), 25);
        tfEmail.setBackground(DARK_CARD);
        tfEmail.setForeground(DARK_TEXT);
        tfEmail.setCaretColor(DARK_TEXT);
        panel.add(tfEmail, gbc);
        
        // Mot de passe
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel lblPassword = new JLabel("Mot de passe :");
        lblPassword.setForeground(DARK_TEXT);
        panel.add(lblPassword, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField tfPassword = new JPasswordField(mailConfig.getPassword(), 25);
        tfPassword.setBackground(DARK_CARD);
        tfPassword.setForeground(DARK_TEXT);
        tfPassword.setCaretColor(DARK_TEXT);
        panel.add(tfPassword, gbc);
        
        // SMTP Host
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        JLabel lblSmtpHost = new JLabel("Serveur SMTP :");
        lblSmtpHost.setForeground(DARK_TEXT);
        panel.add(lblSmtpHost, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField tfSmtpHost = new JTextField(mailConfig.getSmtpHost(), 25);
        tfSmtpHost.setBackground(DARK_CARD);
        tfSmtpHost.setForeground(DARK_TEXT);
        tfSmtpHost.setCaretColor(DARK_TEXT);
        panel.add(tfSmtpHost, gbc);
        
        // SMTP Port
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        JLabel lblSmtpPort = new JLabel("Port SMTP :");
        lblSmtpPort.setForeground(DARK_TEXT);
        panel.add(lblSmtpPort, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField tfSmtpPort = new JTextField(String.valueOf(mailConfig.getSmtpPort()), 25);
        tfSmtpPort.setBackground(DARK_CARD);
        tfSmtpPort.setForeground(DARK_TEXT);
        tfSmtpPort.setCaretColor(DARK_TEXT);
        panel.add(tfSmtpPort, gbc);
        
        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Configuration E-mail",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                mailConfig.setEmail(tfEmail.getText().trim());
                mailConfig.setPassword(new String(tfPassword.getPassword()));
                mailConfig.setSmtpHost(tfSmtpHost.getText().trim());
                mailConfig.setSmtpPort(Integer.parseInt(tfSmtpPort.getText().trim()));
                
                lblEmailStatus.setText("E-mail : ✓ " + mailConfig.getEmail());
                lblEmailStatus.setForeground(GREEN_BRIGHT);
                
                appendLog(timestamp() + " Configuration email mise à jour : " + mailConfig.getEmail() + "\n");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Port SMTP invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String timestamp() {
        return "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]";
    }

    private void appendLog(String text) {
        taLog.append(text);
        taLog.setCaretPosition(taLog.getDocument().getLength());
    }
}
