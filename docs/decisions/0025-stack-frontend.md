# 0025 — Stack technique et système de design du frontend

## Contexte

La stack backend est actée (0024). Le frontend sert trois interfaces sur le même backend et le même contrat : le backoffice de l'accueil et du gérant (R1), l'application coach (R4), le portail parent (R8). La décision 0017 impose une bibliothèque de composants open source et un système de design maison, sans template acheté. Le `CLAUDE.md` impose l'i18n dès le départ, arabe droite-à-gauche compris, et aucun libellé en dur.

Ambition posée par Omar : un design moderne et premium, inspiré de Stripe et d'Apple, des parcours pensés pour l'utilisateur, une charte alignée sur le logo du club pilote, des composants modernes, une cinématique épurée.

## Décision

| Sujet | Choix |
| --- | --- |
| Framework | Angular, version stable courante à l'initialisation (21 ou plus récente) ; composants standalone, signals, zoneless, nouveau flux de contrôle ; aucun NgRx |
| Organisation | Un workspace Angular unique dans `frontend/` : applications `backoffice` (R1), `coach` (R4), `parent` (R8) ; bibliothèques partagées `ui` (système de design) et `api-client` (généré). Pas de micro-frontend |
| Composants et styles | PrimeNG en mode thémable par jetons de design + Tailwind CSS ; jetons maison en variables CSS |
| Charte | Système de design **Clubify**, neutre et premium ; **jetons de marque surchargeables par club** (logo, couleur primaire, couleur secondaire), configurés par ADM-01 et appliqués aux documents, au portail parent et en accent dans le backoffice. Le club pilote est le premier thème, pas la charte du produit |
| Références de design | Stripe Dashboard pour le backoffice (densité, tableaux, chiffres alignés, une action évidente par écran) ; principes Apple (clarté, déférence au contenu, profondeur) pour le coach mobile et le parent |
| Typographie | Inter pour le latin, IBM Plex Sans Arabic pour l'arabe, auto-hébergées ; chiffres tabulaires sur tous les montants |
| Icônes | Lucide |
| i18n | Transloco : langue par utilisateur, changement à chaud, direction droite-à-gauche basculée dynamiquement ; propriétés CSS logiques partout |
| Client d'API | Généré depuis `contracts/openapi.yaml` par openapi-generator (cible typescript-angular) ; jamais écrit à la main |
| Mouvement | CSS uniquement (`animate.enter`/`animate.leave`, transitions, View Transitions API) ; jetons de durée et d'assouplissement ; `prefers-reduced-motion` respecté ; aucune animation sur le chemin critique du comptoir |
| Tests | Vitest (unitaires), Storybook (composants et écrans isolés, FR et RTL), Playwright (parcours critiques seulement : UC1, UC11) |
| Cible | Poste de l'accueil récent, grand écran, navigateur à jour ; budget de performance tout de même plafonné (routes lazy, `@defer`, bundle initial vérifié en CI) |

Le détail des conventions est dans `frontend/CLAUDE.md`.

## Raison

Angular est connu des agents et structuré pour un monorepo multi-applications sans outil supplémentaire. PrimeNG apporte ce que le comptoir exige et qu'un design maison pur coûterait cher à refaire : tableau de données éditable en ligne (matrice de prix OFR-04 avec collage depuis Excel, rosters, caisse, balance âgée), sélecteurs, autocomplétion, RTL et accessibilité ; son mode thémable par jetons laisse le rendu au système de design maison, ce qui permet le rendu Stripe sans se battre contre la bibliothèque. Le workspace unique évite de dupliquer le système de design et le client d'API dans les applications coach et parent.

La charte Clubify avec jetons par club est la seule compatible avec un SaaS multi-clubs : un backoffice aux couleurs du club pilote serait à refaire au deuxième client et ne ressemblerait pas au produit.

Stripe est la bonne référence pour un outil de comptoir : dense, rapide, sans hésitation ; Apple l'est pour des interfaces mobiles de consultation. Premium ne veut pas dire aéré : l'accueil a un parent devant elle et cinq minutes.

## Alternatives écartées

- Angular Material : officiel et accessible, mais rendu Material reconnaissable et long à faire ressembler à Stripe ; tableau sans édition en ligne.
- Headless (Angular CDK + Angular Aria + Tailwind) : contrôle total, mais tout composant riche à construire ; Angular Aria encore en preview.
- Micro-frontends : trois interfaces qui ne se mélangent pas dans une même page, un seul backend, petite équipe ; complexité sans bénéfice.
- Garder `backoffice-frontend/` seul : duplication du système de design et du client d'API en R4 et R8.
- i18n natif Angular : un build par langue, pas de changement à chaud ; contredit « langue par utilisateur » (PLT-08).
- Couleurs du club pilote comme charte du produit : à refaire au deuxième club.
- `@angular/animations` : déprécié ; le CSS suffit et respecte naturellement la réduction de mouvement.
- NgRx : les signals et les services suffisent au périmètre.

## Source dans le cahier des charges

Sections 4.1 (benchmark : appel coach sur mobile), 5 (langues), 10.I (adoption par l'accueil, écrans proches des grilles), PLT-08, ADM-01, OFR-04, ONB-01, UC1, UC6, UC7 ; décision 0017.

## Écarts ou points ouverts

- Aucun écart avec le cahier des charges : il ne prescrit aucune technologie.
- Nouvelles dépendances validées par Omar le 2026-09-19 : PrimeNG, Tailwind CSS, Transloco, openapi-generator (typescript-angular), Storybook, Playwright, Lucide, polices Inter et IBM Plex Sans Arabic auto-hébergées. Toute dépendance supplémentaire repasse par lui.
- **Logo du club pilote** : le fichier déposé dans `docs/cahier-des-charges/sources/logo/` est le logo Eduschool International, de même style que celui du P'tit Club selon Omar (capitales serif, orange et bleu acier, pictogramme intégré). Palette extraite : orange `#F08840`, bleu acier `#307890`. Elle sert de premier thème de marque ; à remplacer par les couleurs exactes du logo du P'tit Club dès qu'il est fourni.
- Le dossier `backoffice-frontend/` a été renommé `frontend/` pour accueillir le workspace.
- Les maquettes du parcours comptoir (UC1 à UC4, UC11) sont un livrable de design à valider avec l'accueil et le gérant avant la deuxième feature de R1 ; elles ne sont pas une feature.

## Date

2026-09-19

## Statut

Acceptée.
