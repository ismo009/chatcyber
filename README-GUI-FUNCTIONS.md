# ChatCyber - Reference des Fonctions GUI

Ce document decrit, classe par classe, ce que fait chaque fonction definie dans le package `com.chatcyber.gui`.

Portee du document :
- `MainFrame`
- `ConfigPanel`
- `ComposePanel`
- `InboxPanel`
- `TrustAuthorityFrame`
- `UITheme`

Le but est de documenter la logique de l'interface Swing, pas les classes cryptographiques ou mail situees dans les autres packages.

---

## 1. MainFrame

Classe : `com.chatcyber.gui.MainFrame`

Role global : fenetre principale du client ChatCyber. Elle charge la configuration locale, cree les trois onglets de l'application, conserve l'etat IBE en memoire et fournit des services communs aux panneaux enfants.

### `MainFrame()`

Constructeur principal.

Ce qu'il fait :
- definit le titre de la fenetre ;
- configure la taille initiale, la taille minimale et le centrage ;
- appelle `loadConfiguration()` pour charger la configuration email, les parametres IBE et la cle privee depuis le disque ;
- appelle `initComponents()` pour construire toute l'interface.

### `initComponents()`

Construit toute la fenetre Swing.

Ce qu'elle fait :
- applique un `BorderLayout` a la fenetre ;
- cree l'en-tete bleu avec le titre de l'application et le badge IBE ;
- cree la barre de statut en bas ;
- cree le `JTabbedPane` lateral ;
- instancie `ConfigPanel`, `ComposePanel` et `InboxPanel` ;
- ajoute ces panneaux dans les onglets "Configuration", "Composer" et "Reception".

### `wrapTab(JPanel content)`

Encapsule un panneau d'onglet dans un conteneur avec marge et fond uniforme.

Ce qu'elle fait :
- cree un panneau intermediaire avec fond `UITheme.BG_MAIN` ;
- ajoute une bordure vide pour aerer le contenu ;
- insere le panneau reel au centre.

Utilite : garantir une presentation visuelle homogene entre les onglets.

### `updateIBEBadge()`

Met a jour le badge d'etat IBE dans l'en-tete.

Ce qu'elle fait :
- appelle `isIBEReady()` ;
- affiche `IBE Actif` en vert si les parametres IBE et la cle privee sont presents ;
- affiche `IBE Inactif` en rouge sinon ;
- reapplique police, couleurs et padding du badge.

### `loadConfiguration()`

Charge l'etat persistant de l'application depuis le repertoire de donnees local.

Ce qu'elle fait :
- cree le dossier applicatif si necessaire ;
- charge la configuration mail via `MailConfig.load(...)` si le fichier existe ;
- sinon cree une configuration mail vide ;
- tente de charger les parametres IBE via `SystemParameters.loadFromFile(...)` ;
- tente de charger la cle privee IBE avec `Files.readAllBytes(...)`.

Effet : au demarrage, l'interface retrouve la configuration et l'etat IBE deja enregistres.

### `saveConfiguration()`

Sauvegarde la configuration email courante.

Ce qu'elle fait :
- appelle `mailConfig.save(...)` ;
- en cas d'erreur, affiche une boite de dialogue d'erreur.

### `saveSystemParams()`

Sauvegarde les parametres IBE publics si un objet `systemParams` est present.

Ce qu'elle fait :
- verifie que `systemParams` n'est pas nul ;
- ecrit le fichier des parametres via `systemParams.saveToFile(...)` ;
- en cas d'echec, ecrit seulement un message sur la sortie d'erreur.

### `savePrivateKey()`

Sauvegarde la cle privee IBE sur disque.

Ce qu'elle fait :
- verifie que `privateKey` n'est pas nul ;
- cree le dossier parent du fichier si besoin ;
- ecrit les octets de la cle avec `Files.write(...)` ;
- journalise une erreur sur `stderr` si l'ecriture echoue.

### `getMailConfig()`

Retourne l'objet `MailConfig` conserve par la fenetre principale.

