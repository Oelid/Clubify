# Roadmap

Le backlog du cahier des charges (section 7, 203 fonctionnalités) est découpé en neuf releases successives, chacune utilisable telle quelle par le club. Le découpage est acté par la décision `docs/decisions/0023-decoupage-releases.md` ; l'affectation de chaque identifiant est dans `docs/couverture-backlog.md`.

Première mise en service : R1 en janvier 2027, en creux de saison. Le MVP du cahier des charges (118 fonctionnalités) est atteint à la fin de R4. Aucune estimation en jours : la stack n'est pas choisie. Ordre de grandeur relatif : R1 ≈ 4 × R2.

## Préalables non logiciels à R1

- Déclaration CNDP et demande d'autorisation pour les données de santé, déposées par le gérant (décision 0018) ; Clubify fournit le dossier d'aide et le contrat de sous-traitance.
- Choix d'hébergement et localisation des données (section 5).
- Formation de l'accueil avant la bascule ; reprise des soldes d'ouverture par famille (voir « Manques » dans `docs/couverture-backlog.md`).

## R1 — Comptoir

- **Objectif** : une inscription saisie une fois produit la facture, le reçu, le solde et la caisse.
- **Usage réel remplacé** : formulaire papier D1, classeur Liste d'inscription D2/D3, grilles de groupes D4, six grilles tarifaires D5, classeur Encaissement D6 (recettes, dépenses, reste caisse), état de remises D7, carnet de reçus D8. Le reste à payer par famille existe pour la première fois.
- **Fenêtre de mise en service** : janvier 2027, entrées en cours de saison. Si ratée : après mars il ne reste pas assez de saison pour amortir la bascule ; fenêtre suivante juillet 2027, et les reliquats 2026-2027 restent suivis de mémoire. Repli si la date glisse : double saisie sur les seules nouvelles inscriptions dès que R1 est prête, bascule complète avec reprise des soldes au plus tard en juillet 2027.
- **Identifiants (72)** :
  - Socle : PLT-01, PLT-04, PLT-05, SEC-01, SEC-02, SEC-03, SEC-04, ADM-01, ADM-04, INT-03
  - Dossiers : FAM-01, FAM-02, FAM-03, FAM-04, FAM-05, FAM-06, FAM-07, FAM-08, FAM-10, FAM-11, FAM-12, FAM-13
  - Inscription guidée : ONB-01, ONB-03, ONB-04, ONB-05
  - Catalogue : ACT-01, ACT-02, ACT-03, ACT-04, NIV-01, GRP-01, GRP-02, GRP-03, GRP-04, GRP-05, PLA-01, PLA-02, PLA-05
  - Offre et tarifs : OFR-01, OFR-02, OFR-03, OFR-04, OFR-05, TAR-01, TAR-02, TAR-03, TAR-04, TAR-05, TAR-06, TAR-08
  - Inscriptions : INS-01, INS-02, INS-03
  - Argent : FAC-01, FAC-02, FAC-03, FAC-04, FAC-05, FAC-06, FAC-07, FAC-08, FAC-09, CPT-02, IMP-01
  - Caisse et dépenses : FIN-01, FIN-02, FIN-03, FIN-04, FIN-05
  - Vente comptoir : POS-01, POS-02
- **Invariants posés ici, jamais rattrapés** : identifiant de club et de site sur chaque donnée, montants en centimes avec devise, dates en UTC, suppression logique, journal d'audit, i18n (FR livré, architecture prête pour l'arabe et l'anglais), connecteurs abstraits paiement et messagerie (l'abstraction est posée, le premier connecteur arrive en R3).
- **Dépendances entrantes** : aucune release ; les préalables non logiciels ci-dessus.
- **Cas d'utilisation couverts** : UC1, UC2, UC3, UC4, UC5, UC11, UC19 (sans stock).
- **Risques** : taille de la release contre la date ; reprise des soldes d'ouverture sans document de référence ; adoption par l'accueil habitué à Excel (parades : matrice de prix collable OFR-04, import FAM-13, écrans proches des grilles).

## R2 — Planning et coachs

