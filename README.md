# ChatCyber — Messagerie Sécurisée avec Chiffrement à Base d'Identité

Application de messagerie sécurisée utilisant le **chiffrement à base d'identité (IBE)** selon le schéma de **Boneh-Franklin**. Les pièces jointes des emails sont chiffrées avec l'identité (adresse email) du destinataire.

## Architecture

Le système est composé de deux éléments principaux :

### 1. Autorité de Confiance (Trust Authority - AC)
- Génère les paramètres du système IBE (courbe elliptique Type A, pairing bilinéaire)
- Génère les clés privées des utilisateurs à partir de leur identité (email)
- Serveur TCP (port 7777 par défaut)

### 2. Client Mail Sécurisé
- Envoi et réception de mails via SMTP/IMAP (JavaMail API)
- Chiffrement IBE des pièces jointes (identité = email du destinataire)
- Déchiffrement des pièces jointes reçues avec la clé privée de l'utilisateur
- Interface graphique Swing avec FlatLaf

## Schéma Cryptographique — Boneh-Franklin IBE

```
Setup (AC) :
  1. Générer les paramètres de courbe Type A (r=160bits, q=512bits)
  2. Choisir P ∈ G1 (générateur), s ∈ Zr (clé maîtresse secrète)
  3. Calculer Ppub = s·P (clé publique maîtresse)
  4. Publier (G1, GT, e, P, Ppub, H1, H2)

Extract (AC → Utilisateur) :
  1. QID = H1(email) ∈ G1
  2. dID = s·QID (clé privée de l'utilisateur)

Encrypt (Expéditeur) :
  1. QID = H1(email_destinataire)
  2. r ← Zr aléatoire
  3. U = r·P
  4. g = e(QID, Ppub)^r
  5. K = H2(g) → clé AES-256
  6. V = AES-GCM_K(fichier)
  7. Chiffré = (U, IV, V)

Decrypt (Destinataire) :
  1. g' = e(dID, U)  [= e(QID, Ppub)^r par bilinéarité]
  2. K = H2(g')
  3. fichier = AES-GCM_K⁻¹(V)
```

## Prérequis

- **Java** 11 ou supérieur
- **Maven** 3.6 ou supérieur
- Un compte **Gmail** avec un [mot de passe d'application](https://support.google.com/mail/answer/185833?hl=fr)

## Compilation

```bash
mvn clean package
```

Le JAR exécutable sera généré dans `target/chatcyber-1.0-SNAPSHOT.jar`.

## Exécution

### Étape 1 — Lancer l'Autorité de Confiance

```bash
java -cp target/chatcyber-1.0-SNAPSHOT.jar com.chatcyber.TrustAuthorityApp
```

Cliquer sur **"▶ Démarrer"** pour initialiser le système IBE et démarrer le serveur.

### Étape 2 — Lancer le Client Mail

```bash
java -jar target/chatcyber-1.0-SNAPSHOT.jar
```

### Étape 3 — Configuration du Client

1. **Onglet Configuration** :
   - Saisir votre adresse Gmail et le mot de passe d'application
   - Vérifier les paramètres SMTP/IMAP (pré-configurés pour Gmail)
   - Cliquer sur **"💾 Sauvegarder"**

2. **Récupérer les paramètres IBE** :
   - Vérifier l'adresse du serveur AC (localhost:7777 par défaut)
   - Cliquer sur **"🔑 Récupérer les paramètres IBE"**

3. **Obtenir votre clé privée** :
   - Cliquer sur **"🔐 Demander ma clé privée"**

### Étape 4 — Envoyer un mail chiffré

1. **Onglet Composer** :
   - Saisir le destinataire, l'objet et le corps du message
   - Sélectionner un fichier à joindre
   - Cocher **"Chiffrer avec IBE"** (activé par défaut)
   - Cliquer sur **"📤 Envoyer"**

### Étape 5 — Recevoir et déchiffrer

1. **Onglet Boîte de réception** :
   - Cliquer sur **"🔄 Actualiser"**
   - Sélectionner un message avec une pièce jointe chiffrée (🔒)
   - Sélectionner la pièce jointe `.ibe`
   - Cliquer sur **"🔓 Télécharger + Déchiffrer"**

## Structure du Projet

```
chatcyber/
├── pom.xml                          # Configuration Maven
├── README.md
├── src/main/java/com/chatcyber/
│   ├── App.java                     # Point d'entrée client mail
│   ├── TrustAuthorityApp.java       # Point d'entrée serveur AC
│   ├── crypto/
│   │   ├── SystemParameters.java    # Paramètres publics IBE (sérialisables)
│   │   ├── TrustAuthority.java      # Logique de l'AC (Setup + Extract)
│   │   ├── TrustAuthorityServer.java # Serveur TCP de l'AC
│   │   ├── TrustAuthorityClient.java # Client TCP vers l'AC
│   │   └── IBECipher.java           # Chiffrement/déchiffrement IBE hybride
│   ├── mail/
│   │   ├── MailConfig.java          # Configuration SMTP/IMAP
│   │   ├── MailSender.java          # Envoi de mails (JavaMail)
│   │   ├── MailReceiver.java        # Réception de mails (IMAP)
│   │   └── EmailMessage.java        # DTO message email
│   └── gui/
│       ├── MainFrame.java           # Fenêtre principale
│       ├── ConfigPanel.java         # Panneau de configuration
│       ├── ComposePanel.java        # Panneau de composition
│       ├── InboxPanel.java          # Panneau boîte de réception
│       └── TrustAuthorityFrame.java # Fenêtre de l'AC
```

## Technologies Utilisées

| Technologie | Rôle |
|-------------|------|
| **JPBC** (Java Pairing-Based Cryptography) | Pairing bilinéaire, courbes elliptiques, IBE |
| **JavaMail API** | Envoi (SMTP) et réception (IMAP) de mails |
| **AES-256-GCM** | Chiffrement symétrique authentifié des fichiers |
| **Java Swing + FlatLaf** | Interface graphique moderne |
| **Maven** | Gestion des dépendances et compilation |

## Sécurité

- **Chiffrement hybride** : IBE (Boneh-Franklin) pour l'échange de clé + AES-256-GCM pour les données
- **Chiffrement authentifié** : AES-GCM assure confidentialité et intégrité
- **Hash-to-point** : SHA-256 + hash-to-curve pour mapper les identités vers G1
- Les clés privées et paramètres sont stockés localement dans `~/.chatcyber/`

## Auteurs

Projet réalisé dans le cadre du cours de **Cryptographie Avancée** — INSA HDF 2026

## Licence

Projet académique — Usage éducatif uniquement.
