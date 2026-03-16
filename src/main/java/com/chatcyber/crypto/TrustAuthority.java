package com.chatcyber.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;


public class TrustAuthority {

    private Pairing pairing;
    private Element masterSecret;
    private Element generatorP;
    private Element publicKeyPpub;
    private String pairingParamsString;

    public Pairing getPairing() { return pairing; }
    public Element getMasterSecret() { return masterSecret; }
    public Element getGeneratorP() { return generatorP; }
    public Element getPublicKeyPpub() { return publicKeyPpub; }
    public String getPairingParamsString() { return pairingParamsString; }
    public boolean isInitialized() { return pairing != null; }

    public void setup(int rBits, int qBits) { //rBits=nb bits ordre sous groupe ; qBits = nb bits corps fq
        //Setup du système IBE de Boneh-Franklin
        System.out.println("[TA] Génération des paramètres de courbe Type A (r=" + rBits + ", q=" + qBits + ")");

        TypeACurveGenerator curveGenerator = new TypeACurveGenerator(rBits, qBits);
        PairingParameters params = curveGenerator.generate();
        pairingParamsString = params.toString();

        pairing = PairingFactory.getPairing(params);

        generatorP = pairing.getG1().newRandomElement().getImmutable();
        System.out.println("[TA] Générateur P choisi.");

        masterSecret = pairing.getZr().newRandomElement().getImmutable();

        publicKeyPpub = generatorP.mulZn(masterSecret).getImmutable();
        System.out.println("[TA] Clé publique Ppub = s·P calculée.");
        System.out.println("[TA] Setup terminé avec succès.");
    }

    public void setup() {
        setup(160, 512); //Parametre defaut 
    }

    public byte[] extractPrivateKey(String identity) {
        if (pairing == null || masterSecret == null) {
            throw new IllegalStateException("Systeme non initialisé, régler bug setup non appellé");
        }

        System.out.println("[TA] Extraction de la clé privée pour  l'indentité : " + identity);

        //hash vers element de G1
        byte[] identityHash = hashIdentity(identity);
        Element qID = pairing.getG1().newElementFromHash(identityHash, 0, identityHash.length).getImmutable();

        Element dID = qID.mulZn(masterSecret).getImmutable();

        System.out.println("[TA] Clé privée générée pour : " + identity);
        return dID.toBytes();
    }

    public SystemParameters getPublicParameters() {
        if (pairing == null) {
            throw new IllegalStateException("System non initialisé, erreur");
        }
        return new SystemParameters(
                pairingParamsString,
                generatorP.toBytes(),
                publicKeyPpub.toBytes()
        );
    }

    public static byte[] hashIdentity(String identity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(identity.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible sur cette plateforme", e);
        }
    }

}
