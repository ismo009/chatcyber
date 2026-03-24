# ChatCyber — Diagramme de Gantt du projet

> Chaque section = une personne = une couleur distincte.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {
    'sectionBkgColor': '#fef9c3',
    'sectionBkgColor2': '#fef9c3',
    'altSectionBkgColor': '#fef3c7',
    'taskBkgColor': '#fde68a',
    'taskTextColor': '#3f3f46',
    'taskBorderColor': '#facc15',
    'doneTaskBkgColor': '#fde68a',
    'doneTaskBorderColor': '#facc15',
    'activeTaskBkgColor': '#fde68a',
    'activeTaskBorderColor': '#facc15',
    'section0': '#fde68a',
    'section1': '#fde68a',
    'section2': '#fde68a',
    'section3': '#fde68a',
    'section4': '#fde68a',
    'taskText0': '#3f3f46',
    'taskText1': '#3f3f46',
    'taskText2': '#3f3f46',
    'taskText3': '#3f3f46',
    'taskText4': '#3f3f46',
    'cScale0': '#fde68a',
    'cScale1': '#fde68a',
    'cScale2': '#fde68a',
    'cScale3': '#fde68a',
    'cScale4': '#fde68a'
}}}%%
gantt
    title Planification du projet ChatCyber
    dateFormat YYYY-MM-DD
    axisFormat %d/%m

    section Zachary
    Recherche IBE et architecture              :done, z1, 2026-03-02, 5d
    Developpement du module crypto IBE         :done, z2, after z1, 6d
    Integration crypto avec le projet          :done, z3, after z2, 4d
    Redaction du rapport                       :done, z4, after z3, 2026-03-24

    section Ismael
    Design de l interface                      :done, i1, 2026-03-03, 4d
    Developpement des ecrans principaux        :done, i2, after i1, 6d
    Corrections et finalisation UI             :done, i3, after i2, 3d
    Redaction du rapport                       :done, i4, after i3, 2026-03-24

    section Julian
    Mise en place configuration mail           :done, j1, 2026-03-02, 3d
    Developpement envoi et reception           :done, j2, after j1, 5d
    Tests mail et sauvegarde config            :done, j3, after j2, 4d
    Redaction du rapport                       :done, j4, after j3, 2026-03-24

    section Robin
    Recherche outils et technologies           :done, r1, 2026-03-04, 4d
    Documentation du projet                    :done, r2, after r1, 4d
    Preparation soutenance et planning         :active, r3, after r2, 5d
    Redaction du rapport                       :done, r4, after r3, 2026-03-24
    Soutenance - Livraison                     :milestone, r5, 2026-03-24, 0d

    section Albin
    Developpement communication reseau         :done, a1, 2026-03-05, 5d
    Ecran de configuration et AC               :done, a2, after a1, 5d
    Tests globaux et validation                :done, a3, after a2, 4d
    Redaction du rapport                       :done, a4, after a3, 2026-03-24
```

## Répartition par membre

| Membre | Rôle principal | Tâches clés |
|--------|---------------|-------------|
| **Zachary** | Cryptographie IBE | Recherche IBE, développement crypto, intégration |
| **Ismael** | Interface graphique | Design UI, écrans principaux, corrections |
| **Julian** | Messagerie email | Configuration mail, envoi/réception, tests |
| **Robin** | Recherche & Documentation | Recherche techno, documentation, soutenance |
| **Albin** | Réseau & Tests | Communication réseau, configuration AC, validation globale |