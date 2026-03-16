package com.chatcyber;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.chatcyber.gui.TrustAuthorityFrame;
import com.formdev.flatlaf.FlatDarkLaf;


public class TrustAuthorityApp {
    public static void main(String[] args) {
        int port = 7777;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port invalide, utilisation du port par defaut 7777.");
            }
        }

        final int serverPort = port;

        SwingUtilities.invokeLater(() -> {
            try {
                FlatDarkLaf.setup();
                UIManager.put("Component.focusWidth", 1);
                UIManager.put("Button.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("ScrollBar.trackArc", 999);
                UIManager.put("ScrollBar.thumbArc", 999);
                UIManager.put("ScrollBar.width", 10);
            } catch (Exception e) {
                System.err.println("Impossible de charger le theme FlatDarkLaf : " + e.getMessage());
            }

            TrustAuthorityFrame frame = new TrustAuthorityFrame(serverPort);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
