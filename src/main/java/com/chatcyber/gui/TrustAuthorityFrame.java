package com.chatcyber.gui;

import com.chatcyber.crypto.TrustAuthorityServer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Fenêtre de gestion de l'Autorité de Confiance (Trust Authority).
 *
 * Permet de :
 *   - Démarrer/arrêter le serveur AC
 *   - Visualiser les logs du serveur (connexions, extraction de clés)
 *   - Voir l'état du système IBE
 *
 * Cette fenêtre peut être lancée indépendamment ou depuis le panneau de configuration.
 */
public class TrustAuthorityFrame extends JFrame {

    private final int port;
    private TrustAuthorityServer server;

    private JTextArea taLog;
    private JButton btnStart;
    private JButton btnStop;
    private JLabel lblStatus;
    private JTextField tfPort;

    public TrustAuthorityFrame(int port) {
        super("Autorité de Confiance — Serveur IBE");
        this.port = port;
        setSize(650, 500);
        setMinimumSize(new Dimension(500, 350));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();

        // Arrêter le serveur à la fermeture de la fenêtre
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ====== Panneau de contrôle ======
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Contrôle du serveur ",
                TitledBorder.LEFT, TitledBorder.TOP));

        controlPanel.add(new JLabel("Port :"));
        tfPort = new JTextField(String.valueOf(port), 6);
        controlPanel.add(tfPort);

        btnStart = new JButton("▶ Démarrer");
        btnStart.setForeground(new Color(0, 128, 0));
        btnStart.addActionListener(e -> startServer());
        controlPanel.add(btnStart);

        btnStop = new JButton("⏹ Arrêter");
        btnStop.setForeground(Color.RED);
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopServer());
        controlPanel.add(btnStop);

        lblStatus = new JLabel("  ⚪ Arrêté");
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD));
        controlPanel.add(lblStatus);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // ====== Logs ======
        taLog = new JTextArea();
        taLog.setEditable(false);
        taLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        taLog.setLineWrap(true);
        taLog.setWrapStyleWord(true);
        taLog.setBackground(new Color(30, 30, 30));
        taLog.setForeground(new Color(0, 255, 100));
        taLog.setCaretColor(Color.WHITE);
        taLog.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane logScroll = new JScrollPane(taLog);
        logScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Journal du serveur ",
                TitledBorder.LEFT, TitledBorder.TOP));
        mainPanel.add(logScroll, BorderLayout.CENTER);

        // ====== Info ======
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel(
                "<html><i>L'Autorité de Confiance gère les paramètres IBE et génère les clés privées des utilisateurs.</i></html>"));
        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Démarre le serveur de l'Autorité de Confiance.
     */
    private void startServer() {
        int serverPort;
        try {
            serverPort = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Port invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnStart.setEnabled(false);
        tfPort.setEnabled(false);
        appendLog("Démarrage du serveur sur le port " + serverPort + "...\n");

        new Thread(() -> {
            try {
                server = new TrustAuthorityServer(serverPort);
                server.setMessageListener(msg ->
                        SwingUtilities.invokeLater(() -> {
                            appendLog(msg + "\n");
                        })
                );
                server.start();

                SwingUtilities.invokeLater(() -> {
                    btnStop.setEnabled(true);
                    lblStatus.setText("  🟢 En marche (port " + serverPort + ")");
                    lblStatus.setForeground(new Color(0, 128, 0));
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    appendLog("ERREUR : " + ex.getMessage() + "\n");
                    btnStart.setEnabled(true);
                    tfPort.setEnabled(true);
                    lblStatus.setText("  🔴 Erreur");
                    lblStatus.setForeground(Color.RED);
                });
            }
        }).start();
    }

    /**
     * Arrête le serveur.
     */
    private void stopServer() {
        if (server != null && server.isRunning()) {
            server.stop();
            appendLog("Serveur arrêté.\n");
        }

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        tfPort.setEnabled(true);
        lblStatus.setText("  ⚪ Arrêté");
        lblStatus.setForeground(Color.GRAY);
    }

    /**
     * Ajoute un message au journal.
     */
    private void appendLog(String text) {
        taLog.append(text);
        taLog.setCaretPosition(taLog.getDocument().getLength());
    }
}
