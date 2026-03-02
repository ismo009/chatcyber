package com.chatcyber.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.chatcyber.crypto.IBECipher;
import com.chatcyber.mail.MailSender;

/**
 * Panneau de composition et d'envoi de mails avec chiffrement IBE.
 */
public class ComposePanel extends JPanel {

    private final MainFrame mainFrame;

    private JTextField tfTo;
    private JTextField tfSubject;
    private JTextArea taBody;
    private JTextField tfAttachment;
    private File selectedFile;
    private JCheckBox cbEncrypt;
    private JButton btnSend;
    private JProgressBar progressBar;

    public ComposePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_MAIN);
        initComponents();
    }

    private void initComponents() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(UITheme.BG_MAIN);

        // ── Header ──
        JPanel headerCard = UITheme.headerPanel(
                "Composer un message",
                "Redigez et envoyez un email securise avec chiffrement IBE."
        );
        content.add(headerCard, BorderLayout.NORTH);

        // ── Zone centrale ──
        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        // Carte destinataire / objet
        JPanel fieldsCard = UITheme.card("Destinataire");
        fieldsCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        fieldsCard.add(UITheme.formLabel("A :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfTo = UITheme.styledTextField(30);
        tfTo.setToolTipText("Adresse email du destinataire");
        fieldsCard.add(tfTo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        fieldsCard.add(UITheme.formLabel("Objet :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfSubject = UITheme.styledTextField(30);
        fieldsCard.add(tfSubject, gbc);

        centerPanel.add(fieldsCard, BorderLayout.NORTH);

        // Corps du message
        JPanel bodyCard = new JPanel(new BorderLayout());
        bodyCard.setBackground(UITheme.BG_CARD);
        bodyCard.setBorder(UITheme.cardBorderWithTitle("Message"));

        taBody = new JTextArea();
        taBody.setLineWrap(true);
        taBody.setWrapStyleWord(true);
        taBody.setFont(UITheme.FONT_BODY);
        taBody.setMargin(new Insets(10, 12, 10, 12));
        taBody.setBorder(null);

        JScrollPane bodyScroll = new JScrollPane(taBody);
        bodyScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        bodyCard.add(bodyScroll, BorderLayout.CENTER);

        centerPanel.add(bodyCard, BorderLayout.CENTER);
        content.add(centerPanel, BorderLayout.CENTER);

        // ── Panneau du bas ──
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        // Piece jointe
        JPanel attachCard = UITheme.card("Piece jointe");
        attachCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
        attachCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        attachCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        tfAttachment = UITheme.styledTextField(28);
        tfAttachment.setEditable(false);
        tfAttachment.setText("Aucun fichier selectionne");
        tfAttachment.setForeground(UITheme.TEXT_MUTED);

        JButton btnBrowse = UITheme.outlineButton("Parcourir...");
        btnBrowse.addActionListener(e -> browseFile());

        JButton btnClear = UITheme.dangerButton("X");
        btnClear.setToolTipText("Retirer la piece jointe");
        btnClear.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btnClear.addActionListener(e -> {
            selectedFile = null;
            tfAttachment.setText("Aucun fichier selectionne");
            tfAttachment.setForeground(UITheme.TEXT_MUTED);
        });

        cbEncrypt = new JCheckBox("Chiffrer avec IBE (identite du destinataire)");
        cbEncrypt.setSelected(true);
        cbEncrypt.setFont(UITheme.FONT_BODY);
        cbEncrypt.setOpaque(false);
        cbEncrypt.setForeground(UITheme.TEXT_PRIMARY);
        cbEncrypt.setToolTipText("Chiffre la piece jointe avec l'identite IBE du destinataire");

        attachCard.add(tfAttachment);
        attachCard.add(btnBrowse);
        attachCard.add(btnClear);
        attachCard.add(cbEncrypt);

        bottomPanel.add(attachCard);
        bottomPanel.add(Box.createVerticalStrut(10));

        // Barre de progression + bouton envoi
        JPanel sendBar = new JPanel(new BorderLayout());
        sendBar.setOpaque(false);
        sendBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        sendBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(200, 8));
        progressBar.setBorderPainted(false);

        JPanel progressWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        progressWrap.setOpaque(false);
        progressWrap.add(progressBar);

        btnSend = UITheme.successButton("Envoyer le message");
        btnSend.setPreferredSize(new Dimension(200, 40));
        btnSend.addActionListener(e -> sendEmail());

        sendBar.add(progressWrap, BorderLayout.CENTER);
        sendBar.add(btnSend, BorderLayout.EAST);

        bottomPanel.add(sendBar);
        content.add(bottomPanel, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private void browseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selectionner une piece jointe");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            tfAttachment.setText(selectedFile.getName() + " (" + formatSize(selectedFile.length()) + ")");
            tfAttachment.setForeground(UITheme.TEXT_PRIMARY);
        }
    }

    private void sendEmail() {
        String to = tfTo.getText().trim();
        String subject = tfSubject.getText().trim();
        String body = taBody.getText();

        if (to.isEmpty()) {
            mainFrame.showError("Erreur", "Veuillez saisir l'adresse du destinataire.");
            return;
        }
        if (!mainFrame.getMailConfig().isEmailConfigured()) {
            mainFrame.showError("Erreur", "Veuillez configurer vos identifiants email dans l'onglet Configuration.");
            return;
        }
        if (selectedFile != null && cbEncrypt.isSelected() && !mainFrame.hasSystemParams()) {
            mainFrame.showError("Erreur IBE",
                    "Les parametres IBE ne sont pas charges.\nAllez dans Configuration > Recuperer les parametres IBE.");
            return;
        }

        btnSend.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        mainFrame.updateStatus("Envoi en cours...");

        new Thread(() -> {
            try {
                File fileToAttach = selectedFile;

                if (selectedFile != null && cbEncrypt.isSelected()) {
                    mainFrame.updateStatus("Chiffrement de la piece jointe...");
                    IBECipher cipher = new IBECipher(mainFrame.getSystemParams());
                    File encryptedFile = new File(
                            System.getProperty("java.io.tmpdir"),
                            selectedFile.getName() + ".ibe"
                    );
                    cipher.encryptFile(selectedFile, encryptedFile, to);
                    fileToAttach = encryptedFile;
                }

                mainFrame.updateStatus("Envoi du mail a " + to + "...");
                MailSender sender = new MailSender(mainFrame.getMailConfig());
                sender.sendEmail(to, subject, body, fileToAttach);

                SwingUtilities.invokeLater(() -> {
                    mainFrame.updateStatus("Mail envoye avec succes a " + to);
                    mainFrame.showInfo("Succes", "Mail envoye avec succes a " + to);
                    clearForm();
                });

            } catch (Exception ex) {
                mainFrame.showError("Erreur d'envoi",
                        "Impossible d'envoyer le mail :\n" + ex.getMessage());
                mainFrame.updateStatus("Erreur d'envoi.");
            } finally {
                SwingUtilities.invokeLater(() -> {
                    btnSend.setEnabled(true);
                    progressBar.setVisible(false);
                });
            }
        }).start();
    }

    private void clearForm() {
        tfTo.setText("");
        tfSubject.setText("");
        taBody.setText("");
        selectedFile = null;
        tfAttachment.setText("Aucun fichier selectionne");
        tfAttachment.setForeground(UITheme.TEXT_MUTED);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        if (bytes < 1024 * 1024) return String.format("%.1f Ko", bytes / 1024.0);
        return String.format("%.1f Mo", bytes / (1024.0 * 1024));
    }
}