- **Objectif** : le planning réel devient la source unique des séances, des coachs et des cours privés.
- **Usage réel remplacé** : affiches planning imprimées D9 ; planning coach et salle qui n'existait nulle part (section 1.6) ; cours privés facturés sans trace de séance.
- **Fenêtre de mise en service** : avril 2027, en service au plus tard en juillet 2027 pour préparer la saison 2027-2028 (duplication de saison, groupes, créneaux). Si ratée : le gérant ressaisit toute l'offre et les groupes à la main en août, et R4 devient impossible en septembre faute de séances datées.
- **Identifiants (18)** : PLA-03, PLA-04, PLA-07, PLA-08, PLA-09, PLA-10, OFR-06, OFR-07, OFR-08, COA-01, COA-03, APC-01, RES-01, RES-06, GRP-06, FAM-09, INS-07, INS-08
- **Dépendances entrantes** : R1.
- **Cas d'utilisation couverts** : UC20 ; UC14 pour la partie essai (la relance J+1 est CRM-05, R8).
- **Risques** : le pointage n'est pas dans cette release (décision d'Omar : pratique nouvelle, introduite en début de saison) ; une séance annulée se saisit sans message jusqu'à R3.

## R3 — Parents, relances et exports comptables

- **Objectif** : le club parle aux parents depuis l'outil, et les reliquats sont relancés sans y penser.
- **Usage réel remplacé** : messages WhatsApp tapés à la main sur la liste de diffusion D10 ; rappels oraux des reliquats ; reçu envoyé en photo ; extraction manuelle pour le comptable.
- **Fenêtre de mise en service** : mai-juin 2027 pour relancer les reliquats de fin de saison et couvrir la cérémonie de juin ; au plus tard septembre 2027 pour les échéanciers du pic. Si ratée après septembre 2027 : le pic se fait sans relances ni annonces, et la première cérémonie couverte est celle de juin 2028. Délai externe : approbation des modèles WhatsApp par Meta, à lancer en amont.
- **Identifiants (23)** : NOT-01, NOT-02, NOT-03, NOT-04, NOT-06, NOT-07, NOT-08, INT-01, COM-01, COM-02, IMP-02, IMP-03, INS-04, INS-09, PLA-06, EVT-06, EVT-08, REP-02, REP-03, REP-05, CPT-01, CPT-03, CPT-04
- **Dépendances entrantes** : R1 ; R2 pour PLA-06.
- **Cas d'utilisation couverts** : UC9, UC10, UC12.
- **Risques** : coût et règles de WhatsApp Business (parades : messages de service seulement, SMS en secours, suivi des coûts NOT-07) ; exercice comptable du club à confirmer pour CPT-01.

## R4 — Pointage

- **Objectif** : savoir qui est venu, débiter les carnets, appliquer la règle du club en cas de dette sans jamais mettre l'enfant en cause.
- **Usage réel remplacé** : aucun, la présence n'est suivie nulle part (section 1.6). Apporte l'assiduité, le décompte des carnets et le contrôle de sortie.
- **Fenêtre de mise en service** : septembre 2027, premier cours de la saison 2027-2028, pour que les coachs prennent l'habitude dès le départ. Si ratée : pratique nouvelle, on ne l'introduit pas en cours de saison ; fenêtre suivante septembre 2028, et la rémunération à la séance (R5) reste bloquée un an.
- **Identifiants (16)** : PRE-01, PRE-02, PRE-03, PRE-04, PRE-06, PRE-07, PRE-08, APC-02, APC-03, IMP-05, RES-04, COA-04, REP-01, NOT-05, SEC-05, SEC-06
- **Dépendances entrantes** : R1, R2, R3.
- **Cas d'utilisation couverts** : UC6, UC7, UC8, UC13 (transfert refusé si le groupe cible est plein), UC16.
- **Équipement** : téléphones des coachs et PC de l'accueil ; aucune pointeuse, aucun achat. Le pointage est un événement « enfant arrivé » avec une origine (coach, accueil), extensible plus tard à un appareil sans refonte.
- **Risques** : refus des coachs (parades : 30 secondes pour 10 enfants, formation avant le pic, mode hors ligne dès le premier jour, feuille imprimée en secours) ; la politique d'accès IMP-05 démarre en mode « alerter », jamais « bloquer ».

## R5 — Rémunération et arrière-boutique

