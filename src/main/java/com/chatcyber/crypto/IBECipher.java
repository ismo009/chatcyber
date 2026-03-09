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

/**
 * Chiffreur/Déchiffreur IBE basé sur le schéma <b>FullIdent</b> de Boneh-Franklin
 * (IND-ID-CCA sécurisé dans le modèle des oracles aléatoires).
 *
 * Paramètres publics : PP = (P, Ppub, H1, H2, H3, H4) avec :
 *   H1 : {0,1}*       → G*       (hash identité → point de G1)
 *   H2 : GT           → {0,1}^n  (hash sortie pairing → n=32 octets)
 *   H3 : {0,1}^n × M → Zp*      (hash (σ,M)  → scalaire)
 *   H4 : {0,1}^n      → {0,1}^* (hash σ → flux de bits, extensible)
 *
 * Chiffrement (Encrypt) :
 *   QID = H1(id)
 *   σ ← {0,1}^n  aléatoire
 *   r = H3(σ, M)
 *   U = r · P  ∈ G1
 *   θ = e(QID, Ppub)^r  ∈ GT
 *   V = σ ⊕ H2(θ)              (32 octets)
 *   W = M ⊕ H4(σ, |M|)         (|M| octets)
 *   Sortie : C = (U, V, W)
 *
 * Déchiffrement (Decrypt) :
 *   θ' = e(dID, U) = e(QID, Ppub)^r
 *   σ' = V ⊕ H2(θ')
 *   M' = W ⊕ H4(σ', |W|)
 *   r' = H3(σ', M')
 *   Vérifier U == r'·P  → sinon retourner ⊥
 *   Sortie : M'
 *
 * Format du paquet chiffré :
 *   [4 octets : longueur de U][U][32 octets : V][W (longueur variable)]
 */
public class IBECipher {

    /** Taille de σ : n = 256 bits = 32 octets */
    private static final int SIGMA_LENGTH = 32;

    private final Pairing pairing;
    private final Element generatorP;       // P ∈ G1
    private final Element publicKeyPpub;    // Ppub = s·P ∈ G1

    /**
     * Initialise le chiffreur IBE avec les paramètres publics du système.
     *
     * @param params Paramètres publics reçus de l'Autorité de Confiance
     */
    public IBECipher(SystemParameters params) {
        // Recréer le pairing bilinéaire à partir des paramètres sauvegardés
        this.pairing = loadPairingFromString(params.getPairingParameters());

        // Restaurer le générateur P et la clé publique Ppub
        this.generatorP = pairing.getG1().newElementFromBytes(params.getGeneratorP()).getImmutable();
        this.publicKeyPpub = pairing.getG1().newElementFromBytes(params.getPublicKeyPpub()).getImmutable();
    }

    /**
     * Chiffre un fichier pour un destinataire identifié par son email.
     *
     * @param inputFile      Fichier source en clair
     * @param outputFile     Fichier de sortie chiffré (.ibe)
     * @param recipientEmail Email du destinataire (identité IBE)
     */
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

    /**
     * Déchiffre un fichier avec la clé privée IBE de l'utilisateur.
     *
     * @param inputFile       Fichier chiffré (.ibe)
     * @param outputDir       Répertoire de sortie pour le fichier déchiffré
     * @param privateKeyBytes Clé privée IBE de l'utilisateur (dID sérialisée)
     * @return Le fichier déchiffré
     */
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

    /**
     * Chiffrement FullIdent (Boneh-Franklin IBE, IND-ID-CCA).
     *
     * @param data              Message en clair
     * @param recipientIdentity Email du destinataire (identité IBE publique)
     * @return Chiffré C = (U, V, W) sérialisé
     */
    public byte[] encrypt(byte[] data, String recipientIdentity) throws Exception {
        // QID = H1(id) : hash de l'identité vers un point de G1
        byte[] idHash = TrustAuthority.hashIdentity(recipientIdentity);
        Element qID = pairing.getG1().newElementFromHash(idHash, 0, idHash.length).getImmutable();

        // σ ← {0,1}^n  aléatoire
        byte[] sigma = new byte[SIGMA_LENGTH];
        new SecureRandom().nextBytes(sigma);

        // r = H3(σ, M) ∈ Zp*
        Element r = h3(sigma, data);

        // U = r · P ∈ G1
        Element U = generatorP.mulZn(r).getImmutable();

        // θ = e(QID, Ppub)^r ∈ GT
        Element theta = pairing.pairing(qID, publicKeyPpub).powZn(r).getImmutable();

        // V = σ ⊕ H2(θ)  [32 octets]
        byte[] h2bytes = h2(theta);
        byte[] V = new byte[SIGMA_LENGTH];
        for (int i = 0; i < SIGMA_LENGTH; i++) {
            V[i] = (byte) (sigma[i] ^ h2bytes[i]);
        }

        // W = M ⊕ H4(σ, |M|)  [|M| octets]
        byte[] h4bytes = h4(sigma, data.length);
        byte[] W = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            W[i] = (byte) (data[i] ^ h4bytes[i]);
        }

        // Assemblage : [len(U)][U][V][W]
        byte[] uBytes = U.toBytes();
        ByteBuffer buffer = ByteBuffer.allocate(4 + uBytes.length + SIGMA_LENGTH + W.length);
        buffer.putInt(uBytes.length);
        buffer.put(uBytes);
        buffer.put(V);
        buffer.put(W);
        return buffer.array();
    }

    /**
     * Déchiffrement FullIdent.
     *
     * @param ciphertext      Données chiffrées au format [len(U)][U][V][W]
     * @param privateKeyBytes Clé privée dID de l'utilisateur (sérialisée)
     * @return Message en clair, ou lève une exception si le chiffré est invalide
     */
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

    // ─── Fonctions de hachage H2, H3, H4 ────────────────────────────────────

    /**
     * H2 : GT → {0,1}^n
     * Hash de la sortie du pairing vers 32 octets.
     * Séparateur de domaine : 0x02.
     */
    private byte[] h2(Element theta) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update((byte) 0x02);
        return sha256.digest(theta.toBytes());
    }

    /**
     * H3 : {0,1}^n × {0,1}^* → Zp*
     * Hash de (σ, M) vers un scalaire de Zr.
     * Séparateur de domaine : 0x03.
     */
    private Element h3(byte[] sigma, byte[] message) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update((byte) 0x03);
        sha256.update(sigma);
        byte[] hashBytes = sha256.digest(message);
        return pairing.getZr().newElementFromHash(hashBytes, 0, hashBytes.length).getImmutable();
    }

    /**
     * H4 : {0,1}^n → {0,1}^length
     * Hash extensible de σ vers {@code length} octets, par SHA-256 en mode compteur.
     * Séparateur de domaine : 0x04.
     */
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

    // ─── Utilitaire ──────────────────────────────────────────────────────────

    /**
     * Charge un Pairing depuis une string de paramètres (format properties JPBC).
     * Écrit dans un fichier temporaire car JPBC requiert un chemin fichier.
     */
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
