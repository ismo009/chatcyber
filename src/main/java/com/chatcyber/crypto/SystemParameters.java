package com.chatcyber.crypto;

import java.io.*;

/**
 * Paramètres publics du système IBE (Identity-Based Encryption).
 * Générés par l'Autorité de Confiance et partagés avec tous les clients.
 *
 * Contient :
 * - Les paramètres de la courbe elliptique (Type A)
 * - Le générateur P ∈ G1
 * - La clé publique maîtresse Ppub = s·P ∈ G1
 */
public class SystemParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Paramètres de la courbe bilinéaire au format properties (Type A) */
    private final String pairingParameters;

    /** Générateur P ∈ G1 sérialisé */
    private final byte[] generatorP;

    /** Clé publique maîtresse Ppub = s·P ∈ G1 sérialisée */
    private final byte[] publicKeyPpub;

    public SystemParameters(String pairingParameters, byte[] generatorP, byte[] publicKeyPpub) {
        this.pairingParameters = pairingParameters;
        this.generatorP = generatorP.clone();
        this.publicKeyPpub = publicKeyPpub.clone();
    }

    public String getPairingParameters() {
        return pairingParameters;
    }

    public byte[] getGeneratorP() {
        return generatorP.clone();
    }

    public byte[] getPublicKeyPpub() {
        return publicKeyPpub.clone();
    }

    /**
     * Sauvegarde les paramètres dans un fichier.
     */
    public void saveToFile(String path) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(this);
        }
    }

    /**
     * Charge les paramètres depuis un fichier.
     */
    public static SystemParameters loadFromFile(String path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return (SystemParameters) ois.readObject();
        }
    }

    @Override
    public String toString() {
        return "SystemParameters{generator=" + generatorP.length + " bytes, "
                + "publicKey=" + publicKeyPpub.length + " bytes}";
    }
}
