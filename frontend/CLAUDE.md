# Frontend — règles propres

Workspace Angular unique pour les trois interfaces : backoffice de l'accueil et du gérant (R1), application coach (R4), portail parent (R8). Toutes consomment le même backend et le même contrat. Les règles générales sont dans `../CLAUDE.md` et s'appliquent ici. La stack et le système de design sont actés par `docs/decisions/0025-stack-frontend.md` ; la bibliothèque de composants respecte la décision 0017.

## Stack

- Angular 21 (21.2.x), TypeScript strict, Vitest intégré. Angular 22 exige Node ≥ 22.22.3 ;
  la montée se fera par `ng update` quand Node sera à jour, en emmenant PrimeNG et
  angularx-qrcode, qui s'alignent sur la même version majeure.
- PrimeNG 21 (mode thémable par jetons de design) + `@primeuix/themes` + `@angular/cdk` + Tailwind CSS 4.
- Transloco pour l'i18n. Lucide pour les icônes. Polices Inter et IBM Plex Sans Arabic, auto-hébergées. angularx-qrcode pour le QR du second facteur (décision 0029).
- Client d'API généré par openapi-generator (typescript-angular) depuis `../contracts/openapi.yaml`.
- Tests : Vitest, Storybook, Playwright.
- Toute dépendance absente de cette liste est demandée à Omar avant d'être ajoutée (`../CLAUDE.md` §5).

## Arborescence

```
frontend/
├── angular.json, package.json
├── projects/
│   ├── backoffice/          application accueil et gérant (R1)
│   ├── coach/               application coach, web mobile (R4, à créer alors)
│   ├── parent/              portail parent, web mobile (R8, à créer alors)
│   ├── ui/                  bibliothèque : système de design, composants, jetons, thème PrimeNG
│   └── api-client/          bibliothèque générée depuis le contrat ; jamais modifiée à la main, jamais commitée
└── e2e/                     Playwright, parcours critiques seulement
```

Dans une application, un dossier par domaine métier, nommé comme au backend et dans `docs/glossaire.md` :

```
projects/backoffice/src/app/
├── core/                    authentification, intercepteurs, garde de routes, TenantContext, configuration
├── layout/                  coquille, navigation, en-tête
├── family/                  pages, composants et services du domaine
│   ├── pages/               composants routés (une page = un fichier)
│   ├── components/          composants propres au domaine
│   ├── data/                services d'accès (appellent api-client), stores en signals
│   └── family.routes.ts     routes lazy du domaine
├── catalog/, offer/, enrollment/, billing/, cash/, pos/ …
└── app.routes.ts            routes racine, chaque domaine chargé en lazy
```

Ce qui est partagé par plus d'une application vit dans `ui` (composants, jetons, pipes de format) ou dans `api-client`. Rien n'est copié d'une application à l'autre.

## Conventions de code

- Composants standalone uniquement ; pas de `NgModule`.
- Signals partout : `signal`, `computed`, `effect` avec parcimonie, `input()`, `output()`, `model()`, `viewChild()`. Appels HTTP par `resource`/`httpResource` ou via les services de `api-client`. RxJS seulement là où un flux est réellement nécessaire (recherche avec anti-rebond, WebSocket).
- Application zoneless ; détection de changement `OnPush` par défaut.
- Nouveau flux de contrôle : `@if`, `@for` (avec `track`), `@switch`, `@defer`. Jamais `*ngIf`/`*ngFor`.
- Routes lazy par domaine ; pas de chargement anticipé du tout.
- Formulaires : réactifs, typés ; validation de forme côté client, règles métier côté backend (`../CLAUDE.md`). Le prix, le solde et les statuts viennent toujours du backend, jamais recalculés côté client.
- Aucune logique métier dans les composants : elle est dans le backend ; le composant affiche, saisit, appelle.
- Noms techniques en anglais tirés du glossaire ; fichiers en kebab-case, classes en PascalCase, un composant par fichier.
- Montants reçus en centimes : formatés par un pipe de `ui` avec la devise du club, chiffres tabulaires. Dates reçues en UTC : affichées dans le fuseau du club par un pipe de `ui`. Aucun formatage à la main dans un composant.

## Contrat d'API

- Le frontend consomme `../contracts/openapi.yaml` par `api-client`, généré à chaque build. Jamais le modèle de données directement, jamais un appel `HttpClient` écrit à la main vers l'API.
- Les identifiants de club et de site ne sont jamais envoyés par le client : ils viennent du jeton.
- Les codes d'erreur du backend (`ProblemDetail`, champ `code`) sont traduits par Transloco ; le message du backend sert de repli.

## Système de design

Bibliothèque `ui`. Construite avant le premier écran ; validée en FR et en RTL dans Storybook avant la première livraison d'écran (`../CLAUDE.md`, PLT-08).

