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
    'activeTaskBkgColor': '#2563eb',
    'activeTaskBorderColor': '#1d4ed8',
    'section0': '#3b82f6',
    'section1': '#3b82f6',
    'section2': '#3b82f6',
    'section3': '#3b82f6',
    'section4': '#3b82f6',
    'taskText0': '#ffffff',
    'taskText1': '#ffffff',
    'taskText2': '#ffffff',
    'taskText3': '#ffffff',
    'taskText4': '#ffffff',
    'cScale0': '#3b82f6',
    'cScale1': '#3b82f6',
    'cScale2': '#3b82f6',
    'cScale3': '#3b82f6',
    'cScale4': '#3b82f6'
}}}%%
gantt
    title Planification du projet ChatCyber
    dateFormat YYYY-MM-DD
    axisFormat %d/%m

    section Zachary
    Recherche IBE et architecture              :done, z1, 2026-01-06, 15d
    Developpement du module crypto IBE         :done, z2, after z1, 20d
    Integration crypto avec le projet          :done, z3, 2026-03-17, 10d

    section Ismael
    Design de l interface                      :done, i1, 2026-02-17, 10d
    Developpement des ecrans principaux        :done, i2, after i1, 17d
    Corrections et finalisation UI             :done, i3, 2026-04-07, 5d

    section Julian
    Mise en place configuration mail           :done, j1, 2026-02-03, 5d
    Developpement envoi et reception           :done, j2, after j1, 14d
    Tests mail et sauvegarde config            :done, j3, after j2, 8d

    section Robin
    Recherche outils et technologies            :done, r1, 2026-01-06, 10d
    Documentation du projet                    :done, r2, 2026-04-02, 5d
    Preparation soutenance et planning         :active, r3, after r2, 6d
    Soutenance - Livraison                     :milestone, r4, after r3, 0d

    section Albin
    Developpement communication reseau         :done, a1, 2026-02-01, 12d
    Ecran de configuration et AC               :done, a2, after a1, 10d
    Tests globaux et validation                :done, a3, 2026-03-30, 8d
```

## Répartition par membre

| Membre | Rôle principal | Tâches clés |
|--------|---------------|-------------|
| **Zachary** | Cryptographie IBE | Recherche IBE, développement crypto, intégration |
| **Ismael** | Interface graphique | Design UI, écrans principaux, corrections |
| **Julian** | Messagerie email | Configuration mail, envoi/réception, tests |
| **Robin** | Recherche & Documentation | Recherche techno, documentation, soutenance |
| **Albin** | Réseau & Tests | Communication réseau, configuration AC, validation globale |