Utilisation : permet aux panneaux enfants d'acceder a la configuration mail courante.

### `getSystemParams()`

Retourne les parametres IBE actuellement charges en memoire.

### `setSystemParams(SystemParameters systemParams)`

Remplace les parametres IBE en memoire et synchronise l'interface.

Ce qu'elle fait :
- met a jour le champ `systemParams` ;
- appelle `saveSystemParams()` pour persister la nouvelle valeur ;
- appelle `updateIBEBadge()` ;
- met a jour la barre de statut avec un message de succes.

### `getPrivateKey()`

Retourne la cle privee IBE chargee en memoire.

### `setPrivateKey(byte[] privateKey)`

Remplace la cle privee en memoire et synchronise l'etat global.

Ce qu'elle fait :
- met a jour le champ `privateKey` ;
- appelle `savePrivateKey()` ;
- appelle `updateIBEBadge()` ;
- affiche dans la barre de statut que la cle a ete enregistree.

### `isIBEReady()`

Indique si le client possede tout le necessaire pour dechiffrer ou chiffrer avec IBE.

Retour : `true` si `systemParams` et `privateKey` sont tous les deux non nuls.

### `hasSystemParams()`

Indique si les parametres IBE publics sont charges.

Retour : `true` si `systemParams` n'est pas nul.

### `updateStatus(String message)`

Met a jour le texte de la barre de statut.

Ce qu'elle fait :
- passe par `SwingUtilities.invokeLater(...)` pour garantir une mise a jour thread-safe de l'UI ;
- remplace le texte du label `statusBar`.

### `showError(String title, String message)`

Affiche une boite de dialogue d'erreur Swing.

Ce qu'elle fait :
- execute l'affichage via `invokeLater(...)` ;
- ouvre un `JOptionPane.ERROR_MESSAGE`.

### `showInfo(String title, String message)`

Affiche une boite de dialogue d'information Swing.

Ce qu'elle fait :
- execute l'affichage via `invokeLater(...)` ;
- ouvre un `JOptionPane.INFORMATION_MESSAGE`.

---

## 2. ConfigPanel

Classe : `com.chatcyber.gui.ConfigPanel`

Role global : onglet de configuration. Il gere les identifiants email, l'adresse de l'Autorite de Confiance, la recuperation des parametres IBE et l'extraction de la cle privee.

### `ConfigPanel(MainFrame mainFrame)`

Constructeur du panneau.

Ce qu'il fait :
- conserve une reference vers `MainFrame` ;
- configure le layout et le fond ;
- appelle `initComponents()` pour creer l'interface ;
- appelle `loadConfig()` pour remplir les champs a partir de la configuration deja chargee.

### `initComponents()`

Assemble tout le contenu visuel de l'onglet Configuration.

Ce qu'elle fait :
- cree un conteneur vertical scrollable ;
- ajoute l'en-tete de l'onglet ;
- ajoute successivement la carte email, la carte Autorite de Confiance et la carte d'etat IBE ;
- cree le bouton `Sauvegarder la configuration` ;
- place l'ensemble dans un `JScrollPane`.

### `createEmailCard()`

Construit la carte de configuration de messagerie.

Ce qu'elle fait :
- cree les champs `tfEmail`, `tfPassword`, `tfSmtpHost`, `tfSmtpPort`, `tfImapHost`, `tfImapPort` ;
- configure leurs infobulles ;
- agence les champs avec un `GridBagLayout` ;
- ajoute une note explicative pour Gmail.

Retour : le `JPanel` complet de la carte email.

### `createTACard()`

Construit la carte dediee a l'Autorite de Confiance.

Ce qu'elle fait :
- cree les champs `tfTaHost` et `tfTaPort` ;
- cree trois boutons d'action : recuperer les parametres IBE, demander la cle privee, lancer le serveur AC ;
- relie chaque bouton a sa methode metier (`fetchSystemParams()`, `extractPrivateKey()`, `launchTrustAuthority()`).

Retour : le `JPanel` complet de la carte AC.