- **Jetons** en variables CSS, source unique dans `ui` : couleurs, typographie, espacements, rayons, ombres, mouvement. Tailwind et le thème PrimeNG lisent ces variables ; aucune couleur ni taille en dur dans un composant.
- **Deux couches de jetons** :
  - jetons **Clubify** : palette neutre et premium (gris chauds, blanc, une couleur d'accent produit), sémantiques (`--color-surface`, `--color-text-muted`, `--color-danger`…) ;
  - jetons **de marque par club** : `--brand-primary`, `--brand-secondary`, logo. Chargés depuis les paramètres du club (ADM-01) à la connexion. Appliqués aux documents (reçu, fiche d'inscription), au portail parent et en accent dans le backoffice ; jamais au-delà. Valeurs par défaut = jetons Clubify. Thème du club pilote : orange `#F08840`, bleu acier `#307890` (palette de référence, à remplacer par le logo du P'tit Club dès qu'il est fourni).
- **Références** : Stripe Dashboard pour le backoffice (densité, tableaux, chiffres alignés à droite, une action principale par écran, hiérarchie par la typographie plutôt que par la couleur) ; principes Apple (clarté, déférence au contenu, profondeur) pour le coach et le parent. Premium ne veut pas dire aéré : l'accueil a un parent devant elle et cinq minutes.
- **Typographie** : Inter (latin) et IBM Plex Sans Arabic (arabe), auto-hébergées, `font-variant-numeric: tabular-nums` sur tout montant, numéro et date.
- **RTL dès le jour 1** : propriétés logiques uniquement (`margin-inline-start`, `padding-inline`, `inset-inline-end`, `text-align: start`) ; jamais `left`/`right`. Icônes directionnelles miroir en RTL. `dir` posé sur `<html>` par Transloco selon la langue.
- **Accessibilité AA** : contraste vérifié sur les jetons, focus visible, navigation clavier complète, libellés sur tout champ et toute icône, `aria-live` pour les confirmations.
- **Mouvement** : jetons `--motion-fast` (150 ms), `--motion-base` (250 ms), assouplissement standard ; CSS uniquement (`animate.enter`/`animate.leave`, transitions, View Transitions API pour les routes) ; tout respecte `prefers-reduced-motion` ; aucune animation sur le chemin critique du comptoir.
- **Écrans proches des grilles Excel** là où le club en a (matrice de prix, rosters, caisse, balance) : tableau PrimeNG avec édition en ligne, collage depuis Excel pour OFR-04, tri et filtres.
- **Composants** : un composant de `ui` enveloppe chaque composant PrimeNG utilisé, avec les jetons appliqués ; les applications n'importent jamais PrimeNG directement. Chaque composant de `ui` a sa story.

## Parcours comptoir

- Inscription en moins de cinq minutes (UC1) : parcours guidé, brouillon sauvegardé (ONB-01), recherche famille instantanée, tabulation logique, Entrée pour valider, validation en ligne sans modale bloquante, téléphone au format E.164 avec +212 par défaut.
- Une action principale par écran, visible sans défilement.
- Les alertes liées à une dette s'adressent au staff, jamais à l'enfant (UC6).
- Les données sensibles (santé, CIN) ne s'affichent que sur les écrans qui y sont autorisés ; fiche enfant réduite du coach (APC-03) sans donnée financière ni coordonnées complètes.

## i18n

- Transloco, fichiers `assets/i18n/<lang>.json` par application et pour `ui` ; FR livré, AR et EN ensuite. Clés en anglais, structurées par domaine (`family.form.phone.label`).
- Aucun libellé en dur dans un template ni dans un composant, y compris les messages d'erreur, les titres, les `aria-label` et les textes des documents.
- Langue par utilisateur (PLT-08), changée à chaud ; la direction suit la langue.

## Coach et parent (R4, R8)

- Mobile d'abord, web, installable en PWA plus tard (mode hors ligne PRE-08). Rien dans `ui` ne doit l'empêcher : aucune dépendance à une taille d'écran fixe.
- Appel en 30 secondes pour 10 enfants, tolérant au réseau faible (UC7).

## Stratégie de tests

- Vitest : composants et services, données fictives.
- Storybook : chaque composant de `ui` et chaque page notable, en FR et en RTL ; c'est le support de validation des maquettes avec l'accueil et le gérant.
- Playwright : parcours critiques seulement — inscription UC1, clôture de caisse UC11 — contre un backend de test.
- Aucun nom réel ni donnée issue des captures dans les stories, les tests ni les captures d'écran (`../CLAUDE.md` §7) : familles fictives partagées avec le backend.
- Budget de performance vérifié en CI : taille du bundle initial de chaque application plafonnée dans `angular.json`.

## Sécurité

- Jeton JWT du backend (0024) stocké en mémoire, refresh token en cookie `HttpOnly` ; jamais de jeton dans `localStorage`.
- Intercepteur unique pour l'authentification et la langue ; garde de routes par rôle.
- Aucune donnée sensible mise en cache côté client au-delà de la session.

## Commandes

Depuis `frontend/` :

| Commande | Effet |
| --- | --- |
| `npm ci` | Installe les dépendances depuis le lock. **npm 11 au minimum** : npm 10 échoue sur le graphe de dépendances pairs de Vitest 4 (`Cannot read properties of null (reading 'edgesOut')`). Mettre à jour par `npm install -g npm@11`. Ne jamais recourir à `--legacy-peer-deps` : le lock produit ainsi est désynchronisé et `npm ci` le refuse. |
| `npm install` | À réserver à l'ajout d'une dépendance, après accord (`../CLAUDE.md` §5). |
| `npm run api` | Régénère `projects/api-client/` depuis `../contracts/openapi.yaml`. À relancer après toute modification du contrat. Le dossier n'est jamais commité. |
| `npm start` | Sert le backoffice sur `http://localhost:4200`. |
| `npm run build` | Construit le backoffice. |
| `npm test` | Tests unitaires de tous les projets (Vitest). |
| `npm run test:ci` | Idem, sans surveillance. |
| `npm run e2e` | Parcours critiques (Playwright). Démarre le serveur si besoin. |
| `npm run storybook` | Catalogue des composants, en FR et en droite-à-gauche. |

Le client d'API est généré, jamais écrit à la main : `projects/api-client/` est dans `.gitignore`.
