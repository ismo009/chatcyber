package com.chatcyber.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

/**
 * Autorité de Confiance (Trust Authority / PKG) pour le système IBE de Boneh-Franklin.
 *
 * Responsabilités :
 * - Setup   : Génération des paramètres publics et de la clé maîtresse secrète.
 * - Extract : Génération de la clé privée d'un utilisateur à partir de son identité.
 *
 * Schéma FullIdent de Boneh-Franklin (cours IBE, pages 35-40) :
 *
 *   Setup :
 *     e : G × G → GT   (pairing bilinéaire, courbe Type A symétrique)
 *     P ∈ G1            (générateur)
 *     s ∈ Zp*           (clé maîtresse secrète — MSK)
 *     Ppub = s · P      (clé publique maîtresse)
 *     PP = (P, Ppub, H1, H2, H3, H4) publiés ; s gardé secret
 *
 *   Extract (Key-Gen) :
 *     QID = H1(id)       (H1 : {0,1}* → G1* via newElementFromHash)
 *     dID = s · QID      (clé privée de l'identité id)
 *
 *   Les fonctions H2, H3, H4 sont implantées dans IBECipher (côté client).
 */
public class TrustAuthority {

    private Pairing pairing;
    private Element masterSecret;   // s ∈ Zr  (clé maîtresse secrète)
    private Element generatorP;     // P ∈ G1  (générateur du groupe)
    private Element publicKeyPpub;  // Ppub = s·P ∈ G1 (clé publique maîtresse)
    private String pairingParamsString;

    public void setup(int rBits, int qBits) { //rBits=nb bits ordre sous groupe ; qBits = nb bits corps fq
        //Setup du système IBE de Boneh-Franklin
        System.out.println("[TA] Génération des paramètres de courbe Type A (r=" + rBits + ", q=" + qBits + ")...");

        // Génération des paramètres de la courbe bilinéaire de Type A
        TypeACurveGenerator curveGenerator = new TypeACurveGenerator(rBits, qBits);
        PairingParameters params = curveGenerator.generate();
        pairingParamsString = params.toString();

        // Création du pairing bilinéaire e: G1 × G1 → GT
        pairing = PairingFactory.getPairing(params);

        // Choix aléatoire du générateur P ∈ G1
        generatorP = pairing.getG1().newRandomElement().getImmutable();
        System.out.println("[TA] Générateur P choisi.");

        // Choix aléatoire de la clé maîtresse secrète s ∈ Zr
        masterSecret = pairing.getZr().newRandomElement().getImmutable();

        // Calcul de la clé publique maîtresse Ppub = s · P
        publicKeyPpub = generatorP.mulZn(masterSecret).getImmutable();
        System.out.println("[TA] Clé publique Ppub = s·P calculée.");
        System.out.println("[TA] Setup terminé avec succès.");
    }

    /**
     * Setup avec les paramètres par défaut (rBits=160, qBits=512).
     */
    public void setup() {
        setup(160, 512);
    }

    /**
     * Phase Extract du schéma de Boneh-Franklin.
     * Génère la clé privée d'un utilisateur à partir de son identité (adresse email).
     *
     * dID = s · H1(ID) où :
     *   - H1 : {0,1}* → G1 (fonction de hachage vers le groupe G1)
     *   - s est la clé maîtresse secrète
     *
     * @param identity L'adresse email de l'utilisateur
     * @return La clé privée sérialisée (bytes)
     */
    public byte[] extractPrivateKey(String identity) {
        if (pairing == null || masterSecret == null) {
            throw new IllegalStateException("Le système n'a pas été initialisé. Appelez setup() d'abord.");
        }

        System.out.println("[TA] Extraction de la clé privée pour : " + identity);

        // H1(ID) : Hash de l'identité vers un élément de G1
        byte[] identityHash = hashIdentity(identity);
        Element qID = pairing.getG1().newElementFromHash(identityHash, 0, identityHash.length).getImmutable();

        // dID = s · QID
        Element dID = qID.mulZn(masterSecret).getImmutable();

        System.out.println("[TA] Clé privée générée pour : " + identity);
        return dID.toBytes();
    }

    /**
     * Retourne les paramètres publics du système (à distribuer aux clients).
     */
    public SystemParameters getPublicParameters() {
        if (pairing == null) {
            throw new IllegalStateException("Le système n'a pas été initialisé.");
        }
        return new SystemParameters(
                pairingParamsString,
                generatorP.toBytes(),
                publicKeyPpub.toBytes()
        );
    }

    /**
     * Fonction de hachage H1 : hash SHA-256 d'une identité (email).
     * Le résultat sera ensuite mappé vers G1 via newElementFromHash().
     */
    public static byte[] hashIdentity(String identity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(identity.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible sur cette plateforme", e);
        }
    }

    // --- Accesseurs ---

    public Pairing getPairing() { return pairing; }
    public Element getMasterSecret() { return masterSecret; }
    public Element getGeneratorP() { return generatorP; }
    public Element getPublicKeyPpub() { return publicKeyPpub; }
    public String getPairingParamsString() { return pairingParamsString; }
    public boolean isInitialized() { return pairing != null; }
}
