package com.chatcyber.crypto;

import it.unisa.dia.gas.jpbc.*;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Chiffreur/Déchiffreur IBE basé sur le schéma de Boneh-Franklin.
 *
 * Approche hybride :
 *   1. IBE (Boneh-Franklin) pour dériver une clé symétrique partagée
 *   2. AES-256-GCM pour chiffrer/déchiffrer les données (chiffrement authentifié)
 *
 * Chiffrement (Encrypt) :
 *   - Entrée : données en clair, email du destinataire (identité IBE)
 *   - r ← Zr aléatoire
 *   - U = r · P
 *   - g = e(H1(ID), Ppub)^r   (pairing bilinéaire)
 *   - K = H2(g)               (dérivation de clé AES-256)
 *   - V = AES-GCM_K(données)
 *   - Sortie : (U, IV, V)
 *
 * Déchiffrement (Decrypt) :
 *   - Entrée : (U, IV, V), clé privée dID
 *   - g = e(dID, U)           (même valeur que lors du chiffrement)
 *   - K = H2(g)
 *   - données = AES-GCM_K^-1(V)
 *
 * Format du fichier chiffré :
 *   [4 octets : longueur de U][U][12 octets : IV AES-GCM][données chiffrées + tag GCM]
 */
public class IBECipher {

    private static final int GCM_IV_LENGTH = 12;    // 96 bits (recommandé pour AES-GCM)
    private static final int GCM_TAG_LENGTH = 128;   // 128 bits tag d'authentification

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
     * Chiffrement IBE hybride (Boneh-Franklin + AES-256-GCM).
     *
     * @param data              Données en clair
     * @param recipientIdentity Email du destinataire
     * @return Données chiffrées au format [len(U)][U][IV][encrypted+tag]
     */
    public byte[] encrypt(byte[] data, String recipientIdentity) throws Exception {
        // H1(ID) : hash de l'identité vers un point de G1
        byte[] idHash = TrustAuthority.hashIdentity(recipientIdentity);
        Element qID = pairing.getG1().newElementFromHash(idHash, 0, idHash.length).getImmutable();

        // r ← Zr (aléatoire)
        Element r = pairing.getZr().newRandomElement().getImmutable();

        // U = r · P  (composante publique du chiffré, envoyée au destinataire)
        Element U = generatorP.mulZn(r).getImmutable();

        // g = e(QID, Ppub) ∈ GT
        Element gID = pairing.pairing(qID, publicKeyPpub).getImmutable();

        // g^r = e(QID, Ppub)^r
        Element gIDr = gID.powZn(r).getImmutable();

        // H2(g^r) → clé AES-256
        byte[] aesKey = deriveAESKey(gIDr.toBytes());

        // Chiffrement AES-256-GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encrypted = cipher.doFinal(data);

        // Assemblage : [len(U)][U][IV][chiffré AES-GCM]
        byte[] uBytes = U.toBytes();
        ByteBuffer buffer = ByteBuffer.allocate(4 + uBytes.length + GCM_IV_LENGTH + encrypted.length);
        buffer.putInt(uBytes.length);
        buffer.put(uBytes);
        buffer.put(iv);
        buffer.put(encrypted);

        return buffer.array();
    }

    /**
     * Déchiffrement IBE hybride.
     *
     * @param ciphertext      Données chiffrées au format [len(U)][U][IV][encrypted+tag]
     * @param privateKeyBytes Clé privée dID de l'utilisateur
     * @return Données en clair
     */
    public byte[] decrypt(byte[] ciphertext, byte[] privateKeyBytes) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(ciphertext);

        // Extraire U ∈ G1
        int uLength = buffer.getInt();
        byte[] uBytes = new byte[uLength];
        buffer.get(uBytes);
        Element U = pairing.getG1().newElementFromBytes(uBytes).getImmutable();

        // Extraire l'IV AES-GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        // Extraire les données chiffrées (+ tag GCM)
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        // Restaurer la clé privée dID ∈ G1
        Element dID = pairing.getG1().newElementFromBytes(privateKeyBytes).getImmutable();

        // Calculer e(dID, U) = e(s·QID, r·P) = e(QID, P)^(sr) = g^r
        // C'est la même valeur que e(QID, Ppub)^r utilisée lors du chiffrement
        Element ePair = pairing.pairing(dID, U).getImmutable();

        // H2(e(dID, U)) → même clé AES-256
        byte[] aesKey = deriveAESKey(ePair.toBytes());

        // Déchiffrement AES-256-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        return cipher.doFinal(encrypted);
    }

    /**
     * H2 : Dérivation de clé AES-256 à partir de la sortie du pairing.
     * Utilise SHA-256 pour produire exactement 32 octets (256 bits).
     */
    private byte[] deriveAESKey(byte[] pairingOutput) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(pairingOutput);
    }

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
