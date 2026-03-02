package com.chatcyber;

import com.chatcyber.gui.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

/**
 * Point d'entrée principal de l'application client mail sécurisé.
 *
 * Lance l'interface graphique du client mail avec support du chiffrement
 * à base d'identité (IBE - Boneh-Franklin).
 *
 * Usage :
 *   java -jar chatcyber.jar             → Lance le client mail
 *   java -cp chatcyber.jar com.chatcyber.TrustAuthorityApp [port]
 *                                        → Lance le serveur de l'Autorité de Confiance
 */
public class App {

    public static void main(String[] args) {
        // Appliquer le Look and Feel FlatLaf (moderne)
        try {
            FlatLightLaf.setup();
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("TextComponent.arc", 5);
            UIManager.put("Button.arc", 5);
        } catch (Exception e) {
            // Fallback vers le Look and Feel par défaut du système
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // Lancer l'interface graphique sur le thread EDT
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
