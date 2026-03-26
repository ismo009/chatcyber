package com.chatcyber.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SystemParameters implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String pairingParameters;
    private final byte[] generatorP;
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
