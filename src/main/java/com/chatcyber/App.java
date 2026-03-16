package com.chatcyber;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.chatcyber.gui.MainFrame;
import com.chatcyber.gui.UITheme;
import com.formdev.flatlaf.FlatLightLaf;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("Component.focusWidth", 1);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arrowType", "chevron");
                UIManager.put("ScrollBar.trackArc", 999);
                UIManager.put("ScrollBar.thumbArc", 999);
                UIManager.put("ScrollBar.width", 10);
                UITheme.applyGlobalTheme();
            } catch (Exception e) {
                System.err.println("Impossible de charger le theme FlatLaf : " + e.getMessage());
            }

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
