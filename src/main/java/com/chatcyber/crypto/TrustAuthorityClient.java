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
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            // Envoyer la commande
            dos.writeUTF("GET_PARAMS");
            dos.flush();

            int length = dis.readInt();
            if (length < 0) {
                throw new IOException("Le serveur a retourné une erreur");
            }

            byte[] data = new byte[length];
            dis.readFully(data);

            //Deserialiser les params
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

            //Clé IBE privée chiffrée
            int length = dis.readInt();
            if (length < 0) {
                throw new IOException("Le serveur a retourné une erreur pour l'extraction de clé");
            }
            byte[] encryptedIbeKey = new byte[length];
            dis.readFully(encryptedIbeKey);

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
