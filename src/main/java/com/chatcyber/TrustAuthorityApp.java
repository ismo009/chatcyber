package com.chatcyber;

import com.chatcyber.gui.TrustAuthorityFrame;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

/**
 * Point d'entrée pour le serveur de l'Autorité de Confiance (Trust Authority).
 *
 * Lance une fenêtre dédiée à la gestion du serveur IBE qui :
 *   - Génère les paramètres du système (Setup de Boneh-Franklin)
 *   - Écoute les requêtes des clients mail
 *   - Génère les clés privées des utilisateurs (Extract)
 *
 * Usage :
 *   java -cp chatcyber.jar com.chatcyber.TrustAuthorityApp [port]
 *
 * @param port Port d'écoute du serveur (par défaut : 7777)
 */
public class TrustAuthorityApp {

    public static void main(String[] args) {
        // Port par défaut ou spécifié en argument
        int port = 7777;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port invalide : " + args[0] + ". Utilisation du port par défaut : 7777");
            }
        }

        final int serverPort = port;

        // Look and Feel sombre pour distinguer visuellement l'AC du client
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // Lancer l'interface de l'AC
        SwingUtilities.invokeLater(() -> {
            TrustAuthorityFrame frame = new TrustAuthorityFrame(serverPort);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
