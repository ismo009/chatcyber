package com.chatcyber.crypto;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Serveur réseau de l'Autorité de Confiance (Trust Authority).
 *
 * Écoute les connexions TCP des clients mail et traite les requêtes :
 *   - GET_PARAMS     → Retourne les paramètres publics du système IBE
 *   - EXTRACT_KEY id → Retourne la clé privée pour l'identité (email) spécifiée
 *
 * Protocole :
 *   Client → Serveur : commande (UTF string via DataOutputStream)
 *   Serveur → Client : longueur des données (int) + données (bytes)
 */
public class TrustAuthorityServer {

    private final TrustAuthority trustAuthority;
    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running;
    private MessageListener listener;

    /** Interface pour recevoir les messages de log du serveur */
    public interface MessageListener {
        void onMessage(String message);
    }

    public TrustAuthorityServer(int port) {
        this.trustAuthority = new TrustAuthority();
        this.port = port;
        this.executor = Executors.newCachedThreadPool();
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    private void log(String message) {
        String formatted = "[AC Serveur] " + message;
        System.out.println(formatted);
        if (listener != null) {
            listener.onMessage(formatted);
        }
    }

    /**
     * Initialise le système IBE et démarre le serveur.
     */
    public void start() throws IOException {
        log("Initialisation du système IBE (Setup de Boneh-Franklin)...");
        trustAuthority.setup();
        log("Système IBE initialisé avec succès.");

        serverSocket = new ServerSocket(port);
        running = true;
        log("Serveur démarré sur le port " + port + " — En attente de connexions...");

        // Thread principale d'écoute
        executor.submit(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientAddr = clientSocket.getInetAddress().getHostAddress();
                    log("Connexion entrante de " + clientAddr);
                    executor.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        log("Erreur d'acceptation de connexion : " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Traite une requête d'un client.
     */
    private void handleClient(Socket socket) {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            String command = dis.readUTF();
            log("Commande reçue : " + command);

            switch (parseCommand(command)) {
                case "GET_PARAMS":
                    handleGetParams(dos);
                    break;
                case "EXTRACT_KEY":
                    String identity = command.substring("EXTRACT_KEY ".length()).trim();
                    handleExtractKey(dos, identity);
                    break;
                default:
                    log("Commande inconnue : " + command);
                    dos.writeInt(-1);
                    dos.flush();
            }

        } catch (Exception e) {
            log("Erreur lors du traitement d'un client : " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Envoie les paramètres publics du système au client.
     */
    private void handleGetParams(DataOutputStream dos) throws IOException {
        SystemParameters params = trustAuthority.getPublicParameters();

        // Sérialiser l'objet SystemParameters
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(params);
        oos.flush();
        byte[] data = baos.toByteArray();

        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();

        log("Paramètres publics envoyés (" + data.length + " octets).");
    }

    /**
     * Extrait et envoie la clé privée pour l'identité demandée.
     */
    private void handleExtractKey(DataOutputStream dos, String identity) throws IOException {
        byte[] privateKey = trustAuthority.extractPrivateKey(identity);

        dos.writeInt(privateKey.length);
        dos.write(privateKey);
        dos.flush();

        log("Clé privée envoyée pour : " + identity + " (" + privateKey.length + " octets).");
    }

    /**
     * Parse le type de commande.
     */
    private String parseCommand(String command) {
        if (command == null) return "";
        if (command.equals("GET_PARAMS")) return "GET_PARAMS";
        if (command.startsWith("EXTRACT_KEY ")) return "EXTRACT_KEY";
        return command;
    }

    /**
     * Arrête le serveur.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
        log("Serveur arrêté.");
    }

    public boolean isRunning() {
        return running;
    }

    public TrustAuthority getTrustAuthority() {
        return trustAuthority;
    }

    public int getPort() {
        return port;
    }
}
