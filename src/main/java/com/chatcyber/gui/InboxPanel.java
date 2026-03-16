package com.chatcyber.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.chatcyber.crypto.IBECipher;
import com.chatcyber.mail.EmailMessage;
import com.chatcyber.mail.MailReceiver;

/**
 * Panneau de la boite de reception avec telechargement et dechiffrement IBE.
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
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_MAIN);
        initComponents();
    }

    private void initComponents() {
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(UITheme.BG_MAIN);

        // ── Header ──
        JPanel headerCard = UITheme.headerPanel(
                "Boite de reception",
                "Consultez vos messages et dechiffrez les pieces jointes securisees."
        );
        content.add(headerCard, BorderLayout.NORTH);

        // ── Zone centrale ──
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 8));
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        // Barre d'outils
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        toolbar.setBackground(UITheme.BG_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        btnRefresh = UITheme.primaryButton("Actualiser");
        btnRefresh.addActionListener(e -> refreshInbox());
        toolbar.add(btnRefresh);

        toolbar.add(Box.createHorizontalStrut(10));
        JLabel lblCount = UITheme.formLabel("Messages :");
        toolbar.add(lblCount);
        spinnerCount = new JSpinner(new SpinnerNumberModel(20, 1, 200, 10));
        spinnerCount.setFont(UITheme.FONT_BODY);
        spinnerCount.setPreferredSize(new Dimension(70, 30));
        toolbar.add(spinnerCount);

        toolbar.add(Box.createHorizontalStrut(20));
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(180, 6));
        progressBar.setBorderPainted(false);
        toolbar.add(progressBar);

        centerWrapper.add(toolbar, BorderLayout.NORTH);

        // Split vertical : tableau en haut, apercu en bas
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(260);
        splitPane.setResizeWeight(0.45);
        splitPane.setBorder(null);
        splitPane.setDividerSize(6);

        // ── Tableau des messages ──
        String[] columns = {"De", "Objet", "Date", "PJ"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        messageTable = new JTable(tableModel);
        messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messageTable.setRowHeight(32);
        messageTable.setFont(UITheme.FONT_BODY);
        messageTable.setGridColor(UITheme.BORDER);
        messageTable.setShowHorizontalLines(true);
        messageTable.setShowVerticalLines(false);
        messageTable.setIntercellSpacing(new Dimension(0, 1));
        messageTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        messageTable.getColumnModel().getColumn(1).setPreferredWidth(350);
        messageTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        messageTable.getColumnModel().getColumn(3).setPreferredWidth(40);

        // Style de l'en-tete du tableau
        JTableHeader tableHeader = messageTable.getTableHeader();
        tableHeader.setFont(UITheme.FONT_LABEL);
        tableHeader.setBackground(UITheme.BG_SIDEBAR);
        tableHeader.setForeground(UITheme.TEXT_PRIMARY);
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UITheme.PRIMARY));
        tableHeader.setPreferredSize(new Dimension(0, 36));

        // Renderer pour les cellules
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_SIDEBAR);
                }
                return c;
            }
        };
        for (int i = 0; i < messageTable.getColumnCount(); i++) {
            messageTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        messageTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedMessage();
        });

        JScrollPane tableScroll = new JScrollPane(messageTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        tableScroll.getViewport().setBackground(UITheme.BG_CARD);
        splitPane.setTopComponent(tableScroll);

        // ── Apercu du message ──
        JPanel previewPanel = new JPanel(new BorderLayout(8, 0));
        previewPanel.setBackground(UITheme.BG_CARD);
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Corps du message
        taPreview = new JTextArea();
        taPreview.setEditable(false);
        taPreview.setLineWrap(true);
        taPreview.setWrapStyleWord(true);
        taPreview.setFont(UITheme.FONT_BODY);
        taPreview.setMargin(new Insets(12, 16, 12, 16));
        taPreview.setBackground(UITheme.BG_CARD);
        taPreview.setForeground(UITheme.TEXT_PRIMARY);

        JScrollPane previewScroll = new JScrollPane(taPreview);
        previewScroll.setBorder(null);
        previewPanel.add(previewScroll, BorderLayout.CENTER);

        // Panneau pieces jointes
        JPanel attachPanel = new JPanel(new BorderLayout(4, 8));
        attachPanel.setBackground(UITheme.BG_SIDEBAR);
        attachPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, UITheme.BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        attachPanel.setPreferredSize(new Dimension(260, 0));

        JLabel attachTitle = new JLabel("Pieces jointes");
        attachTitle.setFont(UITheme.FONT_HEADING);
        attachTitle.setForeground(UITheme.TEXT_PRIMARY);
        attachPanel.add(attachTitle, BorderLayout.NORTH);

        attachmentListModel = new DefaultListModel<>();
        attachmentList = new JList<>(attachmentListModel);
        attachmentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        attachmentList.setFont(UITheme.FONT_BODY);
        attachmentList.setFixedCellHeight(28);
        attachmentList.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JScrollPane attachScroll = new JScrollPane(attachmentList);
        attachScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        attachPanel.add(attachScroll, BorderLayout.CENTER);

        JPanel attachBtnPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        attachBtnPanel.setOpaque(false);
        attachBtnPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        btnDownload = UITheme.outlineButton("Telecharger");
        btnDownload.addActionListener(e -> downloadAttachment());

        btnDecrypt = UITheme.successButton("Dechiffrer + Sauver");
        btnDecrypt.addActionListener(e -> downloadAndDecrypt());

        attachBtnPanel.add(btnDownload);
        attachBtnPanel.add(btnDecrypt);
        attachPanel.add(attachBtnPanel, BorderLayout.SOUTH);

        previewPanel.add(attachPanel, BorderLayout.EAST);
        splitPane.setBottomComponent(previewPanel);

        centerWrapper.add(splitPane, BorderLayout.CENTER);
        content.add(centerWrapper, BorderLayout.CENTER);

        // ── Bas : dechiffrement local ──
        JPanel localBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        localBar.setBackground(UITheme.BG_CARD);
        localBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        localBar.add(Box.createHorizontalStrut(4));

        btnDecryptLocal = UITheme.outlineButton("Dechiffrer un fichier local (.ibe)");
        btnDecryptLocal.addActionListener(e -> decryptLocalFile());
        localBar.add(btnDecryptLocal);

        content.add(localBar, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
    }

    // ==================== ACTIONS ====================

    private void refreshInbox() {
        if (!mainFrame.getMailConfig().isEmailConfigured()) {
            mainFrame.showError("Erreur", "Veuillez configurer vos identifiants email.");
            return;
        }

        btnRefresh.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        mainFrame.updateStatus("Recuperation des messages...");

        int count = (int) spinnerCount.getValue();

        new Thread(() -> {
            try {
                if (mailReceiver != null) mailReceiver.disconnect();
                mailReceiver = new MailReceiver(mainFrame.getMailConfig());
                mailReceiver.connect();
                messages = mailReceiver.fetchMessages(count);

                SwingUtilities.invokeLater(() -> {
                    updateMessageTable();
                    mainFrame.updateStatus(messages.size() + " messages recuperes.");
                });
            } catch (Exception ex) {
                mainFrame.showError("Erreur IMAP",
                        "Impossible de recuperer les messages :\n" + ex.getMessage());
                mainFrame.updateStatus("Erreur de reception.");
            } finally {
                SwingUtilities.invokeLater(() -> {
                    btnRefresh.setEnabled(true);
                    progressBar.setVisible(false);
                });
            }
        }).start();
    }

    private void updateMessageTable() {
        tableModel.setRowCount(0);
        for (EmailMessage msg : messages) {
            String date = msg.getSentDate() != null ? DATE_FORMAT.format(msg.getSentDate()) : "";
            String hasAttach = msg.hasAttachments()
                    ? (msg.hasEncryptedAttachments() ? "[IBE]" : "[PJ]") : "";
            tableModel.addRow(new Object[]{msg.getFrom(), msg.getSubject(), date, hasAttach});
        }
    }

    private void showSelectedMessage() {
        int row = messageTable.getSelectedRow();
        if (row < 0 || row >= messages.size()) {
            taPreview.setText("");
            attachmentListModel.clear();
            return;
        }

        EmailMessage msg = messages.get(row);

        StringBuilder sb = new StringBuilder();
        sb.append("De :     ").append(msg.getFrom()).append("\n");
        sb.append("Objet :  ").append(msg.getSubject()).append("\n");
        if (msg.getSentDate() != null) {
            sb.append("Date :   ").append(DATE_FORMAT.format(msg.getSentDate())).append("\n");
        }
        sb.append("\n").append("-".repeat(60)).append("\n\n");
        sb.append(msg.getBodyText() != null ? msg.getBodyText() : "(pas de contenu)");
        taPreview.setText(sb.toString());
        taPreview.setCaretPosition(0);

        attachmentListModel.clear();
        if (msg.hasAttachments()) {
            for (EmailMessage.AttachmentInfo att : msg.getAttachments()) {
                attachmentListModel.addElement(att.toString());
            }
        }
    }

    private void downloadAttachment() {
        int msgRow = messageTable.getSelectedRow();
        int attachIdx = attachmentList.getSelectedIndex();
        if (msgRow < 0 || attachIdx < 0) {
            mainFrame.showError("Selection", "Selectionez un message et une piece jointe.");
            return;
        }

        EmailMessage msg = messages.get(msgRow);
        EmailMessage.AttachmentInfo attachInfo = msg.getAttachments().get(attachIdx);

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Enregistrer la piece jointe");
        chooser.setSelectedFile(new File(attachInfo.getFileName()));
        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File destFile = chooser.getSelectedFile();
            mainFrame.updateStatus("Telechargement de " + attachInfo.getFileName() + "...");

            new Thread(() -> {
                try {
                    File downloaded = mailReceiver.downloadAttachment(
                            msg.getMessageNumber(), attachInfo.getPartIndex(), destFile.getParentFile());
                    if (!downloaded.getName().equals(destFile.getName())) {
                        downloaded.renameTo(destFile);
                    }
                    mainFrame.showInfo("Telechargement",
                            "Piece jointe sauvegardee :\n" + destFile.getAbsolutePath());
                    mainFrame.updateStatus("Piece jointe telechargee.");
                } catch (Exception ex) {
                    mainFrame.showError("Erreur", "Erreur de telechargement :\n" + ex.getMessage());
                }
            }).start();
        }
    }

    private void downloadAndDecrypt() {
        int msgRow = messageTable.getSelectedRow();
        int attachIdx = attachmentList.getSelectedIndex();
        if (msgRow < 0 || attachIdx < 0) {
            mainFrame.showError("Selection", "Selectionez un message et une piece jointe.");
            return;
        }

        if (!mainFrame.isIBEReady()) {
            mainFrame.showError("Erreur IBE",
                    "Le systeme IBE n'est pas pret.\nVerifiez parametres et cle privee.");
            return;
        }

        EmailMessage msg = messages.get(msgRow);
        EmailMessage.AttachmentInfo attachInfo = msg.getAttachments().get(attachIdx);

        if (!attachInfo.isEncrypted()) {
            mainFrame.showError("Erreur", "Ce fichier n'est pas chiffre (extension .ibe attendue).");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Enregistrer le fichier dechiffre");
        String originalName = normalizeLegacyEncryptedName(attachInfo.getFileName().replaceAll("(?i)\\.ibe$", ""));
        chooser.setSelectedFile(new File(originalName));
        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File destFile = chooser.getSelectedFile();
            mainFrame.updateStatus("Telechargement et dechiffrement...");

            new Thread(() -> {
                try {
                    File tempDir = new File(System.getProperty("java.io.tmpdir"), "chatcyber_temp");
                    tempDir.mkdirs();

                    File encryptedFile = mailReceiver.downloadAttachment(
                            msg.getMessageNumber(), attachInfo.getPartIndex(), tempDir);

                    mainFrame.updateStatus("Dechiffrement IBE en cours...");
                    IBECipher cipher = new IBECipher(mainFrame.getSystemParams());
                    File decryptedFile = cipher.decryptFile(
                            encryptedFile, destFile.getParentFile(), mainFrame.getPrivateKey());

                    if (!decryptedFile.getAbsolutePath().equals(destFile.getAbsolutePath())) {
                        if (destFile.exists()) destFile.delete();
                        decryptedFile.renameTo(destFile);
                    }

                    encryptedFile.delete();

                    mainFrame.showInfo("Dechiffrement",
                            "Fichier dechiffre avec succes :\n" + destFile.getAbsolutePath());
                    mainFrame.updateStatus("Fichier dechiffre.");

                } catch (Exception ex) {
                    mainFrame.showError("Erreur de dechiffrement",
                            "Impossible de dechiffrer :\n" + ex.getMessage() +
                                    "\n\nVerifiez que votre cle privee correspond a l'identite utilisee.");
                    mainFrame.updateStatus("Erreur de dechiffrement.");
                }
            }).start();
        }
    }

    private void decryptLocalFile() {
        if (!mainFrame.isIBEReady()) {
            mainFrame.showError("Erreur IBE",
                    "Le systeme IBE n'est pas pret.\nVerifiez parametres et cle privee.");
            return;
        }

        JFileChooser openChooser = new JFileChooser();
        openChooser.setDialogTitle("Selectionner un fichier chiffre (.ibe)");
        openChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Fichiers chiffres IBE (*.ibe)", "ibe"));
        int openResult = openChooser.showOpenDialog(this);
        if (openResult != JFileChooser.APPROVE_OPTION) return;

        File encryptedFile = openChooser.getSelectedFile();

        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Enregistrer le fichier dechiffre");
        String suggestedName = normalizeLegacyEncryptedName(encryptedFile.getName().replaceAll("(?i)\\.ibe$", ""));
        saveChooser.setSelectedFile(new File(encryptedFile.getParent(), suggestedName));
        int saveResult = saveChooser.showSaveDialog(this);
        if (saveResult != JFileChooser.APPROVE_OPTION) return;

        File outputFile = saveChooser.getSelectedFile();
        mainFrame.updateStatus("Dechiffrement du fichier local...");

        new Thread(() -> {
            try {
                IBECipher cipher = new IBECipher(mainFrame.getSystemParams());
                File decrypted = cipher.decryptFile(
                        encryptedFile, outputFile.getParentFile(), mainFrame.getPrivateKey());

                if (!decrypted.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                    if (outputFile.exists()) outputFile.delete();
                    decrypted.renameTo(outputFile);
                }

                mainFrame.showInfo("Dechiffrement",
                        "Fichier dechiffre avec succes :\n" + outputFile.getAbsolutePath());
                mainFrame.updateStatus("Fichier dechiffre.");

            } catch (Exception ex) {
                mainFrame.showError("Erreur de dechiffrement",
                        "Impossible de dechiffrer :\n" + ex.getMessage());
                mainFrame.updateStatus("\u2717 Erreur de dechiffrement.");
            }
        }).start();
    }

    /**
     * Nettoie les anciens suffixes techniques de nommage (ex: fichier.ext_1712345678901_0)
     * tout en conservant les noms standards non concernés.
     */
    private String normalizeLegacyEncryptedName(String name) {
        if (name == null) {
            return "fichier_dechiffre";
        }
        return name
                .replaceFirst("_(\\d{10,})_(\\d+)( \\\\(\\d+\\\\))?$", "")
                .replaceFirst("_(\\d{10,})$", "");
    }
}