### `createStatusCard()`

Construit la carte qui affiche l'etat courant du systeme IBE.

Ce qu'elle fait :
- cree deux badges `lblIbeStatus` et `lblKeyStatus` ;
- initialise le premier selon `mainFrame.hasSystemParams()` ;
- initialise le second selon `mainFrame.getPrivateKey() != null`.

Retour : un panneau qui sert uniquement d'affichage d'etat.

### `loadConfig()`

Charge les valeurs de `MailConfig` dans les champs de saisie.

Ce qu'elle fait :
- lit `mainFrame.getMailConfig()` ;
- copie l'email, le mot de passe, les hotes et ports SMTP/IMAP, ainsi que l'hote et le port AC dans les champs texte.

Effet : l'utilisateur voit dans l'onglet les valeurs deja connues par l'application.

### `saveConfig()`

Recupere les valeurs saisies et les enregistre dans `MailConfig`.

Ce qu'elle fait :
- lit tous les champs du formulaire ;
- nettoie les espaces avec `trim()` ;
- convertit les ports en entier quand c'est possible ;
- ignore silencieusement les erreurs de conversion de port ;
- appelle `mainFrame.saveConfiguration()` ;
- met a jour la barre de statut ;
- affiche une boite d'information de confirmation.

### `fetchSystemParams()`

Recupere les parametres IBE depuis le serveur de l'Autorite de Confiance.

Ce qu'elle fait :
- met d'abord la barre de statut sur un message de connexion ;
- lance un thread en arriere-plan ;
- cree un `TrustAuthorityClient` avec l'hote et le port saisis ;
- appelle `getParameters()` ;
- transmet le resultat a `mainFrame.setSystemParams(...)` ;
- met a jour le badge `lblIbeStatus` en vert et affiche un message de succes sur le thread Swing ;
- en cas d'erreur, affiche une boite de dialogue et met a jour la barre de statut.

### `extractPrivateKey()`

Demande au serveur AC la cle privee correspondant a l'adresse email renseignee.

Ce qu'elle fait :
- verifie d'abord que le champ email n'est pas vide ;
- affiche une erreur immediate si l'email est absent ;
- met a jour la barre de statut pour indiquer la demande en cours ;
- lance un thread en arriere-plan ;
- cree un `TrustAuthorityClient` ;
- appelle `extractKey(email)` ;
- sauvegarde la cle via `mainFrame.setPrivateKey(...)` ;
- met a jour le badge `lblKeyStatus` et ouvre un message de succes ;
- en cas d'echec, affiche un message d'erreur et met a jour le statut.

### `launchTrustAuthority()`

Ouvre la fenetre locale de serveur Trust Authority.

Ce qu'elle fait :
- lit le port dans `tfTaPort` ;
- si la conversion echoue, utilise `7777` par defaut ;
- cree un `TrustAuthorityFrame` sur ce port ;
- rend la fenetre visible.

### `updateStatusBadge(JLabel badge, boolean ok, String text)`

Met a jour visuellement un badge d'etat.

Ce qu'elle fait :
- change le texte du badge ;
- applique des couleurs vertes si `ok == true` ;
- applique des couleurs rouges sinon.

Utilisation : cette methode sert aux badges des parametres IBE et de la cle privee.

### `refreshStatus()`

Recalcule l'etat des deux badges depuis l'etat reel du `MainFrame`.

Ce qu'elle fait :
- relit `mainFrame.hasSystemParams()` ;
- relit `mainFrame.getPrivateKey()` ;
- reconfigure `lblIbeStatus` et `lblKeyStatus` en consequence.

---

## 3. ComposePanel

Classe : `com.chatcyber.gui.ComposePanel`

Role global : onglet de redaction. Il permet de preparer un email, d'attacher un fichier et, si demande, de chiffrer la piece jointe avec l'identite IBE du destinataire avant l'envoi SMTP.

### `ComposePanel(MainFrame mainFrame)`

Constructeur du panneau de composition.

