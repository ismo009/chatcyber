package com.chatcyber.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;

import javax.crypto.Cipher;
import javax.swing.JOptionPane;

public class TrustAuthorityClient {

    private final String host;
    private final int port;

    public TrustAuthorityClient(String host, int port) {
        this.host = host; 
        this.port = port;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }

    //Obtenor paametres publics
    public SystemParameters getParameters() throws IOException, ClassNotFoundException {
        return requestSystemParameters("GET_PARAMS");
    }

    /**
     * Récupère les informations publiques nécessaires pour chiffrer un mail vers une identité.
     * (Même contenu que GET_PARAMS, mais le serveur logge explicitement le destinataire.)
     */
    public SystemParameters getEncryptionInfo(String recipientEmail) throws IOException, ClassNotFoundException {
        String normalized = recipientEmail == null ? "" : recipientEmail.toLowerCase().trim();
        return requestSystemParameters("GET_ENCRYPTION_INFO " + normalized);
    }

    private SystemParameters requestSystemParameters(String command) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            dos.writeUTF(command);
            dos.flush();

            int length = dis.readInt();
            if (length < 0) {
                throw new IOException("Le serveur a retourné une erreur");
            }

            byte[] data = new byte[length];
            dis.readFully(data);

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (SystemParameters) ois.readObject();
        }
    }

    public byte[] extractKey(String identity) throws Exception {
        //Génération clée RSA éphémère
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair ephemeralKeyPair = kpg.generateKeyPair();
        PrivateKey rsaPrivateKey = ephemeralKeyPair.getPrivate();
        byte[] rsaPublicKeyBytes = ephemeralKeyPair.getPublic().getEncoded();

        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            //En premier commande puis la clée pi=ublique
            dos.writeUTF("EXTRACT_KEY " + identity.toLowerCase().trim());
            dos.writeInt(rsaPublicKeyBytes.length);
            dos.write(rsaPublicKeyBytes);
            dos.flush();

            //Lire la réponse du serveur
            int statusCode = dis.readInt();
            
            // Code 1 = vérification requise
            if (statusCode == 1) {
                return handleVerificationChallenge(identity, rsaPrivateKey, rsaPublicKeyBytes);
            }
            
            // Code -2 ou autre erreur
            if (statusCode <= 0) {
                throw new IOException("Le serveur a retourné une erreur pour l'extraction de clé (code: " + statusCode + ")");
            }
            
            // Code > 1 = l'ancienne réponse avec la clé chiffrée (pour rétrocompatibilité)
            byte[] encryptedIbeKey = new byte[statusCode];
            dis.readFully(encryptedIbeKey);

            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
            return rsaCipher.doFinal(encryptedIbeKey);
        }
    }
    
    /**
     * Gère le défi de vérification par code
     */
    private byte[] handleVerificationChallenge(String identity, PrivateKey rsaPrivateKey, byte[] rsaPublicKeyBytes)
            throws Exception {
        
        // Demander le code de vérification à l'utilisateur (via interface graphique)
        String verificationCode = JOptionPane.showInputDialog(
            null,
            "Un code de vérification à 6 chiffres a été envoyé à " + identity + ".\n" +
            "Veuillez entrer le code :",
            "Vérification d'email",
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            throw new IOException("Vérification annulée par l'utilisateur");
        }
        
        verificationCode = verificationCode.trim();
        
        // Valider que le code est bien 6 chiffres
        if (!verificationCode.matches("\\d{6}")) {
            throw new IOException("Le code doit être composé de 6 chiffres");
        }
        
        // Envoyer le code de vérification au serveur
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            
            String command = "VERIFY_KEY " + identity.toLowerCase().trim() + " " + verificationCode;
            dos.writeUTF(command);
            dos.writeInt(rsaPublicKeyBytes.length);
            dos.write(rsaPublicKeyBytes);
            dos.flush();
            
            // Lire la réponse
            int length = dis.readInt();
            if (length < 0) {
                throw new IOException("Le serveur a rejeté le code de vérification");
            }
            
            byte[] encryptedIbeKey = new byte[length];
            dis.readFully(encryptedIbeKey);
            
            // Déchiffrer avec la clé privée RSA
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
            return rsaCipher.doFinal(encryptedIbeKey);
        }
    }

    public boolean testConnection() {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }


}
