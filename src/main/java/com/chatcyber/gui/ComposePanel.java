package com.chatcyber.gui;

import com.chatcyber.crypto.IBECipher;
import com.chatcyber.mail.MailConfig;
import com.chatcyber.mail.MailSender;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

/**
 * Panneau de composition et d'envoi de mails.
 *
 * Fonctionnalités :
 *   - Saisie du destinataire, objet, corps du message
 *   - Sélection d'une pièce jointe
 *   - Option de chiffrement IBE de la pièce jointe
 *   - Envoi du mail via SMTP
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
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        initComponents();
    }

    private void initComponents() {
        // ====== Panneau d'en-tête (destinataire, objet) ======
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Nouveau message ",
                TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Destinataire
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        headerPanel.add(new JLabel("Destinataire :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfTo = new JTextField(30);
        tfTo.setToolTipText("Adresse email du destinataire");
        headerPanel.add(tfTo, gbc);

        // Objet
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        headerPanel.add(new JLabel("Objet :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfSubject = new JTextField(30);
        headerPanel.add(tfSubject, gbc);

        add(headerPanel, BorderLayout.NORTH);

        // ====== Corps du message ======
        taBody = new JTextArea();
        taBody.setLineWrap(true);
        taBody.setWrapStyleWord(true);
        taBody.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        taBody.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane bodyScroll = new JScrollPane(taBody);
        bodyScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Corps du message ",
                TitledBorder.LEFT, TitledBorder.TOP));
        add(bodyScroll, BorderLayout.CENTER);

        // ====== Panneau du bas (pièce jointe + envoi) ======
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        // Pièce jointe
        JPanel attachPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        attachPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Pièce jointe ",
                TitledBorder.LEFT, TitledBorder.TOP));

        tfAttachment = new JTextField(30);
        tfAttachment.setEditable(false);
        tfAttachment.setText("Aucun fichier sélectionné");

        JButton btnBrowse = new JButton("📁 Parcourir...");
        btnBrowse.addActionListener(e -> browseFile());

        JButton btnClear = new JButton("✖ Retirer");
        btnClear.addActionListener(e -> {
            selectedFile = null;
            tfAttachment.setText("Aucun fichier sélectionné");
        });

        cbEncrypt = new JCheckBox("Chiffrer avec IBE (identité du destinataire)", true);
        cbEncrypt.setToolTipText("Chiffre la pièce jointe avec l'identité IBE du destinataire");

        attachPanel.add(tfAttachment);
        attachPanel.add(btnBrowse);
        attachPanel.add(btnClear);
        attachPanel.add(cbEncrypt);

        bottomPanel.add(attachPanel);

        // Barre de progression + bouton envoi
        JPanel sendPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(200, 22));
        sendPanel.add(progressBar);

        btnSend = new JButton("📤 Envoyer");
        btnSend.setFont(btnSend.getFont().deriveFont(Font.BOLD, 14f));
        btnSend.setPreferredSize(new Dimension(150, 35));
        btnSend.addActionListener(e -> sendEmail());
        sendPanel.add(btnSend);

        bottomPanel.add(sendPanel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Ouvre un sélecteur de fichier pour choisir la pièce jointe.
     */
    private void browseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Sélectionner une pièce jointe");
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            tfAttachment.setText(selectedFile.getName() + " (" + formatSize(selectedFile.length()) + ")");
        }
    }

    /**
     * Envoie le mail (avec chiffrement optionnel de la pièce jointe).
     */
    private void sendEmail() {
        // Validation
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

        // Vérifier les prérequis IBE si chiffrement demandé
        if (selectedFile != null && cbEncrypt.isSelected()) {
            if (!mainFrame.hasSystemParams()) {
                mainFrame.showError("Erreur IBE",
                        "Les paramètres IBE ne sont pas chargés.\n" +
                                "Allez dans Configuration → Récupérer les paramètres IBE.");
                return;
            }
        }

        // Envoi en arrière-plan
        btnSend.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        mainFrame.updateStatus("Envoi en cours...");

        new Thread(() -> {
            try {
                File fileToAttach = selectedFile;

                // Chiffrement IBE de la pièce jointe si demandé
                if (selectedFile != null && cbEncrypt.isSelected()) {
                    mainFrame.updateStatus("Chiffrement de la pièce jointe avec IBE...");

                    IBECipher cipher = new IBECipher(mainFrame.getSystemParams());
                    File encryptedFile = new File(
                            System.getProperty("java.io.tmpdir"),
                            selectedFile.getName() + ".ibe"
                    );
                    cipher.encryptFile(selectedFile, encryptedFile, to);
                    fileToAttach = encryptedFile;

                    System.out.println("[IBE] Fichier chiffré : " + encryptedFile.getAbsolutePath());
                }

                // Envoi du mail
                mainFrame.updateStatus("Envoi du mail à " + to + "...");
                MailSender sender = new MailSender(mainFrame.getMailConfig());
                sender.sendEmail(to, subject, body, fileToAttach);

                SwingUtilities.invokeLater(() -> {
                    mainFrame.updateStatus("Mail envoyé avec succès à " + to);
                    mainFrame.showInfo("Succès", "Mail envoyé avec succès à " + to);
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

    /**
     * Réinitialise le formulaire après un envoi réussi.
     */
    private void clearForm() {
        tfTo.setText("");
        tfSubject.setText("");
        taBody.setText("");
        selectedFile = null;
        tfAttachment.setText("Aucun fichier sélectionné");
    }

    /**
     * Formate une taille en octets pour l'affichage.
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        if (bytes < 1024 * 1024) return String.format("%.1f Ko", bytes / 1024.0);
        return String.format("%.1f Mo", bytes / (1024.0 * 1024));
    }
}
