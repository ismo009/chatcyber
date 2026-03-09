# ChatCyber — Guide de l'Interface Utilisateur

Ce document décrit en détail l'ensemble de l'interface graphique de l'application ChatCyber, construite avec **Java Swing** et le thème **FlatLaf**.

L'application se compose de **deux fenêtres distinctes** :
1. **Le Client Mail** (`MainFrame`) — pour envoyer et recevoir des emails chiffrés
2. **L'Autorité de Confiance** (`TrustAuthorityFrame`) — pour gérer le serveur IBE

---

## Table des matières

- [1. Fenêtre Client Mail](#1-fenêtre-client-mail)
  - [1.1 Barre d'en-tête](#11-barre-den-tête)
  - [1.2 Barre de statut](#12-barre-de-statut)
  - [1.3 Navigation par onglets](#13-navigation-par-onglets)
  - [1.4 Onglet Configuration](#14-onglet-configuration)
  - [1.5 Onglet Composer](#15-onglet-composer)
  - [1.6 Onglet Réception](#16-onglet-réception)
- [2. Fenêtre Autorité de Confiance](#2-fenêtre-autorité-de-confiance)
  - [2.1 En-tête et badge de statut](#21-en-tête-et-badge-de-statut)
  - [2.2 Barre de contrôle](#22-barre-de-contrôle)
  - [2.3 Terminal de logs](#23-terminal-de-logs)
  - [2.4 Pied de page](#24-pied-de-page)
- [3. Thème et Design System](#3-thème-et-design-system)
  - [3.1 Palette de couleurs](#31-palette-de-couleurs)
  - [3.2 Typographie](#32-typographie)
  - [3.3 Composants réutilisables](#33-composants-réutilisables)

---

## 1. Fenêtre Client Mail

**Classe** : `MainFrame`  
**Titre** : *ChatCyber - Messagerie Sécurisée IBE*  
**Taille** : 1050 × 750 px (minimum 800 × 550 px)

La fenêtre principale se divise en quatre zones : l'en-tête, les onglets de navigation à gauche, le contenu central, et la barre de statut en bas.

```
┌──────────────────────────────────────────────────────────┐
│  ChatCyber   Messagerie Sécurisée | IBE Boneh-Franklin   │  ← En-tête (bleu)
│                                              [IBE Actif] │  ← Badge IBE
├──────────┬───────────────────────────────────────────────┤
│          │                                               │
│ Config.  │          Contenu de l'onglet actif             │
│          │                                               │
│ Composer │                                               │
│          │                                               │
│ Récept.  │                                               │
│          │                                               │
├──────────┴───────────────────────────────────────────────┤
│  Prêt                                    ChatCyber v1.0  │  ← Barre de statut
└──────────────────────────────────────────────────────────┘
```

### 1.1 Barre d'en-tête

| Élément | Description |
|---------|-------------|
| **Titre** | "ChatCyber" en blanc, gras, taille 22 |
| **Sous-titre** | "Messagerie Sécurisée \| Chiffrement IBE Boneh-Franklin" en bleu clair |
| **Badge IBE** | Indicateur en haut à droite : **vert** "IBE Actif" si les paramètres IBE ET la clé privée sont chargés, **rouge** "IBE Inactif" sinon |

Le badge IBE se met à jour automatiquement à chaque fois que les paramètres système ou la clé privée changent.

### 1.2 Barre de statut

Barre horizontale en bas de la fenêtre avec :
- **À gauche** : message de statut contextuel (ex: "Prêt", "Envoi en cours...", "3 messages récupérés")
- **À droite** : numéro de version "ChatCyber v1.0"

### 1.3 Navigation par onglets

Les onglets sont affichés **verticalement à gauche** (`JTabbedPane.LEFT`) :
1. **Configuration** — paramètres email et IBE
2. **Composer** — rédaction et envoi de messages
3. **Réception** — boîte de réception et déchiffrement

---

### 1.4 Onglet Configuration

**Classe** : `ConfigPanel`

Cet onglet est un panneau scrollable composé de quatre sections sous forme de cartes.

```
┌─────────────────────────────────────────────┐
│  Configuration                              │
│  Configurez vos paramètres de messagerie... │
├─────────────────────────────────────────────┤
│                                             │
│  ┌─ Configuration Email ──────────────────┐ │
│  │  Adresse email    [___________________]│ │
│  │  Mot de passe     [___________________]│ │
│  │  Serveur SMTP     [__________] [port_] │ │
│  │  Serveur IMAP     [__________] [port_] │ │
│  │  ℹ Pour Gmail, utilisez un mot de      │ │
│  │    passe d'application                  │ │
│  └────────────────────────────────────────┘ │
│                                             │
│  ┌─ Autorité de Confiance (IBE) ──────────┐ │
│  │  Serveur AC [________] Port [____]     │ │
│  │                                        │ │
│  │  [Récupérer paramètres IBE]            │ │
│  │  [Demander ma clé privée]              │ │
│  │  [Lancer serveur AC]                   │ │
│  └────────────────────────────────────────┘ │
│                                             │
│  ┌─ État du système IBE ──────────────────┐ │
│  │  Paramètres IBE  [OK - Chargés]       │ │
│  │  Clé privée      [Non disponible]      │ │
│  └────────────────────────────────────────┘ │
│                                             │
│                [Sauvegarder la configuration]│
└─────────────────────────────────────────────┘
```

#### Carte "Configuration Email"

| Champ | Description | Valeur par défaut |
|-------|-------------|-------------------|
| **Adresse email** | Adresse email complète (ex: `user@gmail.com`) | — |
| **Mot de passe** | Mot de passe d'application (champ masqué) | — |
| **Serveur SMTP** | Hôte du serveur SMTP (ex: `smtp.gmail.com`) | Pré-configuré pour Gmail |
| **Port SMTP** | Port du serveur SMTP | `587` |
| **Serveur IMAP** | Hôte du serveur IMAP (ex: `imap.gmail.com`) | Pré-configuré pour Gmail |
| **Port IMAP** | Port du serveur IMAP | `993` |

> **Note** : Pour les comptes Gmail, il est nécessaire d'utiliser un **mot de passe d'application** et non le mot de passe principal du compte Google.

#### Carte "Autorité de Confiance (IBE)"

| Élément | Type | Description |
|---------|------|-------------|
| **Serveur AC** | Champ texte | Adresse du serveur de l'Autorité de Confiance (par défaut `localhost`) |
| **Port** | Champ texte | Port TCP du serveur AC (par défaut `7777`) |
| **Récupérer paramètres IBE** | Bouton outline | Se connecte à l'AC pour télécharger les paramètres publics du système IBE |
| **Demander ma clé privée** | Bouton primaire (bleu) | Demande à l'AC de générer la clé privée correspondant à l'adresse email configurée |
| **Lancer serveur AC** | Bouton succès (vert) | Ouvre la fenêtre de l'Autorité de Confiance pour démarrer le serveur localement |

#### Carte "État du système IBE"

Affiche deux badges d'état :
- **Paramètres IBE** : badge vert "OK - Chargés" ou badge rouge "Non chargés"
- **Clé privée** : badge vert "OK - Disponible" ou badge rouge "Non disponible"

Ces badges se mettent à jour automatiquement après chaque action de récupération.

#### Bouton "Sauvegarder la configuration"

Bouton bleu aligné à droite qui persiste toute la configuration dans un fichier local (`~/.chatcyber/`). Un message de confirmation s'affiche après la sauvegarde.

---

### 1.5 Onglet Composer

**Classe** : `ComposePanel`

Cet onglet permet de rédiger un email et de l'envoyer avec chiffrement optionnel de la pièce jointe.

```
┌──────────────────────────────────────────────┐
│  Composer un message                         │
│  Rédigez et envoyez un email sécurisé...     │
├──────────────────────────────────────────────┤
│                                              │
│  ┌─ Destinataire ──────────────────────────┐ │
│  │  À :     [____________________________] │ │
│  │  Objet : [____________________________] │ │
│  └─────────────────────────────────────────┘ │
│                                              │
│  ┌─ Message ───────────────────────────────┐ │
│  │                                         │ │
│  │  (Zone de texte pour le corps           │ │
│  │   du message — retour à la ligne        │ │
│  │   automatique)                          │ │
│  │                                         │ │
│  └─────────────────────────────────────────┘ │
│                                              │
│  ┌─ Pièce jointe ─────────────────────────┐ │
│  │  [Aucun fichier sélectionné]            │ │
│  │  [Parcourir...]  [X]                    │ │
│  │  ☑ Chiffrer avec IBE (identité du      │ │
│  │    destinataire)                         │ │
│  └─────────────────────────────────────────┘ │
│                                              │
│  ░░░░░░░░░░ (barre de progression)          │
│                       [Envoyer le message]   │
└──────────────────────────────────────────────┘
```

#### Carte "Destinataire"

| Champ | Description |
|-------|-------------|
| **À** | Adresse email du destinataire (obligatoire) |
| **Objet** | Objet du message |

#### Carte "Message"

Zone de texte multiligne avec retour à la ligne automatique pour rédiger le corps du message.

#### Carte "Pièce jointe"

| Élément | Description |
|---------|-------------|
| **Champ fichier** | Affiche le nom et la taille du fichier sélectionné (lecture seule) |
| **Parcourir...** | Bouton outline qui ouvre un sélecteur de fichier (`JFileChooser`) |
| **X** | Bouton rouge pour retirer la pièce jointe |
| **Chiffrer avec IBE** | Case à cocher (activée par défaut). Si cochée, la pièce jointe sera chiffrée avec l'identité IBE du destinataire avant l'envoi |

#### Zone d'envoi

| Élément | Description |
|---------|-------------|
| **Barre de progression** | Barre indéterminée apparaissant pendant l'envoi |
| **Envoyer le message** | Bouton vert qui lance l'envoi. Grisé automatiquement pendant l'envoi pour éviter les doubles clics |

**Processus d'envoi** :
1. Validation des champs (destinataire obligatoire, configuration email requise)
2. Si pièce jointe + chiffrement IBE activé → chiffrement du fichier (extension `.ibe`)
3. Envoi du mail via SMTP
4. Message de confirmation et réinitialisation du formulaire

---

### 1.6 Onglet Réception

**Classe** : `InboxPanel`

Cet onglet affiche la boîte de réception et permet de déchiffrer les pièces jointes sécurisées.

```
┌──────────────────────────────────────────────┐
│  Boîte de réception                          │
│  Consultez vos messages et déchiffrez...     │
├──────────────────────────────────────────────┤
│  [Actualiser]   Messages: [20 ▾]  ░░░░░░░░  │  ← Barre d'outils
├──────────────────────────────────────────────┤
│  De           │ Objet          │ Date  │ PJ  │
│───────────────┼────────────────┼───────┼─────│
│  alice@ex.com │ Document conf. │ 09/03 │ 📎  │  ← Table des messages
│  bob@ex.com   │ Re: Rapport    │ 08/03 │     │
│  ...          │ ...            │ ...   │     │
├──────────────────────────────────┬───────────┤
│                                  │ Pièces    │
│  Aperçu du message               │ jointes   │
│  (texte du mail sélectionné)     │           │
│                                  │ doc.ibe   │
│                                  │ img.png   │
│                                  │           │
│                                  │[Téléch.]  │
│                                  │[Déchiff.] │
├──────────────────────────────────┴───────────┤
│  [Déchiffrer un fichier local (.ibe)]        │  ← Barre du bas
└──────────────────────────────────────────────┘
```

#### Barre d'outils

| Élément | Description |
|---------|-------------|
| **Actualiser** | Bouton bleu qui se connecte en IMAP pour récupérer les derniers messages |
| **Messages** | Spinner numérique (1–200, pas de 10, défaut 20) pour choisir le nombre de messages à récupérer |
| **Barre de progression** | Indicateur d'activité pendant la récupération |

#### Table des messages

Tableau à 4 colonnes avec sélection simple et lignes alternées (blanc / gris clair) :

| Colonne | Largeur | Contenu |
|---------|---------|---------|
| **De** | 200 px | Adresse de l'expéditeur |
| **Objet** | 350 px | Objet du message |
| **Date** | 130 px | Date au format `dd/MM/yyyy HH:mm` |
| **PJ** | 40 px | Indicateur de pièce(s) jointe(s) |

L'en-tête du tableau est stylisé avec une bordure bleue inférieure.

#### Panneau d'aperçu (bas)

Zone divisée horizontalement :

**À gauche — Corps du message** :
- Zone de texte en lecture seule affichant le contenu du mail sélectionné
- Police Segoe UI 13 pt, retour à la ligne automatique

**À droite — Pièces jointes** (largeur fixe 260 px) :
- Titre "Pièces jointes"
- Liste des fichiers joints du message sélectionné
- Deux boutons d'action :

| Bouton | Style | Action |
|--------|-------|--------|
| **Télécharger** | Outline (bleu) | Sauvegarde la pièce jointe sélectionnée telle quelle |
| **Déchiffrer + Sauver** | Succès (vert) | Déchiffre le fichier `.ibe` avec la clé privée IBE puis sauvegarde le fichier d'origine |

#### Barre du bas

| Élément | Description |
|---------|-------------|
| **Déchiffrer un fichier local (.ibe)** | Bouton outline permettant de sélectionner un fichier `.ibe` stocké localement pour le déchiffrer sans passer par la boîte de réception |

---

## 2. Fenêtre Autorité de Confiance

**Classe** : `TrustAuthorityFrame`  
**Titre** : *Autorité de Confiance - Serveur IBE Boneh-Franklin*  
**Taille** : 750 × 550 px (minimum 550 × 400 px)  
**Thème** : **Sombre** (fond noir/gris foncé, texte vert terminal)

Cette fenêtre est indépendante et peut être lancée :
- Depuis le bouton "Lancer serveur AC" de l'onglet Configuration du client
- Directement via la commande `java -cp ... com.chatcyber.TrustAuthorityApp`

```
┌──────────────────────────────────────────────────┐
│  Autorité de Confiance                           │
│  Serveur IBE | Boneh-Franklin | Courbe Type A    │
│                                     [  Arrêté  ] │
├──────────────────────────────────────────────────┤
│  Port: [7777]  [Démarrer]  [Arrêter]   Cx: 0    │
├──────────────────────────────────────────────────┤
│                                                  │
│  +-------------------------------------------+   │
│  |  ChatCyber - Autorité de Confiance        |   │
│  |  Schéma IBE de Boneh-Franklin             |   │
│  |  Courbe bilinéaire Type A (r=160, q=512)  |   │
│  +-------------------------------------------+   │
│                                                  │
│  Cliquez sur "Démarrer" pour initialiser...      │
│                                                  │
│  [14:32:01] Démarrage du serveur sur port 7777   │
│  [14:32:03] Paramètres IBE générés               │
│  [14:32:03] Serveur en écoute...                 │
│  [14:35:12] Connexion entrante : 192.168.1.5     │
│                                                  │
├──────────────────────────────────────────────────┤
│  L'AC génère les paramètres IBE et distribue...  │
└──────────────────────────────────────────────────┘
```

### 2.1 En-tête et badge de statut

| Élément | Description |
|---------|-------------|
| **Titre** | "Autorité de Confiance" — blanc, gras, 20 pt |
| **Sous-titre** | "Serveur IBE \| Boneh-Franklin \| Courbe Type A" — gris |
| **Badge de statut** | Change dynamiquement selon l'état du serveur |

États du badge :

| État | Couleur | Texte |
|------|---------|-------|
| **Arrêté** | Gris sur fond sombre | "Arrêté" |
| **En marche** | Vert sur fond vert foncé | "En marche (:7777)" |
| **Erreur** | Rouge sur fond rouge foncé | "Erreur" |

### 2.2 Barre de contrôle

| Élément | Description |
|---------|-------------|
| **Port** | Champ texte éditable (police Consolas) pour saisir le port TCP. Désactivé quand le serveur est en marche |
| **Démarrer** | Bouton vert qui initialise le système IBE et démarre le serveur TCP. Se désactive après le lancement |
| **Arrêter** | Bouton rouge pour stopper le serveur. Désactivé tant que le serveur n'est pas lancé |
| **Connexions : N** | Compteur en temps réel du nombre de connexions reçues |

### 2.3 Terminal de logs

Zone de texte en lecture seule simulant un terminal :
- **Fond** : noir (`#18181B`)
- **Texte** : vert menthe (`#34D399`)
- **Police** : Consolas 12 pt
- **Sélection** : fond gris foncé, texte vert vif

Au démarrage, affiche un message de bienvenue ASCII art avec les caractéristiques du système. Les événements du serveur sont horodatés au format `[HH:mm:ss]` :
- Initialisation des paramètres IBE
- Connexions entrantes
- Demandes de paramètres et d'extraction de clés
- Erreurs

### 2.4 Pied de page

Barre horizontale en bas avec un texte explicatif en italique :
> *L'AC génère les paramètres IBE et distribue les clés privées aux utilisateurs.*

---

## 3. Thème et Design System

**Classe** : `UITheme`

Le thème visuel est défini de manière centralisée et appliqué globalement via FlatLaf.

### 3.1 Palette de couleurs

#### Couleurs d'action

| Nom | Code hex | Utilisation |
|-----|----------|-------------|
| **Primary** | `#2563EB` | Boutons principaux, onglets actifs, liens |
| **Primary Dark** | `#1D4ED8` | Survol des boutons principaux |
| **Primary Light** | `#DBEAFE` | Fond des boutons outline au survol |
| **Accent (Succès)** | `#10B981` | Boutons d'envoi, badges positifs |
| **Accent Dark** | `#059669` | Survol des boutons succès |
| **Danger** | `#EF4444` | Boutons de suppression, badges d'erreur |
| **Warning** | `#F59E0B` | Avertissements |

#### Couleurs neutres

| Nom | Code hex | Utilisation |
|-----|----------|-------------|
| **BG Main** | `#F9FAFB` | Fond général de l'application |
| **BG Card** | `#FFFFFF` | Fond des cartes et sections |
| **BG Sidebar** | `#F3F4F6` | Fond de la barre d'onglets et alternance de lignes |
| **Border** | `#E5E7EB` | Bordures des cartes et séparateurs |
| **Text Primary** | `#111827` | Texte principal |
| **Text Secondary** | `#6B7280` | Texte secondaire (sous-titres, descriptions) |
| **Text Muted** | `#9CA3AF` | Texte très discret (placeholders, version) |

### 3.2 Typographie

Toutes les polices utilisent **Segoe UI** (sans-serif) sauf les éléments monospace qui utilisent **Consolas**.

| Style | Police | Taille | Graisse | Usage |
|-------|--------|--------|---------|-------|
| **Title** | Segoe UI | 18 pt | Bold | Titres de section |
| **Heading** | Segoe UI | 14 pt | Bold | Sous-titres, titres de carte |
| **Body** | Segoe UI | 13 pt | Regular | Texte courant, champs |
| **Small** | Segoe UI | 11 pt | Regular | Descriptions, badges |
| **Label** | Segoe UI | 12 pt | Bold | Labels de formulaire |
| **Button** | Segoe UI | 13 pt | Bold | Texte des boutons |
| **Mono** | Consolas | 12 pt | Regular | Port, terminal, données techniques |

### 3.3 Composants réutilisables

Le `UITheme` fournit des méthodes de fabrication pour créer des composants stylisés de manière cohérente :

#### Boutons

| Méthode | Apparence | Utilisation |
|---------|-----------|-------------|
| `primaryButton(text)` | Fond bleu, texte blanc, survol bleu foncé | Actions principales (Sauvegarder, Actualiser, Demander clé) |
| `successButton(text)` | Fond vert, texte blanc, survol vert foncé | Actions positives (Envoyer, Déchiffrer + Sauver) |
| `dangerButton(text)` | Fond rouge, texte blanc | Actions destructives (Supprimer pièce jointe) |
| `outlineButton(text)` | Bordure bleue, fond blanc, survol bleu clair | Actions secondaires (Parcourir, Télécharger, Récupérer) |

Tous les boutons ont un curseur pointeur au survol et un padding uniforme de 8 × 20 px.

#### Cartes

| Méthode | Description |
|---------|-------------|
| `card()` | Panneau blanc avec bordure grise et padding interne (16 × 20 px) |
| `card(title)` | Panneau blanc avec titre de bordure (`TitledBorder`) |
| `headerPanel(title, subtitle)` | Carte d'en-tête avec titre 18 pt et sous-titre gris, bordure inférieure |

#### Champs de formulaire

| Méthode | Description |
|---------|-------------|
| `styledTextField(columns)` | Champ texte avec bordure arrondie et padding interne |
| `styledPasswordField(columns)` | Champ mot de passe avec le même style |
| `formLabel(text)` | Label de formulaire en gras 12 pt |

#### Badges et labels

| Méthode | Description |
|---------|-------------|
| `statusBadge(text, ok)` | Badge vert (fond `#D1FAE5`, texte `#065F46`) si `ok=true`, rouge (fond `#FEE2E2`, texte `#991B1B`) sinon |
| `sectionTitle(text)` | Label en gras 14 pt pour les titres de section |
| `descriptionLabel(text)` | Label discret 11 pt pour les notes et explications |

#### Configuration globale FlatLaf

Le thème configure automatiquement via `UIManager` :
- Coins arrondis (8 px) pour les boutons, champs et composants
- Barres de défilement arrondies avec marges internes
- Onglet actif sur fond blanc avec soulignement bleu
- Largeur de focus réduite à 1 px pour un rendu épuré

---

## Résumé des interactions principales

| Action utilisateur | Écran | Résultat |
|--------------------|-------|----------|
| Cliquer "Sauvegarder la configuration" | Configuration | Sauvegarde les paramètres email et AC dans `~/.chatcyber/` |
| Cliquer "Récupérer paramètres IBE" | Configuration | Connexion TCP à l'AC, téléchargement des paramètres publics IBE |
| Cliquer "Demander ma clé privée" | Configuration | L'AC génère et renvoie la clé privée pour l'email configuré |
| Cliquer "Lancer serveur AC" | Configuration | Ouvre la fenêtre de l'Autorité de Confiance |
| Cliquer "Envoyer le message" | Composer | Chiffre la pièce jointe (si activé) puis envoie le mail via SMTP |
| Cliquer "Actualiser" | Réception | Récupère les N derniers messages via IMAP |
| Sélectionner un message | Réception | Affiche le corps du message et la liste des pièces jointes |
| Cliquer "Déchiffrer + Sauver" | Réception | Déchiffre le fichier `.ibe` sélectionné et propose de l'enregistrer |
| Cliquer "Déchiffrer un fichier local" | Réception | Ouvre un sélecteur pour déchiffrer un fichier `.ibe` hors boîte de réception |
| Cliquer "Démarrer" | Autorité de Confiance | Génère les paramètres IBE et démarre le serveur TCP |
| Cliquer "Arrêter" | Autorité de Confiance | Stoppe le serveur TCP |
