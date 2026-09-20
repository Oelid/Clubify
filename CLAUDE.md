# Clubify — règles du projet

Chargé à chaque session. Court par choix : il renvoie vers les autres fichiers au lieu de les recopier.

## 1. Mission

Clubify est un SaaS multi-clubs de gestion de clubs de sport, conçu d'abord pour un club pour enfants de 3 à 14 ans au Maroc.
Le cœur du métier : inscrire un enfant dans un groupe récurrent pour une saison, et encaisser au comptoir en espèces, chèque ou virement.
Il remplace des classeurs Excel, un formulaire papier et un carnet de reçus, où chaque paiement est saisi trois fois et le reste à payer nulle part.
La réservation à la séance est un cas secondaire : cours privés, carnets, essais, rattrapages, camp.
C'est un logiciel d'école d'activités, pas un logiciel de salle de sport.

## 2. Où trouver quoi

| Chemin | Contenu |
| --- | --- |
| `docs/cahier-des-charges/cahier-des-charges-v1.md` | Référence métier, version 1.1 figée, lecture seule |
| `docs/cahier-des-charges/sources/` | Pièces d'origine ; captures exclues du dépôt (données personnelles) |
| `docs/glossaire.md` | Termes métier et noms techniques anglais à utiliser dans le code |
| `docs/modele-donnees.md` | Entités et relations (brouillon, puis tenu à jour feature par feature) |
| `docs/roadmap.md` | Le plan de releases (R1 à R9) : objectif, usage remplacé, fenêtre de mise en service |
| `docs/couverture-backlog.md` | Affectation des 203 identifiants du backlog aux releases, avec les raisons |
| `docs/decoupage-features.md` | Découpage des releases en 73 features, avec leurs identifiants |
| `docs/suivi-features.xlsx` | Suivi de l'avancement : état, dates de cycle, date de production ; régénéré par `tools/generer-suivi.py` |
| `docs/suivi-tests.xlsx` | Recette fonctionnelle : scénarios joués depuis les écrans, résultats réels, couverture des critères, anomalies ; régénéré par `tools/generer-suivi-tests.py` |
| `docs/suivi-tests.md` | Les scénarios de recette en langage métier, et comment la lancer. Source du classeur ci-dessus |
| `docs/recette.md` | Comment lancer l'application : environnement de recette en conteneurs, ou serveurs de développement |
| `docs/processus-feature.md` | Les six étapes d'implémentation d'une feature, avec leurs points d'arrêt |
| `docs/features/` | Une fiche par feature, à partir de `_gabarit.md` |
| `docs/decisions/` | Une fiche par décision, à partir de `0000-gabarit.md` |
| `contracts/` | Contrat d'API, modifié en premier dans chaque feature |
| `backend/CLAUDE.md` | Règles propres au backend, unique pour toutes les interfaces |
| `frontend/CLAUDE.md` | Règles propres au frontend : workspace Angular unique pour le backoffice (accueil et gérant), le coach et le parent |

## 3. Invariants non négociables

- Identifiant de club, et de site quand il existe, sur chaque donnée. Isolation stricte entre clubs (PLT-01, 9.6).
- Montants stockés en centimes, avec devise (9.6).
- Dates en UTC ; le fuseau du club sert à l'affichage et aux règles calendaires (9.6).
- Suppression logique uniquement (9.6).
- Facture et paiement jamais supprimés ; correction par avoir ou contre-passation (FAC-01, FAC-04, FAC-07).
- Journal d'audit non modifiable sur tout ce qui touche à l'argent, aux remises, aux dérogations et aux données sensibles (SEC-04, 9.4).
- Tous les libellés via i18n : FR d'abord, arabe droite-à-gauche et anglais ensuite (PLT-08).
- Aucune règle métier codée en dur si elle figure parmi les règles configurables par club (section 9.8).
- Connecteurs abstraits pour le paiement et la messagerie ; aucun prestataire codé en dur (INT-01, INT-02).

## 4. Méthode de travail

Le détail est dans `docs/processus-feature.md` : six étapes, un point d'arrêt à chaque fois.

- Une feature à la fois.
- Lire la fiche de la feature (`docs/features/`) avant de coder.
- Vérifier la complétude et faire le benchmark, borné au sujet, avant le plan.
- Proposer un plan avec analyse d'impact (majeur ou maîtrisé) et régressions possibles ; attendre la validation d'Omar avant d'écrire du code.
- Écrire les tests des règles métier et de non-régression avant le code.
- Livrer ensemble, dans la même livraison : contrat d'API, modèle de données, fiche de feature, code, tests.

## 5. Ce que tu ne fais jamais sans demander

- Migration destructive.
- Nouvelle dépendance.
- Modification du contrat d'API hors de la feature en cours.
- Changement d'un invariant de la section 3.
- Refonte hors périmètre de la feature en cours.

## 6. Définition de « terminé »

Une feature est terminée quand :
- les critères d'acceptation de sa fiche passent, avec des tests automatisés pour chaque règle métier et pour chaque régression identifiée ;
- le contrat d'API, `docs/modele-donnees.md` et la fiche sont à jour ;
- les règles configurables par club le sont, avec une valeur par défaut documentée ;
- les libellés sont dans les fichiers i18n, aucun en dur ;
- l'audit et l'isolation par club sont couverts par un test ;
- la ligne de la feature dans `docs/suivi-features.xlsx` est passée à « Production » avec sa date ;
- Omar a validé.

## 7. Données sensibles

- Données d'enfants et de santé : jamais dans les logs, jamais dans les jeux de test, jamais dans les captures d'écran.
- Aucun nom réel dans le dépôt. Les jeux d'essai utilisent des familles fictives qui reproduisent la structure observée (fratries, reliquats, chèque différé), jamais les personnes.
- Les captures d'origine restent hors du dépôt tant qu'elles ne sont pas floutées (`docs/cahier-des-charges/sources/README.md`).
- Santé, CIN et pièces : chiffrement, accès par rôle, accès journalisé (SEC-03).

## 8. Règle de vérité

Le cahier des charges (version 1.1) est figé et ne se modifie pas.
Ensuite, la fiche de feature fait foi.
Tout écart entre une fiche et le cahier des charges est consigné dans `docs/decisions/`, avec sa raison.
En cas de doute sur une règle métier : ne rien inventer, écrire « À confirmer » et poser la question.
