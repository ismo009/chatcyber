package com.chatcyber.crypto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class IBECipher {
    private static final int SIGMA_LENGTH = 32;

    private final Pairing pairing;
    private final Element generatorP;
    private final Element publicKeyPpub;


    public IBECipher(SystemParameters params) {
        this.pairing = loadPairingFromString(params.getPairingParameters());

        this.generatorP = pairing.getG1().newElementFromBytes(params.getGeneratorP()).getImmutable();
        this.publicKeyPpub = pairing.getG1().newElementFromBytes(params.getPublicKeyPpub()).getImmutable();
    }


    public void encryptFile(File inputFile, File outputFile, String recipientEmail) throws Exception {
        byte[] plaintext = Files.readAllBytes(inputFile.toPath());
        byte[] ciphertext = encrypt(plaintext, recipientEmail);

        // Écrire le nom original du fichier + les données chiffrées
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputFile))) {
            // Stocker le nom du fichier original pour pouvoir le récupérer au déchiffrement
            dos.writeUTF(inputFile.getName());
            dos.write(ciphertext);
        }
    }

    public File decryptFile(File inputFile, File outputDir, byte[] privateKeyBytes) throws Exception {
        String originalName;
        byte[] ciphertext;

        try (DataInputStream dis = new DataInputStream(new FileInputStream(inputFile))) {
            originalName = dis.readUTF();
            ciphertext = dis.readAllBytes();
        }

        byte[] plaintext = decrypt(ciphertext, privateKeyBytes);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File outputFile = new File(outputDir, originalName);
        Files.write(outputFile.toPath(), plaintext);

        return outputFile;
    }

    public byte[] encrypt(byte[] data, String recipientIdentity) throws Exception {
        byte[] idHash = TrustAuthority.hashIdentity(recipientIdentity);
        Element qID = pairing.getG1().newElementFromHash(idHash, 0, idHash.length).getImmutable();

        byte[] sigma = new byte[SIGMA_LENGTH];
        new SecureRandom().nextBytes(sigma);

        Element r = h3(sigma, data);
        Element U = generatorP.mulZn(r).getImmutable();
        Element theta = pairing.pairing(qID, publicKeyPpub).powZn(r).getImmutable();

        byte[] h2bytes = h2(theta);
        byte[] V = new byte[SIGMA_LENGTH];
        for (int i = 0; i < SIGMA_LENGTH; i++) {
            V[i] = (byte) (sigma[i] ^ h2bytes[i]);
        }

        byte[] h4bytes = h4(sigma, data.length);
        byte[] W = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            W[i] = (byte) (data[i] ^ h4bytes[i]);
        }

        byte[] uBytes = U.toBytes();
        ByteBuffer buffer = ByteBuffer.allocate(4 + uBytes.length + SIGMA_LENGTH + W.length);
        buffer.putInt(uBytes.length);
        buffer.put(uBytes);
        buffer.put(V);
        buffer.put(W);
        return buffer.array();
    }


    //Données chiffrées au format [len(U)][U][V][W]
    public byte[] decrypt(byte[] ciphertext, byte[] privateKeyBytes) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(ciphertext);

        // Extraire U ∈ G1
        int uLength = buffer.getInt();
        byte[] uBytes = new byte[uLength];
        buffer.get(uBytes);
        Element U = pairing.getG1().newElementFromBytes(uBytes).getImmutable();

        // Extraire V (32 octets : σ ⊕ H2(θ))
        byte[] V = new byte[SIGMA_LENGTH];
        buffer.get(V);

        // Extraire W (octets restants : M ⊕ H4(σ))
        byte[] W = new byte[buffer.remaining()];
        buffer.get(W);

        // dID ∈ G1 (clé privée de l'utilisateur)
        Element dID = pairing.getG1().newElementFromBytes(privateKeyBytes).getImmutable();

        // θ' = e(dID, U) = e(s·QID, r·P) = e(QID, Ppub)^r
        Element theta = pairing.pairing(dID, U).getImmutable();

        // σ' = V ⊕ H2(θ')
        byte[] h2bytes = h2(theta);
        byte[] sigma = new byte[SIGMA_LENGTH];
        for (int i = 0; i < SIGMA_LENGTH; i++) {
            sigma[i] = (byte) (V[i] ^ h2bytes[i]);
        }

        // M' = W ⊕ H4(σ', |W|)
        byte[] h4bytes = h4(sigma, W.length);
        byte[] M = new byte[W.length];
        for (int i = 0; i < W.length; i++) {
            M[i] = (byte) (W[i] ^ h4bytes[i]);
        }

        // Vérification d'intégrité (FullIdent) : r' = H3(σ', M'), U doit valoir r'·P
        Element rPrime = h3(sigma, M);
        Element UPrime = generatorP.mulZn(rPrime).getImmutable();
        if (!Arrays.equals(U.toBytes(), UPrime.toBytes())) {
            throw new Exception("Échec de la vérification FullIdent : "
                    + "chiffré invalide ou clé privée incorrecte.");
        }

        return M;
    }


    private byte[] h2(Element theta) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update((byte) 0x02);
        return sha256.digest(theta.toBytes());
    }

    private Element h3(byte[] sigma, byte[] message) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update((byte) 0x03);
        sha256.update(sigma);
        byte[] hashBytes = sha256.digest(message);
        return pairing.getZr().newElementFromHash(hashBytes, 0, hashBytes.length).getImmutable();
    }

    private byte[] h4(byte[] sigma, int length) throws NoSuchAlgorithmException {
        byte[] output = new byte[length];
        int offset = 0;
        int counter = 0;
        while (offset < length) {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update((byte) 0x04);
            sha256.update(sigma);
            sha256.update(ByteBuffer.allocate(4).putInt(counter).array());
            byte[] block = sha256.digest();
            int toCopy = Math.min(block.length, length - offset);
            System.arraycopy(block, 0, output, offset, toCopy);
            offset += toCopy;
            counter++;
        }
        return output;
    }

    private static Pairing loadPairingFromString(String paramsString) {
        try {
            Path tempFile = Files.createTempFile("ibe_pairing_", ".properties");
            Files.writeString(tempFile, paramsString);
            Pairing pairing = PairingFactory.getPairing(tempFile.toString());
            Files.delete(tempFile);
            return pairing;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement des paramètres de pairing", e);
        }
    }
}
