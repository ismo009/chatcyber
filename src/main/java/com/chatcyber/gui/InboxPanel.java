package com.chatcyber.gui;

import com.chatcyber.crypto.IBECipher;
import com.chatcyber.mail.EmailMessage;
import com.chatcyber.mail.MailConfig;
import com.chatcyber.mail.MailReceiver;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Panneau de la boîte de réception.
 *
 * Fonctionnalités :
 *   - Liste des emails reçus (tableau)
 *   - Aperçu du message sélectionné (corps + liste des pièces jointes)
 *   - Téléchargement des pièces jointes
 *   - Déchiffrement IBE des pièces jointes chiffrées (.ibe)
 */
public class InboxPanel extends JPanel {

    private final MainFrame mainFrame;

    private JTable messageTable;
    private DefaultTableModel tableModel;
    private JTextArea taPreview;
    private JList<String> attachmentList;
    private DefaultListModel<String> attachmentListModel;
    private JButton btnRefresh;
    private JButton btnDownload;
    private JButton btnDecrypt;
    private JButton btnDecryptLocal;
    private JProgressBar progressBar;
    private JSpinner spinnerCount;

    private List<EmailMessage> messages = new ArrayList<>();
    private MailReceiver mailReceiver;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public InboxPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
    }

    private void initComponents() {
        // ====== Barre d'outils ======
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRefresh = new JButton("🔄 Actualiser");
        btnRefresh.addActionListener(e -> refreshInbox());

        toolbar.add(btnRefresh);
        toolbar.add(new JLabel("  Messages :"));
        spinnerCount = new JSpinner(new SpinnerNumberModel(20, 1, 200, 10));
        spinnerCount.setPreferredSize(new Dimension(70, 25));
        toolbar.add(spinnerCount);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(150, 20));
        toolbar.add(progressBar);

        add(toolbar, BorderLayout.NORTH);

        // ====== Panneau principal (split vertical) ======
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.5);

        // --- Tableau des messages ---
        String[] columns = {"De", "Objet", "Date", "📎"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        messageTable = new JTable(tableModel);
        messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messageTable.setRowHeight(24);
        messageTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        messageTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        messageTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        messageTable.getColumnModel().getColumn(3).setPreferredWidth(30);

        messageTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedMessage();
            }
        });

        JScrollPane tableScroll = new JScrollPane(messageTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Messages reçus ",
                TitledBorder.LEFT, TitledBorder.TOP));
        splitPane.setTopComponent(tableScroll);

        // --- Aperçu du message ---
        JPanel previewPanel = new JPanel(new BorderLayout(8, 8));
        previewPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Aperçu du message ",
                TitledBorder.LEFT, TitledBorder.TOP));

        taPreview = new JTextArea();
        taPreview.setEditable(false);
        taPreview.setLineWrap(true);
        taPreview.setWrapStyleWord(true);
        taPreview.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        taPreview.setMargin(new Insets(8, 8, 8, 8));
        JScrollPane previewScroll = new JScrollPane(taPreview);
        previewPanel.add(previewScroll, BorderLayout.CENTER);

        // Panneau des pièces jointes
        JPanel attachPanel = new JPanel(new BorderLayout(4, 4));
        attachPanel.setBorder(BorderFactory.createTitledBorder(" Pièces jointes "));
        attachPanel.setPreferredSize(new Dimension(250, 0));

        attachmentListModel = new DefaultListModel<>();
        attachmentList = new JList<>(attachmentListModel);
        attachmentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane attachScroll = new JScrollPane(attachmentList);
        attachPanel.add(attachScroll, BorderLayout.CENTER);

        JPanel attachBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        btnDownload = new JButton("💾 Télécharger");
        btnDownload.addActionListener(e -> downloadAttachment());
        btnDecrypt = new JButton("🔓 Télécharger + Déchiffrer");
        btnDecrypt.addActionListener(e -> downloadAndDecrypt());
        attachBtnPanel.add(btnDownload);
        attachBtnPanel.add(btnDecrypt);
        attachPanel.add(attachBtnPanel, BorderLayout.SOUTH);

        previewPanel.add(attachPanel, BorderLayout.EAST);
        splitPane.setBottomComponent(previewPanel);

        add(splitPane, BorderLayout.CENTER);

        // ====== Panneau du bas : déchiffrement local ======
        JPanel localDecryptPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        localDecryptPanel.setBorder(BorderFactory.createTitledBorder(" Déchiffrer un fichier local (.ibe) "));
        btnDecryptLocal = new JButton("📂 Sélectionner et déchiffrer un fichier .ibe");
        btnDecryptLocal.addActionListener(e -> decryptLocalFile());
        localDecryptPanel.add(btnDecryptLocal);
        add(localDecryptPanel, BorderLayout.SOUTH);
    }

    // ==================== ACTIONS ====================

    /**
     * Actualise la boîte de réception.
     */
    private void refreshInbox() {
        if (!mainFrame.getMailConfig().isEmailConfigured()) {
            mainFrame.showError("Erreur", "Veuillez configurer vos identifiants email.");
            return;
        }

        btnRefresh.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        mainFrame.updateStatus("Récupération des messages...");

        int count = (int) spinnerCount.getValue();

        new Thread(() -> {
            try {
                if (mailReceiver != null) {
                    mailReceiver.disconnect();
                }
                mailReceiver = new MailReceiver(mainFrame.getMailConfig());
                mailReceiver.connect();
                messages = mailReceiver.fetchMessages(count);

                SwingUtilities.invokeLater(() -> {
                    updateMessageTable();
                    mainFrame.updateStatus(messages.size() + " messages récupérés.");
                });

            } catch (Exception ex) {
                mainFrame.showError("Erreur IMAP",
                        "Impossible de récupérer les messages :\n" + ex.getMessage());
                mainFrame.updateStatus("Erreur de réception.");
            } finally {
                SwingUtilities.invokeLater(() -> {
                    btnRefresh.setEnabled(true);
                    progressBar.setVisible(false);
                });
            }
        }).start();
    }

    /**
     * Met à jour le tableau des messages.
     */
    private void updateMessageTable() {
        tableModel.setRowCount(0);
        for (EmailMessage msg : messages) {
            String date = msg.getSentDate() != null ? DATE_FORMAT.format(msg.getSentDate()) : "";
            String hasAttach = msg.hasAttachments() ?
                    (msg.hasEncryptedAttachments() ? "🔒" : "📎") : "";
            tableModel.addRow(new Object[]{msg.getFrom(), msg.getSubject(), date, hasAttach});
        }
    }

    /**
     * Affiche l'aperçu du message sélectionné.
     */
    private void showSelectedMessage() {
        int row = messageTable.getSelectedRow();
        if (row < 0 || row >= messages.size()) {
            taPreview.setText("");
            attachmentListModel.clear();
            return;
        }

        EmailMessage msg = messages.get(row);

        // Afficher le corps
        StringBuilder preview = new StringBuilder();
        preview.append("De : ").append(msg.getFrom()).append("\n");
        preview.append("Objet : ").append(msg.getSubject()).append("\n");
        if (msg.getSentDate() != null) {
            preview.append("Date : ").append(DATE_FORMAT.format(msg.getSentDate())).append("\n");
        }
        preview.append("─".repeat(50)).append("\n\n");
        preview.append(msg.getBodyText() != null ? msg.getBodyText() : "(pas de contenu)");
        taPreview.setText(preview.toString());
        taPreview.setCaretPosition(0);

        // Mettre à jour la liste des pièces jointes
        attachmentListModel.clear();
        if (msg.hasAttachments()) {
            for (EmailMessage.AttachmentInfo att : msg.getAttachments()) {
                attachmentListModel.addElement(att.toString());
            }
        }
    }

    /**
     * Télécharge la pièce jointe sélectionnée.
     */
    private void downloadAttachment() {
        int msgRow = messageTable.getSelectedRow();
        int attachIdx = attachmentList.getSelectedIndex();
        if (msgRow < 0 || attachIdx < 0) {
            mainFrame.showError("Sélection", "Veuillez sélectionner un message et une pièce jointe.");
            return;
        }

        EmailMessage msg = messages.get(msgRow);
        EmailMessage.AttachmentInfo attachInfo = msg.getAttachments().get(attachIdx);

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Enregistrer la pièce jointe");
        chooser.setSelectedFile(new File(attachInfo.getFileName()));
        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File destFile = chooser.getSelectedFile();
            mainFrame.updateStatus("Téléchargement de " + attachInfo.getFileName() + "...");

            new Thread(() -> {
                try {
                    File downloaded = mailReceiver.downloadAttachment(
                            msg.getMessageNumber(), attachInfo.getPartIndex(), destFile.getParentFile());

                    // Renommer si nécessaire
                    if (!downloaded.getName().equals(destFile.getName())) {
                        downloaded.renameTo(destFile);
                    }

                    mainFrame.showInfo("Téléchargement",
                            "Pièce jointe sauvegardée :\n" + destFile.getAbsolutePath());
                    mainFrame.updateStatus("Pièce jointe téléchargée.");

                } catch (Exception ex) {
                    mainFrame.showError("Erreur", "Erreur de téléchargement :\n" + ex.getMessage());
                }
            }).start();
        }
    }

    /**
     * Télécharge et déchiffre la pièce jointe sélectionnée (fichier .ibe).
     */
    private void downloadAndDecrypt() {
        int msgRow = messageTable.getSelectedRow();
        int attachIdx = attachmentList.getSelectedIndex();
        if (msgRow < 0 || attachIdx < 0) {
            mainFrame.showError("Sélection", "Veuillez sélectionner un message et une pièce jointe.");
            return;
        }

        if (!mainFrame.isIBEReady()) {
            mainFrame.showError("Erreur IBE",
                    "Le système IBE n'est pas prêt.\n" +
                            "Vérifiez que les paramètres et votre clé privée sont chargés.");
            return;
        }

        EmailMessage msg = messages.get(msgRow);
        EmailMessage.AttachmentInfo attachInfo = msg.getAttachments().get(attachIdx);

        if (!attachInfo.isEncrypted()) {
            mainFrame.showError("Erreur",
                    "Ce fichier ne semble pas être chiffré (extension .ibe attendue).");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Enregistrer le fichier déchiffré");
        // Proposer le nom sans .ibe
        String originalName = attachInfo.getFileName().replaceAll("\\.ibe$", "");
        chooser.setSelectedFile(new File(originalName));
        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File destFile = chooser.getSelectedFile();
            mainFrame.updateStatus("Téléchargement et déchiffrement...");

            new Thread(() -> {
                try {
                    // Télécharger dans un répertoire temporaire
                    File tempDir = new File(System.getProperty("java.io.tmpdir"), "chatcyber_temp");
                    tempDir.mkdirs();

                    File encryptedFile = mailReceiver.downloadAttachment(
                            msg.getMessageNumber(), attachInfo.getPartIndex(), tempDir);

                    // Déchiffrer
                    mainFrame.updateStatus("Déchiffrement IBE en cours...");
                    IBECipher cipher = new IBECipher(mainFrame.getSystemParams());
                    File decryptedFile = cipher.decryptFile(
                            encryptedFile, destFile.getParentFile(), mainFrame.getPrivateKey());

                    // Renommer au nom souhaité
                    if (!decryptedFile.getAbsolutePath().equals(destFile.getAbsolutePath())) {
                        if (destFile.exists()) destFile.delete();
                        decryptedFile.renameTo(destFile);
                    }

                    // Nettoyer le fichier temporaire
                    encryptedFile.delete();

                    mainFrame.showInfo("Déchiffrement",
                            "Fichier déchiffré avec succès :\n" + destFile.getAbsolutePath());
                    mainFrame.updateStatus("Fichier déchiffré.");

                } catch (Exception ex) {
                    mainFrame.showError("Erreur de déchiffrement",
                            "Impossible de déchiffrer le fichier :\n" + ex.getMessage() +
                                    "\n\nVérifiez que votre clé privée correspond à l'identité utilisée pour le chiffrement.");
                    mainFrame.updateStatus("Erreur de déchiffrement.");
                }
            }).start();
        }
    }

    /**
     * Déchiffre un fichier .ibe local (sans passer par l'email).
     */
    private void decryptLocalFile() {
        if (!mainFrame.isIBEReady()) {
            mainFrame.showError("Erreur IBE",
                    "Le système IBE n'est pas prêt.\n" +
                            "Vérifiez que les paramètres et votre clé privée sont chargés.");
            return;
        }

        // Sélectionner le fichier chiffré
        JFileChooser openChooser = new JFileChooser();
        openChooser.setDialogTitle("Sélectionner un fichier chiffré (.ibe)");
        openChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Fichiers chiffrés IBE (*.ibe)", "ibe"));
        int openResult = openChooser.showOpenDialog(this);

        if (openResult != JFileChooser.APPROVE_OPTION) return;

        File encryptedFile = openChooser.getSelectedFile();

        // Sélectionner le fichier de sortie
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Enregistrer le fichier déchiffré");
        String suggestedName = encryptedFile.getName().replaceAll("\\.ibe$", "");
        saveChooser.setSelectedFile(new File(encryptedFile.getParent(), suggestedName));
        int saveResult = saveChooser.showSaveDialog(this);

        if (saveResult != JFileChooser.APPROVE_OPTION) return;

        File outputFile = saveChooser.getSelectedFile();
        mainFrame.updateStatus("Déchiffrement du fichier local...");

        new Thread(() -> {
            try {
                IBECipher cipher = new IBECipher(mainFrame.getSystemParams());
                File decrypted = cipher.decryptFile(
                        encryptedFile, outputFile.getParentFile(), mainFrame.getPrivateKey());

                // Renommer si nécessaire
                if (!decrypted.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                    if (outputFile.exists()) outputFile.delete();
                    decrypted.renameTo(outputFile);
                }

                mainFrame.showInfo("Déchiffrement",
                        "Fichier déchiffré avec succès :\n" + outputFile.getAbsolutePath());
                mainFrame.updateStatus("Fichier déchiffré.");

            } catch (Exception ex) {
                mainFrame.showError("Erreur de déchiffrement",
                        "Impossible de déchiffrer :\n" + ex.getMessage());
                mainFrame.updateStatus("Erreur de déchiffrement.");
            }
        }).start();
    }
}