Ce qu'il fait :
- stocke la reference `mainFrame` ;
- initialise layout et couleurs ;
- appelle `initComponents()`.

### `initComponents()`

Construit toute l'interface de redaction.

Ce qu'elle fait :
- cree l'en-tete du panneau ;
- cree les champs du destinataire et de l'objet ;
- cree la zone de texte du corps du message ;
- cree la zone piece jointe avec le champ de nom de fichier, les boutons `Parcourir...` et `X`, ainsi que la case `Chiffrer avec IBE` ;
- cree la barre de progression et le bouton d'envoi ;
- attache les actions aux boutons.

### `browseFile()`

Ouvre un selecteur de fichier pour choisir une piece jointe.

Ce qu'elle fait :
- ouvre un `JFileChooser` ;
- si l'utilisateur valide, stocke le fichier dans `selectedFile` ;
- affiche son nom et sa taille formatee dans `tfAttachment` ;
- remet la couleur du champ en style normal.

### `sendEmail()`

Methode centrale de l'onglet de composition.

Ce qu'elle fait :
- lit le destinataire, l'objet et le corps ;
- verifie que le destinataire n'est pas vide ;
- verifie que la configuration email est complete ;
- si une piece jointe doit etre chiffree, verifie que les parametres IBE sont charges ;
- desactive le bouton d'envoi et affiche la barre de progression ;
- lance un thread d'arriere-plan ;
- si une piece jointe est selectionnee et que le chiffrement est coche, cree un `IBECipher`, chiffre le fichier dans le dossier temporaire avec l'identite du destinataire, et remplace la piece jointe a envoyer par le fichier `.ibe` ;
- cree un `MailSender` et appelle `sendEmail(...)` ;
- en cas de succes, met a jour le statut, affiche une confirmation, puis appelle `clearForm()` ;
- en cas d'echec, affiche une erreur ;
- dans tous les cas, reactive le bouton et masque la barre de progression sur le thread Swing.

### `clearForm()`

Reinitialise le formulaire apres envoi reussi.

Ce qu'elle fait :
- vide les champs destinataire, objet et corps ;
- oublie le fichier selectionne ;
- remet le champ piece jointe sur `Aucun fichier selectionne` avec la couleur attenuee.

### `formatSize(long bytes)`

Convertit une taille de fichier brute en chaine lisible.

Ce qu'elle fait :
- retourne des octets si la taille est inferieure a 1024 ;
- retourne des Ko avec une decimale entre 1 Ko et 1 Mo ;
- retourne des Mo avec une decimale au-dela.

Utilisation : afficher la taille de la piece jointe dans `browseFile()`.

---

## 4. InboxPanel

Classe : `com.chatcyber.gui.InboxPanel`

Role global : onglet de reception. Il recupere les messages via IMAP, affiche leur contenu, liste les pieces jointes et permet soit de les telecharger, soit de les dechiffrer si elles sont au format IBE.

### `InboxPanel(MainFrame mainFrame)`

Constructeur du panneau de reception.

Ce qu'il fait :
- stocke la reference `mainFrame` ;
- configure le layout et le fond ;
- appelle `initComponents()`.

### `initComponents()`

Construit l'interface complete de la boite de reception.

Ce qu'elle fait :
- cree l'en-tete ;
- cree la barre d'outils avec le bouton `Actualiser`, le spinner de nombre de messages et la barre de progression ;
- cree le tableau des messages ;
- installe un `ListSelectionListener` pour appeler `showSelectedMessage()` lors d'un changement de selection ;
- cree le panneau d'apercu du message ;
- cree la liste des pieces jointes ;
- cree les boutons `Telecharger`, `Dechiffrer + Sauver` et `Dechiffrer un fichier local (.ibe)` ;
- compose l'ensemble avec un `JSplitPane` et des panneaux auxiliaires.

### `refreshInbox()`

Recupere les derniers messages via IMAP.

