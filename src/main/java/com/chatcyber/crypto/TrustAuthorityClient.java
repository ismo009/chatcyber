package com.chatcyber.crypto;

import java.io.*;
import java.net.Socket;

/**
 * Client pour communiquer avec le serveur de l'Autorité de Confiance.
 *
 * Permet aux clients mail de :
 *   1. Récupérer les paramètres publics du système IBE (GET_PARAMS)
 *   2. Demander l'extraction de leur clé privée (EXTRACT_KEY email)
 */
public class TrustAuthorityClient {

    private final String host;
    private final int port;

    /**
     * @param host Adresse du serveur de l'AC (ex: "localhost", "192.168.1.10")
     * @param port Port du serveur de l'AC (ex: 7777)
     */
    public TrustAuthorityClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Récupère les paramètres publics du système IBE depuis l'Autorité de Confiance.
     *
     * @return Les paramètres publics (pairingParams, P, Ppub)
     * @throws IOException          En cas d'erreur réseau
     * @throws ClassNotFoundException Si la désérialisation échoue
     */
    public SystemParameters getParameters() throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            // Envoyer la commande
            dos.writeUTF("GET_PARAMS");
            dos.flush();

            // Recevoir la réponse
            int length = dis.readInt();
            if (length < 0) {
                throw new IOException("Le serveur a retourné une erreur");
            }

            byte[] data = new byte[length];
            dis.readFully(data);

            // Désérialiser les paramètres
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (SystemParameters) ois.readObject();
        }
    }

    /**
     * Demande l'extraction de la clé privée pour une identité (adresse email).
     *
     * @param identity L'adresse email pour laquelle extraire la clé privée
     * @return La clé privée sérialisée (bytes de l'élément dID ∈ G1)
     * @throws IOException En cas d'erreur réseau
     */
    public byte[] extractKey(String identity) throws IOException {
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            // Envoyer la commande avec l'identité
            dos.writeUTF("EXTRACT_KEY " + identity.toLowerCase().trim());
            dos.flush();

            // Recevoir la clé privée
            int length = dis.readInt();
            if (length < 0) {
                throw new IOException("Le serveur a retourné une erreur pour l'extraction de clé");
            }

            byte[] privateKey = new byte[length];
            dis.readFully(privateKey);

            return privateKey;
        }
    }

    /**
     * Teste la connectivité avec le serveur de l'AC.
     *
     * @return true si le serveur est joignable
     */
    public boolean testConnection() {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
}
