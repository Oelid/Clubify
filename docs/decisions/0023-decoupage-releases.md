# 0023 — Découpage du backlog en neuf releases

## Contexte

Le cahier des charges met 118 des 203 fonctionnalités au MVP et signale lui-même que c'est un risque (10.I). Il propose quatre lots (10.J) dont les deux premiers ne sont pas utilisables seuls par le club. Il fallait affecter chaque identifiant de la section 7 à une release utilisable de bout en bout, dans le calendrier de la saison (septembre à juin, pic en septembre, camp en juillet), en posant les invariants du `CLAUDE.md` dès la première release.

Décisions prises avec Omar pendant le cadrage : mise en service de R1 en janvier 2027 en creux de saison ; R1 couvre recettes, dépenses et résultat mensuel ; après R1, le terrain sans pointage, puis les parents et relances ; le pointage en release à part au démarrage de la saison 2027-2028 ; aucun matériel de pointage (téléphones des coachs et PC de l'accueil).

## Décision

Neuf releases. R1 « Comptoir » compte 72 identifiants, tous issus du MVP du cahier ; le MVP du cahier est atteint à la fin de R4 ; 13 identifiants V3 sont écartés. Chaque identifiant est affecté une fois et une seule (`docs/couverture-backlog.md`).

| Release | Nom | Fenêtre | Identifiants |
| --- | --- | --- | --- |
| R1 | Comptoir | janvier 2027 | 72 |
| R2 | Planning et coachs | avril 2027, au plus tard juillet 2027 | 18 |
| R3 | Parents, relances et exports comptables | mai-juin 2027, au plus tard septembre 2027 | 23 |
| R4 | Pointage | septembre 2027 | 16 |
| R5 | Rémunération et arrière-boutique | novembre 2027 à janvier 2028 | 18 |
| R6 | Fin de saison et réinscription | mars-avril 2028 | 13 |
| R7 | Camp d'été | mai 2028 | 6 |
| R8 | Portail parent et acquisition | septembre 2028 | 20 |
| R9 | Deuxième club | à la signature d'un second club | 4 |
| — | Écartés | — | 13 |

Le détail par release (objectif, usage remplacé, dépendances, risques) est dans `docs/roadmap.md`.

## Raison

Le MVP du cahier mélange trois usages qui n'ont ni le même calendrier ni les mêmes utilisateurs : le comptoir (accueil, gérant), le terrain (coachs) et les parents (messages). Le premier remplace des classeurs qui existent ; les deux autres créent des pratiques qui n'existent pas. Les livrer ensemble, c'est faire porter trois changements d'un coup au club. R1 ne peut pas être plus petite : on ne facture pas sans dossiers, catalogue, grille et parcours d'inscription, et une release sans facture ajouterait une saisie au lieu d'en retirer une.

Le pointage est une pratique nouvelle (section 1.6) : on l'introduit au premier cours d'une saison, pas au milieu. Sans pointeuse ni borne : le benchmark les écarte pour un club d'enfants (4.1, 4.2, 9.3).

## Alternatives écartées

- Les quatre lots de 10.J : le lot 1 est invisible, le lot 2 impose une double saisie avec Excel Encaissement ; tous deux sont fondus dans R1. Le lot 4 est éclaté en R2, R3 et R4 selon l'utilisateur.
- MVP intégral (118) en une release : trois changements d'un coup, date intenable.
- Pointage en R2 (printemps 2027) : changement d'habitude des coachs en cours de saison pour trois mois de données.
- R1 en juillet 2027, avant le pic : évite la reprise des reliquats mais repousse la première valeur de dix mois ; retenu comme repli si janvier 2027 glisse.
- Pointeuse, borne ou lecteur de badge : inadaptés à des enfants de 3 à 10 ans ; le pointage est un événement « enfant arrivé » avec une origine, extensible à un appareil sans refonte.

## Source dans le cahier des charges

Sections 1.6, 4.1, 4.2, 7 (backlog), 8 (cas d'utilisation), 9.1 à 9.4, 9.6, 10.E, 10.F, 10.G, 10.I, 10.J.

## Écarts ou points ouverts

Écarts avec le cahier :

- 10.J : lots 1 et 2 fondus dans R1 ; « club pilote dès le lot 2 » devient « club pilote dès R1 » ; lot 4 éclaté en R2, R3, R4.
- 10.E : MVP 118 → R1 72. Les 46 autres : 44 en R2 à R4, 2 en R9 (ADM-02, PLT-02, inutiles avec un seul club).
- 10.F : 13 identifiants V2 avancés en R2 à R4 parce que le calendrier l'impose ou qu'ils partagent le chantier : PLA-09, PLA-10, OFR-08, NOT-08, REP-05, CPT-01, CPT-03, CPT-04, PRE-06, PRE-08, NOT-05, SEC-05, SEC-06. Le reste de V2 est réparti sur R5 à R8.
- 10.G : les 13 V3 sont écartés, aucun retenu ; PLT-03 comme écran seulement, l'identifiant de site étant posé en R1.
- Section 8 : les 12 premiers cas d'utilisation fonctionnent de bout en bout à la fin de R4, pas de R1 ; UC20 passe en R2.
- Décisions antérieures : aucune contredite. 0012 dit CPT-03 « en V2 », ici R3, toujours désactivable ; 0015 met EVT-06 au MVP, ici R3 ; 0018 exige la déclaration CNDP avant la mise en service, donc avant janvier 2027 — préalable bloquant.

Dépendances du backlog repérées comme manquantes, fausses ou circulaires :

| # | Constat | Traitement |
| --- | --- | --- |
| 1 | `IMP-05 → PRE-03` et PRE-03 applique la politique définie par IMP-05 : circulaire | Même release (R4) |
| 2 | `FAC-07 → NOT-01` : un reçu PDF imprimé n'a pas besoin du moteur de notifications : fausse | FAC-07 en R1 ; l'envoi arrive avec NOT-01 et COM-01 en R3 |
| 3 | `OFR-08 → INS-05` : dupliquer une saison ne dépend pas de la campagne de réinscription : fausse | OFR-08 en R2, avant juillet 2027 |
| 4 | `INS-04`, `INS-09 → NOT-03` seulement : manquent INS-01 et TAR | Respectées de fait (R3 après R1) |
| 5 | `FAC-06` sans dépendance : dépend de FAC-01, 03, 05, 08 | Même release (R1) |
| 6 | `PRE-02` sans dépendance : dépend de PLA-03 et INS-01 ; sa règle « QR famille » dépend de APP-06 | R4 ; recherche par nom seulement jusqu'à R8 |
| 7 | `RES-04 → TAR-06` seulement : le débit au pointage dépend de PRE-01 | R4, pas R1 ; la vente du carnet reste en R1 |
| 8 | `EVT-06 → FAC-01` : manque une entité Événement, définie seulement par EVT-01 (V2) | R3 crée l'entité minimale |
| 9 | `COA-01 → PLT-02` : surdimensionnée, un profil coach n'exige pas le multi-clubs | COA-01 en R2 sans PLT-02 (R9) |
| 10 | `APC-02` = « Voir PRE-01 » : doublon ; `APC-01` inclus dans COA-03 | Affectés avec leur original |
| 11 | Dépendances au niveau domaine (`GRP-01 → ACT, NIV` ; `ONB-01 → FAM, INS, FAC` ; `INS-01 → GRP, TAR` ; `INS-08 → TAR` ; `REP-03 → FAC` ; `CPT-01 → FIN` ; `OFR-05 → TAR`) | Lues comme « tout le domaine », toutes satisfaites |
| 12 | `TAR-04` priorité S mais UC2 (fratrie) est MVP | Traité comme M en R1 |
| 13 | `NIV-05 → GRP-05` seulement : dépend aussi de NIV-01 et NIV-04 | Respectées (R6) |
| 14 | `FAM-13` sans dépendance : dépend de FAM-01 à 04 et, pour une bascule en cours de saison, du solde d'ouverture | R1 ; voir manques |
| 15 | `FAC-08` : « rejet = créance réouverte et tâche » mais les tâches (PER-03) sont V2 | R1 : créance réouverte et alerte dans la balance ; tâche en R5 |
| 16 | `PLT-03` (V3) et `PLT-08` (V2) : identifiant de site et architecture i18n exigés dès le MVP par 9.6 et par les invariants | Invariants en R1 ; écrans et langues plus tard |

Manques (aucun identifiant ne les couvre) : solde d'ouverture famille pour la reprise des reliquats en janvier 2027, nouvel identifiant `FAC-11` proposé en R1 ; dossier d'aide CNDP et contrat de sous-traitance ; choix d'hébergement ; frais de rejet de chèque (UC5, à cadrer dans FAC-08) ; recalcul du rang fratrie après résiliation (0021, à cadrer dans TAR-04) ; formation et conduite du changement.

Points ouverts : fiche santé FAM-07 en R1 (hypothèse retenue, à confirmer avec le juriste) ; modalités du solde d'ouverture ; exercice comptable du club ; cérémonie de juin 2027 couverte seulement si R3 est en service en mai ; fournisseur WhatsApp Business ; dépôt CNDP avant janvier 2027 ; confirmation des 13 écartés. Liste complète dans `docs/couverture-backlog.md`.

## Date

2026-09-19

## Statut

Acceptée.
