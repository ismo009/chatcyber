package com.chatcyber.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Thème unifié pour l'application ChatCyber.
 * Définit les couleurs, polices, bordures et composants réutilisables.
 */
public final class UITheme {

    private UITheme() {}

    // ── Couleurs principales ──
    public static final Color PRIMARY       = new Color(37, 99, 235);    // Bleu professionnel
    public static final Color PRIMARY_DARK  = new Color(29, 78, 216);
    public static final Color PRIMARY_LIGHT = new Color(219, 234, 254);
    public static final Color ACCENT        = new Color(16, 185, 129);   // Vert succès
    public static final Color ACCENT_DARK   = new Color(5, 150, 105);
    public static final Color DANGER        = new Color(239, 68, 68);    // Rouge erreur
    public static final Color WARNING       = new Color(245, 158, 11);   // Orange avertissement

    // ── Neutres ──
    public static final Color BG_MAIN      = new Color(249, 250, 251);  // Fond principal
    public static final Color BG_CARD      = Color.WHITE;                // Fond des cartes
    public static final Color BG_SIDEBAR   = new Color(243, 244, 246);
    public static final Color BORDER       = new Color(229, 231, 235);
    public static final Color TEXT_PRIMARY  = new Color(17, 24, 39);
    public static final Color TEXT_SECONDARY= new Color(107, 114, 128);
    public static final Color TEXT_MUTED    = new Color(156, 163, 175);

    // ── Polices ──
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO    = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_BUTTON  = new Font("Segoe UI", Font.BOLD, 13);

    // ── Dimensions ──
    public static final int PADDING = 16;
    public static final int CARD_RADIUS = 12;
    public static final Insets CARD_INSETS = new Insets(16, 20, 16, 20);

    // ── Bordures ──
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(CARD_INSETS.top, CARD_INSETS.left, CARD_INSETS.bottom, CARD_INSETS.right)
        );
    }

    public static Border cardBorderWithTitle(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDER, 1),
                        "  " + title + "  ",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        FONT_HEADING,
                        TEXT_PRIMARY
                ),
                BorderFactory.createEmptyBorder(8, 12, 12, 12)
        );
    }

    // ── Composants personnalisés ──

    /**
     * Crée un bouton primaire (bleu).
     */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(PRIMARY_DARK);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(PRIMARY);
            }
        });
        return btn;
    }

    /**
     * Crée un bouton succès (vert).
     */
    public static JButton successButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(ACCENT_DARK);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(ACCENT);
            }
        });
        return btn;
    }

    /**
     * Crée un bouton danger (rouge).
     */
    public static JButton dangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(DANGER);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        return btn;
    }

    /**
     * Crée un bouton secondaire (outline).
     */
    public static JButton outlineButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(BG_CARD);
        btn.setForeground(PRIMARY);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1),
                BorderFactory.createEmptyBorder(7, 18, 7, 18)
        ));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(PRIMARY_LIGHT);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_CARD);
            }
        });
        return btn;
    }

    /**
     * Crée un label de section (titre de carte).
     */
    public static JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_HEADING);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    /**
     * Crée un label de description.
     */
    public static JLabel descriptionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    /**
     * Crée un champ texte stylisé.
     */
    public static JTextField styledTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(FONT_BODY);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    /**
     * Crée un champ mot de passe stylisé.
     */
    public static JPasswordField styledPasswordField(int columns) {
        JPasswordField pf = new JPasswordField(columns);
        pf.setFont(FONT_BODY);
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return pf;
    }

    /**
     * Crée un label de formulaire.
     */
    public static JLabel formLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    /**
     * Crée un badge d'état.
     */
    public static JLabel statusBadge(String text, boolean ok) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SMALL);
        lbl.setOpaque(true);
        if (ok) {
            lbl.setBackground(new Color(209, 250, 229));
            lbl.setForeground(new Color(6, 95, 70));
        } else {
            lbl.setBackground(new Color(254, 226, 226));
            lbl.setForeground(new Color(153, 27, 27));
        }
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return lbl;
    }

    /**
     * Crée un panneau de carte avec fond blanc et bordure.
     */
    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(cardBorder());
        return panel;
    }

    /**
     * Crée un panneau de carte avec titre.
     */
    public static JPanel card(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(cardBorderWithTitle(title));
        return panel;
    }

    /**
     * Crée un séparateur horizontal.
     */
    public static JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        return sep;
    }

    /**
     * Crée un panneau d'en-tête avec titre et sous-titre.
     */
    public static JPanel headerPanel(String title, String subtitle) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            panel.add(Box.createVerticalStrut(4));
            JLabel subLabel = new JLabel(subtitle);
            subLabel.setFont(FONT_BODY);
            subLabel.setForeground(TEXT_SECONDARY);
            subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(subLabel);
        }

        return panel;
    }

    /**
     * Configure le UIManager pour le thème global FlatLaf.
     */
    public static void applyGlobalTheme() {
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("TabbedPane.selectedBackground", BG_CARD);
        UIManager.put("TabbedPane.underlineColor", PRIMARY);
        UIManager.put("TabbedPane.hoverColor", PRIMARY_LIGHT);
        UIManager.put("Table.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ProgressBar.foreground", PRIMARY);
    }
}
