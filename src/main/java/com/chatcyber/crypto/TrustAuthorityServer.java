package com.chatcyber.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;

public class TrustAuthorityServer {

    private final TrustAuthority trustAuthority;
    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running;
    private MessageListener listener;

    /**Interface pour recevoir les messages de log du serveur*/
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
        log("Initialisation du système IBE (Setup Boneh-Franklin)");
        trustAuthority.setup();
        log("Système IBE initialisé avec succès");

        serverSocket = new ServerSocket(port);
        running = true;
        log("Serveur démarré sur le port " + port + " — En attente de clienrs");

        //Thread principale d'écoute
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
                    handleExtractKey(dis, dos, identity);
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

    //Envoie les paramètres publics du système au client ; C'est des parametres publics, pas besoin de protéger
    private void handleGetParams(DataOutputStream dos) throws IOException {
        SystemParameters params = trustAuthority.getPublicParameters();

        //Sérialiser l'objet SystemParameters
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

    //Extrait clée privée IBE, la chiffre avec RSA2048 et l'envoie
    private void handleExtractKey(DataInputStream dis, DataOutputStream dos, String identity)
            throws Exception {
        //Lecture clée publique RSA Client
        int rsaPubLen = dis.readInt();
        if (rsaPubLen <= 0 || rsaPubLen > 8192) {
            dos.writeInt(-1);
            dos.flush();
            log("Clé publique RSA invalide pour : " + identity);
            return;
        }
        byte[] rsaPubBytes = new byte[rsaPubLen];
        dis.readFully(rsaPubBytes);

        PublicKey rsaPublicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(rsaPubBytes));

        //Extraction private KEY IBE
        byte[] ibePrivateKey = trustAuthority.extractPrivateKey(identity);

        //Chiffrement clée IBE via RSA
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedKey = rsaCipher.doFinal(ibePrivateKey);

        dos.writeInt(encryptedKey.length);
        dos.write(encryptedKey);
        dos.flush();

        log("Clé privée IBE chiffrée (RSA-OAEP) envoyée pour : " + identity + " (" + encryptedKey.length + " octets).");
    }

    private String parseCommand(String command) {
        if (command == null) return "";
        if (command.equals("GET_PARAMS")) return "GET_PARAMS";
        if (command.startsWith("EXTRACT_KEY ")) return "EXTRACT_KEY";
        return command;
    }

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