Ce qu'elle fait :
- verifie que la configuration email est complete ;
- desactive le bouton `Actualiser` et affiche la progression ;
- lit la valeur du spinner pour savoir combien de messages charger ;
- lance un thread en arriere-plan ;
- ferme l'ancien `MailReceiver` s'il existe ;
- cree un nouveau `MailReceiver`, se connecte puis appelle `fetchMessages(count)` ;
- stocke les messages dans `messages` ;
- sur le thread Swing, appelle `updateMessageTable()` puis met a jour le statut ;
- en cas d'erreur, affiche une erreur IMAP ;
- en fin de traitement, reactive le bouton et masque la barre de progression.

### `updateMessageTable()`

Reconstruit le tableau a partir de la liste `messages`.

Ce qu'elle fait :
- vide `tableModel` ;
- pour chaque message, calcule la date formatee ;
- calcule le marqueur de piece jointe : `[IBE]` si une piece jointe chiffree est detectee, `[PJ]` si une piece jointe simple est presente, sinon chaine vide ;
- ajoute une ligne au tableau.

### `showSelectedMessage()`

Affiche le contenu du message actuellement selectionne.

Ce qu'elle fait :
- lit la ligne selectionnee dans `messageTable` ;
- si rien n'est selectionne, vide l'apercu et la liste des pieces jointes ;
- sinon recupere le `EmailMessage` correspondant ;
- construit un texte d'apercu avec expediteur, objet, date et corps du message ;
- l'affiche dans `taPreview` ;
- recharge `attachmentListModel` a partir des pieces jointes du message.

### `downloadAttachment()`

Telecharge une piece jointe telle quelle, sans dechiffrement.

Ce qu'elle fait :
- verifie qu'un message et une piece jointe sont selectionnes ;
- recupere l'objet `AttachmentInfo` correspondant ;
- ouvre un `JFileChooser` pour choisir le nom de destination ;
- lance un thread en arriere-plan ;
- appelle `mailReceiver.downloadAttachment(...)` dans le dossier cible ;
- renomme le fichier si le nom recu ne correspond pas au nom choisi ;
- affiche un message de succes et met a jour la barre de statut ;
- en cas d'echec, affiche une erreur.

### `downloadAndDecrypt()`

Telecharge une piece jointe chiffree `.ibe`, puis la dechiffre avec la cle privee IBE locale.

Ce qu'elle fait :
- verifie qu'un message et une piece jointe sont selectionnes ;
- verifie que `mainFrame.isIBEReady()` est vrai ;
- verifie que la piece jointe est marquee comme chiffree ;
- propose un nom de sortie sans l'extension `.ibe` ;
- lance un thread en arriere-plan ;
- telecharge d'abord la piece jointe dans un dossier temporaire ;
- cree un `IBECipher` avec les parametres publics du client ;
- appelle `decryptFile(...)` avec la cle privee de l'utilisateur ;
- renomme le fichier dechiffre si necessaire vers le nom choisi ;
- supprime le fichier temporaire chiffre ;
- affiche un message de succes et met a jour le statut ;
- en cas d'echec, affiche une erreur expliquant notamment que la cle privee peut ne pas correspondre a l'identite.

### `decryptLocalFile()`

Dechiffre un fichier `.ibe` deja present localement sur le disque.

Ce qu'elle fait :
- verifie que le client est pret pour IBE via `mainFrame.isIBEReady()` ;
- ouvre un premier selecteur pour choisir un fichier chiffre `.ibe` ;
- ouvre un second selecteur pour choisir le fichier de sortie ;
- lance un thread en arriere-plan ;
- cree un `IBECipher` ;
- appelle `decryptFile(...)` avec la cle privee stockee dans `MainFrame` ;
- renomme le resultat si besoin ;
- affiche un message de succes et met a jour le statut ;
- en cas d'erreur, affiche un message de dechiffrement.

---

## 5. TrustAuthorityFrame

Classe : `com.chatcyber.gui.TrustAuthorityFrame`

Role global : fenetre de controle du serveur Trust Authority. Elle permet de demarrer et d'arreter le serveur IBE, de suivre les logs et d'afficher le nombre de connexions entrantes.