- **Objectif** : le dû de chaque intervenant se calcule depuis les séances pointées, sans paie (décision 0013).
- **Usage réel remplacé** : lignes de salaires et de prestataires saisies dans le journal mensuel D6 ; comptage manuel des séances par le gérant.
- **Fenêtre de mise en service** : novembre 2027 à janvier 2028, après un mois complet de séances pointées. Si ratée : peu bloquant, l'état mensuel se fait à n'importe quel mois.
- **Identifiants (18)** : REM-01, REM-02, REM-03, REM-04, REM-05, APC-05, PER-01, PER-02, PER-03, COA-02, IMP-04, ADM-05, POS-03, POS-04, STK-01, STK-02, STK-03, TAR-07
- **Dépendances entrantes** : R1, R3, R4.
- **Cas d'utilisation couverts** : UC18.
- **Risques** : contestation séance par séance (parade : détail dans l'état) ; la partie déclarative reste au comptable.

## R6 — Fin de saison et réinscription

- **Objectif** : clore une saison et ouvrir la suivante depuis l'outil : passages de niveau, diplômes, campagne de réinscription, liste d'attente.
- **Usage réel remplacé** : inscrits de la saison précédente recopiés et surlignés (D2, P15) ; liste des diplômes envoyée au prestataire à la main.
- **Fenêtre de mise en service** : mars-avril 2028, pour la campagne de mai-juin 2028 et la cérémonie de juin 2028. Si ratée : la réinscription 2028-2029 se fait à la main ; fenêtre suivante mars 2029.
- **Identifiants (13)** : INS-05, INS-06, RES-02, RES-03, MKT-03, NIV-02, NIV-03, NIV-04, NIV-05, APC-04, EVT-07, FAM-14, REP-04
- **Dépendances entrantes** : R2, R3, R4.
- **Cas d'utilisation couverts** : UC13 complet, UC15, UC17.
- **Risques** : bascule des tranches d'âge et des niveaux d'une saison à l'autre (UC17), à cadrer avec OFR-08.

## R7 — Camp d'été

- **Objectif** : le camp de juillet devient une offre autonome inscrite, tarifée et pointée dans l'outil (décision 0014).
- **Usage réel remplacé** : inscriptions au camp prises par téléphone puis sur papier (D11), affiche et rappels WhatsApp.
- **Fenêtre de mise en service** : mai 2028 pour le camp de juillet 2028. Si ratée : camp 2028 hors outil ; fenêtre suivante mai 2029. Le camp 2027 n'est pas couvert.
- **Identifiants (6)** : EVT-01, EVT-02, EVT-03, EVT-04, EVT-05, RES-07
- **Dépendances entrantes** : R1, R3, R4.
- **Risques** : à dire au club dès maintenant que le camp 2027 reste sur papier.

## R8 — Portail parent et acquisition

- **Objectif** : le parent agit seul (dossier, planning, factures, absences, réinscription, paiement en ligne quand le club aura choisi son acquéreur) ; les prospects et essais sont suivis.
- **Usage réel remplacé** : appels à l'accueil pour un solde ou un horaire ; suivi des essais « c-e » dans le roster D4.
- **Fenêtre de mise en service** : septembre 2028, les parents découvrent le portail à l'inscription. Si ratée : peu bloquant, l'adoption est simplement meilleure au pic.
- **Identifiants (20)** : APP-01, APP-02, APP-03, APP-04, APP-05, APP-06, PRE-05, RES-05, ONB-02, FAC-10, INT-02, COM-03, COM-04, CRM-01, CRM-02, CRM-03, CRM-04, CRM-05, MKT-01, PLT-08
- **Dépendances entrantes** : R1, R3, R4, R6.
- **Cas d'utilisation couverts** : UC14 complet.
- **Risques** : FAC-10 et INT-02 conditionnés au choix d'un acquéreur par le club (décision 0011) ; l'arabe droite-à-gauche (PLT-08) est livré ici, l'architecture i18n étant posée en R1.

## R9 — Deuxième club

- **Objectif** : un nouveau club s'installe seul.
- **Usage réel remplacé** : le paramétrage fait avec nous pour le club pilote.
- **Fenêtre de mise en service** : aucune fenêtre calendaire ; déclenchée par la signature d'un second club.
- **Identifiants (4)** : ADM-02, ADM-03, PLT-02, PLT-07
- **Dépendances entrantes** : R1.
- **Risques** : généralisation prématurée (10.I) ; ne rien développer ici avant le second client.

## Écartés

Treize identifiants, tous V3 dans le cahier, hors de ce plan de releases : STK-04, STK-05, FIN-06, FIN-07, CPT-05, REP-06, MKT-02, MKT-04, APP-07, INT-04, INT-05, PLT-03, PLT-06. Raison par identifiant dans `docs/couverture-backlog.md`. PLT-03 est écarté comme écran seulement : l'identifiant de site est posé en R1 par invariant.

## Suivi de l'avancement

Le découpage des releases en features est dans `docs/decoupage-features.md` (73 features).
L'avancement se suit dans `docs/suivi-features.xlsx` : état, responsable, dates du cycle
de développement et date de mise en production, par feature et par identifiant du backlog.

Le classeur se régénère par `python tools/generer-suivi.py` après tout changement de
périmètre ou de découpage ; les saisies sont conservées.
