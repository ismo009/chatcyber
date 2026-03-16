# ChatCyber — Diagramme de Gantt du projet

> Chaque section = une personne = une couleur distincte.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {
    'sectionBkgColor': '#dbeafe',
    'sectionBkgColor2': '#fce7f3',
    'altSectionBkgColor': '#d1fae5',
    'taskBkgColor': '#2563eb',
    'taskTextColor': '#ffffff',
    'taskBorderColor': '#1d4ed8',
    'doneTaskBkgColor': '#2563eb',
    'doneTaskBorderColor': '#1d4ed8',
    'activeTaskBkgColor': '#f59e0b',
    'activeTaskBorderColor': '#d97706',
    'section0': '#3b82f6',
    'section1': '#ec4899',
    'section2': '#10b981',
    'section3': '#f59e0b',
    'section4': '#8b5cf6',
    'taskText0': '#ffffff',
    'taskText1': '#ffffff',
    'taskText2': '#ffffff',
    'taskText3': '#ffffff',
    'taskText4': '#ffffff',
    'cScale0': '#3b82f6',
    'cScale1': '#ec4899',
    'cScale2': '#10b981',
    'cScale3': '#f59e0b',
    'cScale4': '#8b5cf6'
}}}%%
gantt
    title Planification du projet ChatCyber
    dateFormat YYYY-MM-DD
    axisFormat %d/%m

    section Zachary
    Etude du schema IBE Boneh-Franklin        :done, r1, 2026-01-06, 10d
    Conception de l architecture               :done, r3, after r1, 5d
    Implementation SystemParameters            :done, c1, 2026-01-20, 5d
    Implementation TrustAuthority Setup        :done, c2, after c1, 7d
    Implementation IBECipher Encrypt-Decrypt   :done, c3, after c2, 10d
    Integration IBE - Mail - GUI               :done, i1, 2026-03-17, 7d
    Chiffrement pieces jointes envoi           :done, i2, after i1, 5d

    section Ismael
    Design du theme UITheme                    :done, g1, 2026-02-17, 5d
    Fenetre principale MainFrame               :done, g2, after g1, 5d
    Onglet Composer ComposePanel               :done, g4, after g2, 7d
    Onglet Reception InboxPanel                :done, g5, after g4, 10d
    Dechiffrement pieces jointes reception     :done, i3, after i2, 5d
    Correction de bugs                         :done, i6, 2026-04-07, 5d

    section Julian
    Configuration MailConfig                   :done, m1, 2026-02-03, 3d
    Modele EmailMessage                        :done, m4, 2026-02-03, 2d
    Implementation MailSender SMTP             :done, m2, after m1, 7d
    Implementation MailReceiver IMAP           :done, m3, after m2, 7d
    Tests envoi-reception Gmail                :done, m5, after m3, 5d
    Persistance config chatcyber               :done, i4, after i1, 3d

    section Robin
    Etude de la bibliotheque JPBC              :done, r2, 2026-01-06, 7d
    Choix des technologies Swing JavaMail      :done, r4, 2026-01-13, 3d
    Redaction README principal                 :done, d1, 2026-04-02, 3d
    Documentation interface README-UI          :done, d2, after d1, 2d
    Diagramme de Gantt                         :active, d3, after d2, 1d
    Preparation soutenance                     :d4, after d3, 5d
    Soutenance - Livraison                     :milestone, d5, after d4, 0d

    section Albin
    Serveur TCP TrustAuthorityServer           :done, c4, 2026-02-01, 7d
    Client TCP TrustAuthorityClient            :done, c5, after c4, 5d
    Tests chiffrement-dechiffrement            :done, c6, after c3, 5d
    Onglet Configuration ConfigPanel           :done, g3, after g2, 7d
    Fenetre Autorite de Confiance              :done, g6, after g2, 7d
    Tests end-to-end 2 clients et AC           :done, i5, after i3, 5d
```

## Répartition par membre

| Membre | Rôle principal | Tâches clés |
|--------|---------------|-------------|
| **Zachary** | Cryptographie IBE | SystemParameters, TrustAuthority, IBECipher, intégration, chiffrement envoi |
| **Ismael** | Interface graphique | UITheme, MainFrame, ComposePanel, InboxPanel, déchiffrement réception, bugs |
| **Julian** | Messagerie email | MailConfig, MailSender, MailReceiver, EmailMessage, tests mail, persistance |
| **Robin** | Recherche & Documentation | Étude JPBC, choix technos, README, README-UI, Gantt, soutenance |
| **Albin** | Réseau & Tests | TrustAuthorityServer/Client, ConfigPanel, TrustAuthorityFrame, tests E2E |