### `TrustAuthorityFrame(int port)`

Constructeur de la fenetre serveur.

Ce qu'il fait :
- memorise le port initial ;
- initialise le compteur de connexions a zero ;
- configure la taille, la taille minimale, le centrage et la politique de fermeture ;
- appelle `initComponents()` ;
- installe un `WindowListener` qui appelle `stopServer()` lors de la fermeture de la fenetre.

### `initComponents()`

Construit toute l'interface sombre de la fenetre serveur.

Ce qu'elle fait :
- cree l'en-tete avec le titre et le badge d'etat du serveur ;
- cree la barre de controle avec le champ port, les boutons `Demarrer` et `Arreter`, et le compteur de connexions ;
- cree la zone de logs `taLog` dans un `JScrollPane` ;
- cree le pied de page explicatif ;
- ajoute enfin un message d'accueil dans le terminal de logs avec `appendLog(...)`.

### `createDarkButton(String text, Color accent)`

Fabrique un bouton sombre stylise pour cette fenetre.

Ce qu'elle fait :
- cree un bouton avec une couleur d'accent ;
- applique police, bordure, curseur main et couleurs ;
- installe un effet de survol qui assombrit le fond quand le bouton est actif.

Retour : le bouton configure.

### `startServer()`

Demarre le serveur Trust Authority.

Ce qu'elle fait :
- lit et valide le port saisi dans `tfPort` ;
- affiche une erreur si le port n'est pas un entier valide ;
- desactive le bouton `Demarrer` et le champ de port ;
- remet le compteur de connexions a zero ;
- ajoute une ligne de log de demarrage avec `appendLog(...)` ;
- lance un thread en arriere-plan ;
- cree un `TrustAuthorityServer` ;
- enregistre un listener de messages pour recopier les logs serveur dans `taLog` et incrementer le compteur quand un message contient `Connexion entrante` ;
- appelle `server.start()` ;
- en cas de succes, met a jour le badge de statut sur `En marche` et active le bouton `Arreter` ;
- en cas d'erreur, journalise l'erreur, reactive les controles et affiche le badge `Erreur`.

### `stopServer()`

Arrete le serveur local si celui-ci tourne.

Ce qu'elle fait :
- verifie que `server` existe et que `server.isRunning()` est vrai ;
- appelle `server.stop()` ;
- ajoute une ligne de log d'arret ;
- reactive le bouton `Demarrer` ;
- desactive `Arreter` ;
- reactive le champ de port ;
- remet le badge de statut sur `Arrete` avec son style par defaut.

### `timestamp()`

Retourne l'heure courante formatee pour les logs.

Format retourne : `[HH:mm:ss]`.

### `appendLog(String text)`

Ajoute du texte a la console de logs.

Ce qu'elle fait :
- concatene le texte a `taLog` ;
- deplace le curseur a la fin pour garder le dernier log visible.

---

## 6. UITheme

Classe : `com.chatcyber.gui.UITheme`

Role global : utilitaire statique de theme. Cette classe centralise les couleurs, polices, bordures et fabriques de composants reutilisables pour toute l'interface.

### `UITheme()`

Constructeur prive.

Ce qu'il fait :
- empeche l'instanciation de la classe, car tout son contenu est statique.

### `cardBorder()`

Construit la bordure standard des cartes.

Ce qu'elle fait :
- cree une bordure composee d'un contour fin gris clair et d'un padding interne base sur `CARD_INSETS`.

Retour : une `Border` reutilisable.

### `cardBorderWithTitle(String title)`

Construit une bordure de carte avec titre visible.

Ce qu'elle fait :
- cree une `TitledBorder` portant le titre fourni ;
- applique la police `FONT_HEADING` et la couleur de texte principale ;
- ajoute un padding interne.

### `primaryButton(String text)`

Fabrique un bouton primaire bleu.

Ce qu'elle fait :
- cree un `JButton` ;
- applique police, fond bleu, texte blanc, style sans focus visible ;
- ajoute un effet hover qui passe du bleu principal au bleu fonce.

