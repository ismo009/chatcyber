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
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;

import com.chatcyber.DebugFlags;

public class TrustAuthorityServer {

    private final TrustAuthority trustAuthority;
    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    /** Flag de debug : expose la clé privée IBE en clair (voir DebugFlags). */
    private static final boolean DEBUG_EXPOSE_IBE_PRIVATE_KEY = DebugFlags.EXPOSE_IBE_PRIVATE_KEY;
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

    public void setIbePrivateKeyListener(IbePrivateKeyListener listener) {
        this.ibePrivateKeyListener = listener;
    }

    /** Interface pour notifier l'extraction d'une clé privée IBE (en clair) */
    public interface IbePrivateKeyListener {
        void onIbePrivateKeyExtracted(String identity, byte[] ibePrivateKey);
    }

    private IbePrivateKeyListener ibePrivateKeyListener;

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
                case "GET_ENCRYPTION_INFO":
                    String recipient = command.substring("GET_ENCRYPTION_INFO ".length()).trim();
                    handleGetEncryptionInfo(dos, recipient);
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

    //Même chose que GET_PARAMS, mais on inclut l'identité dans la requête pour avoir un log explicite.
    private void handleGetEncryptionInfo(DataOutputStream dos, String recipientEmail) throws IOException {
        String normalized = recipientEmail == null ? "" : recipientEmail.toLowerCase().trim();
        log("Infos publiques demandées pour envoyer un mail à : " + normalized);

        SystemParameters params = trustAuthority.getPublicParameters();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(params);
        oos.flush();
        byte[] data = baos.toByteArray();

        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();

        log("Infos publiques envoyées pour : " + normalized + " (" + data.length + " octets).");
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

        if (DEBUG_EXPOSE_IBE_PRIVATE_KEY) {
            String b64 = Base64.getEncoder().encodeToString(ibePrivateKey);
            log("[DEBUG] Clé privée IBE (en clair, Base64) pour \"" + identity + "\" : " + b64);
            if (ibePrivateKeyListener != null) {
                ibePrivateKeyListener.onIbePrivateKeyExtracted(identity, ibePrivateKey);
            }
        }

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
        if (command.startsWith("GET_ENCRYPTION_INFO ")) return "GET_ENCRYPTION_INFO";
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
