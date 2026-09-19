# Découpage des releases en features

Une feature est l'unité de travail du processus (`docs/processus-feature.md`) : une fiche dans `docs/features/`, six étapes, un point d'arrêt à chaque étape. Une release en contient plusieurs ; une feature couvre un ou plusieurs identifiants du backlog (section 7 du cahier des charges).

Ce fichier est la source du découpage. Il alimente `docs/suivi-features.xlsx` par `tools/generer-suivi.py`. L'affectation des identifiants aux releases est dans `docs/couverture-backlog.md` ; le plan de releases dans `docs/roadmap.md`.

Les identifiants écartés (13, tous V3) ne sont rattachés à aucune feature.

## Comptes

| Release | Nom | Features | Identifiants |
| --- | --- | --- | --- |
| R1 | Comptoir | 16 | 72 |
| R2 | Planning et coachs | 9 | 18 |
| R3 | Parents, relances et exports comptables | 9 | 23 |
| R4 | Pointage | 8 | 16 |
| R5 | Rémunération et arrière-boutique | 7 | 18 |
| R6 | Fin de saison et réinscription | 8 | 13 |
| R7 | Camp d'été | 4 | 6 |
| R8 | Portail parent et acquisition | 9 | 20 |
| R9 | Deuxième club | 3 | 4 |
| **Total** |  | **73** | **190** |

## R1 — Comptoir

| Feature | Nom | Identifiants du backlog | Nb |
| --- | --- | --- | --- |
| F01 | Socle plateforme | PLT-01, PLT-04, PLT-05, SEC-01, SEC-02, SEC-03, SEC-04, ADM-01, INT-03 | 9 |
| F02 | Famille, parents, enfant | FAM-01, FAM-02, FAM-03, FAM-04, FAM-12 | 5 |
| F03 | Dossier complet | FAM-05, FAM-06, FAM-07, FAM-08, FAM-11, ONB-03, ONB-04, ADM-04 | 8 |
| F04 | Catalogue, groupes, saison | ACT-01, ACT-02, ACT-03, ACT-04, NIV-01, GRP-01, GRP-03, GRP-04, PLA-01, PLA-02, PLA-05 | 11 |
| F05 | Offre et grille tarifaire | OFR-01, OFR-02, OFR-03, OFR-04, OFR-05, TAR-01, TAR-02, TAR-03, TAR-06, TAR-08 | 10 |
| F06 | Remises | TAR-04, TAR-05 | 2 |
| F07 | Inscription | INS-01, INS-02, INS-03, GRP-02, GRP-05, FAM-10, ONB-01, ONB-05 | 8 |
| F08 | Facture famille et solde | FAC-01, FAC-02, FAC-06, FAC-09, CPT-02 | 5 |
| F09 | Encaissement et reçu | FAC-03, FAC-05, FAC-07, FIN-04 | 4 |
| F10 | Registre des chèques | FAC-08 | 1 |
| F11 | Avoir et remboursement | FAC-04 | 1 |
| F12 | Caisse et remise du jour | FIN-01, FIN-03 | 2 |
| F13 | Dépenses et résultat mensuel | FIN-02, FIN-05 | 2 |
| F14 | Vente comptoir | POS-01, POS-02 | 2 |
| F15 | Balance âgée | IMP-01 | 1 |
| F16 | Import Excel et solde d'ouverture | FAM-13 | 1 |

## R2 — Planning et coachs

| Feature | Nom | Identifiants du backlog | Nb |
| --- | --- | --- | --- |
| F17 | Profil intervenant | COA-01 | 1 |
| F18 | Génération des séances et vues planning | PLA-03, PLA-04 | 2 |
| F19 | Séance ponctuelle, remplacement, horaires spéciaux | PLA-07, PLA-08, PLA-09 | 3 |
| F20 | Planning du coach et feuille d'appel | COA-03, APC-01, GRP-06 | 3 |
| F21 | Planning publiable | PLA-10 | 1 |
| F22 | Cours d'essai | RES-01 | 1 |
| F23 | Cours privé et adhérent adulte | RES-06, FAM-09 | 2 |
| F24 | Résiliation et changement d'activité | INS-07, INS-08 | 2 |
| F25 | Gouvernance de l'offre et duplication de saison | OFR-06, OFR-07, OFR-08 | 3 |

## R3 — Parents, relances et exports comptables