### `successButton(String text)`

Fabrique un bouton vert utilise pour les actions reussies ou principales positives.

Ce qu'elle fait :
- cree un bouton stylise comme `primaryButton(...)`, mais en vert ;
- ajoute un hover qui fonce la couleur du bouton.

### `dangerButton(String text)`

Fabrique un bouton rouge pour les actions de suppression ou retrait.

Ce qu'elle fait :
- cree un bouton rouge avec texte blanc ;
- applique le style de base du theme ;
- contrairement aux boutons primaire et succes, ne definit pas d'effet de survol personnalise.

### `outlineButton(String text)`

Fabrique un bouton secondaire a contour bleu.

Ce qu'elle fait :
- cree un bouton a fond blanc et texte bleu ;
- ajoute une bordure bleue ;
- applique un hover vers `PRIMARY_LIGHT`.

### `sectionTitle(String text)`

Retourne un label de titre de section.

Ce qu'elle fait :
- cree un `JLabel` avec `FONT_HEADING` et la couleur principale du texte.

### `descriptionLabel(String text)`

Retourne un label de description secondaire.

Ce qu'elle fait :
- cree un `JLabel` en petite police grisee, pour les notes et sous-textes.

### `styledTextField(int columns)`

Fabrique un champ texte selon le theme.

Ce qu'elle fait :
- cree un `JTextField` ;
- applique `FONT_BODY` ;
- applique une bordure grise avec padding interne.

### `styledPasswordField(int columns)`

Fabrique un champ mot de passe selon le theme.

Ce qu'elle fait :
- cree un `JPasswordField` ;
- applique la meme presentation que `styledTextField(...)`.

### `formLabel(String text)`

Retourne un label destine aux formulaires.

Ce qu'elle fait :
- cree un `JLabel` avec la police `FONT_LABEL` et la couleur de texte principale.

### `statusBadge(String text, boolean ok)`

Fabrique un badge d'etat vert ou rouge.

Ce qu'elle fait :
- cree un `JLabel` opaque ;
- si `ok` vaut vrai, applique des couleurs de succes ;
- sinon applique des couleurs d'erreur ;
- ajoute un padding interne.

### `card()`

Fabrique une carte sans titre.

Ce qu'elle fait :
- cree un `JPanel` avec fond blanc ;
- applique `cardBorder()`.

### `card(String title)`

Fabrique une carte avec titre.

Ce qu'elle fait :
- cree un `JPanel` avec fond blanc ;
- applique `cardBorderWithTitle(title)`.

### `separator()`

Fabrique un separateur horizontal stylise.

Ce qu'elle fait :
- cree un `JSeparator` ;
- lui applique la couleur de bordure du theme.

### `headerPanel(String title, String subtitle)`

Construit un panneau d'en-tete reutilisable pour les ecrans.

Ce qu'elle fait :
- cree un panneau vertical ;
- ajoute le titre principal ;
- si le sous-titre est non vide, ajoute un espace vertical puis un second label descriptif ;
- applique fond blanc, bordure basse et padding.

Retour : un `JPanel` pret a etre insere en haut d'un ecran.

### `applyGlobalTheme()`

Configure certaines cles du `UIManager` Swing pour harmoniser l'apparence globale.

Ce qu'elle fait :
- ajuste les arcs des composants ;
- ajuste l'apparence de la scrollbar ;
- configure les couleurs des onglets, tableaux et barres de progression ;
- prepare ainsi l'integration du theme avec FlatLaf et les composants Swing standards.

---

## Resume rapide par usage

Si vous cherchez ou se trouve une fonctionnalite precise :

- chargement et sauvegarde de l'etat global : `MainFrame`
- saisie de la configuration email et AC : `ConfigPanel`
- envoi et chiffrement de pieces jointes : `ComposePanel`
- reception, telechargement et dechiffrement : `InboxPanel`
- pilotage du serveur Trust Authority : `TrustAuthorityFrame`
- styles et composants reutilisables : `UITheme`