| Feature | Nom | Identifiants du backlog | Nb |
| --- | --- | --- | --- |
| F26 | Moteur de notifications et connecteur messagerie | NOT-01, NOT-02, NOT-06, NOT-07, INT-01 | 5 |
| F27 | Règles financières et alertes d'expiration | NOT-03, NOT-08, INS-09 | 3 |
| F28 | Règles planning, annulation et report | NOT-04, PLA-06 | 2 |
| F29 | Message ciblé, historique et annonce de masse | COM-01, COM-02, EVT-08 | 3 |
| F30 | Relances d'impayés et promesse de paiement | IMP-02, IMP-03 | 2 |
| F31 | Renouvellement de formule courte | INS-04 | 1 |
| F32 | Cérémonie de fin d'année | EVT-06 | 1 |
| F33 | Tableau de bord inscriptions et finances | REP-02, REP-03, REP-05 | 3 |
| F34 | Exports comptables et clôture de période | CPT-01, CPT-03, CPT-04 | 3 |

## R4 — Pointage

| Feature | Nom | Identifiants du backlog | Nb |
| --- | --- | --- | --- |
| F35 | Appel par le coach | PRE-01, APC-02, APC-03, PRE-08 | 4 |
| F36 | Pointage accueil et statut administratif | PRE-02, PRE-03, IMP-05 | 3 |
| F37 | Débit des carnets au pointage | RES-04 | 1 |
| F38 | Vue temps réel et tableau de bord opérationnel | PRE-04, REP-01 | 2 |
| F39 | Historique, assiduité et séances réalisées | PRE-07, COA-04 | 2 |
| F40 | Sortie de l'enfant | PRE-06 | 1 |
| F41 | Règles de notification sur la présence | NOT-05 | 1 |
| F42 | Droits des personnes et durées de conservation | SEC-05, SEC-06 | 2 |

## R5 — Rémunération et arrière-boutique

| Feature | Nom | Identifiants du backlog | Nb |
| --- | --- | --- | --- |
| F43 | Modèles de rémunération | REM-01 | 1 |
| F44 | État mensuel, avances et paiement de la rémunération | REM-02, REM-03, REM-04, REM-05, APC-05 | 5 |
| F45 | Personnel, prestataires et dépenses récurrentes | PER-01, PER-02 | 2 |
| F46 | Disponibilités et absences des coachs | COA-02 | 1 |
| F47 | Tâches et règles d'automatisation | PER-03, ADM-05, IMP-04 | 3 |
| F48 | Stock | STK-01, STK-02, STK-03 | 3 |
| F49 | Retours, articles liés et codes promo | POS-03, POS-04, TAR-07 | 3 |

## R6 — Fin de saison et réinscription

| Feature | Nom | Identifiants du backlog | Nb |
| --- | --- | --- | --- |
| F50 | Référentiel de compétences et évaluations | NIV-02, NIV-03, APC-04 | 3 |
| F51 | Critères et passage de niveau | NIV-04, NIV-05 | 2 |
| F52 | Certificats et diplômes | EVT-07 | 1 |
| F53 | Campagne de réinscription et relance des anciens | INS-05, MKT-03 | 2 |
| F54 | Liste d'attente et place réservée | RES-02, RES-03 | 2 |
| F55 | Pause d'inscription | INS-06 | 1 |
| F56 | Fusion de doublons | FAM-14 | 1 |
| F57 | Rapports standard | REP-04 | 1 |

## R7 — Camp d'été

| Feature | Nom | Identifiants du backlog | Nb |
| --- | --- | --- | --- |
| F58 | Programme et emploi du temps du camp | EVT-01, EVT-02 | 2 |
| F59 | Tarif du camp | EVT-04 | 1 |
| F60 | Inscription au camp, aux stages et événements | EVT-03, RES-07 | 2 |
| F61 | Journée continue | EVT-05 | 1 |

## R8 — Portail parent et acquisition

| Feature | Nom | Identifiants du backlog | Nb |
| --- | --- | --- | --- |
| F62 | Portail parent : accès, enfants et planning | APP-01, APP-02 | 2 |
| F63 | Factures, solde et paiement en ligne | APP-03, FAC-10, INT-02 | 3 |
| F64 | Présences et progression côté parent | APP-04 | 1 |
| F65 | Démarches en libre-service | APP-05, PRE-05, RES-05, ONB-02 | 4 |
| F66 | Carte famille QR | APP-06 | 1 |
| F67 | Annonces et message du coach | COM-03, COM-04 | 2 |
| F68 | CRM prospects | CRM-01, CRM-02, CRM-03, CRM-04, CRM-05 | 5 |
| F69 | Campagnes marketing | MKT-01 | 1 |
| F70 | Arabe et anglais | PLT-08 | 1 |

## R9 — Deuxième club

| Feature | Nom | Identifiants du backlog | Nb |
| --- | --- | --- | --- |
| F71 | Assistant de démarrage et champs personnalisés | ADM-02, ADM-03 | 2 |
| F72 | Utilisateur multi-clubs | PLT-02 | 1 |
| F73 | Abonnement SaaS | PLT-07 | 1 |
